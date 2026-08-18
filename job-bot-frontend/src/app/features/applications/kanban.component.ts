import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApplicationService } from '../../core/services/application.service';
import { ToastService } from '../../core/services/toast.service';
import { Application } from '../../core/models';

interface Stage {
  key: string;
  name: string;
  statuses: string[];
  tone: 'info' | 'warning' | 'accent' | 'success' | 'danger';
  apps: Application[];
}

@Component({
  selector: 'app-kanban',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <header class="masthead">
      <div>
        <div class="kicker">Pipeline</div>
        <h1 class="display">Career progression</h1>
        <p class="lede">Every application, from applied to offer. Expand a stage to act on what's inside.</p>
      </div>
      <div class="masthead-actions">
        <span class="total"><span class="numeric">{{ total() }}</span> in play</span>
      </div>
    </header>

    <div class="empty" *ngIf="!total()">
      <span class="big">✦</span>
      <div style="font-weight:600;color:var(--ink);margin-bottom:4px;">Your pipeline is empty</div>
      <div style="max-width:46ch;margin:0 auto;">
        When you apply — automatically from the queue or by marking a manual application done —
        it will appear here and move through the stages as things progress.
      </div>
    </div>

    <!-- Progression rail -->
    <div class="progression" *ngIf="total()">
      <div class="stage-node" *ngFor="let st of stages(); let i = index; let last = last"
           [class.expanded]="open() === st.key">
        <div class="spine">
          <span class="node" [class]="'node ' + st.tone" [class.filled]="st.apps.length > 0"></span>
          <span class="rail-line" *ngIf="!last"></span>
        </div>

        <div class="stage-content">
          <button class="stage-header" (click)="toggle(st.key)" [attr.aria-expanded]="open() === st.key">
            <span class="stage-name">{{ st.name }}</span>
            <span class="stage-num numeric" [class.zero]="!st.apps.length">{{ st.apps.length }}</span>
            <span class="stage-caret" *ngIf="st.apps.length">{{ open() === st.key ? '▾' : '▸' }}</span>
            <span class="stage-hint" *ngIf="st.apps.length && open() !== st.key">{{ preview(st) }}</span>
          </button>

          <div class="stage-list" *ngIf="open() === st.key && st.apps.length">
            <button class="prog-row" *ngFor="let app of st.apps" (click)="edit(app)">
              <div class="prog-main">
                <span class="prog-company">{{ app.company || 'Unknown company' }}</span>
                <span class="prog-title">{{ app.title }}</span>
              </div>
              <span class="pf-badge" [class]="'pf-badge ' + pf(app.platform)" *ngIf="app.platform">{{ app.platform }}</span>
              <span class="prog-age">{{ age(app) }}</span>
              <span class="prog-next">{{ nextAction(st.key, app) }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Edit drawer (unchanged behaviour) -->
    <div class="overlay" *ngIf="active" (click)="close()">
      <div class="drawer" (click)="$event.stopPropagation()">
        <div class="row">
          <div>
            <div class="kicker">Application</div>
            <h3 style="margin:2px 0 0;font-family:var(--font-display);">{{ active.company }}</h3>
          </div>
          <div class="spacer"></div>
          <button class="iconbtn" (click)="close()">✕</button>
        </div>
        <div class="muted" style="margin:6px 0 18px;">{{ active.title }} · {{ active.platform }}</div>

        <div class="field">
          <label>Stage</label>
          <select [(ngModel)]="active.status">
            <option *ngFor="let s of allStatuses" [value]="s.value">{{ s.label }}</option>
          </select>
        </div>
        <div class="grid cols-2">
          <div class="field"><label>Interview date</label><input type="date" [(ngModel)]="interviewDate" /></div>
          <div class="field"><label>Round</label><input type="number" [(ngModel)]="active.interviewRound" /></div>
        </div>
        <div class="field">
          <label>Notes</label>
          <textarea [(ngModel)]="active.notes" rows="5" placeholder="Recruiter, follow-ups, feedback…"></textarea>
        </div>
        <div class="row">
          <button class="btn" (click)="save()">Save changes</button>
          <button class="btn danger" (click)="remove()">Delete</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .masthead { display:flex; align-items:flex-end; justify-content:space-between; gap:24px; margin-bottom:8px; }
    .masthead .lede { color:var(--ink-2); font-size:15px; margin:8px 0 0; max-width:60ch; }
    .total { font-size:14px; color:var(--ink-2); }
    .total .numeric { font-size:22px; font-weight:600; color:var(--ink); margin-right:4px; }

    .progression { margin-top:28px; max-width:760px; }
    .stage-node { display:flex; gap:18px; }
    .spine { display:flex; flex-direction:column; align-items:center; width:16px; flex-shrink:0; }
    .node { width:14px; height:14px; border-radius:50%; border:2px solid var(--line-strong); background:var(--surface); margin-top:6px; flex-shrink:0; }
    .node.filled.info    { border-color:var(--info);    background:var(--info); }
    .node.filled.warning { border-color:var(--warning); background:var(--warning); }
    .node.filled.accent  { border-color:var(--accent);  background:var(--accent); }
    .node.filled.success { border-color:var(--success); background:var(--success); }
    .node.filled.danger  { border-color:var(--danger);  background:var(--danger); }
    .rail-line { flex:1; width:2px; background:var(--line); margin:4px 0; min-height:24px; }

    .stage-content { flex:1; padding-bottom:8px; }
    .stage-header { display:flex; align-items:center; gap:12px; width:100%; background:transparent; border:0;
      cursor:pointer; text-align:left; padding:2px 0 10px; }
    .stage-name { font-family:var(--font-display); font-size:18px; font-weight:600; color:var(--ink); }
    .stage-num { font-size:18px; font-weight:600; color:var(--ink); }
    .stage-num.zero { color:var(--ink-3); }
    .stage-caret { color:var(--ink-3); font-size:12px; }
    .stage-hint { color:var(--ink-3); font-size:13px; margin-left:auto; }

    .stage-list { display:flex; flex-direction:column; border:1px solid var(--line); border-radius:var(--radius);
      overflow:hidden; margin:0 0 16px; background:var(--surface); }
    .prog-row { display:flex; align-items:center; gap:14px; width:100%; background:transparent; border:0;
      border-bottom:1px solid var(--line); cursor:pointer; text-align:left; padding:12px 14px; }
    .prog-row:last-child { border-bottom:0; }
    .prog-row:hover { background:var(--surface-2); }
    .prog-main { display:flex; flex-direction:column; min-width:0; flex:1; }
    .prog-company { font-weight:600; color:var(--ink); font-size:14px; }
    .prog-title { color:var(--ink-3); font-size:12.5px; }
    .prog-age { font-size:12.5px; color:var(--ink-3); font-family:var(--font-mono); min-width:64px; text-align:right; }
    .prog-next { font-size:12px; color:var(--accent-deep); min-width:120px; text-align:right; }

    .overlay { position:fixed; inset:0; background:rgba(32,30,27,.4); display:flex; justify-content:flex-end; z-index:900; animation:fade .14s; }
    .drawer { width:440px; max-width:94vw; background:var(--surface); height:100%; padding:24px; overflow:auto;
      border-left:1px solid var(--line-strong); box-shadow:-14px 0 44px rgba(32,30,27,.16); animation:slide .18s ease; }
    .iconbtn { background:transparent; border:1px solid var(--line-strong); color:var(--ink-2); border-radius:var(--radius-sm);
      width:30px; height:30px; cursor:pointer; }
    @keyframes fade { from { opacity:0; } to { opacity:1; } }
    @keyframes slide { from { transform:translateX(30px); } to { transform:none; } }

    @media (max-width: 640px) {
      .prog-next { display:none; }
      .masthead { flex-direction:column; align-items:stretch; }
    }
  `]
})
export class KanbanComponent implements OnInit {
  private service = inject(ApplicationService);
  private toast = inject(ToastService);

  private stageDefs: Omit<Stage, 'apps'>[] = [
    { key: 'applied',   name: 'Applied',    statuses: ['applied'], tone: 'info' },
    { key: 'screening', name: 'Screening',  statuses: ['viewed', 'shortlisted'], tone: 'warning' },
    { key: 'interview', name: 'Interview',  statuses: ['interview'], tone: 'accent' },
    { key: 'offer',     name: 'Offer',      statuses: ['offer'], tone: 'success' },
    { key: 'closed',    name: 'Closed',     statuses: ['rejected', 'withdrawn'], tone: 'danger' },
  ];
  allStatuses = [
    { value: 'applied', label: 'Applied' },
    { value: 'viewed', label: 'Viewed' },
    { value: 'shortlisted', label: 'Shortlisted' },
    { value: 'interview', label: 'Interview' },
    { value: 'offer', label: 'Offer' },
    { value: 'rejected', label: 'Rejected' },
    { value: 'withdrawn', label: 'Withdrawn' },
  ];

  board = signal<Record<string, Application[]>>({});
  open = signal<string>('interview');
  active?: Application;
  interviewDate = '';

  ngOnInit(): void { this.load(); }
  load(): void { this.service.kanban().subscribe(b => this.board.set(b || {})); }

  stages = computed<Stage[]>(() => {
    const b = this.board();
    return this.stageDefs.map(def => ({
      ...def,
      apps: def.statuses.flatMap(s => b[s] || []),
    }));
  });
  total = computed(() => this.stages().reduce((n, s) => n + s.apps.length, 0));

  toggle(key: string): void { this.open.set(this.open() === key ? '' : key); }

  preview(st: Stage): string {
    const names = st.apps.slice(0, 2).map(a => a.company || 'Unknown').join(', ');
    const more = st.apps.length - 2;
    return more > 0 ? `${names} +${more} more` : names;
  }
  age(a: Application): string {
    if (!a.appliedAt) return '—';
    const days = Math.floor((Date.now() - new Date(a.appliedAt).getTime()) / 86400000);
    return days <= 0 ? 'today' : `${days}d`;
  }
  nextAction(stage: string, a: Application): string {
    switch (stage) {
      case 'applied': {
        const days = a.appliedAt ? Math.floor((Date.now() - new Date(a.appliedAt).getTime()) / 86400000) : 0;
        return days >= 5 ? 'Follow up' : 'Awaiting response';
      }
      case 'screening': return 'Prep for interview';
      case 'interview': return a.interviewDate ? 'Interview set' : 'Confirm date';
      case 'offer': return 'Review offer';
      default: return '';
    }
  }
  pf(p?: string): string {
    const l = (p || '').toLowerCase();
    return ['naukri', 'linkedin', 'indeed'].includes(l) ? l : 'other';
  }

  edit(app: Application): void {
    this.active = { ...app };
    this.interviewDate = app.interviewDate ? app.interviewDate.substring(0, 10) : '';
  }
  close(): void { this.active = undefined; }

  save(): void {
    if (!this.active) return;
    const a = this.active;
    this.service.updateStatus(a.id, a.status, a.notes).subscribe(() => {
      if (this.interviewDate) {
        this.service.setInterview(a.id, this.interviewDate + 'T09:00:00Z', a.interviewRound || 1)
          .subscribe(() => this.finishSave());
      } else { this.finishSave(); }
    });
  }
  private finishSave(): void { this.toast.success('Application updated'); this.close(); this.load(); }

  remove(): void {
    if (!this.active) return;
    if (!confirm('Delete this application?')) return;
    this.service.remove(this.active.id).subscribe(() => {
      this.toast.success('Application deleted'); this.close(); this.load();
    });
  }
}


