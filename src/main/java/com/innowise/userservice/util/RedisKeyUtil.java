package com.innowise.userservice.util;

import java.util.UUID;

public final class RedisKeyUtil {

    private static final String PREFIX = "user-service";
    private static final String SEPARATOR = ":";

    public static String getUserKey(UUID userId) {
        return PREFIX + SEPARATOR + "user" + SEPARATOR + userId.toString();
    }

    public static String getUserKeyPrefix() {
        return PREFIX + SEPARATOR + "user" + SEPARATOR;
    }

    private RedisKeyUtil() {
    }
}
