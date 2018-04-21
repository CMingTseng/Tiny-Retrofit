package com.github.ganquan.tiny.retrofit.internal;

public final class Preconditions {

    public static <T> T checkNotNull(T value, String message) {
        if (value == null) {
            throw new NullPointerException(message);
        }
        return value;
    }

    public static <T> T checkNotNull(T value) {
        if (value == null) {
            throw new NullPointerException(value.getClass().getSimpleName() + " which can not be null");
        }
        return value;
    }

    private Preconditions() {
        throw new AssertionError("No instances.");
    }
}
