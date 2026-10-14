package tn.tt.smartops.reclamation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tn.tt.smartops.auth.User;
import tn.tt.smartops.common.BaseEntity;
import tn.tt.smartops.intervention.Intervention;
import tn.tt.smartops.team.Team;

import java.time.LocalDateTime;

/** Problème signalé par un client, pris en charge par le chef de l'équipe de sa catégorie. */
@Getter
@Setter
@Entity
@Table(name = "reclamations")
public class Reclamation extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id")
    private User client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReclamationCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReclamationStatus status = ReclamationStatus.NOUVELLE;

    /** Équipe destinataire ; null tant qu'aucune équipe ne couvre la catégorie. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    /** Intervention créée par le chef d'équipe pour traiter la réclamation. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intervention_id", unique = true)
    private Intervention intervention;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
