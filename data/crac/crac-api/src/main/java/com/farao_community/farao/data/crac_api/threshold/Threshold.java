/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api.threshold;

import com.farao_community.farao.commons.Unit;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface Threshold {

    Unit getUnit();

    default boolean limitsByMax() {
        return max().isPresent();
    }

    default boolean limitsByMin() {
        return min().isPresent();
    }

    Optional<Double> max();

    Optional<Double> min();
}
