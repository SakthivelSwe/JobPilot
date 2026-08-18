import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Application, DashboardStats, ResumePerformance } from '../models';

@Injectable({ providedIn: 'root' })
export class ApplicationService {
  private api = inject(ApiService);

  kanban(): Observable<Record<string, Application[]>> {
    return this.api.get<Record<string, Application[]>>('/api/applications/kanban');
  }
  create(body: { jobId: string; resumeId?: string; criteriaId?: string; notes?: string }): Observable<Application> {
    return this.api.post<Application>('/api/applications', body);
  }
  updateStatus(id: string, status: string, notes?: string): Observable<Application> {
    return this.api.put<Application>(`/api/applications/${id}/status`, { status, notes });
  }
  setInterview(id: string, interviewDate: string, interviewRound: number): Observable<Application> {
    return this.api.put<Application>(`/api/applications/${id}/interview`, { interviewDate, interviewRound });
  }
  remove(id: string): Observable<void> {
    return this.api.delete<void>(`/api/applications/${id}`);
  }
  stats(): Observable<DashboardStats> {
    return this.api.get<DashboardStats>('/api/dashboard/stats');
  }
  resumePerformance(): Observable<ResumePerformance[]> {
    return this.api.get<ResumePerformance[]>('/api/dashboard/resume-performance');
  }
}

