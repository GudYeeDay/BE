package org.example.gudyeeday.domain.mission.enums;

import java.time.DayOfWeek;

public enum DayType {
    ALL,
    WEEKDAY,
    WEEKEND;

    public static DayType from(DayOfWeek dayOfWeek) {
        return (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) ? WEEKEND : WEEKDAY;
    }
}
