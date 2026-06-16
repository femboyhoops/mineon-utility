package com.hoops.oretracker.client;

import java.math.BigInteger;

public record OreValue(
        int normal,
        int compressed,
        int superCompressed,
        int ultraCompressed,
        int megaCompressed,
        int hyperCompressed
) {
    public static final BigInteger NORMAL_VALUE = BigInteger.ONE;
    public static final BigInteger COMPRESSED_VALUE = BigInteger.valueOf(64L);
    public static final BigInteger SUPER_COMPRESSED_VALUE = BigInteger.valueOf(64L).pow(2);
    public static final BigInteger ULTRA_COMPRESSED_VALUE = BigInteger.valueOf(64L).pow(3);
    public static final BigInteger MEGA_COMPRESSED_VALUE = BigInteger.valueOf(64L).pow(4);
    public static final BigInteger HYPER_COMPRESSED_VALUE = BigInteger.valueOf(64L).pow(5);

    public BigInteger toBaseValue() {
        return BigInteger.ZERO
                .add(NORMAL_VALUE.multiply(BigInteger.valueOf(normal)))
                .add(COMPRESSED_VALUE.multiply(BigInteger.valueOf(compressed)))
                .add(SUPER_COMPRESSED_VALUE.multiply(BigInteger.valueOf(superCompressed)))
                .add(ULTRA_COMPRESSED_VALUE.multiply(BigInteger.valueOf(ultraCompressed)))
                .add(MEGA_COMPRESSED_VALUE.multiply(BigInteger.valueOf(megaCompressed)))
                .add(HYPER_COMPRESSED_VALUE.multiply(BigInteger.valueOf(hyperCompressed)));
    }

    public static OreValue empty() {
        return new OreValue(0, 0, 0, 0, 0, 0);
    }
}


