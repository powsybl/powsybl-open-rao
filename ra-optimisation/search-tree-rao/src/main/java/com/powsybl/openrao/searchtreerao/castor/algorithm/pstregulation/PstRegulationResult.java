/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm.pstregulation;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;

import java.util.Map;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record PstRegulationResult(Contingency contingency, Map<PstRangeAction, Integer> regulatedTapPerPst) {
}
