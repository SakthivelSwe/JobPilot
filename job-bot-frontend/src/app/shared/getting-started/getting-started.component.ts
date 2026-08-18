import { Component, HostListener, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { OnboardingService } from '../../core/services/onboarding.service';

@Component({
  selector: 'jp-getting-started',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="gs-anchor">
      <button class="gs-btn" (click)="toggle()" [class.done]="onb.allDone()" [attr.aria-expanded]="open()"
              [attr.aria-label]="onb.allDone() ? 'Setup complete' : 'Setup ' + onb.doneCount() + ' of ' + onb.totalSteps()"
              [title]="onb.allDone() ? 'Setup complete — open guide' : ('Setup ' + onb.doneCount() + '/' + onb.totalSteps() + ' — click for checklist')">
        <span class="gs-ring">
          <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
            <circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="2" opacity=".22"/>
            <circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="2"
                    stroke-dasharray="62.83" [attr.stroke-dashoffset]="dashOffset()"
                    stroke-linecap="round" transform="rotate(-90 12 12)"/>
          </svg>
        </span>
        <span class="gs-count" *ngIf="!onb.allDone()">{{ onb.doneCount() }}/{{ onb.totalSteps() }}</span>
        <span class="gs-count done" *ngIf="onb.allDone()" aria-hidden="true">✓</span>
      </button>

      <div class="gs-pop" *ngIf="open()" (click)="$event.stopPropagation()" role="dialog" aria-label="Getting started">
        <div class="gs-head">
          <div class="gs-h-l">
            <div class="kicker">Getting started</div>
            <div class="gs-h-title">{{ onb.doneCount() }} of {{ onb.totalSteps() }} done</div>
          </div>
          <button class="gs-close" (click)="close()" aria-label="Close">✕</button>
        </div>

        <div class="gs-bar"><div class="gs-fill" [style.width.%]="onb.progressPct()"></div></div>

        <a *ngFor="let s of onb.steps(); let i = index"
           class="gs-item" [class.on]="s.done" [routerLink]="s.route" (click)="close()">
          <span class="gs-idx numeric" *ngIf="!s.done">{{ i + 1 | number: '2.0' }}</span>
          <span class="gs-check" *ngIf="s.done" aria-hidden="true">✓</span>
          <span class="gs-body">
            <span class="gs-title">{{ s.label }}</span>
            <span class="gs-hint">{{ s.hint }}</span>
          </span>
          <span class="gs-go" *ngIf="!s.done">→</span>
        </a>

        <div class="gs-foot">
          <button class="btn ghost small" (click)="showTour()">Show welcome tour</button>
          <a class="btn ghost small" routerLink="/help" (click)="close()">Open user guide</a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .gs-anchor { position:relative; }
    .gs-btn { display:inline-flex; align-items:center; gap:6px; background:var(--bg-tint);
      border:1px solid var(--line); border-radius:999px; padding:4px 10px 4px 6px; height:32px;
      cursor:pointer; font:600 12px var(--font-mono); color:var(--ink-2); }
    .gs-btn:hover { border-color:var(--line-strong); color:var(--ink); }
    .gs-btn.done { background:var(--success-wash); border-color:transparent; color:var(--success); }
    .gs-ring { display:inline-flex; color:var(--accent); }
    .gs-btn.done .gs-ring { color:var(--success); }
    .gs-count { white-space:nowrap; }
    .gs-count.done { font-size:13px; }

    .gs-pop { position:absolute; top:calc(100% + 8px); right:0; z-index:120;
      width:360px; background:var(--surface); border:1px solid var(--line-strong);
      border-radius:var(--radius); box-shadow:0 20px 60px rgba(32,30,27,0.22);
      animation:gspop .14s ease; }
    @keyframes gspop { from { transform:translateY(-4px); opacity:0; } to { transform:none; opacity:1; } }

    .gs-head { display:flex; align-items:flex-start; gap:8px; padding:14px 16px 4px; }
    .gs-h-l { flex:1; min-width:0; }
    .gs-h-title { font-family:var(--font-display); font-size:16px; font-weight:600; color:var(--ink); margin-top:2px; }
    .gs-close { background:transparent; border:0; cursor:pointer; color:var(--ink-3); font-size:13px; padding:2px 6px; }
    .gs-close:hover { color:var(--ink); }

    .gs-bar { margin:6px 16px 8px; height:5px; background:var(--bg-tint); border-radius:999px; overflow:hidden; }
    .gs-fill { height:100%; background:var(--accent); border-radius:999px; transition:width .3s ease; }

    .gs-item { display:flex; align-items:flex-start; gap:12px; padding:10px 16px; text-decoration:none;
      color:inherit; border-top:1px solid var(--line); transition:background .12s; }
    .gs-item:first-of-type { border-top:0; }
    .gs-item:hover { background:var(--surface-2); text-decoration:none; }
    .gs-idx { display:inline-flex; align-items:center; justify-content:center;
      width:22px; height:22px; border-radius:50%; background:var(--bg-tint);
      font-size:11px; font-weight:600; color:var(--ink-3); flex-shrink:0; margin-top:1px; }
    .gs-check { display:inline-flex; align-items:center; justify-content:center;
      width:22px; height:22px; border-radius:50%; background:var(--success);
      color:#fff; font-size:12px; font-weight:700; flex-shrink:0; margin-top:1px; }
    .gs-body { flex:1; display:flex; flex-direction:column; gap:2px; min-width:0; }
    .gs-title { font-weight:600; color:var(--ink); font-size:13.5px; }
    .gs-item.on .gs-title { color:var(--ink-3); text-decoration:line-through; }
    .gs-hint { color:var(--ink-3); font-size:12px; line-height:1.4; }
    .gs-go { color:var(--accent); font-weight:600; align-self:center; }

    .gs-foot { display:flex; gap:6px; align-items:center; padding:8px 12px 10px;
      border-top:1px solid var(--line); }
  `]
})
export class GettingStartedComponent {
  onb = inject(OnboardingService);
  private router = inject(Router);
  open = signal(false);

  ngOnInit() {
    // Refresh whenever component becomes visible
    this.onb.refresh();
  }

  toggle(): void {
    this.open.update(v => !v);
    if (this.open()) this.onb.refresh();
  }
  close(): void { this.open.set(false); }
  showTour(): void { this.onb.openWelcome(); this.close(); }

  /** SVG circle progress (2πr ≈ 62.83 for r=10). */
  dashOffset(): number {
    const total = 62.83;
    return total - (total * this.onb.progressPct()) / 100;
  }

  @HostListener('document:click', ['$event'])
  onDocClick(e: MouseEvent): void {
    if (!this.open()) return;
    const target = e.target as HTMLElement;
    if (!target.closest('.gs-anchor')) this.close();
  }

  @HostListener('document:keydown.escape')
  onEsc(): void { this.close(); }
}

