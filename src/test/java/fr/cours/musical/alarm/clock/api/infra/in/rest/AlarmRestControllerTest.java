package fr.cours.musical.alarm.clock.api.infra.in.rest;

import fr.cours.musical.alarm.clock.api.domain.exception.AlarmDeliveryException;
import fr.cours.musical.alarm.clock.api.domain.exception.UserNotFoundException;
import fr.cours.musical.alarm.clock.api.domain.model.AlarmResult;
import fr.cours.musical.alarm.clock.api.domain.model.ChannelType;
import fr.cours.musical.alarm.clock.api.domain.model.Track;
import fr.cours.musical.alarm.clock.api.domain.model.WeatherType;
import fr.cours.musical.alarm.clock.api.domain.port.in.AlarmUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.DayOfWeek;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlarmRestController.class)
class AlarmRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlarmUseCase alarmUseCase;

    private ResultActions trigger(String json) throws Exception {
        return mockMvc.perform(post("/alarms/trigger").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void trigger_returns200WithResult_whenAlarmIsSent() throws Exception {
        when(alarmUseCase.triggerAlarm("alice", DayOfWeek.MONDAY, WeatherType.SOLEIL)).thenReturn(
                new AlarmResult("alice", new Track("Walking on Sunshine", "Katrina & The Waves"),
                        ChannelType.EMAIL, false));

        trigger("""
                {"userId":"alice","dayOfWeek":"MONDAY","weather":"SOLEIL"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("alice"))
                .andExpect(jsonPath("$.track.title").value("Walking on Sunshine"))
                .andExpect(jsonPath("$.track.artist").value("Katrina & The Waves"))
                .andExpect(jsonPath("$.channel").value("EMAIL"))
                .andExpect(jsonPath("$.degraded").value(false));
    }

    @Test
    void trigger_returns404_whenUserIsUnknown() throws Exception {
        when(alarmUseCase.triggerAlarm("ghost", DayOfWeek.MONDAY, WeatherType.PLUIE))
                .thenThrow(new UserNotFoundException("ghost"));

        trigger("""
                {"userId":"ghost","dayOfWeek":"MONDAY","weather":"PLUIE"}""")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void trigger_returns503_whenNoChannelCouldDeliver() throws Exception {
        when(alarmUseCase.triggerAlarm("alice", DayOfWeek.MONDAY, WeatherType.SOLEIL))
                .thenThrow(new AlarmDeliveryException("alice", List.of(ChannelType.EMAIL)));

        trigger("""
                {"userId":"alice","dayOfWeek":"MONDAY","weather":"SOLEIL"}""")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void trigger_returns400_whenUserIdIsBlank() throws Exception {
        trigger("""
                {"userId":"  ","dayOfWeek":"MONDAY","weather":"SOLEIL"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(alarmUseCase);
    }

    @Test
    void trigger_returns400_whenAFieldIsMissing() throws Exception {
        trigger("""
                {"userId":"alice","weather":"SOLEIL"}""")
                .andExpect(status().isBadRequest());
        trigger("""
                {"userId":"alice","dayOfWeek":"MONDAY"}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    void trigger_returns400_whenWeatherOrDayIsOutsideTheEnumeration() throws Exception {
        trigger("""
                {"userId":"alice","dayOfWeek":"MONDAY","weather":"BROUILLARD"}""")
                .andExpect(status().isBadRequest());
        trigger("""
                {"userId":"alice","dayOfWeek":"FUNDAY","weather":"SOLEIL"}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    void trigger_returns400_whenBodyIsNotJson() throws Exception {
        trigger("not json").andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void trigger_returns405_whenMethodIsNotPost() throws Exception {
        mockMvc.perform(get("/alarms/trigger"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void trigger_returns415_whenContentTypeIsNotJson() throws Exception {
        mockMvc.perform(post("/alarms/trigger").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void unknownPath_returns404_notAServerError() throws Exception {
        mockMvc.perform(get("/nothing-here"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void trigger_returns500WithoutLeakingDetails_onUnexpectedError() throws Exception {
        when(alarmUseCase.triggerAlarm("alice", DayOfWeek.MONDAY, WeatherType.SOLEIL))
                .thenThrow(new IllegalStateException("secret internals"));

        trigger("""
                {"userId":"alice","dayOfWeek":"MONDAY","weather":"SOLEIL"}""")
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected server error"));
    }
}
