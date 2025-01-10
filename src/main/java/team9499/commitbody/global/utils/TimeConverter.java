package team9499.commitbody.global.utils;

import lombok.extern.slf4j.Slf4j;
import team9499.commitbody.domain.record.domain.Record;

import java.time.*;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

import static team9499.commitbody.global.constants.Delimiter.*;

@Slf4j
public class TimeConverter {

    private static final int SEC = 60;
    private static final int MIN = 60;
    private static final int HOUR = 24;
    private static final int DAY = 30;
    private static final int MONTH = 12;
    public final static String OPEN_PARENTHESIS = ".(";
    public final static String CLOSE_PARENTHESIS = ")";

    public static String converter(LocalDateTime updatedAt) {
        LocalDateTime now = LocalDateTime.now();

        long diffTime = updatedAt.until(now, ChronoUnit.SECONDS); // now보다 이후면 +, 전이면 -

        if (diffTime < SEC){
            return diffTime + "초전";
        }
        diffTime = diffTime / SEC;
        if (diffTime < MIN) {
            return diffTime + "분 전";
        }
        diffTime = diffTime / MIN;
        if (diffTime < HOUR) {
            return diffTime + "시간 전";
        }
        diffTime = diffTime / HOUR;
        if (diffTime < DAY) {
            return diffTime + "일 전";
        }
        diffTime = diffTime / DAY;
        if (diffTime < MONTH) {
            return diffTime + "개월 전";
        }

        diffTime = diffTime / MONTH;
        return diffTime + "년 전";
    }

    public static LocalDateTime getStartOfMonth(int year, int month) {
        return LocalDateTime.of(year, month, 1, 0, 0);
    }

    public static LocalDateTime getEndOfMonth(int year, int month) {
        return LocalDate.of(year, month, 1)
                .with(TemporalAdjusters.lastDayOfMonth())
                .atTime(23, 59, 59);
    }

    /*
   LocalDateTime을 2024.08.25.(금) 형식으로변경
    */
    public static String converterTime(Record record) {
        LocalDateTime startTime = record.getStartTime();
        String localDate = getLocalDateToString(startTime);
        DayOfWeek dayOfWeek = startTime.getDayOfWeek();
        String displayName = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREA);
        return localDate +OPEN_PARENTHESIS + displayName + CLOSE_PARENTHESIS;
    }

    private static String getLocalDateToString(LocalDateTime startTime) {
        return LocalDate.of(startTime.getYear(),
                startTime.getMonth(),
                startTime.getDayOfMonth()).toString().replace(DASH_CH, COMMA_CH
        );
    }

    public static LocalDateTime startOfWeek(){
        LocalDate today = getLocalDateNow();
        return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).atStartOfDay();
    }

    public static LocalDateTime endOfWeek(){
        LocalDate today = getLocalDateNow();
        return today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)).atTime(23, 59, 59);
    }


    /*
 운동 시작 시간과 운동 끝시간을 18:14~20:15 로변환 하는메서드
  */
    public static String converterDurationTime(Record record) {
        StringBuilder sb = new StringBuilder();
        LocalDateTime startTime = record.getStartTime();
        LocalDateTime endTime = record.getEndTime();

        return sb.append(startTime.getHour()).append(COLON_SEPARATOR)
                .append(startTime.getMinute()).append(APPROXIMATION)
                .append(endTime.getHour()).append(COLON_SEPARATOR)
                .append(endTime.getMinute()).toString();
    }


    public static Duration calculateMaxAge() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfDay = LocalDateTime.of(now.toLocalDate(), LocalTime.of(23, 59, 58));
        long secondsUntilEndOfDay = ChronoUnit.SECONDS.between(now, endOfDay);
        return Duration.ofSeconds(secondsUntilEndOfDay);
    }

    private static LocalDate getLocalDateNow() {
        return LocalDate.now();
    }
}
