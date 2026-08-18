import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { JobService } from '../../core/services/job.service';
import { CriteriaService } from '../../core/services/criteria.service';
import { PackService } from '../../core/services/pack.service';
import { ToastService } from '../../core/services/toast.service';
import { Job, JobCriteria, ResumeMatch } from '../../core/models';

@Component({
  selector: 'app-job-import',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-head row">
      <div>
        <div class="page-title">Import a Job</div>
        <div class="page-sub">Paste the URL + description. JobPilot scores it and builds your application pack — you apply yourself.</div>
      </div>
    </div>

    <div class="grid cols-2">
      <div class="card">
        <div class="field">
          <label>Job URL</label>
          <input [(ngModel)]="model.url" placeholder="https://www.linkedin.com/jobs/view/..." />
        </div>
        <div class="grid cols-2">
          <div class="field">
            <label>Platform</label>
            <select [(ngModel)]="model.platform">
              <option>linkedin</option><option>naukri</option><option>indeed</option><option>other</option>
            </select>
          </div>
          <div class="field">
            <label>Score against Criteria</label>
            <select [(ngModel)]="model.criteriaId">
              <option [ngValue]="undefined">— none —</option>
              <option *ngFor="let c of criteria" [ngValue]="c.id">{{ c.name }}</option>
            </select>
          </div>
        </div>
        <div class="field">
          <label>Title</label>
          <input [(ngModel)]="model.title" placeholder="Java Backend Engineer" />
        </div>
        <div class="grid cols-2">
          <div class="field"><label>Company</label><input [(ngModel)]="model.company" placeholder="Acme Corp" /></div>
          <div class="field"><label>Location</label><input [(ngModel)]="model.location" placeholder="Chennai" /></div>
        </div>
        <div class="field">
          <label>Job Description (paste full text)</label>
          <textarea [(ngModel)]="model.description" rows="10" placeholder="Paste the JD here — this is what gets scored"></textarea>
        </div>
        <div class="row">
          <button class="btn" (click)="submit()">Import &amp; Score</button>
          <button class="btn secondary" (click)="findBestResume()" [disabled]="!model.description">🔎 Find best resume</button>
        </div>
      </div>

      <div>
        <div class="card" *ngIf="result" style="margin-bottom:18px;">
          <div class="row">
            <h3 style="margin:0;">ATS Result</h3>
            <div class="spacer"></div>
            <div class="ring" [style.background]="ringBg(result.matchScore || 0)">
              <span [style.color]="scoreColor(result.matchScore || 0)">{{ result.matchScore ?? '—' }}%</span>
            </div>
          </div>
          <pre class="mono">{{ result.reasonToApply }}</pre>
          <div><strong>Matched:</strong> <span class="chip green" *ngFor="let k of result.matchKeywords">{{ k }}</span></div>
          <div style="margin-top:6px;"><strong>Missing:</strong> <span class="chip red" *ngFor="let k of result.missingKeywords">{{ k }}</span></div>
        </div>

        <div class="card" *ngIf="matches.length" style="margin-bottom:18px;">
          <h3>🏆 Best Resume for this Job</h3>
          <div class="match" *ngFor="let m of matches" [class.rec]="m.recommended" (click)="selected = m">
            <div class="row">
              <input type="radio" name="rm" [checked]="selected?.resumeId === m.resumeId" />
              <strong>{{ m.resumeName }}</strong>
              <span *ngIf="m.recommended" class="chip green">Recommended</span>
              <div class="spacer"></div>
              <span class="chip"
                [class.green]="m.score >= 80" [class.yellow]="m.score >= 60 && m.score < 80" [class.red]="m.score < 60">
                {{ m.score }}%
              </span>
            </div>
            <div class="muted" style="font-size:12px;margin-top:4px;">{{ m.bestResumeAngle }}</div>
          </div>
        </div>

        <div class="card" *ngIf="result && selected">
          <div class="row">
            <h3 style="margin:0;">✍️ Cover Letter</h3>
            <div class="spacer"></div>
            <button class="btn small" (click)="generateCover()">Generate</button>
            <button class="btn secondary small" *ngIf="coverLetter" (click)="copyCover()">Copy</button>
          </div>
          <textarea *ngIf="coverLetter" [(ngModel)]="coverLetter" rows="12" style="width:100%;margin-top:10px;"></textarea>
          <div *ngIf="!coverLetter" class="muted" style="font-size:13px;margin-top:8px;">
            Pick a resume above, then Generate a tailored cover letter for {{ result.company }}.
          </div>
        </div>

        <div class="card" *ngIf="result && selected" style="margin-top:18px;">
          <div class="row">
            <h3 style="margin:0;">💬 Screening Answers</h3>
            <div class="spacer"></div>
            <button class="btn small" (click)="generateAnswers()">Generate</button>
          </div>
          <div *ngFor="let qa of answers" class="qa">
            <div class="q">{{ qa.question }}</div>
            <div class="a">{{ qa.answer }}</div>
          </div>
          <div *ngIf="!answers.length" class="muted" style="font-size:13px;margin-top:8px;">
            Draft answers to common screening questions — review & edit before submitting.
          </div>
        </div>

        <div class="card" *ngIf="!result && !matches.length">
          <div class="empty"><span class="big">📋</span>Import a job or find the best resume to see results here.</div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .match { border:1px solid #eef2f7; border-radius:12px; padding:12px; margin-bottom:10px; cursor:pointer; transition:border .15s, background .15s; }
    .match:hover { background:#f8fafc; }
    .match.rec { border-color:#16a34a; background:#f0fdf4; }
    .qa { padding:10px 0; border-bottom:1px solid #f1f5f9; }
    .qa .q { font-weight:600; font-size:13px; color:#334155; }
    .qa .a { font-size:13px; color:#475569; margin-top:3px; }
  `]
})
export class JobImportComponent implements OnInit {
  private jobService = inject(JobService);
  private criteriaService = inject(CriteriaService);
  private packService = inject(PackService);
  private toast = inject(ToastService);
  private router = inject(Router);

  criteria: JobCriteria[] = [];
  model: Partial<Job> & { criteriaId?: string } = { platform: 'linkedin' };
  result?: Job;
  matches: ResumeMatch[] = [];
  selected?: ResumeMatch;
  coverLetter = '';
  answers: { question: string; answer: string }[] = [];

  ngOnInit(): void {
    this.criteriaService.list().subscribe(c => (this.criteria = c));
  }

  submit(): void {
    if (!this.model.description) { this.toast.error('Job description is required'); return; }
    this.jobService.import(this.model).subscribe({
      next: j => { this.result = j; this.toast.success('Job imported' + (j.matchScore != null ? ` · ${j.matchScore}%` : '')); },
      error: e => this.toast.error(e?.error?.message || 'Import failed')
    });
  }

  findBestResume(): void {
    const body = this.result ? { jobId: this.result.id } : { jobDescription: this.model.description };
    this.packService.bestResume(body).subscribe({
      next: m => {
        this.matches = m;
        this.selected = m.find(x => x.recommended) || m[0];
        this.toast.success('Ranked ' + m.length + ' resumes');
      },
      error: e => this.toast.error(e?.error?.message || 'Could not rank resumes')
    });
  }

  generateCover(): void {
    if (!this.result || !this.selected) { this.toast.error('Import the job and pick a resume first'); return; }
    this.packService.coverLetter(this.result.id, this.selected.resumeId).subscribe({
      next: r => { this.coverLetter = r.coverLetter; this.toast.success('Cover letter generated'); },
      error: e => this.toast.error(e?.error?.message || 'Generation failed')
    });
  }

  copyCover(): void {
    navigator.clipboard.writeText(this.coverLetter).then(() => this.toast.success('Copied to clipboard'));
  }

  generateAnswers(): void {
    if (!this.result || !this.selected) { this.toast.error('Import the job and pick a resume first'); return; }
    this.packService.answers(this.result.id, this.selected.resumeId).subscribe({
      next: a => { this.answers = a; this.toast.success('Generated ' + a.length + ' answers'); },
      error: e => this.toast.error(e?.error?.message || 'Generation failed')
    });
  }

  scoreColor(v: number): string { return v >= 80 ? '#15803d' : v >= 60 ? '#a16207' : '#b91c1c'; }
  ringBg(v: number): string {
    const c = this.scoreColor(v);
    return `conic-gradient(${c} ${v * 3.6}deg, #f1f5f9 0deg)`;
  }
}


