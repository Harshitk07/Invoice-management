package ui.interfaces;

import ui.DashboardController;

public interface Navigable {
    void setDashboard(DashboardController dashboard);

    default void onNavigateTo() {}
    default void onNavigateFrom() {}
}
