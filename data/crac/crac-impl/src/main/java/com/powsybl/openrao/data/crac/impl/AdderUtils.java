/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;

import java.util.Collection;
import java.util.Objects;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class AdderUtils {

    private AdderUtils() {
    }

    static void assertAttributeNotNull(Object attribute, String className, String attributeDescription, String methodName) {
        if (Objects.isNull(attribute)) {
            throw new OpenRaoException(String.format("Cannot add %s without a %s. Please use %s with a non null value", className, attributeDescription, methodName));
            // example: "Cannot add a PstRangeAction without a maximum value. Please use setMaxValue()."
        }
    }

    static void assertAttributeNotEmpty(Collection<?> attribute, String className, String attributeDescription, String methodName) {
        if (attribute.isEmpty()) {
            throw new OpenRaoException(String.format("Cannot add %s without a %s. Please use %s", className, attributeDescription, methodName));
            // example: "Cannot add a InjectionShiftRangeAction without an injectionShiftKey. Please use withNetworkElementAndKey()."
        }
    }
}
