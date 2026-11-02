package org.example.ac1devops.recomendacao.internal.domain;

public class RecommendationUnavailableException extends RuntimeException {

    public RecommendationUnavailableException(Throwable cause) {
        super("Recomendacoes indisponiveis", cause);
    }
}
