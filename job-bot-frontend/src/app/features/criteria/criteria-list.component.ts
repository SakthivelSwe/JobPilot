import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CriteriaService } from '../../core/services/criteria.service';
import { ToastService } from '../../core/services/toast.service';
import { JobCriteria } from '../../core/models';

@Component({
  selector: 'app-criteria-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <header class="masthead">
      <div>
        <div class="kicker">Search strategy</div>
        <h1 class="display">Criteria</h1>
        <p class="lede">The rules that shape discovery and matching. Keep the ones you're actively hunting with switched on.</p>
      </div>
      <div class="masthead-actions">
        <a class="btn secondary" routerLink="/target-roles">Target roles</a>
        <a class="btn" routerLink="/criteria/new">Add criteria</a>
      </div>
    </header>

    <div class="clist" *ngIf="items.length; else empty">
      <div class="crow" *ngFor="let c of items">
        <div class="cmark" [class.on]="c.active"></div>
        <div class="cmain">
          <div class="ctop">
            <span class="cname">{{ c.name }}</span>
            <span class="chip" [class.green]="c.active" [class.gray]="!c.active">{{ c.active ? 'Active' : 'Off' }}</span>
          </div>
          <div class="cmeta">
            {{ c.locations && c.locations.length ? c.locations.join(', ') : 'Any location' }} ·
            {{ c.experienceMin }}–{{ c.experienceMax }} yrs ·
            min match {{ c.minMatchScore }}
          </div>
          <div class="cskills">
            <span class="chip" *ngFor="let k of (c.keywords || []).slice(0, 8)">{{ k }}</span>
          </div>
          <div class="cbool mono" *ngIf="c.booleanQuery">⌗ {{ c.booleanQuery }}</div>
        </div>
        <div class="cactions">
          <a class="btn secondary small" [routerLink]="['/criteria', c.id]">Edit</a>
          <button class="btn ghost small" (click)="toggle(c)">{{ c.active ? 'Turn off' : 'Turn on' }}</button>
          <button class="btn ghost small" (click)="remove(c)">Delete</button>
        </div>
      </div>
    </div>
    <ng-template #empty>
      <div class="panel"><div class="empty">
        <span class="big">✦</span>
        <div style="font-weight:600;color:var(--ink);margin-bottom:4px;">No criteria yet</div>
        <div style="max-width:48ch;margin:0 auto 14px;">Criteria set the minimum match score and keywords discovery filters against.</div>
        <a class="btn small" routerLink="/criteria/new">Add your first criteria</a>
      </div></div>
    </ng-template>
  `,
  styles: [`
    .masthead { display:flex; align-items:flex-end; justify-content:space-between; gap:24px; margin-bottom:8px; }
    .masthead .lede { color:var(--ink-2); font-size:15px; margin:8px 0 0; max-width:64ch; }
    .masthead-actions { display:flex; gap:10px; flex-shrink:0; }
    .clist { margin-top:24px; display:flex; flex-direction:column; border:1px solid var(--line); border-radius:var(--radius); overflow:hidden; background:var(--surface); }
    .crow { display:flex; align-items:flex-start; gap:16px; padding:18px 20px; border-bottom:1px solid var(--line); }
    .crow:last-child { border-bottom:0; } .crow:hover { background:var(--surface-2); }
    .cmark { width:3px; align-self:stretch; border-radius:2px; background:var(--line-strong); flex-shrink:0; }
    .cmark.on { background:var(--accent); }
    .cmain { flex:1; min-width:0; }
    .ctop { display:flex; align-items:center; gap:10px; }
    .cname { font-family:var(--font-display); font-size:18px; font-weight:600; color:var(--ink); }
    .cmeta { color:var(--ink-2); font-size:13px; margin:3px 0 8px; }
    .cskills { display:flex; flex-wrap:wrap; gap:4px; }
    .cbool { margin-top:8px; font-size:12px; color:var(--accent-deep); background:var(--accent-wash); padding:4px 8px; border-radius:6px; display:inline-block; }
    .cactions { display:flex; gap:6px; flex-shrink:0; }
    @media (max-width:720px){ .masthead{ flex-direction:column; align-items:stretch; } }
  `]
})
export class CriteriaListComponent implements OnInit {
  private service = inject(CriteriaService);
  private toast = inject(ToastService);
  items: JobCriteria[] = [];

  ngOnInit(): void { this.load(); }

  load(): void { this.service.list().subscribe(i => (this.items = i)); }

  toggle(c: JobCriteria): void {
    this.service.toggle(c.id).subscribe(() => { this.toast.success('Criteria updated'); this.load(); });
  }

  remove(c: JobCriteria): void {
    if (confirm(`Delete criteria "${c.name}"?`)) {
      this.service.remove(c.id).subscribe(() => { this.toast.success('Criteria deleted'); this.load(); });
    }
  }
}


