/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.parameters;

import com.farao_community.farao.commons.Unit;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class MaxMinMarginParameters {
    public static final double DEFAULT_PST_PENALTY_COST = 0.01;

    protected final Unit unit;
    protected double pstPenaltyCost;

    public MaxMinMarginParameters(Unit unit, double pstPenaltyCost) {
        this.unit = unit;
        this.pstPenaltyCost = pstPenaltyCost;
    }

    public MaxMinMarginParameters(Unit unit) {
        this(unit, DEFAULT_PST_PENALTY_COST);
    }

    public final Unit getUnit() {
        return unit;
    }

    public final double getPstPenaltyCost() {
        return pstPenaltyCost;
    }
}
