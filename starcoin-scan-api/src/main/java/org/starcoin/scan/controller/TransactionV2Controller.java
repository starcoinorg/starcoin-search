package org.starcoin.scan.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.starcoin.api.Result;
import org.starcoin.bean.Event;
import org.starcoin.bean.PendingTransaction;
import org.starcoin.bean.TokenTransfer;
import org.starcoin.bean.Transfer;
import org.starcoin.scan.service.TransactionService;
import org.starcoin.scan.service.TransactionWithEvent;

import java.io.IOException;

@Api(tags = "transaction")
@RestController
@RequestMapping("v2/transaction")
public class TransactionV2Controller {
    @Autowired
    private TransactionService transactionService;

    @ApiOperation("get transaction by ID")
    @GetMapping("/{network}/{id}")
    public TransactionWithEvent getTransaction(@PathVariable("network") String network, @PathVariable("id") String id) throws IOException {
        return transactionService.get(network, id);
    }

    @ApiOperation("get transaction by hash")
    @GetMapping("/{network}/hash/{hash}")
    public TransactionWithEvent getTransactionByHash(@PathVariable("network") String network, @PathVariable("hash") String hash) throws IOException {
        return transactionService.getTransactionByHash(network, hash);
    }

    @ApiOperation("get transaction list")
    @GetMapping("/{network}/page/{page}")
    public Result<TransactionWithEvent> getRangeTransactions(@PathVariable("network") String network, @PathVariable("page") int page,
                                                             @RequestParam(value = "count", required = false, defaultValue = "20") int count,
                                                             @RequestParam(value = "start_height", required = false, defaultValue = "0") int startHeight,
                                                             @RequestParam(value = "txn_type", required = false, defaultValue = "1") int txnType) throws Exception {
        return transactionService.getRange(network, page, count, startHeight, txnType);
    }

    @ApiOperation("get transaction list by start time")
    @GetMapping("/{network}/start_time/")
    public Result<TransactionWithEvent> getByStartTime(@PathVariable("network") String network, @RequestParam(value = "start_time", required = false, defaultValue = "0") long start_time,
                                                       @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                                                       @RequestParam(value = "count", required = false, defaultValue = "20") int count,
                                                       @RequestParam(value = "txn_type", required = false, defaultValue = "1") int txnType) throws Exception {
        return transactionService.getTxnByStartTime(network, start_time, page, count, txnType);
    }

    @ApiOperation("get pending transaction list")
    @GetMapping("/pending_txns/{network}/page/{page}")
    public Result<PendingTransaction> getRangePendingTransactions(@PathVariable("network") String network, @PathVariable("page") int page,
                                                                  @RequestParam(value = "count", required = false, defaultValue = "20") int count,
                                                                  @RequestParam(value = "start_height", required = false, defaultValue = "0") int startHeight) throws Exception {
        return transactionService.getRangePendingTransaction(network, page, count, startHeight);
    }

    @ApiOperation("get pending transaction by ID")
    @GetMapping("/pending_txn/get/{network}/{id}")
    public PendingTransaction getPendingTransaction(@PathVariable("network") String network, @PathVariable("id") String id) throws IOException {
        return transactionService.getPending(network, id);
    }

    @ApiOperation("get transaction list by address")
    @GetMapping("{network}/byAddress/{address}")
    public Result<TransactionWithEvent> getRangeByAddressAlias(@PathVariable("network") String network, @PathVariable("address") String address,
                                                               @RequestParam(value = "count", required = false, defaultValue = "20") int count) throws IOException {
        return transactionService.getRangeByAddressAll(network, address, 1, count);
    }

    @ApiOperation("get transaction list of page range by address")
    @GetMapping("/address/{network}/{address}/page/{page}")
    public Result<TransactionWithEvent> getRangeByAddress(@PathVariable("network") String network, @PathVariable("address") String address, @PathVariable(value = "page", required = false) int page,
                                                          @RequestParam(value = "count", required = false, defaultValue = "20") int count) throws IOException {
        return transactionService.getRangeByAddressAll(network, address, page, count);
    }

