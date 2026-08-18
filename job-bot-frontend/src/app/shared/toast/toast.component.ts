import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-wrap">
      <div class="toast" *ngFor="let t of toast.toasts()" [class]="t.type" (click)="toast.dismiss(t.id)">
        <span class="ic">{{ icon(t.type) }}</span>
        <span>{{ t.message }}</span>
      </div>
    </div>
  `,
  styles: [`
    .toast-wrap { position: fixed; top: 18px; right: 18px; z-index: 1000; display: flex; flex-direction: column; gap: 10px; }
    .toast { display:flex; align-items:center; gap:10px; padding:12px 16px; border-radius:12px; color:#fff;
      font-size:14px; font-weight:500; box-shadow:0 10px 30px rgba(0,0,0,.18); cursor:pointer; min-width:220px;
      animation: slidein .25s ease; backdrop-filter: blur(6px); }
    .toast.success { background: linear-gradient(135deg,#16a34a,#15803d); }
    .toast.error { background: linear-gradient(135deg,#dc2626,#b91c1c); }
    .toast.info { background: linear-gradient(135deg,#2563eb,#1d4ed8); }
    .ic { font-size:16px; }
    @keyframes slidein { from { transform: translateX(40px); opacity:0; } to { transform:none; opacity:1; } }
  `]
})
export class ToastComponent {
  toast = inject(ToastService);
  icon(t: string): string {
    return t === 'success' ? '✅' : t === 'error' ? '⚠️' : 'ℹ️';
  }
}


