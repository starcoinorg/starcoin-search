package org.starcoin.scan.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.starcoin.api.Result;
import org.starcoin.bean.TokenHolderInfo;
import org.starcoin.bean.TokenStatistic;
import org.starcoin.bean.TokenStatisticView;
import org.starcoin.scan.service.TokenService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Api(tags = "token")
@RestController
@RequestMapping("v2/token")
public class TokenController {
    @Autowired
    private TokenService tokenService;

    @ApiOperation("get token aggregate stat list")
    @GetMapping("/{network}/stats/{page}")
    public Result<TokenStatisticView> getAggregate(@PathVariable("network") String network, @PathVariable("page") int page,
                                               @RequestParam(value = "count", required = false, defaultValue = "50") int count) {
        Result<TokenStatistic> result = tokenService.tokenAggregateList(network, page, count);
        Result<TokenStatisticView> resultView = new Result<>();
        if(result != null && !result.getContents().isEmpty()) {
           List<TokenStatistic> tokenStatisticList = result.getContents();
            List<TokenStatisticView> viewList = new ArrayList<>();
            for (TokenStatistic stat: tokenStatisticList) {
                viewList.add(TokenStatisticView.fromTokenStatistic(stat));
            }
            resultView.setContents(viewList);
            resultView.setTotal(result.getTotal());
        }
        return resultView;
    }

    @ApiOperation("get token aggregate info")
    @GetMapping("/{network}/info/{token}")
    public Result<TokenStatisticView> tokenInfoAggregate(@PathVariable("network") String network, @PathVariable(value = "token", required = true) String token) {
        Result<TokenStatistic> result = tokenService.tokenInfoAggregate(network, token);
        Result<TokenStatisticView> viewResult = new Result<>();
        if(result != null && !result.getContents().isEmpty()) {
            List<TokenStatistic> tokenStatisticList = result.getContents();
            List<TokenStatisticView> viewList = new ArrayList<>();
            for (TokenStatistic stat: tokenStatisticList) {
                viewList.add(TokenStatisticView.fromTokenStatistic(stat));
            }
            viewResult.setContents(viewList);
            viewResult.setTotal(result.getTotal());
        }
        return viewResult;
    }

    @ApiOperation("get token market cap")
    @GetMapping("/{network}/market_cap/{token}")
    public BigDecimal tokenMarketCap(@PathVariable("network") String network, @PathVariable(value = "token", required = true) String token) {
        return tokenService.getTokenMarketCap(network, token);
    }


    @ApiOperation("get token holders list")
    @GetMapping("/{network}/holders/page/{page}")
    public Result<TokenHolderInfo> getHoldersByToken(@PathVariable("network") String network, @PathVariable("page") int page,
                                                     @RequestParam(value = "count", required = false, defaultValue = "20") int count,
                                                     @RequestParam("token_type") String tokenType) throws IOException {
        return tokenService.getHoldersByToken(network, page, count, tokenType);
    }

}
