/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.roda.sensitivity;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.MultiScenarioTemporalData;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.searchtreerao.roda.scenariorepository.ScenarioRepository;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

/**
 * Runs load-flow & sensitivity computations on reference scenario only, then approximates the other scenarios using sensitivity values
 */
public class MultiSensitivityOptimizedComputer implements MultiSensitivityComputer {
    @Override
    public MultiScenarioTemporalData<Pair<FlowResult, SensitivityResult>> run(TemporalData<RaoInput> raoInputs, ScenarioRepository scenarioRepository, RaoParameters raoParameters) {
        return null;
    }
}
