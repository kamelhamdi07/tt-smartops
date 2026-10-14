package tn.tt.smartops.dashboard;

import tn.tt.smartops.auth.Role;

import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() {}

    /** tone : neutral | info | success | warning | danger, utilisé par le frontend pour la couleur. */
    public record Kpi(String key, String label, long value, String tone, String link) {}

    public record ChartPoint(String label, long value) {}

    public record Chart(String key, String title, List<ChartPoint> points) {}

    public record DashboardResponse(Role role, List<Kpi> kpis, List<Chart> charts) {}
}
