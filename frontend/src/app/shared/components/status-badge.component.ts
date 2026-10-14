import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { label, tone } from '../utils/labels';

/** Badge coloré pour statuts et priorités ; la couleur est toujours doublée par le libellé. */
@Component({
  selector: 'app-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span [class]="'badge ' + cssTone()">{{ text() }}</span>`,
})
export class StatusBadgeComponent {
  readonly value = input<string | null>(null);
  readonly text = computed(() => label(this.value()));
  readonly cssTone = computed(() => tone(this.value()));
}
