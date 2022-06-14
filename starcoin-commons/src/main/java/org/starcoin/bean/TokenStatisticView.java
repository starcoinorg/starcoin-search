package org.starcoin.bean;

import com.alibaba.fastjson.annotation.JSONField;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class TokenStatisticView extends TokenStatistic {
    @JSONField(name = "market_cap_str")
    private String marketCapStr;

    @JSONField(name = "volume_str")
    private String volumeStr;

    public String getMarketCapStr() {
        return marketCapStr;
    }

    public void setMarketCapStr(String marketCapStr) {
        this.marketCapStr = marketCapStr;
    }

    public String getVolumeStr() {
        return volumeStr;
    }

    public void setVolumeStr(String volumeStr) {
        this.volumeStr = volumeStr;
    }

    public static TokenStatisticView fromTokenStatistic(TokenStatistic tokenStatistic) {
        TokenStatisticView view = new TokenStatisticView();
        view.setAddressHolder(tokenStatistic.getAddressHolder());
        view.setMarketCap(tokenStatistic.getMarketCap());
        view.setVolume(tokenStatistic.getVolume());
        view.setTypeTag(tokenStatistic.getTypeTag());
        view.setMarketCapStr(DecimalFormat.getNumberInstance().format(tokenStatistic.getMarketCap()));
        return view;
    }

    public static TokenStatisticView fromTokenVolume(BigDecimal volume, String token) {
        TokenStatisticView view = new TokenStatisticView();
        view.setTypeTag(token);
        view.setVolumeStr(volume.toString());
        return view;
    }

    @Override
    public String toString() {
        return "TokenStatisticView{" +
                "marketCapStr='" + marketCapStr + '\'' +
                ", volumeStr='" + volumeStr + '\'' +
                '}';
    }
}
