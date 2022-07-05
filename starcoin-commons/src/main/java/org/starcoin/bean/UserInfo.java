package org.starcoin.bean;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "user_info")
public class UserInfo {
    @Id
    @SequenceGenerator(name = "seq_user_id", allocationSize = 1, initialValue = 1, sequenceName = "user_info_user_id_seq")
    @GeneratedValue(generator = "seq_user_id", strategy = GenerationType.SEQUENCE)
    @Column(name = "user_id")
    private long id;
    @Column(name = "is_valid")
    private boolean isValid;
    @Column(name = "wallet_addr")
    private String walletAddr;
    @Column(name = "mobile")
    private String mobile;
    @Column(name = "e_mail")
    private String eMail;
    @Column(name = "user_grade")
    private UserGrade userGrade;
    @Column(name = "avatar")
    private String avatar;
    @Column(name = "twitter_name")
    private String twitterName;
    @Column(name = "discord_name")
    private String discordName;
    @Column(name = "telegram_name")
    private String telegramName;
    @Column(name = "domain_name")
    private String domainName;
    @Column(name = "blog_addr")
    private String blogAddr;
    @Column(name = "profile")
    private String profile;
    @Column(name = "create_time")
    private Date createTime;
    @Column(name = "last_login")
    private Date lastLogin;

    public UserInfo() {}
    public UserInfo(String walletAddr) {
        this.walletAddr = walletAddr;
        this.userGrade = UserGrade.free; //default grade is free
        this.createTime = new Date();
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getWalletAddr() {
        return walletAddr;
    }

    public void setWalletAddr(String userName) {
        this.walletAddr = userName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String geteMail() {
        return eMail;
    }

    public void seteMail(String eMail) {
        this.eMail = eMail;
    }

    public UserGrade getUserGrade() {
        return userGrade;
    }

    public void setUserGrade(UserGrade userGrade) {
        this.userGrade = userGrade;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getTwitterName() {
        return twitterName;
    }

    public void setTwitterName(String twitterName) {
        this.twitterName = twitterName;
    }

    public String getDiscordName() {
        return discordName;
    }

    public void setDiscordName(String discordName) {
        this.discordName = discordName;
    }

    public String getTelegramName() {
        return telegramName;
    }

    public void setTelegramName(String telegramName) {
        this.telegramName = telegramName;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getBlogAddr() {
        return blogAddr;
    }

    public void setBlogAddr(String blogAddr) {
        this.blogAddr = blogAddr;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "id=" + id +
                ", userName='" + walletAddr + '\'' +
                ", password='" + mobile + '\'' +
                ", eMail='" + eMail + '\'' +
                ", userGrade=" + userGrade +
                ", avatar='" + avatar + '\'' +
                ", twitterName='" + twitterName + '\'' +
                ", discordName='" + discordName + '\'' +
                ", telegramName='" + telegramName + '\'' +
                ", domainName='" + domainName + '\'' +
                ", blogAddr='" + blogAddr + '\'' +
                ", profile='" + profile + '\'' +
                ", createTime=" + createTime +
                ", lastLogin=" + lastLogin +
                '}';
    }
}
