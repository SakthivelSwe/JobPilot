import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { JobPosting } from './match.service';

export interface SourceHealthRow {
  source: string;
  status: 'HEALTHY' | 'DEGRADED' | 'UNAVAILABLE' | 'MANUAL';
  companies: number;
  lastChecked?: string;
}

export interface CoverageStats {
  sourcesConfigured: number;
  sourcesActive: number;
  sourcesManual: number;
  sourcesUnavailable: number;
  companiesMonitored: number;
  postingsTotal: number;
  postingsLast24h: number;
}

export interface DiscoveryScanResult {
  companiesScanned: number;
  totalFound: number;
  newPostings: number;
  crossSourceDuplicates: number;
  alreadySeen: number;
  errors: number;
  scannedAt: string;
  sourceOutcomes: any[];
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class DiscoveryService {
  private api = inject(ApiService);

  scan() { return this.api.post<DiscoveryScanResult>('/api/discovery/scan', {}); }
  sources() { return this.api.get<SourceHealthRow[]>('/api/discovery/sources'); }
  coverage() { return this.api.get<CoverageStats>('/api/discovery/coverage'); }
  postings(page = 0, size = 20, status?: string) {
    return this.api.get<Page<JobPosting>>('/api/discovery/postings', { page, size, status });
  }
}


