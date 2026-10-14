import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'app-pager',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (totalPages() > 1) {
      <div class="pager">
        <span>{{ totalElements() }} résultat(s) · page {{ page() + 1 }} sur {{ totalPages() }}</span>
        <div class="row">
          <button class="btn btn-sm" [disabled]="page() === 0" (click)="pageChange.emit(page() - 1)">Précédent</button>
          <button class="btn btn-sm" [disabled]="page() + 1 >= totalPages()" (click)="pageChange.emit(page() + 1)">Suivant</button>
        </div>
      </div>
    }
  `,
})
export class PagerComponent {
  readonly page = input(0);
  readonly totalPages = input(0);
  readonly totalElements = input(0);
  readonly pageChange = output<number>();
}
