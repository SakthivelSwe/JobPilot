import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';

export interface TargetRole {
  id?: string;
  roleTitle: string;
  priority: number;
  requiredSkills: string[];
  preferredSkills: string[];
  excludedSkills: string[];
  minimumExperience?: number | null;
  maximumExperience?: number | null;
  locations: string[];
  remotePreference: 'ANY' | 'REMOTE' | 'HYBRID' | 'ONSITE' | string;
  salaryMinLpa?: number | null;
  salaryMaxLpa?: number | null;
  noticePeriodToleranceDays?: number | null;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class TargetRoleService {
  private api = inject(ApiService);

  list() { return this.api.get<TargetRole[]>('/api/target-roles'); }
  get(id: string) { return this.api.get<TargetRole>(`/api/target-roles/${id}`); }
  create(body: Partial<TargetRole>) { return this.api.post<TargetRole>('/api/target-roles', body); }
  update(id: string, body: Partial<TargetRole>) { return this.api.put<TargetRole>(`/api/target-roles/${id}`, body); }
  remove(id: string) { return this.api.delete<void>(`/api/target-roles/${id}`); }
  /** Body: { "<roleId>": priority, ... } */
  reorder(priorities: Record<string, number>) { return this.api.post<TargetRole[]>('/api/target-roles/reorder', priorities); }
}


