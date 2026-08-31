import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DiscoveryService, SourceHealthRow, CoverageStats } from '../../core/services/discovery.service';
import { MatchService, RankedMatch } from '../../core/services/match.service';
import { ManualQueueService } from '../../core/services/manual-queue.service';
import { JobQueueService } from '../../core/services/job-queue.service';
import { ToastService } from '../../core/services/toast.service';
import { ConfigService } from '../../core/config/thresholds';

@Component({
  selector: 'app-discovery',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <!-- Masthead -->
    <header class="masthead">
      <div>
        <div class="kicker">Discover</div>
        <h1 class="display">Opportunities</h1>
        <p class="lede">
          Curated from authorized public sources and ranked against your profile.
          <span *ngIf="coverage() as c"> {{ c.postingsTotal }} tracked · {{ c.postingsLast24h }} new in 24h.</span>
        </p>
      </div>
      <div class="masthead-actions">
        <button class="btn" (click)="scan()" [disabled]="scanning()">
          {{ scanning() ? 'Scanning…' : 'Scan for jobs' }}
        </button>
      </div>
    </header>

    <div class="discover-layout">
      <!-- Filter rail -->
      <aside class="rail">
        <div class="rail-section">
          <div class="kicker">Refine</div>
          <div class="field">
            <label>Search</label>
            <input type="text" [ngModel]="q()" (ngModelChange)="q.set($event)" placeholder="Role, company, skill…" />
          </div>
          <div class="field">
            <label>Minimum match</label>
            <input type="range" min="0" max="100" step="5" [ngModel]="minScore()" (ngModelChange)="minScore.set(+$event)" />
            <div class="rail-range"><span class="numeric">{{ minScore() }}%</span> and above</div>
          </div>
          <div class="field">
            <label>Source</label>
            <div class="rail-toggles">
              <button *ngFor="let s of platforms" class="toggle"
                      [class.on]="platform() === s" (click)="platform.set(platform() === s ? null : s)">{{ s }}</button>
            </div>
          </div>
          <div class="field">
            <label>Recommendation</label>
            <div class="rail-toggles">
              <button *ngFor="let r of recommendations" class="toggle"
                      [class.on]="rec() === r" (click)="rec.set(rec() === r ? null : r)">{{ label(r) }}</button>
            </div>
          </div>
          <div class="field">
            <label>Work Mode</label>
            <div class="rail-toggles">
              <button *ngFor="let m of ['REMOTE', 'HYBRID', 'ONSITE']" class="toggle"
                      [class.on]="workMode() === m" (click)="workMode.set(workMode() === m ? null : m)">{{ m | titlecase }}</button>
            </div>
          </div>
          <div class="field" *ngIf="locations().length">
            <label>Location</label>
            <select class="rail-select" [ngModel]="locationFilter()" (ngModelChange)="locationFilter.set($event)">
              <option [ngValue]="null">All Locations</option>
              <option *ngFor="let l of locations()" [ngValue]="l">{{ l }}</option>
            </select>
          </div>
          <div class="field">
            <label>Posted within</label>
            <div class="rail-toggles">
              <button *ngFor="let a of ageOptions" class="toggle"
                      [class.on]="ageFilter() === a.days" (click)="setAgeFilter(a.days)">{{ a.label }}</button>
            </div>
          </div>
          <button class="btn ghost small" (click)="clear()" *ngIf="dirty()">Clear filters</button>
        </div>

        <div class="rail-section">
          <div class="kicker">Sources</div>
          <div class="src-row" *ngFor="let s of sources()">
            <span class="dot" [class.live]="s.status==='HEALTHY'" [class.warn]="s.status==='MANUAL'||s.status==='DEGRADED'" [class.err]="s.status==='UNAVAILABLE'"></span>
            <span class="src-name">{{ s.source }}</span>
            <span class="src-status">{{ s.status | lowercase }}</span>
          </div>
          <p class="rail-note">LinkedIn is applied via the browser extension, never server-side.</p>
        </div>
      </aside>

      <!-- Opportunity feed -->
      <section class="feed">
        <div class="feed-head">
          <span class="count numeric">{{ visible().length }}</span>
          <span class="feed-head-label">opportunit{{ visible().length === 1 ? 'y' : 'ies' }}</span>
          <span class="spacer"></span>
          <div class="bulk-actions" *ngIf="visible().length > 0">
            <button class="btn secondary small" [disabled]="selectedIds().size === 0" (click)="bulkAutoApplySelected()">
              Auto Apply Selected ({{ selectedIds().size }})
            </button>
            <button class="btn small" (click)="bulkAutoApplyAll()">
              Auto Apply All ({{ visible().length }})
            </button>
          </div>
          <span class="muted" style="font-size:12.5px; margin-left:12px;">ranked by match</span>
        </div>

        <!-- Loading skeletons -->
        <div *ngIf="loading()" class="feed-body">
          <div class="opp skeleton-opp" *ngFor="let _ of [1,2,3]">
            <div class="skeleton" style="width:52px;height:52px;border-radius:50%;"></div>
            <div style="flex:1;"><div class="skeleton" style="height:16px;width:50%;margin-bottom:8px;"></div>
              <div class="skeleton" style="height:12px;width:30%;"></div></div>
          </div>
        </div>

        <!-- Empty -->
        <div *ngIf="!loading() && !visible().length" class="empty">
          <span class="big">✦</span>
          <div style="font-weight:600;color:var(--ink);margin-bottom:4px;">
            {{ matches().length ? 'No opportunities match these filters' : 'No opportunities yet' }}
          </div>
          <div style="max-width:44ch;margin:0 auto 14px;">
            {{ matches().length
                ? 'Try relaxing the minimum match or clearing a source filter.'
                : 'Run a scan and JobPilot will surface roles that fit your profile, ranked by a deterministic match score.' }}
          </div>
          <button class="btn small" *ngIf="!matches().length" (click)="scan()">Scan for jobs</button>
          <button class="btn secondary small" *ngIf="matches().length" (click)="clear()">Clear filters</button>
        </div>

        <!-- Feed -->
        <div class="feed-body" *ngIf="!loading() && visible().length">
          <article class="opp" *ngFor="let m of paginatedVisible()" [class.is-strong]="m.match.overallScore >= strong()">
            
            <div class="opp-checkbox">
              <input type="checkbox" [checked]="selectedIds().has(m.posting.id)" (change)="toggleSelection(m.posting.id)" />
            </div>

            <div class="opp-score" [class.strong]="m.match.overallScore >= strong()" [class.mid]="m.match.overallScore >= 55 && m.match.overallScore < 80">
              <span class="numeric">{{ m.match.overallScore }}</span>
              <small>match</small>
            </div>

            <div class="opp-body">
              <div class="opp-top">
                <a class="opp-title" [routerLink]="['/jobs/posting', m.posting.id]">{{ m.posting.title }}</a>
                <span class="pf-badge" [class]="'pf-badge ' + pf(m.posting.source)">{{ m.posting.source }}</span>
                <span class="opp-rec" *ngIf="m.match.overallScore >= strong()">Strong opportunity</span>
              </div>
              <div class="opp-meta">
                <span class="strong" style="color:var(--ink-2);">{{ m.posting.company }}</span>
                <span *ngIf="m.posting.location">· {{ m.posting.location }}</span>
                <span *ngIf="m.posting.remoteType && m.posting.remoteType !== 'UNKNOWN'">· {{ m.posting.remoteType | titlecase }}</span>
                <span *ngIf="m.posting.maximumExperience">· {{ m.posting.minimumExperience || 0 }}–{{ m.posting.maximumExperience }} yrs</span>
              </div>

              <!-- Why this job -->
              <div class="why">
                <span class="why-label">Why it fits</span>
                <span class="why-skill ok" *ngFor="let s of m.match.matchedSkills.slice(0, 7)">✓ {{ s }}</span>
                <span class="why-skill gap" *ngFor="let s of m.match.missingRequiredSkills.slice(0, 3)">! {{ s }}</span>
                <span class="why-none" *ngIf="!m.match.matchedSkills.length">No overlapping skills detected — likely a weak fit.</span>
              </div>
            </div>

            <div class="opp-actions">
              <a class="btn small" [routerLink]="['/jobs/posting', m.posting.id]">Review</a>
              <button class="btn secondary small" (click)="addToManual(m)">Save</button>
              <a class="opp-open" [href]="m.posting.sourceUrl" target="_blank" rel="noopener">Open source ↗</a>
            </div>
          </article>
        </div>

        <!-- Pagination -->
        <div class="pagination" *ngIf="!loading() && totalPages() > 1">
          <button class="btn secondary small" [disabled]="page() === 0" (click)="prevPage()">Previous</button>
          <span class="muted" style="font-size:14px;">Page {{ Math.min(page(), totalPages() - 1) + 1 }} of {{ totalPages() }} ({{ visible().length }} items)</span>
          <button class="btn secondary small" [disabled]="page() >= totalPages() - 1" (click)="nextPage()">Next</button>
        </div>
      </section>
    </div>
  `,
  styles: [`
    .masthead { display:flex; align-items:flex-end; justify-content:space-between; gap:24px; margin-bottom:8px; }
    .masthead .lede { color:var(--ink-2); font-size:15px; margin:8px 0 0; max-width:64ch; }

    .discover-layout { display:grid; grid-template-columns: 250px 1fr; gap:32px; margin-top:24px; align-items:start; }
    .rail { position:sticky; top:78px; display:flex; flex-direction:column; gap:24px; }
    .rail-section { border-top:1px solid var(--line); padding-top:14px; }
    .rail-section .kicker { margin-bottom:12px; }
    .rail-range { font-size:12.5px; color:var(--ink-3); margin-top:4px; }
    .rail-toggles { display:flex; flex-wrap:wrap; gap:6px; }
    .toggle { background:transparent; border:1px solid var(--line-strong); color:var(--ink-2);
      border-radius:999px; padding:4px 11px; font:600 12px var(--font-sans); cursor:pointer; }
    .toggle:hover { border-color:var(--ink-3); }
    .toggle.on { background:var(--accent); color:var(--ink-on); border-color:var(--accent); }
    .src-row { display:flex; align-items:center; gap:8px; padding:5px 0; font-size:13px; }
    .src-name { font-weight:600; color:var(--ink); }
    .src-status { margin-left:auto; color:var(--ink-3); font-size:12px; }
    .rail-note { font-size:11.5px; color:var(--ink-3); margin:10px 0 0; line-height:1.5; }
    .rail-select { width: 100%; padding: 6px 10px; border: 1px solid var(--line-strong); border-radius: 6px; font: 13px var(--font-sans); color: var(--ink); background: var(--surface); }

    .feed-head { display:flex; align-items:baseline; gap:8px; padding-bottom:10px; border-bottom:1px solid var(--line); }
    .feed-head .count { font-size:22px; font-weight:600; color:var(--ink); }
    .feed-head-label { color:var(--ink-2); font-size:14px; }
    .feed-body { display:flex; flex-direction:column; }

    .opp { display:flex; align-items:flex-start; gap:18px; padding:20px 4px; border-bottom:1px solid var(--line); }
    .opp.is-strong { position:relative; }
    .opp.is-strong::before { content:''; position:absolute; left:-16px; top:20px; bottom:20px; width:3px; background:var(--accent); border-radius:2px; }
    .opp:hover { background:var(--surface-2); }
    .skeleton-opp { align-items:center; }

    .opp-checkbox { display:flex; align-items:center; justify-content:center; padding-top:14px; }
    .bulk-actions { display:flex; gap:8px; align-items:center; }

    .opp-score { width:56px; flex-shrink:0; display:flex; flex-direction:column; align-items:center; justify-content:center;
      border:1px solid var(--line); border-radius:12px; padding:8px 0; color:var(--ink-3); }
    .opp-score .numeric { font-size:22px; font-weight:600; color:var(--ink); }
    .opp-score small { font-size:10px; text-transform:uppercase; letter-spacing:0.05em; }
    .opp-score.mid { border-color:var(--warning); color:var(--warning); }
    .opp-score.mid .numeric { color:var(--warning); }
    .opp-score.strong { border-color:var(--accent); background:var(--accent-wash); }
    .opp-score.strong .numeric { color:var(--accent-deep); }

    .opp-body { flex:1; min-width:0; }
    .opp-top { display:flex; align-items:center; gap:10px; flex-wrap:wrap; }
    .opp-title { font-family:var(--font-display); font-size:18px; font-weight:600; color:var(--ink); }
    .opp-title:hover { color:var(--accent); text-decoration:none; }
    .opp-rec { font-size:11.5px; font-weight:700; color:var(--accent-deep); text-transform:uppercase; letter-spacing:0.03em; }
    .opp-meta { color:var(--ink-2); font-size:13.5px; margin-top:3px; display:flex; gap:6px; flex-wrap:wrap; }

    .why { margin-top:12px; display:flex; align-items:center; gap:6px; flex-wrap:wrap; }
    .why-label { font-size:11px; font-weight:700; letter-spacing:0.06em; text-transform:uppercase; color:var(--ink-3); margin-right:4px; }
    .why-skill { font-size:12.5px; font-weight:600; padding:2px 8px; border-radius:6px; }
    .why-skill.ok  { background:var(--success-wash); color:var(--success); }
    .why-skill.gap { background:var(--warning-wash); color:var(--warning); }
    .why-none { font-size:12.5px; color:var(--ink-3); font-style:italic; }

    .opp-actions { display:flex; flex-direction:column; align-items:flex-end; gap:8px; flex-shrink:0; }
    .opp-open { font-size:12px; color:var(--ink-3); }

    @media (max-width: 900px) {
      .discover-layout { grid-template-columns:1fr; gap:20px; }
      .rail { position:static; }
      .opp { flex-wrap:wrap; }
      .opp-actions { flex-direction:row; align-items:center; width:100%; }
      .masthead { flex-direction:column; align-items:stretch; }
    }
    .pagination { display: flex; justify-content: center; align-items: center; gap: 16px; margin-top: 24px; padding: 16px 0; border-top: 1px solid var(--line); }
  `]
})
export class DiscoveryPageComponent implements OnInit {
  protected readonly Math = Math;
  private discovery = inject(DiscoveryService);
  private matchSvc = inject(MatchService);
  private manual = inject(ManualQueueService);
  private queue = inject(JobQueueService);
  private toast = inject(ToastService);
  private config = inject(ConfigService);

  /** Single source of truth (rule 68). */
  strong = () => this.config.strongMatch;

  coverage = signal<CoverageStats | null>(null);
  sources = signal<SourceHealthRow[]>([]);
  matches = signal<RankedMatch[]>([]);
  scanning = signal(false);
  loading = signal(true);

  // filters
  q = signal('');
  minScore = signal(0);
  platform = signal<string | null>(null);
  rec = signal<string | null>(null);
  ageFilter = signal<number | null>(null);
  workMode = signal<string | null>(null);
  locationFilter = signal<string | null>(null);
  
  locations = computed(() => {
    const locs = new Set<string>();
    for (const m of this.matches()) {
      if (m.posting.location) {
        const parts = m.posting.location.split(/[,|–-]/).map(s => s.trim()).filter(Boolean);
        for (const p of parts) {
          if (p.toLowerCase() !== 'india' && p.length > 2 && p.toLowerCase() !== 'remote' && p.toLowerCase() !== 'hybrid') {
             locs.add(p);
          }
        }
      }
    }
    return Array.from(locs).sort();
  });
  
  selectedIds = signal<Set<string>>(new Set());
  
  page = signal(0);
  pageSize = signal(50);
  
  paginatedVisible = computed(() => {
    const total = this.visible().length;
    let p = this.page();
    const maxPage = Math.max(0, Math.ceil(total / this.pageSize()) - 1);
    if (p > maxPage) p = maxPage;
    
    const start = p * this.pageSize();
    return this.visible().slice(start, start + this.pageSize());
  });
  
  totalPages = computed(() => Math.ceil(this.visible().length / this.pageSize()));

  platforms = ['NAUKRI', 'LINKEDIN', 'INDEED'];
  recommendations = ['STRONG_APPLY', 'APPLY', 'REVIEW'];
  ageOptions = [
    { label: '24h', days: 1 },
    { label: '1 week', days: 7 },
    { label: '1 month', days: 30 },
  ];

  ngOnInit(): void { this.reload(); }

  reload(): void {
    this.loading.set(true);
    this.discovery.coverage().subscribe({ next: c => this.coverage.set(c), error: () => {} });
    this.discovery.sources().subscribe({ next: s => this.sources.set(s), error: () => {} });
    // Pass the current age filter to the backend so it filters at query time
    this.matchSvc.top(1000, 1000, this.ageFilter() ?? undefined).subscribe({
      next: m => { this.matches.set(m); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  visible = computed<RankedMatch[]>(() => {
    const q = this.q().trim().toLowerCase();
    const min = this.minScore();
    const pf = this.platform();
    const rec = this.rec();
    return this.matches().filter(m => {
      if (m.match.overallScore < min) return false;
      if (pf && (m.posting.source || '').toUpperCase() !== pf) return false;
      if (rec && (m.match.recommendation || '') !== rec) return false;
      
      if (this.workMode()) {
        const mode = this.workMode();
        const rmType = (m.posting.remoteType || '').toUpperCase();
        const locStr = (m.posting.location || '').toUpperCase();
        if (mode === 'REMOTE' && rmType !== 'REMOTE' && !locStr.includes('REMOTE')) return false;
        if (mode === 'HYBRID' && rmType !== 'HYBRID' && !locStr.includes('HYBRID')) return false;
        if (mode === 'ONSITE' && (rmType === 'REMOTE' || rmType === 'HYBRID' || locStr.includes('REMOTE') || locStr.includes('HYBRID'))) return false;
      }
      
      if (this.locationFilter()) {
        const locFilter = this.locationFilter()!.toLowerCase();
        const locStr = (m.posting.location || '').toLowerCase();
        if (!locStr.includes(locFilter)) return false;
      }
      
      if (q) {
        const hay = `${m.posting.title} ${m.posting.company} ${m.posting.location} ${m.match.matchedSkills.join(' ')}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
  });

  dirty = computed(() => !!this.q() || this.minScore() > 0 || !!this.platform() || !!this.rec() || this.ageFilter() != null || !!this.workMode() || !!this.locationFilter());

  clear(): void { this.q.set(''); this.minScore.set(0); this.platform.set(null); this.rec.set(null); this.ageFilter.set(null); this.workMode.set(null); this.locationFilter.set(null); this.page.set(0); this.reload(); }

  setAgeFilter(days: number): void {
    // Toggle: if same filter clicked, clear it; otherwise set it
    this.ageFilter.set(this.ageFilter() === days ? null : days);
    this.page.set(0);
    this.reload(); // Re-fetch from backend with the new date window
  }

  nextPage() {
    if (this.page() < this.totalPages() - 1) {
      this.page.set(this.page() + 1);
      window.scrollTo(0, 0);
    }
  }

  prevPage() {
    if (this.page() > 0) {
      this.page.set(this.page() - 1);
      window.scrollTo(0, 0);
    }
  }

  scan(): void {
    this.scanning.set(true);
    this.discovery.scan().subscribe({
      next: r => { this.toast.success(`${r.newPostings} new jobs from ${r.companiesScanned} sources`); this.scanning.set(false); this.reload(); },
      error: () => { this.toast.error('Scan failed — your saved data is safe.'); this.scanning.set(false); }
    });
  }

  addToManual(m: RankedMatch): void {
    this.manual.add(m.posting.id).subscribe({
      next: () => this.toast.success('Saved to manual queue'),
      error: () => this.toast.error('Could not save'),
    });
  }

  toggleSelection(id: string): void {
    const current = new Set(this.selectedIds());
    if (current.has(id)) {
      current.delete(id);
    } else {
      current.add(id);
    }
    this.selectedIds.set(current);
  }

  bulkAutoApplySelected(): void {
    const ids = Array.from(this.selectedIds());
    if (ids.length === 0) return;
    this.queue.approveBulk(ids).subscribe({
      next: (n) => {
        this.toast.success(`Sent ${n} jobs to auto-apply queue`);
        this.selectedIds.set(new Set());
        // Reload to remove them from discovery if they no longer match, or just let them stay but now they're enqueued.
        this.reload();
      },
      error: () => this.toast.error('Failed to auto-apply selected jobs')
    });
  }

  bulkAutoApplyAll(): void {
    const ids = this.visible().map(m => m.posting.id);
    if (ids.length === 0) return;
    this.queue.approveBulk(ids).subscribe({
      next: (n) => {
        this.toast.success(`Sent ${n} jobs to auto-apply queue`);
        this.selectedIds.set(new Set());
        this.reload();
      },
      error: () => this.toast.error('Failed to auto-apply jobs')
    });
  }

  pf(p?: string): string {
    const l = (p || '').toLowerCase();
    return ['naukri', 'linkedin', 'indeed'].includes(l) ? l : 'other';
  }
  label(r: string): string {
    return r === 'STRONG_APPLY' ? 'Strong' : r === 'APPLY' ? 'Apply' : 'Review';
  }
}


