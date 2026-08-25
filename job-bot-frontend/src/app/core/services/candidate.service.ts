import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models';
import { ApiService } from './api.service';

export interface DetectedSkill { name: string; category?: string; proficiency?: string; evidence?: string[]; }
export interface DetectedExperience {
  company?: string; role?: string; startDate?: string; endDate?: string;
  current?: boolean; technologies?: string[];
}
export interface ParsedResume {
  fileName: string; mimeType: string; size: number; checksum: string; storagePath: string;
  detectedName?: string; detectedEmail?: string; detectedPhone?: string; detectedSummary?: string;
  detectedSkills: DetectedSkill[];
  detectedExperience: DetectedExperience[];
  detectedEducation: any[];
  detectedProjects: string[];
  rawTextPreview?: string;
}

export interface CandidateProfile {
  id?: string;
  name?: string; email?: string; phone?: string; currentLocation?: string;
  preferredLocations?: string[]; preferredWorkModes?: string[];
  yearsOfExperience?: number; noticePeriodDays?: number; lastWorkingDate?: string;
  expectedSalary?: number; minimumSalary?: number;
  relocationPreference?: string; remotePreference?: string; workAuthorization?: string;
  summary?: string; targetRoles?: string[]; excludedRoles?: string[];
  preferredCompanies?: string[]; excludedCompanies?: string[];
  verified?: boolean; updatedAt?: string;
}

/** Payload for POST /api/candidate/profile/confirm */
export interface ConfirmProfile extends CandidateProfile {
  skills?: { name: string; category?: string; proficiency?: string; evidence?: string[] }[];
  storagePath?: string; fileName?: string; mimeType?: string; size?: number; checksum?: string;
  extractedText?: string;
}

@Injectable({ providedIn: 'root' })
export class CandidateService {
  private api = inject(ApiService);
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  /** Multipart résumé upload → unsaved parsed preview. */
  parse(file: File): Observable<ParsedResume> {
    const form = new FormData();
    form.append('file', file);
    return this.http
      .post<ApiResponse<ParsedResume>>(`${this.base}/api/candidate/resume/parse`, form)
      .pipe(map(r => r.data));
  }

  /** 204 → null (no profile yet). */
  getProfile(): Observable<CandidateProfile | null> {
    return this.http
      .get<ApiResponse<CandidateProfile>>(`${this.base}/api/candidate/profile`, { observe: 'response' })
      .pipe(map(res => (res.status === 204 || !res.body ? null : res.body.data)));
  }

  confirm(body: ConfirmProfile) { return this.api.post<CandidateProfile>('/api/candidate/profile/confirm', body); }
  update(body: ConfirmProfile) { return this.api.put<CandidateProfile>('/api/candidate/profile', body); }
  skills() { return this.api.get<any[]>('/api/candidate/skills'); }
  getResumeText() { return this.api.get<string>('/api/candidate/resume/text'); }
}

