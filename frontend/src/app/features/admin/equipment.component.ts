import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, debounceTime, switchMap } from 'rxjs';
import { AssetStatus, Equipment } from '../../core/models/admin.model';
import { ApiError, Page } from '../../core/models/api.model';
import { Employee } from '../../core/models/employee.model';
import { AdminService, AssetFilter } from '../../core/services/admin.service';
import { toApiError } from '../../core/services/api-error';
import { EmployeeService } from '../../core/services/employee.service';
import { ErrorAlertComponent } from '../../shared/components/error-alert.component';
import { PagerComponent } from '../../shared/components/pager.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { ASSET_STATUSES, options } from '../../shared/utils/labels';

@Component({
  selector: 'app-equipment',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, DatePipe, StatusBadgeComponent, PagerComponent, ErrorAlertComponent],
  template: `
    <div class="page">
      <div class="page-head">
        <div><h1>Équipements</h1><p>Matériel mobilisable lors des interventions et suivi de sa maintenance.</p></div>
      </div>

      <form class="card" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <div class="card-head"><h2>{{ editing() ? "Modifier l'équipement" : 'Nouvel équipement' }}</h2></div>
        <div class="card-body form-grid">
          <div class="field"><label for="ref">Référence</label><input id="ref" class="input" formControlName="reference" maxlength="40" /></div>
          <div class="field"><label for="name">Désignation</label><input id="name" class="input" formControlName="name" maxlength="150" /></div>
          <div class="field"><label for="type">Type</label><input id="type" class="input" formControlName="type" maxlength="60" placeholder="ex. Soudeuse fibre, OTDR…" /></div>
          <div class="field"><label for="status">État</label>
            <select id="status" class="input" formControlName="status">
              @for (s of statuses; track s.value) { <option [value]="s.value">{{ s.label }}</option> }
            </select></div>
          <div class="field"><label for="loc">Emplacement</label><input id="loc" class="input" formControlName="location" maxlength="150" /></div>
          <div class="field"><label for="resp">Responsable</label>
            <select id="resp" class="input" formControlName="responsibleId">
              <option [ngValue]="null">Aucun</option>
              @for (e of employees(); track e.id) { <option [ngValue]="e.id">{{ e.fullName }} ({{ e.matricule }})</option> }
            </select></div>
          <div class="field"><label for="lm">Dernière maintenance</label><input id="lm" type="date" class="input" formControlName="lastMaintenanceDate" /></div>
          <div class="field"><label for="nm">Prochaine maintenance</label><input id="nm" type="date" class="input" formControlName="nextMaintenanceDate" /></div>
          <div class="field full"><label for="notes">Notes</label><textarea id="notes" class="input" formControlName="notes" maxlength="1000"></textarea></div>
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
          <input class="input" type="search" placeholder="Référence ou désignation" [(ngModel)]="filter.q" (ngModelChange)="reload()" />
          <select class="input" [(ngModel)]="filter.status" (ngModelChange)="reload()">
            <option value="">Tous les états</option>
            @for (s of statuses; track s.value) { <option [value]="s.value">{{ s.label }}</option> }
          </select>
        </div>
        <app-error-alert [error]="error()" />
        @if (page(); as p) {
          @if (p.content.length === 0) {
            <div class="empty"><strong>Aucun équipement</strong></div>
          } @else {
            <div class="table-wrap">
              <table class="table">
                <thead><tr><th>Équipement</th><th>Type</th><th>Emplacement</th><th>Responsable</th><th>Prochaine maintenance</th><th>État</th><th></th></tr></thead>
                <tbody>
                  @for (e of p.content; track e.id) {
                    <tr>
                      <td><span class="cell-strong">{{ e.name }}</span><span class="cell-sub">{{ e.reference }}</span></td>
                      <td>{{ e.type }}</td>
                      <td>{{ e.location || '—' }}</td>
                      <td>{{ e.responsibleName || '—' }}</td>
                      <td>{{ e.nextMaintenanceDate ? (e.nextMaintenanceDate | date: 'dd/MM/yyyy') : '—' }}</td>
                      <td><app-badge [value]="e.status" /></td>
                      <td class="actions"><button class="btn btn-sm" (click)="edit(e)">Modifier</button></td>
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
export class EquipmentComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly admin = inject(AdminService);
  private readonly employeeService = inject(EmployeeService);

  readonly statuses = options(ASSET_STATUSES);
  readonly employees = signal<Employee[]>([]);
  readonly page = signal<Page<Equipment> | null>(null);
  readonly editing = signal<number | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly formError = signal<ApiError | null>(null);

  filter: AssetFilter = { q: '', status: '', page: 0, size: 20 };
  private readonly trigger = new Subject<void>();

  readonly form = this.fb.group({
    reference: ['', Validators.required],
    name: ['', Validators.required],
    type: ['', Validators.required],
    status: ['DISPONIBLE' as AssetStatus, Validators.required],
    location: [''],
    responsibleId: [null as number | null],
    lastMaintenanceDate: [''],
    nextMaintenanceDate: [''],
    notes: [''],
  });

  ngOnInit(): void {
    this.employeeService.search({ status: 'ACTIF', size: 100 }).subscribe((p) => this.employees.set(p.content));
    this.trigger.pipe(debounceTime(250), switchMap(() => this.admin.equipment(this.filter))).subscribe({
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

  edit(e: Equipment): void {
    this.editing.set(e.id);
    this.formError.set(null);
    this.form.setValue({
      reference: e.reference, name: e.name, type: e.type, status: e.status, location: e.location ?? '',
      responsibleId: e.responsibleId, lastMaintenanceDate: e.lastMaintenanceDate ?? '',
      nextMaintenanceDate: e.nextMaintenanceDate ?? '', notes: e.notes ?? '',
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  resetForm(): void {
    this.editing.set(null);
    this.form.reset({ reference: '', name: '', type: '', status: 'DISPONIBLE', location: '', responsibleId: null,
      lastMaintenanceDate: '', nextMaintenanceDate: '', notes: '' });
  }

  save(): void {
    const v = this.form.getRawValue();
    const body = {
      reference: v.reference!.trim(), name: v.name!.trim(), type: v.type!.trim(), status: v.status!,
      location: v.location || null, responsibleId: v.responsibleId,
      lastMaintenanceDate: v.lastMaintenanceDate || null, nextMaintenanceDate: v.nextMaintenanceDate || null,
      notes: v.notes || null,
    };
    const id = this.editing();
    this.formError.set(null);
    (id ? this.admin.updateEquipment(id, body) : this.admin.createEquipment(body)).subscribe({
      next: () => { this.resetForm(); this.trigger.next(); },
      error: (e) => this.formError.set(toApiError(e)),
    });
  }
}
