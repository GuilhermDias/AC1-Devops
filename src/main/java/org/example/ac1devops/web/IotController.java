package org.example.ac1devops.web;

import java.util.List;
import org.example.ac1devops.iot.api.DeviceEventDTO;
import org.example.ac1devops.iot.api.IotApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/iot")
public class IotController {

    private final IotApi iot;

    public IotController(IotApi iot) {
        this.iot = iot;
    }

    @GetMapping("/eventos")
    public ResponseEntity<List<DeviceEventDTO>> listEvents() {
        return ResponseEntity.ok(iot.listEvents());
    }
}
