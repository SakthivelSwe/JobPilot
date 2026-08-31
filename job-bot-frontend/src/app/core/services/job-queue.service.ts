import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';

export type JobQueueStatus =
  | 'PENDING_REVIEW'
  | 'APPROVED'
  | 'AUTO_APPLYING'
  | 'APPLIED'
  | 'FAILED_APPLY'
  | 'MANUAL_APPLY'
  | 'SKIPPED'
  | 'FILTERED_OUT';

export interface JobQueueEntry {
  id: string;
  jobPostingId?: string;
  externalId: string;
  platform: 'NAUKRI' | 'LINKEDIN' | 'INDEED' | string;
  title: string;
  company: string;
  location?: string;
  jobUrl: string;
  description?: string;
  atsScore?: number;
  matchScore?: number;
  recommendation?: string;
  matchedKeywords?: string[];
  missingKeywords?: string[];
  resumeVariant?: string;
  status: JobQueueStatus;
  failureReason?: string;
  appliedAt?: string;
  reviewedAt?: string;
  createdAt?: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class JobQueueService {
  private api = inject(ApiService);

  pending(page = 0, size = 20) {
    return this.api.get<Page<JobQueueEntry>>('/api/queue/pending', { page, size });
  }
  autoApplying(page = 0, size = 20) {
    return this.api.get<Page<JobQueueEntry>>('/api/queue/auto-applying', { page, size });
  }
  manual(page = 0, size = 50) {
    return this.api.get<Page<JobQueueEntry>>('/api/queue/manual', { page, size });
  }
  stats() {
    return this.api.get<Record<JobQueueStatus, number>>('/api/queue/stats');
  }
  approve(id: string) { return this.api.post<JobQueueEntry>(`/api/queue/${id}/approve`, {}); }
  skip(id: string) { return this.api.post<JobQueueEntry>(`/api/queue/${id}/skip`, {}); }
  sendToManual(id: string) { return this.api.post<JobQueueEntry>(`/api/queue/${id}/send-to-manual`, {}); }
  markApplied(id: string) { return this.api.post<JobQueueEntry>(`/api/queue/${id}/mark-applied`, {}); }
  approveAllAbove(threshold = 80) {
    return this.api.post<number>(`/api/queue/approve-all-above?threshold=${threshold}`, {});
  }
  approveBulk(postingIds: string[]) {
    return this.api.post<number>('/api/queue/approve-bulk', postingIds);
  }
}

