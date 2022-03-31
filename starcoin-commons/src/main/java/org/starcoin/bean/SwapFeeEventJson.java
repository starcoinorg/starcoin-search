package org.starcoin.bean;

import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class SwapFeeEventJson {

    @JSONField(name = "x_token_code")
    private TypeTag xTokenCode;
    @JSONField(name = "swap_fee")
    private long swapFee;
    @JSONField(name = "y_token_code")
    private TypeTag yTokenCode;
    @JSONField(name = "fee_out")
    private long feeOut;
    @JSONField(name = "fee_addree")
    private String feeAddree;
    @JSONField(name = "signer")
    private String signer;

    public TypeTag getxTokenCode() {
        return xTokenCode;
    }

    public void setxTokenCode(TypeTag xTokenCode) {
        this.xTokenCode = xTokenCode;
    }

    public long getSwapFee() {
        return swapFee;
    }

    public void setSwapFee(long swapFee) {
        this.swapFee = swapFee;
    }

    public TypeTag getyTokenCode() {
        return yTokenCode;
    }

    public void setyTokenCode(TypeTag yTokenCode) {
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

class TypeTag {
    @JSONField(name = "name")
    private String name;
    @JSONField(name = "module_name")
    private String moduleName;
    @JSONField(name = "addr")
    private String addr;

    public String uniform() {
        String module = moduleName;
        if(module.startsWith("0x")) {
            try {
                module = new String(Hex.decodeHex(module.substring(2)));
            } catch (DecoderException e) {
                e.printStackTrace();
            }
        }
        String tempName = name;
        if(tempName.startsWith("0x")) {
            try {
                tempName = new String(Hex.decodeHex(tempName.substring(2)));
            } catch (DecoderException e) {
                e.printStackTrace();
            }
        }
        return this.addr + "::" + module + "::" + tempName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }
}