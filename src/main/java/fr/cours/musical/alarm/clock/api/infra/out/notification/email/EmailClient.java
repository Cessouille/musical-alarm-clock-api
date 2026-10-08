package fr.cours.musical.alarm.clock.api.infra.out.notification.email;

public interface EmailClient {

    void sendEmail(String to, String subject, String htmlBody);
}
