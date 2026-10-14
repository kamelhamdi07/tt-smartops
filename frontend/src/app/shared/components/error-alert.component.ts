import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { ApiError } from '../../core/models/api.model';

@Component({
  selector: 'app-error-alert',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (error(); as e) {
      <div class="alert alert-error" role="alert">
        {{ e.message }}
        @if (e.details?.length) {
          <ul>@for (d of e.details; track d) { <li>{{ d }}</li> }</ul>
        }
      </div>
    }
  `,
})
export class ErrorAlertComponent {
  readonly error = input<ApiError | null>(null);
}
