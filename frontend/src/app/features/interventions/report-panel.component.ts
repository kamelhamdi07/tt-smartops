import { DatePipe } from '@angular/common';
import { Component, OnChanges, computed, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../../core/models/api.model';
import { Intervention, InterventionReport, ReportResult } from '../../core/models/intervention.model';
import { toApiError } from '../../core/services/api-error';
import { InterventionService } from '../../core/services/intervention.service';
import { ErrorAlertComponent } from '../../shared/components/error-alert.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { LabelPipe } from '../../shared/pipes/label.pipe';
import { fromInputDateTime, toInputDateTime } from '../../shared/utils/dates';

@Component({
  selector: 'app-report-panel',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, LabelPipe, StatusBadgeComponent, ErrorAlertComponent],
  template: `
    <section class="card">
      <div class="card-head">
        <h2>Rapport d'intervention</h2>
        @if (report()) { <app-badge [value]="report()!.status" /> }
      </div>

      @if (report()?.status === 'A_CORRIGER') {
        <div class="card-body"><div class="alert alert-error">Correction demandée : {{ report()!.reviewComment }}</div></div>
      }

      @if (canEdit()) {
        <form class="card-body form-grid" [formGroup]="form" (ngSubmit)="save()">
          <div class="field"><label for="ws">Début des travaux</label><input id="ws" type="datetime-local" class="input" formControlName="workStart" /></div>
          <div class="field"><label for="we">Fin des travaux</label><input id="we" type="datetime-local" class="input" formControlName="workEnd" /></div>
          <div class="field"><label for="res">Résultat</label>
            <select id="res" class="input" formControlName="result">
              <option [ngValue]="null">À préciser</option>
              @for (r of results; track r) { <option [ngValue]="r">{{ r | label }}</option> }
            </select></div>
          <div class="field full"><label for="pb">Problème constaté</label><textarea id="pb" class="input" formControlName="problemFound"></textarea></div>
          <div class="field full"><label for="dg">Diagnostic</label><textarea id="dg" class="input" formControlName="diagnosis"></textarea></div>
          <div class="field full"><label for="sol">Solution appliquée</label><textarea id="sol" class="input" formControlName="solutionApplied"></textarea></div>
          <div class="field"><label for="mat">Matériel utilisé</label><textarea id="mat" class="input" formControlName="materialsUsed"></textarea></div>
          <div class="field"><label for="rep">Pièces remplacées</label><textarea id="rep" class="input" formControlName="replacedParts"></textarea></div>
          <div class="field full"><label for="obs">Observations</label><textarea id="obs" class="input" formControlName="observations"></textarea></div>
          <div class="field full"><app-error-alert [error]="error()" />
            @if (info()) { <div class="alert alert-ok">{{ info() }}</div> }</div>
          <div class="field full row">
            <span class="spacer"></span>
            <button class="btn" type="submit" [disabled]="busy()">Enregistrer le brouillon</button>
            <button class="btn btn-primary" type="button" (click)="submit()"
                    [disabled]="busy() || intervention().status !== 'TERMINEE'"
                    [attr.title]="intervention().status !== 'TERMINEE' ? 'Terminez l’intervention pour soumettre' : null">
              Soumettre pour validation
            </button>
          </div>
          @if (intervention().status !== 'TERMINEE') {
            <p class="field full muted">
              Pour soumettre le rapport, cliquez d’abord sur « Marquer comme terminée » en haut de la page.
            </p>
          }
        </form>
      } @else {
       @if (report(); as r) {
        <div class="card-body">
          <dl class="details">
            <dt>Rédigé par</dt><dd>{{ r.authorName }}</dd>
            <dt>Travaux</dt><dd>{{ r.workStart | date: 'dd/MM HH:mm' }} → {{ r.workEnd | date: 'dd/MM HH:mm' }}</dd>
            <dt>Résultat</dt><dd>{{ r.result | label }}</dd>
            <dt>Problème</dt><dd class="pre">{{ r.problemFound || '—' }}</dd>
            <dt>Diagnostic</dt><dd class="pre">{{ r.diagnosis || '—' }}</dd>
            <dt>Solution</dt><dd class="pre">{{ r.solutionApplied || '—' }}</dd>
            <dt>Matériel</dt><dd class="pre">{{ r.materialsUsed || '—' }}</dd>
            <dt>Pièces remplacées</dt><dd class="pre">{{ r.replacedParts || '—' }}</dd>
            <dt>Observations</dt><dd class="pre">{{ r.observations || '—' }}</dd>
            @if (r.reviewedBy) { <dt>Examiné par</dt><dd>{{ r.reviewedBy }}</dd> }
          </dl>
          @if (canReview()) {
            <div class="review">
              <app-error-alert [error]="error()" />
              <textarea class="input" placeholder="Commentaire (obligatoire pour demander une correction)" #comment></textarea>
              <div class="row">
                <span class="spacer"></span>
                <button class="btn btn-danger" (click)="review(false, comment.value)" [disabled]="busy()">Demander une correction</button>
                <button class="btn btn-primary" (click)="review(true, comment.value)" [disabled]="busy()">Valider et clôturer</button>
              </div>
            </div>
          }
        </div>
      } @else {
        <div class="empty">Le technicien n'a pas encore rédigé de rapport.</div>
       }
      }
    </section>
  `,
  styles: [`
    .pre { white-space: pre-wrap; }
    .review { display: flex; flex-direction: column; gap: 10px; margin-top: 20px; padding-top: 16px; border-top: 1px solid var(--line); }
  `],
})
export class ReportPanelComponent implements OnChanges {
  readonly intervention = input.required<Intervention>();
  readonly isAssignedTechnician = input(false);
  readonly isManager = input(false);
  readonly changed = output<void>();

