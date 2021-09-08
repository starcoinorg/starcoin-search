package org.starcoin.search.utils;

import org.starcoin.types.StructTag;

public class StructTagUtil {

    public static final String structTagToString(StructTag structTag){
        StringBuilder sb = new StringBuilder();
        sb.append(structTag.address.toString());
        sb.append("::");
        sb.append(structTag.module.value);
        sb.append("::");
        sb.append(structTag.name.value);
        return sb.toString();
    }

    public static final String structTagToSwapUsdtPair(StructTag structTag){
        return String.format("%s / USD",structTag.name.value);
    }

    public static final String structTagsToTokenPair(StructTag structTagFirst,StructTag structTagSecond){
        String tagFirst = structTagToString(structTagFirst);
        String tagSecond = structTagToString(structTagSecond);

        if(tagFirst.compareTo(tagSecond)<0){
            return tagFirst+"/"+tagSecond;
        }else {
            return tagSecond+"/"+tagFirst;
        }
    }
}
