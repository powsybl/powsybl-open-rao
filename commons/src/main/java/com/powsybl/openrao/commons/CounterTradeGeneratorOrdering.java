/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Enrico Bettiol {@literal <enrico.bettiol at artelys.com>}
 */
public record CounterTradeGeneratorOrdering(
        List<GlskGeneratorAllocation> upwardOrder,
        List<GlskGeneratorAllocation> downwardOrder,
        int upwardLastStage,
        double maxUpwardMagnitude,
        int downwardLastStage,
        double maxDownwardMagnitude,
        Map<String, Double> initialTargetP,
        Set<String> zeroWeightGeneratorIds,
        boolean meritOrder) { }
