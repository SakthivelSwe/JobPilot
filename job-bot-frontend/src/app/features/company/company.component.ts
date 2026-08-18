import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CompanyOverview, CompanyService } from '../../core/services/company.service';

@Component({
  selector: 'app-company',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <ng-container *ngIf="data() as c">
      <header class="masthead">
        <div>
          <div class="kicker">Company</div>
          <h1 class="display">{{ c.company }}</h1>
          <p class="lede">Everything JobPilot knows about your relationship with this company.</p>
        </div>
      </header>

      <div class="stat-line">
        <div class="s"><span class="n numeric">{{ c.openRoles }}</span><span class="l">open roles</span></div>
        <div class="s"><span class="n numeric">{{ c.applications }}</span><span class="l">applications</span></div>
        <div class="s"><span class="n numeric">{{ c.interviews }}</span><span class="l">interviews</span></div>
        <div class="s"><span class="n numeric">{{ c.saved }}</span><span class="l">saved</span></div>
      </div>

      <!-- Open roles -->
      <section>
        <div class="section-head"><h2>Open roles</h2><span class="count">{{ c.roles.length }}</span></div>
        <div class="empty" *ngIf="!c.roles.length" style="text-align:left;padding:16px 0;">
          No discovered roles at {{ c.company }} right now. Run a scan or import a job.
        </div>
        <div class="rows" *ngIf="c.roles.length">
          <a class="rrow" *ngFor="let r of c.roles" [routerLink]="r.route">
            <span class="rscore numeric" *ngIf="r.matchScore != null">{{ r.matchScore }}</span>
            <span class="rscore muted" *ngIf="r.matchScore == null">—</span>
            <div class="rmain">
              <span class="rtitle">{{ r.title }}</span>
              <span class="rmeta" *ngIf="r.location">{{ r.location }}</span>
            </div>
            <span class="ropen">Review →</span>
          </a>
        </div>
      </section>

      <!-- Applications -->
      <section>
        <div class="section-head"><h2>Your applications</h2><span class="count">{{ c.apps.length }}</span></div>
        <div class="empty" *ngIf="!c.apps.length" style="text-align:left;padding:16px 0;">
          You haven't applied to {{ c.company }} yet.
        </div>
        <table class="data" *ngIf="c.apps.length">
          <thead><tr><th>Role</th><th>Stage</th><th>Applied</th></tr></thead>
          <tbody>
            <tr *ngFor="let a of c.apps">
              <td class="strong">{{ a.title }}</td>
              <td><span class="chip" [class]="chip(a.status)">{{ a.status }}</span></td>
              <td class="muted numeric">{{ a.appliedAt ? (a.appliedAt | date:'dd MMM') : '—' }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <a routerLink="/applications" class="back">← Back to pipeline</a>
    </ng-container>

    <div class="empty" *ngIf="!data() && !loading()">
      <span class="big">✦</span> Company not found in your data.
    </div>
    <div class="empty" *ngIf="loading()"><span class="big">…</span> Loading…</div>
  `,
  styles: [`
    .masthead { margin-bottom:8px; }
    .masthead .lede { color:var(--ink-2); font-size:15px; margin:8px 0 0; }
    .stat-line { display:flex; gap:36px; margin:20px 0 8px; flex-wrap:wrap; }
    .s { display:flex; flex-direction:column; }
    .s .n { font-size:26px; font-weight:600; color:var(--ink); }
    .s .l { font-size:12.5px; color:var(--ink-2); }
    .rows { display:flex; flex-direction:column; border:1px solid var(--line); border-radius:var(--radius); overflow:hidden; background:var(--surface); }
    .rrow { display:flex; align-items:center; gap:16px; padding:14px 16px; border-bottom:1px solid var(--line); text-decoration:none; }
    .rrow:last-child { border-bottom:0; } .rrow:hover { background:var(--surface-2); text-decoration:none; }
    .rscore { width:40px; text-align:center; font-size:18px; font-weight:600; color:var(--accent-deep); }
    .rmain { flex:1; display:flex; flex-direction:column; }
    .rtitle { font-weight:600; color:var(--ink); }
    .rmeta { font-size:12.5px; color:var(--ink-3); }
    .ropen { font-size:12.5px; color:var(--accent); }
    .back { display:inline-block; margin-top:24px; font-size:13.5px; }
  `]
})
export class CompanyPageComponent implements OnInit {
  private svc = inject(CompanyService);
  private route = inject(ActivatedRoute);

  data = signal<CompanyOverview | null>(null);
  loading = signal(true);

  ngOnInit(): void {
    const name = decodeURIComponent(this.route.snapshot.paramMap.get('name') || '');
    if (!name) { this.loading.set(false); return; }
    this.svc.overview(name).subscribe({
      next: d => { this.data.set(d); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  chip(status?: string): string {
    const s = (status || '').toLowerCase();
    if (s === 'offer') return 'green';
    if (s === 'interview') return 'blue';
    if (s === 'rejected' || s === 'withdrawn') return 'red';
    return 'gray';
  }
}


