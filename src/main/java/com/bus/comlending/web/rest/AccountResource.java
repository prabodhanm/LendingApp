package com.bus.comlending.web.rest;

import com.bus.comlending.domain.UserOTP;
import com.bus.comlending.listener.OnRegistrationCompleteEvent;
import com.bus.comlending.listener.RegistrationListener;
import com.bus.comlending.repository.UserOTPRepository;
import com.bus.comlending.service.UserOTPService;
import com.codahale.metrics.annotation.Timed;

import com.bus.comlending.domain.PersistentToken;
import com.bus.comlending.repository.PersistentTokenRepository;
import com.bus.comlending.domain.User;
import com.bus.comlending.repository.UserRepository;
import com.bus.comlending.security.SecurityUtils;
import com.bus.comlending.service.MailService;
import com.bus.comlending.service.UserService;
import com.bus.comlending.service.dto.PasswordChangeDTO;
import com.bus.comlending.service.dto.UserDTO;
import com.bus.comlending.web.rest.errors.*;
import com.bus.comlending.web.rest.vm.KeyAndPasswordVM;
import com.bus.comlending.web.rest.vm.ManagedUserVM;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;


/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
public class AccountResource {

    private final Logger log = LoggerFactory.getLogger(AccountResource.class);

    private final UserRepository userRepository;

    private final UserService userService;

    private final MailService mailService;

    private final PersistentTokenRepository persistentTokenRepository;

    private final UserOTPRepository userOTPRepository;

    public AccountResource(UserRepository userRepository, UserService userService, MailService mailService,
                           PersistentTokenRepository persistentTokenRepository, UserOTPRepository userOTPRepository) {

        this.userRepository = userRepository;
        this.userService = userService;
        this.mailService = mailService;
        this.persistentTokenRepository = persistentTokenRepository;
        this.userOTPRepository = userOTPRepository;
    }

    /**
     * POST  /register : register the user.
     *
     * @param managedUserVM the managed user View Model
     * @throws InvalidPasswordException 400 (Bad Request) if the password is incorrect
     * @throws EmailAlreadyUsedException 400 (Bad Request) if the email is already used
     * @throws LoginAlreadyUsedException 400 (Bad Request) if the login is already used
     */
    @PostMapping("/register")
    @Timed
    @ResponseStatus(HttpStatus.CREATED)
    public void registerAccount(@Valid @RequestBody ManagedUserVM managedUserVM, HttpServletRequest request) {
        if (!checkPasswordLength(managedUserVM.getPassword())) {
            throw new InvalidPasswordException();
        }
        User user = userService.registerUser(managedUserVM, managedUserVM.getPassword());
        //mailService.sendActivationEmail(user);

        Map<String, String> mapMail = new HashMap<String,String>();

        mapMail.put("subject","Registration Confirmation");
        mapMail.put("url", request.getRequestURL().toString() + "/registrationConfirm?token=" + user.getTokennumber());
        mapMail.put("message","You registered successfully. We will send you a confirmation message to your email account");
        mapMail.put("email",managedUserVM.getEmail().toString());


        RegistrationListener reglistner = new RegistrationListener();
        reglistner.onApplicationEvent(new OnRegistrationCompleteEvent(managedUserVM.getLogin(),request.getLocale(),
            request.getRequestURL().toString(), user.getTokennumber(),
            constructEmailMessage(request.getRequestURL().toString(),managedUserVM.getLogin(),user.getTokennumber(), mapMail)));
    }

