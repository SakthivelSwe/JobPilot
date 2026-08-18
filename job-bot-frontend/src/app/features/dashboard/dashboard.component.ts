import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApplicationService } from '../../core/services/application.service';
import { AnalyticsService, AnalyticsOverview, LearningResult, Momentum } from '../../core/services/analytics.service';
import { JobQueueService } from '../../core/services/job-queue.service';
import { ActivityService, ActivityEvent } from '../../core/services/activity.service';
import { Application, DashboardStats, ResumePerformance } from '../../core/models';

interface AgendaItem {
  tone: 'accent' | 'signal' | 'success' | 'info' | 'warning';
  title: string;
  meta: string;
  lead?: string;
  link: string;
  action: string;
}
interface Stage { name: string; count: number; }

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <!-- ============ Masthead ============ -->
    <header class="masthead">
      <div>
        <div class="kicker">{{ today }}</div>
        <h1 class="display">{{ greeting }}</h1>
        <p class="lede">{{ statusLine() }}</p>
      </div>
      <div class="masthead-actions">
        <a class="btn" routerLink="/discovery">Scan for jobs</a>
        <a class="btn secondary" routerLink="/jobs/import">Import a job</a>
      </div>
    </header>

    <!-- ============ Needs attention ============ -->
    <section>
      <div class="section-head">
        <h2>Needs your attention</h2>
        <span class="count" *ngIf="agenda().length">{{ agenda().length }} item{{ agenda().length === 1 ? '' : 's' }}</span>
      </div>

      <div class="panel" *ngIf="agenda().length; else allClear">
        <div class="agenda">
          <a *ngFor="let a of agenda()" class="agenda-row" [routerLink]="a.link">
            <span class="agenda-mark" [class]="'agenda-mark ' + a.tone"></span>
            <div class="agenda-body">
              <div class="agenda-title">{{ a.title }}</div>
              <div class="agenda-meta">{{ a.meta }}</div>
            </div>
            <span class="agenda-lead" *ngIf="a.lead">{{ a.lead }}</span>
            <span class="btn ghost small">{{ a.action }} →</span>
          </a>
        </div>
      </div>

      <ng-template #allClear>
        <div class="panel">
          <div class="empty">
            <span class="big">✦</span>
            You're all caught up. Run a scan to surface fresh opportunities.
          </div>
        </div>
      </ng-template>
    </section>

    <!-- ============ Pipeline rail ============ -->
    <section>
      <div class="section-head">
        <h2>Your pipeline</h2>
        <span class="count">career movement, left to right</span>
      </div>
      <div class="stage-rail">
        <div *ngFor="let st of stages()" class="stage" [class.is-active]="st.count > 0 && st.name === activeStage()">
          <div class="stage-name">{{ st.name }}</div>
          <div class="stage-count">{{ st.count }}</div>
        </div>
      </div>
      <div class="rail-foot">
        <a routerLink="/applications">Open pipeline</a>
        <span class="sep">·</span>
        <a routerLink="/queue">Review queue</a>
      </div>
    </section>

    <!-- ============ Momentum + insight ============ -->
    <section>
      <div class="section-head">
        <h2>Momentum</h2>
        <span class="count">last 7 days</span>
      </div>

      <!-- Career momentum score (explainable) -->
      <div class="mom" *ngIf="momentum() as mo">
        <div class="mom-score" *ngIf="mo.available; else noMom">
          <div class="mom-ring" [style.--pct]="mo.score">
            <span class="mom-num numeric">{{ mo.score }}</span>
          </div>
          <div class="mom-body">
            <div class="mom-label">{{ mo.label }}</div>
            <div class="mom-msg">{{ mo.message }}</div>
            <div class="mom-factors">
              <span class="mom-f" *ngFor="let f of mo.factors">
                {{ f.name }} <b class="numeric">{{ f.value }}</b>
                <span class="mom-pts">+{{ f.points }}</span>
              </span>
            </div>
          </div>
        </div>
        <ng-template #noMom>
          <div class="mom-empty">
            <strong>{{ mo.label }}</strong> — {{ mo.message }}
          </div>
        </ng-template>
      </div>

      <div class="momentum" *ngIf="overview() as o">
        <div class="m"><span class="n">{{ o.jobsDiscovered }}</span><span class="l">discovered</span></div>
        <div class="m"><span class="n">{{ o.strongMatches }}</span><span class="l">strong matches</span></div>
        <div class="m"><span class="n">{{ stats()?.totalApplied ?? 0 }}</span><span class="l">applications</span></div>
        <div class="m"><span class="n">{{ stats()?.interviews ?? 0 }}</span><span class="l">interviews</span></div>
        <div class="m">
          <span class="n">{{ (stats()?.totalApplied ?? 0) > 0 ? o.interviewRate + '%' : '—' }}</span>
          <span class="l">{{ (stats()?.totalApplied ?? 0) > 0 ? 'interview rate' : 'no applications yet' }}</span>
        </div>
      </div>
      <p class="insight" *ngIf="insight()">{{ insight() }}</p>
    </section>

    <!-- ============ Recent activity ============ -->
    <section *ngIf="activity().length">
      <div class="section-head">
        <h2>Recent activity</h2>
        <span class="count">a truthful log — every entry is a real event</span>
      </div>
      <div class="timeline">
        <div class="tl-row" *ngFor="let a of activity()">
          <span class="tl-mark" [class]="'tl-mark ' + toneFor(a.type)"></span>
          <div class="tl-body">
            <div class="tl-title">{{ a.title }}</div>
            <div class="tl-detail" *ngIf="a.detail">{{ a.detail }}</div>
          </div>
          <span class="tl-time">{{ ago(a.createdAt) }}</span>
        </div>
      </div>
    </section>

    <!-- ============ Résumé performance ============ -->
    <section *ngIf="performance().length">
      <div class="section-head">
        <h2>Which résumé is working</h2>
        <span class="count">interview conversion by version</span>
      </div>
      <table class="data">
        <thead>
          <tr><th>Résumé</th><th class="num">Applications</th><th class="num">Interviews</th><th class="num">Offers</th><th>Interview rate</th></tr>
        </thead>
        <tbody>
          <tr *ngFor="let r of performance(); let i = index">
            <td>
              <span class="strong">{{ r.resumeName }}</span>
              <span *ngIf="i === 0 && r.interviewRate > 0" class="chip green" style="margin-left:8px;">Top performer</span>
            </td>
            <td class="num numeric">{{ r.applications }}</td>
            <td class="num numeric">{{ r.interviews }}</td>
            <td class="num numeric">{{ r.offers }}</td>
            <td>
              <div class="rate">
                <div class="rate-track"><div class="rate-fill" [style.width.%]="r.interviewRate"></div></div>
                <span class="numeric">{{ r.interviewRate }}%</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  `,
  styles: [`
    .masthead { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 8px; }
    .masthead .lede { color: var(--ink-2); font-size: 15px; margin: 8px 0 0; max-width: 60ch; }
    .masthead-actions { display: flex; gap: 10px; flex-shrink: 0; }
    .num { text-align: right; }
    .rail-foot { margin-top: 10px; font-size: 13px; color: var(--ink-3); }
    .rail-foot .sep { margin: 0 8px; }
    .insight {
      margin: 4px 0 0; padding: 14px 16px;
      background: var(--accent-wash); border-left: 3px solid var(--accent);
      border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
      color: var(--ink); font-size: 14.5px; max-width: 72ch;
    }
    .mom { margin-bottom: 16px; }
    .mom-score { display: flex; align-items: center; gap: 20px; padding: 16px 0; }
    .mom-ring { --pct: 0; --size: 76px; width: var(--size); height: var(--size); border-radius: 50%;
      display: flex; align-items: center; justify-content: center; position: relative; flex-shrink: 0;
      background: conic-gradient(var(--accent) calc(var(--pct) * 1%), var(--bg-tint) 0); }
    .mom-ring::before { content:''; position:absolute; inset:6px; border-radius:50%; background:var(--surface); }
    .mom-num { position: relative; font-size: 26px; font-weight: 600; color: var(--accent-deep); }
    .mom-body { flex: 1; }
    .mom-label { font-family: var(--font-display); font-size: 19px; font-weight: 600; color: var(--ink); }
    .mom-msg { color: var(--ink-2); font-size: 13.5px; margin-top: 2px; }
    .mom-factors { display: flex; flex-wrap: wrap; gap: 14px; margin-top: 10px; }
    .mom-f { font-size: 12.5px; color: var(--ink-2); }
    .mom-f b { color: var(--ink); margin: 0 2px; }
    .mom-pts { color: var(--success); font-weight: 600; margin-left: 3px; }
    .mom-empty { padding: 14px 16px; background: var(--surface-2); border: 1px solid var(--line);
      border-radius: var(--radius-sm); color: var(--ink-2); font-size: 13.5px; }
    .rate { display: flex; align-items: center; gap: 10px; }
    .rate-track { width: 90px; height: 6px; background: var(--bg-tint); border-radius: 999px; overflow: hidden; }
    .rate-fill { height: 100%; background: var(--success); border-radius: 999px; }
    .timeline { display: flex; flex-direction: column; }
    .tl-row { display: flex; align-items: flex-start; gap: 14px; padding: 12px 2px; border-bottom: 1px solid var(--line); }
    .tl-row:last-child { border-bottom: 0; }
    .tl-mark { width: 8px; height: 8px; border-radius: 50%; margin-top: 6px; background: var(--ink-3); flex-shrink: 0; }
    .tl-mark.discovery { background: var(--info); }
    .tl-mark.application { background: var(--accent); }
    .tl-mark.interview { background: var(--success); }
    .tl-mark.profile, .tl-mark.criteria { background: var(--warning); }
    .tl-body { flex: 1; min-width: 0; }
    .tl-title { font-weight: 600; color: var(--ink); font-size: 14px; }
    .tl-detail { color: var(--ink-3); font-size: 12.5px; margin-top: 1px; }
    .tl-time { color: var(--ink-3); font-size: 12px; font-family: var(--font-mono); white-space: nowrap; }
    @media (max-width: 720px) {
      .masthead { flex-direction: column; align-items: stretch; }
      .masthead-actions { flex-wrap: wrap; }
    }
  `]
})
export class DashboardComponent implements OnInit {
  private appService = inject(ApplicationService);
  private analytics = inject(AnalyticsService);
  private queue = inject(JobQueueService);
  private activityService = inject(ActivityService);

  stats = signal<DashboardStats | null>(null);
  overview = signal<AnalyticsOverview | null>(null);
  performance = signal<ResumePerformance[]>([]);
  learning = signal<LearningResult | null>(null);
  kanban = signal<Record<string, Application[]>>({});
  queueStats = signal<Record<string, number>>({});
  activity = signal<ActivityEvent[]>([]);
  momentum = signal<Momentum | null>(null);

  today = new Date().toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' });
  greeting = this.greetingFor(new Date().getHours());

  ngOnInit(): void {
    this.appService.stats().subscribe({ next: s => this.stats.set(s), error: () => {} });
    this.appService.resumePerformance().subscribe({ next: p => this.performance.set(p), error: () => {} });
    this.appService.kanban().subscribe({ next: k => this.kanban.set(k || {}), error: () => {} });
    this.analytics.overview().subscribe({ next: o => this.overview.set(o), error: () => {} });
    this.analytics.momentum().subscribe({ next: m => this.momentum.set(m), error: () => {} });
    this.analytics.learning().subscribe({ next: l => this.learning.set(l), error: () => {} });
    this.queue.stats().subscribe({ next: s => this.queueStats.set(s as any), error: () => {} });
    this.activityService.recent(12).subscribe({ next: a => this.activity.set(a || []), error: () => {} });
  }

  toneFor(type: string): string { return (type || '').toLowerCase(); }
  ago(iso: string): string {
    const s = Math.floor((Date.now() - new Date(iso).getTime()) / 1000);
    if (s < 60) return 'just now';
    if (s < 3600) return Math.floor(s / 60) + 'm ago';
    if (s < 86400) return Math.floor(s / 3600) + 'h ago';
    return Math.floor(s / 86400) + 'd ago';
  }

  // ---------- one-line status ----------
  statusLine = computed(() => {
    const o = this.overview();
    const pending = this.queueStats()['PENDING_REVIEW'] ?? 0;
    const parts: string[] = [];
    if (o?.strongMatches) parts.push(`${o.strongMatches} strong match${o.strongMatches === 1 ? '' : 'es'}`);
    if (pending) parts.push(`${pending} awaiting review`);
    const interviews = this.upcomingInterviews().length;
    if (interviews) parts.push(`${interviews} interview${interviews === 1 ? '' : 's'} ahead`);
    return parts.length ? `You have ${parts.join(' · ')}.` : 'A calm day — nothing urgent on your desk.';
  });

  // ---------- agenda ----------
  agenda = computed<AgendaItem[]>(() => {
    const items: AgendaItem[] = [];
    const upcoming = this.upcomingInterviews();
    for (const a of upcoming.slice(0, 3)) {
      items.push({
        tone: 'accent',
        title: `Interview — ${a.title || 'role'}${a.company ? ' at ' + a.company : ''}`,
        meta: a.interviewDate ? this.relativeDate(a.interviewDate) : 'Date to confirm',
        link: '/applications', action: 'Prepare',
      });
    }
    const pending = this.queueStats()['PENDING_REVIEW'] ?? 0;
    if (pending) items.push({
      tone: 'signal', title: 'Jobs waiting for your review',
      meta: 'Approve, skip or send to manual', lead: String(pending),
      link: '/queue', action: 'Review',
    });
    const strong = this.overview()?.strongMatches ?? 0;
    if (strong) items.push({
      tone: 'success', title: 'Strong matches to your criteria',
      meta: 'High-fit roles found in discovery', lead: String(strong),
      link: '/discovery', action: 'View',
    });
    const followUps = this.followUps().length;
    if (followUps) items.push({
      tone: 'warning', title: 'Applications ready for follow-up',
      meta: 'Applied 5+ days ago with no movement', lead: String(followUps),
      link: '/applications', action: 'Follow up',
    });
    const manual = this.queueStats()['MANUAL_APPLY'] ?? 0;
    if (manual) items.push({
      tone: 'info', title: 'Manual applications to finish',
      meta: 'LinkedIn and jobs the engine cannot auto-submit', lead: String(manual),
      link: '/manual', action: 'Open',
    });
    return items;
  });

  // ---------- pipeline stages ----------
  stages = computed<Stage[]>(() => {
    const k = this.kanban();
    const len = (key: string) => (k[key]?.length ?? 0);
    return [
      { name: 'Review', count: this.queueStats()['PENDING_REVIEW'] ?? 0 },
      { name: 'Applied', count: len('applied') },
      { name: 'Screening', count: len('viewed') + len('shortlisted') },
      { name: 'Interview', count: len('interview') },
      { name: 'Offer', count: len('offer') },
    ];
  });
  activeStage = computed(() => {
    const s = this.stages();
    // furthest-right non-zero stage = most advanced momentum
    for (let i = s.length - 1; i >= 0; i--) if (s[i].count > 0) return s[i].name;
    return '';
  });

  // ---------- insight ----------
  insight = computed<string>(() => {
    const rec = this.learning()?.recommendations?.[0];
    if (rec) return rec;
    const top = this.performance()[0];
    if (top && top.interviewRate > 0) {
      return `Your “${top.resumeName}” résumé is converting best right now — ${top.interviewRate}% of its applications reach an interview.`;
    }
    const o = this.overview();
    if (o && o.strongMatches > 0) {
      return `${o.strongMatches} roles strongly match your profile. Reviewing them first tends to yield the best response rate.`;
    }
    return '';
  });

  // ---------- helpers ----------
  private upcomingInterviews(): Application[] {
    const list = this.kanban()['interview'] || [];
    return list
      .filter(a => a.interviewDate)
      .sort((a, b) => new Date(a.interviewDate!).getTime() - new Date(b.interviewDate!).getTime());
  }
  private followUps(): Application[] {
    const applied = this.kanban()['applied'] || [];
    const cutoff = Date.now() - 5 * 24 * 3600 * 1000;
    return applied.filter(a => a.appliedAt && new Date(a.appliedAt).getTime() < cutoff);
  }
  private relativeDate(iso: string): string {
    const d = new Date(iso).getTime();
    const days = Math.round((d - Date.now()) / (24 * 3600 * 1000));
    if (days < 0) return `${Math.abs(days)}d ago`;
    if (days === 0) return 'Today';
    if (days === 1) return 'Tomorrow';
    return `In ${days} days`;
  }
  private greetingFor(h: number): string {
    if (h < 12) return 'Good morning.';
    if (h < 17) return 'Good afternoon.';
    return 'Good evening.';
  }
}


