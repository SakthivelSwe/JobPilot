import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';

export interface ActivityEvent {
  id: string;
  type: string;
  title: string;
  detail?: string;
  entityType?: string;
  entityId?: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class ActivityService {
  private api = inject(ApiService);
  recent(limit = 20) { return this.api.get<ActivityEvent[]>('/api/activity', { limit }); }
}

