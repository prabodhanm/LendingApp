import { Component, AfterViewInit, Renderer, ElementRef } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Router } from '@angular/router';
import { JhiEventManager } from 'ng-jhipster';

import { LoginService } from 'app/core/login/login.service';
import { StateStorageService } from 'app/core/auth/state-storage.service';
import { HttpClient, HttpClientModule, HttpResponse, HttpHeaders, HttpParams } from '@angular/common/http';

//import 'rxjs/add/operator/map';

@Component({
    selector: 'jhi-login-modal',
    templateUrl: './login.component.html',
    styleUrls: ["../../components/styles/lending.css"]
})
export class JhiLoginModalComponent implements AfterViewInit {
    authenticationError: boolean;
    password: string;
    rememberMe: boolean;
    username: string;
    credentials: any;
    show:boolean = false;
    otpnumber:string;
    error:boolean = false;
    expire:boolean=false;
    success:boolean;
    mydata:any;
    timeLeft:number = 120;
    interval;
    public background_color="grey";
    constructor(
        private eventManager: JhiEventManager,
        private loginService: LoginService,
        private stateStorageService: StateStorageService,
        private elementRef: ElementRef,
        private renderer: Renderer,
        private router: Router,
        public activeModal: NgbActiveModal,
        private http: HttpClient
    ) {
        this.credentials = {};
    }

    ngAfterViewInit() {
        setTimeout(() => this.renderer.invokeElementMethod(this.elementRef.nativeElement.querySelector('#username'), 'focus', []), 0);
    }

    startTimer() {
        this.interval = setInterval(() => {
          if(this.timeLeft > 0) {
            this.timeLeft--;
          } else {
            this.timeLeft = 0;
          }
        },1000)
      }

    async changeOTP() {
        await this.http.get('http://localhost:8080/api/otp',
            {params: new HttpParams().set('otpnumber', this.otpnumber)})
                        .subscribe(
                        res => {
                                this.mydata=res;
                                if(this.mydata.otpnumber === "Expired"){
                                    this.error=false;
                                    this.expire=true;
                                    this.otpnumber="";
                                }
                                else{
                                    this.expire=false;
                                    this.error=false;
                                    this.authenticationError = false;
                                    this.activeModal.dismiss('login success');
                                    if (this.router.url === '/register' || /^\/activate\//.test(this.router.url) || /^\/reset\//.test(this.router.url)) {
                                        this.router.navigate(['']);
                                    }

                                    this.eventManager.broadcast({
                                        name: 'authenticationSuccess',
                                        content: 'Sending Authentication Success'
                                    });

                                    // previousState was set in the authExpiredInterceptor before being redirected to login modal.
                                    // since login is succesful, go to stored previousState and clear previousState
                                    const redirect = this.stateStorageService.getUrl();
                                    if (redirect) {
                                        this.stateStorageService.storeUrl(null);
                                        this.router.navigate([redirect]);
                                    }
                                }
                            },
                        err => {
                            this.error=true;
                            this.expire=false;
                            this.otpnumber="";
                        });

    }

    ngOnInit(){
            if(JSON.parse(localStorage.getItem('RememberMe')) !== null)
            {
              this.username = localStorage.getItem('Id');
              this.password = localStorage.getItem('Password');
              this.rememberMe = JSON.parse(localStorage.getItem('RememberMe'));
            }
        }

    cancel() {
        this.credentials = {
            username: null,
            password: null,
            rememberMe: true
        };
        this.authenticationError = false;
        this.activeModal.dismiss('cancel');
    }

    toggle(){
        this.show = !this.show;
    }

    async resendOTP() {
        //alert('resend OTP from login component...');
        await this.loginService
            .resendOTP(this.username,this.otpnumber);
        this.error=false;
        this.expire=false;
        this.timeLeft=120;
        this.startTimer();
    }



    login() {
        if(this.rememberMe){
            localStorage.setItem('Id', this.username);
            localStorage.setItem('Password', this.password);
            localStorage.setItem('RememberMe', JSON.stringify(this.rememberMe));
        }
        else {
            localStorage.removeItem('Id');
            localStorage.removeItem('Password');
            localStorage.removeItem('RememberMe');
        }
        this.loginService
            .login({
                username: this.username,
                password: this.password,
                rememberMe: this.rememberMe
            })
            .then(() => {
                this.show=!this.show;
                this.authenticationError = false;
            })
            /*.then(() => {
                this.authenticationError = false;
                this.activeModal.dismiss('login success');
                if (this.router.url === '/register' || /^\/activate\//.test(this.router.url) || /^\/reset\//.test(this.router.url)) {
                    this.router.navigate(['']);
                }

                this.eventManager.broadcast({
                    name: 'authenticationSuccess',
                    content: 'Sending Authentication Success'
                });

                // previousState was set in the authExpiredInterceptor before being redirected to login modal.
                // since login is succesful, go to stored previousState and clear previousState
                const redirect = this.stateStorageService.getUrl();
                if (redirect) {
                    this.stateStorageService.storeUrl(null);
                    this.router.navigate([redirect]);
                }
            })*/
            .catch(() => {
                this.authenticationError = true;
            });

    }


    register() {
        this.activeModal.dismiss('to state register');
        this.router.navigate(['/register']);
    }

    requestResetPassword() {
        this.activeModal.dismiss('to state requestReset');
        this.router.navigate(['/reset', 'request']);
    }
}
