import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { ToastComponent } from './shared/toast/toast.component';
import { CommandPaletteComponent } from './shared/command-palette/command-palette.component';
import { WelcomeModalComponent } from './shared/welcome-modal/welcome-modal.component';
import { GettingStartedComponent } from './shared/getting-started/getting-started.component';
import { AuthService } from './core/services/auth.service';
import { ManualQueueService } from './core/services/manual-queue.service';
import { JobQueueService } from './core/services/job-queue.service';
import { UiService } from './core/services/ui.service';
import { ConfigService } from './core/config/thresholds';
import { OnboardingService } from './core/services/onboarding.service';

interface NavItem { path: string; label: string; badge?: () => number; }
interface NavCluster { items: NavItem[]; }

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive,
            ToastComponent, CommandPaletteComponent, WelcomeModalComponent, GettingStartedComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  private auth = inject(AuthService);
  private manual = inject(ManualQueueService);
  private queue = inject(JobQueueService);
  private router = inject(Router);
  ui = inject(UiService);
  private config = inject(ConfigService);
  private onboarding = inject(OnboardingService);

  title = 'JobPilot';
  pendingReview = signal(0);
  manualCount = signal(0);
  isAuthed = this.auth.isAuthenticated;
  mobileOpen = signal(false);

  /** Nav grouped by the real workflow: Focus · Find & apply · Track · Library. */
  clusters: NavCluster[] = [
    { items: [{ path: '/dashboard', label: 'Today' }] },
    { items: [
      { path: '/discovery', label: 'Discover' },
      { path: '/queue', label: 'Review', badge: () => this.pendingReview() },
      { path: '/manual', label: 'Manual', badge: () => this.manualCount() },
    ]},
    { items: [{ path: '/applications', label: 'Pipeline' }] },
    { items: [{ path: '/interviews', label: 'Interviews' }] },
    { items: [
      { path: '/profile', label: 'Profile' },
      { path: '/target-roles', label: 'Roles' },
      { path: '/resumes', label: 'Résumés' },
      { path: '/criteria', label: 'Criteria' },
      { path: '/analytics', label: 'Insights' },
      { path: '/settings', label: 'Settings' },
    ]},
  ];

  ngOnInit(): void {
    if (this.isAuthed()) {
      this.refreshBadges();
      this.config.refresh();
      this.onboarding.refresh();
    }
    setInterval(() => { if (this.isAuthed()) this.refreshBadges(); }, 30_000);
  }

  refreshBadges(): void {
    this.manual.stats().subscribe({ next: s => this.manualCount.set(s.pending), error: () => {} });
    this.queue.stats().subscribe({ next: s => this.pendingReview.set(s['PENDING_REVIEW'] ?? 0), error: () => {} });
  }

  logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}

