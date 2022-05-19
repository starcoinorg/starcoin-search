package org.starcoin.bean;

import com.alibaba.fastjson.annotation.JSONField;

import java.text.DecimalFormat;

public class TokenStatisticView extends TokenStatistic {
    @JSONField(name = "market_cap_str")
    private String marketCapStr;

    public String getMarketCapStr() {
        return marketCapStr;
    }

    public void setMarketCapStr(String marketCapStr) {
        this.marketCapStr = marketCapStr;
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

    @Override
    public String toString() {
        return "TokenStatisticView{" +
                "marketCapStr='" + marketCapStr + '\'' +
                '}';
    }
}
