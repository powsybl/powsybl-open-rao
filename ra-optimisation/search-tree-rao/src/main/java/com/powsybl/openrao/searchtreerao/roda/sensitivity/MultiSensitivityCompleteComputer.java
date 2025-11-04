/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.roda.sensitivity;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.MultiScenarioTemporalData;
import com.powsybl.openrao.commons.RandomizedString;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PrePerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.searchtreerao.roda.scenariorepository.ScenarioRepository;
import org.apache.commons.lang3.tuple.Pair;

import java.time.OffsetDateTime;

/**
 * Runs load-flow & sensitivity computations on all scenarios
 */
public class MultiSensitivityCompleteComputer implements MultiSensitivityComputer {
    public MultiSensitivityCompleteComputer() {
        // nothing to do
    }

    @Override
    public MultiScenarioTemporalData<Pair<FlowResult, SensitivityResult>> run(TemporalData<RaoInput> raoInputs, ScenarioRepository scenarioRepository, RaoParameters raoParameters) {
        MultiScenarioTemporalData<Pair<FlowResult, SensitivityResult>> results = new MultiScenarioTemporalData<>();
        for (String scenario : scenarioRepository.getScenarios()) {
            for (OffsetDateTime ts : raoInputs.getTimestamps()) {
                RaoInput raoInput = raoInputs.getData(ts).orElseThrow();
                Network network = raoInput.getNetwork();

                String initialVariant = network.getVariantManager().getWorkingVariantId();
                String newVariant = RandomizedString.getRandomizedString("", network.getVariantManager().getVariantIds(), 10);
                network.getVariantManager().cloneVariant(initialVariant, newVariant);

                scenarioRepository.applyScenario(scenario, network, ts);

                PrePerimeterResult result = runPrePerimeterSensitivityAnalysisWithRangeActions(raoInput, raoParameters);
                results.put(scenario, ts, Pair.of(result, result));

                network.getVariantManager().removeVariant(newVariant);
            }
        }
        return results;
    }

    // TODO duplicate from Marmot
    private static PrePerimeterResult runPrePerimeterSensitivityAnalysisWithRangeActions(RaoInput raoInput, RaoParameters raoParameters) {
        Crac crac = raoInput.getCrac();
        return new PrePerimeterSensitivityAnalysis(
            crac,
            crac.getFlowCnecs(), // want results on all cnecs
            crac.getRangeActions(crac.getPreventiveState()),
            raoParameters,
            ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters)
        ).runInitialSensitivityAnalysis(raoInput.getNetwork());
    }
}
