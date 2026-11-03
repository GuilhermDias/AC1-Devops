package org.example.ac1devops.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.assertj.MockMvcTester.create;

import java.time.Instant;
import java.util.List;
import org.example.ac1devops.iot.api.DeviceEventDTO;
import org.example.ac1devops.iot.api.IotApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IotController.class)
class IotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IotApi iot;

    @Test
    void deveListarOsEventosRecebidos() {
        Instant now = Instant.parse("2026-11-03T12:00:00Z");
        when(iot.listEvents()).thenReturn(List.of(new DeviceEventDTO(
                "e1", "sensor-01", "PRESENCA", 7L, "ci-cd", now, now, "ACCEPTED", null, 2)));

        create(mockMvc).get().uri("/iot/eventos")
                .exchange()
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].duplicatesDiscarded")
                .isEqualTo(2);
    }
}
