package org.example.ac1devops.web;

import org.example.ac1devops.recomendacao.api.RecomendacaoApi;
import org.example.ac1devops.recomendacao.api.RecommendationsDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recomendacoes")
public class RecomendacaoController {

    private final RecomendacaoApi recomendacao;

    public RecomendacaoController(RecomendacaoApi recomendacao) {
        this.recomendacao = recomendacao;
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<RecommendationsDTO> getRecommendations(@PathVariable Long id) {
        return ResponseEntity.ok(recomendacao.getRecommendations(id));
    }
}
