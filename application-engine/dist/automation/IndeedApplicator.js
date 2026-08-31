"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.IndeedApplicator = void 0;
const fs_1 = __importDefault(require("fs"));
const path_1 = __importDefault(require("path"));
const logger_1 = require("../logger");
const HumanBehavior_1 = require("./HumanBehavior");
/**
 * Indeed Easy Apply flow (basic first pass). Skips complex multi-step forms —
 * those get returned as MANUAL and end up in the manual queue.
 */
class IndeedApplicator {
    context;
    constructor(context) {
        this.context = context;
    }
    async apply(job) {
        const page = await this.context.newPage();
        try {
            await (0, HumanBehavior_1.humanDelay)(3000, 6000);
            logger_1.logger.info(`[INDEED] Opening ${job.jobUrl}`);
            await page.goto(job.jobUrl, { waitUntil: 'domcontentloaded', timeout: 45_000 });
            if (await this.hasCaptcha(page))
                return this.fail('CAPTCHA', job, page);
            const easyApply = page.locator('button[id*=indeedApplyButton], button:has-text("Apply now"), button:has-text("Easy apply")').first();
            if (!(await easyApply.count()))
                return this.fail('NO_APPLY_BUTTON', job, page);
            await (0, HumanBehavior_1.humanClick)(page, easyApply);
            await (0, HumanBehavior_1.humanDelay)(3000, 5000);
            // Step through Continue / Review / Submit up to 8 steps.
            for (let i = 0; i < 8; i++) {
                if (await this.hasCaptcha(page))
                    return this.fail('CAPTCHA', job, page);
                const submit = page
                    .locator('button:has-text("Submit application"), button:has-text("Submit your application")')
                    .first();
                if (await submit.count()) {
                    await (0, HumanBehavior_1.humanClick)(page, submit);
                    await (0, HumanBehavior_1.humanDelay)(3000, 5000);
                    break;
                }
                const continueBtn = page
                    .locator('button:has-text("Continue"), button:has-text("Review")')
                    .first();
                if (!(await continueBtn.count()))
                    break;
                await (0, HumanBehavior_1.humanClick)(page, continueBtn);
                await (0, HumanBehavior_1.humanDelay)(2000, 3500);
            }
            const success = (await page
                .locator('text=Application submitted, text=Your application was submitted')
                .count()) > 0;
            const shot = await this.screenshot(page, job, success ? 'success' : 'unclear');
            return success
                ? { success: true, screenshotPath: shot }
                : { success: false, reason: 'SUBMIT_UNCONFIRMED', screenshotPath: shot };
        }
        catch (e) {
            logger_1.logger.warn(`[INDEED] Exception: ${e.message}`);
            return { success: false, reason: `EXCEPTION: ${e.message}` };
        }
        finally {
            await page.close({ runBeforeUnload: true }).catch(() => void 0);
        }
    }
    async hasCaptcha(page) {
        return ((await page.locator('iframe[src*="recaptcha"], .g-recaptcha').count()) > 0);
    }
    async fail(reason, job, page) {
        const shot = await this.screenshot(page, job, `fail-${reason}`);
        return { success: false, reason, screenshotPath: shot };
    }
    async screenshot(page, job, kind) {
        const dir = process.env.SCREENSHOT_DIR || './logs/screenshots';
        if (!fs_1.default.existsSync(dir))
            fs_1.default.mkdirSync(dir, { recursive: true });
        const file = path_1.default.join(dir, `${job.id}-${kind}-${Date.now()}.png`);
        try {
            await page.screenshot({ path: file, fullPage: false });
        }
        catch { /* ignore */ }
        return file;
    }
}
exports.IndeedApplicator = IndeedApplicator;
//# sourceMappingURL=IndeedApplicator.js.map