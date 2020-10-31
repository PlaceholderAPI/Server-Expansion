package at.helpch.placeholderapi.expansion.server;

import at.helpch.placeholderapi.file.annotations.File;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Singleton;

@Singleton
@File(
        internalPath = "/config.yml",
        externalPath = "server/config.yml"
)
final class Config {

    @SerializedName("timeFormat")
    private TimeFormat timeFormat;

    @SerializedName("dateFormat")
    private DateFormat dateFormat;

    @SerializedName("tpsColor")
    private TpsColor tpsColor;

    TimeFormat getTimeFormat() {
        return this.timeFormat;
    }

    DateFormat getDateFormat() { return this.dateFormat; }

    TpsColor getTpsColor() { return this.tpsColor; }

    static final class TpsColor {

        private String low;
        private String medium;
        private String high;

        String getLow() { return this.low; }
        String getMedium() { return this.medium; }
        String getHigh() { return this.high; }

    }

    static final class DateFormat {

        private String format;

        String getFormat() { return this.format; }
    }

    static final class TimeFormat {

        private String second;
        private String seconds;
        private String minute;
        private String minutes;
        private String hour;
        private String hours;
        private String day;
        private String days;
        private String week;
        private String weeks;

        String getSecond() {
            return this.second;
        }

        String getSeconds() {
            return this.seconds;
        }

        String getMinute() {
            return this.minute;
        }

        String getMinutes() {
            return this.minutes;
        }

        String getHour() {
            return this.hour;
        }

        String getHours() {
            return this.hours;
        }

        String getDay() {
            return this.day;
        }

        String getDays() {
            return this.days;
        }

        String getWeek() {
            return this.week;
        }

        String getWeeks() {
            return this.weeks;
        }
    }

}
