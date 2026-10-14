package tn.tt.smartops.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.tt.smartops.auth.Role;
import tn.tt.smartops.auth.User;
import tn.tt.smartops.auth.UserRepository;
import tn.tt.smartops.common.AssetStatus;
import tn.tt.smartops.common.DateRanges;
import tn.tt.smartops.department.Department;
import tn.tt.smartops.department.DepartmentRepository;
import tn.tt.smartops.employee.Employee;
import tn.tt.smartops.employee.EmployeeRepository;
import tn.tt.smartops.employee.Specialty;
import tn.tt.smartops.equipment.Equipment;
import tn.tt.smartops.equipment.EquipmentRepository;
import tn.tt.smartops.leave.LeaveRequest;
import tn.tt.smartops.leave.LeaveRequestRepository;
import tn.tt.smartops.leave.LeaveStatus;
import tn.tt.smartops.leave.LeaveType;
import tn.tt.smartops.team.Team;
import tn.tt.smartops.team.TeamRepository;
import tn.tt.smartops.vehicle.Vehicle;
import tn.tt.smartops.vehicle.VehicleRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Jeu de données de démonstration (DEMO_DATA_ENABLED=true). S'exécute une seule fois sur
 * une base vide. Le mot de passe commun des comptes de démo vient de DEMO_PASSWORD.
 * À désactiver en dehors des environnements de démonstration.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private final AppProperties props;
    private final DepartmentRepository departments;
    private final TeamRepository teams;
    private final EmployeeRepository employees;
    private final UserRepository users;
    private final EquipmentRepository equipment;
    private final VehicleRepository vehicles;
    private final LeaveRequestRepository leaves;
    private final PasswordEncoder encoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.demo().enabled() || departments.count() > 0) return;
        String password = props.demo().password();
        if (password == null || password.length() < 10) {
            log.warn("DEMO_DATA_ENABLED=true mais DEMO_PASSWORD absent ou trop court : données de démo ignorées.");
            return;
        }

        Department drt = dept("DRT", "Direction Réseau et Transmission");
        Department drh = dept("DRH", "Direction des Ressources Humaines");
        Team fibre = team("Équipe Fibre Tunis Sud", drt);
        Team energie = team("Équipe Énergie Ben Arous", drt);
        Team gestionRh = team("Gestion du personnel", drh);

        Employee rh = emp("TT1001", "Salma", "Ben Ali", "Chargée RH", false, null, gestionRh);
        Employee resp = emp("TT2001", "Karim", "Trabelsi", "Responsable maintenance réseau", false, null, fibre);
        Employee t1 = emp("TT3001", "Mehdi", "Gharbi", "Technicien fibre optique", true, Specialty.FIBRE_OPTIQUE, fibre);
        Employee t2 = emp("TT3002", "Yassine", "Jebali", "Technicien réseau", true, Specialty.RESEAU, fibre);
        Employee t3 = emp("TT3003", "Amine", "Mansouri", "Technicien énergie", true, Specialty.ENERGIE, energie);
        Employee assistant = emp("TT4001", "Ines", "Haddad", "Assistante administrative", false, null, gestionRh);

        fibre.setLeader(resp);
        gestionRh.setLeader(rh);

        account("rh@smartops.tn", Role.RH, rh, password);
        account("responsable@smartops.tn", Role.RESPONSABLE_TECHNIQUE, resp, password);
        account("mehdi.gharbi@smartops.tn", Role.TECHNICIEN, t1, password);
        account("yassine.jebali@smartops.tn", Role.TECHNICIEN, t2, password);
        account("amine.mansouri@smartops.tn", Role.TECHNICIEN, t3, password);
        account("ines.haddad@smartops.tn", Role.EMPLOYE, assistant, password);

        equip("EQ-OTDR-01", "Réflectomètre OTDR", "Mesure fibre", t1);
        equip("EQ-SOUD-01", "Soudeuse fibre optique", "Raccordement", t1);
        equip("EQ-GE-01", "Groupe électrogène 20 kVA", "Énergie", t3);

        vehicle("123 TU 4567", "Renault Kangoo", "Utilitaire", fibre);
        vehicle("124 TU 8910", "Isuzu D-Max", "Pick-up", energie);

        // Un congé approuvé la semaine prochaine pour illustrer la détection de conflit
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(t2);
        leave.setType(LeaveType.ANNUEL);
        leave.setStartDate(monday);
        leave.setEndDate(monday.plusDays(2));
        leave.setWorkingDays(DateRanges.workingDays(monday, monday.plusDays(2)));
        leave.setStatus(LeaveStatus.APPROUVEE);
        leave.setProcessedAt(LocalDateTime.now());
        leaves.save(leave);

        log.info("Données de démonstration créées (6 comptes, mot de passe = DEMO_PASSWORD).");
    }

    private Department dept(String code, String name) {
        Department d = new Department();
        d.setCode(code);
        d.setName(name);
        return departments.save(d);
    }

    private Team team(String name, Department d) {
        Team t = new Team();
        t.setName(name);
        t.setDepartment(d);
        return teams.save(t);
    }

    private Employee emp(String matricule, String first, String last, String job, boolean tech, Specialty s, Team team) {
        Employee e = new Employee();
        e.setMatricule(matricule);
        e.setFirstName(first);
        e.setLastName(last);
        e.setEmail((first + "." + last).toLowerCase().replace(" ", "") + "@smartops.tn");
        e.setJobTitle(job);
        e.setHireDate(LocalDate.now().minusYears(3));
        e.setTechnician(tech);
        e.setSpecialty(s);
        e.setTeam(team);
        e.setDepartment(team.getDepartment());
        return employees.save(e);
    }

    private void account(String email, Role role, Employee e, String password) {
        User u = new User();
        u.setEmail(email);
        u.setRole(role);
        u.setEmployee(e);
        u.setPasswordHash(encoder.encode(password));
        users.save(u);
    }

    private void equip(String ref, String name, String type, Employee responsible) {
        Equipment e = new Equipment();
        e.setReference(ref);
        e.setName(name);
        e.setType(type);
        e.setStatus(AssetStatus.DISPONIBLE);
        e.setLocation("Dépôt El Mourouj");
        e.setResponsible(responsible);
        equipment.save(e);
    }

    private void vehicle(String plate, String model, String type, Team team) {
        Vehicle v = new Vehicle();
        v.setPlateNumber(plate);
        v.setBrandModel(model);
        v.setType(type);
        v.setStatus(AssetStatus.DISPONIBLE);
        v.setMileage(42000);
        v.setTeam(team);
        vehicles.save(v);
    }
}
