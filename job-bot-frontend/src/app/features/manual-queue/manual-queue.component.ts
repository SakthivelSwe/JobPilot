import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ManualQueueService, ManualQueueEntry } from '../../core/services/manual-queue.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-manual-queue',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page-head row">
      <div>
        <div class="page-title">Manual Applications</div>
        <div class="page-sub">High-quality jobs that need your final click. JobPilot prepared everything — you submit.</div>
      </div>
      <div class="spacer"></div>
      <div class="row" style="gap:6px;">
        <button class="btn secondary small" [class.active]="filter()==='PENDING'" (click)="setFilter('PENDING')">Pending</button>
        <button class="btn secondary small" [class.active]="filter()==='APPLIED'" (click)="setFilter('APPLIED')">Applied</button>
        <button class="btn secondary small" [class.active]="filter()===''" (click)="setFilter('')">All</button>
      </div>
    </div>

    <div class="card" style="margin-top:12px;">
      <table class="data" style="width:100%; border-collapse:collapse;">
        <thead>
          <tr class="muted" style="text-align:left; font-size:12px;">
            <th style="padding:6px 4px;">Match</th>
            <th>Company</th>
            <th>Role</th>
            <th>Source</th>
            <th>Reason</th>
            <th>Resume</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let j of items()" style="border-top:1px solid #eef2f7;">
            <td style="padding:8px 4px;"><strong>{{ j.matchScore }}</strong></td>
            <td>{{ j.company }}</td>
            <td>{{ j.role }}</td>
            <td><span class="chip">{{ j.source }}</span></td>
            <td class="muted" style="max-width:280px;">{{ j.reason }}</td>
            <td><span class="chip amber">{{ j.recommendedVariant }}</span></td>
            <td class="row" style="gap:6px; justify-content:flex-end;">
              <a class="btn secondary small" [href]="j.applicationUrl || j.jobUrl" target="_blank" rel="noopener"
                 (click)="open(j)">Open</a>
              <button class="btn small" *ngIf="j.status!=='APPLIED'" (click)="markApplied(j)">Applied ✓</button>
              <button class="btn secondary small" *ngIf="j.status!=='APPLIED'" (click)="skip(j)">Skip</button>
              <span *ngIf="j.status==='APPLIED'" class="chip green">Applied</span>
            </td>
          </tr>
          <tr *ngIf="!items().length">
            <td colspan="7" class="muted" style="padding:14px;">Nothing here 🎉 — add jobs from Discovery.</td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
  styles: [`.btn.secondary.active { background:#eef2ff; border-color:#c7d2fe; color:#4338ca; }`]
})
export class ManualQueuePageComponent implements OnInit {
  private queue = inject(ManualQueueService);
  private toast = inject(ToastService);

  items = signal<ManualQueueEntry[]>([]);
  filter = signal<string>('PENDING');

  ngOnInit(): void { this.load(); }

  setFilter(f: string): void { this.filter.set(f); this.load(); }

  load(): void {
    this.queue.list(this.filter() || undefined).subscribe(list => this.items.set(list));
  }

  open(j: ManualQueueEntry): void {
    this.queue.open(j.id).subscribe();
  }

  markApplied(j: ManualQueueEntry): void {
    this.queue.markApplied(j.id).subscribe(() => {
      this.toast.success('Marked applied — added to Kanban');
      this.load();
    });
  }

  skip(j: ManualQueueEntry): void {
    this.queue.skip(j.id).subscribe(() => { this.toast.info('Skipped'); this.load(); });
  }
}

