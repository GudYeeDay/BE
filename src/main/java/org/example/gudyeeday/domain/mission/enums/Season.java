package org.example.gudyeeday.domain.mission.enums;

import java.time.Month;

public enum Season {
    ALL,
    SPRING,
    SUMMER,
    FALL,
    WINTER;

    // 봄 3~5월, 여름 6~8월, 가을 9~11월, 겨울 12~2월
    public static Season from(Month month) {
        return switch (month) {
            case MARCH, APRIL, MAY -> SPRING;
            case JUNE, JULY, AUGUST -> SUMMER;
            case SEPTEMBER, OCTOBER, NOVEMBER -> FALL;
            case DECEMBER, JANUARY, FEBRUARY -> WINTER;
        };
    }
}
