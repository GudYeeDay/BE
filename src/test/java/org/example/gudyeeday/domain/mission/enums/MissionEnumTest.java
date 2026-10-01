package org.example.gudyeeday.domain.mission.enums;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.DayOfWeek;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class MissionEnumTest {

    @ParameterizedTest
    @CsvSource({
            "MONDAY, WEEKDAY", "FRIDAY, WEEKDAY",
            "SATURDAY, WEEKEND", "SUNDAY, WEEKEND"
    })
    void 요일_구분(DayOfWeek dayOfWeek, DayType expected) {
        assertThat(DayType.from(dayOfWeek)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "MARCH, SPRING", "MAY, SPRING",
            "JUNE, SUMMER", "AUGUST, SUMMER",
            "SEPTEMBER, FALL", "NOVEMBER, FALL",
            "DECEMBER, WINTER", "JANUARY, WINTER", "FEBRUARY, WINTER"
    })
    void 계절_구분(Month month, Season expected) {
        assertThat(Season.from(month)).isEqualTo(expected);
    }
}
