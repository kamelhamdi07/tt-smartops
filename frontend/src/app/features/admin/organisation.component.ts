import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiError } from '../../core/models/api.model';
import { Department, Employee, Team } from '../../core/models/employee.model';
import { AdminService } from '../../core/services/admin.service';
import { toApiError } from '../../core/services/api-error';
import { EmployeeService } from '../../core/services/employee.service';
import { ErrorAlertComponent } from '../../shared/components/error-alert.component';
import { LabelPipe } from '../../shared/pipes/label.pipe';
import { RECLAMATION_CATEGORIES, options } from '../../shared/utils/labels';

@Component({
  selector: 'app-organisation',
  standalone: true,
  imports: [ReactiveFormsModule, ErrorAlertComponent, LabelPipe],
  template: `
    <div class="page">
      <div class="page-head">
        <div><h1>Départements et équipes</h1><p>Structure de l'organisation utilisée pour rattacher employés et véhicules.</p></div>
      </div>

      <div class="grid-2">
        <form class="card" [formGroup]="depForm" (ngSubmit)="saveDepartment()" novalidate>
          <div class="card-head"><h2>{{ editingDep() ? 'Modifier le département' : 'Nouveau département' }}</h2></div>
          <div class="card-body form-grid">
            <div class="field"><label for="dcode">Code</label><input id="dcode" class="input" formControlName="code" maxlength="20" /></div>
            <div class="field"><label for="dname">Nom</label><input id="dname" class="input" formControlName="name" maxlength="120" /></div>
            <div class="field full"><label for="ddesc">Description</label><textarea id="ddesc" class="input" formControlName="description" maxlength="500"></textarea></div>
            <div class="field full"><app-error-alert [error]="depError()" /></div>
          </div>
          <div class="card-body row" style="border-top: 1px solid var(--line)">
            <span class="spacer"></span>
            @if (editingDep()) { <button class="btn" type="button" (click)="resetDepartment()">Annuler</button> }
            <button class="btn btn-primary" type="submit" [disabled]="depForm.invalid">{{ editingDep() ? 'Enregistrer' : 'Ajouter' }}</button>
          </div>
        </form>

        <form class="card" [formGroup]="teamForm" (ngSubmit)="saveTeam()" novalidate>
          <div class="card-head"><h2>{{ editingTeam() ? "Modifier l'équipe" : 'Nouvelle équipe' }}</h2></div>
          <div class="card-body form-grid">
            <div class="field"><label for="tname">Nom</label><input id="tname" class="input" formControlName="name" maxlength="120" /></div>
            <div class="field"><label for="tdep">Département</label>
              <select id="tdep" class="input" formControlName="departmentId">
                <option [ngValue]="null" disabled>Choisir</option>
                @for (d of departments(); track d.id) { <option [ngValue]="d.id">{{ d.name }}</option> }
              </select></div>
            <div class="field full"><label for="tlead">Chef d'équipe</label>
              <select id="tlead" class="input" formControlName="leaderId">
                <option [ngValue]="null">Aucun</option>
                @for (e of employees(); track e.id) { <option [ngValue]="e.id">{{ e.fullName }} ({{ e.matricule }})</option> }
              </select></div>
            <div class="field full"><label for="tdesc">Description</label><textarea id="tdesc" class="input" formControlName="description" maxlength="500"></textarea></div>
            <div class="field full"><label>Réclamations clients reçues par cette équipe</label>
              <div class="cats">
                @for (c of categories; track c.value) {
                  <label class="checkbox"><input type="checkbox" [checked]="hasCategory(c.value)" (change)="toggleCategory(c.value)" /> {{ c.label }}</label>
                }
              </div>
              <span class="muted small">Une catégorie ne peut être prise en charge que par une seule équipe. Le chef d'équipe reçoit ces réclamations.</span>
            </div>
            <div class="field full"><app-error-alert [error]="teamError()" /></div>
          </div>
          <div class="card-body row" style="border-top: 1px solid var(--line)">
            <span class="spacer"></span>
            @if (editingTeam()) { <button class="btn" type="button" (click)="resetTeam()">Annuler</button> }
            <button class="btn btn-primary" type="submit" [disabled]="teamForm.invalid">{{ editingTeam() ? 'Enregistrer' : 'Ajouter' }}</button>
          </div>
        </form>
      </div>

      <section class="card">
        <div class="card-head"><h2>Départements</h2></div>
        @if (departments().length === 0) {
          <div class="empty"><strong>Aucun département</strong></div>
        } @else {
          <div class="table-wrap">
            <table class="table">
              <thead><tr><th>Département</th><th>Description</th><th>Équipes</th><th>Employés</th><th></th></tr></thead>
              <tbody>
                @for (d of departments(); track d.id) {
                  <tr>
                    <td><span class="cell-strong">{{ d.name }}</span><span class="cell-sub">{{ d.code }}</span></td>
                    <td>{{ d.description || '—' }}</td>
                    <td>{{ d.teamCount }}</td>
                    <td>{{ d.employeeCount }}</td>
                    <td class="actions"><button class="btn btn-sm" (click)="editDepartment(d)">Modifier</button></td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>

      <section class="card">
        <div class="card-head"><h2>Équipes</h2></div>
        @if (teams().length === 0) {
          <div class="empty"><strong>Aucune équipe</strong></div>
        } @else {
          <div class="table-wrap">
            <table class="table">
              <thead><tr><th>Équipe</th><th>Département</th><th>Chef d'équipe</th><th>Membres</th><th>Réclamations</th><th></th></tr></thead>
              <tbody>
                @for (t of teams(); track t.id) {
                  <tr>
                    <td><span class="cell-strong">{{ t.name }}</span><span class="cell-sub">{{ t.description || '' }}</span></td>
                    <td>{{ t.departmentName }}</td>
                    <td>{{ t.leaderName || '—' }}</td>
                    <td>{{ t.memberCount }}</td>
                    <td>@for (c of t.categories; track c) { <span class="badge info">{{ c | label }}</span> } @empty { — }</td>
                    <td class="actions"><button class="btn btn-sm" (click)="editTeam(t)">Modifier</button></td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>
    </div>
  `,
  styles: [`
    .actions { white-space: nowrap; text-align: right; }
    .cats { display: flex; flex-wrap: wrap; gap: 6px 18px; }
    .small { font-size: 12px; }
    td .badge + .badge { margin-left: 4px; }
  `],
})
export class OrganisationComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly admin = inject(AdminService);
  private readonly employeeService = inject(EmployeeService);

  readonly categories = options(RECLAMATION_CATEGORIES);
  readonly departments = signal<Department[]>([]);
  readonly teams = signal<Team[]>([]);
  readonly employees = signal<Employee[]>([]);
  readonly editingDep = signal<number | null>(null);
  readonly editingTeam = signal<number | null>(null);
  readonly depError = signal<ApiError | null>(null);
  readonly teamError = signal<ApiError | null>(null);

  readonly depForm = this.fb.group({
    code: ['', Validators.required],
    name: ['', Validators.required],
    description: [''],
  });

  readonly teamForm = this.fb.group({
    name: ['', Validators.required],
    departmentId: [null as number | null, Validators.required],
    leaderId: [null as number | null],
    description: [''],
    categories: [[] as string[]],
  });

  ngOnInit(): void {
    this.employeeService.search({ status: 'ACTIF', size: 100 }).subscribe((p) => this.employees.set(p.content));
    this.reload();
  }

  reload(): void {
    this.employeeService.departments().subscribe((d) => this.departments.set(d));
    this.employeeService.teams().subscribe((t) => this.teams.set(t));
  }

  editDepartment(d: Department): void {
    this.editingDep.set(d.id);
    this.depError.set(null);
    this.depForm.setValue({ code: d.code, name: d.name, description: d.description ?? '' });
  }

  resetDepartment(): void {
    this.editingDep.set(null);
    this.depForm.reset({ code: '', name: '', description: '' });
  }

  saveDepartment(): void {
    const v = this.depForm.getRawValue();
    const body = { code: v.code!.trim(), name: v.name!.trim(), description: v.description || null };
    const id = this.editingDep();
    this.depError.set(null);
    (id ? this.admin.updateDepartment(id, body) : this.admin.createDepartment(body)).subscribe({
      next: () => { this.resetDepartment(); this.reload(); },
      error: (e) => this.depError.set(toApiError(e)),
    });
  }

  editTeam(t: Team): void {
    this.editingTeam.set(t.id);
    this.teamError.set(null);
    this.teamForm.setValue({ name: t.name, departmentId: t.departmentId, leaderId: t.leaderId, description: t.description ?? '',
      categories: [...(t.categories ?? [])] });
  }

  resetTeam(): void {
    this.editingTeam.set(null);
    this.teamForm.reset({ name: '', departmentId: null, leaderId: null, description: '', categories: [] });
  }

  hasCategory(c: string): boolean {
    return (this.teamForm.controls.categories.value ?? []).includes(c);
  }

  toggleCategory(c: string): void {
    const current = this.teamForm.controls.categories.value ?? [];
    this.teamForm.controls.categories.setValue(current.includes(c) ? current.filter((x) => x !== c) : [...current, c]);
  }

  saveTeam(): void {
    const v = this.teamForm.getRawValue();
    const body = { name: v.name!.trim(), departmentId: v.departmentId!, leaderId: v.leaderId, description: v.description || null,
      categories: v.categories ?? [] };
    const id = this.editingTeam();
    this.teamError.set(null);
    (id ? this.admin.updateTeam(id, body) : this.admin.createTeam(body)).subscribe({
      next: () => { this.resetTeam(); this.reload(); },
      error: (e) => this.teamError.set(toApiError(e)),
    });
  }
}
