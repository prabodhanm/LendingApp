package com.bus.comlending.listener;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class RegistrationListener implements ApplicationListener<OnRegistrationCompleteEvent> {
    @Autowired
    private MessageSource messages;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private Environment env;

    // API

    @Override
    public void onApplicationEvent(final OnRegistrationCompleteEvent event) {
        this.confirmRegistration(event);
    }


    private void confirmRegistration(final OnRegistrationCompleteEvent event) {
        final String user = event.getUser();


        final String token =  event.getTokennumber();
        System.out.println("Token:::"+token);
        //final SimpleMailMessage email = constructEmailMessage(event, user, token);
        final SimpleMailMessage email = event.getSimpleMailMessage();
            mailSender = getMailSender(mailSender);


        try {

            mailSender.send(email);
            System.out.println("Mail sent successfully....");
        }
        catch(Exception ex){
            System.out.println(ex.getMessage());
        }
    }

    //

    public JavaMailSender getMailSender(JavaMailSender mailSender) {
        String smtpServer = "smtp.gmail.com.";
        Integer smtpPort = 587; //465 or 587
        String smtpUsername = "pmestry007";
        String smtpPassword = "parv@n!21";
        if (mailSender == null) {
            final JavaMailSenderImpl impl = new JavaMailSenderImpl();
            impl.setHost(smtpServer);
            impl.setPort(smtpPort);

            Properties props = new Properties();



            impl.setJavaMailProperties(props);
            final Properties properties = new Properties();
            if (StringUtils.isNotEmpty(smtpUsername)) {
                // Use authentication
                //properties.setProperty("mail.smtp.auth", "true");
                impl.setUsername(smtpUsername);
                impl.setPassword(smtpPassword);

                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.from", "pmestry007@gmail.com");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.port", "587");
                props.setProperty("mail.debug", "true");


            }
            /*if (smtpUseTLS) {
                properties.setProperty("mail.smtp.starttls.enable", "true");
            }*/
            impl.setJavaMailProperties(props);
            mailSender = impl;
        }
        return mailSender;
    }

    private final SimpleMailMessage constructEmailMessage(final OnRegistrationCompleteEvent event, final String user,
                                                          final String token) {
        //final String recipientAddress = "manojkumar.kvns@gmail.com";
        final String recipientAddress = "pmestry007@gmail.com";
        final String subject = "Registration Confirmation";
        final String confirmationUrl = event.getAppUrl() + "/registrationConfirm?token=" + token;
        final String message = "You registered successfully. We will send you a confirmation message to your email account";
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipientAddress);
        email.setSubject(subject);
        email.setText(message + " \r\n" + confirmationUrl);
        //email.setFrom(env.getProperty("support.email"));
        email.setFrom("pmestry007@gmail.com");
        return email;
    }

}
