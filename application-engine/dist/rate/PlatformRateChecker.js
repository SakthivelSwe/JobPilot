"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PlatformRateChecker = void 0;
const logger_1 = require("../logger");
/**
 * Client-side courtesy check against the server's PlatformConfig. The server
 * ALWAYS re-validates when handing out a pending job, but doing this locally
 * first avoids pulling a job we can't apply to.
 */
class PlatformRateChecker {
    api;
    constructor(api) {
        this.api = api;
    }
    async canApply(platform) {
        try {
            const cfg = await this.api.getPlatformConfig(platform);
            if (!cfg)
                return false;
            if (!cfg.enabled) {
                logger_1.logger.info(`Rate check: ${platform} disabled`);
                return false;
            }
            if (cfg.paused) {
                logger_1.logger.info(`Rate check: ${platform} paused`);
                return false;
            }
            if (cfg.currentCountToday >= cfg.dailyLimit) {
                logger_1.logger.info(`Rate check: ${platform} at daily limit (${cfg.currentCountToday}/${cfg.dailyLimit})`);
                return false;
            }
            return true;
        }
        catch (e) {
            logger_1.logger.warn(`Rate check failed for ${platform}: ${e.message}`);
            return false;
        }
    }
}
exports.PlatformRateChecker = PlatformRateChecker;
//# sourceMappingURL=PlatformRateChecker.js.map