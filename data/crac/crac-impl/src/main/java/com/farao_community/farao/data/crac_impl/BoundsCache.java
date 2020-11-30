/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.Unit;

import java.util.Arrays;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class BoundsCache {

    private boolean[] boundsComputed = {false, false, false, false};
    private Double[] boundValues = new Double[4];

    public boolean isLowerBoundComputed(Unit unit) {
        if (unit.equals(Unit.AMPERE)) {
            return boundsComputed[0];
        } else {
            return boundsComputed[1];
        }
    }

    public boolean isUpperBoundComputed(Unit unit) {
        if (unit.equals(Unit.AMPERE)) {
            return boundsComputed[2];
        } else {
            return boundsComputed[3];
        }
    }

    public Double getLowerBound(Unit unit) {
        if (unit.equals(Unit.AMPERE) && isLowerBoundComputed(unit)) {
            return boundValues[0];
        } else if (unit.equals(Unit.MEGAWATT) && isLowerBoundComputed(unit)) {
            return boundValues[1];
        } else {
            return null;
        }
    }

    public void setLowerBound(Double lowerBound, Unit unit) {
        if (unit.equals(Unit.AMPERE)) {
            boundsComputed[0] = true;
            boundValues[0] = lowerBound;
        } else {
            boundsComputed[1] = true;
            boundValues[1] = lowerBound;
        }
    }

    public Double getUpperBound(Unit unit) {
        if (unit.equals(Unit.AMPERE) && isUpperBoundComputed(unit)) {
            return boundValues[2];
        } else if (unit.equals(Unit.MEGAWATT) && isUpperBoundComputed(unit)) {
            return boundValues[3];
        } else {
            return null;
        }
    }

    public void setUpperBound(Double upperBound, Unit unit) {
        if (unit.equals(Unit.AMPERE)) {
            boundsComputed[2] = true;
            boundValues[2] = upperBound;
        } else {
            boundsComputed[3] = true;
            boundValues[3] = upperBound;
        }
    }

    public void resetBounds() {
        Arrays.fill(boundsComputed, false);
    }
}
