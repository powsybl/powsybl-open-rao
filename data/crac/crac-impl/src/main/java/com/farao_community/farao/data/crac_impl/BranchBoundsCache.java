/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Side;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

/**
 * Object that stores bounds of a BranchCnec. It enables to avoid computing these values several times when they are
 * not supposed to change along the optimization.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class BranchBoundsCache {

    private enum Bound {
        LOWER,
        UPPER
    }

    private List<Boolean> boundsComputed = Arrays.asList(false, false, false, false, false, false);
    private List<Double> boundValues = Arrays.asList(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

    private static int getIndex(Side side, Unit unit, Bound bound) {
        if (unit.equals(Unit.AMPERE)) {
            if (side.equals(Side.LEFT)) {
                if (bound.equals(Bound.LOWER)) {
                    return 0;
                } else {
                    return 3;
                }
            } else if (side.equals(Side.RIGHT)) {
                if (bound.equals(Bound.LOWER)) {
                    return 1;
                } else {
                    return 4;
                }
            } else {
                throw new UnsupportedOperationException(format("Side %s not supported", side));
            }
        } else if (unit.equals(Unit.MEGAWATT)) {
            if (bound.equals(Bound.LOWER)) {
                return 2;
            } else {
                return 5;
            }
        } else {
            throw new UnsupportedOperationException(format("Unit %s not supported", unit));
        }
    }

    public boolean isLowerBoundComputed(Side side, Unit unit) {
        return boundsComputed.get(getIndex(side, unit, Bound.LOWER));
    }

    public boolean isUpperBoundComputed(Side side, Unit unit) {
        return boundsComputed.get(getIndex(side, unit, Bound.UPPER));
    }

    public Double getLowerBound(Side side, Unit unit) {
        if (!isLowerBoundComputed(side, unit)) {
            throw new FaraoException("Trying to access not computed bound");
        }
        return boundValues.get(getIndex(side, unit, Bound.LOWER));
    }

    public void setLowerBound(Double lowerBound, Side side, Unit unit) {
        boundValues.set(getIndex(side, unit, Bound.LOWER), lowerBound);
        boundsComputed.set(getIndex(side, unit, Bound.LOWER), true);
    }

    public Double getUpperBound(Side side, Unit unit) {
        if (!isUpperBoundComputed(side, unit)) {
            throw new FaraoException("Trying to access not computed bound");
        }
        return boundValues.get(getIndex(side, unit, Bound.UPPER));
    }

    public void setUpperBound(Double upperBound, Side side, Unit unit) {
        boundValues.set(getIndex(side, unit, Bound.UPPER), upperBound);
        boundsComputed.set(getIndex(side, unit, Bound.UPPER), true);
    }

    public void resetBounds() {
        Collections.fill(boundsComputed, false);
    }
}
