import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';

export interface CompanyRoleRef { id: string; title: string; location?: string; matchScore?: number; route: string; }
export interface CompanyAppRef { id: string; title?: string; status?: string; appliedAt?: string; }
export interface CompanyOverview {
  company: string;
  openRoles: number; applications: number; interviews: number; saved: number;
  roles: CompanyRoleRef[];
  apps: CompanyAppRef[];
}

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private api = inject(ApiService);
  overview(name: string) { return this.api.get<CompanyOverview>('/api/companies/overview', { name }); }
}