  private readonly service = inject(InterventionService);
  private readonly fb = inject(FormBuilder);

  readonly results: ReportResult[] = ['RESOLU', 'PARTIELLEMENT_RESOLU', 'NON_RESOLU'];
  readonly report = signal<InterventionReport | null>(null);
  readonly busy = signal(false);
  readonly error = signal<ApiError | null>(null);
  readonly info = signal<string | null>(null);

  readonly canEdit = computed(() => {
    const st = this.intervention().status;
    const rs = this.report()?.status;
    return this.isAssignedTechnician() && ['EN_COURS', 'SUSPENDUE', 'TERMINEE'].includes(st)
      && (rs === undefined || rs === 'BROUILLON' || rs === 'A_CORRIGER');
  });
  readonly canReview = computed(() => this.isManager() && this.report()?.status === 'SOUMIS');

  readonly form = this.fb.group({
    workStart: [''], workEnd: [''], result: [null as ReportResult | null],
    problemFound: [''], diagnosis: [''], solutionApplied: [''],
    materialsUsed: [''], replacedParts: [''], observations: [''],
  });

  ngOnChanges(): void {
    this.service.report(this.intervention().id).subscribe({
      next: (r) => this.setReport(r),
      error: (e: HttpErrorResponse) => {
        this.report.set(null);
        if (e.status !== 404) this.error.set(toApiError(e));
      },
    });
  }

  save(): void {
    const v = this.form.getRawValue();
    this.busy.set(true);
    this.error.set(null);
    this.info.set(null);
    this.service.saveReport(this.intervention().id, {
      ...v, workStart: fromInputDateTime(v.workStart), workEnd: fromInputDateTime(v.workEnd),
    } as Partial<InterventionReport>).subscribe({
      next: (r) => { this.setReport(r); this.info.set('Brouillon enregistré.'); this.busy.set(false); },
      error: (e) => { this.error.set(toApiError(e)); this.busy.set(false); },
    });
  }

  submit(): void {
    const v = this.form.getRawValue();
    this.busy.set(true);
    this.error.set(null);
    // On enregistre d'abord la dernière saisie, puis on soumet
    this.service.saveReport(this.intervention().id, {
      ...v, workStart: fromInputDateTime(v.workStart), workEnd: fromInputDateTime(v.workEnd),
    } as Partial<InterventionReport>).subscribe({
      next: () => this.service.submitReport(this.intervention().id).subscribe({
        next: (r) => { this.setReport(r); this.busy.set(false); this.changed.emit(); },
        error: (e) => { this.error.set(toApiError(e)); this.busy.set(false); },
      }),
      error: (e) => { this.error.set(toApiError(e)); this.busy.set(false); },
    });
  }

  review(approve: boolean, comment: string): void {
    this.busy.set(true);
    this.error.set(null);
    this.service.reviewReport(this.intervention().id, approve, comment || null).subscribe({
      next: (r) => { this.setReport(r); this.busy.set(false); this.changed.emit(); },
      error: (e) => { this.error.set(toApiError(e)); this.busy.set(false); },
    });
  }

  private setReport(r: InterventionReport): void {
    this.report.set(r);
    this.form.patchValue({
      ...r, workStart: toInputDateTime(r.workStart), workEnd: toInputDateTime(r.workEnd),
    });
  }
}
