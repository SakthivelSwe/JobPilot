import { Browser, BrowserContext, chromium as playwrightChromium } from 'playwright';
import { logger } from '../logger';
import fs from 'fs';
import path from 'path';

// Stealth is applied via playwright-extra. We keep the type as vanilla playwright.
// eslint-disable-next-line @typescript-eslint/no-var-requires
const { chromium } = require('playwright-extra');
// eslint-disable-next-line @typescript-eslint/no-var-requires
const StealthPlugin = require('puppeteer-extra-plugin-stealth');
chromium.use(StealthPlugin());

/**
 * Manages a persistent Chromium context. NEVER headless — running visibly is
 * the single most important anti-detection rule.
 */
export class BrowserManager {
  private context: BrowserContext | null = null;

  constructor(
    private readonly profileDir: string = './chrome-profile',
    private readonly viewport = { width: 1280, height: 900 },
  ) {}

  async launch(): Promise<BrowserContext> {
    if (this.context) return this.context;
    if (!fs.existsSync(this.profileDir)) fs.mkdirSync(this.profileDir, { recursive: true });
    const abs = path.resolve(this.profileDir);
    logger.info(`Launching persistent Chromium at ${abs}`);
    this.context = await chromium.launchPersistentContext(abs, {
      headless: false,
      viewport: this.viewport,
      args: [
        '--no-sandbox',
        '--disable-setuid-sandbox',
        '--disable-blink-features=AutomationControlled',
      ],
    });
    // Auto-login to Naukri on startup so all subsequent page visits are authenticated
    await this.ensureNaukriLogin();
    return this.context!;
  }

  /**
   * Opens Naukri and logs in if not already logged in.
   * Uses a persistent profile so session is reused across runs.
   */
  async ensureNaukriLogin(): Promise<void> {
    const email = process.env.NAUKRI_EMAIL;
    const password = process.env.NAUKRI_PASSWORD;
    if (!email || !password) {
      logger.warn('NAUKRI_EMAIL/NAUKRI_PASSWORD not set — skipping auto-login');
      return;
    }
    const page = await this.context!.newPage();
    try {
      logger.info('Checking Naukri login status...');
      await page.goto('https://www.naukri.com/mnjuser/homepage', { waitUntil: 'domcontentloaded', timeout: 30_000 });
      await page.waitForTimeout(2000);
      
      // Check if already logged in (profile icon visible)
      const loggedIn = await page.locator('.nI-gNb-drawer__icon, [class*="userAvatar"], .view-profile-wrapper, .nI-gNb-sb__icon-text:has-text("Hi")').count();
      if (loggedIn > 0) {
        logger.info('✅ Already logged in to Naukri');
        return;
      }
      
      logger.info('Logging in to Naukri...');
      // Click login button
      const loginBtn = page.locator('a:has-text("Login"), button:has-text("Login"), [class*="login" i]').first();
      if (await loginBtn.count()) await loginBtn.click();
      await page.waitForTimeout(1500);
      
      // Fill credentials
      const emailInput = page.locator('input#usernameField, input[name=email], input[type=email], input[placeholder*="Email" i]').first();
      const passInput = page.locator('input#passwordField, input[name=password], input[type=password]').first();
      
      if (await emailInput.count()) {
        await emailInput.fill(email);
        await page.waitForTimeout(500);
        await passInput.fill(password);
        await page.waitForTimeout(500);
        await page.keyboard.press('Enter');
        await page.waitForTimeout(4000);
        logger.info('✅ Logged in to Naukri successfully');
      } else {
        logger.warn('Could not find Naukri login form');
      }
    } catch (e: any) {
      logger.warn(`Naukri login attempt failed: ${e.message}`);
    } finally {
      await page.close().catch(() => void 0);
    }
  }

  async close(): Promise<void> {
    if (this.context) {
      await this.context.close();
      this.context = null;
    }
  }
}

// Re-export so consumers can typecheck against the vanilla type.
export type { Browser };
export const chromiumRef = playwrightChromium;

