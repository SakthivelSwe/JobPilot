import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { OnboardingService } from '../../core/services/onboarding.service';

interface Slide {
  kicker: string;
  title: string;
  body: string;
  bullets?: string[];
}

@Component({
  selector: 'jp-welcome-modal',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="w-overlay" *ngIf="onb.welcomeOpen()" (click)="skip()">
      <div class="w-card" (click)="$event.stopPropagation()" role="dialog" aria-modal="true" aria-labelledby="w-title">
        <div class="w-progress">
          <span class="dot" *ngFor="let s of slides; let i = index" [class.on]="i === index()"></span>
        </div>

        <ng-container *ngIf="current() as s">
          <div class="kicker">{{ s.kicker }}</div>
          <h2 id="w-title" class="w-title">{{ s.title }}</h2>
          <p class="w-body">{{ s.body }}</p>
          <ul class="w-bullets" *ngIf="s.bullets?.length">
            <li *ngFor="let b of s.bullets">{{ b }}</li>
          </ul>
        </ng-container>

        <div class="w-actions">
          <button class="btn ghost small" (click)="skip()">Skip tour</button>
          <div class="spacer"></div>
          <button class="btn secondary small" *ngIf="index() > 0" (click)="prev()">← Back</button>
          <button class="btn small" *ngIf="!isLast()" (click)="next()">Next →</button>
          <a class="btn small" *ngIf="isLast()" routerLink="/profile" (click)="finish()">
            Let's begin →
          </a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .w-overlay { position:fixed; inset:0; z-index:1200; background:rgba(32,30,27,0.4);
      display:flex; align-items:center; justify-content:center; padding:20px; animation:wfade .16s ease; }
    .w-card { width:520px; max-width:100%; background:var(--surface); border:1px solid var(--line-strong);
      border-radius:var(--radius); padding:28px 30px 22px; box-shadow:0 20px 60px rgba(32,30,27,0.24);
      animation:wpop .16s ease; }
    .w-progress { display:flex; gap:6px; margin-bottom:18px; }
    .dot { width:24px; height:3px; border-radius:2px; background:var(--line); }
    .dot.on { background:var(--accent); }
    .kicker { font-size:11.5px; font-weight:700; letter-spacing:0.09em; text-transform:uppercase; color:var(--ink-3); }
    .w-title { font-family:var(--font-display); font-size:24px; font-weight:600; letter-spacing:-0.01em;
      color:var(--ink); margin:4px 0 12px; }
    .w-body { color:var(--ink-2); font-size:14.5px; line-height:1.55; margin:0; }
    .w-bullets { margin:14px 0 4px; padding:0; list-style:none; }
    .w-bullets li { padding:6px 0 6px 22px; font-size:14px; color:var(--ink); position:relative; }
    .w-bullets li::before { content:'✓'; position:absolute; left:0; color:var(--success); font-weight:700; }
    .w-actions { display:flex; align-items:center; gap:8px; margin-top:22px; padding-top:16px;
      border-top:1px solid var(--line); }
    @keyframes wfade { from { opacity:0; } to { opacity:1; } }
    @keyframes wpop { from { transform:translateY(6px); opacity:0; } to { transform:none; opacity:1; } }
  `]
})
export class WelcomeModalComponent {
  onb = inject(OnboardingService);
  private router = inject(Router);
  index = signal(0);

  slides: Slide[] = [
    {
      kicker: 'Welcome',
      title: 'JobPilot is your career-search assistant.',
      body: 'Add your résumé, tell it what jobs you want, and JobPilot finds matching roles on Naukri, LinkedIn and Indeed, scores each one, and helps you apply.',
      bullets: [
        'Deterministic match scores — no guessing',
        'Nothing is submitted without your approval',
        'Your data stays with you — export any time',
      ],
    },
    {
      kicker: 'Step 1 · Set up (5 minutes)',
      title: 'Tell JobPilot who you are and what you want.',
      body: 'Six quick steps get you a running search. The checklist at the top of the app shows your progress.',
      bullets: [
        'Profile — upload a résumé or fill your details',
        'Target roles — job titles you want (e.g. Java Backend)',
        'Résumés & Criteria — versions and search rules',
      ],
    },
    {
      kicker: 'Step 2 · Discover & review',
      title: 'Find matches and decide what to apply to.',
      body: 'Click "Scan jobs" and JobPilot pulls postings from public sources and ranks them 0–100 against your profile.',
      bullets: [
        'Review queue — approve, skip, or send to Manual',
        'Match score above 80 = strong fit',
        'Every decision is yours',
      ],
    },
    {
      kicker: 'Step 3 · Apply & track',
      title: 'Auto or manual — you stay in control.',
      body: 'Naukri and Indeed can be submitted by a local companion engine. LinkedIn always uses your own browser via the Chrome extension. Anything the engine can\'t handle goes to Manual for you to submit.',
      bullets: [
        'Pipeline tracks Applied → Interview → Offer',
        'Interview center builds prep packs from job skills',
        'Insights show which résumé is converting best',
      ],
    },
  ];

  current(): Slide { return this.slides[this.index()]; }
  isLast(): boolean { return this.index() === this.slides.length - 1; }

  next(): void { if (!this.isLast()) this.index.set(this.index() + 1); }
  prev(): void { if (this.index() > 0) this.index.set(this.index() - 1); }
  skip(): void { this.onb.closeWelcome(true); this.index.set(0); }
  finish(): void { this.onb.closeWelcome(true); this.index.set(0); }
}

