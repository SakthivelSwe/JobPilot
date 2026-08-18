import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AnalyticsService, AnalyticsOverview, LearningResult, Momentum } from '../../core/services/analytics.service';
import { ConfigService } from '../../core/config/thresholds';

interface Finding { tone: 'success' | 'warning' | 'info'; label: string; value: string; detail: string; }

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule],
  template: `
    <header class="masthead">
      <div>
        <div class="kicker">Decision intelligence</div>
        <h1 class="display">Insights</h1>
        <p class="lede">What's working, what isn't, and what to change — every number is derived from your real applications.</p>
      </div>
    </header>

    <!-- Headline story -->
    <div class="story" *ngIf="overview() as o">
      <p class="story-lead">
        <ng-container *ngIf="o.applications > 0; else noApps">
          You've sent <b>{{ o.applications }}</b> application{{ o.applications === 1 ? '' : 's' }}.
          <ng-container *ngIf="o.applications > 0">
            <b>{{ o.interviewRate }}%</b> reached an interview<span *ngIf="o.responseRate">, and <b>{{ o.responseRate }}%</b> got some response</span>.
          </ng-container>
          <span *ngIf="momentum()?.available"> Momentum is <b>{{ momentum()!.label!.toLowerCase() }}</b> ({{ momentum()!.score }}/100).</span>
        </ng-container>
        <ng-template #noApps>
          You haven't applied to anything yet. Once you do, this page will tell you which
          roles, sources and résumés are converting — and where to focus.
        </ng-template>
      </p>
    </div>

    <!-- What's working -->
    <section *ngIf="(overview()?.applications ?? 0) > 0">
      <div class="section-head"><h2>What's working</h2></div>
      <div class="findings">
        <div class="finding" *ngFor="let f of working()" [class]="'finding ' + f.tone">
          <div class="f-label">{{ f.label }}</div>
          <div class="f-value">{{ f.value }}</div>
          <div class="f-detail">{{ f.detail }}</div>
        </div>
        <div class="finding info" *ngIf="!working().length">
          <div class="f-value">Not enough signal yet</div>
          <div class="f-detail">Apply to a few more roles to see which are converting.</div>
        </div>
      </div>
    </section>

    <!-- What to improve -->
    <section *ngIf="bottleneck() as b">
      <div class="section-head"><h2>What to improve</h2></div>
      <div class="bottleneck">
        <div class="b-title">{{ b.value }}</div>
        <div class="b-detail">{{ b.detail }}</div>
      </div>
    </section>

    <!-- Learning recommendations -->
    <section *ngIf="learning() as l">
      <div class="section-head">
        <h2>JobPilot recommendations</h2>
        <span class="count">{{ l.applications }} / {{ l.threshold }} applications</span>
      </div>
      <div class="panel" style="padding:16px 20px;">
        <div class="muted" *ngIf="!l.ready">{{ l.message }}</div>
        <ul *ngIf="l.ready" style="margin:0; padding-left:18px;">
          <li *ngFor="let r of l.recommendations" style="margin:6px 0;">{{ r }}</li>
          <li *ngIf="!l.recommendations.length" class="muted">No strong signal yet — keep applying.</li>
        </ul>
      </div>
    </section>

    <!-- Evidence -->
    <section>
      <div class="section-head"><h2>Evidence</h2><span class="count">the numbers behind the story</span></div>
      <div class="grid cols-2">
        <div class="panel" style="padding:16px 20px;">
          <div class="kicker" style="margin-bottom:10px;">By role</div>
          <table class="data">
            <thead><tr><th>Role</th><th class="r">Apps</th><th class="r">Interviews</th><th class="r">Rate</th></tr></thead>
            <tbody>
              <tr *ngFor="let r of roles()">
                <td>{{ r['role'] }}</td><td class="r numeric">{{ r['applications'] }}</td>
                <td class="r numeric">{{ r['interviews'] }}</td>
                <td class="r numeric">{{ r['applications'] > 0 ? r['interviewRate'] + '%' : '—' }}</td>
              </tr>
              <tr *ngIf="!roles().length"><td colspan="4" class="muted">No data yet.</td></tr>
            </tbody>
          </table>
        </div>
        <div class="panel" style="padding:16px 20px;">
          <div class="kicker" style="margin-bottom:10px;">By source</div>
          <table class="data">
            <thead><tr><th>Source</th><th class="r">Discovered</th><th class="r">Applied</th><th class="r">Response</th></tr></thead>
            <tbody>
              <tr *ngFor="let s of sources()">
                <td>{{ s['source'] }}</td><td class="r numeric">{{ s['discovered'] }}</td>
                <td class="r numeric">{{ s['applications'] }}</td>
                <td class="r numeric">{{ s['applications'] > 0 ? s['responseRate'] + '%' : '—' }}</td>
              </tr>
              <tr *ngIf="!sources().length"><td colspan="4" class="muted">No data yet.</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Funnel -->
      <div class="funnel" *ngIf="overview() as o">
        <div class="fn"><span class="fn-n numeric">{{ o.jobsDiscovered }}</span><span class="fn-l">discovered</span></div>
        <span class="fn-arrow">→</span>
        <div class="fn"><span class="fn-n numeric">{{ o.jobsMatched }}</span><span class="fn-l">matched ≥{{ config.thresholds().defaultMinMatchScore }}</span></div>
        <span class="fn-arrow">→</span>
        <div class="fn"><span class="fn-n numeric">{{ o.strongMatches }}</span><span class="fn-l">strong</span></div>
        <span class="fn-arrow">→</span>
        <div class="fn"><span class="fn-n numeric">{{ o.applications }}</span><span class="fn-l">applied</span></div>
      </div>
    </section>
  `,
  styles: [`
    .masthead { margin-bottom:8px; }
    .masthead .lede { color:var(--ink-2); font-size:15px; margin:8px 0 0; max-width:64ch; }
    .story { margin-top:20px; }
    .story-lead { font-family:var(--font-display); font-size:20px; line-height:1.5; color:var(--ink); max-width:70ch; margin:0; }
    .story-lead b { color:var(--accent-deep); font-weight:600; }
    .findings { display:grid; grid-template-columns:repeat(3,1fr); gap:14px; }
    @media (max-width:800px){ .findings{ grid-template-columns:1fr; } }
    .finding { border:1px solid var(--line); border-radius:var(--radius); padding:16px; border-left-width:3px; }
    .finding.success { border-left-color:var(--success); }
    .finding.warning { border-left-color:var(--warning); }
    .finding.info { border-left-color:var(--info); }
    .f-label { font-size:11.5px; font-weight:700; letter-spacing:0.05em; text-transform:uppercase; color:var(--ink-3); }
    .f-value { font-family:var(--font-display); font-size:18px; font-weight:600; color:var(--ink); margin:4px 0 2px; }
    .f-detail { font-size:12.5px; color:var(--ink-2); }
    .bottleneck { border:1px solid var(--warning); background:var(--warning-wash); border-radius:var(--radius); padding:16px 20px; }
    .b-title { font-family:var(--font-display); font-size:18px; font-weight:600; color:var(--ink); }
    .b-detail { font-size:13.5px; color:var(--ink-2); margin-top:4px; max-width:70ch; }
    .r { text-align:right; }
    .funnel { display:flex; align-items:center; gap:16px; margin-top:20px; flex-wrap:wrap; }
    .fn { display:flex; flex-direction:column; }
    .fn-n { font-size:24px; font-weight:600; color:var(--ink); }
    .fn-l { font-size:12px; color:var(--ink-2); }
    .fn-arrow { color:var(--ink-3); font-size:18px; }
  `]
})
export class AnalyticsPageComponent implements OnInit {
  private analytics = inject(AnalyticsService);
  config = inject(ConfigService);

