import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { toApiError } from '../../core/services/api-error';

type Field = 'fullName' | 'email' | 'phone' | 'address' | 'password';

/** Inscription libre d'un client, qui peut ensuite déposer ses réclamations. */
@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <form class="auth-form" [formGroup]="form" (ngSubmit)="submit()" novalidate>
      <div class="auth-head">
        <h1>Créer un compte client</h1>
        <p>Signalez un problème Wi-Fi, ADSL, fibre ou téléphone et suivez sa résolution.</p>
      </div>

      <div class="field">
        <label for="name">Nom et prénom</label>
        <input id="name" class="input" formControlName="fullName" autocomplete="name" />
        @if (invalid('fullName')) { <span class="field-error">Indiquez votre nom.</span> }
      </div>
      <div class="field">
        <label for="email">Email</label>
        <input id="email" class="input" type="email" formControlName="email" autocomplete="email" placeholder="nom@exemple.tn" />
        @if (invalid('email')) { <span class="field-error">Saisissez une adresse email valide.</span> }
      </div>
      <div class="row2">
        <div class="field">
          <label for="phone">Téléphone</label>
          <input id="phone" class="input" type="tel" formControlName="phone" autocomplete="tel" placeholder="ex. 22 123 456" />
          @if (invalid('phone')) { <span class="field-error">Indiquez un numéro.</span> }
        </div>
        <div class="field">
          <label for="address">Adresse du service</label>
          <input id="address" class="input" formControlName="address" autocomplete="street-address" />
          @if (invalid('address')) { <span class="field-error">Indiquez l'adresse.</span> }
        </div>
      </div>
      <div class="field">
        <label for="pwd">Mot de passe</label>
        <div class="password">
          <input id="pwd" class="input" [type]="showPassword() ? 'text' : 'password'" formControlName="password" autocomplete="new-password" />
          <button type="button" class="toggle" (click)="showPassword.set(!showPassword())">{{ showPassword() ? 'Masquer' : 'Afficher' }}</button>
        </div>
        @if (invalid('password')) { <span class="field-error">Au moins 10 caractères.</span> }
        @else { <span class="hint">Au moins 10 caractères.</span> }
      </div>

      @if (error()) { <div class="alert alert-error" role="alert">{{ error() }}</div> }

      <button class="btn btn-primary" type="submit" [disabled]="loading()">
        @if (loading()) { <span class="spinner" aria-hidden="true"></span> Création du compte… } @else { Créer mon compte }
      </button>

      <p class="switch">Déjà inscrit ? <a routerLink="/login">Se connecter</a></p>
    </form>
  `,
  styles: [`
    .row2 { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .hint { font-size: 12px; color: var(--ink-3); }
    @media (max-width: 480px) { .row2 { grid-template-columns: 1fr; } }
  `],
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly showPassword = signal(false);

  readonly form = this.fb.nonNullable.group({
    fullName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', Validators.required],
    address: ['', Validators.required],
    password: ['', [Validators.required, Validators.minLength(10)]],
  });

  invalid(name: Field): boolean {
    const c = this.form.controls[name];
    return c.invalid && c.touched;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const v = this.form.getRawValue();
    this.auth.register({ ...v, email: v.email.trim() }).subscribe({
      next: () => this.router.navigateByUrl('/my-reclamations'),
      error: (e) => {
        const err = toApiError(e);
        this.error.set(err.status === 0 || err.status >= 500
          ? 'Le service est momentanément indisponible. Réessayez dans quelques instants.' : err.message);
        this.loading.set(false);
      },
    });
  }
}
