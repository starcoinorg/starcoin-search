package org.starcoin.bean;

import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class HexTypeTag {
    @JSONField(name = "name")
    private String name;
    @JSONField(name = "module_name")
    private String moduleName;
    @JSONField(name = "addr")
    private String addr;

    public HexTypeTag(String name, String moduleName, String addr) {
        this.name = name;
        this.moduleName = moduleName;
        this.addr =addr;
    }

    public static HexTypeTag fromString( String source) {
        if(source != null) {
            String[] tags = source.split("::");
            if(tags.length == 3) {
                HexTypeTag  hexTypeTag = new HexTypeTag(tags[2], tags[1],tags[0]);
                return hexTypeTag;
            }
        }
        return null;
    }

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
