/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cim.parameters;

import java.util.Objects;

/**
 * A class that maps rangeActionId to speed
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class RangeActionSpeed {
    private final String rangeActionId;
    private final Integer speed;

    public RangeActionSpeed(String rangeActionId, Integer speed) {
        this.rangeActionId = rangeActionId;
        this.speed = speed;
    }

    public String getRangeActionId() {
        return rangeActionId;
    }

    public Integer getSpeed() {
        return speed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RangeActionSpeed that = (RangeActionSpeed) o;
        return Objects.equals(rangeActionId, that.rangeActionId) && Objects.equals(speed, that.speed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rangeActionId, speed);
    }
}
