/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.Side;

import java.util.*;

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

    private List<Boolean> boundsComputed = Arrays.asList(false, false, false, false, false, false, false, false);
    private List<Double> boundValues = Arrays.asList(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

    private static int getIndex(Side side, Unit unit, Bound bound) {
        if (unit.equals(Unit.AMPERE)) {
            return getAmpereIndex(side, bound);
        } else if (unit.equals(Unit.MEGAWATT)) {
            return getMegawattIndex(side, bound);
        } else {
            throw new UnsupportedOperationException(format("Unit %s not supported", unit));
        }
    }

    private static int getMegawattIndex(Side side, Bound bound) {
        if (bound.equals(Bound.LOWER)) {
            return side.equals(Side.LEFT) ? 0 : 1;
        } else {
            return side.equals(Side.LEFT) ? 2 : 3;
        }
    }

    private static int getAmpereIndex(Side side, Bound bound) {
        if (bound.equals(Bound.LOWER)) {
            return side.equals(Side.LEFT) ? 4 : 5;
        } else {
            return side.equals(Side.LEFT) ? 6 : 7;
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
            throw new OpenRaoException("Trying to access not computed bound");
        }
        return boundValues.get(getIndex(side, unit, Bound.LOWER));
    }

    public void setLowerBound(Double lowerBound, Side side, Unit unit) {
        boundValues.set(getIndex(side, unit, Bound.LOWER), lowerBound);
        boundsComputed.set(getIndex(side, unit, Bound.LOWER), true);
    }

    public Double getUpperBound(Side side, Unit unit) {
        if (!isUpperBoundComputed(side, unit)) {
            throw new OpenRaoException("Trying to access not computed bound");
        }
        return boundValues.get(getIndex(side, unit, Bound.UPPER));
    }

    public void setUpperBound(Double upperBound, Side side, Unit unit) {
        boundValues.set(getIndex(side, unit, Bound.UPPER), upperBound);
        boundsComputed.set(getIndex(side, unit, Bound.UPPER), true);
    }
}
