package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;

import java.util.Objects;

public final class AdderUtils {

    private AdderUtils() {
    }

    // todo: make class private package once all java file are on the same level
    public static String missingAttributeError(String className, String attributeDescription, String methodName) {
        return String.format("Cannot add %s without a %s. Please use %s", className, attributeDescription, methodName);
    }

    public static void assertAttributeNotNull(Object attribute, String className, String attributeDescription, String methodName) {
        if (Objects.isNull(attribute)) {
            throw new FaraoException(String.format("Cannot add %s without a %s. Please use %s with a non null value", className, attributeDescription, methodName));
            // example: "Cannot add a PstRangeAction without a maximum value. Please use setMaxValue."
        }
    }
}
