import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';

export interface VariantScore {
  variant: string;
  roleTarget: string;
  score: number;
  matchedPrioritySkills: string[];
  recommended: boolean;
}

export interface TailoredResume {
  profileId: string;
  variant: string;
  roleTarget: string;
  title: string;
  summary: string;
  orderedSkills: string[];
  projects: { name: string; technologies: string[] }[];
  experiences: { company: string; role: string; current: boolean; technologies: string[] }[];
  emphasizedKeywords: string[];
}

export interface VariantInfo {
  variant: string;
  roleTarget: string;
  prioritySkills: string[];
}

@Injectable({ providedIn: 'root' })
export class ResumeEngineService {
  private api = inject(ApiService);

  variants() { return this.api.get<VariantInfo[]>('/api/resume-engine/variants'); }
  select(postingId: string) { return this.api.get<VariantScore[]>(`/api/resume-engine/select/${postingId}`); }
  tailor(postingId: string, variant?: string) {
    return this.api.get<TailoredResume>(`/api/resume-engine/tailor/${postingId}`, variant ? { variant } : undefined);
  }
}

