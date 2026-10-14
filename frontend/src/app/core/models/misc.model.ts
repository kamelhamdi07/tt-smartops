import { Role } from './auth.model';

export interface Kpi {
  key: string;
  label: string;
  value: number;
  tone: 'neutral' | 'info' | 'success' | 'warning' | 'danger';
  link: string | null;
}

export interface Chart {
  key: string;
  title: string;
  points: { label: string; value: number }[];
}

export interface Dashboard {
  role: Role;
  kpis: Kpi[];
  charts: Chart[];
}

export interface AppNotification {
  id: number;
  type: string;
  title: string;
  message: string;
  link: string | null;
  read: boolean;
  createdAt: string;
}
