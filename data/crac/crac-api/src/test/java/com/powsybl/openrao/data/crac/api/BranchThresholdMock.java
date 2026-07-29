/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.threshold.BranchThreshold;

import java.util.Optional;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class BranchThresholdMock implements BranchThreshold {
    private final TwoSides side;
    private final Unit unit;
    private final double min;
    private final double max;

    public BranchThresholdMock(TwoSides side, Unit unit, double min, double max) {
        this.side = side;
        this.unit = unit;
        this.min = min;
        this.max = max;
    }

    @Override
    public TwoSides getSide() {
        return side;
    }

    @Override
    public Unit getUnit() {
        return unit;
    }

    @Override
    public Optional<Double> max() {
        return Optional.of(min);
    }

    @Override
    public Optional<Double> min() {
        return Optional.of(max);
    }
}
