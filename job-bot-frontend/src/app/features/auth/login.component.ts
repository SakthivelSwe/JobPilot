import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div style="max-width: 360px; margin: 8vh auto; padding: 24px;
                background:#fff; border-radius: 12px; box-shadow: 0 4px 24px rgba(0,0,0,.06);">
      <h2 style="margin: 0 0 4px;">JobPilot</h2>
      <div class="muted" style="margin-bottom: 20px;">Sign in to continue</div>

      <label style="display:block; font-size:12px;">Username
        <input [(ngModel)]="username" style="width:100%; padding:8px;" autofocus />
      </label>
      <label style="display:block; font-size:12px; margin-top:10px;">Password
        <input type="password" [(ngModel)]="password" style="width:100%; padding:8px;"
               (keyup.enter)="submit()" />
      </label>

      <button class="btn" style="width:100%; margin-top:16px;" [disabled]="loading()" (click)="submit()">
        {{ loading() ? 'Signing in…' : 'Sign in' }}
      </button>
    </div>
  `
})
export class LoginPageComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  private toast = inject(ToastService);

  username = 'admin';
  password = '';
  loading = signal(false);

  async submit(): Promise<void> {
    if (!this.username || !this.password) return;
    this.loading.set(true);
    try {
      await this.auth.login(this.username, this.password);
      this.router.navigateByUrl('/dashboard');
    } catch (e: any) {
      this.toast.error('Login failed');
    } finally {
      this.loading.set(false);
    }
  }
}


