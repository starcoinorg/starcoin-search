package org.starcoin.bean;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "api_key")
public class ApiKey {
    @Id
    @SequenceGenerator(name = "seq_api_key_id", allocationSize = 1, initialValue = 1, sequenceName = "api_key_id_seq")
    @GeneratedValue(generator = "seq_api_key_id", strategy = GenerationType.SEQUENCE)
    @Column(name = "key_id")
    private long id;
    @Column(name = "user_id")
    private long userId;
    @Column(name = "app_name")
    private String appName;
    @Column(name = "api_key")
    private String apiKey;
    @Column(name = "is_valid")
    private boolean isValid;
    @Column(name = "create_time")
    private Date createTime;

    public ApiKey(long userId, String appName, String apiKey) {
        this.userId = userId;
        this.appName = appName;
        this.apiKey = apiKey;
        this.isValid = true;
        this.createTime = new Date();
    }
    public ApiKey() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
