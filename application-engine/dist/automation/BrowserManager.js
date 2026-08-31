"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.chromiumRef = exports.BrowserManager = void 0;
const playwright_1 = require("playwright");
const logger_1 = require("../logger");
const fs_1 = __importDefault(require("fs"));
const path_1 = __importDefault(require("path"));
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
class BrowserManager {
    profileDir;
    viewport;
    context = null;
    constructor(profileDir = './chrome-profile', viewport = { width: 1280, height: 900 }) {
        this.profileDir = profileDir;
        this.viewport = viewport;
    }
    async launch() {
        if (this.context)
            return this.context;
        if (!fs_1.default.existsSync(this.profileDir))
            fs_1.default.mkdirSync(this.profileDir, { recursive: true });
        const abs = path_1.default.resolve(this.profileDir);
        logger_1.logger.info(`Launching persistent Chromium at ${abs}`);
        this.context = await chromium.launchPersistentContext(abs, {
            headless: false,
            viewport: this.viewport,
            args: [
                '--no-sandbox',
                '--disable-setuid-sandbox',
                '--disable-blink-features=AutomationControlled',
            ],
        });
        return this.context;
    }
    /**
     * Fetches the authenticated session (cookies) from the JobPilot backend
     * and injects it into the persistent browser context.
     */
    async injectSession(api, platform) {
        if (!this.context)
            return;
        try {
            logger_1.logger.info(`Fetching ${platform} session from backend...`);
            const sessionState = await api.getSessionState(platform);
            if (sessionState && sessionState.cookies) {
                await this.context.addCookies(sessionState.cookies);
                logger_1.logger.info(`✅ Injected ${platform} session cookies from backend`);
            }
            else {
                logger_1.logger.warn(`No active session found for ${platform} in backend. Please connect your account in JobPilot Settings.`);
            }
        }
        catch (e) {
            logger_1.logger.warn(`Failed to fetch/inject ${platform} session: ${e.message}`);
        }
    }
    async close() {
        if (this.context) {
            await this.context.close();
            this.context = null;
        }
    }
}
exports.BrowserManager = BrowserManager;
exports.chromiumRef = playwright_1.chromium;
//# sourceMappingURL=BrowserManager.js.map