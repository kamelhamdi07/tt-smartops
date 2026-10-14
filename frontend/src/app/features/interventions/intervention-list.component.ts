import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subject, debounceTime, switchMap } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError, Page } from '../../core/models/api.model';
import { InterventionListItem, InterventionStatus, Priority } from '../../core/models/intervention.model';
import { toApiError } from '../../core/services/api-error';
import { InterventionFilter, InterventionService } from '../../core/services/intervention.service';
import { ErrorAlertComponent } from '../../shared/components/error-alert.component';
import { PagerComponent } from '../../shared/components/pager.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { LabelPipe } from '../../shared/pipes/label.pipe';
import { INTERVENTION_STATUSES, PRIORITIES, options } from '../../shared/utils/labels';

@Component({
  selector: 'app-intervention-list',
  standalone: true,
  imports: [FormsModule, RouterLink, DatePipe, LabelPipe, StatusBadgeComponent, PagerComponent, ErrorAlertComponent],
  template: `
    <div class="page">
      <div class="page-head">
        <div>
          <h1>{{ isTechnician ? 'Mes interventions' : 'Interventions' }}</h1>
          <p>{{ isTechnician ? 'Les interventions qui vous sont affectées, de la plus proche à la plus lointaine.'
                             : 'Création, planification et suivi des opérations techniques.' }}</p>
        </div>
        @if (canCreate) { <a class="btn btn-primary" routerLink="/interventions/new">Nouvelle intervention</a> }
      </div>

      <section class="card">
        <div class="card-head filters">
          @if (!isTechnician) {
            <input class="input" type="search" placeholder="Référence, titre ou lieu" [(ngModel)]="filter.q" (ngModelChange)="reload()" />
            <select class="input" [(ngModel)]="filter.priority" (ngModelChange)="reload()">
              <option value="">Toutes priorités</option>
              @for (p of priorities; track p.value) { <option [value]="p.value">{{ p.label }}</option> }
            </select>
          }
          <select class="input" [(ngModel)]="filter.status" (ngModelChange)="reload()">
            <option value="">Tous les statuts</option>
            @for (s of statuses; track s.value) { <option [value]="s.value">{{ s.label }}</option> }
          </select>
        </div>

        <app-error-alert [error]="error()" />

        @if (page(); as p) {
          @if (p.content.length === 0) {
            <div class="empty"><strong>Aucune intervention</strong>
              {{ isTechnician ? 'Rien ne vous est affecté pour le moment.' : 'Créez une intervention pour commencer.' }}</div>
          } @else {
            <div class="table-wrap">
              <table class="table">
                <thead><tr><th>Intervention</th><th>Type</th><th>Lieu</th><th>Créneau</th><th>Priorité</th><th>Statut</th></tr></thead>
                <tbody>
                  @for (i of p.content; track i.id) {
                    <tr class="clickable" (click)="open(i)" [class.critical]="i.priority === 'CRITIQUE'">
                      <td><span class="cell-strong">{{ i.title }}</span><span class="cell-sub">{{ i.reference }}{{ i.teamName ? ' · ' + i.teamName : '' }}</span></td>
                      <td>{{ i.type | label }}</td>
                      <td>{{ i.location }}</td>
                      <td>
                        @if (i.plannedStart) {
                          {{ i.plannedStart | date: 'EEE dd/MM' }}<span class="cell-sub">{{ i.plannedStart | date: 'HH:mm' }} – {{ i.plannedEnd | date: 'HH:mm' }}</span>
                        } @else { <span class="muted">À planifier</span> }
                      </td>
                      <td><app-badge [value]="i.priority" /></td>
                      <td><app-badge [value]="i.status" />@if (i.late) { <span class="late">En retard</span> }</td>
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
    tr.critical td:first-child { box-shadow: inset 3px 0 0 var(--danger); }
    .late { display: block; font-size: 12px; color: var(--danger); font-weight: 500; margin-top: 2px; }
  `],
})
export class InterventionListComponent implements OnInit {
  /** Filtre initial passé en query param (?status=EN_COURS) depuis le tableau de bord */
  readonly status = input<string>();

  private readonly service = inject(InterventionService);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly isTechnician = this.auth.hasRole('TECHNICIEN');
  readonly canCreate = this.auth.hasRole('RESPONSABLE_TECHNIQUE');
  readonly statuses = options(INTERVENTION_STATUSES);
  readonly priorities = options(PRIORITIES);

  readonly page = signal<Page<InterventionListItem> | null>(null);
  readonly error = signal<ApiError | null>(null);
  filter: InterventionFilter = { q: '', status: '', priority: '', page: 0 };
  private readonly trigger = new Subject<void>();

  ngOnInit(): void {
    this.filter.status = (this.status() ?? '') as InterventionStatus | '';
    this.trigger.pipe(
      debounceTime(250),
      switchMap(() => this.isTechnician
        ? this.service.mine(this.filter.status, this.filter.page)
        : this.service.search(this.filter))
    ).subscribe({
      next: (p) => { this.page.set(p); this.error.set(null); },
      error: (e) => this.error.set(toApiError(e)),
    });
    this.trigger.next();
  }

  reload(): void { this.filter.page = 0; this.trigger.next(); }
  goTo(page: number): void { this.filter.page = page; this.trigger.next(); }
  open(i: InterventionListItem): void { this.router.navigate(['/interventions', i.id]); }
}
