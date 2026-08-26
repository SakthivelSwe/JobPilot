import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';

export interface JobPosting {
  id: string;
  source: string;
  externalId: string;
  title: string;
  company: string;
  location?: string;
  remoteType?: string;
  sourceUrl?: string;
  applicationUrl?: string;
  description?: string;
  requiredSkills?: string[];
  preferredSkills?: string[];
  minimumExperience?: number;
  maximumExperience?: number;
  applicationCapability?: string;
  matchScore?: number;
  recommendation?: string;
  status?: string;
  createdAt?: string;
}

export interface MatchResult {
  overallScore: number;
  technicalScore: number;
  experienceScore: number;
  roleScore: number;
  locationScore: number;
  workModeScore: number;
  noticeScore: number;
  salaryScore: number;
  companyScore: number;
  matchedSkills: string[];
  missingRequiredSkills: string[];
  preferredSkillGaps: string[];
  riskFactors: string[];
  recommendation: string;
}

export interface RankedMatch {
  posting: JobPosting;
  match: MatchResult;
}

@Injectable({ providedIn: 'root' })
export class MatchService {
  private api = inject(ApiService);

  top(limit = 20, scanSize = 200, maxAgeDays?: number) {
    const params: any = { limit, scanSize };
    if (maxAgeDays != null) params['maxAgeDays'] = maxAgeDays;
    return this.api.get<RankedMatch[]>('/api/match/top', params);
  }
  posting(id: string) { return this.api.get<RankedMatch>(`/api/match/posting/${id}`); }
  rescore(max = 1000) { return this.api.post<{ rescored: number }>(`/api/match/rescore?max=${max}`, {}); }
}

