import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { ApiService } from './api.service';

/**
 * Data export & ownership (spec §69/§70). Downloads use HttpClient (so the auth
 * interceptor attaches the JWT) and stream the response as a file.
 */
@Injectable({ providedIn: 'root' })
export class AccountService {
  private http = inject(HttpClient);
  private api = inject(ApiService);
  private base = environment.apiUrl;

  download(path: string, filename: string): void {
    this.http.get(`${this.base}${path}`, { responseType: 'blob' }).subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  reset() { return this.api.post<Record<string, number>>('/api/account/reset', {}); }
}

