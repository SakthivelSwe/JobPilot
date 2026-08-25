import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface PlatformSession {
  platformName: string;
  /** DISCONNECTED | CONNECTED | EXPIRED | ERROR */
  sessionStatus: string;
  sessionActive: boolean;
  sessionUsername: string | null;
  sessionConnectedAt: string | null;
}

interface ApiResponse<T> {
  data: T;
  message: string;
}

import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PlatformSessionService {
  private base = `${environment.apiUrl}/api/platform-config`;

  constructor(private http: HttpClient) {}

  getSession(platform: string): Observable<PlatformSession> {
    return this.http
      .get<ApiResponse<PlatformSession>>(`${this.base}/${platform}/session`)
      .pipe(map(r => r.data));
  }

  connect(platform: string): Observable<PlatformSession> {
    return this.http
      .post<ApiResponse<PlatformSession>>(`${this.base}/${platform}/connect`, {})
      .pipe(map(r => r.data));
  }

  validate(platform: string): Observable<PlatformSession> {
    return this.http
      .post<ApiResponse<PlatformSession>>(`${this.base}/${platform}/validate`, {})
      .pipe(map(r => r.data));
  }

  disconnect(platform: string): Observable<PlatformSession> {
    return this.http
      .post<ApiResponse<PlatformSession>>(`${this.base}/${platform}/disconnect`, {})
      .pipe(map(r => r.data));
  }
}
