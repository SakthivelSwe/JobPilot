import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { JobCriteria } from '../models';

export interface QueryValidation { valid: boolean; error?: string | null; matches?: boolean | null; }

@Injectable({ providedIn: 'root' })
export class CriteriaService {
  private api = inject(ApiService);

  list(): Observable<JobCriteria[]> {
    return this.api.get<JobCriteria[]>('/api/criteria');
  }
  get(id: string): Observable<JobCriteria> {
    return this.api.get<JobCriteria>(`/api/criteria/${id}`);
  }
  create(body: Partial<JobCriteria>): Observable<JobCriteria> {
    return this.api.post<JobCriteria>('/api/criteria', body);
  }
  update(id: string, body: Partial<JobCriteria>): Observable<JobCriteria> {
    return this.api.put<JobCriteria>(`/api/criteria/${id}`, body);
  }
  toggle(id: string): Observable<JobCriteria> {
    return this.api.patch<JobCriteria>(`/api/criteria/${id}/toggle`);
  }
  remove(id: string): Observable<void> {
    return this.api.delete<void>(`/api/criteria/${id}`);
  }
  /** Live-validate a boolean expression (never throws server-side). */
  validateQuery(query: string, sampleText?: string): Observable<QueryValidation> {
    return this.api.post<QueryValidation>('/api/criteria/validate-query', { query, sampleText });
  }
}


