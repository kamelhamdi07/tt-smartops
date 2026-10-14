import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiError, Page } from '../../core/models/api.model';
import { LeaveRequest, LeaveType } from '../../core/models/leave.model';
import { toApiError } from '../../core/services/api-error';
import { LeaveService } from '../../core/services/leave.service';
import { ErrorAlertComponent } from '../../shared/components/error-alert.component';
import { PagerComponent } from '../../shared/components/pager.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { LabelPipe } from '../../shared/pipes/label.pipe';
import { LEAVE_TYPES, options } from '../../shared/utils/labels';

@Component({
  selector: 'app-my-leaves',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, LabelPipe, StatusBadgeComponent, PagerComponent, ErrorAlertComponent],
  template: `
    <div class="page">
      <div class="page-head">
        <div><h1>Mes congés</h1><p>Déposez une demande ; le service RH vous notifiera de sa décision.</p></div>
      </div>

      <form class="card" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <div class="card-head"><h2>Nouvelle demande</h2></div>
        <div class="card-body form-grid">
          <div class="field"><label for="type">Type de congé</label>
            <select id="type" class="input" formControlName="type">
              @for (t of types; track t.value) { <option [value]="t.value">{{ t.label }}</option> }
            </select></div>
          <div class="field"><label for="start">Du</label><input id="start" type="date" class="input" formControlName="startDate" /></div>
          <div class="field"><label for="end">Au (inclus)</label><input id="end" type="date" class="input" formControlName="endDate" /></div>
          <div class="field full"><label for="comment">Commentaire (facultatif)</label>
            <textarea id="comment" class="input" formControlName="comment" maxlength="500"></textarea></div>
          <div class="field full">
            <app-error-alert [error]="error()" />
            @if (success()) { <div class="alert alert-ok">{{ success() }}</div> }
          </div>
        </div>
        <div class="card-body row" style="border-top: 1px solid var(--line)">
          <span class="spacer"></span>
          <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving()">Envoyer la demande</button>
        </div>
      </form>

      <section class="card">
        <div class="card-head"><h2>Historique</h2></div>
        @if (page(); as p) {
          @if (p.content.length === 0) {
            <div class="empty"><strong>Aucune demande</strong>Vos demandes de congé apparaîtront ici.</div>
          } @else {
            <div class="table-wrap">
              <table class="table">
                <thead><tr><th>Période</th><th>Type</th><th>Jours ouvrés</th><th>Statut</th><th>Décision</th><th></th></tr></thead>
                <tbody>
                  @for (l of p.content; track l.id) {
                    <tr>
                      <td>{{ l.startDate | date: 'dd/MM/yyyy' }} → {{ l.endDate | date: 'dd/MM/yyyy' }}</td>
                      <td>{{ l.type | label }}</td>
                      <td>{{ l.workingDays }}</td>
                      <td><app-badge [value]="l.status" /></td>
                      <td>{{ l.decisionComment || '—' }}</td>
                      <td>@if (l.status === 'EN_ATTENTE') { <button class="btn btn-sm btn-danger" (click)="cancel(l)">Annuler</button> }</td>
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
export class MyLeavesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(LeaveService);

  readonly types = options(LEAVE_TYPES);
  readonly page = signal<Page<LeaveRequest> | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly success = signal<string | null>(null);
  readonly saving = signal(false);

  readonly form = this.fb.nonNullable.group({
    type: ['ANNUEL' as LeaveType, Validators.required],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
    comment: [''],
  });

  ngOnInit(): void { this.load(0); }

  load(page: number): void {
    this.service.mine(page).subscribe({ next: (p) => this.page.set(p), error: (e) => this.error.set(toApiError(e)) });
  }

  submit(): void {
    const v = this.form.getRawValue();
    if (v.endDate < v.startDate) {
      this.error.set({ status: 400, message: 'La date de fin doit être postérieure ou égale à la date de début.' });
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    this.service.create({ ...v, comment: v.comment || null }).subscribe({
      next: (l) => {
        this.success.set(`Demande envoyée : ${l.workingDays} jour(s) ouvré(s).`);
        this.form.reset({ type: 'ANNUEL', startDate: '', endDate: '', comment: '' });
        this.saving.set(false);
        this.load(0);
      },
      error: (e) => { this.error.set(toApiError(e)); this.saving.set(false); },
    });
  }

  cancel(l: LeaveRequest): void {
    if (!confirm('Annuler cette demande de congé ?')) return;
    this.service.cancel(l.id).subscribe({ next: () => this.load(this.page()?.page ?? 0), error: (e) => this.error.set(toApiError(e)) });
  }
}
