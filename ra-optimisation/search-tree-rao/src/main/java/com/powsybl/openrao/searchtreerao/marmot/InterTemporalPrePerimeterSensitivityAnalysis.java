/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PrePerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;

import java.util.Set;

import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.getPreventivePerimeterCnecs;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class InterTemporalPrePerimeterSensitivityAnalysis {

    private InterTemporalPrePerimeterSensitivityAnalysis() {
    }

    public static TemporalData<PrePerimeterResult> runInitialSensitivityAnalysis(TemporalData<RaoInput> inputs, RaoParameters parameters) {
        return inputs.map(raoInput -> {
            Crac crac = raoInput.getCrac();
            Network network = raoInput.getNetwork();
            State preventiveState = crac.getPreventiveState();
            Set<RangeAction<?>> rangeActions = crac.getRangeActions(preventiveState, UsageMethod.AVAILABLE);
            return new PrePerimeterSensitivityAnalysis(getPreventivePerimeterCnecs(crac), rangeActions, parameters, ToolProvider.buildFromRaoInputAndParameters(raoInput, parameters)).runInitialSensitivityAnalysis(network, crac);
        });
    }
}
