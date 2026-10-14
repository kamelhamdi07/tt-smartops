/** Libellés français des énumérations du backend : une seule source pour toute l'interface. */
export const LABELS: Record<string, string> = {
  // rôles
  ADMIN: 'Administrateur', RH: 'Ressources humaines', RESPONSABLE_TECHNIQUE: 'Responsable technique',
  TECHNICIEN: 'Technicien', EMPLOYE: 'Employé', CLIENT: 'Client',
  // statuts employé
  ACTIF: 'Actif', SUSPENDU: 'Suspendu', INACTIF: 'Inactif',
  // spécialités
  RESEAU: 'Réseau', FIBRE_OPTIQUE: 'Fibre optique', TRANSMISSION: 'Transmission', RADIO_MOBILE: 'Radio mobile',
  ENERGIE: 'Énergie', CLIMATISATION: 'Climatisation', INFORMATIQUE: 'Informatique',
  // congés
  ANNUEL: 'Annuel', MALADIE: 'Maladie', EXCEPTIONNEL: 'Exceptionnel', MATERNITE: 'Maternité', SANS_SOLDE: 'Sans solde',
  EN_ATTENTE: 'En attente', APPROUVEE: 'Approuvée', REFUSEE: 'Refusée',
  // absences
  AUTORISEE: 'Autorisée', MISSION_EXTERNE: 'Mission externe', FORMATION: 'Formation', INJUSTIFIEE: 'Injustifiée',
  // interventions
  MAINTENANCE: 'Maintenance', REPARATION: 'Réparation', DEPANNAGE: 'Dépannage', INSTALLATION: 'Installation',
  CONTROLE: 'Contrôle', REMPLACEMENT_EQUIPEMENT: "Remplacement d'équipement", INTERVENTION_RESEAU: 'Intervention réseau',
  BASSE: 'Basse', NORMALE: 'Normale', HAUTE: 'Haute', CRITIQUE: 'Critique',
  NOUVELLE: 'Nouvelle', PLANIFIEE: 'Planifiée', AFFECTEE: 'Affectée', EN_COURS: 'En cours', SUSPENDUE: 'Suspendue',
  TERMINEE: 'Terminée', VALIDEE: 'Clôturée', ANNULEE: 'Annulée',
  // rapports
  BROUILLON: 'Brouillon', SOUMIS: 'Soumis', A_CORRIGER: 'À corriger', VALIDE: 'Validé',
  RESOLU: 'Résolu', PARTIELLEMENT_RESOLU: 'Partiellement résolu', NON_RESOLU: 'Non résolu',
  // disponibilité
  DISPONIBLE: 'Disponible', OCCUPE: 'Occupé', CONGE: 'Congé', ABSENT: 'Absent',
  // équipements et véhicules (AFFECTE est partagé avec les interventions)
  EN_MAINTENANCE: 'En maintenance', HORS_SERVICE: 'Hors service',
  // réclamations clients (NOUVELLE, AFFECTEE, EN_COURS, ANNULEE sont partagés avec les interventions)
  RESOLUE: 'Résolue',
  WIFI: 'Wi-Fi', ADSL: 'ADSL', FIBRE: 'Fibre', TELEPHONE_FIXE: 'Téléphone fixe', MOBILE: 'Mobile', AUTRE: 'Autre',
};

export function label(value: string | null | undefined): string {
  return value ? LABELS[value] ?? value : '—';
}

export type Tone = '' | 'info' | 'ok' | 'warn' | 'danger' | 'signal';

const TONES: Record<string, Tone> = {
  CRITIQUE: 'danger', HAUTE: 'warn', NORMALE: 'info', BASSE: '',
  NOUVELLE: 'info', PLANIFIEE: '', AFFECTEE: 'signal', EN_COURS: 'warn', SUSPENDUE: 'danger',
  TERMINEE: 'ok', VALIDEE: 'ok', ANNULEE: '',
  EN_ATTENTE: 'warn', APPROUVEE: 'ok', REFUSEE: 'danger',
  ACTIF: 'ok', SUSPENDU: 'warn', INACTIF: '',
  BROUILLON: '', SOUMIS: 'info', A_CORRIGER: 'warn', VALIDE: 'ok',
  DISPONIBLE: 'ok', EN_MAINTENANCE: 'warn', HORS_SERVICE: 'danger', RESOLUE: 'ok',
};

export function tone(value: string | null | undefined): Tone {
  return value ? TONES[value] ?? '' : '';
}

export function options<T extends string>(values: readonly T[]): { value: T; label: string }[] {
  return values.map((v) => ({ value: v, label: label(v) }));
}

export const SPECIALTIES = ['RESEAU', 'FIBRE_OPTIQUE', 'TRANSMISSION', 'RADIO_MOBILE', 'ENERGIE', 'CLIMATISATION', 'INFORMATIQUE'] as const;
export const PRIORITIES = ['BASSE', 'NORMALE', 'HAUTE', 'CRITIQUE'] as const;
export const INTERVENTION_TYPES = ['MAINTENANCE', 'REPARATION', 'DEPANNAGE', 'INSTALLATION', 'CONTROLE',
  'REMPLACEMENT_EQUIPEMENT', 'INTERVENTION_RESEAU'] as const;
export const INTERVENTION_STATUSES = ['NOUVELLE', 'PLANIFIEE', 'AFFECTEE', 'EN_COURS', 'SUSPENDUE', 'TERMINEE',
  'VALIDEE', 'ANNULEE'] as const;
export const LEAVE_TYPES = ['ANNUEL', 'MALADIE', 'EXCEPTIONNEL', 'MATERNITE', 'SANS_SOLDE'] as const;
export const ABSENCE_TYPES = ['MALADIE', 'AUTORISEE', 'MISSION_EXTERNE', 'FORMATION', 'INJUSTIFIEE'] as const;
export const ROLES = ['ADMIN', 'RH', 'RESPONSABLE_TECHNIQUE', 'TECHNICIEN', 'EMPLOYE', 'CLIENT'] as const;
export const RECLAMATION_CATEGORIES = ['WIFI', 'ADSL', 'FIBRE', 'TELEPHONE_FIXE', 'MOBILE', 'AUTRE'] as const;
export const RECLAMATION_STATUSES = ['NOUVELLE', 'AFFECTEE', 'EN_COURS', 'RESOLUE', 'ANNULEE'] as const;
export const ASSET_STATUSES = ['DISPONIBLE', 'AFFECTE', 'EN_MAINTENANCE', 'HORS_SERVICE'] as const;
