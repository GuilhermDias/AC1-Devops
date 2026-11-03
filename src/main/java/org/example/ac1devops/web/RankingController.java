package org.example.ac1devops.web;

import java.util.List;
import org.example.ac1devops.ranking.api.RankingApi;
import org.example.ac1devops.ranking.api.RankingEntryDTO;
import org.example.ac1devops.ranking.api.RankingPageDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ranking")
public class RankingController {

    private final RankingApi ranking;

    public RankingController(RankingApi ranking) {
        this.ranking = ranking;
    }

    @GetMapping
    public ResponseEntity<RankingPageDTO> getRanking(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ranking.getRanking(page, size));
    }

    @GetMapping("/top")
    public ResponseEntity<List<RankingEntryDTO>> getTop(@RequestParam(defaultValue = "10") int n) {
        return ResponseEntity.ok(ranking.getTop(n));
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<RankingEntryDTO> getPosition(@PathVariable Long id) {
        return ResponseEntity.ok(ranking.getPosition(id));
    }
}
