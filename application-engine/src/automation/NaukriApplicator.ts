import { BrowserContext, Page } from 'playwright';
import fs from 'fs';
import path from 'path';
import { logger } from '../logger';
import { ApplyResult, PendingJob } from '../types';
import { humanClick, humanDelay, humanType } from './HumanBehavior';

/**
 * Naukri apply flow.
 *  1. Open the job URL.
 *  2. Detect "Already Applied" → early exit.
 *  3. Detect CAPTCHA → return CAPTCHA (backend routes to manual queue).
 *  4. Click "Apply" (both variants).
 *  5. If a login wall appears and credentials are configured, fill + submit.
 *  6. Screenshot the outcome.
 */
export class NaukriApplicator {
  constructor(private readonly context: BrowserContext) {}

  async apply(job: PendingJob): Promise<ApplyResult> {
    const page = await this.context.newPage();
    try {
      await humanDelay(3000, 6000);
      logger.info(`[NAUKRI] Opening ${job.jobUrl}`);
      await page.goto(job.jobUrl, { waitUntil: 'domcontentloaded', timeout: 45_000 });

      // ---- Guards
      if (await this.hasCaptcha(page)) return this.fail('CAPTCHA', job, page);
      if (await this.isAlreadyApplied(page)) {
        return { success: false, reason: 'ALREADY_APPLIED' };
      }
      const applyBtn = await this.findApplyButton(page);
      if (!applyBtn) return this.fail('NO_APPLY_BUTTON', job, page);

      await humanClick(page, applyBtn);
      await humanDelay(2500, 4500);

      // ---- Login wall
      if (await page.locator('input[type=password]').count()) {
        if (!process.env.NAUKRI_EMAIL || !process.env.NAUKRI_PASSWORD) {
          return this.fail('LOGIN_REQUIRED', job, page);
        }
        logger.info('[NAUKRI] Login wall — filling credentials');
        // Common selectors on Naukri login modal / page
        const emailSel = 'input#usernameField, input[name=email], input[type=email]';
        const passSel = 'input#passwordField, input[name=password], input[type=password]';
        await humanType(page, emailSel, process.env.NAUKRI_EMAIL!);
        await humanDelay(500, 900);
        await humanType(page, passSel, process.env.NAUKRI_PASSWORD!);
        await humanDelay(600, 1200);
        const submit = page.locator('button[type=submit], button:has-text("Login")').first();
        await humanClick(page, submit);
        await humanDelay(3000, 5000);
        if (await this.hasCaptcha(page)) return this.fail('CAPTCHA', job, page);
      }

      // ---- File upload (best effort — Naukri usually already has your resume on file)
      // If a file input appears we skip; user's Naukri profile carries the résumé.

      // ---- Final submit if a chatbot-style form is present
      const submitBtn = page.locator(
        'button:has-text("Submit"), button:has-text("Send"), button.chatbot_Submit',
      ).last();
      if (await submitBtn.count()) {
        await humanClick(page, submitBtn);
        await humanDelay(3000, 5000);
      }

      // ---- Verify success (text can vary)
      const successText = await page
        .locator(
          'text=Applied Successfully, text=You have applied, text=Application Submitted, text=successfully applied',
        )
        .count();
      const success = successText > 0;
      const shot = await this.screenshot(page, job, success ? 'success' : 'unclear');
      if (!success) {
        return { success: false, reason: 'SUBMIT_UNCONFIRMED', screenshotPath: shot };
      }
      logger.info(`[NAUKRI] Applied: ${job.title} @ ${job.company}`);
      return { success: true, screenshotPath: shot };
    } catch (e: any) {
      logger.warn(`[NAUKRI] Exception applying to ${job.id}: ${e.message}`);
      return { success: false, reason: `EXCEPTION: ${e.message}` };
    } finally {
      await page.close({ runBeforeUnload: true }).catch(() => void 0);
    }
  }

  // ---- helpers ----

  private async hasCaptcha(page: Page): Promise<boolean> {
    return (
      (await page.locator('iframe[src*="recaptcha"], .g-recaptcha, iframe[title*="captcha" i]').count()) > 0
    );
  }

  private async isAlreadyApplied(page: Page): Promise<boolean> {
    return (
      (await page.locator('text=Already Applied, text=You applied on').count()) > 0
    );
  }

  private async findApplyButton(page: Page) {
    const candidates = [
      'button:has-text("Apply")',
      'button:has-text("Apply for this job")',
      'a:has-text("Apply")',
      '#apply-button',
      'button[id*=Apply]',
    ];
    for (const sel of candidates) {
      const l = page.locator(sel).first();
      if (await l.count()) return l;
    }
    return null;
  }

  private async screenshot(page: Page, job: PendingJob, kind: string) {
    const dir = process.env.SCREENSHOT_DIR || './logs/screenshots';
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    const file = path.join(dir, `${job.id}-${kind}-${Date.now()}.png`);
    try {
      await page.screenshot({ path: file, fullPage: false });
    } catch { /* ignore */ }
    return file;
  }

  private async fail(reason: string, job: PendingJob, page: Page): Promise<ApplyResult> {
    const shot = await this.screenshot(page, job, `fail-${reason}`);
    return { success: false, reason, screenshotPath: shot };
  }
}

