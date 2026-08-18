import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { PlatformConfig } from '../models';

export interface PlatformConfigUpdate {
  enabled?: boolean;
  dailyLimit?: number;
  minDelaySeconds?: number;
  // No credential fields (spec §1): JobPilot never stores platform account secrets.
}

@Injectable({ providedIn: 'root' })
export class PlatformConfigService {
  private api = inject(ApiService);

  list() { return this.api.get<PlatformConfig[]>('/api/platform-config'); }
  get(platform: string) { return this.api.get<PlatformConfig>(`/api/platform-config/${platform}`); }
  update(platform: string, body: PlatformConfigUpdate) {
    return this.api.put<PlatformConfig>(`/api/platform-config/${platform}`, body);
  }
  pause(platform: string) { return this.api.post<PlatformConfig>(`/api/platform-config/${platform}/pause`, {}); }
  resume(platform: string) { return this.api.post<PlatformConfig>(`/api/platform-config/${platform}/resume`, {}); }
  resetCount(platform: string) { return this.api.post<PlatformConfig>(`/api/platform-config/${platform}/reset-count`, {}); }
}

