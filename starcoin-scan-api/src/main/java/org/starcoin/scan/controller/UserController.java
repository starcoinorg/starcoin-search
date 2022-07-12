package org.starcoin.scan.controller;

import com.novi.serde.DeserializationError;
import com.novi.serde.SerializationError;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.starcoin.bean.ApiKey;
import org.starcoin.bean.UserInfo;
import org.starcoin.scan.service.RateLimitService;
import org.starcoin.scan.utils.JSONResult;
import org.starcoin.types.SignedMessage;
import org.starcoin.utils.Hex;
import org.starcoin.utils.SignatureUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

@Api(tags = "user")
@RestController
@RequestMapping("v2/user")
@Slf4j
public class UserController {
    @Autowired
    private RateLimitService rateLimitService;

    @ApiOperation("login by address")
    @GetMapping("/login/{address}/")
    public JSONResult login(@PathVariable(value = "address") String address, @RequestParam("sign") String sign, HttpSession session) {
        //verify sign
        boolean checked = false;
        log.info("user login : {}, {}", address, sign);
        try {
            SignedMessage message = SignedMessage.bcsDeserialize(Hex.decode(sign));
            if (!message.account.toString().equals(address)) {
                return new JSONResult<>("401", "address information and signature do not match");
            }
            checked = SignatureUtils.signedMessageCheckSignature(message);
        } catch (DeserializationError | SerializationError e) {
            return new JSONResult<>("401", "incorrect signature message");
        }
        if (checked) {
            //login
            long userId = rateLimitService.logIn(address);
            session.setAttribute(address, userId);
            log.info("save session: {}, {}, {}", address, userId, session.getMaxInactiveInterval());
            session.setMaxInactiveInterval(3600);
            return new JSONResult<>("200", "login success");
        }
        return new JSONResult<>("401", "signature message verification does not pass");
    }

    @ApiOperation("logout")
    @GetMapping("/logout/{address}/")
    public JSONResult logout(HttpServletRequest request, @PathVariable(value = "address") String address) {
        HttpSession session = request.getSession();
        Long userId = (Long) session.getAttribute(address);
        if (userId == null) {
            log.warn("user not login: {}", address);
            return new JSONResult("401", "address not login");
        }
        session.removeAttribute(address);
        return new JSONResult<>("200", "logout ok");
    }

    @ApiOperation("show user info")
    @GetMapping("/show/{address}")
    public JSONResult<UserInfo> showUser(HttpServletRequest request, @PathVariable(value = "address") String address){
        HttpSession session = request.getSession();
        Long userId = (Long) session.getAttribute(address);
        if (userId == null) {
            log.warn("user show but not login: {}", address);
            return new JSONResult("401", "address not login");
        }
        return new JSONResult("200", "ok", rateLimitService.showUser(userId));
    }

    @ApiOperation("update wallet address")
    @GetMapping("/update/address/{new}")
    public JSONResult updateUserAddr(HttpServletRequest request, @PathVariable(value = "new") String address, @RequestParam(value = "old") String old, HttpSession session){
        HttpSession requestSession = request.getSession();
        Long userId = (Long) requestSession.getAttribute(old);
        if (userId == null) {
            return new JSONResult("401", "address not login");
        }
        long result = rateLimitService.updateAddress(userId, address, old);
        if (result == 1) {
            //update ok, set session
            session.removeAttribute(old);
            session.setAttribute(address, userId);
            return new JSONResult("200", "address update ok");
        }
        return new JSONResult("500", "address update failure, status:" + result);
    }

    @ApiOperation("update user profile info")
    @GetMapping("/update/{address}")
    public JSONResult updateUser(HttpServletRequest request, @PathVariable(value = "address") String address,
                                 @RequestParam(value = "mobile", required = false) String mobile,
                                 @RequestParam(value = "email", required = false) String email,
                                 @RequestParam(value = "avatar", required = false) String avatar,
                                 @RequestParam(value = "twitter", required = false) String twitter,
                                 @RequestParam(value = "discord", required = false) String discord,
                                 @RequestParam(value = "telegram", required = false) String telegram,
                                 @RequestParam(value = "domain", required = false) String domain,
                                 @RequestParam(value = "blog", required = false) String blog,
                                 @RequestParam(value = "profile", required = false) String profile
    ) {
        HttpSession session = request.getSession();
        Long userId = (Long) session.getAttribute(address);
        if (userId == null) {
            return new JSONResult("401", "address not login");
        }
        long code = rateLimitService.updateUserInfo(userId, mobile, email, avatar, twitter, discord, telegram, domain, blog, profile);
        return new JSONResult("200", "address update ok, status:" + code);
    }

    @ApiOperation("delete user")
    @GetMapping("/destroy/{address}")
    public JSONResult destroy(HttpServletRequest request, @PathVariable(value = "address") String address) throws Exception {
        HttpSession session = request.getSession();
        Long userId = (Long) session.getAttribute(address);
        if (userId == null) {
            return new JSONResult("401", "address not login");
        }
        long code = rateLimitService.destroyUser(userId);
        return new JSONResult("200", "address destroy ok, status:" + code);
    }

    @ApiOperation("get user api keys")
    @GetMapping("/apikey/list/")
    public JSONResult<List<ApiKey>> getAppKeys(HttpServletRequest request, @RequestParam(value = "address") String address){
        HttpSession session = request.getSession();
        Long userId = (Long) session.getAttribute(address);
        if (userId == null) {
            return new JSONResult("401", "address not login");
        }
        List<ApiKey> apiKeyList = rateLimitService.getApiKeys(userId);
        return new JSONResult("200", "ok", apiKeyList);
    }

    @ApiOperation("add api key of dapp")
    @GetMapping("/apikey/add/{app_name}")
    public JSONResult addAppKey(HttpServletRequest request, @RequestParam(value = "address") String address, @PathVariable(value = "app_name") String appName) throws Exception {
        HttpSession session = request.getSession();
        Long userId = (Long) session.getAttribute(address);
        if (userId == null) {
            return new JSONResult("401", "address not login");
        }
        long code = rateLimitService.addApiKey(userId, appName);
        return new JSONResult("200", "app name add ok, status:" + code);
    }

    @ApiOperation("update app name")
    @GetMapping("/apikey/update/{app_name}")
    public JSONResult updateAppName(@PathVariable(value = "app_name") String appName, @RequestParam(value = "app_key") String appKey) {
        long code = rateLimitService.updateAppName(appName, appKey);
        return new JSONResult("200", "update app name ok, status:" + code);
    }

    @ApiOperation("remove api key")
    @GetMapping("/apikey/remove")
    public JSONResult updateAppName(@RequestParam(value = "app_key") String appKey) {
        long code = rateLimitService.remove(appKey);
        return new JSONResult("200", "remove api key ok, status:" + code);
    }
}
