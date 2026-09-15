package org.example.ac1devops.web;

import java.util.List;
import org.example.ac1devops.dto.CreateStudentRequestDTO;
import org.example.ac1devops.dto.ReceiveXpRequestDTO;
import org.example.ac1devops.dto.StudentResponseDTO;
import org.example.ac1devops.service.StudentService;
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

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    public ResponseEntity<StudentResponseDTO> createStudent(@RequestBody CreateStudentRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.createStudent(request));
    }

    @PostMapping("/{id}/xp")
    public ResponseEntity<StudentResponseDTO> receiveXp(
            @PathVariable Long id, @RequestBody ReceiveXpRequestDTO request) {
        return ResponseEntity.ok(studentService.receiveXp(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDTO> getStudent(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudent(id));
    }

    @GetMapping
    public ResponseEntity<List<StudentResponseDTO>> listStudents() {
        return ResponseEntity.ok(studentService.listStudents());
    }
}
