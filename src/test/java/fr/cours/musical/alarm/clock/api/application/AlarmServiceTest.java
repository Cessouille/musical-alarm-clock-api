package fr.cours.musical.alarm.clock.api.application;

import fr.cours.musical.alarm.clock.api.domain.exception.AlarmDeliveryException;
import fr.cours.musical.alarm.clock.api.domain.exception.UserNotFoundException;
import fr.cours.musical.alarm.clock.api.domain.exception.UserPreferencesUnavailableException;
import fr.cours.musical.alarm.clock.api.domain.model.AlarmResult;
import fr.cours.musical.alarm.clock.api.domain.model.AlarmSlot;
import fr.cours.musical.alarm.clock.api.domain.model.ChannelType;
import fr.cours.musical.alarm.clock.api.domain.model.Contact;
import fr.cours.musical.alarm.clock.api.domain.model.Track;
import fr.cours.musical.alarm.clock.api.domain.model.UserPreferences;
import fr.cours.musical.alarm.clock.api.domain.model.WakeUpMessage;
import fr.cours.musical.alarm.clock.api.domain.model.WeatherType;
import fr.cours.musical.alarm.clock.api.domain.port.out.UserPreferencesProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlarmServiceTest {

    private static final Track TRACK = new Track("Walking on Sunshine", "Katrina & The Waves");
    private static final Contact CONTACT = new Contact(Map.of(ChannelType.EMAIL, "alice@example.invalid", ChannelType.SMS, "+33600000001",
            ChannelType.PUSH, "device-alice"));
    private static final UserPreferences ALICE = new UserPreferences("alice",
            Map.of(new AlarmSlot(DayOfWeek.MONDAY, WeatherType.SUN), "Walking on Sunshine"),
            "Here Comes the Sun", ChannelType.EMAIL, CONTACT);

    @Mock
    private UserPreferencesProvider userPreferencesProvider;

    @Mock
    private TrackSelector trackSelector;

    @Mock
    private NotificationDispatcher notificationDispatcher;

    private AlarmService alarmService;

    @BeforeEach
    void setUp() {
        alarmService = new AlarmService(userPreferencesProvider, trackSelector, notificationDispatcher);
    }

    @Test
    void triggerAlarm_sendsWeatherTrackOnPreferredChannel_notDegraded() {
        when(userPreferencesProvider.findByUserId("alice")).thenReturn(Optional.of(ALICE));
        when(trackSelector.select("Walking on Sunshine", DayOfWeek.MONDAY))
                .thenReturn(new TrackSelector.Selection(TRACK, false));
        WakeUpMessage expected = new WakeUpMessage("alice", DayOfWeek.MONDAY, WeatherType.SUN, TRACK, CONTACT);
        when(notificationDispatcher.dispatch(expected, ChannelType.EMAIL))
                .thenReturn(new NotificationDispatcher.Delivery(ChannelType.EMAIL, false));

        AlarmResult result = alarmService.triggerAlarm("alice", DayOfWeek.MONDAY, WeatherType.SUN);

        assertThat(result).isEqualTo(new AlarmResult("alice", TRACK, ChannelType.EMAIL, false));
    }

    @Test
    void triggerAlarm_looksUpTheUserFallbackTrack_whenWeatherIsNotMapped() {
        when(userPreferencesProvider.findByUserId("alice")).thenReturn(Optional.of(ALICE));
        when(trackSelector.select("Here Comes the Sun", DayOfWeek.MONDAY))
                .thenReturn(new TrackSelector.Selection(TRACK, false));
        when(notificationDispatcher.dispatch(new WakeUpMessage("alice", DayOfWeek.MONDAY, WeatherType.SNOW, TRACK, CONTACT),
                ChannelType.EMAIL)).thenReturn(new NotificationDispatcher.Delivery(ChannelType.EMAIL, false));

        AlarmResult result = alarmService.triggerAlarm("alice", DayOfWeek.MONDAY, WeatherType.SNOW);

        assertThat(result.track()).isEqualTo(TRACK);
    }

    @Test
    void triggerAlarm_looksUpTheUserFallbackTrack_whenOnlyTheDayDiffers() {
        when(userPreferencesProvider.findByUserId("alice")).thenReturn(Optional.of(ALICE));
        when(trackSelector.select("Here Comes the Sun", DayOfWeek.TUESDAY))
                .thenReturn(new TrackSelector.Selection(TRACK, false));
        when(notificationDispatcher.dispatch(new WakeUpMessage("alice", DayOfWeek.TUESDAY, WeatherType.SUN, TRACK, CONTACT),
                ChannelType.EMAIL)).thenReturn(new NotificationDispatcher.Delivery(ChannelType.EMAIL, false));

        alarmService.triggerAlarm("alice", DayOfWeek.TUESDAY, WeatherType.SUN);

        verify(trackSelector).select("Here Comes the Sun", DayOfWeek.TUESDAY);
    }

    @Test
    void triggerAlarm_isDegraded_whenMusicFellBack() {
        when(userPreferencesProvider.findByUserId("alice")).thenReturn(Optional.of(ALICE));
        when(trackSelector.select("Walking on Sunshine", DayOfWeek.MONDAY))
                .thenReturn(new TrackSelector.Selection(TRACK, true));
        when(notificationDispatcher.dispatch(new WakeUpMessage("alice", DayOfWeek.MONDAY, WeatherType.SUN, TRACK, CONTACT),
                ChannelType.EMAIL)).thenReturn(new NotificationDispatcher.Delivery(ChannelType.EMAIL, false));

        assertThat(alarmService.triggerAlarm("alice", DayOfWeek.MONDAY, WeatherType.SUN).degraded()).isTrue();
    }

    @Test
    void triggerAlarm_isDegradedAndReportsActualChannel_whenChannelFellBack() {
        when(userPreferencesProvider.findByUserId("alice")).thenReturn(Optional.of(ALICE));
        when(trackSelector.select("Walking on Sunshine", DayOfWeek.MONDAY))
                .thenReturn(new TrackSelector.Selection(TRACK, false));
        when(notificationDispatcher.dispatch(new WakeUpMessage("alice", DayOfWeek.MONDAY, WeatherType.SUN, TRACK, CONTACT),
                ChannelType.EMAIL)).thenReturn(new NotificationDispatcher.Delivery(ChannelType.PUSH, true));

        AlarmResult result = alarmService.triggerAlarm("alice", DayOfWeek.MONDAY, WeatherType.SUN);

        assertThat(result.channel()).isEqualTo(ChannelType.PUSH);
        assertThat(result.degraded()).isTrue();
    }

    @Test
    void triggerAlarm_throwsUserNotFound_andDoesNothingElse_whenUserIsUnknown() {
        when(userPreferencesProvider.findByUserId("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alarmService.triggerAlarm("ghost", DayOfWeek.MONDAY, WeatherType.SUN))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("ghost");

        verifyNoInteractions(trackSelector, notificationDispatcher);
    }

    @Test
    void triggerAlarm_propagatesDeliveryFailure() {
        when(userPreferencesProvider.findByUserId("alice")).thenReturn(Optional.of(ALICE));
        when(trackSelector.select("Walking on Sunshine", DayOfWeek.MONDAY))
                .thenReturn(new TrackSelector.Selection(TRACK, false));
        when(notificationDispatcher.dispatch(new WakeUpMessage("alice", DayOfWeek.MONDAY, WeatherType.SUN, TRACK, CONTACT),
                ChannelType.EMAIL)).thenThrow(new AlarmDeliveryException("alice", List.of(ChannelType.EMAIL)));

        assertThatThrownBy(() -> alarmService.triggerAlarm("alice", DayOfWeek.MONDAY, WeatherType.SUN))
                .isInstanceOf(AlarmDeliveryException.class);
    }

    @Test
    void triggerAlarm_throwsPreferencesUnavailable_andDoesNothingElse_whenTheUserServiceFails() {
        when(userPreferencesProvider.findByUserId("alice")).thenThrow(new IllegalStateException("user service down"));

        assertThatThrownBy(() -> alarmService.triggerAlarm("alice", DayOfWeek.MONDAY, WeatherType.SUN))
                .isInstanceOf(UserPreferencesUnavailableException.class)
                .hasMessageContaining("alice")
                .hasCauseInstanceOf(IllegalStateException.class);

        verifyNoInteractions(trackSelector, notificationDispatcher);
    }
}
