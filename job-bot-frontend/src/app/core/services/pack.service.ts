import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { ResumeMatch } from '../models';

@Injectable({ providedIn: 'root' })
export class PackService {
  private api = inject(ApiService);

  bestResume(body: { jobId?: string; jobDescription?: string }): Observable<ResumeMatch[]> {
    return this.api.post<ResumeMatch[]>('/api/pack/best-resume', body);
  }

  coverLetter(jobId: string, resumeId: string): Observable<{ coverLetter: string }> {
    return this.api.post<{ coverLetter: string }>('/api/pack/cover-letter', { jobId, resumeId });
  }

  answers(jobId: string, resumeId: string): Observable<{ question: string; answer: string }[]> {
    return this.api.post<{ question: string; answer: string }[]>('/api/pack/answers', { jobId, resumeId });
  }
}

