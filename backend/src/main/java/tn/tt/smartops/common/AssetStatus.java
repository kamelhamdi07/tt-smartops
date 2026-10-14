package tn.tt.smartops.common;

/**
 * État physique d'un équipement ou d'un véhicule.
 * AFFECTE est positionné automatiquement quand une intervention qui l'utilise est EN_COURS.
 * Les réservations futures sont calculées à partir des créneaux des interventions.
 */
public enum AssetStatus {
    DISPONIBLE,
    AFFECTE,
    EN_MAINTENANCE,
    HORS_SERVICE;

    public boolean isBookable() {
        return this == DISPONIBLE || this == AFFECTE;
    }
}
