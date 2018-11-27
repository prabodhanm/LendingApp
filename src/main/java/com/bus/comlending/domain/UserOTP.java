package com.bus.comlending.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "user_otp")
public class UserOTP implements Serializable {

    /*@NotNull
    @Size(min = 1, max = 50)
    @Column( name = "userid")*/

    @Id
    private String userid;

    @Size(max = 50)
    @Column(name = "otpnumber", length = 50)
    private String oTPNumber;

    @NotNull
    @Column(name = "active")
    private boolean active = false;

    @Column(name = "creation_date")
    private Instant creation_date = null;

    @Column(name = "modified_date")
    private Instant modified_date = null;

    public String getUserid() {
        return userid;
    }

    public void setUserid(String id) {
        this.userid = id;
    }

    public  String getOtpnumber() {return oTPNumber;}

    public void setOtpnumber(String otpnumber) {this.oTPNumber = otpnumber;}

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setCreation_date(Instant creationdate) {this.creation_date = creationdate;}

    public Instant getCreation_date(){return this.creation_date;}

    public void setModified_date(Instant modified_date) {this.modified_date = modified_date;}

    public Instant getModified_date(){return this.modified_date;}

    @Override
    public String toString() {
        return "UserOTP{" +
            "userid='" + userid + '\'' +
            ", oTPNumber='" + oTPNumber + '\'' +
            ", active='" + active + '\'' +
            ", creation_date='" + creation_date + '\'' +
            ", modified_date='" + modified_date + '\'' +
            "}";
    }
}
