import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth/auth.service';
import { UserAccount } from '../../core/models/admin.model';
import { ApiError, Page } from '../../core/models/api.model';
import { Role } from '../../core/models/auth.model';
import { Employee } from '../../core/models/employee.model';
import { AdminService } from '../../core/services/admin.service';
import { toApiError } from '../../core/services/api-error';
import { EmployeeService } from '../../core/services/employee.service';
import { ErrorAlertComponent } from '../../shared/components/error-alert.component';
import { PagerComponent } from '../../shared/components/pager.component';
import { LabelPipe } from '../../shared/pipes/label.pipe';
import { ROLES, options } from '../../shared/utils/labels';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, LabelPipe, PagerComponent, ErrorAlertComponent],
  template: `
    <div class="page">
      <div class="page-head">
        <div><h1>Comptes utilisateurs</h1><p>Accès à l'application. Hors ADMIN, chaque compte est lié à une fiche employé.</p></div>
      </div>

      <form class="card" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <div class="card-head"><h2>Créer un compte</h2></div>
        <div class="card-body form-grid">
          <div class="field"><label for="email">Email</label><input id="email" type="email" class="input" formControlName="email" /></div>
          <div class="field"><label for="pwd">Mot de passe (10 caractères min.)</label><input id="pwd" type="password" class="input" formControlName="password" autocomplete="new-password" /></div>
          <div class="field"><label for="role">Rôle</label>
            <select id="role" class="input" formControlName="role">
              @for (r of roles; track r.value) { <option [value]="r.value">{{ r.label }}</option> }
            </select></div>
          <div class="field"><label for="emp">Fiche employé</label>
            <select id="emp" class="input" formControlName="employeeId">
              <option [ngValue]="null">Aucune (ADMIN uniquement)</option>
              @for (e of employees(); track e.id) { <option [ngValue]="e.id">{{ e.fullName }} ({{ e.matricule }})</option> }
            </select></div>
          <div class="field full"><app-error-alert [error]="formError()" />
            @if (info()) { <div class="alert alert-ok">{{ info() }}</div> }</div>
        </div>
        <div class="card-body row" style="border-top: 1px solid var(--line)">
          <span class="spacer"></span>
          <button class="btn btn-primary" type="submit" [disabled]="form.invalid || busy()">Créer le compte</button>
        </div>
      </form>

      <section class="card">
        <app-error-alert [error]="error()" />
        @if (page(); as p) {
          <div class="table-wrap">
            <table class="table">
              <thead><tr><th>Compte</th><th>Employé</th><th>Rôle</th><th>Dernière connexion</th><th>État</th><th></th></tr></thead>
              <tbody>
                @for (u of p.content; track u.id) {
                  <tr>
                    <td><span class="cell-strong">{{ u.email }}</span><span class="cell-sub">créé le {{ u.createdAt | date: 'dd/MM/yyyy' }}</span></td>
                    <td>{{ u.employeeName || '—' }}</td>
                    <td>
                      <select class="input input-sm" [value]="u.role" (change)="changeRole(u, $any($event.target).value)" [disabled]="u.id === meId">
                        @for (r of roles; track r.value) { <option [value]="r.value" [selected]="r.value === u.role">{{ r.label }}</option> }
                      </select>
                    </td>
                    <td>{{ u.lastLoginAt ? (u.lastLoginAt | date: 'dd/MM/yyyy HH:mm') : 'Jamais' }}</td>
                    <td><span [class]="'badge ' + (u.enabled ? 'ok' : '')">{{ u.enabled ? 'Actif' : 'Désactivé' }}</span></td>
                    <td class="actions">
                      @if (u.id !== meId) {
                        <button class="btn btn-sm" [class.btn-danger]="u.enabled" (click)="toggle(u)">{{ u.enabled ? 'Désactiver' : 'Activer' }}</button>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <app-pager [page]="p.page" [totalPages]="p.totalPages" [totalElements]="p.totalElements" (pageChange)="load($event)" />
        } @else { <div class="loading">Chargement…</div> }
      </section>
    </div>
  `,
  styles: [`
    .actions { white-space: nowrap; text-align: right; }
    .input-sm { padding: 4px 8px; width: auto; }
  `],
})
export class UsersComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly admin = inject(AdminService);
  private readonly employeeService = inject(EmployeeService);
  readonly meId = inject(AuthService).user()?.id;

  readonly roles = options(ROLES);
  readonly employees = signal<Employee[]>([]);
  readonly page = signal<Page<UserAccount> | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly formError = signal<ApiError | null>(null);
  readonly info = signal<string | null>(null);
  readonly busy = signal(false);
  private current = 0;

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(10)]],
    role: ['EMPLOYE' as Role, Validators.required],
    employeeId: [null as number | null],
  });

  ngOnInit(): void {
    this.employeeService.search({ status: 'ACTIF', size: 100 }).subscribe((p) => this.employees.set(p.content));
    this.load(0);
  }

  load(page: number): void {
    this.current = page;
    this.admin.users(page).subscribe({ next: (p) => { this.page.set(p); this.error.set(null); }, error: (e) => this.error.set(toApiError(e)) });
  }

  save(): void {
    const v = this.form.getRawValue();
    this.busy.set(true);
    this.formError.set(null);
    this.info.set(null);
    this.admin.createUser({ email: v.email!.trim(), password: v.password!, role: v.role!, employeeId: v.employeeId }).subscribe({
      next: (u) => {
        this.busy.set(false);
        this.info.set(`Compte ${u.email} créé.`);
        this.form.reset({ email: '', password: '', role: 'EMPLOYE', employeeId: null });
        this.load(this.current);
      },
      error: (e) => { this.busy.set(false); this.formError.set(toApiError(e)); },
    });
  }

  changeRole(u: UserAccount, role: Role): void {
    this.update(u, role, u.enabled);
  }

  toggle(u: UserAccount): void {
    const verb = u.enabled ? 'désactiver' : 'activer';
    if (!confirm(`Voulez-vous ${verb} le compte ${u.email} ?`)) return;
    this.update(u, u.role, !u.enabled);
  }

  private update(u: UserAccount, role: Role, enabled: boolean): void {
    this.admin.updateUser(u.id, { role, enabled }).subscribe({
      next: () => this.load(this.current),
      error: (e) => { this.error.set(toApiError(e)); this.load(this.current); },
    });
  }
}
