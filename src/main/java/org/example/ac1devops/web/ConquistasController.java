package org.example.ac1devops.web;

import java.util.List;
import org.example.ac1devops.conquistas.api.BadgeDTO;
import org.example.ac1devops.conquistas.api.ConquistasApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/conquistas")
public class ConquistasController {

    private final ConquistasApi conquistas;

    public ConquistasController(ConquistasApi conquistas) {
        this.conquistas = conquistas;
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<List<BadgeDTO>> listBadges(@PathVariable Long id) {
        return ResponseEntity.ok(conquistas.listBadges(id));
    }
}
