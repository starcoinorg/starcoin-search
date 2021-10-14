package org.starcoin.utils;

import org.starcoin.types.StructTag;

public class StructTagUtil {

    public static String structTagToString(StructTag structTag) {
        return structTag.address.toString() +
                "::" +
                structTag.module.value +
                "::" +
                structTag.name.value;
    }

    public static String structTagToSwapUsdtPair(StructTag structTag) {
        return String.format("%s / USD", structTag.name.value);
    }

    public static String structTagsToTokenPair(StructTag structTagFirst, StructTag structTagSecond) {
        String tagFirst = structTagToString(structTagFirst);
        String tagSecond = structTagToString(structTagSecond);

        if (tagFirst.compareTo(tagSecond) < 0) {
            return tagFirst + "/" + tagSecond;
        } else {
            return tagSecond + "/" + tagFirst;
        }
    }
}
