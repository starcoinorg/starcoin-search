package org.starcoin.scan.controller;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.starcoin.scan.service.DagInspectorService;
import org.starcoin.scan.service.vo.DIAppConfigVo;
import org.starcoin.scan.service.vo.DIBlocksAndEdgesAndHeightGroupsVo;

import java.util.List;

@Api(tags = "dag-inspector")
@RestController
@RequestMapping("v2/dag-inspector")
public class DagInspectorController {

    @Autowired
    DagInspectorService dagInspectorService;

    @GetMapping("/{network}/blocksBetweenHeights")
    public DIBlocksAndEdgesAndHeightGroupsVo getBlocksBetweenHeights(
            @PathVariable("network") String network,
            @RequestParam Long startHeight,
            @RequestParam Long endHeight
    ) throws Exception {
        return dagInspectorService.getBlocksAndEdgesAndHeightGroups(network, startHeight, endHeight);
    }

    @GetMapping("/{network}/header")
    public DIBlocksAndEdgesAndHeightGroupsVo header(
            @PathVariable("network") String network,
            @RequestParam Long heightDifferent
    ) throws Exception {
        return dagInspectorService.getHead(network, heightDifferent);
    }

    @GetMapping("/{network}/blockHash")
    public DIBlocksAndEdgesAndHeightGroupsVo getBlockHash(
            @PathVariable("network") String network,
            @RequestParam String blockHash,
            @RequestParam Long heightDifference
    ) throws Exception {
        return dagInspectorService.getBlockHash(network, blockHash, heightDifference);
    }

    @GetMapping("/{network}/blockDAAScore")
    public DIBlocksAndEdgesAndHeightGroupsVo getBlockDAAScore(
            @PathVariable("network") String network,
            @RequestParam Long daaScore,
            @RequestParam Long heightDifference
    ) throws Exception {
        return dagInspectorService.getBlockDAAScore(network, daaScore, heightDifference);
    }

    @GetMapping("/{network}/appConfig")
    public DIAppConfigVo getAppConfig(
            @PathVariable("network") String network
    ) throws Exception {
        return dagInspectorService.getAppConfig(network);
    }
}
