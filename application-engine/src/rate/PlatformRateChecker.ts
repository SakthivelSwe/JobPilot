import { logger } from '../logger';
import { JobBotApiClient } from '../api/JobBotApiClient';

/**
 * Client-side courtesy check against the server's PlatformConfig. The server
 * ALWAYS re-validates when handing out a pending job, but doing this locally
 * first avoids pulling a job we can't apply to.
 */
export class PlatformRateChecker {
  constructor(private readonly api: JobBotApiClient) {}

  async canApply(platform: string): Promise<boolean> {
    try {
      const cfg = await this.api.getPlatformConfig(platform);
      if (!cfg) return false;
      if (!cfg.enabled) {
        logger.info(`Rate check: ${platform} disabled`);
        return false;
      }
      if (cfg.paused) {
        logger.info(`Rate check: ${platform} paused`);
        return false;
      }
      if (cfg.currentCountToday >= cfg.dailyLimit) {
        logger.info(
          `Rate check: ${platform} at daily limit (${cfg.currentCountToday}/${cfg.dailyLimit})`,
        );
        return false;
      }
      return true;
    } catch (e: any) {
      logger.warn(`Rate check failed for ${platform}: ${e.message}`);
      return false;
    }
  }
}

