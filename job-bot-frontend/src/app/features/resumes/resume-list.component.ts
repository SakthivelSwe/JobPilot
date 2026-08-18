import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ResumeService } from '../../core/services/resume.service';
import { ToastService } from '../../core/services/toast.service';
import { Resume } from '../../core/models';

@Component({
  selector: 'app-resume-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <header class="masthead">
      <div>
        <div class="kicker">Library</div>
        <h1 class="display">Résumé studio</h1>
        <p class="lede">Your master profile drives four role-targeted variants. Keep each one focused on the skills that matter for its target.</p>
      </div>
      <div class="masthead-actions">
        <a class="btn secondary" routerLink="/profile">Edit profile</a>
        <a class="btn" routerLink="/resumes/new">Add résumé</a>
      </div>
    </header>

    <div class="rlist" *ngIf="resumes.length; else empty">
      <div class="rrow" *ngFor="let r of resumes">
        <div class="rmark" [class.on]="r.active"></div>
        <div class="rmain">
          <div class="rtop">
            <span class="rname">{{ r.name }}</span>
            <span class="chip" [class.green]="r.active" [class.gray]="!r.active">{{ r.active ? 'Active' : 'Inactive' }}</span>
          </div>
          <div class="rroles" *ngIf="r.targetRoles?.length">
            <span class="muted">Targets:</span>
            <span class="chip gray" *ngFor="let role of r.targetRoles">{{ role }}</span>
          </div>
          <div class="rskills" *ngIf="r.targetSkills?.length">
            <span class="chip green" *ngFor="let s of (r.targetSkills || []).slice(0, 8)">{{ s }}</span>
            <span class="muted small" *ngIf="(r.targetSkills || []).length > 8">+{{ r.targetSkills.length - 8 }} more</span>
          </div>
        </div>
        <div class="ractions">
          <a class="btn secondary small" [routerLink]="['/resumes', r.id]">Edit</a>
          <button class="btn ghost small" (click)="remove(r)">Delete</button>
        </div>
      </div>
    </div>
    <ng-template #empty>
      <div class="panel"><div class="empty">
        <span class="big">✦</span>
        <div style="font-weight:600;color:var(--ink);margin-bottom:4px;">No résumés yet</div>
        <div style="max-width:48ch;margin:0 auto 14px;">Upload a résumé on your profile to auto-build one, or add a variant manually.</div>
        <a class="btn small" routerLink="/resumes/new">Add your first résumé</a>
      </div></div>
    </ng-template>
  `,
  styles: [`
    .masthead { display:flex; align-items:flex-end; justify-content:space-between; gap:24px; margin-bottom:8px; }
    .masthead .lede { color:var(--ink-2); font-size:15px; margin:8px 0 0; max-width:64ch; }
    .masthead-actions { display:flex; gap:10px; flex-shrink:0; }
    .rlist { margin-top:24px; display:flex; flex-direction:column; border:1px solid var(--line); border-radius:var(--radius); overflow:hidden; background:var(--surface); }
    .rrow { display:flex; align-items:flex-start; gap:16px; padding:18px 20px; border-bottom:1px solid var(--line); }
    .rrow:last-child { border-bottom:0; } .rrow:hover { background:var(--surface-2); }
    .rmark { width:3px; align-self:stretch; border-radius:2px; background:var(--line-strong); flex-shrink:0; }
    .rmark.on { background:var(--accent); }
    .rmain { flex:1; min-width:0; }
    .rtop { display:flex; align-items:center; gap:10px; }
    .rname { font-family:var(--font-display); font-size:18px; font-weight:600; color:var(--ink); }
    .rroles { margin:8px 0 4px; display:flex; align-items:center; gap:4px; flex-wrap:wrap; font-size:12.5px; }
    .rskills { display:flex; flex-wrap:wrap; gap:4px; align-items:center; }
    .ractions { display:flex; gap:6px; flex-shrink:0; }
    .small { font-size:12px; }
    @media (max-width:720px){ .masthead{ flex-direction:column; align-items:stretch; } }
  `]
})
export class ResumeListComponent implements OnInit {
  private service = inject(ResumeService);
  private toast = inject(ToastService);
  resumes: Resume[] = [];

  ngOnInit(): void { this.load(); }

  load(): void {
    this.service.list().subscribe(r => (this.resumes = r));
  }

  remove(r: Resume): void {
    if (confirm(`Delete resume "${r.name}"?`)) {
      this.service.remove(r.id).subscribe(() => { this.toast.success('Resume deleted'); this.load(); });
    }
  }
}