  overview = signal<AnalyticsOverview | null>(null);
  learning = signal<LearningResult | null>(null);
  momentum = signal<Momentum | null>(null);
  roles = signal<Record<string, any>[]>([]);
  sources = signal<Record<string, any>[]>([]);

  ngOnInit(): void {
    this.analytics.overview().subscribe({ next: o => this.overview.set(o), error: () => {} });
    this.analytics.learning().subscribe({ next: l => this.learning.set(l), error: () => {} });
    this.analytics.momentum().subscribe({ next: m => this.momentum.set(m), error: () => {} });
    this.analytics.roles().subscribe({ next: r => this.roles.set(r), error: () => {} });
    this.analytics.sources().subscribe({ next: s => this.sources.set(s), error: () => {} });
  }

  /** Best role / source / résumé — only where there's real signal. */
  working = computed<Finding[]>(() => {
    const out: Finding[] = [];
    const bestRole = [...this.roles()]
      .filter(r => (r['applications'] ?? 0) > 0)
      .sort((a, b) => (b['interviewRate'] ?? 0) - (a['interviewRate'] ?? 0))[0];
    if (bestRole && bestRole['interviewRate'] > 0) {
      out.push({ tone: 'success', label: 'Best-performing role', value: bestRole['role'],
        detail: `${bestRole['interviewRate']}% of ${bestRole['applications']} applications reached an interview.` });
    }
    const bestSource = [...this.sources()]
      .filter(s => (s['applications'] ?? 0) > 0)
      .sort((a, b) => (b['responseRate'] ?? 0) - (a['responseRate'] ?? 0))[0];
    if (bestSource && bestSource['responseRate'] > 0) {
      out.push({ tone: 'success', label: 'Strongest source', value: bestSource['source'],
        detail: `${bestSource['responseRate']}% response rate from ${bestSource['applications']} applications.` });
    }
    const o = this.overview();
    if (o && o.strongMatches > 0) {
      out.push({ tone: 'info', label: 'Opportunity supply', value: `${o.strongMatches} strong matches`,
        detail: 'High-fit roles are being found — keep the review queue moving.' });
    }
    return out;
  });

  /** The single biggest bottleneck, derived from the funnel. */
  bottleneck = computed<Finding | null>(() => {
    const o = this.overview();
    if (!o) return null;
    if (o.applications === 0 && o.strongMatches > 0) {
      return { tone: 'warning', label: 'bottleneck', value: 'Strong matches aren\'t being applied to',
        detail: `${o.strongMatches} strong matches are waiting. Applying to them is the fastest way to create interviews.` };
    }
    if (o.applications > 0 && o.interviewRate < 10) {
      return { tone: 'warning', label: 'bottleneck', value: 'Low response after application',
        detail: 'Few applications are converting to interviews. Consider tightening your résumé-to-role match, or targeting higher-fit roles.' };
    }
    if (o.jobsDiscovered > 0 && o.jobsMatched === 0) {
      return { tone: 'warning', label: 'bottleneck', value: 'Criteria are filtering out everything',
        detail: 'Nothing is matching your criteria. Loosen the minimum match score or broaden your target roles.' };
    }
    return null;
  });
}


