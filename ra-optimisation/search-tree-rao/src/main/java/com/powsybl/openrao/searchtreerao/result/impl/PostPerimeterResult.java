/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;

/**
 * This record is here to contain two objects:
 * <ul>
 *     <li> an {@link OptimizationResult} that contains the result specific to the optimization perimeter only </li>
 *     <li> a {@link PrePerimeterResult} that contains the result for the optimization perimeter, as well as any state following </li>
 * </ul>
 *
 * The need for this record is motivated by the need of being able to get results (for costs, margins, flows) after each optimization instant, that considers all following instants.
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public record PostPerimeterResult(OptimizationResult optimizationResult,
                                  PrePerimeterResult prePerimeterResultForAllFollowingStates) {

}
