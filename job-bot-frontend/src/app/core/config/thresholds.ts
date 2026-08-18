import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from '../services/api.service';

/**
 * Single frontend source of truth for business thresholds (spec rule 68).
 * Defaults mirror the backend {@code JobPilotThresholds}; `refresh()` syncs the
 * live values from `/api/config/thresholds` so the two never drift.
 * Import THESE — never hard-code 80 / 5 / 20 in a component again.
 */
export interface Thresholds {
  learningMinApplications: number;
  defaultMinMatchScore: number;
  strongMatchScore: number;
  followUpDays: number;
  recommendStrongApply: number;
  recommendApply: number;
  recommendReview: number;
  recommendLowPriority: number;
}

export const DEFAULT_THRESHOLDS: Thresholds = {
  learningMinApplications: 20,
  defaultMinMatchScore: 65,
  strongMatchScore: 80,
  followUpDays: 5,
  recommendStrongApply: 90,
  recommendApply: 80,
  recommendReview: 70,
  recommendLowPriority: 55,
};

@Injectable({ providedIn: 'root' })
export class ConfigService {
  private api = inject(ApiService);
  readonly thresholds = signal<Thresholds>(DEFAULT_THRESHOLDS);

  /** Sync from the backend once after login; falls back silently to defaults. */
  refresh(): void {
    this.api.get<Thresholds>('/api/config/thresholds').subscribe({
      next: t => this.thresholds.set({ ...DEFAULT_THRESHOLDS, ...t }),
      error: () => { /* keep defaults */ },
    });
  }

  get strongMatch(): number { return this.thresholds().strongMatchScore; }
  get followUpDays(): number { return this.thresholds().followUpDays; }
  get learningMin(): number { return this.thresholds().learningMinApplications; }
}

