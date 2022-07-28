package org.starcoin.utils;

import java.math.BigInteger;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

public class KeyUtils {
    private static final BigInteger HALF = BigInteger.ONE.shiftLeft(64); // 2^64
    private static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);

    private static final BigInteger BASE = BigInteger.valueOf(62);
    private static final String DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static Function<BigInteger, BigInteger> toUnsigned = value
            -> value.signum() < 0 ? value.add(HALF) : value;
    private static Function<BigInteger, BigInteger> toSigned =
            value -> MAX_LONG.compareTo(value) < 0 ? value.subtract(HALF) : value;

    public static String base62Encode() {
        return base62Encode(UUID.randomUUID()).toUpperCase(Locale.ROOT);
    }

    public static String base62Encode(UUID uuid) {
        BigInteger pair = toBigInteger(uuid);
        return base62Encode(pair);
    }

    static BigInteger pair(BigInteger hi, BigInteger lo) {
        BigInteger unsignedLo = toUnsigned.apply(lo);
        BigInteger unsignedHi = toUnsigned.apply(hi);
        return unsignedLo.add(unsignedHi.multiply(HALF));
    }

    static BigInteger toBigInteger(UUID uuid) {
        return pair(
                BigInteger.valueOf(uuid.getMostSignificantBits()),
                BigInteger.valueOf(uuid.getLeastSignificantBits())
        );
    }

    static String base62Encode(BigInteger number) {
        if (number.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("number must not be negative");
        }
        StringBuilder result = new StringBuilder();
        while (number.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divmod = number.divideAndRemainder(BASE);
            number = divmod[0];
            int digit = divmod[1].intValue();
            result.insert(0, DIGITS.charAt(digit));
        }
        return (result.length() == 0) ? DIGITS.substring(0, 1) : result.toString();
    }



}
