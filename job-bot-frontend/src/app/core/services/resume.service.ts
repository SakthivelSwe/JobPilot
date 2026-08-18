import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Resume } from '../models';

@Injectable({ providedIn: 'root' })
export class ResumeService {
  private api = inject(ApiService);

  list(): Observable<Resume[]> {
    return this.api.get<Resume[]>('/api/resumes');
  }
  get(id: string): Observable<Resume> {
    return this.api.get<Resume>(`/api/resumes/${id}`);
  }
  create(body: Partial<Resume>): Observable<Resume> {
    return this.api.post<Resume>('/api/resumes', body);
  }
  update(id: string, body: Partial<Resume>): Observable<Resume> {
    return this.api.put<Resume>(`/api/resumes/${id}`, body);
  }
  remove(id: string): Observable<void> {
    return this.api.delete<void>(`/api/resumes/${id}`);
  }
}

