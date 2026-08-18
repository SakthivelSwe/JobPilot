import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal, computed } from '@angular/core';
import { Observable, firstValueFrom, map, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models';

const TOKEN_KEY = 'jobpilot.jwt';
const REFRESH_KEY = 'jobpilot.refresh';

interface TokenResp { token: string; refreshToken: string; type: string; }

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private _token = signal<string | null>(localStorage.getItem(TOKEN_KEY));

  readonly token = this._token.asReadonly();
  readonly isAuthenticated = computed(() => !!this._token());

  get refreshToken(): string | null { return localStorage.getItem(REFRESH_KEY); }

  private store(t: TokenResp): void {
    localStorage.setItem(TOKEN_KEY, t.token);
    localStorage.setItem(REFRESH_KEY, t.refreshToken);
    this._token.set(t.token);
  }

  async login(username: string, password: string): Promise<void> {
    const res = await firstValueFrom(
      this.http.post<ApiResponse<TokenResp>>(
        `${environment.apiUrl}/api/auth/login`,
        { username, password }
      )
    );
    this.store(res.data);
  }

  /** Exchange the stored refresh token for a fresh access token (rotation). */
  refresh(): Observable<string> {
    return this.http
      .post<ApiResponse<TokenResp>>(`${environment.apiUrl}/api/auth/refresh`, {
        refreshToken: this.refreshToken,
      })
      .pipe(tap(res => this.store(res.data)), map(res => res.data.token));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
    this._token.set(null);
  }

  deleteAccount(): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/api/auth/me`).pipe(
      tap(() => this.logout())
    );
  }
}
