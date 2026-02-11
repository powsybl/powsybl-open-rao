/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.openrao.commons.OpenRaoException;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MinAndMax<T extends Comparable<T>> {
    private T min;
    private T max;

    public MinAndMax(@Nullable T min, @Nullable T max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new OpenRaoException("Min should be smaller than max!");
        }
        this.min = min;
        this.max = max;
    }

    public Optional<T> getMin() {
        return Optional.ofNullable(min);
    }

    public Optional<T> getMax() {
        return Optional.ofNullable(max);
    }

}
