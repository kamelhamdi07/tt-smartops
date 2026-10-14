import { Role } from '../core/models/auth.model';

/** teamLeaders : visible aussi pour un chef d'équipe, quel que soit son rôle. */
export interface NavItem { label: string; path: string; roles: Role[]; teamLeaders?: boolean; }
export interface NavSection { title: string; items: NavItem[]; }

const ALL: Role[] = ['ADMIN', 'RH', 'RESPONSABLE_TECHNIQUE', 'TECHNICIEN', 'EMPLOYE'];
const STAFF: Role[] = ['RH', 'RESPONSABLE_TECHNIQUE', 'TECHNICIEN', 'EMPLOYE'];

export const NAVIGATION: NavSection[] = [
  {
    title: 'Pilotage',
    items: [
      { label: 'Tableau de bord', path: '/dashboard', roles: ALL },
      { label: 'Notifications', path: '/notifications', roles: ALL },
    ],
  },
  {
    title: 'Opérations',
    items: [
      { label: 'Interventions', path: '/interventions', roles: ['ADMIN', 'RESPONSABLE_TECHNIQUE', 'TECHNICIEN'] },
      { label: 'Planning', path: '/planning', roles: ['ADMIN', 'RESPONSABLE_TECHNIQUE', 'TECHNICIEN'] },
      { label: 'Réclamations clients', path: '/reclamations', roles: ['ADMIN', 'RESPONSABLE_TECHNIQUE'], teamLeaders: true },
    ],
  },
  {
    title: 'Espace client',
    items: [
      { label: 'Mes réclamations', path: '/my-reclamations', roles: ['CLIENT'] },
    ],
  },
  {
    title: 'Ressources humaines',
    items: [
      { label: 'Employés', path: '/employees', roles: ['ADMIN', 'RH', 'RESPONSABLE_TECHNIQUE'] },
      { label: 'Demandes de congé', path: '/leaves/manage', roles: ['RH'] },
      { label: 'Absences', path: '/absences', roles: ['RH'] },
    ],
  },
  {
    title: 'Administration',
    items: [
      { label: 'Comptes utilisateurs', path: '/admin/users', roles: ['ADMIN'] },
      { label: 'Départements et équipes', path: '/admin/organisation', roles: ['ADMIN', 'RH'] },
      { label: 'Équipements', path: '/admin/equipment', roles: ['ADMIN', 'RESPONSABLE_TECHNIQUE'] },
      { label: 'Véhicules', path: '/admin/vehicles', roles: ['ADMIN', 'RESPONSABLE_TECHNIQUE'] },
    ],
  },
  {
    title: 'Mon espace',
    items: [
      { label: 'Mon profil', path: '/profile', roles: STAFF },
      { label: 'Mes congés', path: '/leaves', roles: STAFF },
    ],
  },
];
