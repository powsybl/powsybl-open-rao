/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.roda;

import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.MultiScenarioTemporalData;
import com.powsybl.openrao.commons.RandomizedString;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.InterTemporalRaoResult;
import com.powsybl.openrao.raoapi.*;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.marmot.MarmotUtils;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.roda.scenariorepository.ScenarioRepository;
import com.powsybl.openrao.searchtreerao.roda.sensitivity.MultiSensitivityCompleteComputer;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Robust Optimizer for Dispatch Actions
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@AutoService(InterTemporalRaoProvider.class)
public class Roda implements InterTemporalRaoProvider {

    private static final String RODA = "Roda";
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
        MultiScenarioTemporalData<FlowAndSensitivityResult> initialResults = new MultiSensitivityCompleteComputer().run(interTemporalRaoInput.getRaoInputs(), scenarioRepository, parameters);
        // Build objective function evaluator
        MultiScenarioObjectiveFunction objectiveFunction = buildObjectiveFunction(interTemporalRaoInput.getRaoInputs().map(RaoInput::getCrac), initialResults, parameters);
        ObjectiveFunctionResult initialObjectiveFunctionResult = objectiveFunction.evaluate(initialResults);
        // Run linear range actions optimization
        TemporalData<RangeActionActivationResult> newSetpoints = RodaLinearOptimizer.optimize(
            interTemporalRaoInput.getRaoInputs().map(RaoInput::getCrac),
            interTemporalRaoInput.getRaoInputs().map(RaoInput::getNetwork),
            initialResults,
            parameters
        );
        // Update setpoints and rerun loadflow computations
        MultiScenarioTemporalData<FlowAndSensitivityResult> newResults = computeNewFlows(raoInput, parameters, newSetpoints, networks, interTemporalRaoInput, scenarioRepository);
        // Evaluate
        ObjectiveFunctionResult finalObjectiveFunctionResult  = objectiveFunction.evaluate(newResults); // TODO use new setpoints in obj fn evaluation

        return null;
    }

    private static MultiScenarioTemporalData<FlowAndSensitivityResult> computeNewFlows(InterTemporalRaoInputWithNetworkPaths raoInput, RaoParameters parameters, TemporalData<RangeActionActivationResult> newSetpoints, TemporalData<Network> networks, InterTemporalRaoInput interTemporalRaoInput, ScenarioRepository scenarioRepository) {
        Map<OffsetDateTime, String> oldVariants = new HashMap<>();
        Map<OffsetDateTime, String> tmpVariants = new HashMap<>();
        for (OffsetDateTime timestamp : newSetpoints.getTimestamps()) {
            Crac crac = raoInput.getRaoInputs().map(RaoInputWithNetworkPaths::getCrac).getData(timestamp).orElseThrow();
            Network network = networks.getData(timestamp).orElseThrow();
            String initialVariant = network.getVariantManager().getWorkingVariantId();
            String tmpVariant = RandomizedString.getRandomizedString("network_post_pra", network.getVariantManager().getVariantIds());
            network.getVariantManager().cloneVariant(initialVariant, tmpVariant);
            network.getVariantManager().setWorkingVariant(tmpVariant);
            RangeActionActivationResult tsResults = newSetpoints.getData(timestamp).orElseThrow();
            tsResults.getRangeActions().forEach(ra -> ra.apply(network, tsResults.getOptimizedSetpoint(ra, crac.getPreventiveState())));
            oldVariants.put(timestamp, initialVariant);
            tmpVariants.put(timestamp, tmpVariant);
        }
        MultiScenarioTemporalData<FlowAndSensitivityResult> newResults = new MultiSensitivityCompleteComputer().run(interTemporalRaoInput.getRaoInputs(), scenarioRepository, parameters);
        for (OffsetDateTime timestamp : newSetpoints.getTimestamps()) {
            Network network = networks.getData(timestamp).orElseThrow();
            network.getVariantManager().setWorkingVariant(oldVariants.get(timestamp));
            network.getVariantManager().removeVariant(tmpVariants.get(timestamp));
        }
        return newResults;
    }

    private static MultiScenarioObjectiveFunction buildObjectiveFunction(TemporalData<Crac> cracs, MultiScenarioTemporalData<? extends FlowResult> initialResults, RaoParameters raoParameters) {
        Set<FlowCnec> allFlowCnecs = cracs.map(MarmotUtils::getPreventivePerimeterCnecs).getDataPerTimestamp().values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        Set<State> allOptimizedStates = new HashSet<>(cracs.map(Crac::getPreventiveState).getDataPerTimestamp().values());
        return MultiScenarioObjectiveFunction.build(allFlowCnecs,
            new HashSet<>(), // no loop flows for now
            initialResults,
            initialResults,
            Collections.emptySet(), // not set for now
            raoParameters,
            allOptimizedStates);
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
        return RODA;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }
}
