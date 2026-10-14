import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiError, Page } from '../../core/models/api.model';
import { Employee } from '../../core/models/employee.model';
import { Absence, AbsenceType } from '../../core/models/leave.model';
import { toApiError } from '../../core/services/api-error';
import { EmployeeService } from '../../core/services/employee.service';
import { LeaveService } from '../../core/services/leave.service';
import { ErrorAlertComponent } from '../../shared/components/error-alert.component';
import { PagerComponent } from '../../shared/components/pager.component';
import { LabelPipe } from '../../shared/pipes/label.pipe';
import { ABSENCE_TYPES, options } from '../../shared/utils/labels';

@Component({
  selector: 'app-absence-list',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, LabelPipe, PagerComponent, ErrorAlertComponent],
  template: `
    <div class="page">
      <div class="page-head">
        <div><h1>Absences</h1><p>Une absence rend l'employé indisponible pour toute intervention sur la période.</p></div>
      </div>

      <form class="card" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <div class="card-head"><h2>Enregistrer une absence</h2></div>
        <div class="card-body form-grid">
          <div class="field"><label for="emp">Employé</label>
            <select id="emp" class="input" formControlName="employeeId">
              <option [ngValue]="null" disabled>Choisir</option>
              @for (e of employees(); track e.id) { <option [ngValue]="e.id">{{ e.fullName }} ({{ e.matricule }})</option> }
            </select></div>
          <div class="field"><label for="type">Type</label>
            <select id="type" class="input" formControlName="type">
              @for (t of types; track t.value) { <option [value]="t.value">{{ t.label }}</option> }
            </select></div>
          <div class="field"><label for="s">Du</label><input id="s" type="date" class="input" formControlName="startDate" /></div>
          <div class="field"><label for="e">Au (inclus)</label><input id="e" type="date" class="input" formControlName="endDate" /></div>
          <div class="field full"><label for="r">Motif</label><input id="r" class="input" formControlName="reason" maxlength="500" /></div>
          <div class="field"><label class="checkbox"><input type="checkbox" formControlName="justified" /> Justificatif reçu</label></div>
          <div class="field full"><app-error-alert [error]="error()" /></div>
        </div>
        <div class="card-body row" style="border-top: 1px solid var(--line)">
          <span class="spacer"></span>
          <button class="btn btn-primary" type="submit" [disabled]="form.invalid">Enregistrer l'absence</button>
        </div>
      </form>

      <section class="card">
        @if (page(); as p) {
          @if (p.content.length === 0) {
            <div class="empty"><strong>Aucune absence enregistrée</strong></div>
          } @else {
            <div class="table-wrap">
              <table class="table">
                <thead><tr><th>Employé</th><th>Période</th><th>Type</th><th>Motif</th><th>Justifiée</th><th></th></tr></thead>
                <tbody>
                  @for (a of p.content; track a.id) {
                    <tr>
                      <td><span class="cell-strong">{{ a.employeeName }}</span><span class="cell-sub">{{ a.matricule }}</span></td>
                      <td>{{ a.startDate | date: 'dd/MM/yyyy' }} → {{ a.endDate | date: 'dd/MM/yyyy' }}</td>
                      <td>{{ a.type | label }}</td>
                      <td>{{ a.reason || '—' }}</td>
                      <td>{{ a.justified ? 'Oui' : 'Non' }}</td>
                      <td><button class="btn btn-sm btn-danger" (click)="remove(a)">Supprimer</button></td>
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
})
export class AbsenceListComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly leaves = inject(LeaveService);
  private readonly employeeService = inject(EmployeeService);

  readonly types = options(ABSENCE_TYPES);
  readonly employees = signal<Employee[]>([]);
  readonly page = signal<Page<Absence> | null>(null);
  readonly error = signal<ApiError | null>(null);

  readonly form = this.fb.group({
    employeeId: [null as number | null, Validators.required],
    type: ['MALADIE' as AbsenceType, Validators.required],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
    reason: [''],
    justified: [false],
  });

  ngOnInit(): void {
    this.employeeService.search({ status: 'ACTIF', size: 100 }).subscribe((p) => this.employees.set(p.content));
    this.load(0);
  }

  load(page: number): void {
    this.leaves.absences(null, page).subscribe({ next: (p) => this.page.set(p), error: (e) => this.error.set(toApiError(e)) });
  }

  save(): void {
    const v = this.form.getRawValue();
    this.error.set(null);
    this.leaves.createAbsence({
      employeeId: v.employeeId!, type: v.type!, startDate: v.startDate!, endDate: v.endDate!,
      reason: v.reason || null, justified: !!v.justified,
    }).subscribe({
      next: () => { this.form.reset({ employeeId: null, type: 'MALADIE', startDate: '', endDate: '', reason: '', justified: false }); this.load(0); },
      error: (e) => this.error.set(toApiError(e)),
    });
  }

  remove(a: Absence): void {
    if (!confirm(`Supprimer l'absence de ${a.employeeName} ?`)) return;
    this.leaves.deleteAbsence(a.id).subscribe({ next: () => this.load(0), error: (e) => this.error.set(toApiError(e)) });
  }
}
