import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/** Écrans publics (connexion, inscription) : panneau de marque à gauche, formulaire à droite. */
@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <div class="auth-shell">
      <aside class="brand">
        <div class="brand-top">
          <span class="mark" aria-hidden="true"></span>
          <span class="name">TT SmartOps</span>
        </div>

        <div class="pitch">
          <h1>Interventions techniques et ressources humaines, sur une seule plateforme.</h1>
          <ul>
            <li><span class="dot"></span><div><strong>Réclamations suivies en temps réel</strong>Du signalement client jusqu'à la résolution.</div></li>
            <li><span class="dot"></span><div><strong>Affectation par le chef d'équipe</strong>Technicien, véhicule et créneau en quelques clics.</div></li>
            <li><span class="dot"></span><div><strong>Planning sans conflit</strong>Congés, absences et ressources vérifiés automatiquement.</div></li>
          </ul>
        </div>

        <p class="foot">Tunisie Telecom · Direction Réseau et Transmission</p>
      </aside>

      <main class="panel">
        <router-outlet />
      </main>
    </div>
  `,
  styles: [`
    .auth-shell { min-height: 100vh; display: grid; grid-template-columns: minmax(380px, 5fr) 7fr; background: var(--card); }

    .brand {
      position: relative; overflow: hidden;
      display: flex; flex-direction: column; justify-content: space-between; gap: 40px;
      padding: 40px 48px; color: #fff;
      background: linear-gradient(160deg, var(--navy) 0%, var(--navy-2) 60%, #0d4f86 100%);
    }
    /* trame discrète évoquant un réseau */
    .brand::after {
      content: ''; position: absolute; inset: 0; pointer-events: none; opacity: .08;
      background-image: radial-gradient(circle, #fff 1px, transparent 1.5px);
      background-size: 22px 22px;
      mask-image: linear-gradient(200deg, #000 0%, transparent 70%);
    }
    .brand > * { position: relative; z-index: 1; }
    .brand-top { display: flex; align-items: center; gap: 12px; }
    .name { font-size: 18px; font-weight: 600; letter-spacing: .2px; }
    .mark { width: 38px; height: 38px; border-radius: 10px; background: var(--blue); position: relative; flex-shrink: 0;
            box-shadow: 0 0 0 1px rgba(255,255,255,.15) inset; }
    .mark::before { content: ''; position: absolute; left: 14px; top: 14px; width: 10px; height: 10px; border-radius: 50%; background: #fff; }
    .mark::after { content: ''; position: absolute; left: 9px; top: 9px; width: 20px; height: 20px; border-radius: 50%;
                   border: 2px solid rgba(255,255,255,.45); }

    .pitch h1 { font-size: 28px; line-height: 1.3; font-weight: 600; max-width: 460px; margin-bottom: 32px; }
    .pitch ul { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 20px; max-width: 440px; }
    .pitch li { display: flex; gap: 14px; align-items: flex-start; color: rgba(255,255,255,.75); }
    .pitch strong { display: block; color: #fff; font-weight: 600; margin-bottom: 2px; }
    .dot { width: 10px; height: 10px; margin-top: 6px; border-radius: 50%; background: var(--signal); flex-shrink: 0;
           box-shadow: 0 0 0 4px rgba(0,166,180,.25); }
    .foot { margin: 0; font-size: 12px; color: rgba(255,255,255,.55); }

    .panel { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 48px 24px; background: var(--card); }

    @media (max-width: 900px) {
      .auth-shell { grid-template-columns: minmax(0, 1fr); grid-template-rows: auto 1fr; }
      .brand { padding: 24px 20px; gap: 0; }
      .pitch, .foot { display: none; }
      .panel { padding: 32px 16px 48px; justify-content: flex-start; }
    }
  `],
})
export class AuthLayoutComponent {}
