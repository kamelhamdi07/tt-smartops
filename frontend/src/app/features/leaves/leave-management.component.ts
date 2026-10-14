import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiError, Page } from '../../core/models/api.model';
import { LeaveRequest, LeaveStatus } from '../../core/models/leave.model';
import { toApiError } from '../../core/services/api-error';
import { LeaveService } from '../../core/services/leave.service';
import { ErrorAlertComponent } from '../../shared/components/error-alert.component';
import { PagerComponent } from '../../shared/components/pager.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { LabelPipe } from '../../shared/pipes/label.pipe';

@Component({
  selector: 'app-leave-management',
  standalone: true,
  imports: [FormsModule, DatePipe, LabelPipe, StatusBadgeComponent, PagerComponent, ErrorAlertComponent],
  template: `
    <div class="page">
      <div class="page-head">
        <div><h1>Demandes de congé</h1><p>Approuvez ou refusez les demandes ; l'employé est notifié automatiquement.</p></div>
        <select class="input" [(ngModel)]="status" (ngModelChange)="load(0)" style="width: 200px">
          <option value="">Toutes les demandes</option>
          @for (s of statuses; track s) { <option [value]="s">{{ s | label }}</option> }
        </select>
      </div>

      <app-error-alert [error]="error()" />

      <section class="card">
        @if (page(); as p) {
          @if (p.content.length === 0) {
            <div class="empty"><strong>Rien à traiter</strong>Aucune demande ne correspond à ce filtre.</div>
          } @else {
            <div class="table-wrap">
              <table class="table">
                <thead><tr><th>Employé</th><th>Période</th><th>Type</th><th>Jours</th><th>Commentaire</th><th>Statut</th><th></th></tr></thead>
                <tbody>
                  @for (l of p.content; track l.id) {
                    <tr>
                      <td><span class="cell-strong">{{ l.employeeName }}</span><span class="cell-sub">{{ l.matricule }}</span></td>
                      <td>{{ l.startDate | date: 'dd/MM/yyyy' }} → {{ l.endDate | date: 'dd/MM/yyyy' }}</td>
                      <td>{{ l.type | label }}</td>
                      <td>{{ l.workingDays }}</td>
                      <td>{{ l.comment || '—' }}</td>
                      <td><app-badge [value]="l.status" />
                        @if (l.processedBy) { <span class="cell-sub">par {{ l.processedBy }}</span> }</td>
                      <td class="actions">
                        @if (l.status === 'EN_ATTENTE') {
                          <button class="btn btn-sm btn-primary" (click)="decide(l, true)">Approuver</button>
                          <button class="btn btn-sm btn-danger" (click)="decide(l, false)">Refuser</button>
                        }
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
            <app-pager [page]="p.page" [totalPages]="p.totalPages" [totalElements]="p.totalElements" (pageChange)="load($event)" />
          }
        } @else { <div class="loading">Chargement…</div> }
      </section>
    </div>
  `,
  styles: [`.actions { white-space: nowrap; text-align: right; } .actions .btn + .btn { margin-left: 6px; }`],
})
export class LeaveManagementComponent implements OnInit {
  private readonly service = inject(LeaveService);
  readonly statuses: LeaveStatus[] = ['EN_ATTENTE', 'APPROUVEE', 'REFUSEE', 'ANNULEE'];
  status: LeaveStatus | '' = 'EN_ATTENTE';

  readonly page = signal<Page<LeaveRequest> | null>(null);
  readonly error = signal<ApiError | null>(null);

  ngOnInit(): void { this.load(0); }

  load(page: number): void {
    this.service.search(this.status, page).subscribe({ next: (p) => this.page.set(p), error: (e) => this.error.set(toApiError(e)) });
  }

  decide(l: LeaveRequest, approve: boolean): void {
    const comment = approve
      ? prompt("Commentaire pour l'employé (facultatif) :", '')
      : prompt("Motif du refus (communiqué à l'employé) :", '');
    if (comment === null) return;
    this.error.set(null);
    this.service.decide(l.id, approve, comment || null).subscribe({
      next: () => this.load(this.page()?.page ?? 0),
      error: (e) => this.error.set(toApiError(e)),
    });
  }
}
