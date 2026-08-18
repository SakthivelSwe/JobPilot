export interface PendingJob {
  id: string;
  jobPostingId?: string;
  externalId: string;
  platform: 'NAUKRI' | 'LINKEDIN' | 'INDEED' | string;
  title: string;
  company: string;
  location?: string;
  jobUrl: string;
  description?: string;
  matchScore?: number;
  resumeVariant?: string;
}

export interface ApplyResult {
  success: boolean;
  reason?: string | null;
  screenshotPath?: string;
}

export interface PlatformConfig {
  platformName: string;
  enabled: boolean;
  dailyLimit: number;
  minDelaySeconds: number;
  currentCountToday: number;
  paused: boolean;
}

