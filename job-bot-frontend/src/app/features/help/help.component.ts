import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { OnboardingService } from '../../core/services/onboarding.service';

interface FaqItem { q: string; a: string; }

@Component({
  selector: 'app-help',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <!-- Masthead -->
    <header class="masthead">
      <div>
        <div class="kicker">User guide</div>
        <h1 class="display">How JobPilot works</h1>
        <p class="lede">A plain-language guide — no jargon. Read the top part in 60 seconds, or jump to any section on the right.</p>
      </div>
      <div class="masthead-actions">
        <button class="btn" (click)="showTour()">Show welcome tour</button>
      </div>
    </header>

    <div class="help-layout">
      <!-- Main content -->
      <div class="help-main">

        <!-- What is JobPilot? -->
        <section id="what" class="h-section">
          <div class="section-head"><h2>What is JobPilot?</h2></div>
          <p class="h-lead">
            JobPilot is a personal <b>career-search assistant</b>. It watches public job sources for you,
            scores each posting against your skills, and helps you review, apply, and track everything
            in one place.
          </p>
          <div class="promise-grid">
            <div class="promise">
              <div class="p-title">You stay in control</div>
              <div class="p-body">Nothing is submitted anywhere without your approval.</div>
            </div>
            <div class="promise">
              <div class="p-title">No stored passwords</div>
              <div class="p-body">JobPilot never stores your Naukri, LinkedIn, or Indeed passwords.</div>
            </div>
            <div class="promise">
              <div class="p-title">Real numbers only</div>
              <div class="p-body">Match scores and stats come from your real data — nothing is faked.</div>
            </div>
            <div class="promise">
              <div class="p-title">Your data is yours</div>
              <div class="p-body">Export it or wipe it any time in Settings → Data &amp; privacy.</div>
            </div>
          </div>
        </section>

        <!-- Quick start -->
        <section id="quick-start" class="h-section">
          <div class="section-head"><h2>Quick start · 5 minutes</h2></div>
          <p class="h-lead">Do these six things once and you're set. The <b>Setup</b> chip at the top-right shows your progress live.</p>

          <ol class="steps">
            <li>
              <div class="s-title">Build your profile <a class="s-cta" routerLink="/profile">Open Profile →</a></div>
              <div class="s-body">Upload a résumé (PDF/DOC/TXT) or fill in your name, experience, skills, and location. This is the "you" that JobPilot matches against every job.</div>
            </li>
            <li>
              <div class="s-title">Add target roles <a class="s-cta" routerLink="/target-roles">Open Target roles →</a></div>
              <div class="s-body">Job titles you actually want — for example <em>Java Backend Developer</em>, <em>Full Stack Engineer</em>. Naukri/LinkedIn/Indeed searches are built from these.</div>
            </li>
            <li>
              <div class="s-title">Add at least one résumé <a class="s-cta" routerLink="/resumes">Open Résumés →</a></div>
              <div class="s-body">You can keep multiple versions targeted at different roles (e.g. one Backend, one Full Stack). JobPilot picks the best one per job.</div>
            </li>
            <li>
              <div class="s-title">Set search criteria <a class="s-cta" routerLink="/criteria">Open Criteria →</a></div>
              <div class="s-body">Locations, experience range, salary, keywords, and the minimum match score to shortlist. You can even write an advanced boolean rule like <code class="mono">Java AND (Kafka OR Microservices) AND NOT Intern</code>.</div>
            </li>
            <li>
              <div class="s-title">Run your first scan <a class="s-cta" routerLink="/discovery">Open Discover →</a></div>
              <div class="s-body">Click <b>Scan for jobs</b>. JobPilot pulls postings from public sources and ranks them 0–100 for you. Anything above 80 is a <b>strong</b> match.</div>
            </li>
            <li>
              <div class="s-title">Review and act <a class="s-cta" routerLink="/queue">Open Review queue →</a></div>
              <div class="s-body">Approve, skip, or send to Manual for jobs that need your final click. Applied jobs show up in Pipeline and progress to Interview and Offer as things move.</div>
            </li>
          </ol>
        </section>

        <!-- How auto-apply works -->
        <section id="how-apply" class="h-section">
          <div class="section-head"><h2>How auto-apply actually works</h2></div>
          <p class="h-lead">This is the part most tools lie about. Here's the honest version:</p>

          <div class="platform-grid">
            <!-- Naukri -->
            <article class="pf">
              <div class="pf-head">
                <span class="pf-badge naukri">NAUKRI</span>
                <span class="pf-mode">Local companion</span>
              </div>
              <p>Handled by the local <b>application-engine</b> — a small Node.js companion that lives on your machine. When you approve a queued job, the engine signs in to Naukri <em>using a session you provide</em> and submits the application, respecting the daily limit and delay you set in Settings.</p>
              <ul>
                <li>Needs the companion running on your PC (not on JobPilot's server).</li>
                <li>Daily cap and minimum delay between applications are configurable.</li>
                <li>Falls back to Manual if the posting can't be auto-submitted.</li>
              </ul>
            </article>

            <!-- LinkedIn -->
            <article class="pf">
              <div class="pf-head">
                <span class="pf-badge linkedin">LINKEDIN</span>
                <span class="pf-mode">Chrome extension</span>
              </div>
              <p>LinkedIn applications always run inside <b>your own Chrome browser</b> via the JobPilot extension. LinkedIn's terms don't allow server-side automation, and JobPilot won't do it — the extension only fills fields on Easy Apply forms after you click.</p>
              <ul>
                <li>Never runs while you're offline.</li>
                <li>Your LinkedIn session cookie never leaves your browser.</li>
                <li>Any posting the extension can't fully submit goes to Manual.</li>
              </ul>
            </article>

            <!-- Indeed -->
            <article class="pf">
              <div class="pf-head">
                <span class="pf-badge indeed">INDEED</span>
                <span class="pf-mode">Local companion</span>
              </div>
              <p>Handled by the same local application-engine as Naukri. Indeed's "Apply with Indeed" postings can be auto-submitted; the ones that redirect to a company site go to Manual.</p>
              <ul>
                <li>Same limits and delay controls as Naukri.</li>
                <li>Passwords are never stored — the engine uses a session token you set up locally.</li>
              </ul>
            </article>

            <!-- Manual -->
            <article class="pf highlight">
              <div class="pf-head">
                <span class="pf-badge other">MANUAL</span>
                <span class="pf-mode">Always available</span>
              </div>
              <p>If you don't run the companion engine or the extension at all, JobPilot still works — every strong match just goes to the <a routerLink="/manual">Manual queue</a> with the résumé and cover-letter draft ready. You click <b>Open</b> to reach the job page, submit it yourself, then click <b>Applied ✓</b>.</p>
              <ul>
                <li>No setup required — works out of the box.</li>
                <li>Marks the application as Applied in your Pipeline automatically.</li>
              </ul>
            </article>
          </div>

          <div class="callout">
            <b>Bottom line:</b> JobPilot never bypasses a platform's login. It uses <em>your</em> browser or <em>your</em> local session. If you turn everything off, you still get ranked jobs, prepared applications, and a proper tracker — you just click Submit yourself.
          </div>
        </section>

        <!-- Understanding the pages -->
        <section id="pages" class="h-section">
          <div class="section-head"><h2>What each page does</h2></div>
          <table class="data pages-table">
            <thead>
              <tr><th>Page</th><th>Purpose</th><th>You use it to…</th></tr>
            </thead>
            <tbody>
              <tr>
                <td><a routerLink="/dashboard" class="strong">Today</a></td>
                <td>Daily briefing</td>
                <td>See what needs attention, momentum, and recent activity in one place.</td>
              </tr>
              <tr>
                <td><a routerLink="/discovery" class="strong">Discover</a></td>
                <td>Find opportunities</td>
                <td>Run scans, filter by source or match score, save interesting jobs.</td>
              </tr>
              <tr>
                <td><a routerLink="/queue" class="strong">Review</a></td>
                <td>Decision inbox</td>
                <td>Approve, skip or send to Manual — one strong match at a time.</td>
              </tr>
              <tr>
                <td><a routerLink="/manual" class="strong">Manual</a></td>
                <td>Human-submit queue</td>
                <td>Jobs the engine can't auto-submit — you click Open, then Applied ✓.</td>
              </tr>
              <tr>
                <td><a routerLink="/applications" class="strong">Pipeline</a></td>
                <td>Career progression</td>
                <td>Track Applied → Screening → Interview → Offer, add notes, set interview dates.</td>
              </tr>
              <tr>
                <td><a routerLink="/interviews" class="strong">Interviews</a></td>
                <td>Prep center</td>
                <td>Prep pack per interview — technical topics, questions, and a checklist.</td>
              </tr>
              <tr>
                <td><a routerLink="/profile" class="strong">Profile</a></td>
                <td>Your identity</td>
                <td>Details, résumé parsing, skills — the source of truth for matching.</td>
              </tr>
              <tr>
                <td><a routerLink="/target-roles" class="strong">Roles</a></td>
                <td>Search targets</td>
                <td>Job titles you want, ranked by priority. Discovery uses these.</td>
              </tr>
              <tr>
                <td><a routerLink="/resumes" class="strong">Résumés</a></td>
                <td>Library</td>
                <td>Keep multiple résumé versions — JobPilot picks the best per job.</td>
              </tr>
              <tr>
                <td><a routerLink="/criteria" class="strong">Criteria</a></td>
                <td>Search rules</td>
                <td>Locations, salary, keywords, minimum match score, boolean rules.</td>
              </tr>
              <tr>
                <td><a routerLink="/analytics" class="strong">Insights</a></td>
                <td>What's working</td>
                <td>Best-performing role/source/résumé, biggest bottleneck.</td>
              </tr>
              <tr>
                <td><a routerLink="/settings" class="strong">Settings</a></td>
                <td>Configuration</td>
                <td>Platform daily limits, AI provider, export data, reset account.</td>
              </tr>
            </tbody>
          </table>
        </section>

        <!-- FAQ -->
        <section id="faq" class="h-section">
          <div class="section-head"><h2>Common questions</h2></div>
          <div class="faq">
            <details class="faq-item" *ngFor="let f of faq; let i = index">
              <summary>{{ f.q }}</summary>
              <p>{{ f.a }}</p>
            </details>
          </div>
        </section>

        <!-- Modify / customize -->
        <section id="modify" class="h-section">
          <div class="section-head"><h2>Modifying and personalising</h2></div>
          <ul class="mod-list">
            <li><b>Change search behaviour</b> — edit <a routerLink="/criteria">Criteria</a> (min match, keywords, boolean rules) and <a routerLink="/target-roles">Target roles</a> (priorities, required/preferred/excluded skills).</li>
            <li><b>Change auto-apply limits</b> — <a routerLink="/settings">Settings → Job sources</a>: daily cap and minimum delay per platform.</li>
            <li><b>Turn auto-apply off completely</b> — disable the platform in Settings. JobPilot will still discover and rank; strong matches will just land in Manual.</li>
            <li><b>Add or refine a résumé</b> — <a routerLink="/resumes">Résumés</a> → <em>Add résumé</em>. JobPilot will re-score jobs the next time you scan.</li>
            <li><b>AI provider</b> — <a routerLink="/settings">Settings → AI</a>. AI is optional and only powers summarisation and cover-letter drafts. Match scores are always deterministic.</li>
            <li><b>Export or wipe your data</b> — <a routerLink="/settings">Settings → Data &amp; privacy</a>. Applications, jobs, or a full JSON backup.</li>
          </ul>
        </section>

      </div>

      <!-- Sidebar table of contents -->
      <aside class="help-toc">
        <div class="kicker">On this page</div>
        <a href="#what">What is JobPilot?</a>
        <a href="#quick-start">Quick start · 5 min</a>
        <a href="#how-apply">How auto-apply works</a>
        <a href="#pages">What each page does</a>
        <a href="#faq">Common questions</a>
        <a href="#modify">Modifying &amp; personalising</a>

        <div class="kicker" style="margin-top:24px;">Still stuck?</div>
        <button class="btn secondary small" (click)="showTour()" style="width:100%;">Replay welcome tour</button>
      </aside>
    </div>
  `,
  styles: [`
    .masthead { display:flex; align-items:flex-end; justify-content:space-between; gap:24px; margin-bottom:8px; }
    .masthead .lede { color:var(--ink-2); font-size:15px; margin:8px 0 0; max-width:66ch; }
    .masthead-actions { flex-shrink:0; }

    .help-layout { display:grid; grid-template-columns: 1fr 220px; gap:36px; margin-top:24px; align-items:start; }
    .help-main { min-width:0; max-width:760px; }
    .help-toc { position:sticky; top:78px; display:flex; flex-direction:column; gap:4px; }
    .help-toc a { padding:6px 0; color:var(--ink-2); font-size:13px; text-decoration:none; }
    .help-toc a:hover { color:var(--accent); }
    .help-toc .kicker { margin-bottom:8px; }
    .help-toc .kicker:first-of-type { margin-top:0; }

    .h-section { margin-bottom:40px; scroll-margin-top:76px; }
    .h-lead { color:var(--ink-2); font-size:14.5px; line-height:1.6; max-width:70ch; margin:0 0 16px; }
    .h-lead b { color:var(--ink); }

    /* Promise cards */
    .promise-grid { display:grid; grid-template-columns:repeat(2,1fr); gap:12px; margin-top:8px; }
    @media (max-width:640px) { .promise-grid { grid-template-columns:1fr; } }
    .promise { border:1px solid var(--line); border-radius:var(--radius); padding:14px 16px; background:var(--surface); border-left:3px solid var(--success); }
    .p-title { font-weight:600; color:var(--ink); font-size:14px; }
    .p-body { color:var(--ink-2); font-size:13px; margin-top:3px; }

    /* Steps */
    .steps { list-style:none; padding:0; margin:0; counter-reset:step; }
    .steps > li { position:relative; padding:16px 0 16px 44px; border-top:1px solid var(--line); counter-increment:step; }
    .steps > li:first-child { border-top:0; }
    .steps > li::before { content:counter(step, decimal-leading-zero); position:absolute; left:0; top:18px;
      width:32px; height:32px; border-radius:50%; background:var(--accent-wash); color:var(--accent-deep);
      display:flex; align-items:center; justify-content:center; font:600 12px var(--font-mono); }
    .s-title { display:flex; align-items:baseline; gap:12px; flex-wrap:wrap; font-weight:600; font-size:15px; color:var(--ink); }
    .s-cta { font-size:12.5px; font-weight:600; color:var(--accent); }
    .s-body { color:var(--ink-2); font-size:13.5px; line-height:1.55; margin-top:4px; max-width:68ch; }
    .s-body em { font-style:normal; color:var(--ink); }
    .s-body code { background:var(--surface-2); padding:1px 5px; border:1px solid var(--line); border-radius:4px; font-size:12px; }

    /* Platform grid */
    .platform-grid { display:grid; grid-template-columns:repeat(2,1fr); gap:14px; margin-top:8px; }
    @media (max-width:800px) { .platform-grid { grid-template-columns:1fr; } }
    .pf { border:1px solid var(--line); border-radius:var(--radius); padding:16px 18px; background:var(--surface); }
    .pf.highlight { background:var(--accent-wash); border-color:var(--accent); }
    .pf-head { display:flex; align-items:center; gap:10px; margin-bottom:8px; }
    .pf-mode { font-size:11.5px; font-weight:600; color:var(--ink-3); }
    .pf p { color:var(--ink); font-size:13.5px; line-height:1.55; margin:0 0 8px; }
    .pf ul { margin:6px 0 0; padding-left:18px; }
    .pf ul li { color:var(--ink-2); font-size:12.5px; padding:3px 0; }

    .callout { margin-top:16px; padding:14px 16px; background:var(--info-wash); border-left:3px solid var(--info);
      border-radius:0 var(--radius-sm) var(--radius-sm) 0; color:var(--ink); font-size:13.5px; line-height:1.55; }

    /* Pages table */
    .pages-table td { vertical-align:top; }
    .pages-table td:nth-child(2) { color:var(--ink-2); }
    .pages-table td:nth-child(3) { color:var(--ink-2); font-size:13px; }

    /* FAQ */
    .faq { display:flex; flex-direction:column; }
    .faq-item { border-top:1px solid var(--line); padding:12px 0; }
    .faq-item:first-child { border-top:0; }
    .faq-item summary { cursor:pointer; font-weight:600; color:var(--ink); font-size:14px; padding:4px 0;
      display:flex; align-items:center; gap:10px; }
    .faq-item summary::before { content:'+'; color:var(--accent); font-weight:700; width:14px; text-align:center; }
    .faq-item[open] summary::before { content:'−'; }
    .faq-item summary::-webkit-details-marker { display:none; }
    .faq-item p { color:var(--ink-2); font-size:13.5px; line-height:1.6; margin:8px 0 4px 24px; max-width:68ch; }

    .mod-list { padding-left:18px; margin:8px 0 0; }
    .mod-list li { padding:6px 0; color:var(--ink-2); font-size:14px; line-height:1.55; }
    .mod-list li b { color:var(--ink); }

    @media (max-width:900px) {
      .help-layout { grid-template-columns:1fr; }
      .help-toc { position:static; }
      .masthead { flex-direction:column; align-items:stretch; }
    }
  `]
})
export class HelpPageComponent {
  onb = inject(OnboardingService);

  faq: FaqItem[] = [
    { q: 'Do I need to install anything to start?',
      a: 'No. You can use JobPilot fully from the browser — discover jobs, review, prepare applications, and submit them manually. The optional companion engine and browser extension are only for auto-submitting Naukri, LinkedIn, or Indeed.' },
    { q: 'Where are my Naukri / LinkedIn passwords stored?',
      a: 'Nowhere on JobPilot. The Chrome extension uses your existing LinkedIn browser session. The local application-engine uses a session token you configure on your own machine. Passwords never leave your device.' },
    { q: 'What is a "match score"?',
      a: 'A 0–100 number computed by the backend from your profile, skills, target roles, and the job description. Above 80 is a strong match. It is deterministic — the same inputs always produce the same score. It is not AI-generated.' },
    { q: 'What if I don\'t like the ranking?',
      a: 'Tweak your Target roles (skills required/preferred/excluded), Criteria (min match, keywords, boolean rules), or your Profile skills. Re-run a scan and JobPilot will re-rank.' },
    { q: 'How do I stop auto-applying?',
      a: 'Go to Settings → Job sources and toggle a platform off. You can also lower its daily limit to 0. Nothing pending will be submitted.' },
    { q: 'What happens if the companion engine is off?',
      a: 'Everything still works — discovery, matching, review, application tracking. Approved jobs simply pile up in the queue until the engine comes online, or you can send them to Manual and submit yourself.' },
    { q: 'Can I use multiple résumés?',
      a: 'Yes. Add them under Résumés. JobPilot picks the best-matched résumé for each job and shows you which one it chose in the Review queue.' },
    { q: 'How do I import a job I found myself?',
      a: 'Use "Import a job" from the Jobs page or the Today screen. Paste the URL and description; JobPilot scores it against your profile and drafts a résumé match and cover letter.' },
    { q: 'How do I export or delete my data?',
      a: 'Settings → Data & privacy. You can export applications, jobs, or a full JSON backup. "Reset my data" wipes the profile, criteria, jobs, queue, and applications (platform limits are kept).' },
    { q: 'Does JobPilot lie about being able to auto-apply anywhere?',
      a: 'No. Naukri and Indeed use your local companion engine; LinkedIn uses the browser extension in your own Chrome. Anything a platform blocks or a form JobPilot doesn\'t recognise goes to Manual.' },
  ];

  showTour(): void { this.onb.openWelcome(); }
}

