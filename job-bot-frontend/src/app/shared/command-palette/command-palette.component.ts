import { Component, ElementRef, ViewChild, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { UiService } from '../../core/services/ui.service';
import { SearchService, SearchHit } from '../../core/services/search.service';

interface Command {
  label: string;
  hint: string;
  group: 'Actions' | 'Go to';
  run: () => void;
  keywords?: string;
}

/**
 * Global command palette (⌘K / Ctrl-K). Quick actions + navigation.
 * Everything routes into existing flows — no new backend needed.
 */
@Component({
  selector: 'jp-command-palette',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="cmd-overlay" *ngIf="ui.paletteOpen()" (click)="close()">
      <div class="cmd" (click)="$event.stopPropagation()" role="dialog" aria-modal="true" aria-label="Command palette">
        <div class="cmd-input">
          <span class="cmd-prompt">⌘</span>
          <input #box type="text" [value]="query()" (input)="onInput($event)"
                 (keydown)="onKey($event)" placeholder="Search jobs, applications, résumés, actions…"
                 aria-label="Search JobPilot" autocomplete="off" spellcheck="false" />
          <kbd>esc</kbd>
        </div>
        <div class="cmd-list">
          <ng-container *ngIf="results().length">
            <div class="cmd-group">Results</div>
            <button *ngFor="let r of results()" class="cmd-item"
                    (click)="openHit(r)">
              <span class="cmd-kind">{{ r.kind }}</span>
              <span class="cmd-label">{{ r.title }}</span>
              <span class="cmd-hint">{{ r.subtitle }}</span>
            </button>
          </ng-container>
          <ng-container *ngFor="let g of groups()">
            <div class="cmd-group">{{ g.name }}</div>
            <button *ngFor="let c of g.items"
                    class="cmd-item" [class.active]="c === active()"
                    (mouseenter)="setActive(c)" (click)="choose(c)">
              <span class="cmd-label">{{ c.label }}</span>
              <span class="cmd-hint">{{ c.hint }}</span>
            </button>
          </ng-container>
          <div class="cmd-empty" *ngIf="!filtered().length && !results().length">No matches for “{{ query() }}”.</div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .cmd-overlay {
      position: fixed; inset: 0; z-index: 1000;
      background: rgba(32, 30, 27, 0.32);
      display: flex; align-items: flex-start; justify-content: center;
      padding-top: 12vh; animation: fade .12s ease;
    }
    .cmd {
      width: 560px; max-width: 92vw;
      background: var(--surface); border: 1px solid var(--line-strong);
      border-radius: 12px; overflow: hidden;
      box-shadow: 0 24px 60px rgba(32,30,27,0.28);
      animation: pop .12s ease;
    }
    .cmd-input {
      display: flex; align-items: center; gap: 10px;
      padding: 14px 16px; border-bottom: 1px solid var(--line);
    }
    .cmd-prompt { font-family: var(--font-mono); color: var(--accent); font-size: 15px; }
    .cmd-input input {
      flex: 1; border: 0; outline: 0; background: transparent;
      font: 16px var(--font-sans); color: var(--ink);
    }
    .cmd-input kbd {
      font: 11px var(--font-mono); color: var(--ink-3);
      border: 1px solid var(--line-strong); border-radius: 5px; padding: 1px 6px;
    }
    .cmd-list { max-height: 52vh; overflow-y: auto; padding: 6px; }
    .cmd-group {
      font: 700 10.5px var(--font-sans); letter-spacing: 0.08em; text-transform: uppercase;
      color: var(--ink-3); padding: 12px 12px 6px;
    }
    .cmd-item {
      display: flex; align-items: baseline; gap: 12px; width: 100%;
      background: transparent; border: 0; cursor: pointer; text-align: left;
      padding: 9px 12px; border-radius: var(--radius-sm);
      color: var(--ink); font: 14px var(--font-sans);
    }
    .cmd-item.active { background: var(--accent-wash); }
    .cmd-kind { font: 700 10px var(--font-sans); letter-spacing: 0.04em; text-transform: uppercase;
      color: var(--accent-deep); background: var(--accent-wash); border-radius: 4px; padding: 2px 6px; flex-shrink: 0; }
    .cmd-label { font-weight: 600; }
    .cmd-hint { color: var(--ink-3); font-size: 12.5px; margin-left: auto; }
    .cmd-empty { padding: 20px 12px; color: var(--ink-3); font-size: 14px; text-align: center; }
    @keyframes fade { from { opacity: 0; } to { opacity: 1; } }
    @keyframes pop { from { transform: translateY(-6px); opacity: .6; } to { transform: none; opacity: 1; } }
  `]
})
export class CommandPaletteComponent {
  ui = inject(UiService);
  private router = inject(Router);
  private search = inject(SearchService);
  @ViewChild('box') box?: ElementRef<HTMLInputElement>;

  query = signal('');
  active = signal<Command | null>(null);
  results = signal<SearchHit[]>([]);
  private searchTimer: any;

  private commands: Command[] = [
    // Actions
    { group: 'Actions', label: 'Scan for jobs', hint: 'Discovery', run: () => this.go('/discovery'), keywords: 'find search discover' },
    { group: 'Actions', label: 'Upload résumé / edit profile', hint: 'Profile', run: () => this.go('/profile'), keywords: 'candidate cv parse' },
    { group: 'Actions', label: 'Add a target role', hint: 'Roles', run: () => this.go('/target-roles'), keywords: 'strategy title search' },
    { group: 'Actions', label: 'Import a job', hint: 'Paste URL / JD', run: () => this.go('/jobs/import'), keywords: 'add paste url' },
    { group: 'Actions', label: 'Add a résumé', hint: 'Résumés', run: () => this.go('/resumes/new'), keywords: 'upload cv new' },
    { group: 'Actions', label: 'New search criteria', hint: 'Criteria', run: () => this.go('/criteria/new'), keywords: 'strategy filter' },
    { group: 'Actions', label: 'Review the queue', hint: 'Approve / skip', run: () => this.go('/queue'), keywords: 'approve pending' },
    { group: 'Actions', label: 'Finish manual applications', hint: 'Manual', run: () => this.go('/manual'), keywords: 'linkedin apply' },
    // Navigation
    { group: 'Go to', label: 'Today', hint: 'Daily briefing', run: () => this.go('/dashboard') },
    { group: 'Go to', label: 'Profile', hint: 'Candidate', run: () => this.go('/profile') },
    { group: 'Go to', label: 'Target roles', hint: 'Strategy', run: () => this.go('/target-roles') },
    { group: 'Go to', label: 'Discover', hint: 'Opportunity feed', run: () => this.go('/discovery') },
    { group: 'Go to', label: 'Review', hint: 'Action inbox', run: () => this.go('/queue') },
    { group: 'Go to', label: 'Manual apply', hint: 'Unfinished', run: () => this.go('/manual') },
    { group: 'Go to', label: 'Pipeline', hint: 'Progression', run: () => this.go('/applications') },
    { group: 'Go to', label: 'Interviews', hint: 'Prep center', run: () => this.go('/interviews') },
    { group: 'Go to', label: 'Résumés', hint: 'Library', run: () => this.go('/resumes') },
    { group: 'Go to', label: 'Criteria', hint: 'Strategy', run: () => this.go('/criteria') },
    { group: 'Go to', label: 'Insights', hint: 'Analytics', run: () => this.go('/analytics') },
    { group: 'Go to', label: 'Settings', hint: 'Configuration', run: () => this.go('/settings') },
    { group: 'Go to', label: 'User guide', hint: 'Help & how-to', run: () => this.go('/help'), keywords: 'help how docs guide onboarding faq' },
  ];

  constructor() {
    // Focus the box + reset when opened.
    effect(() => {
      if (this.ui.paletteOpen()) {
        this.query.set('');
        this.results.set([]);
        this.active.set(this.commands[0]);
        setTimeout(() => this.box?.nativeElement.focus(), 0);
      }
    });
    // Global ⌘K / Ctrl-K.
    document.addEventListener('keydown', (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        this.ui.togglePalette();
      }
    });
  }

  filtered = computed<Command[]>(() => {
    const q = this.query().trim().toLowerCase();
    if (!q) return this.commands;
    return this.commands.filter(c =>
      (c.label + ' ' + c.hint + ' ' + (c.keywords || '')).toLowerCase().includes(q));
  });

  groups = computed(() => {
    const list = this.filtered();
    const names: Command['group'][] = ['Actions', 'Go to'];
    return names
      .map(name => ({ name, items: list.filter(c => c.group === name) }))
      .filter(g => g.items.length);
  });

  onInput(e: Event): void {
    const val = (e.target as HTMLInputElement).value;
    this.query.set(val);
    this.active.set(this.filtered()[0] ?? null);
    clearTimeout(this.searchTimer);
    const q = val.trim();
    if (q.length < 2) { this.results.set([]); return; }
    this.searchTimer = setTimeout(() => {
      this.search.search(q).subscribe({
        next: r => this.results.set(r?.hits ?? []),
        error: () => this.results.set([]),
      });
    }, 250);
  }

  openHit(hit: SearchHit): void {
    this.router.navigateByUrl(hit.route);
    this.close();
  }

  onKey(e: KeyboardEvent): void {
    const list = this.filtered();
    const idx = this.active() ? list.indexOf(this.active()!) : -1;
    if (e.key === 'ArrowDown') { e.preventDefault(); this.active.set(list[Math.min(idx + 1, list.length - 1)] ?? null); }
    else if (e.key === 'ArrowUp') { e.preventDefault(); this.active.set(list[Math.max(idx - 1, 0)] ?? null); }
    else if (e.key === 'Enter') { e.preventDefault(); if (this.active()) this.choose(this.active()!); }
    else if (e.key === 'Escape') { this.close(); }
  }

  setActive(c: Command): void { this.active.set(c); }
  choose(c: Command): void { c.run(); this.close(); }
  close(): void { this.ui.closePalette(); }
  private go(path: string): void { this.router.navigateByUrl(path); }
}

