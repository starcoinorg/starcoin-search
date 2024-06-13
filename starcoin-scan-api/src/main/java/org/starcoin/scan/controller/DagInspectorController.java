package org.starcoin.scan.controller;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.starcoin.bean.Block;
import org.starcoin.scan.service.DagInspectorService;

@Api(tags = "dag-inspector")
@RestController
@RequestMapping("v2/dag-inspector")
public class DagInspectorController {

    @Autowired
    DagInspectorService dagInspectorService;

    @GetMapping("/{network}/")
    public Block getBlock(@PathVariable("network") String network, @RequestParam String id) throws Exception {
        // return dagInspectorService.getBlock(network, id);
        return null;
    }
}
