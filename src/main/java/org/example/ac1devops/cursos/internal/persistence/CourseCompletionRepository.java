package org.example.ac1devops.cursos.internal.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseCompletionRepository extends JpaRepository<CourseCompletionEntity, Long> {

    Optional<CourseCompletionEntity> findByStudentIdAndCourseId(Long studentId, String courseId);

    long countByStudentIdAndCourseId(Long studentId, String courseId);
}
