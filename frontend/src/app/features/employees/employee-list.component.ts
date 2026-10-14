import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subject, debounceTime, switchMap } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError, Page } from '../../core/models/api.model';
import { Department, Employee, EmployeeStatus } from '../../core/models/employee.model';
import { toApiError } from '../../core/services/api-error';
import { EmployeeFilter, EmployeeService } from '../../core/services/employee.service';
import { ErrorAlertComponent } from '../../shared/components/error-alert.component';
import { PagerComponent } from '../../shared/components/pager.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { LabelPipe } from '../../shared/pipes/label.pipe';

@Component({
  selector: 'app-employee-list',
  standalone: true,
  imports: [FormsModule, RouterLink, LabelPipe, StatusBadgeComponent, PagerComponent, ErrorAlertComponent],
  template: `
    <div class="page">
      <div class="page-head">
        <div>
          <h1>Employés</h1>
          <p>Fiches du personnel, rattachement aux départements et équipes.</p>
        </div>
        @if (canEdit) { <a class="btn btn-primary" routerLink="/employees/new">Ajouter un employé</a> }
      </div>

      <section class="card">
        <div class="card-head filters">
          <input class="input" type="search" placeholder="Nom, matricule ou email" [(ngModel)]="filter.q" (ngModelChange)="reload()" />
          <select class="input" [(ngModel)]="filter.departmentId" (ngModelChange)="reload()">
            <option [ngValue]="null">Tous les départements</option>
            @for (d of departments(); track d.id) { <option [ngValue]="d.id">{{ d.name }}</option> }
          </select>
          <select class="input" [(ngModel)]="filter.status" (ngModelChange)="reload()">
            <option value="">Tous les statuts</option>
            @for (s of statuses; track s) { <option [value]="s">{{ s | label }}</option> }
          </select>
          <select class="input" [(ngModel)]="filter.technician" (ngModelChange)="reload()">
            <option [ngValue]="null">Tous les profils</option>
            <option [ngValue]="true">Techniciens</option>
            <option [ngValue]="false">Autres employés</option>
          </select>
        </div>

        <app-error-alert [error]="error()" />

        @if (page(); as p) {
          @if (p.content.length === 0) {
            <div class="empty"><strong>Aucun employé trouvé</strong>Modifiez les filtres ou ajoutez une fiche.</div>
          } @else {
            <div class="table-wrap">
              <table class="table">
                <thead><tr><th>Employé</th><th>Fonction</th><th>Département / équipe</th><th>Spécialité</th><th>Statut</th>@if (canEdit) {<th></th>}</tr></thead>
                <tbody>
                  @for (e of p.content; track e.id) {
                    <tr>
                      <td><span class="cell-strong">{{ e.fullName }}</span><span class="cell-sub">{{ e.matricule }} · {{ e.email }}</span></td>
                      <td>{{ e.jobTitle }}</td>
                      <td>{{ e.departmentName || '—' }}<span class="cell-sub">{{ e.teamName || 'Sans équipe' }}</span></td>
                      <td>{{ e.technician ? (e.specialty | label) : '—' }}</td>
                      <td><app-badge [value]="e.status" /></td>
                      @if (canEdit) {
                        <td class="actions">
                          <button class="btn btn-sm" (click)="edit(e)">Modifier</button>
                          @if (e.status === 'ACTIF') {
                            <button class="btn btn-sm btn-danger" (click)="setStatus(e, 'INACTIF')">Désactiver</button>
                          } @else {
                            <button class="btn btn-sm" (click)="setStatus(e, 'ACTIF')">Réactiver</button>
                          }
                        </td>
                      }
                    </tr>
                  }
                </tbody>
              </table>
            </div>
            <app-pager [page]="p.page" [totalPages]="p.totalPages" [totalElements]="p.totalElements" (pageChange)="goTo($event)" />
          }
        } @else { <div class="loading">Chargement…</div> }
      </section>
    </div>
  `,
  styles: [`
    .filters { flex-wrap: wrap; justify-content: flex-start; }
    .filters .input { width: auto; min-width: 180px; }
    .filters input { flex: 1; min-width: 240px; }
    .actions { white-space: nowrap; text-align: right; }
    .actions .btn + .btn { margin-left: 6px; }
  `],
})
export class EmployeeListComponent implements OnInit {
  private readonly service = inject(EmployeeService);
  private readonly router = inject(Router);
  readonly canEdit = inject(AuthService).hasRole('RH');

  readonly statuses: EmployeeStatus[] = ['ACTIF', 'SUSPENDU', 'INACTIF'];
  readonly page = signal<Page<Employee> | null>(null);
  readonly departments = signal<Department[]>([]);
  readonly error = signal<ApiError | null>(null);

  filter: EmployeeFilter = { q: '', departmentId: null, status: '', technician: null, page: 0, size: 20 };
  private readonly trigger = new Subject<void>();

  ngOnInit(): void {
    this.service.departments().subscribe((d) => this.departments.set(d));
    this.trigger.pipe(debounceTime(250), switchMap(() => this.service.search(this.filter))).subscribe({
      next: (p) => { this.page.set(p); this.error.set(null); },
      error: (e) => this.error.set(toApiError(e)),
    });
    this.trigger.next();
  }

  reload(): void {
    this.filter.page = 0;
    this.trigger.next();
  }

  goTo(page: number): void {
    this.filter.page = page;
    this.trigger.next();
  }

  edit(e: Employee): void {
    this.router.navigate(['/employees', e.id, 'edit']);
  }

  setStatus(e: Employee, status: EmployeeStatus): void {
    const verb = status === 'ACTIF' ? 'réactiver' : 'désactiver';
    if (!confirm(`Voulez-vous ${verb} ${e.fullName} ? Son accès à l'application suivra ce statut.`)) return;
    this.service.changeStatus(e.id, status).subscribe({
      next: () => this.trigger.next(),
      error: (err) => this.error.set(toApiError(err)),
    });
  }
}
