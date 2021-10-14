package org.starcoin.scan.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.starcoin.api.Result;
import org.starcoin.bean.Block;
import org.starcoin.bean.UncleBlock;
import org.starcoin.scan.service.BlockService;

@Api(tags = "block")
@RestController
@RequestMapping("v2/block")
public class BlockV2Controller {
    @Autowired
    private BlockService blockService;

    @ApiOperation("get block by ID")
    @GetMapping("/{network}/")
    public Block getBlock(@PathVariable("network") String network, @RequestParam String id) throws Exception {
        return blockService.getBlock(network, id);
    }

    @ApiOperation("get block by hash")
    @GetMapping("/{network}/hash/{hash}")
    public Block getBlockByHash(@PathVariable("network") String network, @PathVariable("hash") String hash) {
        return blockService.getBlockByHash(network, hash);
    }

    @ApiOperation("get block by height")
    @GetMapping("/{network}/height/{height}")
    public Block getBlockByHeight(@PathVariable("network") String network, @PathVariable("height") long height) {
        return blockService.getBlockByHeight(network, height);
    }

    @ApiOperation("get block list")
    @GetMapping("/{network}/page/{page}")
    public Result<Block> getRangeBlocks(@PathVariable("network") String network, @PathVariable("page") int page,
                                        @RequestParam(value = "count", required = false, defaultValue = "20") int count
    ) {
        return blockService.getRange(network, page, count, 0);
    }

    @ApiOperation("get block list by start height")
    @GetMapping("/{network}/start_height/")
    public Result<Block> getBlocksStartWith(@PathVariable("network") String network, @RequestParam(value = "start_height", required = false, defaultValue = "0") long start_height,
                                            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                                            @RequestParam(value = "count", required = false, defaultValue = "20") int count
    ) {
        return blockService.getBlocksStartWith(network, start_height, page, count);
    }

    @ApiOperation("get uncle block list")
    @GetMapping("/{network}/uncle/page/{page}")
    public Result<UncleBlock> getRangeUncleBlocks(@PathVariable("network") String network, @PathVariable("page") int page,
                                                  @RequestParam(value = "count", required = false, defaultValue = "20") int count,
                                                  @RequestParam(value = "total", required = false, defaultValue = "0") int start_height) {
        return blockService.getUnclesRange(network, page, count, start_height);
    }

    @ApiOperation("get uncle block by height")
    @GetMapping("/{network}/uncle/height/{height}")
    public UncleBlock getUncleBlockByHeight(@PathVariable("network") String network, @PathVariable("height") long height) {
        return blockService.getUncleBlockByHeight(network, height);
    }

    @ApiOperation("get uncle block by hash")
    @GetMapping("/{network}/uncle/hash/{hash}")
    public UncleBlock getUncleBlockByHash(@PathVariable("network") String network, @PathVariable("hash") String hash) {
        return blockService.getUncleBlockByHash(network, hash);
    }
}
