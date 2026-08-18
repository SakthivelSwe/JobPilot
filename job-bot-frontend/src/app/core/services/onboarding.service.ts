import { Injectable, computed, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { CandidateService } from './candidate.service';
import { TargetRoleService } from './target-role.service';
import { ResumeService } from './resume.service';
import { CriteriaService } from './criteria.service';
import { ApplicationService } from './application.service';
import { DiscoveryService } from './discovery.service';

const WELCOME_KEY = 'jobpilot.welcome.seen';

export interface SetupStep {
  key: string;
  label: string;
  done: boolean;
  hint: string;
  route: string;
}

/**
 * Central onboarding state. Checks real backend to compute setup progress
 * for the "Getting Started" checklist. Nothing is fabricated.
 */
@Injectable({ providedIn: 'root' })
export class OnboardingService {
  private candidate = inject(CandidateService);
  private targetRoles = inject(TargetRoleService);
  private resumes = inject(ResumeService);
  private criteria = inject(CriteriaService);
  private apps = inject(ApplicationService);
  private discovery = inject(DiscoveryService);

  readonly loaded = signal(false);
  readonly welcomeOpen = signal(false);

  // Individual step signals — filled from real API responses
  readonly hasProfile = signal(false);
  readonly hasTargetRoles = signal(false);
  readonly hasResume = signal(false);
  readonly hasCriteria = signal(false);
  readonly hasScanned = signal(false);
  readonly hasApplied = signal(false);

  /** Ordered checklist. Each item points to a real page. */
  readonly steps = computed<SetupStep[]>(() => [
    { key: 'profile',      label: 'Build your profile',       done: this.hasProfile(),
      hint: 'Upload a résumé or fill your details so JobPilot knows what to match.', route: '/profile' },
    { key: 'target-roles', label: 'Add a target role',         done: this.hasTargetRoles(),
      hint: 'Tell JobPilot what job titles to search for (e.g. Java Backend Developer).', route: '/target-roles' },
    { key: 'resume',       label: 'Add at least one résumé',   done: this.hasResume(),
      hint: 'Résumés are matched against every job to compute a fit score.', route: '/resumes' },
    { key: 'criteria',     label: 'Set search criteria',       done: this.hasCriteria(),
      hint: 'Locations, experience range, minimum match score, and keywords.', route: '/criteria' },
    { key: 'scan',         label: 'Run your first job scan',   done: this.hasScanned(),
      hint: 'Pulls jobs from Naukri, LinkedIn and Indeed and ranks them for you.', route: '/discovery' },
    { key: 'apply',        label: 'Send your first application', done: this.hasApplied(),
      hint: 'Review a match and approve, or mark a manual application done.', route: '/queue' },
  ]);

  readonly doneCount = computed(() => this.steps().filter(s => s.done).length);
  readonly totalSteps = computed(() => this.steps().length);
  readonly progressPct = computed(() =>
    this.totalSteps() ? Math.round((this.doneCount() / this.totalSteps()) * 100) : 0);
  readonly nextStep = computed<SetupStep | null>(() => this.steps().find(s => !s.done) ?? null);
  readonly allDone = computed(() => this.doneCount() === this.totalSteps());

  /** Refresh all setup checks from the backend. Silent on errors. */
  refresh(): void {
    forkJoin({
      profile: this.candidate.getProfile(),
      roles: this.targetRoles.list(),
      resumes: this.resumes.list(),
      criteria: this.criteria.list(),
      stats: this.apps.stats(),
      coverage: this.discovery.coverage(),
    }).subscribe({
      next: r => {
        this.hasProfile.set(!!r.profile && !!(r.profile.name || r.profile.email));
        this.hasTargetRoles.set((r.roles || []).some(x => x.active));
        this.hasResume.set((r.resumes || []).some(x => x.active));
        this.hasCriteria.set((r.criteria || []).some(x => x.active));
        this.hasScanned.set((r.coverage?.postingsTotal ?? 0) > 0);
        this.hasApplied.set((r.stats?.totalApplied ?? 0) > 0);
        this.loaded.set(true);
        this.maybeShowWelcome();
      },
      error: () => this.loaded.set(true),
    });
  }

  /** Show the welcome modal exactly once, for brand-new users. */
  private maybeShowWelcome(): void {
    if (localStorage.getItem(WELCOME_KEY) === '1') return;
    // Only auto-show if the user actually has nothing set up yet.
    if (this.doneCount() === 0) this.welcomeOpen.set(true);
  }

  openWelcome(): void { this.welcomeOpen.set(true); }
  closeWelcome(remember = true): void {
    this.welcomeOpen.set(false);
    if (remember) localStorage.setItem(WELCOME_KEY, '1');
  }

  /** Manually mark a step as re-checked after user action. */
  recheck(): void { this.refresh(); }
}

