/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.iidm.network.TwoSides;

import java.util.*;

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

    private record BoundIndex(TwoSides twoSides, Bound bound, Unit unit) {
    }

    private final Map<BoundIndex, Double> boundsCache = new HashMap<>();

    public boolean isLowerBoundComputed(TwoSides side, Unit unit) {
        return boundsCache.containsKey(new BoundIndex(side, Bound.LOWER, unit));
    }

    public boolean isUpperBoundComputed(TwoSides side, Unit unit) {
        return boundsCache.containsKey(new BoundIndex(side, Bound.UPPER, unit));
    }

    public Double getLowerBound(TwoSides side, Unit unit) {
        if (!isLowerBoundComputed(side, unit)) {
            throw new OpenRaoException("Trying to access not computed bound");
        }
        return boundsCache.get(new BoundIndex(side, Bound.LOWER, unit));
    }

    public void setLowerBound(Double lowerBound, TwoSides side, Unit unit) {
        boundsCache.put(new BoundIndex(side, Bound.LOWER, unit), lowerBound);
    }

    public Double getUpperBound(TwoSides side, Unit unit) {
        if (!isUpperBoundComputed(side, unit)) {
            throw new OpenRaoException("Trying to access not computed bound");
        }
        return boundsCache.get(new BoundIndex(side, Bound.UPPER, unit));
    }

    public void setUpperBound(Double upperBound, TwoSides side, Unit unit) {
        boundsCache.put(new BoundIndex(side, Bound.UPPER, unit), upperBound);
    }
}
