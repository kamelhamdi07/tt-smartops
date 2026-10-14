package tn.tt.smartops.reclamation;

/** Nature du problème signalé par le client ; sert au routage vers l'équipe compétente. */
public enum ReclamationCategory {
    WIFI,
    ADSL,
    FIBRE,
    TELEPHONE_FIXE,
    MOBILE,
    AUTRE
}
