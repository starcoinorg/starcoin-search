package org.starcoin.scan.controller;

import com.hazelcast.cache.ICache;
import com.hazelcast.core.HazelcastInstance;
import com.novi.serde.Bytes;
import com.novi.serde.DeserializationError;
import com.novi.serde.SerializationError;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.starcoin.bean.ApiKey;
import org.starcoin.bean.UserInfo;
import org.starcoin.scan.service.RateLimitService;
import org.starcoin.scan.utils.CodeUtils;
import org.starcoin.scan.utils.JSONResult;
import org.starcoin.types.SignedMessage;
import org.starcoin.utils.Hex;
import org.starcoin.utils.SignatureUtils;

import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

@Api(tags = "user")
@RestController
@RequestMapping("v2/user")
@Slf4j
public class UserController {
    private final String LOGIN_CODE_PREFIX = "STCSCAN_LOGIN_CODE:";
    private final String UPDATE_CODE_PREFIX = "STCSCAN_UPDATE_ADDR_CODE:";
    private final String DESTROY_CODE_PREFIX = "STCSCAN_DESTROY_ADDR_CODE:";
    private final String DESTROY_APIKEY_CODE_PREFIX = "STCSCAN_DESTROY_APIKEY_CODE:";


    private static ExpiryPolicy codeExpiryPolicy = AccessedExpiryPolicy.factoryOf(Duration.FIVE_MINUTES).create();
    @Autowired
    private RateLimitService rateLimitService;
    @Qualifier("hazelcastInstance")
    @Autowired
    private HazelcastInstance hazelcastInstance;

    private ConcurrentMap<String, Long> sessionMap() {
        return hazelcastInstance.getMap("session");
    }

    private ICache<String, String> codeCache() {
        ICache<String, String> cache = hazelcastInstance.getCacheManager().getCache("code_cache");
        if(cache == null) {
           log.error("cache not exist!");
        }
        return cache;
    }

    private Long getSession(String address) {
        if(address != null && address.length() == 34) { // address length
            return sessionMap().get(address);
        }
        return null;
    }

    @ApiOperation("login by address")
    @GetMapping("/login/{address}/")
    public JSONResult login(@PathVariable(value = "address") String address, @RequestParam("sign") String sign) {
        address = address.toLowerCase();
        Long userId = getSession(address);
        if (userId != null) {
            return new JSONResult<>("401", "address already log in.");
        }
        //verify sign
        log.info("user login : {}, {}", address, sign);
        JSONResult result = verifyMessage(address, sign, 1);
        if(result.getStatusCodeValue() != 200) {
            return result;
        }
        //login
        long uid = rateLimitService.logIn(address);
        sessionMap().put(address, uid);

        log.info("save session: {}, {}", address, uid);
        return new JSONResult<>("200", "login success");
    }

    @ApiOperation("logout")
    @GetMapping("/logout/{address}/")
    public JSONResult logout(@PathVariable(value = "address") String address) {
        address = address.toLowerCase();
        Long userId = getSession(address);
        if (userId == null) {
            log.warn("user not login: {}", address);
            return new JSONResult("401", "address not login");
        }
        sessionMap().remove(address);
        return new JSONResult<>("200", "logout ok");
    }

    @ApiOperation("show user info")
    @GetMapping("/show/{address}")
    public JSONResult<UserInfo> showUser(@PathVariable(value = "address") String address){
        address = address.toLowerCase();
        Long userId = getSession(address);
        if (userId == null) {
            log.warn("user show but not login: {}", address);
            return new JSONResult("401", "address not login");
        }
        return new JSONResult("200", "ok", rateLimitService.getUser(address));
    }
    @ApiOperation("get code")
    @GetMapping("/code/")
    public JSONResult getCode(@RequestParam(value = "opt") int opt, @RequestParam(value = "address") String address) {
        address = address.toLowerCase();
        if(opt < 1 || opt > 4) {
            log.warn("get code opt err: {}, {}", address, opt);
            return new JSONResult("401", "opt not allowed");
        }
        String code = CodeUtils.generateCode(6);
        codeCache().put(address + "::" + opt, code, codeExpiryPolicy);
        return new JSONResult("200", "ok", code);
    }

    @ApiOperation("update wallet address")
    @GetMapping("/update/address/{new}")
    public JSONResult updateUserAddr(@PathVariable(value = "new") String address, @RequestParam(value = "old") String old, @RequestParam("sign") String sign){
        address = address.toLowerCase();
        old = old.toLowerCase();
        Long userId = getSession(old);
        if (userId == null) {
            return new JSONResult("401", "address not login");
        }
        //verify code
        JSONResult resultObj = verifyMessage(old, sign, 2);
        if(resultObj.getStatusCodeValue() != 200) {
            return resultObj;
        }

        long result = rateLimitService.updateAddress(userId, address, old);
        if (result == 1) {
            //update ok, set session
            sessionMap().remove(old);
            sessionMap().put(address, userId);
            return new JSONResult("200", "address update ok");
        }
        return new JSONResult("500", "address update failure, status:" + result);
    }

