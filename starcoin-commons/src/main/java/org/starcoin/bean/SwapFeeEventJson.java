package org.starcoin.bean;

import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class SwapFeeEventJson {

    @JSONField(name = "x_token_code")
    private HexTypeTag xTokenCode;
    @JSONField(name = "swap_fee")
    private long swapFee;
    @JSONField(name = "y_token_code")
    private HexTypeTag yTokenCode;
    @JSONField(name = "fee_out")
    private long feeOut;
    @JSONField(name = "fee_addree")
    private String feeAddree;
    @JSONField(name = "signer")
    private String signer;

    public HexTypeTag getxTokenCode() {
        return xTokenCode;
    }

    public void setxTokenCode(HexTypeTag xTokenCode) {
        this.xTokenCode = xTokenCode;
    }

    public long getSwapFee() {
        return swapFee;
    }

    public void setSwapFee(long swapFee) {
        this.swapFee = swapFee;
    }

    public HexTypeTag getyTokenCode() {
        return yTokenCode;
    }

    public void setyTokenCode(HexTypeTag yTokenCode) {
        this.yTokenCode = yTokenCode;
    }

    public long getFeeOut() {
        return feeOut;
    }

    public void setFeeOut(long feeOut) {
        this.feeOut = feeOut;
    }

    public String getFeeAddree() {
        return feeAddree;
    }

    public void setFeeAddree(String feeAddree) {
        this.feeAddree = feeAddree;
    }

    public String getSigner() {
        return signer;
    }

    public void setSigner(String signer) {
        this.signer = signer;
    }
}