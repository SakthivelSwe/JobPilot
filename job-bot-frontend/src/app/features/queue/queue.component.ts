import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { JobQueueEntry, JobQueueService } from '../../core/services/job-queue.service';
import { ToastService } from '../../core/services/toast.service';

type Tab = 'pending' | 'auto';

@Component({
  selector: 'queue-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page-head row">
      <div>
        <div class="page-title">Job Queue</div>
        <div class="page-sub">
          {{ stats()?.['PENDING_REVIEW'] ?? 0 }} awaiting review ·
          {{ (stats()?.['APPROVED'] ?? 0) + (stats()?.['AUTO_APPLYING'] ?? 0) }} in-flight ·
          {{ stats()?.['APPLIED'] ?? 0 }} applied
        </div>
      </div>
      <div class="spacer"></div>
      <button class="btn secondary small" (click)="load()">Refresh</button>
      <button class="btn small" *ngIf="tab() === 'pending' && pending().length"
              (click)="approveAll()">Approve all 80+</button>
    </div>

    <div class="tab-strip">
      <button [class.active]="tab() === 'pending'" (click)="setTab('pending')">
        Pending Review
        <span class="chip gray" style="margin-left:6px">{{ stats()?.['PENDING_REVIEW'] ?? 0 }}</span>
      </button>
      <button [class.active]="tab() === 'auto'" (click)="setTab('auto')">
        Auto-Applying
        <span class="chip gray" style="margin-left:6px">
          {{ (stats()?.['APPROVED'] ?? 0) + (stats()?.['AUTO_APPLYING'] ?? 0) }}
        </span>
      </button>
    </div>

    <ng-container *ngIf="tab() === 'pending'">
      <div class="empty" *ngIf="!pending().length">
        <span class="big">✨</span>
        No jobs waiting review.<br />
        <a routerLink="/discovery">Run a discovery scan</a> to find new opportunities.
      </div>

      <div class="grid" style="grid-template-columns: 1fr; gap: 14px;">
        <div *ngFor="let j of pending()" class="card queue-card"
             [class.glow-strong]="(j.matchScore ?? 0) >= 85">
          <div class="row" style="align-items:flex-start; gap:18px;">
            <div class="score-ring"
                 [style.--pct]="j.matchScore ?? 0"
                 [style.--size]="'72px'">
              <span class="num">{{ j.matchScore ?? '-' }}</span>
            </div>
            <div style="flex:1; min-width:0;">
              <div class="row" style="gap:8px; margin-bottom:4px;">
                <span class="pf-badge" [class]="'pf-badge ' + pfClass(j.platform)">{{ j.platform }}</span>
                <span class="chip green" *ngIf="(j.matchScore ?? 0) >= 85">STRONG MATCH</span>
                <span class="chip blue" *ngIf="j.recommendation">{{ j.recommendation }}</span>
                <span class="chip gray" *ngIf="j.resumeVariant">Resume: {{ j.resumeVariant }}</span>
              </div>
              <div style="font-size:16px; font-weight:700; color:var(--t1);">{{ j.title }}</div>
              <div class="muted" style="font-size:13px; margin-top:2px;">
                {{ j.company }}<span *ngIf="j.location"> · {{ j.location }}</span>
              </div>
              <div style="margin-top:10px;">
                <span class="chip green" *ngFor="let k of (j.matchedKeywords || []).slice(0, 6)">✓ {{ k }}</span>
                <span class="chip red" *ngFor="let k of (j.missingKeywords || []).slice(0, 4)">! {{ k }}</span>
              </div>
            </div>
            <div style="display:flex; flex-direction:column; gap:8px; min-width:150px;">
              <button class="btn success small" (click)="approve(j)">✓ Approve</button>
              <button class="btn secondary small" (click)="manual(j)">Manual</button>
              <button class="btn secondary small" (click)="skip(j)">Skip</button>
              <a class="btn secondary small" [href]="j.jobUrl" target="_blank" rel="noopener">Open ↗</a>
            </div>
          </div>
        </div>
      </div>
    </ng-container>

    <ng-container *ngIf="tab() === 'auto'">
      <div class="empty" *ngIf="!auto().length">
        <span class="big">💤</span>
        No jobs currently applying.<br />
        Approve jobs on the <b>Pending Review</b> tab and the local application-engine
        (or Chrome extension for LinkedIn) will pick them up.
      </div>

      <table class="data card" style="padding: 0" *ngIf="auto().length">
        <thead>
          <tr>
            <th>Status</th><th>Job</th><th>Company</th><th>Platform</th>
            <th>Match</th><th></th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let j of auto()">
            <td>
              <span [ngSwitch]="j.status">
                <span *ngSwitchCase="'APPROVED'" class="chip yellow">
                  <span class="pulse-dot"></span> Queued
                </span>
                <span *ngSwitchCase="'AUTO_APPLYING'" class="chip blue">
                  <span class="pulse-dot"></span> Applying
                </span>
                <span *ngSwitchCase="'APPLIED'" class="chip green">Applied</span>
                <span *ngSwitchDefault class="chip gray">{{ j.status }}</span>
              </span>
            </td>
            <td style="font-weight:600">{{ j.title }}</td>
            <td>{{ j.company }}</td>
            <td><span class="pf-badge" [class]="'pf-badge ' + pfClass(j.platform)">{{ j.platform }}</span></td>
            <td class="mono">{{ j.matchScore ?? '-' }}</td>
            <td>
              <button *ngIf="j.status === 'FAILED_APPLY'" class="btn secondary small"
                      (click)="manual(j)">Send to Manual</button>
            </td>
          </tr>
        </tbody>
      </table>
    </ng-container>

    <div class="pagination" *ngIf="totalPages() > 1">
      <button class="btn secondary small" [disabled]="page() === 0" (click)="prevPage()">Previous</button>
      <span class="muted" style="font-size:14px;">Page {{ page() + 1 }} of {{ totalPages() }} ({{ totalElements() }} items)</span>
      <button class="btn secondary small" [disabled]="page() >= totalPages() - 1" (click)="nextPage()">Next</button>
    </div>
  `,
  styles: [`
    .queue-card { padding: 16px 20px; }
    .pulse-dot {
      display: inline-block; width: 6px; height: 6px; border-radius: 50%;
      background: currentColor; margin-right: 6px;
      animation: pulse-dot 1.6s ease-in-out infinite;
    }
    @keyframes pulse-dot {
      0%, 100% { opacity: 1; } 50% { opacity: 0.3; }
    }
    .pagination { display: flex; justify-content: center; align-items: center; gap: 16px; margin-top: 24px; padding: 16px 0; border-top: 1px solid var(--line); }
  `],
})
export class QueuePageComponent implements OnInit {
  private queue = inject(JobQueueService);
  private toast = inject(ToastService);

  tab = signal<Tab>('pending');
  pending = signal<JobQueueEntry[]>([]);
  auto = signal<JobQueueEntry[]>([]);
  stats = signal<Record<string, number> | null>(null);

  page = signal(0);
  pageSize = signal(50);
  totalPages = signal(0);
  totalElements = signal(0);

  ngOnInit(): void { this.load(); }

  setTab(t: Tab) {
    this.tab.set(t);
    this.page.set(0);
    this.load();
  }

  load(): void {
    this.queue.stats().subscribe(s => this.stats.set(s as any));
    if (this.tab() === 'pending') {
      this.queue.pending(this.page(), this.pageSize()).subscribe(p => {
        this.pending.set(p.content);
        this.totalPages.set(p.totalPages);
        this.totalElements.set(p.totalElements);
      });
    } else {
      this.queue.autoApplying(this.page(), this.pageSize()).subscribe(p => {
        this.auto.set(p.content);
        this.totalPages.set(p.totalPages);
        this.totalElements.set(p.totalElements);
      });
    }
  }

  nextPage() {
    if (this.page() < this.totalPages() - 1) {
      this.page.set(this.page() + 1);
      this.load();
      window.scrollTo(0, 0);
    }
  }

  prevPage() {
    if (this.page() > 0) {
      this.page.set(this.page() - 1);
      this.load();
      window.scrollTo(0, 0);
    }
  }

  approve(j: JobQueueEntry) {
    this.queue.approve(j.id).subscribe({
      next: () => { this.toast.show('Approved — the engine will pick it up.', 'success'); this.load(); },
      error: () => this.toast.show('Approve failed', 'error'),
    });
  }
  skip(j: JobQueueEntry) {
    this.queue.skip(j.id).subscribe({
      next: () => { this.toast.show('Skipped', 'info'); this.load(); },
    });
  }
  manual(j: JobQueueEntry) {
    this.queue.sendToManual(j.id).subscribe({
      next: () => { this.toast.show('Moved to Manual queue', 'info'); this.load(); },
    });
  }
  approveAll() {
    this.queue.approveAllAbove(80).subscribe({
      next: n => { this.toast.show(`Approved ${n} jobs above 80% match`, 'success'); this.load(); },
    });
  }

  pfClass(p: string): string {
    const l = (p || '').toLowerCase();
    if (l === 'naukri' || l === 'linkedin' || l === 'indeed') return l;
    return 'other';
  }
}


