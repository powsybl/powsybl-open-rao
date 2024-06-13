/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.api;

import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;

import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface RangeActionResult {

    Set<RangeAction<?>> getRangeActions();

    Set<RangeAction<?>> getActivatedRangeActions();

    double getOptimizedSetpoint(RangeAction<?> rangeAction);

    Map<RangeAction<?>, Double> getOptimizedSetpoints();

    int getOptimizedTap(PstRangeAction pstRangeAction);

    Map<PstRangeAction, Integer> getOptimizedTaps();
}
