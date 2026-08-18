import express from 'express';
import dotenv from 'dotenv';
import { logger } from './logger';
import { JobBotApiClient } from './api/JobBotApiClient';
import { BrowserManager } from './automation/BrowserManager';
import { NaukriApplicator } from './automation/NaukriApplicator';
import { IndeedApplicator } from './automation/IndeedApplicator';
import { PlatformRateChecker } from './rate/PlatformRateChecker';
import { humanDelay } from './automation/HumanBehavior';
import { PendingJob } from './types';

dotenv.config();

const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080';
const POLL_MS = Number(process.env.POLL_INTERVAL_MS || 30_000);
const MIN_APPLY_GAP = Number(process.env.MIN_INTER_APPLY_MS || 5 * 60 * 1000);
const MAX_APPLY_GAP = Number(process.env.MAX_INTER_APPLY_MS || 8 * 60 * 1000);
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
  lastPollAt: null as string | null,
  lastAppliedAt: null as string | null,
  lastError: null as string | null,
};

async function main() {
  const api = new JobBotApiClient(
    API_BASE_URL,
    process.env.API_USERNAME,
    process.env.API_PASSWORD,
    process.env.API_TOKEN || undefined,
  );
  await api.ensureAuthenticated();

  const bm = new BrowserManager(process.env.CHROME_PROFILE_DIR);
  const context = await bm.launch();
  const rate = new PlatformRateChecker(api);

  const naukri = new NaukriApplicator(context);
  const indeed = new IndeedApplicator(context);

  startHealthEndpoint();

  let polling = false;
  const tick = async () => {
    if (polling) return;
    polling = true;
    stats.polls++;
    stats.lastPollAt = new Date().toISOString();
    try {
      for (const platform of PLATFORMS) {
        if (!(await rate.canApply(platform))) continue;
        const job = await api.getNextPending(platform);
        if (!job) continue;
        logger.info(`Picked ${platform} job ${job.id} — ${job.title} @ ${job.company}`);
        const result = await applyOn(platform, job, naukri, indeed);
        try {
          await api.report({
            jobQueueId: job.id,
            success: result.success,
            failureReason: result.reason ?? null,
          });
        } catch (e: any) {
          logger.warn(`Report failed: ${e.message}`);
        }
        if (result.success) {
          stats.applied++;
          stats.lastAppliedAt = new Date().toISOString();
          await humanDelay(MIN_APPLY_GAP, MAX_APPLY_GAP);
        } else {
          stats.failed++;
          stats.lastError = result.reason ?? null;
          await humanDelay(60_000, 120_000);
        }
      }
    } catch (e: any) {
      stats.lastError = e.message;
      logger.warn(`Poll cycle failed: ${e.message}`);
    } finally {
      polling = false;
    }
  };

  await tick();
  setInterval(() => void tick(), POLL_MS);

  process.on('SIGINT', async () => {
    logger.info('SIGINT — shutting down');
    await bm.close();
    process.exit(0);
  });
}

async function applyOn(
  platform: string,
  job: PendingJob,
  naukri: NaukriApplicator,
  indeed: IndeedApplicator,
) {
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
  const app = express();
  app.get('/health', (_req, res) => res.json({ status: 'running', ...stats }));
  app.get('/stats', (_req, res) => res.json(stats));
  app.listen(HEALTH_PORT, () => logger.info(`Health endpoint on :${HEALTH_PORT}`));
}

main().catch((e) => {
  logger.error(`Fatal: ${e.message}`);
  process.exit(1);
});

