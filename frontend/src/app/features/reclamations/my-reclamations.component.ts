import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiError, Page } from '../../core/models/api.model';
import { Reclamation, ReclamationCategory, ReclamationStatus } from '../../core/models/reclamation.model';
import { toApiError } from '../../core/services/api-error';
import { ReclamationService } from '../../core/services/reclamation.service';
import { ErrorAlertComponent } from '../../shared/components/error-alert.component';
import { PagerComponent } from '../../shared/components/pager.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { LabelPipe } from '../../shared/pipes/label.pipe';
import { RECLAMATION_CATEGORIES, options } from '../../shared/utils/labels';

const STEPS: { status: ReclamationStatus; label: string }[] = [
  { status: 'NOUVELLE', label: 'Envoyée' },
  { status: 'AFFECTEE', label: 'Technicien affecté' },
  { status: 'EN_COURS', label: 'Intervention en cours' },
  { status: 'RESOLUE', label: 'Résolue' },
];

@Component({
  selector: 'app-my-reclamations',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, LabelPipe, StatusBadgeComponent, PagerComponent, ErrorAlertComponent],
  template: `
    <div class="page">
      <div class="page-head">
        <div><h1>Mes réclamations</h1><p>Signalez un problème : il est transmis au chef de l'équipe concernée, qui vous envoie un technicien.</p></div>
      </div>

      <form class="card" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <div class="card-head"><h2>Nouvelle réclamation</h2></div>
        <div class="card-body form-grid">
          <div class="field"><label for="cat">Type de problème</label>
            <select id="cat" class="input" formControlName="category">
              @for (c of categories; track c.value) { <option [value]="c.value">{{ c.label }}</option> }
            </select></div>
          <div class="field"><label for="title">Objet</label><input id="title" class="input" formControlName="title" maxlength="200" placeholder="ex. Pas de connexion Wi-Fi depuis ce matin" /></div>
          <div class="field full"><label for="desc">Description</label><textarea id="desc" class="input" formControlName="description" maxlength="2000" placeholder="Depuis quand, voyants du modem, ce que vous avez déjà essayé…"></textarea></div>
          <div class="field"><label for="addr">Adresse d'intervention</label><input id="addr" class="input" formControlName="address" maxlength="255" placeholder="Par défaut : adresse de votre compte" /></div>
          <div class="field"><label for="tel">Téléphone de contact</label><input id="tel" class="input" type="tel" formControlName="phone" maxlength="30" placeholder="Par défaut : téléphone de votre compte" /></div>
          <div class="field full"><app-error-alert [error]="formError()" />
            @if (info()) { <div class="alert alert-ok">{{ info() }}</div> }</div>
        </div>
        <div class="card-body row" style="border-top: 1px solid var(--line)">
          <span class="spacer"></span>
          <button class="btn btn-primary" type="submit" [disabled]="form.invalid || busy()">Envoyer la réclamation</button>
        </div>
      </form>

      <app-error-alert [error]="error()" />
      @if (page(); as p) {
        @if (p.content.length === 0) {
          <section class="card"><div class="empty"><strong>Aucune réclamation</strong>Vos réclamations et leur avancement s'afficheront ici.</div></section>
        } @else {
          @for (r of p.content; track r.id) {
            <section class="card rec">
              <div class="card-head">
                <div>
                  <h2>{{ r.title }}</h2>
                  <span class="muted">{{ r.reference }} · {{ r.category | label }} · envoyée le {{ r.createdAt | date: 'dd/MM/yyyy HH:mm' }}</span>
                </div>
                <app-badge [value]="r.status" />
              </div>
              <div class="card-body">
                @if (r.status !== 'ANNULEE') {
                  <ol class="steps">
                    @for (s of steps; track s.status; let i = $index) {
                      <li [class.done]="stepIndex(r) >= i" [class.current]="stepIndex(r) === i">{{ s.label }}</li>
                    }
                  </ol>
                }
                <p class="pre">{{ r.description }}</p>
                <dl class="details">
                  <dt>Adresse</dt><dd>{{ r.address }}</dd>
                  <dt>Équipe</dt><dd>{{ r.teamName || 'En cours d\\'orientation' }}</dd>
                  @if (r.technicians.length) { <dt>Technicien</dt><dd>{{ r.technicians.join(', ') }}</dd> }
                  @if (r.plannedStart) { <dt>Passage prévu</dt><dd>{{ r.plannedStart | date: 'dd/MM/yyyy HH:mm' }} – {{ r.plannedEnd | date: 'HH:mm' }}</dd> }
                  @if (r.vehiclePlate) { <dt>Véhicule</dt><dd>{{ r.vehiclePlate }}</dd> }
                  @if (r.resolvedAt) { <dt>Résolue le</dt><dd>{{ r.resolvedAt | date: 'dd/MM/yyyy HH:mm' }}</dd> }
                </dl>
                @if (r.status === 'NOUVELLE') {
                  <div class="row"><span class="spacer"></span><button class="btn btn-sm btn-danger" (click)="cancel(r)">Annuler la réclamation</button></div>
                }
              </div>
            </section>
          }
          <app-pager [page]="p.page" [totalPages]="p.totalPages" [totalElements]="p.totalElements" (pageChange)="load($event)" />
        }
      } @else { <div class="loading">Chargement…</div> }
    </div>
  `,
  styles: [`
    .rec .card-head { align-items: flex-start; }
    .rec h2 { margin-bottom: 2px; }
    .pre { white-space: pre-wrap; margin: 0 0 12px; }
    .steps { list-style: none; display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; margin: 0 0 16px; padding: 0; counter-reset: s; }
    .steps li { counter-increment: s; padding: 8px 10px; border-radius: 6px; background: var(--surface); color: var(--ink-3); font-size: 13px; border: 1px solid var(--line); }
    .steps li::before { content: counter(s) '. '; font-weight: 600; }
    .steps li.done { background: var(--ok-soft); color: var(--ok); border-color: transparent; }
    .steps li.current { font-weight: 600; }
    @media (max-width: 640px) { .steps { grid-template-columns: 1fr 1fr; } }
  `],
})
export class MyReclamationsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ReclamationService);

  readonly categories = options(RECLAMATION_CATEGORIES);
  readonly steps = STEPS;
  readonly page = signal<Page<Reclamation> | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly formError = signal<ApiError | null>(null);
  readonly info = signal<string | null>(null);
  readonly busy = signal(false);
  private current = 0;

  readonly form = this.fb.group({
    category: ['WIFI' as ReclamationCategory, Validators.required],
    title: ['', Validators.required],
    description: ['', Validators.required],
    address: [''],
    phone: [''],
  });

  constructor() {
    // Le statut change sous l'action d'autres utilisateurs : on recharge la page affichée toutes les 15 s.
    interval(15_000).pipe(takeUntilDestroyed()).subscribe(() => this.load(this.current));
  }

  ngOnInit(): void {
    this.load(0);
  }

  load(page: number): void {
    this.current = page;
    this.service.mine(page).subscribe({ next: (p) => { this.page.set(p); this.error.set(null); }, error: (e) => this.error.set(toApiError(e)) });
  }

  stepIndex(r: Reclamation): number {
    return STEPS.findIndex((s) => s.status === r.status);
  }

  save(): void {
    const v = this.form.getRawValue();
    this.busy.set(true);
    this.formError.set(null);
    this.info.set(null);
    this.service.create({
      category: v.category!, title: v.title!.trim(), description: v.description!.trim(),
      address: v.address || null, phone: v.phone || null,
    }).subscribe({
      next: (r) => {
        this.busy.set(false);
        this.info.set(`Réclamation ${r.reference} envoyée${r.teamName ? ' à ' + r.teamName : ''}.`);
        this.form.reset({ category: 'WIFI', title: '', description: '', address: '', phone: '' });
        this.load(0);
      },
      error: (e) => { this.busy.set(false); this.formError.set(toApiError(e)); },
    });
  }

  cancel(r: Reclamation): void {
    if (!confirm(`Annuler la réclamation ${r.reference} ?`)) return;
    this.service.cancel(r.id).subscribe({ next: () => this.load(0), error: (e) => this.error.set(toApiError(e)) });
  }
}
