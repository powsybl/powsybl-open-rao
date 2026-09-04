/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.extension.Metadata;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class CastorMetadataHelper {
    private CastorMetadataHelper() {
    }

    public static void fillAndAddFromPrePerimeter(Crac crac,
                                                  RaoResult raoResult,
                                                  PrePerimeterResult prePerimeterResult,
                                                  String executionDetails) {
        Metadata metadata = new Metadata();
        metadata.setExecutionDetails(executionDetails);
        for (State state : crac.getStates()) {
            if (prePerimeterResult.getComputationStatus(state) != ComputationStatus.DEFAULT) {
                metadata.setComputationStatus(state, prePerimeterResult.getComputationStatus(state));
            }
        }
        raoResult.addExtension(Metadata.class, metadata);
    }

    public static void fillAndAddWithGlobalFailure(Crac crac, RaoResult raoResult, String failureReason) {
        Metadata metadata = new Metadata();
        metadata.setExecutionDetails(failureReason);
        metadata.setComputationStatus(crac.getPreventiveState(), ComputationStatus.FAILURE);
        raoResult.addExtension(Metadata.class, metadata);
    }
}
