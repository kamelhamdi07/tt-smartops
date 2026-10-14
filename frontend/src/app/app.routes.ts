import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { AuthLayoutComponent } from './layouts/auth-layout.component';
import { DashboardLayoutComponent } from './layouts/dashboard-layout.component';

/**
 * Les rôles déclarés ici ne servent qu'à la navigation : chaque endpoint
 * est protégé indépendamment côté backend par @PreAuthorize.
 */
export const routes: Routes = [
  {
    path: 'login',
    component: AuthLayoutComponent,
    children: [{ path: '', loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent) }],
  },
  {
    path: 'register',
    component: AuthLayoutComponent,
    children: [{ path: '', loadComponent: () => import('./features/auth/register.component').then((m) => m.RegisterComponent) }],
  },
  {
    path: '',
    component: DashboardLayoutComponent,
    canActivate: [authGuard],
    canActivateChild: [roleGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent) },
      {
        path: 'profile',
        data: { roles: ['RH', 'RESPONSABLE_TECHNIQUE', 'TECHNICIEN', 'EMPLOYE'] },
        loadComponent: () => import('./features/profile/profile.component').then((m) => m.ProfileComponent),
      },
      {
        path: 'employees',
        data: { roles: ['ADMIN', 'RH', 'RESPONSABLE_TECHNIQUE'] },
        loadComponent: () => import('./features/employees/employee-list.component').then((m) => m.EmployeeListComponent),
      },
      {
        path: 'employees/new',
        data: { roles: ['RH'] },
        loadComponent: () => import('./features/employees/employee-form.component').then((m) => m.EmployeeFormComponent),
      },
      {
        path: 'employees/:id/edit',
        data: { roles: ['RH'] },
        loadComponent: () => import('./features/employees/employee-form.component').then((m) => m.EmployeeFormComponent),
      },
      {
        path: 'leaves',
        data: { roles: ['RH', 'RESPONSABLE_TECHNIQUE', 'TECHNICIEN', 'EMPLOYE'] },
        loadComponent: () => import('./features/leaves/my-leaves.component').then((m) => m.MyLeavesComponent),
      },
      {
        path: 'leaves/manage',
        data: { roles: ['RH'] },
        loadComponent: () => import('./features/leaves/leave-management.component').then((m) => m.LeaveManagementComponent),
      },
      {
        path: 'absences',
        data: { roles: ['RH'] },
        loadComponent: () => import('./features/absences/absence-list.component').then((m) => m.AbsenceListComponent),
      },
      {
        path: 'interventions',
        data: { roles: ['ADMIN', 'RESPONSABLE_TECHNIQUE', 'TECHNICIEN'] },
        loadComponent: () => import('./features/interventions/intervention-list.component').then((m) => m.InterventionListComponent),
      },
      {
        path: 'interventions/new',
        data: { roles: ['RESPONSABLE_TECHNIQUE'] },
        loadComponent: () => import('./features/interventions/intervention-form.component').then((m) => m.InterventionFormComponent),
      },
      {
        path: 'interventions/:id/edit',
        data: { roles: ['RESPONSABLE_TECHNIQUE'] },
        loadComponent: () => import('./features/interventions/intervention-form.component').then((m) => m.InterventionFormComponent),
      },
      {
        path: 'interventions/:id',
        data: { roles: ['ADMIN', 'RESPONSABLE_TECHNIQUE', 'TECHNICIEN'] },
        loadComponent: () => import('./features/interventions/intervention-detail.component').then((m) => m.InterventionDetailComponent),
      },
      {
        path: 'planning',
        data: { roles: ['ADMIN', 'RESPONSABLE_TECHNIQUE', 'TECHNICIEN'] },
        loadComponent: () => import('./features/planning/planning.component').then((m) => m.PlanningComponent),
      },
      {
        path: 'my-reclamations',
        data: { roles: ['CLIENT'] },
        loadComponent: () => import('./features/reclamations/my-reclamations.component').then((m) => m.MyReclamationsComponent),
      },
      {
        path: 'reclamations',
        data: { roles: ['ADMIN', 'RESPONSABLE_TECHNIQUE'], teamLeaders: true },
        loadComponent: () => import('./features/reclamations/reclamation-inbox.component').then((m) => m.ReclamationInboxComponent),
      },
      {
        path: 'admin/users',
        data: { roles: ['ADMIN'] },
        loadComponent: () => import('./features/admin/users.component').then((m) => m.UsersComponent),
      },
      {
        path: 'admin/organisation',
        data: { roles: ['ADMIN', 'RH'] },
        loadComponent: () => import('./features/admin/organisation.component').then((m) => m.OrganisationComponent),
      },
      {
        path: 'admin/equipment',
        data: { roles: ['ADMIN', 'RESPONSABLE_TECHNIQUE'] },
        loadComponent: () => import('./features/admin/equipment.component').then((m) => m.EquipmentComponent),
      },
      {
        path: 'admin/vehicles',
        data: { roles: ['ADMIN', 'RESPONSABLE_TECHNIQUE'] },
        loadComponent: () => import('./features/admin/vehicles.component').then((m) => m.VehiclesComponent),
      },
      { path: 'notifications', loadComponent: () => import('./features/notifications/notification-list.component').then((m) => m.NotificationListComponent) },
    ],
  },
  { path: '**', redirectTo: '' },
];
