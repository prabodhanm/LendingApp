package com.bus.comlending.listener;

import org.springframework.context.ApplicationEvent;
import org.springframework.mail.SimpleMailMessage;

import java.util.Locale;

@SuppressWarnings("serial")
public class OnRegistrationCompleteEvent extends ApplicationEvent {

    private final String appUrl;
    private final Locale locale;
    private final String user;
    private final String tokennumber;
    private final SimpleMailMessage simpleMailMessage;

    public OnRegistrationCompleteEvent(final String user, final Locale locale, final String appUrl,
                                       final String token, final SimpleMailMessage simpleMailMessage) {
        super(user);
        this.user = user;
        this.locale = locale;
        this.appUrl = appUrl;
        this.tokennumber = token;
        this.simpleMailMessage = simpleMailMessage;
    }

    //

    public String getAppUrl() {
        return appUrl;
    }

    public Locale getLocale() {
        return locale;
    }

    public String getUser() {
        return user;
    }

    public String getTokennumber() {return tokennumber;}

    public  SimpleMailMessage getSimpleMailMessage() {return this.simpleMailMessage;}

}
