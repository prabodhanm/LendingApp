package com.bus.comlending.repository;

import com.bus.comlending.domain.UserOTP;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserOTPRepository extends CrudRepository<UserOTP, Long> {
//    List<UserOTP> findByUser(String userid);
//    List<UserOTP> findByTokenDateBefore(LocalDate localDate);
    Optional<UserOTP> findByOTPNumber(String otpnumber);
}
