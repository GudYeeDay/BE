package org.example.gudyeeday.domain.calender.dto.response;

import java.util.List;

public record CalenderResponse(
        Long counts,
        List<CalenderRecordResponse> calenderRecordResponseList
) {
    public static CalenderResponse of(Long counts, List<CalenderRecordResponse> calenderRecordResponseList) {
        return new CalenderResponse(
                counts,
                calenderRecordResponseList
        );
    }
}
