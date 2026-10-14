import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiError } from '../../core/models/api.model';
import { Department, EmployeeRequest, Specialty, Team } from '../../core/models/employee.model';
import { toApiError } from '../../core/services/api-error';
import { EmployeeService } from '../../core/services/employee.service';
import { ErrorAlertComponent } from '../../shared/components/error-alert.component';
import { SPECIALTIES, options } from '../../shared/utils/labels';

@Component({
  selector: 'app-employee-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, ErrorAlertComponent],
  template: `
    <div class="page">
      <div class="page-head">
        <h1>{{ id() ? 'Modifier la fiche' : 'Nouvel employé' }}</h1>
        <a class="btn" routerLink="/employees">Retour à la liste</a>
      </div>

      <form class="card" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <div class="card-body form-grid">
          <div class="field"><label for="matricule">Matricule</label><input id="matricule" class="input" formControlName="matricule" />
            @if (fieldError('matricule')) { <span class="field-error">{{ fieldError('matricule') }}</span> }</div>
          <div class="field"><label for="firstName">Prénom</label><input id="firstName" class="input" formControlName="firstName" /></div>
          <div class="field"><label for="lastName">Nom</label><input id="lastName" class="input" formControlName="lastName" /></div>
          <div class="field"><label for="email">Email professionnel</label><input id="email" type="email" class="input" formControlName="email" />
            @if (fieldError('email')) { <span class="field-error">{{ fieldError('email') }}</span> }</div>
          <div class="field"><label for="phone">Téléphone</label><input id="phone" class="input" formControlName="phone" /></div>
          <div class="field"><label for="jobTitle">Fonction</label><input id="jobTitle" class="input" formControlName="jobTitle" /></div>
          <div class="field"><label for="hireDate">Date d'embauche</label><input id="hireDate" type="date" class="input" formControlName="hireDate" /></div>

          <div class="field"><label for="dept">Département</label>
            <select id="dept" class="input" formControlName="departmentId" (change)="onDepartmentChange()">
              <option [ngValue]="null">Aucun</option>
              @for (d of departments(); track d.id) { <option [ngValue]="d.id">{{ d.name }}</option> }
            </select></div>
          <div class="field"><label for="team">Équipe</label>
            <select id="team" class="input" formControlName="teamId">
              <option [ngValue]="null">Aucune</option>
              @for (t of teams(); track t.id) { <option [ngValue]="t.id">{{ t.name }}</option> }
            </select></div>

          <div class="field full">
            <label class="checkbox"><input type="checkbox" formControlName="technician" /> Cet employé est technicien</label>
          </div>
          @if (form.controls.technician.value) {
            <div class="field"><label for="specialty">Spécialité</label>
              <select id="specialty" class="input" formControlName="specialty">
                <option [ngValue]="null" disabled>Choisir</option>
                @for (s of specialties; track s.value) { <option [ngValue]="s.value">{{ s.label }}</option> }
              </select></div>
          }
        </div>
        <div class="card-body footer">
          <app-error-alert [error]="error()" />
          <div class="row">
            <span class="spacer"></span>
            <a class="btn" routerLink="/employees">Annuler</a>
            <button class="btn btn-primary" type="submit" [disabled]="saving()">{{ id() ? 'Enregistrer' : "Créer l'employé" }}</button>
          </div>
        </div>
      </form>
    </div>
  `,
  styles: [`.footer { border-top: 1px solid var(--line); display: flex; flex-direction: column; gap: 12px; }`],
})
export class EmployeeFormComponent implements OnInit {
  /** Lié au paramètre de route :id (withComponentInputBinding) */
  readonly id = input<string>();

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(EmployeeService);
  private readonly router = inject(Router);

  readonly specialties = options(SPECIALTIES);
  readonly departments = signal<Department[]>([]);
  readonly teams = signal<Team[]>([]);
  readonly saving = signal(false);
  readonly error = signal<ApiError | null>(null);

  readonly form = this.fb.group({
    matricule: ['', [Validators.required, Validators.maxLength(20)]],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    jobTitle: ['', Validators.required],
    hireDate: ['', Validators.required],
    departmentId: [null as number | null],
    teamId: [null as number | null],
    technician: [false],
    specialty: [null as Specialty | null],
  });

  ngOnInit(): void {
    this.service.departments().subscribe((d) => this.departments.set(d));
    const id = this.id();
    if (id) {
      this.service.get(+id).subscribe((e) => {
        this.form.patchValue({ ...e, phone: e.phone ?? '' });
        this.loadTeams(e.departmentId);
      });
    }
  }

  onDepartmentChange(): void {
    this.form.controls.teamId.setValue(null);
    this.loadTeams(this.form.controls.departmentId.value);
  }

  fieldError(name: string): string | null {
    return this.error()?.fieldErrors?.[name] ?? null;
  }

  save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const body: EmployeeRequest = {
      matricule: v.matricule!, firstName: v.firstName!, lastName: v.lastName!, email: v.email!,
      phone: v.phone || null, jobTitle: v.jobTitle!, hireDate: v.hireDate!,
      technician: !!v.technician, specialty: v.technician ? v.specialty : null,
      departmentId: v.departmentId, teamId: v.teamId,
    };
    const id = this.id();
    this.saving.set(true);
    (id ? this.service.update(+id, body) : this.service.create(body)).subscribe({
      next: () => this.router.navigate(['/employees']),
      error: (e) => { this.error.set(toApiError(e)); this.saving.set(false); },
    });
  }

  private loadTeams(departmentId: number | null): void {
    if (!departmentId) { this.teams.set([]); return; }
    this.service.teams(departmentId).subscribe((t) => this.teams.set(t));
  }
}
