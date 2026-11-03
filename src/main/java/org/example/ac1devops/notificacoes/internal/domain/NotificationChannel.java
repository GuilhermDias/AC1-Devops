package org.example.ac1devops.notificacoes.internal.domain;

/** Porta para o canal externo de envio (e-mail, push). Pode falhar; quem chama trata como temporario. */
public interface NotificationChannel {

    void send(Notification notification);
}
