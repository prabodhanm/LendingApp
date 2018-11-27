package com.bus.comlending.security;

import com.bus.comlending.domain.User;
import com.bus.comlending.listener.OnRegistrationCompleteEvent;
import com.bus.comlending.listener.RegistrationListener;
import com.bus.comlending.repository.UserOTPRepository;
import com.bus.comlending.repository.UserRepository;
import com.bus.comlending.service.UserOTPService;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Authenticate a user from the database.
 */
@Component("userDetailsService")
public class DomainUserDetailsService implements UserDetailsService {

    private final Logger log = LoggerFactory.getLogger(DomainUserDetailsService.class);

    private final UserRepository userRepository;

    private final UserOTPRepository userOTPRepository;

    public DomainUserDetailsService(UserRepository userRepository, UserOTPRepository userOTPRepository) {
        this.userRepository = userRepository;
        this.userOTPRepository = userOTPRepository;
    }

//    public DomainUserDetailsService(UserRepository userRepository) {
//        this.userRepository = userRepository;
//    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(final String login) {
        log.debug("Authenticating {}", login);

        if (new EmailValidator().isValid(login, null)) {
            return userRepository.findOneWithAuthoritiesByEmail(login)
                .map(user -> createSpringSecurityUser(login, user))
                .orElseThrow(() -> new UsernameNotFoundException("User with email " + login + " was not found in the database"));
        }

        String lowercaseLogin = login.toLowerCase(Locale.ENGLISH);
        return userRepository.findOneWithAuthoritiesByLogin(lowercaseLogin)
            .map(user -> createSpringSecurityUser(lowercaseLogin, user))
            .orElseThrow(() -> new UsernameNotFoundException("User " + lowercaseLogin + " was not found in the database"));

    }

    private final SimpleMailMessage constructEmailMessage(final String user,
                                                          final String token) {
        //final String recipientAddress = "manojkumar.kvns@gmail.com";
        final String recipientAddress = "pmestry007@gmail.com";
        final String subject = "OTP Sent";
        //final String confirmationUrl = appurl + "/registrationConfirm?token=" + token;
        final String message = "OTP Number " + token + " for user " + user + " sent successfully.";
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipientAddress);
        email.setSubject(subject);
        email.setText(message);
        //email.setFrom(env.getProperty("support.email"));
        email.setFrom("pmestry007@gmail.com");
        return email;
    }

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(String lowercaseLogin, User user) {
        if (!user.getActivated()) {
            throw new UserNotActivatedException("User " + lowercaseLogin + " was not activated");
        }
        //Insert details into otp table

        log.debug("Creating spring security user");

        Random rnd = new Random();
        int otpnumber = 100000 + rnd.nextInt(900000);

        UserOTPService userOTPService = new UserOTPService(userOTPRepository);
        userOTPService.AddOTPDetails(user, String.valueOf(otpnumber));

        //Send mail
        RegistrationListener reglistner = new RegistrationListener();
        reglistner.onApplicationEvent(new OnRegistrationCompleteEvent(lowercaseLogin,null,
            "", String.valueOf(otpnumber),
            constructEmailMessage(lowercaseLogin,String.valueOf(otpnumber))));


        List<GrantedAuthority> grantedAuthorities = user.getAuthorities().stream()
            .map(authority -> new SimpleGrantedAuthority(authority.getName()))
            .collect(Collectors.toList());
        return new org.springframework.security.core.userdetails.User(user.getLogin(),
            user.getPassword(),
            grantedAuthorities);
    }
}
