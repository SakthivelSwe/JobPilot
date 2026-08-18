import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';

export interface AnalyticsOverview {
  jobsDiscovered: number;
  discoveredLast24h: number;
  jobsMatched: number;
  strongMatches: number;
  inQueue: number;
  autoEligibleJobs: number;
  manualRequiredJobs: number;
  applications: number;
  manualApplications: number;
  responseRate: number;
  interviewRate: number;
  offerRate: number;
  averageAts: number;
  averageMatch: number;
}

export interface LearningResult {
  applications: number;
  threshold: number;
  ready: boolean;
  message?: string;
  recommendations: string[];
}

export interface MomentumFactor { name: string; value: number; points: number; detail: string; }
export interface Momentum {
  available: boolean;
  score: number | null;
  label: string;
  windowDays: number;
  factors: MomentumFactor[];
  message: string;
}

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private api = inject(ApiService);

  overview() { return this.api.get<AnalyticsOverview>('/api/analytics/overview'); }
  momentum() { return this.api.get<Momentum>('/api/analytics/momentum'); }
  roles() { return this.api.get<Record<string, any>[]>('/api/analytics/roles'); }
  sources() { return this.api.get<Record<string, any>[]>('/api/analytics/sources'); }
  locations() { return this.api.get<Record<string, any>[]>('/api/analytics/locations'); }
  learning() { return this.api.get<LearningResult>('/api/analytics/learning'); }
}

export interface AiUsage {
  date: string;
  dailyLimit: number;
  used: number;
  remaining: number;
  byFeature: Record<string, number>;
}

@Injectable({ providedIn: 'root' })
export class AiUsageService {
  private api = inject(ApiService);
  usage() { return this.api.get<AiUsage>('/api/ai/usage'); }
}

