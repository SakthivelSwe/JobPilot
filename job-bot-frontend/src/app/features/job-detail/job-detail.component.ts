import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatchService, RankedMatch } from '../../core/services/match.service';
import { ResumeEngineService, VariantScore, TailoredResume } from '../../core/services/resume-engine.service';
import { ManualQueueService } from '../../core/services/manual-queue.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-job-detail',
  standalone: true,
  imports: [CommonModule],
  template: `
    <ng-container *ngIf="rm() as m; else loading">
      <div class="page-head row">
        <div>
          <div class="page-title">{{ m.posting.title }}</div>
          <div class="page-sub">
            {{ m.posting.company }} · {{ m.posting.location || '—' }} · {{ m.posting.remoteType }}
            <span *ngIf="m.posting.minimumExperience != null">
              · {{ m.posting.minimumExperience }}–{{ m.posting.maximumExperience }} yrs</span>
          </div>
        </div>
      </div>

      <div class="detail-grid">
        <!-- LEFT: job content -->
        <div>
          <div class="card">
            <h3 style="margin:0 0 8px;">Requirements</h3>
            <div>
              <span class="chip green" *ngFor="let s of m.posting.requiredSkills" style="margin:2px;">{{ s }}</span>
              <span class="chip" *ngFor="let s of m.posting.preferredSkills" style="margin:2px;">{{ s }}</span>
              <span *ngIf="!m.posting.requiredSkills?.length" class="muted">No structured skills listed.</span>
            </div>
          </div>
          <div class="card" style="margin-top:14px;">
            <h3 style="margin:0 0 8px;">Description</h3>
            <div class="muted" style="white-space:pre-wrap; font-size:13px; line-height:1.5;">{{ m.posting.description || 'No description.' }}</div>
          </div>

          <div class="card" style="margin-top:14px;" *ngIf="tailored() as t">
            <div class="row">
              <h3 style="margin:0;">Tailored résumé — {{ t.roleTarget }}</h3>
              <div class="spacer"></div>
              <span class="chip amber">from verified facts only</span>
            </div>
            <p class="muted" style="white-space:pre-wrap; font-size:13px;">{{ t.summary }}</p>
            <div style="margin-top:6px;">
              <strong style="font-size:13px;">Skills (reordered):</strong>
              <span class="chip" *ngFor="let s of t.orderedSkills" style="margin:2px;"
                    [class.green]="t.emphasizedKeywords.includes(s)">{{ s }}</span>
            </div>
            <div style="margin-top:8px;" *ngIf="t.projects.length">
              <strong style="font-size:13px;">Projects:</strong>
              <div class="muted" style="font-size:13px;" *ngFor="let p of t.projects">
                • {{ p.name }} <span *ngIf="p.technologies.length">— {{ p.technologies.join(', ') }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- RIGHT: sticky match panel -->
        <aside class="match-panel">
          <div class="card">
            <div class="row">
              <h3 style="margin:0;">Match</h3>
              <div class="spacer"></div>
              <div class="big-score" [class.strong]="m.match.overallScore>=80" [class.mid]="m.match.overallScore>=55 && m.match.overallScore<80">
                {{ m.match.overallScore }}
              </div>
            </div>
            <div class="chip green" style="margin-top:4px;">{{ m.match.recommendation }}</div>

            <div class="factor" *ngFor="let f of factors(m)">
              <span class="f-label">{{ f.label }}</span>
              <div class="f-bar"><div class="f-fill" [style.width.%]="f.value"></div></div>
              <span class="f-val">{{ f.value }}</span>
            </div>

            <div style="margin-top:10px;" *ngIf="riskFactors(m).length">
              <strong style="font-size:12px;color:#b91c1c;">Risks</strong>
              <div class="muted" style="font-size:12px;" *ngFor="let r of riskFactors(m)">△ {{ r }}</div>
            </div>
          </div>

          <div class="card" style="margin-top:12px;">
            <div class="row">
              <div>
                <div class="muted" style="font-size:12px;">Recommended résumé</div>
                <strong>{{ recommendedVariant()?.roleTarget || '—' }}</strong>
              </div>
              <div class="spacer"></div>
              <div class="big-score mid" *ngIf="recommendedVariant() as rv">{{ rv.score }}</div>
            </div>
            <div class="muted" style="font-size:12px; margin-top:8px;">Application</div>
            <div class="chip amber">{{ m.posting.applicationCapability }}</div>

            <div class="row" style="gap:6px; margin-top:12px; flex-wrap:wrap;">
              <button class="btn small" (click)="prepare()" [disabled]="preparing()">
                {{ preparing() ? 'Preparing…' : 'Prepare application' }}
              </button>
              <a class="btn secondary small" [href]="m.posting.applicationUrl || m.posting.sourceUrl" target="_blank" rel="noopener">Open</a>
              <button class="btn secondary small" (click)="addToManual()">＋ Manual queue</button>
            </div>
          </div>
        </aside>
      </div>
    </ng-container>
    <ng-template #loading><div class="empty"><span class="big">⏳</span>Loading…</div></ng-template>
  `,
  styles: [`
    .detail-grid { display:grid; grid-template-columns: 1fr 340px; gap:16px; margin-top:8px; }
    @media (max-width: 900px) { .detail-grid { grid-template-columns: 1fr; } }
    .match-panel { position:sticky; top:16px; align-self:start; }
    .big-score { width:52px; height:52px; border-radius:50%; display:flex; align-items:center; justify-content:center;
                 font-weight:800; font-size:18px; color:#64748b; background:#f1f5f9; border:2px solid #e2e8f0; }
    .big-score.mid { color:#b45309; background:#fffbeb; border-color:#fde68a; }
    .big-score.strong { color:#15803d; background:#f0fdf4; border-color:#bbf7d0; }
    .factor { display:flex; align-items:center; gap:8px; margin:7px 0; }
    .f-label { width:92px; font-size:12px; text-transform:capitalize; }
    .f-bar { flex:1; height:8px; background:#f1f5f9; border-radius:999px; overflow:hidden; }
    .f-fill { height:100%; background:linear-gradient(90deg,#2563eb,#4f46e5); }
    .f-val { width:26px; text-align:right; font-size:12px; font-weight:700; }
  `]
})
export class JobDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private matchSvc = inject(MatchService);
  private resumeEngine = inject(ResumeEngineService);
  private manual = inject(ManualQueueService);
  private toast = inject(ToastService);

  rm = signal<RankedMatch | null>(null);
  variants = signal<VariantScore[]>([]);
  tailored = signal<TailoredResume | null>(null);
  preparing = signal(false);

  private id = '';

  ngOnInit(): void {
    this.id = this.route.snapshot.paramMap.get('id') ?? '';
    this.matchSvc.posting(this.id).subscribe(m => this.rm.set(m));
    this.resumeEngine.select(this.id).subscribe(v => this.variants.set(v));
  }

  recommendedVariant(): VariantScore | undefined {
    return this.variants().find(v => v.recommended) ?? this.variants()[0];
  }

  factors(m: RankedMatch): { label: string; value: number }[] {
    const x = m.match;
    return [
      { label: 'Skills', value: x.technicalScore },
      { label: 'Experience', value: x.experienceScore },
      { label: 'Role', value: x.roleScore },
      { label: 'Location', value: x.locationScore },
      { label: 'Work mode', value: x.workModeScore },
      { label: 'Notice', value: x.noticeScore },
      { label: 'Salary', value: x.salaryScore },
      { label: 'Company', value: x.companyScore },
    ];
  }

  riskFactors(m: RankedMatch): string[] { return m.match.riskFactors ?? []; }

  prepare(): void {
    this.preparing.set(true);
    this.resumeEngine.tailor(this.id).subscribe({
      next: t => { this.tailored.set(t); this.preparing.set(false); this.toast.success('Application prepared'); },
      error: () => { this.preparing.set(false); this.toast.error('Add a candidate profile first'); }
    });
  }

  addToManual(): void {
    this.manual.add(this.id).subscribe(() => this.toast.success('Added to manual queue'));
  }
}


