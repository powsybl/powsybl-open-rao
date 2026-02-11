/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Indicates what elements to simulate as contingencies (N-1).
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class Contingencies extends AbstractCountriesFilter {
    private MinAndMax<Double> minAndMaxV = new MinAndMax<>(null, null);

    public Optional<Double> getMinV() {
        return minAndMaxV.getMin();
    }

    public Optional<Double> getMaxV() {
        return minAndMaxV.getMax();
    }

    /**
     * Set the voltage thresholds (in kV, included) to consider branches as critical contingencies (N-1).
     * You can use {@code null} to disable min and/or max filter.
     * By default, this filter is disabled.
     */
    public void setMinAndMaxV(@Nullable Double minV, @Nullable Double maxV) {
        this.minAndMaxV = new MinAndMax<>(minV, maxV);
    }
}
