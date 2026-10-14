import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ApiError } from '../../core/models/api.model';
import { Employee } from '../../core/models/employee.model';
import { toApiError } from '../../core/services/api-error';
import { EmployeeService } from '../../core/services/employee.service';
import { ErrorAlertComponent } from '../../shared/components/error-alert.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { LabelPipe } from '../../shared/pipes/label.pipe';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [DatePipe, LabelPipe, StatusBadgeComponent, ErrorAlertComponent],
  template: `
    <div class="page">
      <div class="page-head"><h1>Mon profil</h1></div>
      <app-error-alert [error]="error()" />
      @if (me(); as e) {
        <section class="card">
          <div class="card-head">
            <h2>{{ e.fullName }}</h2>
            <app-badge [value]="e.status" />
          </div>
          <div class="card-body">
            <dl class="details">
              <dt>Matricule</dt><dd>{{ e.matricule }}</dd>
              <dt>Fonction</dt><dd>{{ e.jobTitle }}</dd>
              <dt>Email</dt><dd>{{ e.email }}</dd>
              <dt>Téléphone</dt><dd>{{ e.phone || '—' }}</dd>
              <dt>Département</dt><dd>{{ e.departmentName || '—' }}</dd>
              <dt>Équipe</dt><dd>{{ e.teamName || '—' }}</dd>
              <dt>Date d'embauche</dt><dd>{{ e.hireDate | date: 'longDate' }}</dd>
              @if (e.technician) { <dt>Spécialité</dt><dd>{{ e.specialty | label }}</dd> }
            </dl>
            <p class="muted">Pour corriger une information, contactez le service RH.</p>
          </div>
        </section>
      }
    </div>
  `,
})
export class ProfileComponent implements OnInit {
  private readonly employees = inject(EmployeeService);
  readonly me = signal<Employee | null>(null);
  readonly error = signal<ApiError | null>(null);

  ngOnInit(): void {
    this.employees.me().subscribe({ next: (e) => this.me.set(e), error: (e) => this.error.set(toApiError(e)) });
  }
}
