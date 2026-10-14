import { Pipe, PipeTransform } from '@angular/core';
import { label } from '../utils/labels';

@Pipe({ name: 'label', standalone: true })
export class LabelPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    return label(value);
  }
}
