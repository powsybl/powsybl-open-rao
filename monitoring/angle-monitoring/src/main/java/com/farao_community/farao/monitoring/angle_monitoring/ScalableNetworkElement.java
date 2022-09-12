/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.angle_monitoring;

import java.util.Objects;

/**
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class ScalableNetworkElement {
    private final String id;
    private final Float percentage;
    private final ScalableType scalableType;

    public enum ScalableType {
        GENERATOR,
        LOAD,
        UNHANDLED
    }

    ScalableNetworkElement(String id, Float percentage, ScalableType scalableType) {
        this.id = Objects.requireNonNull(id);
        this.percentage = Objects.requireNonNull(percentage);
        this.scalableType = Objects.requireNonNull(scalableType);
    }

    public ScalableType getScalableType() {
        return scalableType;
    }

    public Float getPercentage() {
        return percentage;
    }

    public String getId() {
        return id;
    }
}
