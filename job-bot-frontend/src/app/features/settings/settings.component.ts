import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PlatformConfigService } from '../../core/services/platform-config.service';
import { AiUsageService, AiUsage } from '../../core/services/analytics.service';
import { AccountService } from '../../core/services/account.service';
import { ToastService } from '../../core/services/toast.service';

type Section = 'sources' | 'ai' | 'privacy';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <header class="masthead">
      <div>
        <div class="kicker">Configuration</div>
        <h1 class="display">Settings</h1>
        <p class="lede">Sources, automation limits, AI, and your data. No platform credentials are ever stored here.</p>
      </div>
    </header>

    <div class="settings-layout">
      <!-- Section nav -->
      <nav class="settings-nav">
        <button *ngFor="let s of navItems" class="snav" [class.active]="section() === s.key" (click)="section.set(s.key)">
          <span class="snav-label">{{ s.label }}</span>
          <span class="snav-desc">{{ s.desc }}</span>
        </button>
      </nav>

      <!-- Panel -->
      <section class="settings-panel">
        <!-- ============ Job sources ============ -->
        <ng-container *ngIf="section() === 'sources'">
          <div class="section-head"><h2>Job sources & limits</h2></div>
          <p class="panel-lede">Enable a platform for discovery and cap how many applications the engine sends per day.</p>

          <div class="src-list">
            <div class="src" *ngFor="let p of platforms()">
              <div class="src-id">
                <span class="pf-badge" [class]="'pf-badge ' + pf(p.platformName)">{{ p.platformName }}</span>
                <span class="src-usage numeric" *ngIf="p.currentCountToday != null">{{ p.currentCountToday }}/{{ p.dailyLimit }} today</span>
              </div>
              <div class="src-controls">
                <label class="switch">
                  <input type="checkbox" [(ngModel)]="p.enabled" />
                  <span>{{ p.enabled ? 'Enabled' : 'Disabled' }}</span>
                </label>
                <label class="inline-num">Daily limit
                  <input type="number" [(ngModel)]="p.dailyLimit" min="1" />
                </label>
                <label class="inline-num">Min delay (s)
                  <input type="number" [(ngModel)]="p.minDelaySeconds" min="30" />
                </label>
                <button class="btn small" (click)="save(p)">Save</button>
              </div>
            </div>
          </div>
          <p class="panel-note">
            JobPilot never stores account passwords or session cookies. LinkedIn applications happen
            in your own browser via the extension; Naukri/Indeed via the local engine.
          </p>
        </ng-container>

        <!-- ============ AI ============ -->
        <ng-container *ngIf="section() === 'ai'">
          <div class="section-head"><h2>AI assistance</h2></div>
          <p class="panel-lede">Deterministic engines always run first. AI is optional, contextual, and capped daily.</p>

          <div class="toggle-row">
            <div>
              <div class="tr-label">Provider</div>
              <div class="tr-desc">Set <code>AI_PROVIDER</code> on the backend to activate. This is a local hint.</div>
            </div>
            <select [(ngModel)]="aiProvider" (change)="saveProvider()">
              <option value="noop">None (deterministic only)</option>
              <option value="ollama">Ollama (local)</option>
              <option value="cloudflare">Cloudflare Workers AI</option>
            </select>
          </div>

          <div class="toggle-row" *ngIf="ai() as u">
            <div>
              <div class="tr-label">Daily usage</div>
              <div class="tr-desc">{{ u.used }} of {{ u.dailyLimit }} calls used today · {{ u.remaining }} remaining</div>
            </div>
            <div class="usage-meter">
              <div class="usage-track"><div class="usage-fill" [style.width.%]="pct(u.used, u.dailyLimit)"></div></div>
            </div>
          </div>
          <div class="empty" *ngIf="!ai()" style="text-align:left;padding:16px 0;">
            AI usage is unavailable right now — the deterministic engines are unaffected.
          </div>
        </ng-container>

        <!-- ============ Privacy ============ -->
        <ng-container *ngIf="section() === 'privacy'">
          <div class="section-head"><h2>Data & privacy</h2></div>
          <p class="panel-lede">Your career data belongs to you. Export it any time or wipe it entirely.</p>

          <div class="toggle-row">
            <div>
              <div class="tr-label">Export</div>
              <div class="tr-desc">Download your applications, jobs, or a full JSON backup.</div>
            </div>
            <div class="row" style="gap:8px;flex-wrap:wrap;">
              <button class="btn secondary small" (click)="exportFile('/api/account/export/applications.csv','applications.csv')">Applications</button>
              <button class="btn secondary small" (click)="exportFile('/api/account/export/jobs.csv','jobs.csv')">Jobs</button>
              <button class="btn secondary small" (click)="exportFile('/api/account/export/data.json','jobpilot-export.json')">Full backup</button>
            </div>
          </div>

          <div class="toggle-row danger-row">
            <div>
              <div class="tr-label">Reset all data</div>
              <div class="tr-desc">Deletes your profile, criteria, target roles, discovered jobs, queue and applications. Source configuration is kept. This cannot be undone.</div>
            </div>
            <button class="btn danger small" (click)="reset()">Reset my data</button>
          </div>
        </ng-container>
      </section>
    </div>
  `,
  styles: [`
    .masthead { margin-bottom:8px; }
    .masthead .lede { color:var(--ink-2); font-size:15px; margin:8px 0 0; max-width:66ch; }

    .settings-layout { display:grid; grid-template-columns:230px 1fr; gap:36px; margin-top:28px; align-items:start; }
    .settings-nav { display:flex; flex-direction:column; gap:2px; position:sticky; top:78px; }
    .snav { display:flex; flex-direction:column; gap:1px; text-align:left; background:transparent; border:0;
      border-left:2px solid transparent; padding:10px 14px; cursor:pointer; border-radius:0 var(--radius-sm) var(--radius-sm) 0; }
    .snav:hover { background:var(--surface-2); }
    .snav.active { background:var(--accent-wash); border-left-color:var(--accent); }
    .snav-label { font-weight:600; color:var(--ink); font-size:14px; }
    .snav-desc { font-size:12px; color:var(--ink-3); }

    .settings-panel { min-width:0; max-width:680px; }
    .panel-lede { color:var(--ink-2); font-size:14px; margin:0 0 20px; }
    .panel-note { color:var(--ink-3); font-size:12.5px; margin-top:18px; line-height:1.6; }

    .src-list { display:flex; flex-direction:column; border:1px solid var(--line); border-radius:var(--radius); overflow:hidden; }
    .src { padding:16px; border-bottom:1px solid var(--line); }
    .src:last-child { border-bottom:0; }
    .src-id { display:flex; align-items:center; gap:12px; margin-bottom:12px; }
    .src-usage { font-size:12.5px; color:var(--ink-3); margin-left:auto; }
    .src-controls { display:flex; align-items:flex-end; gap:16px; flex-wrap:wrap; }
    .switch { display:flex; align-items:center; gap:8px; font-size:13px; color:var(--ink-2); font-weight:600; }
    .inline-num { display:flex; flex-direction:column; gap:4px; font-size:11.5px; color:var(--ink-3); font-weight:600; }
    .inline-num input { width:96px; padding:7px 9px; border:1px solid var(--line-strong); border-radius:var(--radius-sm);
      font:14px var(--font-mono); background:var(--surface); color:var(--ink); }

    .toggle-row { display:flex; align-items:center; justify-content:space-between; gap:20px;
      padding:16px 0; border-bottom:1px solid var(--line); }
    .toggle-row:last-child { border-bottom:0; }
    .tr-label { font-weight:600; color:var(--ink); font-size:14px; }
    .tr-desc { color:var(--ink-3); font-size:12.5px; margin-top:2px; max-width:48ch; line-height:1.5; }
    .toggle-row select { padding:8px 10px; border:1px solid var(--line-strong); border-radius:var(--radius-sm);
      background:var(--surface); color:var(--ink); font:13px var(--font-sans); }
    .usage-meter { width:180px; }
    .usage-track { height:8px; background:var(--bg-tint); border-radius:999px; overflow:hidden; }
    .usage-fill { height:100%; background:var(--success); border-radius:999px; }
    .danger-row .tr-label { color:var(--danger); }

    @media (max-width: 900px) {
      .settings-layout { grid-template-columns:1fr; gap:20px; }
      .settings-nav { position:static; flex-direction:row; overflow-x:auto; gap:4px; }
      .snav { border-left:0; border-bottom:2px solid transparent; border-radius:0; flex-shrink:0; }
      .snav.active { border-left:0; border-bottom-color:var(--accent); }
      .snav-desc { display:none; }
      .toggle-row { flex-direction:column; align-items:stretch; }
    }
  `]
})
export class SettingsPageComponent implements OnInit {
  private platform = inject(PlatformConfigService);
  private aiUsage = inject(AiUsageService);
  private account = inject(AccountService);
  private toast = inject(ToastService);

  section = signal<Section>('sources');
  navItems: { key: Section; label: string; desc: string }[] = [
    { key: 'sources', label: 'Job sources', desc: 'Platforms & limits' },
    { key: 'ai', label: 'AI', desc: 'Provider & usage' },
    { key: 'privacy', label: 'Data & privacy', desc: 'Export & reset' },
  ];

  platforms = signal<any[]>([]);
  ai = signal<AiUsage | null>(null);
  aiProvider = localStorage.getItem('jobpilot.aiProvider') ?? 'noop';

  ngOnInit(): void {
    this.platform.list().subscribe({ next: list => this.platforms.set(list), error: () => {} });
    this.aiUsage.usage().subscribe({ next: u => this.ai.set(u), error: () => {} });
  }

  save(p: any): void {
    this.platform.update(p.platformName, {
      enabled: p.enabled, dailyLimit: p.dailyLimit, minDelaySeconds: p.minDelaySeconds,
    }).subscribe(() => this.toast.success(`${p.platformName} saved`));
  }
  saveProvider(): void {
    localStorage.setItem('jobpilot.aiProvider', this.aiProvider);
    this.toast.info('Provider hint saved (set AI_PROVIDER on the backend to activate)');
  }
  pct(v: number, max: number): number { return max ? Math.round((v / max) * 100) : 0; }
  pf(p?: string): string {
    const l = (p || '').toLowerCase();
    return ['naukri', 'linkedin', 'indeed'].includes(l) ? l : 'other';
  }
  exportFile(path: string, filename: string): void {
    this.account.download(path, filename);
    this.toast.info('Preparing download…');
  }
  reset(): void {
    if (!confirm('Delete all your personal data? This cannot be undone.')) return;
    this.account.reset().subscribe(() => this.toast.success('Your data has been reset'));
  }
}


