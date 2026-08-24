export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
  timestamp: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface Resume {
  id: string;
  name: string;
  fileUrl?: string;
  targetRoles: string[];
  targetSkills: string[];
  resumeText?: string;
  experienceSummary?: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface JobCriteria {
  id: string;
  name: string;
  resumeId?: string;
  keywords: string[];
  locations: string[];
  experienceMin: number;
  experienceMax: number;
  salaryMinLpa?: number;
  salaryMaxLpa?: number;
  jobType?: string;
  excludeCompanies: string[];
  minMatchScore: number;
  booleanQuery?: string;
  active: boolean;
  createdAt?: string;
}

export type JobStatus = 'new' | 'matched' | 'applying' | 'applied' | 'skipped' | 'error';

export interface Job {
  id: string;
  platform: string;
  platformJobId?: string;
  title: string;
  company: string;
  location: string;
  description: string;
  url?: string;
  salaryRange?: string;
  experienceRequired?: string;
  postedDate?: string;
  scrapedAt?: string;
  criteriaId?: string;
  matchScore?: number;
  matchKeywords?: string[];
  missingKeywords?: string[];
  reasonToApply?: string;
  status: JobStatus;
}

export type ApplicationStatus =
  | 'applied' | 'viewed' | 'shortlisted' | 'interview' | 'offer' | 'rejected' | 'withdrawn';

export interface Application {
  id: string;
  jobId: string;
  resumeId?: string;
  criteriaId?: string;
  platform?: string;
  company?: string;
  title?: string;
  appliedAt?: string;
  status: ApplicationStatus;
  atsScore?: number;
  matchedKeywords?: string[];
  missingKeywords?: string[];
  interviewDate?: string;
  interviewRound?: number;
  offerCtcLpa?: number;
  notes?: string;
}

export interface AtsResult {
  score: number;
  matchedKeywords: string[];
  missingKeywords: string[];
  bestResumeAngle: string;
  suggestions: string;
  shouldApply: boolean;
  reasonToApply: string;
  breakdown: Record<string, number>;
  aiNote?: string;
}

export interface DashboardStats {
  totalApplied: number;
  interviews: number;
  offers: number;
  activeRejections: number;
  successRate: number;
  todayApplied: number;
  byPlatform: Record<string, number>;
  byStatus: Record<string, number>;
}

export interface ResumeMatch {
  resumeId: string;
  resumeName: string;
  score: number;
  shouldApply: boolean;
  matchedKeywords: string[];
  missingKeywords: string[];
  bestResumeAngle: string;
  recommended: boolean;
}

export interface ResumePerformance {
  resumeId: string;
  resumeName: string;
  applications: number;
  responses: number;
  interviews: number;
  offers: number;
  responseRate: number;
  interviewRate: number;
}

// ============ v2 additions ============

export type QueueStatus =
  | 'PENDING_REVIEW' | 'APPROVED' | 'AUTO_APPLYING' | 'APPLIED'
  | 'FAILED_APPLY' | 'MANUAL_APPLY' | 'SKIPPED' | 'FILTERED_OUT';

export interface JobQueueEntry {
  id: string;
  externalId: string;
  platform: string;
  title: string;
  company: string;
  location?: string;
  jobUrl: string;
  description?: string;
  atsScore?: number;
  matchedKeywords: string[];
  missingKeywords: string[];
  resumeId?: string;
  criteriaId?: string;
  status: QueueStatus;
  failureReason?: string;
  appliedAt?: string;
  reviewedAt?: string;
  createdAt?: string;
}

export interface QueueStats {
  pendingReview: number;
  approved: number;
  autoApplying: number;
  applied: number;
  failed: number;
  manual: number;
  skipped: number;
  filteredOut: number;
}

export interface PlatformConfig {
  id: string;
  platformName: 'NAUKRI' | 'LINKEDIN' | 'INDEED' | string;
  enabled: boolean;
  dailyLimit: number;
  minDelaySeconds: number;
  currentCountToday: number;
  lastResetDate?: string;
  paused: boolean;
  createdAt?: string;
  // Session linking fields (v2.1)
  /** DISCONNECTED | CONNECTED | EXPIRED | ERROR */
  sessionStatus?: string;
  sessionActive?: boolean;
  /** Display-only: the username/email of the linked account. */
  sessionUsername?: string | null;
  sessionConnectedAt?: string | null;
}

export interface DiscoveryRunResult {
  platform: string;
  totalFound: number;
  newJobs: number;
  filteredOut: number;
  alreadySeen: number;
  ranAt: string;
  durationMs: number;
  errorMessage?: string;
}

export interface DiscoveryStatus {
  lastRunAt: string;
  cron: string;
}