    private final SimpleMailMessage constructEmailMessage(String appurl, final String user,
                                                          final String token, final Map<String, String> mapMail) {
        final String recipientAddress = mapMail.get("email").toString();
        final String subject = mapMail.get("subject").toString();
        final String confirmationUrl = mapMail.get("url").toString();
        final String message = mapMail.get("message").toString();
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipientAddress);
        email.setSubject(subject);
        email.setText(message + " \r\n" + confirmationUrl);
        //email.setFrom(env.getProperty("support.email"));
        email.setFrom("pmestry007@gmail.com");
        return email;
    }

    /**
     * GET  /registrationConfirm : Registration confirmation.
     *
     * @param token the token key
     * @throws RuntimeException 500 (Internal Server Error) if the user couldn't be activated
     */
    @RequestMapping("/register/registrationConfirm")
    @Timed
    public String confirmRegistration(@RequestParam(value = "token") final String token, final HttpServletRequest request)
        throws UnsupportedEncodingException {
        Optional<User> user =  userService.confirmRegistration(token);

        Locale locale = request.getLocale();

        if(user.isPresent()){
            //System.out.println(user.get().getActivationKey().toString());
            if(user.get().getActivationKey() == null){
                return "<html><title></title><head></head><body><h1 style=\"background-color:DodgerBlue;\">" +
                    "User already registered.</h1> <br />" +
                    "<a href='http://localhost:8080'>Click here to Sign In</a></body></html>"  ;
            }
            else {
                System.out.println("User registered");
                return "<html><title></title><head></head><body><h1 style=\"background-color:DodgerBlue;\">" +
                    "User Registration Successful.</h1><br />" +
                    "<a href='http://localhost:8080'>Click here to Sign In</a></body></html>"  ;
            }
        }
        else{
            return "<html><title></title><head></head><body><h1 style=\"background-color:DodgerBlue;\">" +
                "Link Expired. Contact System administrator for a new link.</h1></body></html>"  ;
        }
    }

    /**
     * GET  /activate : activate the registered user.
     *
     * @param key the activation key
     * @throws RuntimeException 500 (Internal Server Error) if the user couldn't be activated
     */
    @GetMapping("/activate")
    @Timed
    public void activateAccount(@RequestParam(value = "key") String key) {
        Optional<User> user = userService.activateRegistration(key);
        if (!user.isPresent()) {
            throw new InternalServerErrorException("No user was found for this activation key");
        }
    }

    @GetMapping(value = "/otp")
    @Timed
    public ResponseEntity<UserOTP> updateotpdetails(@RequestParam(value="otpnumber") final String otpnumber)
        throws Exception {
        log.debug("OTP Number in Account Resource=" + otpnumber);
        UserOTPService userOTPService = new UserOTPService(userOTPRepository);

        Optional<UserOTP> userOTPOptional = userOTPService.UpdateOTPDetails(otpnumber);


//        return userOTPService.UpdateOTPDetails(otpnumber)
//            .map(otp -> new ResponseEntity<>("Success", HttpStatus.OK))
//            .orElseThrow(() -> new BadRequestAlertException("OTP Number not found", "", ""));


        if(userOTPOptional != null){
            return  new ResponseEntity<>(userOTPOptional.get() , HttpStatus.OK);
        }
        else{
            return new ResponseEntity<>(userOTPOptional.get() , HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(value = "/addotp")
    @Timed
    public void addOTPDetails(@RequestParam(value="username") final String username)
        throws Exception {

        UserOTPService userOTPService = new UserOTPService(userOTPRepository);

        Optional<User> user = userRepository.findOneByLogin(username);

        Random rnd = new Random();
        int otpnumber = 100000 + rnd.nextInt(900000);
        log.debug("OTP Number in Account Resource=" + otpnumber);

        userOTPService.AddOTPDetails(user.get(), String.valueOf(otpnumber));

        Map<String, String> mapMail = new HashMap<String,String>();

        mapMail.put("subject","OTP Resend Confirmation");
        mapMail.put("url", "");
        mapMail.put("message","OTP " + String.valueOf(otpnumber) + " is generated.");
        mapMail.put("email","pmestry007@gmail.com");

        RegistrationListener reglistner = new RegistrationListener();
        reglistner.onApplicationEvent(new OnRegistrationCompleteEvent(username,null,
            "",String.valueOf(otpnumber),
            constructEmailMessage("",username,String.valueOf(otpnumber), mapMail)));
        /*return userOTPService.UpdateOTPDetails(otpnumber)
            .map(otp -> new ResponseEntity<>(otp, HttpStatus.OK))
            .orElseThrow(() -> new BadRequestAlertException("OTP Number not found", "", ""));*/
    }
    /**
     * GET  /authenticate : check if the user is authenticated, and return its login.
     *
     * @param request the HTTP request
     * @return the login if the user is authenticated
     */
    @GetMapping("/authenticate")
    @Timed
    public String isAuthenticated(HttpServletRequest request) {
        log.debug("REST request to check if the current user is authenticated");
        return request.getRemoteUser();
    }

    /**
     * GET  /account : get the current user.
     *
     * @return the current user
     * @throws RuntimeException 500 (Internal Server Error) if the user couldn't be returned
     */
    @GetMapping("/account")
    @Timed
    public UserDTO getAccount() {
        return userService.getUserWithAuthorities()
            .map(UserDTO::new)
            .orElseThrow(() -> new InternalServerErrorException("User could not be found"));
    }

    /**
     * POST  /account : update the current user information.
     *
     * @param userDTO the current user information
     * @throws EmailAlreadyUsedException 400 (Bad Request) if the email is already used
     * @throws RuntimeException 500 (Internal Server Error) if the user login wasn't found
     */
    @PostMapping("/account")
    @Timed
    public void saveAccount(@Valid @RequestBody UserDTO userDTO) {
        final String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new InternalServerErrorException("Current user login not found"));
        Optional<User> existingUser = userRepository.findOneByEmailIgnoreCase(userDTO.getEmail());
        if (existingUser.isPresent() && (!existingUser.get().getLogin().equalsIgnoreCase(userLogin))) {
            throw new EmailAlreadyUsedException();
        }
        Optional<User> user = userRepository.findOneByLogin(userLogin);
        if (!user.isPresent()) {
            throw new InternalServerErrorException("User could not be found");
        }
        userService.updateUser(userDTO.getFirstName(), userDTO.getLastName(), userDTO.getEmail(),
            userDTO.getLangKey(), userDTO.getImageUrl());
    }

    /**
     * POST  /account/change-password : changes the current user's password
     *
     * @param passwordChangeDto current and new password
     * @throws InvalidPasswordException 400 (Bad Request) if the new password is incorrect
     */
    @PostMapping(path = "/account/change-password")
    @Timed
    public void changePassword(@RequestBody PasswordChangeDTO passwordChangeDto) {
        if (!checkPasswordLength(passwordChangeDto.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        userService.changePassword(passwordChangeDto.getCurrentPassword(), passwordChangeDto.getNewPassword());
    }

    /**
     * GET  /account/sessions : get the current open sessions.
     *
     * @return the current open sessions
     * @throws RuntimeException 500 (Internal Server Error) if the current open sessions couldn't be retrieved
     */
    @GetMapping("/account/sessions")
    @Timed
    public List<PersistentToken> getCurrentSessions() {
        return persistentTokenRepository.findByUser(
            userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new InternalServerErrorException("Current user login not found")))
                    .orElseThrow(() -> new InternalServerErrorException("User could not be found"))
        );
    }

    /**
     * DELETE  /account/sessions?series={series} : invalidate an existing session.
     *
     * - You can only delete your own sessions, not any other user's session
     * - If you delete one of your existing sessions, and that you are currently logged in on that session, you will
     *   still be able to use that session, until you quit your browser: it does not work in real time (there is
     *   no API for that), it only removes the "remember me" cookie
     * - This is also true if you invalidate your current session: you will still be able to use it until you close
     *   your browser or that the session times out. But automatic login (the "remember me" cookie) will not work
     *   anymore.
     *   There is an API to invalidate the current session, but there is no API to check which session uses which
     *   cookie.
     *
     * @param series the series of an existing session
     * @throws UnsupportedEncodingException if the series couldnt be URL decoded
     */
    @DeleteMapping("/account/sessions/{series}")
    @Timed
    public void invalidateSession(@PathVariable String series) throws UnsupportedEncodingException {
        String decodedSeries = URLDecoder.decode(series, "UTF-8");
        SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .ifPresent(u ->
                persistentTokenRepository.findByUser(u).stream()
                    .filter(persistentToken -> StringUtils.equals(persistentToken.getSeries(), decodedSeries))
                    .findAny().ifPresent(t -> persistentTokenRepository.deleteById(decodedSeries)));
    }

    /**
     * POST   /account/reset-password/init : Send an email to reset the password of the user
     *
     * @param mail the mail of the user
     * @throws EmailNotFoundException 400 (Bad Request) if the email address is not registered
     */
    @PostMapping(path = "/account/reset-password/init")
    @Timed
    public void requestPasswordReset(@RequestBody String mail) {
       mailService.sendPasswordResetMail(
           userService.requestPasswordReset(mail)
               .orElseThrow(EmailNotFoundException::new)
       );
    }

    /**
     * POST   /account/reset-password/finish : Finish to reset the password of the user
     *
     * @param keyAndPassword the generated key and the new password
     * @throws InvalidPasswordException 400 (Bad Request) if the password is incorrect
     * @throws RuntimeException 500 (Internal Server Error) if the password could not be reset
     */
    @PostMapping(path = "/account/reset-password/finish")
    @Timed
    public void finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword) {
        if (!checkPasswordLength(keyAndPassword.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        Optional<User> user =
            userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey());

        if (!user.isPresent()) {
            throw new InternalServerErrorException("No user was found for this reset key");
        }
    }

    private static boolean checkPasswordLength(String password) {
        return !StringUtils.isEmpty(password) &&
            password.length() >= ManagedUserVM.PASSWORD_MIN_LENGTH &&
            password.length() <= ManagedUserVM.PASSWORD_MAX_LENGTH;
    }
}
