package org.starcoin.scan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.bean.ApiKey;
import org.starcoin.bean.UserInfo;
import org.starcoin.constant.Constant;
import org.starcoin.scan.repository.user.ApiKeyRepository;
import org.starcoin.scan.repository.user.RateLimitRepository;
import org.starcoin.scan.repository.user.UserInfoRepository;
import org.starcoin.utils.KeyUtils;

import java.util.*;

@Service
public class RateLimitService {
    @Autowired
    private UserInfoRepository userInfoRepository;
    @Autowired
    private ApiKeyRepository apiKeyRepository;
    @Autowired
    private RateLimitRepository rateLimitRepository;

    public void saveUser(UserInfo userInfo) {
        userInfoRepository.save(userInfo);
    }

    public UserInfo showUser(Long userId) {
        Optional<UserInfo> userInfo = userInfoRepository.findById(userId);
        if(userInfo.isPresent()) {
            return userInfo.get();
        }
       return null;
    }

    public long logIn(String address) {
        UserInfo dbUser = userInfoRepository.getByWalletAddr(address);
        if(dbUser != null) {
            return dbUser.getId();
        }else {
            //insert to db
            dbUser = userInfoRepository.save(new UserInfo(address));
            return dbUser.getId();
        }
    }
    public long updateAddress(long userId, String newAddress, String old) {
        return userInfoRepository.updateAddress(newAddress, userId, old);
    }

    public long destroyUser(long userId) {
        return userInfoRepository.updateStatus(userId);
    }

    public long updateUserInfo(long userId, String mobile, String email, String avatar, String twitter, String discord, String telegram, String domain, String blog, String profile) {
        //read from db
       Optional<UserInfo> userInfo = userInfoRepository.findById(userId);
       if(userInfo.isPresent()) {
           UserInfo newUser = userInfo.get();
           if(mobile != null && mobile.length() >0) {
               newUser.setMobile(mobile);
           }
           if(email != null && email.length() >0) {
               newUser.seteMail(email);
           }
           if(avatar != null && avatar.length() >0) {
               newUser.setAvatar(avatar);
           }
           if(twitter != null && twitter.length() >0) {
               newUser.setTwitterName(twitter);
           }
           if(discord != null && discord.length() >0) {
               newUser.setDiscordName(discord);
           }
           if(telegram != null && telegram.length() >0) {
               newUser.setTelegramName(telegram);
           }
           if(domain != null && domain.length() >0) {
               newUser.setDomainName(domain);
           }
           if(blog != null && blog.length() >0) {
               newUser.setBlogAddr(blog);
           }
           if(profile != null && profile.length() >0) {
               newUser.setProfile(profile);
           }
           userInfoRepository.save(newUser);
           return 1;
       }
           return 0;
    }


    public boolean checkUser(long userId) {
        //todo cache not exist user
       Optional<UserInfo> userInfo = userInfoRepository.findById(userId);
       if(userInfo.isPresent()) return true;
       return false;
    }

    public List<ApiKey> getApiKeys(long userId) {
        return apiKeyRepository.findApiKeyByUserId(userId);
    }

    public long addApiKey(long userId, String appName) {
        //check userid
        if(checkUser(userId)) {
            List<ApiKey> apiKeyList = apiKeyRepository.findApiKeyByUserId(userId);
            if(apiKeyList != null) {
                if (apiKeyList.size() >= Constant.MAX_KEYS_COUNT) {
                    return 1;
                }
            }
            //add api key
            ApiKey newApiKey = apiKeyRepository.save(new ApiKey(userId, appName, KeyUtils.base62Encode()));
            if(newApiKey != null) {
                return newApiKey.getId();
            }
         }
        return 0;
    }

    public int updateAppName(String newAppName, String appKey) {
        return apiKeyRepository.updateAppName(newAppName, appKey);
    }


    public int remove(String appKey) {
        return apiKeyRepository.deleteByAppKey(appKey);
    }
}
