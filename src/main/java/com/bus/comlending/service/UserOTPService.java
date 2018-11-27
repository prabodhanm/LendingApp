package com.bus.comlending.service;

import com.bus.comlending.domain.User;
import com.bus.comlending.domain.UserOTP;
import com.bus.comlending.repository.UserOTPRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;


@Service
@Transactional
public class UserOTPService {
    private final Logger log = LoggerFactory.getLogger(UserOTPService.class);
    private final UserOTPRepository userOTPRepository;

    public UserOTPService(UserOTPRepository userOTPRepository) {
        this.userOTPRepository = userOTPRepository;
    }

    public void AddOTPDetails(User user, String otpnumber){
        log.debug("Adding OTP details for user " + user.getLogin());
        //final String otpnumber = UUID.randomUUID().toString();


        UserOTP userOTP = new UserOTP();
        userOTP.setUserid(user.getLogin());
        userOTP.setOtpnumber(otpnumber);
        userOTP.setActive(true);
        userOTP.setCreation_date(Instant.now());
        userOTP.setModified_date(Instant.now());
        userOTPRepository.save(userOTP);
    }

    private long CheckTimeDiff(UserOTP userOTP){
        long timediff = 0;

        Instant cdate = userOTP.getCreation_date();
        LocalDateTime localCreatedDate = LocalDateTime.ofInstant(cdate,ZoneOffset.systemDefault());
        LocalDateTime curLocalDateTime = LocalDateTime.now();
        timediff = localCreatedDate.until(curLocalDateTime, ChronoUnit.MINUTES);
        return timediff;
    }

    public Optional<UserOTP> UpdateOTPDetails(String otpnumber){
        log.debug("OTP NUmber= " + otpnumber);

        Optional<UserOTP> userOTP = userOTPRepository.findByOTPNumber(otpnumber);

        UserOTP userOTP1 = new UserOTP();


        if(userOTP.isPresent()){
            if(CheckTimeDiff(userOTP.get()) < 2) {
                userOTP1.setUserid(userOTP.get().getUserid());
                userOTP1.setOtpnumber(otpnumber);
                userOTP1.setActive(false);
                userOTP1.setCreation_date(userOTP.get().getCreation_date());
                userOTP1.setModified_date(Instant.now());

                userOTPRepository.save(userOTP1);
            }
            else{
                //userOTP = null;
                /*userOTP1.setUserid(userOTP.get().getUserid());
                userOTP1.setOtpnumber("Expired");
                userOTP1.setActive(false);
                userOTP1.setCreation_date(userOTP.get().getCreation_date());
                userOTP1.setModified_date(Instant.now());*/

                userOTP.map(userOTP2 -> {
                    userOTP2.setOtpnumber("Expired");
                    return  userOTP2;
                });
            }
        }

        return userOTP;



//        return userOTPRepository.findByOTPNumber(otpnumber)
//            .ifPresent(userOTP -> {
//                userOTP.setOtpnumber(otpnumber);
//                userOTP.setActive(false);
//                userOTP.setModified_date(Instant.now());
//                log.debug("Changed Information for OTP: {}", userOTP);
//            });

//        return userOTPRepository.findByOTPNumber(otpnumber)
//            .map(userOTP -> {
//                userOTP.setOtpnumber(otpnumber);
//                userOTP.setActive(false);
//                userOTP.setModified_date(Instant.now());
//                log.debug("Changed Information for OTP: {}", userOTP);
//            })
//            .orElseThrow(() -> new UsernameNotFoundException("OTP Number " + otpnumber + " was not found in the database"));

    }
}