    @ApiOperation("update user profile info")
    @GetMapping("/update/{address}")
    public JSONResult updateUser(@PathVariable(value = "address") String address,
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
        address = address.toLowerCase();
        Long userId = getSession(address);
        if (userId == null) {
            return new JSONResult("401", "address not login");
        }
        long code = rateLimitService.updateUserInfo(userId, mobile, email, avatar, twitter, discord, telegram, domain, blog, profile);
        return new JSONResult("200", "address update ok, status:" + code);
    }

    @ApiOperation("delete user")
    @GetMapping("/destroy/{address}")
    public JSONResult destroy(@PathVariable(value = "address") String address, @RequestParam("sign") String sign){
        address = address.toLowerCase();
        Long userId = getSession(address);
        if (userId == null) {
            return new JSONResult("401", "address not login");
        }
        //verify code
        JSONResult resultObj = verifyMessage(address, sign, 3);
        if(resultObj.getStatusCodeValue() != 200) {
            return resultObj;
        }

        rateLimitService.destroyUser(userId);
        //delete session
        sessionMap().remove(address);
        return new JSONResult("200", "address destroy ok.");
    }

    @ApiOperation("get user api keys")
    @GetMapping("/apikey/list/")
    public JSONResult<List<ApiKey>> getAppKeys(@RequestParam(value = "address") String address){
        address = address.toLowerCase();
        Long userId = getSession(address);
        if (userId == null) {
            return new JSONResult("401", "address not login");
        }
        List<ApiKey> apiKeyList = rateLimitService.getApiKeys(userId);
        return new JSONResult("200", "ok", apiKeyList);
    }

    @ApiOperation("add api key of dapp")
    @GetMapping("/apikey/add/{app_name}")
    public JSONResult addAppKey(@RequestParam(value = "address") String address, @PathVariable(value = "app_name") String appName){
        address = address.toLowerCase();
        Long userId = getSession(address);
        if (userId == null) {
            return new JSONResult("401", "address not login");
        }
        long code = rateLimitService.addApiKey(userId, appName);
        return new JSONResult("200", "app name add ok, status:" + code);
    }

    @ApiOperation("update app name")
    @GetMapping("/apikey/update/{app_name}")
    public JSONResult updateAppName(@PathVariable(value = "app_name") String appName, @RequestParam(value = "app_key") String appKey){
        long code = rateLimitService.updateAppName(appName, appKey);
        return new JSONResult("200", "update app name ok, status:" + code);
    }

    @ApiOperation("remove api key")
    @GetMapping("/apikey/remove")
    public JSONResult removeAppName(@RequestParam(value = "app_key") String appKey,  @RequestParam(value = "address") String address, @RequestParam("sign") String sign) {
        address = address.toLowerCase();
        Long userId = getSession(address);
        if (userId == null) {
            return new JSONResult("401", "address not login");
        }
        //verify code
        JSONResult resultObj = verifyMessage(address, sign, 4);
        if(resultObj.getStatusCodeValue() != 200) {
            return resultObj;
        }
        long code = rateLimitService.remove(appKey);
        return new JSONResult("200", "remove api key ok, status:" + code);
    }

    private JSONResult verifyMessage(String address, String sign, int opt) {
        try {
            SignedMessage message = SignedMessage.bcsDeserialize(Hex.decode(sign));
            if (!message.account.toString().equals(address)) {
                log.warn("verify message not match: {}, {}, message account: {}", address, opt, message.account);
                return new JSONResult<>("401", "address information and signature do not match");
            }
            //verify code
            String code = codeCache().get(address + "::"+ opt);
            Bytes msg2 = Bytes.fromList(message.message.value);
            String verifying = new String(msg2.content());
            String verifyMessage;
            switch (opt) {
                case 1:
                    verifyMessage = LOGIN_CODE_PREFIX + code;
                    break;
                case 2:
                    verifyMessage = UPDATE_CODE_PREFIX + code;
                    break;
                case 3:
                    verifyMessage = DESTROY_CODE_PREFIX + code;
                    break;
                case 4:
                    verifyMessage= DESTROY_APIKEY_CODE_PREFIX + code;
                    break;
                default:
                    verifyMessage = "";
            };
            if(!verifyMessage.equals(verifying)) {
                log.warn("verigy message code is invalid: {}, {}, {}, {} ", address, opt, verifyMessage, verifying);
                return new JSONResult<>("401", "signature message code is invalid");
            }
            if(SignatureUtils.signedMessageCheckSignature(message)) {
                return new JSONResult<>("200", "ok");
            }else {
                return new JSONResult<>("401", "signature message verification does not pass");
            }
        } catch (DeserializationError | SerializationError e) {
            return new JSONResult<>("401", "incorrect signature message");
        }
    }
}
