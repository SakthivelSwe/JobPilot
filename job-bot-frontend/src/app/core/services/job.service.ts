import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Job, Page, AtsResult } from '../models';

@Injectable({ providedIn: 'root' })
export class JobService {
  private api = inject(ApiService);

  list(params: { status?: string; platform?: string; page?: number; size?: number } = {}): Observable<Page<Job>> {
    return this.api.get<Page<Job>>('/api/jobs', params);
  }
  get(id: string): Observable<Job> {
    return this.api.get<Job>(`/api/jobs/${id}`);
  }
  import(body: Partial<Job> & { criteriaId?: string }): Observable<Job> {
    return this.api.post<Job>('/api/jobs/import', body);
  }
  score(id: string, body: { criteriaId?: string; resumeId?: string }): Observable<Job> {
    return this.api.post<Job>(`/api/jobs/${id}/score`, body);
  }
  updateStatus(id: string, status: string): Observable<Job> {
    return this.api.put<Job>(`/api/jobs/${id}/status`, { status });
  }
  remove(id: string): Observable<void> {
    return this.api.delete<void>(`/api/jobs/${id}`);
  }
  analyze(resumeId: string | undefined, jobDescription: string): Observable<AtsResult> {
    return this.api.post<AtsResult>('/api/ats/analyze', { resumeId, jobDescription });
  }
}

