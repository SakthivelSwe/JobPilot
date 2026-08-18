import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';

export interface ManualQueueEntry {
  id: string;
  postingId: string;
  company: string;
  role: string;
  source: string;
  jobUrl: string;
  applicationUrl: string;
  capability: string;
  reason: string;
  matchScore: number;
  recommendedVariant: string;
  status: 'PENDING' | 'OPENED' | 'APPLIED' | 'SKIPPED';
  applicationId?: string;
  createdAt?: string;
  appliedAt?: string;
}

export interface ManualQueueStats {
  pending: number;
  opened: number;
  applied: number;
  skipped: number;
}

@Injectable({ providedIn: 'root' })
export class ManualQueueService {
  private api = inject(ApiService);

  list(status?: string) {
    return this.api.get<ManualQueueEntry[]>('/api/manual-queue', status ? { status } : undefined);
  }
  stats() { return this.api.get<ManualQueueStats>('/api/manual-queue/stats'); }
  add(postingId: string) { return this.api.post<ManualQueueEntry>(`/api/manual-queue/add/${postingId}`, {}); }
  open(id: string) { return this.api.post<ManualQueueEntry>(`/api/manual-queue/${id}/open`, {}); }
  markApplied(id: string) { return this.api.post<ManualQueueEntry>(`/api/manual-queue/${id}/mark-applied`, {}); }
  skip(id: string) { return this.api.post<ManualQueueEntry>(`/api/manual-queue/${id}/skip`, {}); }
}

