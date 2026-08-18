import { Injectable, signal } from '@angular/core';

/**
 * Small cross-component UI state (command palette open/close, etc.).
 * Kept deliberately tiny — no library, just signals.
 */
@Injectable({ providedIn: 'root' })
export class UiService {
  readonly paletteOpen = signal(false);

  openPalette(): void { this.paletteOpen.set(true); }
  closePalette(): void { this.paletteOpen.set(false); }
  togglePalette(): void { this.paletteOpen.update(v => !v); }
}

