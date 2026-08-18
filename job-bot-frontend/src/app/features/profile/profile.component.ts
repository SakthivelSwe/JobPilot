import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CandidateProfile, CandidateService, ConfirmProfile, ParsedResume } from '../../core/services/candidate.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <header class="masthead">
      <div>
        <div class="kicker">Candidate</div>
        <h1 class="display">Your profile</h1>
        <p class="lede">
          The verified source of truth for matching. Upload a résumé to auto-detect the
          facts, then confirm — nothing is saved until you verify it.
        </p>
      </div>
    </header>

    <!-- Upload zone -->
    <div class="panel upload" *ngIf="!parsed() && !editing()">
      <div class="upload-inner">
        <div class="upload-title">Drop a résumé to get started</div>
        <div class="upload-sub">PDF, DOC, DOCX or TXT — parsed locally, never sent to a third party.</div>
        <label class="btn">
          Choose file
          <input type="file" accept=".pdf,.doc,.docx,.txt" hidden (change)="onFile($event)" />
        </label>
        <div class="upload-or">or</div>
        <button class="btn secondary small" (click)="startBlank()">Fill in manually</button>
        <div class="parsing" *ngIf="parsing()">Parsing résumé…</div>
      </div>
    </div>

    <!-- Detected preview banner -->
    <div class="detected-banner" *ngIf="parsed() && editing()">
      <span class="dot busy"></span>
      Detected from <strong>{{ parsed()!.fileName }}</strong> — review each field, then confirm.
      Nothing is saved until you press <strong>Save profile</strong>.
    </div>

    <!-- Editor -->
    <div class="panel editor" *ngIf="editing() as p">
      <div class="section-head"><h2>Identity</h2></div>
      <div class="grid cols-2">
        <div class="field"><label>Full name</label><input [(ngModel)]="p.name" /></div>
        <div class="field"><label>Email</label><input [(ngModel)]="p.email" /></div>
        <div class="field"><label>Phone</label><input [(ngModel)]="p.phone" /></div>
        <div class="field"><label>Current location</label><input [(ngModel)]="p.currentLocation" /></div>
      </div>

      <div class="section-head"><h2>Experience & availability</h2></div>
      <div class="grid cols-2">
        <div class="field"><label>Years of experience</label>
          <input type="number" step="0.1" [(ngModel)]="p.yearsOfExperience" /></div>
        <div class="field"><label>Notice period (days)</label>
          <input type="number" [(ngModel)]="p.noticePeriodDays" /></div>
        <div class="field"><label>Expected salary (LPA)</label>
          <input type="number" [(ngModel)]="p.expectedSalary" /></div>
        <div class="field"><label>Minimum salary (LPA)</label>
          <input type="number" [(ngModel)]="p.minimumSalary" /></div>
        <div class="field"><label>Remote preference</label>
          <select [(ngModel)]="p.remotePreference">
            <option value="ANY">Any</option><option value="REMOTE">Remote</option>
            <option value="HYBRID">Hybrid</option><option value="ONSITE">Onsite</option>
          </select></div>
        <div class="field"><label>Work authorization</label><input [(ngModel)]="p.workAuthorization" /></div>
      </div>
      <div class="field"><label>Preferred locations (comma-separated)</label>
        <input [ngModel]="join(p.preferredLocations)" (ngModelChange)="p.preferredLocations = split($event)"
               placeholder="Chennai, Bangalore, Remote" /></div>

      <div class="section-head"><h2>Skills</h2></div>
      <div class="skills-grid" *ngIf="skills().length">
        <div class="skill" *ngFor="let s of skills(); let i = index">
          <input class="skill-name" [(ngModel)]="s.name" placeholder="Skill" />
          <select class="skill-prof" [(ngModel)]="s.proficiency">
            <option *ngFor="let lvl of levels" [value]="lvl">{{ lvl | titlecase }}</option>
          </select>
          <button class="btn ghost small" (click)="removeSkill(i)">×</button>
        </div>
      </div>
      <button class="btn secondary small" (click)="addSkill()" style="margin-top:8px;">＋ Add skill</button>
      <p class="note">Proficiency is never auto-upgraded to Expert — you set it. Evidence from your résumé is preserved.</p>

      <div class="field" style="margin-top:20px;"><label>Summary</label>
        <textarea rows="3" [(ngModel)]="p.summary"></textarea></div>

      <div class="row" style="margin-top:8px;">
        <button class="btn" (click)="save()" [disabled]="!p.name">Save profile</button>
        <button class="btn secondary" (click)="reset()">Start over</button>
      </div>
    </div>

    <!-- Saved view -->
    <div class="panel saved" *ngIf="saved() && !editing()">
      <div class="row">
        <div>
          <div class="kicker" style="color:var(--success);">✓ Verified profile</div>
          <h2 style="font-family:var(--font-display);margin-top:4px;">{{ saved()!.name }}</h2>
          <div class="muted">{{ saved()!.email }} · {{ saved()!.currentLocation }}</div>
        </div>
        <div class="spacer"></div>
        <button class="btn secondary small" (click)="editSaved()">Edit</button>
      </div>
      <div class="saved-facts">
        <div class="fact"><span class="fact-n numeric">{{ saved()!.yearsOfExperience ?? '—' }}</span><span class="fact-l">yrs experience</span></div>
        <div class="fact"><span class="fact-n numeric">{{ saved()!.noticePeriodDays ?? '—' }}</span><span class="fact-l">day notice</span></div>
        <div class="fact"><span class="fact-n numeric">{{ saved()!.expectedSalary ?? '—' }}</span><span class="fact-l">LPA expected</span></div>
        <div class="fact"><span class="fact-n numeric">{{ skills().length }}</span><span class="fact-l">skills</span></div>
      </div>
    </div>
  `,
  styles: [`
    .masthead { margin-bottom:8px; }
    .masthead .lede { color:var(--ink-2); font-size:15px; margin:8px 0 0; max-width:64ch; }
    .upload { margin-top:24px; padding:48px 24px; text-align:center; border-style:dashed; }
    .upload-title { font-family:var(--font-display); font-size:20px; font-weight:600; color:var(--ink); }
    .upload-sub { color:var(--ink-2); font-size:13.5px; margin:6px 0 18px; }
    .upload .btn { cursor:pointer; }
    .upload-or { color:var(--ink-3); font-size:12px; margin:12px 0; }
    .parsing { margin-top:16px; color:var(--accent-deep); font-size:14px; }
    .detected-banner { display:flex; align-items:center; gap:8px; margin-top:24px; padding:12px 16px;
      background:var(--info-wash); border-radius:var(--radius-sm); font-size:13.5px; color:var(--ink); }
    .editor { margin-top:16px; padding:24px; max-width:760px; }
    .note { font-size:12px; color:var(--ink-3); margin-top:8px; }
    .skills-grid { display:flex; flex-direction:column; gap:8px; }
    .skill { display:flex; gap:8px; align-items:center; }
    .skill-name { flex:1; padding:8px 10px; border:1px solid var(--line-strong); border-radius:var(--radius-sm);
      background:var(--surface); color:var(--ink); font:14px var(--font-sans); }
    .skill-prof { padding:8px; border:1px solid var(--line-strong); border-radius:var(--radius-sm);
      background:var(--surface); color:var(--ink); }
    .saved { margin-top:24px; padding:24px; }
    .saved-facts { display:flex; gap:32px; margin-top:20px; flex-wrap:wrap; }
    .fact { display:flex; flex-direction:column; }
    .fact-n { font-size:26px; font-weight:600; color:var(--ink); }
    .fact-l { font-size:12.5px; color:var(--ink-2); }
  `]
})
export class ProfilePageComponent implements OnInit {
  private svc = inject(CandidateService);
  private toast = inject(ToastService);

  levels = ['UNKNOWN', 'LEARNING', 'BEGINNER', 'WORKING', 'STRONG', 'EXPERT'];

  parsed = signal<ParsedResume | null>(null);
  parsing = signal(false);
  editing = signal<ConfirmProfile | null>(null);
  saved = signal<CandidateProfile | null>(null);
  skills = signal<{ name: string; category?: string; proficiency?: string; evidence?: string[] }[]>([]);

  ngOnInit(): void {
    this.svc.getProfile().subscribe({
      next: p => { if (p) { this.saved.set(p); this.loadSkills(); } },
      error: () => {},
    });
  }

  private loadSkills(): void {
    this.svc.skills().subscribe({
      next: s => this.skills.set((s || []).map((x: any) => ({
        name: x.canonicalName || x.name, category: x.category,
        proficiency: x.proficiency || 'UNKNOWN', evidence: x.evidence || [],
      }))),
      error: () => {},
    });
  }

  onFile(ev: Event): void {
    const file = (ev.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.parsing.set(true);
    this.svc.parse(file).subscribe({
      next: p => {
        this.parsing.set(false);
        this.parsed.set(p);
        this.skills.set((p.detectedSkills || []).map(s => ({
          name: s.name, category: s.category, proficiency: s.proficiency || 'UNKNOWN', evidence: s.evidence || [],
        })));
        this.editing.set({
          name: p.detectedName, email: p.detectedEmail, phone: p.detectedPhone,
          summary: p.detectedSummary, remotePreference: 'ANY',
          preferredLocations: [],
          storagePath: p.storagePath, fileName: p.fileName, mimeType: p.mimeType,
          size: p.size, checksum: p.checksum, extractedText: p.rawTextPreview,
        });
      },
      error: () => { this.parsing.set(false); this.toast.error('Could not parse that file — try another format.'); },
    });
  }

  startBlank(): void {
    this.editing.set({ remotePreference: 'ANY', preferredLocations: [] });
    this.skills.set([]);
  }

  editSaved(): void {
    const p = this.saved();
    if (!p) return;
    this.editing.set({ ...p });
  }

  addSkill(): void { this.skills.update(s => [...s, { name: '', proficiency: 'UNKNOWN', evidence: [] }]); }
  removeSkill(i: number): void { this.skills.update(s => s.filter((_, idx) => idx !== i)); }

  save(): void {
    const p = this.editing();
    if (!p || !p.name) return;
    const body: ConfirmProfile = { ...p, skills: this.skills().filter(s => s.name.trim()) };
    this.svc.confirm(body).subscribe({
      next: saved => {
        this.toast.success('Profile saved');
        this.saved.set(saved);
        this.editing.set(null);
        this.parsed.set(null);
        this.loadSkills();
      },
      error: () => this.toast.error('Could not save the profile'),
    });
  }

  reset(): void { this.editing.set(null); this.parsed.set(null); }

  join(a?: string[]): string { return (a || []).join(', '); }
  split(s: string): string[] { return s.split(',').map(x => x.trim()).filter(Boolean); }
}

