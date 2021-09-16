package org.starcoin.search.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.starcoin.api.OracleRPCClient;
import org.starcoin.bean.OracleTokenPair;
import org.starcoin.search.bean.OracleTokenPrice;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class OracleTokenPriceService {

    @Value("${starcoin.oracle.base.url}")
    private String oracleBaseUrl;

    @Value("${starcoin.network}")
    private String network;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    private static final Logger logger = LoggerFactory.getLogger(OracleTokenPriceService.class);

    private static final Map<String,Integer> statusMap = new HashMap<>();

    private static final Map<Integer,String> internalStatusMap = new HashMap<>();

    static {
        statusMap.put("CONFIRMED",1);
        statusMap.put("UPDATING",2);
        statusMap.put("SUBMITTED",3);

        internalStatusMap.put(1,"CONFIRMED");
        internalStatusMap.put(2,"UPDATING");
        internalStatusMap.put(3,"SUBMITTED");
    }

    public void fetchAndStoreOracleTokenPrice(){
        OracleRPCClient oracleRPCClient = new OracleRPCClient(oracleBaseUrl);
        try {
            List<OracleTokenPair> oracleTokenPairList = oracleRPCClient.getOracleTokenPair();
            if (oracleTokenPairList==null || oracleTokenPairList.size()==0)
                return ;
            List<Object[]> values = new ArrayList<>();
            for(OracleTokenPair tokenPair:oracleTokenPairList){
                int status =0 ;
                if(statusMap.containsKey(tokenPair.getOnChainStatus())){
                    status = statusMap.get(tokenPair.getOnChainStatus());
                }
                values.add(new Object[]{tokenPair.getPairName(),tokenPair.getLatestPrice(),tokenPair.getDecimals(),status,statusMap.get("onChainTransactionHash")});
            }
            jdbcTemplate.batchUpdate(String.format("insert into %s.oracle_token_price(token_pair_name,price,decimals,status,txn_hash) values(?,?,?,?,?)",network),values);
        } catch (IOException e) {
            logger.error("fetch oracle failed",e);
        }
    }

    public OracleTokenPrice getPriceByTimeRange(long startTimeStamp, long endTimeStamp){
        LocalDateTime startTs = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTimeStamp), ZoneId.systemDefault());
        LocalDateTime endTs = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTimeStamp), ZoneId.systemDefault());

        List<OracleTokenPair> oracleTokenPairList = jdbcTemplate.query(String.format("select * from %s.oracle_token_price where ts > ? and ts< ?",network),
                new OracleTokenPairRowMapper(),new Object[]{startTs,endTs});
        return new OracleTokenPrice(oracleTokenPairList);
    }

    class OracleTokenPairRowMapper implements RowMapper<OracleTokenPair> {

        @Override
        public OracleTokenPair mapRow(ResultSet rs, int rowNum) throws SQLException {

            OracleTokenPair oracleTokenPair = new OracleTokenPair();
            oracleTokenPair.setPairName(rs.getString("token_pair_name"));
            oracleTokenPair.setCreatedAt(rs.getTimestamp("ts").getTime());
            oracleTokenPair.setLatestPrice(rs.getBigDecimal("price").toBigInteger());
            oracleTokenPair.setDecimals(rs.getInt("decimals"));
            oracleTokenPair.setOnChainTransactionHash(rs.getString("txn_hash"));
            String status = "unknown";
            if(internalStatusMap.containsKey(rs.getInt("status"))){
                status = internalStatusMap.get(rs.getInt("status"));
            }
            oracleTokenPair.setOnChainStatus(status);

            return oracleTokenPair;
        }
    }

}
