"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = __importDefault(require("express"));
const dotenv_1 = __importDefault(require("dotenv"));
const logger_1 = require("./logger");
const JobBotApiClient_1 = require("./api/JobBotApiClient");
const BrowserManager_1 = require("./automation/BrowserManager");
const NaukriApplicator_1 = require("./automation/NaukriApplicator");
const IndeedApplicator_1 = require("./automation/IndeedApplicator");
const PlatformRateChecker_1 = require("./rate/PlatformRateChecker");
const HumanBehavior_1 = require("./automation/HumanBehavior");
dotenv_1.default.config();
const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080';
const POLL_MS = Number(process.env.POLL_INTERVAL_MS || 30_000);
const MIN_APPLY_GAP = Number(process.env.MIN_INTER_APPLY_MS || 60_000);
const MAX_APPLY_GAP = Number(process.env.MAX_INTER_APPLY_MS || 90_000);
const HEALTH_PORT = Number(process.env.HEALTH_PORT || 3001);
const PLATFORMS = (process.env.PLATFORMS || 'NAUKRI,INDEED')
    .split(',')
    .map((s) => s.trim().toUpperCase())
    .filter(Boolean);
const stats = {
    startedAt: new Date().toISOString(),
    polls: 0,
    applied: 0,
    failed: 0,
    lastPollAt: null,
    lastAppliedAt: null,
    lastError: null,
};
async function main() {
    const api = new JobBotApiClient_1.JobBotApiClient(API_BASE_URL, process.env.API_USERNAME, process.env.API_PASSWORD, process.env.API_TOKEN || undefined);
    let authenticated = false;
    while (!authenticated) {
        try {
            await api.ensureAuthenticated();
            authenticated = true;
        }
        catch (e) {
            logger_1.logger.warn(`[ENGINE] Failed to connect to backend (${e.message}). Retrying in 5 seconds...`);
            await new Promise(resolve => setTimeout(resolve, 5000));
        }
    }
    const bm = new BrowserManager_1.BrowserManager(process.env.CHROME_PROFILE_DIR);
    let context = await bm.launch();
    for (const platform of PLATFORMS) {
        await bm.injectSession(api, platform);
    }
    const rate = new PlatformRateChecker_1.PlatformRateChecker(api);
    // BUG FIX: Keep applicators as mutable refs so we can re-create them after
    // a browser crash without losing the outer scope references.
    let applicators = {
        naukri: new NaukriApplicator_1.NaukriApplicator(context),
        indeed: new IndeedApplicator_1.IndeedApplicator(context),
    };
    startHealthEndpoint();
    let polling = false;
    const tick = async () => {
        if (polling)
            return;
        polling = true;
        stats.polls++;
        stats.lastPollAt = new Date().toISOString();
        try {
            for (const platform of PLATFORMS) {
                if (!(await rate.canApply(platform)))
                    continue;
                const job = await api.getNextPending(platform);
                if (!job)
                    continue;
                logger_1.logger.info(`Picked ${platform} job ${job.id} — ${job.title} @ ${job.company}`);
                let result;
                try {
                    result = await applyOn(platform, job, applicators.naukri, applicators.indeed);
                }
                catch (e) {
                    const msg = e.message || '';
                    // BUG FIX: Detect browser/context crash and auto-recover
                    if (msg.includes('Target page, context or browser has been closed') ||
                        msg.includes('browserContext.newPage') ||
                        msg.includes('Browser has been closed') ||
                        msg.includes('Target closed')) {
                        logger_1.logger.warn(`[ENGINE] Browser context crashed — attempting auto-recovery...`);
                        try {
                            await bm.close().catch(() => { });
                            context = await bm.launch();
                            for (const p of PLATFORMS) {
                                await bm.injectSession(api, p);
                            }
                            applicators = {
                                naukri: new NaukriApplicator_1.NaukriApplicator(context),
                                indeed: new IndeedApplicator_1.IndeedApplicator(context),
                            };
                            logger_1.logger.info('[ENGINE] ✅ Browser recovered. Retrying job...');
                            result = await applyOn(platform, job, applicators.naukri, applicators.indeed);
                        }
                        catch (recoveryErr) {
                            logger_1.logger.error(`[ENGINE] Browser recovery failed: ${recoveryErr.message}`);
                            result = { success: false, reason: `BROWSER_CRASH: ${msg}` };
                        }
                    }
                    else {
                        result = { success: false, reason: `EXCEPTION: ${msg}` };
                    }
                }
                try {
                    await api.report({
                        jobQueueId: job.id,
                        success: result.success,
                        failureReason: result.reason ?? null,
                    });
                }
                catch (e) {
                    logger_1.logger.warn(`Report failed: ${e.message}`);
                }
                if (result.success) {
                    stats.applied++;
                    stats.lastAppliedAt = new Date().toISOString();
                    logger_1.logger.info(`[ENGINE] ✅ Applied! Waiting ${Math.round(MIN_APPLY_GAP / 1000)}s before next job.`);
                    await (0, HumanBehavior_1.humanDelay)(MIN_APPLY_GAP, MAX_APPLY_GAP);
                }
                else {
                    stats.failed++;
                    stats.lastError = result.reason ?? null;
                    // Company website redirect: short wait (these are fast failures)
                    const waitMs = result.reason?.includes('COMPANY_WEBSITE_REDIRECT') ? 5_000 : 60_000;
                    await (0, HumanBehavior_1.humanDelay)(waitMs, waitMs + 30_000);
                }
            }
        }
        catch (e) {
            stats.lastError = e.message;
            logger_1.logger.warn(`Poll cycle failed: ${e.message}`);
        }
        finally {
            polling = false;
        }
    };
    await tick();
    setInterval(() => void tick(), POLL_MS);
    process.on('SIGINT', async () => {
        logger_1.logger.info('SIGINT — shutting down');
        await bm.close();
        process.exit(0);
    });
}
async function applyOn(platform, job, naukri, indeed) {
    switch (platform) {
        case 'NAUKRI':
            return naukri.apply(job);
        case 'INDEED':
            return indeed.apply(job);
        default:
            return { success: false, reason: `Unsupported platform ${platform}` };
    }
}
function startHealthEndpoint() {
    const app = (0, express_1.default)();
    app.get('/health', (_req, res) => res.json({ status: 'running', ...stats }));
    app.get('/stats', (_req, res) => res.json(stats));
    app.listen(HEALTH_PORT, () => logger_1.logger.info(`Health endpoint on :${HEALTH_PORT}`));
}
main().catch((e) => {
    logger_1.logger.error(`Fatal: ${e.message}`);
    process.exit(1);
});
//# sourceMappingURL=index.js.map