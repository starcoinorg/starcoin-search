package org.starcoin.indexer.handler;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.api.TransactionRPCClient;
import org.starcoin.bean.Event;
import org.starcoin.bean.SwapFeeEvent;
import org.starcoin.bean.SwapFeeEventJson;
import org.starcoin.indexer.service.SwapEventService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class SwapEventHandle {
    private static final String TYPE_TAG = "0x8c109349c6bd91411d6bc962e080c4a3::TokenSwapFee::SwapFeeEvent";
    private static final Logger logger = LoggerFactory.getLogger(SwapEventHandle.class);
    @Autowired
    private TransactionRPCClient transactionRPCClient;

    @Autowired
    private SwapEventService swapEventService;

    public void handle() {
        //read offset
        long offset = swapEventService.getOffset();

        try {
            //read from node
            long toNumber = offset + 32;
            List<Event> eventList = transactionRPCClient.getEvents(offset, toNumber,
                    null, null, Collections.singletonList(TYPE_TAG), null);
            if(eventList != null) {
                List<SwapFeeEvent> swapFeeEventList = new ArrayList<>();
                long blockNumber = 0;
                for (Event event: eventList) {
                    SwapFeeEventJson eventJson = JSON.parseObject(event.getDecodeEventData(), SwapFeeEventJson.class);
                    swapFeeEventList.add(SwapFeeEvent.fromJson(eventJson));
                    long tempNumber = Long.parseLong(event.getBlockNumber());
                    if( tempNumber > blockNumber) {
                        blockNumber = tempNumber;
                    }
                }
                swapEventService.saveAllFeeEvent(swapFeeEventList);
                logger.info("handle swap event ok: " + offset);
            }else {
                logger.warn("get events from node is null: " + offset);
            }
            //set new offset
            swapEventService.updateOffset(toNumber);
            logger.info("update swap handle offset: " + toNumber);
        } catch (Exception e) {
            logger.error("handle swap event error:", e);
        }
    }

}