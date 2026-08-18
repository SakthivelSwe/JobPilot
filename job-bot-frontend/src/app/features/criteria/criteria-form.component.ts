import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { CriteriaService } from '../../core/services/criteria.service';
import { ResumeService } from '../../core/services/resume.service';
import { ToastService } from '../../core/services/toast.service';
import { JobCriteria, Resume } from '../../core/models';

@Component({
  selector: 'app-criteria-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-title">{{ id ? 'Edit' : 'New' }} Criteria</div>
    <div class="page-sub">Define what jobs to target and the minimum ATS score to shortlist</div>

    <div class="card" style="max-width:760px;">
      <div class="field">
        <label>Name</label>
        <input [(ngModel)]="model.name" placeholder="e.g. Senior Java Chennai" />
      </div>
      <div class="field">
        <label>Resume</label>
        <select [(ngModel)]="model.resumeId">
          <option [ngValue]="undefined">— none —</option>
          <option *ngFor="let r of resumes" [ngValue]="r.id">{{ r.name }}</option>
        </select>
      </div>
      <div class="field">
        <label>Keywords (comma separated)</label>
        <input [(ngModel)]="keywordsText" placeholder="Java, Spring Boot, Kafka" />
      </div>
      <div class="field">
        <label>Locations (comma separated)</label>
        <input [(ngModel)]="locationsText" placeholder="Chennai, Bangalore, Remote" />
      </div>
      <div class="grid cols-2">
        <div class="field">
          <label>Experience Min</label>
          <input type="number" [(ngModel)]="model.experienceMin" />
        </div>
        <div class="field">
          <label>Experience Max</label>
          <input type="number" [(ngModel)]="model.experienceMax" />
        </div>
      </div>
      <div class="field">
        <label>Exclude Companies (comma separated)</label>
        <input [(ngModel)]="excludeText" placeholder="CompanyA, CompanyB" />
      </div>
      <div class="field">
        <label>Minimum Match Score: {{ model.minMatchScore }}</label>
        <input type="range" min="0" max="100" [(ngModel)]="model.minMatchScore" />
      </div>

      <!-- Boolean query builder -->
      <div class="bq">
        <div class="bq-head">
          <label>Advanced boolean rule <span class="muted">(optional)</span></label>
          <span class="bq-status" [class.ok]="bqValid === true" [class.bad]="bqValid === false">
            {{ bqValid === null ? '' : (bqValid ? '✓ valid' : '✕ ' + bqError) }}
          </span>
        </div>
        <input [ngModel]="model.booleanQuery || ''" (ngModelChange)="onQueryChange($event)"
               placeholder="Java AND (Kafka OR Microservices) AND NOT Intern" class="bq-input mono" />
        <div class="bq-chips">
          <button type="button" class="bq-chip" (click)="insert('AND')">AND</button>
          <button type="button" class="bq-chip" (click)="insert('OR')">OR</button>
          <button type="button" class="bq-chip" (click)="insert('NOT')">NOT</button>
          <button type="button" class="bq-chip" (click)="insert('(')">(</button>
          <button type="button" class="bq-chip" (click)="insert(')')">)</button>
        </div>
        <div class="bq-test" *ngIf="model.booleanQuery">
          <input [(ngModel)]="bqSample" (ngModelChange)="onQueryChange(model.booleanQuery || '')"
                 placeholder="Paste a job description to test the rule…" />
          <span class="bq-match" *ngIf="bqValid && bqMatches !== null"
                [class.hit]="bqMatches" [class.miss]="!bqMatches">
            {{ bqMatches ? 'MATCH' : 'no match' }}
          </span>
        </div>
        <div class="muted bq-hint">Precedence: NOT &gt; AND &gt; OR. Use parentheses to group. Multi-word phrases are matched whole.</div>
      </div>

      <div class="field">
        <label><input type="checkbox" [(ngModel)]="model.active" /> Active</label>
      </div>
      <div class="row">
        <button class="btn" (click)="save()">Save</button>
        <button class="btn secondary" (click)="cancel()">Cancel</button>
        <span class="muted" *ngIf="error">{{ error }}</span>
      </div>
    </div>
  `,
  styles: [`
    .bq { margin: 4px 0 16px; padding: 16px; border: 1px solid var(--line); border-radius: var(--radius); background: var(--surface-2); }
    .bq-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:8px; }
    .bq-status { font-size:12.5px; font-weight:600; }
    .bq-status.ok { color: var(--success); }
    .bq-status.bad { color: var(--danger); }
    .bq-input { width:100%; padding:9px 11px; border:1px solid var(--line-strong); border-radius:var(--radius-sm);
      background:var(--surface); color:var(--ink); font-size:13.5px; }
    .bq-chips { display:flex; gap:6px; margin:8px 0; }
    .bq-chip { background:var(--surface); border:1px solid var(--line-strong); border-radius:6px; padding:3px 10px;
      font:600 12px var(--font-mono); color:var(--ink-2); cursor:pointer; }
    .bq-chip:hover { border-color:var(--accent); color:var(--accent); }
    .bq-test { display:flex; align-items:center; gap:10px; margin-top:8px; }
    .bq-test input { flex:1; padding:8px 10px; border:1px solid var(--line-strong); border-radius:var(--radius-sm);
      background:var(--surface); color:var(--ink); font-size:13px; }
    .bq-match { font-size:12px; font-weight:700; padding:3px 9px; border-radius:6px; }
    .bq-match.hit { background:var(--success-wash); color:var(--success); }
    .bq-match.miss { background:var(--bg-tint); color:var(--ink-3); }
    .bq-hint { font-size:11.5px; margin-top:8px; }
  `]
})
export class CriteriaFormComponent implements OnInit {
  private service = inject(CriteriaService);
  private resumeService = inject(ResumeService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private toast = inject(ToastService);

  id: string | null = null;
  resumes: Resume[] = [];
  model: Partial<JobCriteria> = {
    experienceMin: 2, experienceMax: 6, minMatchScore: 65, active: true
  };
  keywordsText = '';
  locationsText = '';
  excludeText = '';
  error = '';

  // boolean-query builder state
  bqValid: boolean | null = null;
  bqError = '';
  bqMatches: boolean | null = null;
  bqSample = '';
  private bqTimer: any;

  ngOnInit(): void {
    this.resumeService.list().subscribe(r => (this.resumes = r));
    this.id = this.route.snapshot.paramMap.get('id');
    if (this.id) {
      this.service.get(this.id).subscribe(c => {
        this.model = c;
        this.keywordsText = (c.keywords || []).join(', ');
        this.locationsText = (c.locations || []).join(', ');
        this.excludeText = (c.excludeCompanies || []).join(', ');
        if (c.booleanQuery) this.onQueryChange(c.booleanQuery);
      });
    }
  }

  onQueryChange(q: string): void {
    this.model.booleanQuery = q;
    clearTimeout(this.bqTimer);
    if (!q || !q.trim()) { this.bqValid = null; this.bqError = ''; this.bqMatches = null; return; }
    this.bqTimer = setTimeout(() => {
      this.service.validateQuery(q, this.bqSample || undefined).subscribe({
        next: v => { this.bqValid = v.valid; this.bqError = v.error || ''; this.bqMatches = v.matches ?? null; },
        error: () => { this.bqValid = null; },
      });
    }, 350);
  }

  insert(token: string): void {
    const cur = this.model.booleanQuery || '';
    const sep = cur && !cur.endsWith(' ') ? ' ' : '';
    this.onQueryChange(`${cur}${sep}${token} `);
  }

  private split(s: string): string[] {
    return s.split(',').map(x => x.trim()).filter(x => x);
  }

  save(): void {
    if (!this.model.name) { this.error = 'Name is required'; return; }
    if (this.model.booleanQuery && this.bqValid === false) {
      this.error = 'Fix the boolean rule before saving: ' + this.bqError;
      this.toast.error('Invalid boolean rule');
      return;
    }
    const body: Partial<JobCriteria> = {
      ...this.model,
      keywords: this.split(this.keywordsText),
      locations: this.split(this.locationsText),
      excludeCompanies: this.split(this.excludeText)
    };
    const obs = this.id ? this.service.update(this.id, body) : this.service.create(body);
    obs.subscribe({
      next: () => { this.toast.success('Criteria saved'); this.router.navigate(['/criteria']); },
      error: e => { this.error = e?.error?.message || 'Save failed'; this.toast.error(this.error); }
    });
  }

  cancel(): void { this.router.navigate(['/criteria']); }
}


