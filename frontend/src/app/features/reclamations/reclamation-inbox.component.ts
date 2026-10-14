import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError, Page } from '../../core/models/api.model';
import { Team } from '../../core/models/employee.model';
import { Priority } from '../../core/models/intervention.model';
import { DispatchOptions, Reclamation, ReclamationStatus } from '../../core/models/reclamation.model';
import { toApiError } from '../../core/services/api-error';
import { EmployeeService } from '../../core/services/employee.service';
import { ReclamationService } from '../../core/services/reclamation.service';
import { ErrorAlertComponent } from '../../shared/components/error-alert.component';
import { PagerComponent } from '../../shared/components/pager.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { LabelPipe } from '../../shared/pipes/label.pipe';
import { fromInputDateTime } from '../../shared/utils/dates';
import { PRIORITIES, RECLAMATION_STATUSES, options } from '../../shared/utils/labels';

/** Boîte de réception du chef d'équipe : affecter technicien et véhicule à chaque réclamation client. */
@Component({
  selector: 'app-reclamation-inbox',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, RouterLink, DatePipe, LabelPipe, StatusBadgeComponent, PagerComponent, ErrorAlertComponent],
  template: `
    <div class="page">
      <div class="page-head">
        <div><h1>Réclamations clients</h1><p>Choisissez le technicien et le véhicule : une intervention de dépannage est créée et le client est informé.</p></div>
      </div>

      @if (selected(); as r) {
        <section class="card">
          <div class="card-head">
            <div><h2>{{ r.reference }} · {{ r.title }}</h2><span class="muted">{{ r.category | label }} · {{ r.teamName || 'Aucune équipe' }}</span></div>
            <button class="btn btn-sm" type="button" (click)="close()">Fermer</button>
          </div>
          <div class="card-body">
            <p class="pre">{{ r.description }}</p>
            <dl class="details">
              <dt>Client</dt><dd>{{ r.clientName }} · {{ r.clientEmail }}</dd>
              <dt>Téléphone</dt><dd>{{ r.phone }}</dd>
              <dt>Adresse</dt><dd>{{ r.address }}</dd>
            </dl>
          </div>

          @if (!r.teamId && canRoute) {
            <div class="card-body row" style="border-top: 1px solid var(--line)">
              <span>Aucune équipe ne couvre la catégorie {{ r.category | label }}. Transmettre à :</span>
              <select class="input route" [(ngModel)]="routeTeamId">
                <option [ngValue]="null" disabled>Choisir une équipe</option>
                @for (t of teams(); track t.id) { <option [ngValue]="t.id">{{ t.name }}</option> }
              </select>
              <button class="btn btn-primary btn-sm" type="button" [disabled]="!routeTeamId" (click)="route(r)">Transmettre</button>
            </div>
          }

          @if (r.canDispatch) {
            <form class="card-body form-grid" style="border-top: 1px solid var(--line)" [formGroup]="form" (ngSubmit)="dispatch(r)" novalidate>
              <div class="field"><label for="tech">Technicien</label>
                <select id="tech" class="input" formControlName="technicianId">
                  <option [ngValue]="null" disabled>Choisir</option>
                  @for (t of opts()?.technicians ?? []; track t.id) { <option [ngValue]="t.id">{{ t.fullName }} ({{ t.specialty | label }})</option> }
                </select>
                @if (opts() && !opts()!.technicians.length) { <span class="field-error">Aucun technicien actif dans cette équipe.</span> }
              </div>
              <div class="field"><label for="veh">Véhicule</label>
                <select id="veh" class="input" formControlName="vehicleId">
                  <option [ngValue]="null">Sans véhicule</option>
                  @for (v of opts()?.vehicles ?? []; track v.id) { <option [ngValue]="v.id">{{ v.plateNumber }} · {{ v.brandModel }}{{ v.teamName ? ' (' + v.teamName + ')' : '' }}</option> }
                </select></div>
              <div class="field"><label for="prio">Priorité</label>
                <select id="prio" class="input" formControlName="priority">
                  @for (p of priorities; track p.value) { <option [value]="p.value">{{ p.label }}</option> }
                </select></div>
              <div class="field"><label for="start">Début du passage</label><input id="start" type="datetime-local" class="input" formControlName="plannedStart" /></div>
              <div class="field"><label for="end">Fin prévue</label><input id="end" type="datetime-local" class="input" formControlName="plannedEnd" /></div>
              <div class="field full"><app-error-alert [error]="formError()" /></div>
              <div class="field full row">
                <span class="spacer"></span>
                <button class="btn btn-primary" type="submit" [disabled]="form.invalid || busy()">Affecter et envoyer le technicien</button>
              </div>
            </form>
          } @else if (r.interventionId) {
            <div class="card-body" style="border-top: 1px solid var(--line)">
              <dl class="details">
                <dt>Intervention</dt><dd><a [routerLink]="['/interventions', r.interventionId]">{{ r.interventionReference }}</a></dd>
                <dt>Technicien</dt><dd>{{ r.technicians.join(', ') || '—' }}</dd>
                <dt>Véhicule</dt><dd>{{ r.vehiclePlate || '—' }}</dd>
                <dt>Créneau</dt><dd>{{ r.plannedStart | date: 'dd/MM/yyyy HH:mm' }} – {{ r.plannedEnd | date: 'HH:mm' }}</dd>
              </dl>
            </div>
          }
        </section>
      }

      <section class="card">
        <div class="card-head filters">
          <select class="input" [(ngModel)]="status" (ngModelChange)="load(0)">
            <option value="">Tous les statuts</option>
            @for (s of statuses; track s.value) { <option [value]="s.value">{{ s.label }}</option> }
          </select>
        </div>
        <app-error-alert [error]="error()" />
        @if (page(); as p) {
          @if (p.content.length === 0) {
            <div class="empty"><strong>Aucune réclamation</strong>Les réclamations des clients pour votre équipe arriveront ici.</div>
          } @else {
            <div class="table-wrap">
              <table class="table">
                <thead><tr><th>Réclamation</th><th>Client</th><th>Adresse</th><th>Équipe</th><th>Technicien</th><th>Statut</th><th></th></tr></thead>
                <tbody>
                  @for (r of p.content; track r.id) {
                    <tr [class.active]="selected()?.id === r.id">
                      <td><span class="cell-strong">{{ r.title }}</span><span class="cell-sub">{{ r.reference }} · {{ r.category | label }} · {{ r.createdAt | date: 'dd/MM HH:mm' }}</span></td>
                      <td>{{ r.clientName }}<span class="cell-sub">{{ r.phone }}</span></td>
                      <td>{{ r.address }}</td>
                      <td>{{ r.teamName || '—' }}</td>
                      <td>{{ r.technicians.join(', ') || '—' }}</td>
                      <td><app-badge [value]="r.status" /></td>
                      <td class="actions">
                        <button class="btn btn-sm" [class.btn-primary]="r.canDispatch" (click)="open(r)">{{ r.canDispatch ? 'Affecter' : 'Voir' }}</button>
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
  styles: [`
    .pre { white-space: pre-wrap; margin: 0 0 12px; }
    .filters { justify-content: flex-start; }
    .filters .input, .route { width: auto; min-width: 200px; }
    .actions { white-space: nowrap; text-align: right; }
    tr.active td { background: var(--blue-soft); }
  `],
})
export class ReclamationInboxComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ReclamationService);
  private readonly employeeService = inject(EmployeeService);
  readonly canRoute = inject(AuthService).hasRole('ADMIN', 'RESPONSABLE_TECHNIQUE');

  readonly statuses = options(RECLAMATION_STATUSES);
  readonly priorities = options(PRIORITIES);
  readonly page = signal<Page<Reclamation> | null>(null);
  readonly selected = signal<Reclamation | null>(null);
  readonly opts = signal<DispatchOptions | null>(null);
  readonly teams = signal<Team[]>([]);
  readonly error = signal<ApiError | null>(null);
  readonly formError = signal<ApiError | null>(null);
  readonly busy = signal(false);

  status: ReclamationStatus | '' = 'NOUVELLE';
  routeTeamId: number | null = null;
  private current = 0;

  readonly form = this.fb.group({
    technicianId: [null as number | null, Validators.required],
    vehicleId: [null as number | null],
    priority: ['NORMALE' as Priority, Validators.required],
    plannedStart: ['', Validators.required],
    plannedEnd: ['', Validators.required],
  });

  constructor() {
    // Le statut change sous l'action d'autres utilisateurs : on recharge la page affichée toutes les 15 s.
    interval(15_000).pipe(takeUntilDestroyed()).subscribe(() => this.load(this.current));
  }

  ngOnInit(): void {
    if (this.canRoute) this.employeeService.teams().subscribe((t) => this.teams.set(t));
    this.load(0);
  }

  load(page: number): void {
    this.current = page;
    this.service.search(this.status, page).subscribe({
      next: (p) => { this.page.set(p); this.error.set(null); },
      error: (e) => this.error.set(toApiError(e)),
    });
  }

  open(r: Reclamation): void {
    this.selected.set(r);
    this.opts.set(null);
    this.formError.set(null);
    this.routeTeamId = null;
    // créneau proposé : dans une heure, pour deux heures
    const start = new Date(Date.now() + 60 * 60 * 1000);
    start.setMinutes(0, 0, 0);
    const end = new Date(start.getTime() + 2 * 60 * 60 * 1000);
    this.form.reset({ technicianId: null, vehicleId: null, priority: 'NORMALE', plannedStart: toInput(start), plannedEnd: toInput(end) });
    if (r.canDispatch) {
      this.service.options(r.id).subscribe({ next: (o) => this.opts.set(o), error: (e) => this.formError.set(toApiError(e)) });
    }
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  close(): void {
    this.selected.set(null);
  }

  dispatch(r: Reclamation): void {
    const v = this.form.getRawValue();
    this.busy.set(true);
    this.formError.set(null);
    this.service.dispatch(r.id, {
      technicianId: v.technicianId!, vehicleId: v.vehicleId, priority: v.priority!,
      plannedStart: fromInputDateTime(v.plannedStart)!, plannedEnd: fromInputDateTime(v.plannedEnd)!,
    }).subscribe({
      next: (updated) => { this.busy.set(false); this.selected.set(updated); this.load(this.current); },
      error: (e) => { this.busy.set(false); this.formError.set(toApiError(e)); },
    });
  }

  route(r: Reclamation): void {
    if (!this.routeTeamId) return;
    this.service.route(r.id, this.routeTeamId).subscribe({
      next: (updated) => { this.open(updated); this.load(this.current); },
      error: (e) => this.formError.set(toApiError(e)),
    });
  }
}

function toInput(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
