import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InterviewRef, InterviewService, PrepPack } from '../../core/services/interview.service';

@Component({
  selector: 'app-interviews',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <header class="masthead">
      <div>
        <div class="kicker">Prepare</div>
        <h1 class="display">Interview center</h1>
        <p class="lede">Every interview in your pipeline, with a suggested prep pack built from the role's skills.</p>
      </div>
    </header>

    <div class="empty" *ngIf="!loading() && !list().length">
      <span class="big">✦</span>
      <div style="font-weight:600;color:var(--ink);margin-bottom:4px;">No interviews yet</div>
      <div style="max-width:46ch;margin:0 auto;">
        When a company moves you to the interview stage in your pipeline, it will appear
        here with a preparation workspace.
      </div>
    </div>

    <div class="iv-layout" *ngIf="list().length">
      <!-- List rail -->
      <aside class="iv-rail">
        <div class="rail-group" *ngIf="upcoming().length">
          <div class="kicker">Upcoming</div>
          <button *ngFor="let i of upcoming()" class="iv-item" [class.active]="selected()?.applicationId === i.applicationId"
                  (click)="select(i)">
            <span class="iv-company">{{ i.company || 'Company' }}</span>
            <span class="iv-role">{{ i.role }}</span>
            <span class="iv-when">{{ when(i) }}</span>
          </button>
        </div>
        <div class="rail-group" *ngIf="others().length">
          <div class="kicker">Other</div>
          <button *ngFor="let i of others()" class="iv-item" [class.active]="selected()?.applicationId === i.applicationId"
                  (click)="select(i)">
            <span class="iv-company">{{ i.company || 'Company' }}</span>
            <span class="iv-role">{{ i.role }}</span>
            <span class="iv-when">{{ when(i) }}</span>
          </button>
        </div>
      </aside>

      <!-- Prep workspace -->
      <section class="iv-prep" *ngIf="prep() as p">
        <div class="prep-head">
          <div>
            <h2 style="font-family:var(--font-display);">{{ p.role }}</h2>
            <div class="muted">{{ p.company }}<span *ngIf="p.round"> · Round {{ p.round }}</span>
              <span *ngIf="p.scheduledAt"> · {{ p.scheduledAt | date:'EEE d MMM, h:mm a' }}</span></div>
          </div>
        </div>

        <div class="prep-note">{{ p.note }}</div>

        <div class="prep-grid">
          <div class="prep-card">
            <div class="kicker">Technical topics</div>
            <div class="chips" *ngIf="p.technicalTopics.length; else noTopics">
              <span class="chip green" *ngFor="let t of p.technicalTopics">{{ t }}</span>
            </div>
            <ng-template #noTopics><p class="muted small">No skills captured for this application yet.</p></ng-template>
          </div>

          <div class="prep-card">
            <div class="kicker">Prep checklist</div>
            <label class="check" *ngFor="let c of checklist()">
              <input type="checkbox" [(ngModel)]="c.done" (change)="persist()" />
              <span [class.done]="c.done">{{ c.label }}</span>
            </label>
          </div>

          <div class="prep-card wide">
            <div class="kicker">Likely technical questions</div>
            <ul class="qlist"><li *ngFor="let q of p.likelyQuestions">{{ q }}</li></ul>
          </div>

          <div class="prep-card">
            <div class="kicker">Behavioural questions</div>
            <ul class="qlist"><li *ngFor="let q of p.behavioralQuestions">{{ q }}</li></ul>
          </div>

          <div class="prep-card">
            <div class="kicker">Questions to ask them</div>
            <ul class="qlist"><li *ngFor="let q of p.questionsToAsk">{{ q }}</li></ul>
          </div>
        </div>
      </section>
    </div>
  `,
  styles: [`
    .masthead { margin-bottom:8px; }
    .masthead .lede { color:var(--ink-2); font-size:15px; margin:8px 0 0; max-width:60ch; }
    .iv-layout { display:grid; grid-template-columns:260px 1fr; gap:32px; margin-top:24px; align-items:start; }
    .iv-rail { position:sticky; top:78px; display:flex; flex-direction:column; gap:20px; }
    .rail-group .kicker { margin-bottom:8px; }
    .iv-item { display:flex; flex-direction:column; gap:1px; width:100%; text-align:left; background:transparent;
      border:1px solid var(--line); border-radius:var(--radius-sm); padding:10px 12px; cursor:pointer; margin-bottom:6px; }
    .iv-item:hover { border-color:var(--line-strong); }
    .iv-item.active { border-color:var(--accent); background:var(--accent-wash); }
    .iv-company { font-weight:600; color:var(--ink); font-size:14px; }
    .iv-role { font-size:12.5px; color:var(--ink-2); }
    .iv-when { font-size:11.5px; color:var(--ink-3); font-family:var(--font-mono); margin-top:2px; }
    .prep-note { background:var(--info-wash); border-left:3px solid var(--info); border-radius:0 var(--radius-sm) var(--radius-sm) 0;
      padding:12px 14px; font-size:13px; color:var(--ink); margin:14px 0 20px; max-width:72ch; }
    .prep-grid { display:grid; grid-template-columns:1fr 1fr; gap:16px; }
    .prep-card { border:1px solid var(--line); border-radius:var(--radius); padding:16px; background:var(--surface); }
    .prep-card.wide { grid-column:1 / -1; }
    .prep-card .kicker { margin-bottom:10px; }
    .chips { display:flex; flex-wrap:wrap; gap:4px; }
    .qlist { margin:0; padding-left:18px; }
    .qlist li { margin:6px 0; color:var(--ink); font-size:13.5px; }
    .check { display:flex; align-items:center; gap:8px; padding:5px 0; font-size:13.5px; color:var(--ink); }
    .check .done { text-decoration:line-through; color:var(--ink-3); }
    .small { font-size:12.5px; }
    @media (max-width:900px){ .iv-layout{ grid-template-columns:1fr; } .iv-rail{ position:static; } .prep-grid{ grid-template-columns:1fr; } }
  `]
})
export class InterviewsPageComponent implements OnInit {
  private svc = inject(InterviewService);

  list = signal<InterviewRef[]>([]);
  selected = signal<InterviewRef | null>(null);
  prep = signal<PrepPack | null>(null);
  loading = signal(true);

  upcoming = computed(() => this.list().filter(i => i.upcoming));
  others = computed(() => this.list().filter(i => !i.upcoming));
  checklist = computed(() => this.prep()?.checklist ?? []);

  ngOnInit(): void {
    this.svc.list().subscribe({
      next: l => { this.list.set(l || []); this.loading.set(false); if (l?.length) this.select(l[0]); },
      error: () => this.loading.set(false),
    });
  }

  select(i: InterviewRef): void {
    this.selected.set(i);
    this.svc.prep(i.applicationId).subscribe({
      next: p => { this.applySavedChecklist(p); this.prep.set(p); },
      error: () => {},
    });
  }

  when(i: InterviewRef): string {
    if (!i.scheduledAt) return 'Date to confirm';
    const d = new Date(i.scheduledAt).getTime();
    const days = Math.round((d - Date.now()) / 86400000);
    if (days < 0) return `${Math.abs(days)}d ago`;
    if (days === 0) return 'Today';
    if (days === 1) return 'Tomorrow';
    return `in ${days} days`;
  }

  // Checklist is a local convenience (per application) — persisted to localStorage only.
  private key(id: string): string { return `jobpilot.iv.checklist.${id}`; }
  private applySavedChecklist(p: PrepPack): void {
    try {
      const saved = JSON.parse(localStorage.getItem(this.key(p.applicationId)) || '{}');
      p.checklist.forEach(c => { if (saved[c.label] != null) c.done = saved[c.label]; });
    } catch { /* ignore */ }
  }
  persist(): void {
    const p = this.prep();
    if (!p) return;
    const map: Record<string, boolean> = {};
    p.checklist.forEach(c => (map[c.label] = c.done));
    localStorage.setItem(this.key(p.applicationId), JSON.stringify(map));
  }
}



