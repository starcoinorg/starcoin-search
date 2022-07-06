package org.starcoin.scan.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.tomcat.util.buf.HexUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.starcoin.bean.ApiKey;
import org.starcoin.bean.UserInfo;
import org.starcoin.scan.service.RateLimitService;
import org.starcoin.types.SignedMessage;
import org.starcoin.utils.Hex;
import org.starcoin.utils.SignatureUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

@Api(tags = "user")
@RestController
@RequestMapping("v2/user")
public class UserController {
    @Autowired
    private RateLimitService rateLimitService;

    @ApiOperation("login by address")
    @GetMapping("/login/{address}/")
    public long login(HttpServletRequest request, @PathVariable(value = "address") String address,@RequestParam("sign")String sign) throws Exception {
        //verify sign
        SignedMessage message = SignedMessage.bcsDeserialize(Hex.decode(sign));
        boolean checked = SignatureUtils.signedMessageCheckSignature(message);
        if(checked) {
            //login
            long userId = rateLimitService.logIn(address);
            HttpSession session = request.getSession();
            session.setAttribute(address, userId);
            return userId;
        }
        return -1;
    }

    @ApiOperation("logout")
    @GetMapping("/logout/{address}/")
    public String logout(HttpServletRequest request, @PathVariable(value = "address") String address) throws Exception {
        HttpSession session = request.getSession();
        session.removeAttribute(address);
        return "ok";
    }

    @ApiOperation("show user info")
    @GetMapping("/show/{address}")
    public UserInfo showUser(HttpServletRequest request, @PathVariable(value = "address") String address) throws Exception {
        HttpSession session = request.getSession();
        Long userId = (Long) session.getAttribute(address);
        if(userId == null) {
            return null;
        }
        return rateLimitService.showUser(userId);
    }

    @ApiOperation("update wallet address")
    @GetMapping("/update/address/{new}")
    public long updateUserAddr(HttpServletRequest request, @PathVariable(value = "new") String address, @RequestParam(value = "old") String old) throws Exception {
        HttpSession session = request.getSession();
        Long userId = (Long) session.getAttribute(old);
        if(userId == null) {
            return -1;
        }
        long result = rateLimitService.updateAddress(userId, address, old);
        if(result == 1) {
            //update ok, set session
            session.setAttribute(address, userId);
        }
        return result;
    }

    @ApiOperation("update user profile info")
    @GetMapping("/update/{address}")
    public long updateUser(HttpServletRequest request, @PathVariable(value = "address") String address,
                           @RequestParam(value = "mobile",required = false) String mobile,
                           @RequestParam(value = "email",required = false) String email,
                           @RequestParam(value = "avatar",required = false) String avatar,
                           @RequestParam(value = "twitter",required = false) String twitter,
                           @RequestParam(value = "discord",required = false) String discord,
                           @RequestParam(value = "telegram",required = false) String telegram,
                           @RequestParam(value = "domain",required = false) String domain,
                           @RequestParam(value = "blog",required = false) String blog,
                           @RequestParam(value = "profile",required = false) String profile
                           ) throws Exception {
        HttpSession session = request.getSession();
        Long userId = (Long) session.getAttribute(address);
        if(userId == null) {
            return -1;
        }
        return rateLimitService.updateUserInfo(userId, mobile, email, avatar, twitter, discord, telegram, domain, blog, profile);
    }

    @ApiOperation("delete user")
    @GetMapping("/destroy/{address}")
    public long destroy(HttpServletRequest request, @PathVariable(value = "address") String address) throws Exception {
        HttpSession session = request.getSession();
        Long userId = (Long) session.getAttribute(address);
        if(userId == null) {
            return -1;
        }
        return rateLimitService.destroyUser(userId);
    }

    @ApiOperation("get user api keys")
    @GetMapping("/apikey/list/")
    public List<ApiKey> getAppKeys(HttpServletRequest request, @RequestParam(value = "address") String address) throws Exception {
        HttpSession session = request.getSession();
        Long userId = (Long) session.getAttribute(address);
        if(userId == null) {
            return null;
        }
        return rateLimitService.getApiKeys(userId);
    }

    @ApiOperation("add api key of dapp")
    @GetMapping("/apikey/add/{app_name}")
    public long addAppKey(HttpServletRequest request, @RequestParam(value = "address") String address, @PathVariable(value = "app_name") String appName) throws Exception {
        HttpSession session = request.getSession();
        Long userId = (Long) session.getAttribute(address);
        if(userId == null) {
            return -1;
        }
        return rateLimitService.addApiKey(userId, appName);
    }

    @ApiOperation("update app name")
    @GetMapping("/apikey/update/{app_name}")
    public long updateAppName(@PathVariable(value = "app_name") String appName, @RequestParam(value = "app_key") String appKey) throws Exception {
        rateLimitService.updateAppName(appName, appKey);
        return 0;
    }
    @ApiOperation("remove api key")
    @GetMapping("/apikey/remove")
    public long updateAppName(@RequestParam(value = "app_key") String appKey) throws Exception {
        rateLimitService.remove(appKey);
        return 0;
    }

    }
