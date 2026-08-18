import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';

export interface SearchHit { kind: string; title: string; subtitle: string; route: string; }
export interface SearchResults { query: string; total: number; hits: SearchHit[]; }

@Injectable({ providedIn: 'root' })
export class SearchService {
  private api = inject(ApiService);
  search(q: string) { return this.api.get<SearchResults>('/api/search', { q }); }
}

