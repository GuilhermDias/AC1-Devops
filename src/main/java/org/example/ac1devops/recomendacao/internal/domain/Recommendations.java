package org.example.ac1devops.recomendacao.internal.domain;

import java.util.List;

public record Recommendations(List<CourseRecommendation> courses, boolean degraded) {
}
