package fr.cours.musical.alarm.clock.api.infra.out.notification.sms;

public interface SmsGateway {

    String sendText(String phoneNumber, String text);
}
