import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { toApiError } from '../../core/services/api-error';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <form class="auth-form" [formGroup]="form" (ngSubmit)="submit()" novalidate>
      <div class="auth-head">
        <h1>Connexion</h1>
        <p>Accédez à votre espace avec votre email et votre mot de passe.</p>
      </div>

      <div class="field">
        <label for="email">Email</label>
        <input id="email" class="input" type="email" formControlName="email" autocomplete="username"
               placeholder="nom@exemple.tn" [attr.aria-invalid]="invalid('email')" />
        @if (invalid('email')) { <span class="field-error">Saisissez une adresse email valide.</span> }
      </div>

      <div class="field">
        <label for="password">Mot de passe</label>
        <div class="password">
          <input id="password" class="input" [type]="showPassword() ? 'text' : 'password'" formControlName="password"
                 autocomplete="current-password" [attr.aria-invalid]="invalid('password')" />
          <button type="button" class="toggle" (click)="showPassword.set(!showPassword())"
                  [attr.aria-label]="showPassword() ? 'Masquer le mot de passe' : 'Afficher le mot de passe'">
            {{ showPassword() ? 'Masquer' : 'Afficher' }}
          </button>
        </div>
        @if (invalid('password')) { <span class="field-error">Saisissez votre mot de passe.</span> }
      </div>

      @if (error()) { <div class="alert alert-error" role="alert">{{ error() }}</div> }

      <button class="btn btn-primary" type="submit" [disabled]="loading()">
        @if (loading()) { <span class="spinner" aria-hidden="true"></span> Connexion… } @else { Se connecter }
      </button>

      <p class="switch">Nouveau client ? <a routerLink="/register">Créer un compte</a></p>
    </form>
  `,
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly showPassword = signal(false);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  invalid(name: 'email' | 'password'): boolean {
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
    const { email, password } = this.form.getRawValue();
    this.auth.login(email.trim(), password).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/dashboard';
        this.router.navigateByUrl(returnUrl.startsWith('/') ? returnUrl : '/dashboard');
      },
      error: (e) => {
        const err = toApiError(e);
        this.error.set(
          err.status === 401 ? 'Email ou mot de passe incorrect. Vérifiez votre saisie, ou créez un compte si vous êtes un nouveau client.'
          : err.status === 0 || err.status >= 500 ? 'Le service est momentanément indisponible. Réessayez dans quelques instants.'
          : err.message);
        this.form.controls.password.reset();
        this.loading.set(false);
      },
    });
  }
}
