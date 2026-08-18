import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TargetRole, TargetRoleService } from '../../core/services/target-role.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-target-roles',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <header class="masthead">
      <div>
        <div class="kicker">Search strategy</div>
        <h1 class="display">Target roles</h1>
        <p class="lede">
          These are what discovery searches for. Rank them by priority and define the
          skills that matter — Naukri, LinkedIn and Indeed searches are built from these.
        </p>
      </div>
      <div class="masthead-actions">
        <button class="btn" (click)="newRole()">Add target role</button>
      </div>
    </header>

    <!-- Empty state -->
    <div class="panel" *ngIf="!loading() && !roles().length && !editing()">
      <div class="empty">
        <span class="big">✦</span>
        <div style="font-weight:600;color:var(--ink);margin-bottom:4px;">No target roles yet</div>
        <div style="max-width:52ch;margin:0 auto 14px;">
          Discovery needs at least one target role to know what to search for. Add your
          first — for example "Java Backend Developer" with required skills Java, Spring Boot.
        </div>
        <button class="btn small" (click)="newRole()">Add your first role</button>
      </div>
    </div>

    <!-- List -->
    <div class="roles" *ngIf="roles().length && !editing()">
      <div class="role-row" *ngFor="let r of roles()">
        <div class="role-rank numeric">{{ r.priority }}</div>
        <div class="role-main">
          <div class="role-title">{{ r.roleTitle }}
            <span class="chip gray" *ngIf="!r.active">inactive</span>
          </div>
          <div class="role-meta">
            <span *ngIf="r.minimumExperience != null || r.maximumExperience != null">
              {{ r.minimumExperience ?? 0 }}–{{ r.maximumExperience ?? '?' }} yrs ·
            </span>
            <span *ngIf="r.locations?.length">{{ r.locations.join(', ') }} · </span>
            <span>{{ r.remotePreference | titlecase }}</span>
          </div>
          <div class="role-skills">
            <span class="chip green" *ngFor="let s of r.requiredSkills">{{ s }}</span>
            <span class="chip" *ngFor="let s of r.preferredSkills">{{ s }}</span>
            <span class="chip red" *ngFor="let s of r.excludedSkills">− {{ s }}</span>
          </div>
        </div>
        <div class="role-actions">
          <button class="btn secondary small" (click)="edit(r)">Edit</button>
          <button class="btn ghost small" (click)="remove(r)">Delete</button>
        </div>
      </div>
    </div>

    <!-- Editor -->
    <div class="panel editor" *ngIf="editing() as e">
      <div class="section-head"><h2>{{ e.id ? 'Edit role' : 'New target role' }}</h2></div>
      <div class="grid cols-2">
        <div class="field"><label>Role title *</label>
          <input [(ngModel)]="e.roleTitle" placeholder="Java Backend Developer" /></div>
        <div class="field"><label>Priority (1 = highest)</label>
          <input type="number" min="1" [(ngModel)]="e.priority" /></div>
      </div>
      <div class="grid cols-2">
        <div class="field"><label>Min experience (yrs)</label>
          <input type="number" min="0" [(ngModel)]="e.minimumExperience" /></div>
        <div class="field"><label>Max experience (yrs)</label>
          <input type="number" min="0" [(ngModel)]="e.maximumExperience" /></div>
      </div>
      <div class="field"><label>Locations (comma-separated)</label>
        <input [ngModel]="join(e.locations)" (ngModelChange)="e.locations = split($event)"
               placeholder="Chennai, Bangalore, Remote" /></div>
      <div class="field"><label>Work mode</label>
        <select [(ngModel)]="e.remotePreference">
          <option value="ANY">Any</option><option value="REMOTE">Remote</option>
          <option value="HYBRID">Hybrid</option><option value="ONSITE">Onsite</option>
        </select></div>
      <div class="field"><label>Required skills (comma-separated)</label>
        <input [ngModel]="join(e.requiredSkills)" (ngModelChange)="e.requiredSkills = split($event)"
               placeholder="Java, Spring Boot, Kafka" /></div>
      <div class="field"><label>Preferred skills</label>
        <input [ngModel]="join(e.preferredSkills)" (ngModelChange)="e.preferredSkills = split($event)"
               placeholder="AWS, Docker, Kubernetes" /></div>
      <div class="field"><label>Excluded skills</label>
        <input [ngModel]="join(e.excludedSkills)" (ngModelChange)="e.excludedSkills = split($event)"
               placeholder="PHP, .NET" /></div>
      <div class="grid cols-2">
        <div class="field"><label>Salary min (LPA)</label>
          <input type="number" [(ngModel)]="e.salaryMinLpa" /></div>
        <div class="field"><label>Salary max (LPA)</label>
          <input type="number" [(ngModel)]="e.salaryMaxLpa" /></div>
      </div>
      <label class="switch"><input type="checkbox" [(ngModel)]="e.active" /> <span>Active (included in discovery)</span></label>
      <div class="row" style="margin-top:18px;">
        <button class="btn" (click)="save()" [disabled]="!e.roleTitle">{{ e.id ? 'Save changes' : 'Create role' }}</button>
        <button class="btn secondary" (click)="cancel()">Cancel</button>
      </div>
    </div>
  `,
  styles: [`
    .masthead { display:flex; align-items:flex-end; justify-content:space-between; gap:24px; margin-bottom:8px; }
    .masthead .lede { color:var(--ink-2); font-size:15px; margin:8px 0 0; max-width:64ch; }
    .roles { margin-top:24px; display:flex; flex-direction:column; border:1px solid var(--line);
      border-radius:var(--radius); overflow:hidden; background:var(--surface); }
    .role-row { display:flex; align-items:flex-start; gap:16px; padding:16px; border-bottom:1px solid var(--line); }
    .role-row:last-child { border-bottom:0; }
    .role-row:hover { background:var(--surface-2); }
    .role-rank { width:32px; height:32px; flex-shrink:0; display:flex; align-items:center; justify-content:center;
      border:1px solid var(--line-strong); border-radius:8px; font-size:15px; font-weight:600; color:var(--accent-deep); }
    .role-main { flex:1; min-width:0; }
    .role-title { font-family:var(--font-display); font-size:17px; font-weight:600; color:var(--ink); }
    .role-meta { color:var(--ink-2); font-size:13px; margin:2px 0 8px; }
    .role-skills { display:flex; flex-wrap:wrap; gap:4px; }
    .role-actions { display:flex; gap:6px; flex-shrink:0; }
    .editor { margin-top:24px; padding:24px; max-width:720px; }
    .switch { display:flex; align-items:center; gap:8px; font-size:13.5px; color:var(--ink-2); font-weight:600; margin-top:6px; }
    @media (max-width: 720px) { .masthead { flex-direction:column; align-items:stretch; } }
  `]
})
export class TargetRolesPageComponent implements OnInit {
  private svc = inject(TargetRoleService);
  private toast = inject(ToastService);

  roles = signal<TargetRole[]>([]);
  editing = signal<TargetRole | null>(null);
  loading = signal(true);

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.svc.list().subscribe({
      next: r => { this.roles.set([...r].sort((a, b) => a.priority - b.priority)); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  newRole(): void {
    this.editing.set({
      roleTitle: '', priority: (this.roles().length + 1),
      requiredSkills: [], preferredSkills: [], excludedSkills: [],
      minimumExperience: null, maximumExperience: null, locations: [],
      remotePreference: 'ANY', salaryMinLpa: null, salaryMaxLpa: null,
      noticePeriodToleranceDays: null, active: true,
    });
  }
  edit(r: TargetRole): void { this.editing.set({ ...r, requiredSkills: [...r.requiredSkills], preferredSkills: [...r.preferredSkills], excludedSkills: [...r.excludedSkills], locations: [...(r.locations || [])] }); }
  cancel(): void { this.editing.set(null); }

  save(): void {
    const e = this.editing();
    if (!e || !e.roleTitle) return;
    const op = e.id ? this.svc.update(e.id, e) : this.svc.create(e);
    op.subscribe({
      next: () => { this.toast.success(e.id ? 'Role updated' : 'Role created'); this.editing.set(null); this.load(); },
      error: () => this.toast.error('Could not save the role'),
    });
  }
  remove(r: TargetRole): void {
    if (!r.id || !confirm(`Delete "${r.roleTitle}"?`)) return;
    this.svc.remove(r.id).subscribe({
      next: () => { this.toast.success('Role deleted'); this.load(); },
      error: () => this.toast.error('Could not delete'),
    });
  }

  join(a?: string[]): string { return (a || []).join(', '); }
  split(s: string): string[] { return s.split(',').map(x => x.trim()).filter(Boolean); }
}


