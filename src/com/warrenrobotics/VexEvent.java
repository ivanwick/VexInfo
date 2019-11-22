package com.warrenrobotics;

import java.time.LocalDate;
import java.util.List;

/**
 * Event Information
 */
public class VexEvent {
    private String season;
    private String eventName;
    private LocalDate eventDate;
    private List<Team> teamList;
    private String sku;

    public VexEvent(String season, String eventName, LocalDate eventDate, List<Team> teamList, String sku) {
        this.season = season;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.teamList = teamList;
        this.sku = sku;
    }

    public String getSeason() {
        return season;
    }

    public String getEventName() {
        return eventName;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public List<Team> getTeamList() {
        return teamList;
    }

    public String getSku() {
        return sku;
    }
}
