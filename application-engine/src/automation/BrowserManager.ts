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
        '--start-minimized',
        '--no-sandbox',
        '--disable-setuid-sandbox',
        '--disable-blink-features=AutomationControlled',
      ],
    });
    return this.context!;
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

