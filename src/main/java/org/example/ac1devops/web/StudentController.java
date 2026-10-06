package org.example.ac1devops.web;

import java.util.List;
import org.example.ac1devops.progression.api.CreateStudentRequestDTO;
import org.example.ac1devops.progression.api.ReceiveXpRequestDTO;
import org.example.ac1devops.progression.api.StudentProgressionApi;
import org.example.ac1devops.progression.api.StudentResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentProgressionApi studentProgression;

    public StudentController(StudentProgressionApi studentProgression) {
        this.studentProgression = studentProgression;
    }

    @PostMapping
    public ResponseEntity<StudentResponseDTO> createStudent(@RequestBody CreateStudentRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentProgression.createStudent(request));
    }

    @PostMapping("/{id}/xp")
    public ResponseEntity<StudentResponseDTO> receiveXp(
            @PathVariable Long id, @RequestBody ReceiveXpRequestDTO request) {
        return ResponseEntity.ok(studentProgression.receiveXp(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDTO> getStudent(@PathVariable Long id) {
        return ResponseEntity.ok(studentProgression.getStudent(id));
    }

    @GetMapping
    public ResponseEntity<List<StudentResponseDTO>> listStudents() {
        return ResponseEntity.ok(studentProgression.listStudents());
    }
}
