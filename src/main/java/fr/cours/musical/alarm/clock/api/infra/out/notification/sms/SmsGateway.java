package fr.cours.musical.alarm.clock.api.infra.out.notification.sms;

public interface SmsGateway {

    /** Returns the gateway message id. */
    String sendText(String phoneNumber, String text);
}
