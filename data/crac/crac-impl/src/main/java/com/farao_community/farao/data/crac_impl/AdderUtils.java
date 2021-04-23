/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;

import java.util.Objects;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class AdderUtils {

    private AdderUtils() {
    }

    static void assertAttributeNotNull(Object attribute, String className, String attributeDescription, String methodName) {
        if (Objects.isNull(attribute)) {
            throw new FaraoException(String.format("Cannot add %s without a %s. Please use %s with a non null value", className, attributeDescription, methodName));
            // example: "Cannot add a PstRangeAction without a maximum value. Please use setMaxValue."
        }
    }
}
