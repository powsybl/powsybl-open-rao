/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.roda;

import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.MultiScenarioTemporalData;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.raoresult.api.InterTemporalRaoResult;
import com.powsybl.openrao.raoapi.InterTemporalRaoInput;
import com.powsybl.openrao.raoapi.InterTemporalRaoInputWithNetworkPaths;
import com.powsybl.openrao.raoapi.InterTemporalRaoProvider;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.searchtreerao.roda.scenariorepository.GeneratorTargetPNetworkVariation;
import com.powsybl.openrao.searchtreerao.roda.scenariorepository.NetworkVariation;
import com.powsybl.openrao.searchtreerao.roda.scenariorepository.ScenarioRepository;
import com.powsybl.openrao.searchtreerao.roda.sensitivity.MultiSensitivityCompleteComputer;
import org.apache.commons.lang3.tuple.Pair;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Robust Optimizer for Dispatch Actions
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@AutoService(InterTemporalRaoProvider.class)
public class Roda implements InterTemporalRaoProvider {

    private static final String RAODA = "Roda";
    private static final String VERSION = "0.0.1";

    private static final String INITIAL_SCENARIO = "InitialScenario";

    @Override
    public CompletableFuture<InterTemporalRaoResult> run(InterTemporalRaoInputWithNetworkPaths raoInput, RaoParameters parameters) {
        // Import inputs
        InterTemporalRaoInput interTemporalRaoInput = importNetworksFromInterTemporalRaoInputWithNetworkPaths(raoInput);
        TemporalData<Network> networks = interTemporalRaoInput.getRaoInputs().map(RaoInput::getNetwork);
        // Generate scenarios
        ScenarioRepository scenarioRepository = ScenarioBuilderMock.createScenarios(networks);
        // Run initial multi-scenario load-flow & sensitivity analysis
        MultiScenarioTemporalData<Pair<FlowResult, SensitivityResult>> initialResults = new MultiSensitivityCompleteComputer().run(interTemporalRaoInput.getRaoInputs(), scenarioRepository, parameters);

        return null;
    }

    // TODO this is a duplicate code from Marmot ; where to put it?
    private InterTemporalRaoInput importNetworksFromInterTemporalRaoInputWithNetworkPaths(InterTemporalRaoInputWithNetworkPaths interTemporalRaoInputWithNetworkPaths) {
        return new InterTemporalRaoInput(
            interTemporalRaoInputWithNetworkPaths.getRaoInputs().map(raoInputWithNetworksPath -> {
                RaoInput raoInput = raoInputWithNetworksPath.toRaoInputWithPostIcsImportNetworkPath();
                raoInput.getNetwork().getVariantManager().cloneVariant(raoInput.getNetworkVariantId(), INITIAL_SCENARIO);
                return raoInput;
            }),
            interTemporalRaoInputWithNetworkPaths.getTimestampsToRun(),
            interTemporalRaoInputWithNetworkPaths.getGeneratorConstraints()
        );
    }

    @Override
    public String getName() {
        return RAODA;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }
}
