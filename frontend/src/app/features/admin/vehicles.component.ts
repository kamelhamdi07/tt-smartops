import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, debounceTime, switchMap } from 'rxjs';
import { AssetStatus, Vehicle } from '../../core/models/admin.model';
import { ApiError, Page } from '../../core/models/api.model';
import { Team } from '../../core/models/employee.model';
import { AdminService, AssetFilter } from '../../core/services/admin.service';
import { toApiError } from '../../core/services/api-error';
import { EmployeeService } from '../../core/services/employee.service';
import { ErrorAlertComponent } from '../../shared/components/error-alert.component';
import { PagerComponent } from '../../shared/components/pager.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { ASSET_STATUSES, options } from '../../shared/utils/labels';

@Component({
  selector: 'app-vehicles',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, DatePipe, StatusBadgeComponent, PagerComponent, ErrorAlertComponent],
  template: `
    <div class="page">
      <div class="page-head">
        <div><h1>Véhicules</h1><p>Parc automobile affecté aux équipes d'intervention.</p></div>
      </div>

      <form class="card" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <div class="card-head"><h2>{{ editing() ? 'Modifier le véhicule' : 'Nouveau véhicule' }}</h2></div>
        <div class="card-body form-grid">
          <div class="field"><label for="plate">Immatriculation</label><input id="plate" class="input" formControlName="plateNumber" maxlength="20" placeholder="ex. 215 TU 4521" /></div>
          <div class="field"><label for="model">Marque / modèle</label><input id="model" class="input" formControlName="brandModel" maxlength="100" /></div>
          <div class="field"><label for="type">Type</label><input id="type" class="input" formControlName="type" maxlength="40" placeholder="ex. Utilitaire, 4x4…" /></div>
          <div class="field"><label for="status">État</label>
            <select id="status" class="input" formControlName="status">
              @for (s of statuses; track s.value) { <option [value]="s.value">{{ s.label }}</option> }
            </select></div>
          <div class="field"><label for="km">Kilométrage</label><input id="km" type="number" min="0" class="input" formControlName="mileage" /></div>
          <div class="field"><label for="lm">Dernière maintenance</label><input id="lm" type="date" class="input" formControlName="lastMaintenanceDate" /></div>
          <div class="field"><label for="team">Équipe</label>
            <select id="team" class="input" formControlName="teamId">
              <option [ngValue]="null">Aucune</option>
              @for (t of teams(); track t.id) { <option [ngValue]="t.id">{{ t.name }} ({{ t.departmentName }})</option> }
            </select></div>
          <div class="field full"><app-error-alert [error]="formError()" /></div>
        </div>
        <div class="card-body row" style="border-top: 1px solid var(--line)">
          <span class="spacer"></span>
          @if (editing()) { <button class="btn" type="button" (click)="resetForm()">Annuler</button> }
          <button class="btn btn-primary" type="submit" [disabled]="form.invalid">{{ editing() ? 'Enregistrer' : 'Ajouter' }}</button>
        </div>
      </form>

      <section class="card">
        <div class="card-head filters">
          <input class="input" type="search" placeholder="Immatriculation ou modèle" [(ngModel)]="filter.q" (ngModelChange)="reload()" />
          <select class="input" [(ngModel)]="filter.status" (ngModelChange)="reload()">
            <option value="">Tous les états</option>
            @for (s of statuses; track s.value) { <option [value]="s.value">{{ s.label }}</option> }
          </select>
        </div>
        <app-error-alert [error]="error()" />
        @if (page(); as p) {
          @if (p.content.length === 0) {
            <div class="empty"><strong>Aucun véhicule</strong></div>
          } @else {
            <div class="table-wrap">
              <table class="table">
                <thead><tr><th>Véhicule</th><th>Type</th><th>Équipe</th><th>Kilométrage</th><th>Dernière maintenance</th><th>État</th><th></th></tr></thead>
                <tbody>
                  @for (v of p.content; track v.id) {
                    <tr>
                      <td><span class="cell-strong">{{ v.plateNumber }}</span><span class="cell-sub">{{ v.brandModel }}</span></td>
                      <td>{{ v.type }}</td>
                      <td>{{ v.teamName || '—' }}</td>
                      <td>{{ v.mileage }} km</td>
                      <td>{{ v.lastMaintenanceDate ? (v.lastMaintenanceDate | date: 'dd/MM/yyyy') : '—' }}</td>
                      <td><app-badge [value]="v.status" /></td>
                      <td class="actions"><button class="btn btn-sm" (click)="edit(v)">Modifier</button></td>
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
  `],
})
export class VehiclesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly admin = inject(AdminService);
  private readonly employeeService = inject(EmployeeService);

  readonly statuses = options(ASSET_STATUSES);
  readonly teams = signal<Team[]>([]);
  readonly page = signal<Page<Vehicle> | null>(null);
  readonly editing = signal<number | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly formError = signal<ApiError | null>(null);

  filter: AssetFilter = { q: '', status: '', page: 0, size: 20 };
  private readonly trigger = new Subject<void>();

  readonly form = this.fb.group({
    plateNumber: ['', Validators.required],
    brandModel: ['', Validators.required],
    type: ['', Validators.required],
    status: ['DISPONIBLE' as AssetStatus, Validators.required],
    mileage: [0, [Validators.required, Validators.min(0)]],
    lastMaintenanceDate: [''],
    teamId: [null as number | null],
  });

  ngOnInit(): void {
    this.employeeService.teams().subscribe((t) => this.teams.set(t));
    this.trigger.pipe(debounceTime(250), switchMap(() => this.admin.vehicles(this.filter))).subscribe({
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

  edit(v: Vehicle): void {
    this.editing.set(v.id);
    this.formError.set(null);
    this.form.setValue({
      plateNumber: v.plateNumber, brandModel: v.brandModel, type: v.type, status: v.status, mileage: v.mileage,
      lastMaintenanceDate: v.lastMaintenanceDate ?? '', teamId: v.teamId,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  resetForm(): void {
    this.editing.set(null);
    this.form.reset({ plateNumber: '', brandModel: '', type: '', status: 'DISPONIBLE', mileage: 0, lastMaintenanceDate: '', teamId: null });
  }

  save(): void {
    const v = this.form.getRawValue();
    const body = {
      plateNumber: v.plateNumber!.trim(), brandModel: v.brandModel!.trim(), type: v.type!.trim(), status: v.status!,
      mileage: Number(v.mileage ?? 0), lastMaintenanceDate: v.lastMaintenanceDate || null, teamId: v.teamId,
    };
    const id = this.editing();
    this.formError.set(null);
    (id ? this.admin.updateVehicle(id, body) : this.admin.createVehicle(body)).subscribe({
      next: () => { this.resetForm(); this.trigger.next(); },
      error: (e) => this.formError.set(toApiError(e)),
    });
  }
}
