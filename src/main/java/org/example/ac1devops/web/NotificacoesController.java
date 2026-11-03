package org.example.ac1devops.web;

import java.util.List;
import org.example.ac1devops.notificacoes.api.NotificacoesApi;
import org.example.ac1devops.notificacoes.api.NotificationDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notificacoes")
public class NotificacoesController {

    private final NotificacoesApi notificacoes;

    public NotificacoesController(NotificacoesApi notificacoes) {
        this.notificacoes = notificacoes;
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<List<NotificationDTO>> listByStudent(@PathVariable Long id) {
        return ResponseEntity.ok(notificacoes.listByStudent(id));
    }
}
