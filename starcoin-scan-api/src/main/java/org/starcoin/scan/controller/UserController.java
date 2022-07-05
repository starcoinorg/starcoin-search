package org.starcoin.scan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.starcoin.bean.UserInfo;
import org.starcoin.scan.service.RateLimitService;

@RestController
@RequestMapping("user")
public class UserController {
    @Autowired
    private RateLimitService rateLimitService;

    @GetMapping("/login/{address}/")
    public long login(@PathVariable("address") String address) throws Exception {
        //todo write user_id to session
        return rateLimitService.logIn(address);
    }

    @GetMapping("/logout/{address}/")
    public long logout(@PathVariable("address") String address) throws Exception {
        //todo remove user_id to session
        return 1;
    }

    @GetMapping("/show")
    public UserInfo showUser() throws Exception {
        //todo get user_id from session
        Long userId = 3l;
        return rateLimitService.showUser(userId);
    }

    @GetMapping("/update/address/{new_address}")
    public long updateUserAddr(@PathVariable("new_address") String address, @RequestParam("old") String old) throws Exception {
        //todo get user_id from session
        long userId = 0;
        return rateLimitService.updateAddress(userId, address, old);
    }

    @GetMapping("/update/")
    public long updateUser(@RequestParam("mobile") String mobile,
                           @RequestParam("email") String email,
                           @RequestParam("avatar") String avatar,
                           @RequestParam("twitter") String twitter,
                           @RequestParam("discord") String discord,
                           @RequestParam("telegram") String telegram,
                           @RequestParam("domain") String domain,
                           @RequestParam("blog") String blog,
                           @RequestParam("profile") String profile
                           ) throws Exception {
        //get user_id from session
        long userId = 0;
        return rateLimitService.updateUserInfo(userId, mobile, email, avatar, twitter, discord, telegram, domain, blog, profile);
    }

    @GetMapping("/destroy")
    public long destroy() throws Exception {
        //todo get user_id from session
        long userId = 0;
        return rateLimitService.destroyUser(userId);
    }

    @GetMapping("/apikey/add/{app_name}")
    public long addAppKey(@PathVariable("app_name") String appName) throws Exception {
        //get user_id from session
        long userId = 0;
        return rateLimitService.addApiKey(userId, appName);
    }

    @GetMapping("/apikey/update/{app_name}")
    public long updateAppName(@PathVariable("app_name") String appName, @RequestParam("app_key") String appKey) throws Exception {
        rateLimitService.updateAppName(appName, appKey);
        return 0;
    }
    @GetMapping("/apikey/remove")
    public long updateAppName(@RequestParam("app_key") String appKey) throws Exception {
        rateLimitService.remove(appKey);
        return 0;
    }

    }
