package org.starcoin.search.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.starcoin.search.bean.TokenPoolStat;
import org.starcoin.search.bean.TokenStat;

@Component
public class SwapStatService {

    private static final Logger logger = LoggerFactory.getLogger(SwapStatService.class);
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Value("${starcoin.network}")
    private String network;

    public void persistTokenStatInfo(TokenStat tokenStat) {
        jdbcTemplate.update(String.format("insert into %s.token_swap_stat values(?,?,?,?,?)", network), new Object[]{tokenStat.getToken(),
                tokenStat.getVolumeAmount(),
                tokenStat.getVolume(),
                tokenStat.getTvlAmount(),
                tokenStat.getTvl()
        });
    }

    public void persistTokenPoolStatInfo(TokenPoolStat tokenPoolStat) {
        jdbcTemplate.update(String.format("insert into %s.token_pool_swap_stat values(?,?,?,?,?,?,?,?,?,?)", network), new Object[]{tokenPoolStat.getxStats().getToken(),
                tokenPoolStat.getyStats().getToken(),
                tokenPoolStat.getxStats().getVolumeAmount(),
                tokenPoolStat.getxStats().getVolume(),
                tokenPoolStat.getxStats().getTvlAmount(),
                tokenPoolStat.getxStats().getTvl(),
                tokenPoolStat.getyStats().getVolumeAmount(),
                tokenPoolStat.getyStats().getVolume(),
                tokenPoolStat.getyStats().getTvlAmount(),
                tokenPoolStat.getyStats().getTvl(),
        });
    }

}
