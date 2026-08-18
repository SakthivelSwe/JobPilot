import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { JobService } from '../../core/services/job.service';
import { CriteriaService } from '../../core/services/criteria.service';
import { ApplicationService } from '../../core/services/application.service';
import { ToastService } from '../../core/services/toast.service';
import { Job, JobCriteria } from '../../core/models';

@Component({
  selector: 'app-job-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  template: `
    <div class="page-head row">
      <div>
        <div class="page-title">Jobs</div>
        <div class="page-sub">Imported jobs with deterministic ATS scores</div>
      </div>
      <div class="spacer"></div>
      <a class="btn" routerLink="/jobs/import">＋ Import Job</a>
    </div>

    <div class="card" style="margin-bottom:18px;">
      <div class="row">
        <input placeholder="🔍 Search title or company" [(ngModel)]="search" style="max-width:280px;padding:9px 12px;border:1px solid #e2e8f0;border-radius:10px;" />
        <select [(ngModel)]="status" (change)="load()" style="padding:9px 12px;border:1px solid #e2e8f0;border-radius:10px;">
          <option value="">All statuses</option>
          <option>new</option><option>matched</option><option>applied</option><option>skipped</option>
        </select>
        <div class="spacer"></div>
        <span class="muted" style="font-size:13px;">{{ filtered().length }} shown</span>
      </div>
    </div>

    <div class="card" *ngIf="filtered().length; else empty" style="padding:6px 0;">
      <table class="data">
        <thead>
          <tr><th style="padding-left:20px;">Role</th><th>Company</th><th>Location</th><th>Platform</th><th>Score</th><th>Status</th><th></th></tr>
        </thead>
        <tbody>
          <tr *ngFor="let j of filtered()">
            <td style="padding-left:20px;">
              <strong>{{ j.title }}</strong>
              <div *ngIf="j.reasonToApply" class="muted reason">{{ firstLine(j.reasonToApply) }}</div>
            </td>
            <td>{{ j.company }}</td>
            <td>{{ j.location }}</td>
            <td><span class="chip gray">{{ j.platform }}</span></td>
            <td>
              <div *ngIf="j.matchScore != null" class="ring" [style.background]="ringBg(j.matchScore)">
                <span [style.color]="scoreColor(j.matchScore)">{{ j.matchScore }}</span>
              </div>
              <span *ngIf="j.matchScore == null" class="muted">—</span>
            </td>
            <td><span class="chip" [ngClass]="statusClass(j.status)">{{ j.status }}</span></td>
            <td>
              <div class="row">
                <select class="mini" (change)="score(j, $any($event.target).value); $any($event.target).value=''">
                  <option value="">Score…</option>
                  <option *ngFor="let c of criteria" [value]="c.id">{{ c.name }}</option>
                </select>
                <button class="btn small" (click)="recordApplied(j)">Applied ✓</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <ng-template #empty><div class="card"><div class="empty"><span class="big">💼</span>No jobs yet. Import one to get started.</div></div></ng-template>
  `,
  styles: [`
    .reason { font-size:11px; max-width:280px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; margin-top:2px; }
    .mini { padding:6px 8px; border:1px solid #e2e8f0; border-radius:8px; font-size:12px; }
  `]
})
export class JobListComponent implements OnInit {
  private jobService = inject(JobService);
  private criteriaService = inject(CriteriaService);
  private appService = inject(ApplicationService);
  private toast = inject(ToastService);

  jobs: Job[] = [];
  criteria: JobCriteria[] = [];
  status = '';
  search = '';

  ngOnInit(): void {
    this.criteriaService.list().subscribe(c => (this.criteria = c));
    this.load();
  }

  load(): void {
    this.jobService.list({ status: this.status, size: 100 }).subscribe(p => (this.jobs = p.content || []));
  }

  filtered(): Job[] {
    const q = this.search.toLowerCase().trim();
    if (!q) return this.jobs;
    return this.jobs.filter(j =>
      (j.title || '').toLowerCase().includes(q) || (j.company || '').toLowerCase().includes(q));
  }

  score(job: Job, criteriaId: string): void {
    if (!criteriaId) return;
    this.jobService.score(job.id, { criteriaId }).subscribe({
      next: j => { this.toast.success(`Scored ${j.matchScore}% · ${j.status}`); this.load(); },
      error: () => this.toast.error('Scoring failed')
    });
  }

  recordApplied(job: Job): void {
    this.appService.create({ jobId: job.id, criteriaId: job.criteriaId }).subscribe({
      next: () => { this.toast.success('Marked as applied'); this.load(); },
      error: e => this.toast.error(e?.error?.message || 'Failed')
    });
  }

  firstLine(text: string): string { return (text || '').split('\n')[0]; }
  scoreColor(v: number): string { return v >= 80 ? '#15803d' : v >= 60 ? '#a16207' : '#b91c1c'; }
  ringBg(v: number): string {
    const c = this.scoreColor(v);
    return `conic-gradient(${c} ${v * 3.6}deg, #f1f5f9 0deg)`;
  }
  statusClass(s: string): string {
    return s === 'applied' ? 'green' : s === 'matched' ? 'yellow' : s === 'skipped' || s === 'error' ? 'red' : 'gray';
  }
}


