import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models/api.model';

/** Normalise une erreur HTTP en message affichable, avec le détail des conflits éventuels. */
export function toApiError(err: unknown): ApiError {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return { status: 0, message: 'Le serveur ne répond pas. Vérifiez votre connexion puis réessayez.' };
    }
    const body = err.error as Partial<ApiError> | null;
    return {
      status: err.status,
      message: body?.message ?? 'Une erreur est survenue.',
      fieldErrors: body?.fieldErrors,
      details: body?.details,
    };
  }
  return { status: -1, message: 'Une erreur inattendue est survenue.' };
}