    @ApiOperation("get nft transaction list of page range by address")
    @GetMapping("/nft/{network}/page/{page}")
    public Result<TransactionWithEvent> getNFTTxnsByAddress(@PathVariable("network") String network, @PathVariable(value = "page", required = false) int page,
                                                            @RequestParam(value = "start_time", required = false, defaultValue = "0") long start_time,
                                                            @RequestParam(value = "address", required = false, defaultValue = "") String address,
                                                          @RequestParam(value = "count", required = false, defaultValue = "20") int count) throws IOException {
        return transactionService.getNFTTxns(network, start_time, address, page, count);
    }

    @ApiOperation("get transaction by block")
    @GetMapping("/{network}/byBlock/{block_hash}")
    public Result<TransactionWithEvent> getByBlockHash(@PathVariable("network") String network, @PathVariable("block_hash") String blockHash) throws IOException {
        return transactionService.getByBlockHash(network, blockHash);
    }

    @ApiOperation("get transaction by block height")
    @GetMapping("/{network}/byBlockHeight/{block_height}")
    public Result<TransactionWithEvent> getByBlockHeight(@PathVariable("network") String network, @PathVariable("block_height") int blockHeight) throws IOException {
        return transactionService.getByBlockHeight(network, blockHeight);
    }

    @ApiOperation("get transaction events by tag")
    @GetMapping("{network}/events/byTag/{tag_name}/page/{page}")
    public Result<Event> getEventsByTag(@PathVariable("network") String network, @PathVariable("tag_name") String tag_name,
                                        @PathVariable(value = "page", required = false) int page,
                                        @RequestParam(value = "count", required = false, defaultValue = "20") int count) throws IOException {
        return transactionService.getEvents(network, tag_name, page, count);
    }

    @ApiOperation("get transfer by token")
    @GetMapping("{network}/transfer/byTag/{tag_name}/page/{page}")
    public Result<Transfer> getTransfers(@PathVariable("network") String network, @PathVariable("tag_name") String tag_name,
                                         @PathVariable(value = "page", required = false) int page,
                                         @RequestParam(value = "count", required = false, defaultValue = "20") int count) {
        return transactionService.getRangeTransfers(network, tag_name, null, null, page, count);
    }

    @ApiOperation("get transfers count by token")
    @GetMapping("{network}/transfer/count/byTag/{tag_name}")
    public Result<TokenTransfer> getTransferCount(@PathVariable("network") String network, @PathVariable("tag_name") String tag_name
    ) {
        return transactionService.getTransferCount(network, tag_name);
    }

    @ApiOperation("get transfer by sender")
    @GetMapping("{network}/transfer/sender/{sender}/page/{page}")
    public Result<Transfer> getTransfersBySender(@PathVariable("network") String network, @PathVariable("sender") String sender,
                                                 @PathVariable(value = "page", required = false) int page,
                                                 @RequestParam(value = "count", required = false, defaultValue = "20") int count) {
        return transactionService.getRangeTransfers(network, null, null, sender, page, count);
    }

    @ApiOperation("get transfer by receiver")
    @GetMapping("{network}/transfer/receiver/{receiver}/page/{page}")
    public Result<Transfer> getTransfersByReceiver(@PathVariable("network") String network, @PathVariable("receiver") String receiver,
                                                   @PathVariable(value = "page", required = false) int page,
                                                   @RequestParam(value = "count", required = false, defaultValue = "20") int count) {
        return transactionService.getRangeTransfers(network, null, receiver, null, page, count);
    }

    @ApiOperation("get transfer by sender and receiver")
    @GetMapping("{network}/transfer/conversations/page/{page}")
    public Result<Transfer> getTransferConversations(@PathVariable("network") String network, @RequestParam("receiver") String receiver,
                                                     @RequestParam("sender") String sender,
                                                     @PathVariable(value = "page", required = false) int page,
                                                     @RequestParam(value = "count", required = false, defaultValue = "20") int count) {
        return transactionService.getRangeTransfers(network, null, receiver, sender, page, count);
    }

}