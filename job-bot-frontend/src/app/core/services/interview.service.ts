import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';

export interface InterviewRef {
  applicationId: string;
  company?: string;
  role?: string;
  status?: string;
  scheduledAt?: string;
  round?: number;
  upcoming: boolean;
}

export interface ChecklistItem { label: string; done: boolean; }

export interface PrepPack {
  applicationId: string;
  company?: string;
  role?: string;
  scheduledAt?: string;
  round?: number;
  technicalTopics: string[];
  likelyQuestions: string[];
  behavioralQuestions: string[];
  questionsToAsk: string[];
  checklist: ChecklistItem[];
  note: string;
}

@Injectable({ providedIn: 'root' })
export class InterviewService {
  private api = inject(ApiService);
  list() { return this.api.get<InterviewRef[]>('/api/interviews'); }
  prep(applicationId: string) { return this.api.get<PrepPack>(`/api/interviews/${applicationId}/prep`); }
}

