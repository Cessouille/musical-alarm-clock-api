package fr.cours.musical.alarm.clock.api.infra.out.notification;

import fr.cours.musical.alarm.clock.api.domain.model.WakeUpMessage;
import fr.cours.musical.alarm.clock.api.domain.model.WeatherType;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;

public final class WakeUpTexts {

    private WakeUpTexts() {
    }

    public static String dayName(DayOfWeek day) {
        return day.getDisplayName(TextStyle.FULL, Locale.FRENCH);
    }

    public static String subject(WakeUpMessage message) {
        return "Réveil musical – " + dayName(message.dayOfWeek());
    }

    public static String body(WakeUpMessage message) {
        return "Bonjour ! Ce %s, météo : %s. Votre morceau du réveil : %s – %s."
                .formatted(dayName(message.dayOfWeek()),
                        weatherLabel(message.weather()),
                        message.track().title(),
                        message.track().artist());
    }

    private static String weatherLabel(WeatherType weather) {
        return switch (weather) {
            case SUN -> "soleil";
            case RAIN -> "pluie";
            case SNOW -> "neige";
            case CLOUDY -> "nuageux";
        };
    }
}
