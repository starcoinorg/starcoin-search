package org.starcoin.indexer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.starcoin.bean.HandleOffset;
import org.starcoin.bean.OracleTokenPair;
import org.starcoin.bean.PoolFeeStat;
import org.starcoin.bean.SwapFeeEvent;
import org.starcoin.indexer.repository.HandleOffsetRepository;
import org.starcoin.indexer.repository.SwapFeeEventRepository;
import org.starcoin.utils.SwapApiClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.starcoin.constant.Constant.STC_TOKEN_OR_TAG;

@Service
public class SwapEventService {
    private static final Logger logger = LoggerFactory.getLogger(SwapEventService.class);
    private static final String SWAP_EVENT_HANDLE_OFFSET = "SWAP_EVENT_OFFSET";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("YYYY-MM-dd");
    @Autowired
    private HandleOffsetRepository handleOffsetRepository;
    @Autowired
    private SwapFeeEventRepository swapFeeEventRepository;
    @Autowired
    private SwapApiClient swapApiClient;
    @Value("${starcoin.network}")
    private String network;

    public long getOffset() {
        HandleOffset offset = null;
        try {
            offset = handleOffsetRepository.getByOffsetId(SWAP_EVENT_HANDLE_OFFSET);
            if (offset != null)
            {
                return offset.getOffset();
            }else {
                //init offset
                offset = new HandleOffset(SWAP_EVENT_HANDLE_OFFSET, System.currentTimeMillis(), 0);
                handleOffsetRepository.save(offset);
                return 0;
            }
        } catch (Exception e) {
            logger.error("get offset err:", e);
            return 0;
        }
    }

    public void updateOffset(long offset) {
        HandleOffset newOffset = new HandleOffset(SWAP_EVENT_HANDLE_OFFSET, offset);
        handleOffsetRepository.save(newOffset);
    }

    public void saveFeeEvent(SwapFeeEvent event) {
        if(event != null) {
            swapFeeEventRepository.save(event);
        }
    }

    public void saveAllFeeEvent(List<SwapFeeEvent> swapFeeEventList) {
        if(swapFeeEventList != null && swapFeeEventList.size() > 0) {
            swapFeeEventRepository.saveAll(swapFeeEventList);
        }
    }

    public List<PoolFeeStat> getFeeStat(Date statDate) {
        List<SwapFeeEvent> feeEvents = swapFeeEventRepository.sumPoolFeeList(statDate);
        if(feeEvents != null && feeEvents.size() > 0) {
            List<PoolFeeStat> poolFeeStatList = new ArrayList<>();
            //get price
            OracleTokenPair oracleTokenPair = null;
            try {
                oracleTokenPair = swapApiClient.getProximatePriceRound(network, STC_TOKEN_OR_TAG, String.valueOf(statDate.getTime()));
            } catch (IOException e) {
                logger.error("get price error:", e);
            }
            BigDecimal price = new BigDecimal(0);
            if(oracleTokenPair != null) {
                price = new BigDecimal(oracleTokenPair.getPrice());
                price = price.movePointLeft(oracleTokenPair.getDecimals());
            }
            for (SwapFeeEvent fee : feeEvents) {
                PoolFeeStat poolFeeStat = fee.toPoolFeeStat();
                if(!price.equals(new BigDecimal(0))) {
                    poolFeeStat.setFees(price.multiply(poolFeeStat.getFeesAmount()));
                }
                poolFeeStatList.add(poolFeeStat);
            }
            return poolFeeStatList;
        }
        return null;
    }
}
