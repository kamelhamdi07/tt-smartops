package tn.tt.smartops.intervention;

/** Publié dans la transaction de chaque changement de statut, pour les modules qui suivent une intervention. */
public record InterventionStatusChangedEvent(Intervention intervention, InterventionStatus from, InterventionStatus to) {}
