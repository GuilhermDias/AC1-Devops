package org.example.ac1devops.web;

import java.util.List;
import org.example.ac1devops.cursos.api.CourseCompletionDTO;
import org.example.ac1devops.cursos.api.CourseEventDTO;
import org.example.ac1devops.cursos.api.CursosApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cursos")
public class CursosController {

    private final CursosApi cursos;

    public CursosController(CursosApi cursos) {
        this.cursos = cursos;
    }

    @PostMapping("/{courseId}/alunos/{studentId}/conclusao")
    public ResponseEntity<CourseCompletionDTO> completeCourse(@PathVariable String courseId, @PathVariable Long studentId) {
        CourseCompletionDTO completion = cursos.completeCourse(courseId, studentId);
        return ResponseEntity.status(completion.alreadyCompleted() ? HttpStatus.OK : HttpStatus.CREATED).body(completion);
    }

    @GetMapping("/eventos")
    public ResponseEntity<List<CourseEventDTO>> listEvents() {
        return ResponseEntity.ok(cursos.listEvents());
    }

    @PostMapping("/eventos/{id}/reenvio")
    public ResponseEntity<Void> requeueEvent(@PathVariable Long id) {
        cursos.requeueEvent(id);
        return ResponseEntity.accepted().build();
    }
}
