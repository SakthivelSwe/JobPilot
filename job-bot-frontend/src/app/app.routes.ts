import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login.component').then(m => m.LoginPageComponent)
  },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'resumes',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/resumes/resume-list.component').then(m => m.ResumeListComponent)
  },
  {
    path: 'resumes/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/resumes/resume-form.component').then(m => m.ResumeFormComponent)
  },
  {
    path: 'resumes/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/resumes/resume-form.component').then(m => m.ResumeFormComponent)
  },
  {
    path: 'criteria',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/criteria/criteria-list.component').then(m => m.CriteriaListComponent)
  },
  {
    path: 'criteria/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/criteria/criteria-form.component').then(m => m.CriteriaFormComponent)
  },
  {
    path: 'criteria/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/criteria/criteria-form.component').then(m => m.CriteriaFormComponent)
  },
  {
    path: 'jobs',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/jobs/job-list.component').then(m => m.JobListComponent)
  },
  {
    path: 'jobs/import',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/jobs/job-import.component').then(m => m.JobImportComponent)
  },
  {
    path: 'jobs/posting/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/job-detail/job-detail.component').then(m => m.JobDetailComponent)
  },
  {
    path: 'applications',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/applications/kanban.component').then(m => m.KanbanComponent)
  },
  // ============ v2 additions ============
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/profile/profile.component').then(m => m.ProfilePageComponent)
  },
  {
    path: 'target-roles',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/target-roles/target-roles.component').then(m => m.TargetRolesPageComponent)
  },
  {
    path: 'companies/:name',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/company/company.component').then(m => m.CompanyPageComponent)
  },
  {
    path: 'interviews',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/interviews/interviews.component').then(m => m.InterviewsPageComponent)
  },
  {
    path: 'discovery',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/discovery/discovery.component').then(m => m.DiscoveryPageComponent)
  },
  {
    path: 'queue',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/queue/queue.component').then(m => m.QueuePageComponent)
  },
  {
    path: 'analytics',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/analytics/analytics.component').then(m => m.AnalyticsPageComponent)
  },
  {
    path: 'manual',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/manual-queue/manual-queue.component').then(m => m.ManualQueuePageComponent)
  },
  {
    path: 'settings',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/settings/settings.component').then(m => m.SettingsPageComponent)
  },
  {
    path: 'help',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/help/help.component').then(m => m.HelpPageComponent)
  },
  { path: '**', redirectTo: 'dashboard' }
];
