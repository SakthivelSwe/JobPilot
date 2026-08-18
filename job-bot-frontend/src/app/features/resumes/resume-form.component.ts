import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ResumeService } from '../../core/services/resume.service';
import { ToastService } from '../../core/services/toast.service';
import { Resume } from '../../core/models';

@Component({
  selector: 'app-resume-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-title">{{ id ? 'Edit' : 'New' }} Resume</div>
    <div class="page-sub">Paste your resume text so the ATS engine can score jobs against it</div>

    <div class="card" style="max-width:760px;">
      <div class="field">
        <label>Name</label>
        <input [(ngModel)]="model.name" placeholder="e.g. Java Backend Senior" />
      </div>
      <div class="field">
        <label>Target Roles (comma separated)</label>
        <input [(ngModel)]="rolesText" placeholder="Senior Java Developer, Backend Engineer" />
      </div>
      <div class="field">
        <label>Target Skills (comma separated)</label>
        <input [(ngModel)]="skillsText" placeholder="Java, Spring Boot, Kafka, AWS, REST" />
      </div>
      <div class="field">
        <label>Resume Text</label>
        <textarea [(ngModel)]="model.resumeText" rows="10"
          placeholder="Paste the plain text of your resume here"></textarea>
      </div>
      <div class="field">
        <label>Experience Summary</label>
        <input [(ngModel)]="model.experienceSummary" placeholder="2.8 yrs Java/Spring Boot/Kafka/AWS" />
      </div>
      <div class="field">
        <label><input type="checkbox" [(ngModel)]="model.active" /> Active</label>
      </div>
      <div class="row">
        <button class="btn" (click)="save()">Save</button>
        <button class="btn secondary" (click)="cancel()">Cancel</button>
        <span class="muted" *ngIf="error">{{ error }}</span>
      </div>
    </div>
  `
})
export class ResumeFormComponent implements OnInit {
  private service = inject(ResumeService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private toast = inject(ToastService);

  id: string | null = null;
  model: Partial<Resume> = { active: true };
  rolesText = '';
  skillsText = '';
  error = '';

  ngOnInit(): void {
    this.id = this.route.snapshot.paramMap.get('id');
    if (this.id) {
      this.service.get(this.id).subscribe(r => {
        this.model = r;
        this.rolesText = (r.targetRoles || []).join(', ');
        this.skillsText = (r.targetSkills || []).join(', ');
      });
    }
  }

  private split(s: string): string[] {
    return s.split(',').map(x => x.trim()).filter(x => x);
  }

  save(): void {
    if (!this.model.name) { this.error = 'Name is required'; return; }
    const body: Partial<Resume> = {
      ...this.model,
      targetRoles: this.split(this.rolesText),
      targetSkills: this.split(this.skillsText)
    };
    const obs = this.id
      ? this.service.update(this.id, body)
      : this.service.create(body);
    obs.subscribe({
      next: () => { this.toast.success('Resume saved'); this.router.navigate(['/resumes']); },
      error: e => { this.error = e?.error?.message || 'Save failed'; this.toast.error(this.error); }
    });
  }

  cancel(): void { this.router.navigate(['/resumes']); }
}


