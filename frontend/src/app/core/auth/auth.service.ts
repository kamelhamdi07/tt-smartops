import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, RegisterRequest, Role, UserInfo } from '../models/auth.model';

const TOKEN_KEY = 'smartops.token';
const USER_KEY = 'smartops.user';
const EXPIRES_KEY = 'smartops.expiresAt';

/**
 * Session côté navigateur. Le jeton est conservé en sessionStorage (fermé avec l'onglet).
 * Les droits réels sont toujours vérifiés par le backend : ce service ne sert qu'à l'affichage.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _user = signal<UserInfo | null>(this.restoreUser());
  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._user() !== null);
  readonly role = computed(() => this._user()?.role ?? null);
  readonly isTeamLeader = computed(() => this._user()?.teamLeader === true);

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', { email, password }).pipe(tap((res) => this.store(res)));
  }

  /** Inscription d'un client : la session est ouverte directement. */
  register(body: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/register', body).pipe(tap((res) => this.store(res)));
  }

  /** Accès à une page réservée à certains rôles, ou ouverte aussi aux chefs d'équipe. */
  canAccess(roles: Role[], teamLeaders = false): boolean {
    return roles.length === 0 || this.hasRole(...roles) || (teamLeaders && this.isTeamLeader());
  }

  /**
   * Relit le compte côté serveur. Si l'ADMIN a changé le rôle entre-temps,
   * la session est mise à jour et l'utilisateur revient au tableau de bord.
   */
  syncUser(): void {
    const before = this._user();
    if (!before) return;
    this.http.get<UserInfo>('/api/auth/me').subscribe({
      next: (user) => {
        if (user.role === before.role && user.teamLeader === before.teamLeader) return;
        sessionStorage.setItem(USER_KEY, JSON.stringify(user));
        this._user.set(user);
        this.router.navigate(['/dashboard']);
      },
      error: () => {},
    });
  }

  private store(res: AuthResponse): void {
    sessionStorage.setItem(TOKEN_KEY, res.token);
    sessionStorage.setItem(USER_KEY, JSON.stringify(res.user));
    sessionStorage.setItem(EXPIRES_KEY, String(Date.now() + res.expiresInSeconds * 1000));
    this._user.set(res.user);
  }

  logout(redirect = true): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
    sessionStorage.removeItem(EXPIRES_KEY);
    this._user.set(null);
    if (redirect) this.router.navigate(['/login']);
  }

  token(): string | null {
    const expiresAt = Number(sessionStorage.getItem(EXPIRES_KEY) ?? 0);
    if (!expiresAt || Date.now() > expiresAt) return null;
    return sessionStorage.getItem(TOKEN_KEY);
  }

  hasRole(...roles: Role[]): boolean {
    const r = this.role();
    return r !== null && roles.includes(r);
  }

  private restoreUser(): UserInfo | null {
    if (!this.token()) return null;
    try {
      return JSON.parse(sessionStorage.getItem(USER_KEY) ?? 'null');
    } catch {
      return null;
    }
  }
}
