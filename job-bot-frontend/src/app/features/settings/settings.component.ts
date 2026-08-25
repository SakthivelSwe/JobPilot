import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PlatformConfigService } from '../../core/services/platform-config.service';
import { AiUsageService, AiUsage } from '../../core/services/analytics.service';
import { AccountService } from '../../core/services/account.service';
import { ToastService } from '../../core/services/toast.service';
import { AuthService } from '../../core/services/auth.service';
import { Router } from '@angular/router';
import { PlatformSessionService, PlatformSession } from '../../core/services/platform-session.service';

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

              <!-- Account Session Card (Naukri / Indeed) -->
              <div class="session-card" *ngIf="p.platformName !== 'LINKEDIN'">

                <!-- Status row -->
                <div class="session-row">
                  <span class="session-dot"
                    [class.dot-connected]="p.sessionStatus === 'CONNECTED'"
                    [class.dot-expired]="p.sessionStatus === 'EXPIRED' || p.sessionStatus === 'ERROR'"
                    [class.dot-disconnected]="!p.sessionStatus || p.sessionStatus === 'DISCONNECTED'">
                  </span>
                  <span class="session-label">
                    <ng-container [ngSwitch]="p.sessionStatus">
                      <span *ngSwitchCase="'CONNECTED'">Connected as <strong>{{ p.sessionUsername || 'your account' }}</strong></span>
                      <span *ngSwitchCase="'EXPIRED'">&#9888; Session expired &mdash; reconnect to resume logged-in applies</span>
                      <span *ngSwitchCase="'ERROR'">&#10007; Session error &mdash; try reconnecting</span>
                      <span *ngSwitchDefault>Not connected &mdash; applies will be anonymous</span>
                    </ng-container>
                  </span>
                  <div class="session-actions">
                    <button class="btn secondary small"
                      *ngIf="p.sessionStatus === 'CONNECTED'"
                      [disabled]="!!sessionLoading()"
                      (click)="validateSession(p.platformName)">&#10003; Validate</button>
                    <button class="btn danger small"
                      *ngIf="p.sessionStatus === 'CONNECTED'"
                      [disabled]="!!sessionLoading()"
                      (click)="disconnectSession(p.platformName)">Disconnect</button>
                    <button class="btn accent small"
                      *ngIf="p.sessionStatus !== 'CONNECTED'"
                      [disabled]="!!sessionLoading()"
                      (click)="connectSession(p.platformName)">
                      <span *ngIf="sessionLoading() !== p.platformName">&#128279; Connect {{ p.platformName }} Account</span>
                      <span *ngIf="sessionLoading() === p.platformName">&#9203; Opening browser&hellip;</span>
                    </button>
                  </div>
                </div>

                <!-- ── HOW TO CONNECT — Step-by-step (shown when NOT connected) ── -->
                <div class="steps-block" *ngIf="p.sessionStatus !== 'CONNECTED'">
                  <div class="steps-title">
                    <span class="steps-icon">&#128196;</span>
                    How to link your {{ p.platformName }} account
                  </div>
                  <ol class="steps-list">
                    <li>
                      <span class="step-num">1</span>
                      <div class="step-body">
                        <strong>Click &ldquo;Connect {{ p.platformName }} Account&rdquo;</strong>
                        <span class="step-desc">A Chromium browser window will open on your screen automatically.</span>
                      </div>
                    </li>
                    <li>
                      <span class="step-num">2</span>
                      <div class="step-body">
                        <strong>Log in with your own credentials</strong>
                        <span class="step-desc">
                          Type your {{ p.platformName }} email and password <em>directly in that browser</em>.
                          JobPilot never sees your password &mdash; it only waits for the login to complete.
                        </span>
                      </div>
                    </li>
                    <li *ngIf="p.platformName === 'NAUKRI'">
                      <span class="step-num">3</span>
                      <div class="step-body">
                        <strong>Complete any OTP or CAPTCHA</strong>
                        <span class="step-desc">If Naukri asks for an OTP on your phone or email, enter it. You have up to 3 minutes.</span>
                      </div>
                    </li>
                    <li>
                      <span class="step-num">{{ p.platformName === 'NAUKRI' ? 4 : 3 }}</span>
                      <div class="step-body">
                        <strong>Wait for the browser to close automatically</strong>
                        <span class="step-desc">Once JobPilot detects a successful login, it saves your session (encrypted) and closes the browser. The status above will update to &ldquo;Connected&rdquo;.</span>
                      </div>
                    </li>
                    <li>
                      <span class="step-num">{{ p.platformName === 'NAUKRI' ? 5 : 4 }}</span>
                      <div class="step-body">
                        <strong>You&rsquo;re done &mdash; applies now use your real profile</strong>
                        <span class="step-desc">Future job applications on {{ p.platformName }} will be submitted from your logged-in account, giving recruiters full access to your profile score and &ldquo;Active&rdquo; status.</span>
                      </div>
                    </li>
                  </ol>
                  <div class="steps-security">
                    <span class="lock-icon">&#128274;</span>
                    <div>
                      <strong>Security:</strong> Your password is typed directly in the browser window &mdash; it is <strong>never</strong> captured or stored by JobPilot.
                      Only an encrypted session cookie file is saved locally on <em>your machine</em>.
                      Sessions typically last 30&ndash;90 days. You can disconnect any time.
                    </div>
                  </div>

                  <div class="steps-security" style="margin-top: 10px; background: rgba(0,0,0,0.03);">
                    <div>
                      <strong>Alternative: Provide cookie directly</strong><br/>
                      <span class="step-desc" style="display:block; margin-top:4px;">
                        If the browser does not open correctly, log in to {{ p.platformName }} in your normal browser, open Developer Tools (F12) > Application > Cookies, and copy the value of the <code>{{ p.platformName === 'NAUKRI' ? 'nauk_at' : 'CTK' }}</code> cookie.
                      </span>
                      <div style="display:flex; gap:8px; margin-top:8px;">
                        <input type="password" [(ngModel)]="p.manualCookie" placeholder="Paste cookie value here" style="flex:1; padding:6px 10px; border:1px solid var(--line-strong); border-radius:var(--radius-sm);" />
                        <button class="btn secondary small" (click)="connectManual(p)" [disabled]="!p.manualCookie || !!sessionLoading()">Save Cookie</button>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- ── CONNECTED STATE — What happens now ── -->
                <div class="steps-block connected-block" *ngIf="p.sessionStatus === 'CONNECTED'">
                  <div class="steps-title connected-title">
                    <span class="steps-icon">&#9989;</span>
                    What happens now
                  </div>
                  <ul class="connected-list">
                    <li>&#9679; Job discovery runs on the <strong>public search page</strong> (no login needed)</li>
                    <li>&#9679; When a matching job is queued, the apply engine opens it <strong>logged in as you</strong></li>
                    <li>&#9679; Recruiters see your <strong>full profile score</strong>, resume, and &ldquo;Active in last 30 days&rdquo; badge</li>
                    <li>&#9679; Your application appears in <strong>{{ p.platformName }} &rarr; My Applications</strong></li>
                    <li>&#9679; Session auto-checks on each apply; if expired you will be notified to reconnect</li>
                  </ul>
                  <div class="steps-security" style="margin-top:10px;">
                    <span class="lock-icon">&#128274;</span>
                    <div>No password stored. Session file is AES-256 encrypted and lives only on your machine.
                    Click <strong>Validate</strong> to confirm the session is still active, or <strong>Disconnect</strong> to remove it.</div>
                  </div>
                </div>

              </div>

              <!-- LinkedIn: Chrome Extension handles applies -->
              <div class="session-card" *ngIf="p.platformName === 'LINKEDIN'">
                <div class="session-row">
                  <span class="session-dot dot-info"></span>
                  <span class="session-label"><strong>LinkedIn &mdash; Chrome Extension applies</strong></span>
                </div>
                <div class="steps-block" style="margin-top:10px;">
                  <div class="steps-title" style="margin-bottom:10px;">
                    <span class="steps-icon">&#128279;</span>
                    How LinkedIn applications work in JobPilot
                  </div>
                  <ol class="steps-list">
                    <li>
                      <span class="step-num">1</span>
                      <div class="step-body">
                        <strong>JobPilot discovers jobs from LinkedIn&rsquo;s public search</strong>
                        <span class="step-desc">No login required for discovery. Jobs appear in your queue automatically.</span>
                      </div>
                    </li>
                    <li>
                      <span class="step-num">2</span>
                      <div class="step-body">
                        <strong>Install the JobPilot Chrome Extension</strong>
                        <span class="step-desc">The extension runs inside your <em>own</em> Chrome browser where you are already logged into LinkedIn.</span>
                      </div>
                    </li>
                    <li>
                      <span class="step-num">3</span>
                      <div class="step-body">
                        <strong>Extension opens queued LinkedIn jobs and clicks &ldquo;Easy Apply&rdquo;</strong>
                        <span class="step-desc">Because it runs inside your real browser session, LinkedIn sees it as a normal user action &mdash; not a bot.</span>
                      </div>
                    </li>
                    <li>
                      <span class="step-num">4</span>
                      <div class="step-body">
                        <strong>Application is recorded in your JobPilot dashboard</strong>
                        <span class="step-desc">Status updates to Applied and the job moves on your Kanban board.</span>
                      </div>
                    </li>
                  </ol>
                  <div class="steps-security">
                    <span class="lock-icon">&#128274;</span>
                    <div>LinkedIn credentials are never stored server-side. The Extension acts entirely within your own logged-in browser session.</div>
                  </div>
                </div>
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
          
          <div class="toggle-row danger-row">
            <div>
              <div class="tr-label">Delete account</div>
              <div class="tr-desc">Permanently erase your entire account, configuration, and all data. You will be logged out. This cannot be undone.</div>
            </div>
            <button class="btn danger small" (click)="deleteAccount()">Delete account</button>
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
    .session-card { margin-top:12px; padding:11px 14px; background:var(--surface-2,#f5f6fa); border-radius:var(--radius-sm,6px); border:1px solid var(--line); }
    .session-row { display:flex; align-items:center; gap:9px; flex-wrap:wrap; }
    .session-dot { width:9px; height:9px; border-radius:50%; flex-shrink:0; background:var(--ink-4,#c4c4c4); transition:background .2s; }
    .dot-connected { background:#22c55e; box-shadow:0 0 0 3px rgba(34,197,94,.18); }
    .dot-expired { background:#f97316; box-shadow:0 0 0 3px rgba(249,115,22,.18); }
    .dot-disconnected { background:var(--ink-4,#c4c4c4); }
    .dot-info { background:#60a5fa; }
    .session-label { font-size:13px; color:var(--ink-2); flex:1; min-width:0; }
    .session-label strong { color:var(--ink); }
    .session-actions { display:flex; gap:6px; flex-shrink:0; flex-wrap:wrap; }
    .session-hint { margin-top:8px; font-size:12px; color:var(--ink-3); line-height:1.5; }
    .session-hint.connected { color:#16a34a; }
    .btn.accent { background:var(--accent,#6366f1); color:#fff; border:1px solid var(--accent,#6366f1); }
    .btn.accent:hover:not(:disabled) { opacity:.88; }

    /* ── Steps block ── */
    .steps-block { margin-top:14px; padding:14px 16px; background:var(--surface,#fff);
      border:1px solid var(--line); border-radius:var(--radius-sm,6px); }
    .connected-block { background:rgba(34,197,94,.05); border-color:rgba(34,197,94,.25); }
    .steps-title { display:flex; align-items:center; gap:7px; font-weight:700; font-size:13px;
      color:var(--ink); margin-bottom:14px; }
    .connected-title { color:#15803d; }
    .steps-icon { font-size:15px; }
    .steps-list { list-style:none; margin:0; padding:0; display:flex; flex-direction:column; gap:10px; }
    .steps-list li { display:flex; align-items:flex-start; gap:10px; }
    .step-num { display:flex; align-items:center; justify-content:center; width:22px; height:22px;
      min-width:22px; border-radius:50%; background:var(--accent,#6366f1); color:#fff;
      font-size:11px; font-weight:700; line-height:1; margin-top:1px; }
    .step-body { display:flex; flex-direction:column; gap:2px; }
    .step-body strong { font-size:13px; color:var(--ink); font-weight:600; }
    .step-desc { font-size:12px; color:var(--ink-3); line-height:1.55; }
    .steps-security { display:flex; align-items:flex-start; gap:8px; margin-top:14px;
      padding:10px 12px; background:rgba(99,102,241,.07); border-radius:var(--radius-sm,6px);
      font-size:12px; color:var(--ink-2); line-height:1.55; }
    .lock-icon { font-size:14px; flex-shrink:0; margin-top:1px; }
    .connected-list { list-style:none; margin:0; padding:0; display:flex; flex-direction:column; gap:6px; }
    .connected-list li { font-size:13px; color:var(--ink-2); }
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
  private sessionSvc = inject(PlatformSessionService);

  section = signal<Section>('sources');
  navItems: { key: Section; label: string; desc: string }[] = [
    { key: 'sources', label: 'Job sources', desc: 'Platforms & limits' },
    { key: 'ai', label: 'AI', desc: 'Provider & usage' },
    { key: 'privacy', label: 'Data & privacy', desc: 'Export & reset' },
  ];

  platforms = signal<any[]>([]);
  ai = signal<AiUsage | null>(null);
  aiProvider = localStorage.getItem('jobpilot.aiProvider') ?? 'noop';
  sessionLoading = signal<string | null>(null);

  ngOnInit(): void {
    this.platform.list().subscribe({ next: list => this.platforms.set(list), error: () => {} });
    this.aiUsage.usage().subscribe({ next: u => this.ai.set(u), error: () => {} });
  }

  connectSession(platformName: string): void {
    if (this.sessionLoading()) return;
    this.sessionLoading.set(platformName);
    this.toast.info(`Opening ${platformName} login window — please log in in the browser that appears.`);
    this.sessionSvc.connect(platformName).subscribe({
      next: (s) => { this.sessionLoading.set(null); this.patchSession(platformName, s); this.toast.success(`${platformName} connected as ${s.sessionUsername || 'your account'}!`); },
      error: (err) => { this.sessionLoading.set(null); this.toast.error(`Failed to connect ${platformName}: ${err?.error?.message ?? 'Unknown error'}`); }
    });
  }

  connectManual(p: any): void {
    if (this.sessionLoading() || !p.manualCookie) return;
    this.sessionLoading.set(p.platformName);
    this.sessionSvc.connectManual(p.platformName, p.manualCookie).subscribe({
      next: (s) => { this.sessionLoading.set(null); this.patchSession(p.platformName, s); p.manualCookie = ''; this.toast.success(`${p.platformName} connected via manual cookie!`); },
      error: (err) => { this.sessionLoading.set(null); this.toast.error(`Failed to connect ${p.platformName}: ${err?.error?.message ?? 'Unknown error'}`); }
    });
  }

  disconnectSession(platformName: string): void {
    this.sessionLoading.set(platformName);
    this.sessionSvc.disconnect(platformName).subscribe({
      next: (s) => { this.sessionLoading.set(null); this.patchSession(platformName, s); this.toast.info(`${platformName} account disconnected.`); },
      error: () => this.sessionLoading.set(null)
    });
  }

  validateSession(platformName: string): void {
    this.sessionLoading.set(platformName);
    this.sessionSvc.validate(platformName).subscribe({
      next: (s) => {
        this.sessionLoading.set(null); this.patchSession(platformName, s);
        s.sessionActive ? this.toast.success(`${platformName} session is active.`) : this.toast.error(`${platformName} session expired. Please reconnect.`);
      },
      error: () => this.sessionLoading.set(null)
    });
  }

  private patchSession(platformName: string, s: PlatformSession): void {
    this.platforms.update(list => list.map(p =>
      p.platformName === platformName
        ? { ...p, sessionStatus: s.sessionStatus, sessionActive: s.sessionActive, sessionUsername: s.sessionUsername, sessionConnectedAt: s.sessionConnectedAt }
        : p
    ));
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
  private auth = inject(AuthService);
  private router = inject(Router);

  reset(): void {
    if (!confirm('Delete all your personal data? This cannot be undone.')) return;
    this.account.reset().subscribe(() => this.toast.success('Your data has been reset'));
  }

  deleteAccount(): void {
    if (!confirm('Permanently delete your account and all data? This cannot be undone.')) return;
    this.auth.deleteAccount().subscribe(() => {
      this.toast.success('Your account has been deleted');
      this.router.navigateByUrl('/login');
    });
  }
}


