/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.raoapi.InterTemporalRaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.InterTemporalParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.result.api.LoadFlowAndSensitivityResult;
import com.powsybl.openrao.util.InterTemporalPool;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class InterTemporalSensitivityAnalysis {
    private final InterTemporalRaoInput input;
    private final RaoParameters parameters;
    private final Map<OffsetDateTime, Set<RangeAction<?>>> rangeActionsPerTimestamp;
    private final Map<OffsetDateTime, Set<FlowCnec>> flowCnecsPerTimestamp;

    public InterTemporalSensitivityAnalysis(InterTemporalRaoInput input, RaoParameters parameters) {
        this.input = input;
        this.parameters = parameters;
        this.rangeActionsPerTimestamp = getRangeActionsPerTimestamp();
        this.flowCnecsPerTimestamp = getFlowCnecsPerTimestamp();
    }

    public TemporalData<LoadFlowAndSensitivityResult> runInitialSensitivityAnalysis() throws InterruptedException {
        return new InterTemporalPool(input.getTimestampsToRun(), getNumberOfThreads()).runTasks(this::runForTimestamp);
    }

    int getNumberOfThreads() {
        if (parameters.hasExtension(InterTemporalParametersExtension.class)) {
            return Math.min(input.getTimestampsToRun().size(), parameters.getExtension(InterTemporalParametersExtension.class).getSensitivityComputationsInParallel());
        }
        return input.getTimestampsToRun().size();
    }

    Map<OffsetDateTime, Set<RangeAction<?>>> getRangeActionsPerTimestamp() {
        List<OffsetDateTime> timestampsToRun = input.getTimestampsToRun().stream().sorted().toList();
        Map<OffsetDateTime, Set<RangeAction<?>>> rangeActions = new HashMap<>();
        Set<RangeAction<?>> allRangeActions = new HashSet<>();

        // TODO: see what to do if RAs have same id across timestamps (same object from RemedialAction::equals)
        timestampsToRun.forEach(timestamp -> {
            Crac crac = input.getRaoInputs().getData(timestamp).orElseThrow().getCrac();
            allRangeActions.addAll(crac.getRangeActions(crac.getPreventiveState(), UsageMethod.AVAILABLE, UsageMethod.FORCED));
            rangeActions.put(timestamp, new HashSet<>(allRangeActions));
        });

        return rangeActions;
    }

    Map<OffsetDateTime, Set<FlowCnec>> getFlowCnecsPerTimestamp() {
        Map<OffsetDateTime, Set<FlowCnec>> flowCnecsMap = new HashMap<>();

        input.getTimestampsToRun().forEach(timestamp -> {
            Crac crac = input.getRaoInputs().getData(timestamp).orElseThrow().getCrac();
            Set<FlowCnec> flowCnecs = crac.getFlowCnecs(crac.getPreventiveState());
            crac.getStates().stream()
                    .filter(state -> state.getInstant().isOutage())
                    .forEach(state -> flowCnecs.addAll(crac.getFlowCnecs(state)));
            //TODO: add auto/curative cnecs with no RA
            flowCnecsMap.put(timestamp, flowCnecs);
        });

        return flowCnecsMap;
    }

    private ToolProvider buildToolProvider(OffsetDateTime timestamp) {
        return ToolProvider.buildFromRaoInputAndParameters(input.getRaoInputs().getData(timestamp).orElseThrow(), parameters);
    }

    private SensitivityComputer buildSensitivityComputer(OffsetDateTime timestamp, ToolProvider toolProvider) {
        Crac crac = input.getRaoInputs().getData(timestamp).orElseThrow().getCrac();
        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = SensitivityComputer.create()
                .withToolProvider(toolProvider)
                .withCnecs(flowCnecsPerTimestamp.get(timestamp))
                .withRangeActions(rangeActionsPerTimestamp.get(timestamp))
                .withOutageInstant(crac.getOutageInstant());

        if (parameters.hasExtension(LoopFlowParametersExtension.class)) {
            sensitivityComputerBuilder.withCommercialFlowsResults(toolProvider.getLoopFlowComputation(), toolProvider.getLoopFlowCnecs(flowCnecsPerTimestamp.get(timestamp)));
        }
        if (parameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
            sensitivityComputerBuilder.withPtdfsResults(toolProvider.getAbsolutePtdfSumsComputation(), flowCnecsPerTimestamp.get(timestamp));
        }

        return sensitivityComputerBuilder.build();
    }

    private LoadFlowAndSensitivityResult runForTimestamp(OffsetDateTime timestamp) {
        Network network = input.getRaoInputs().getData(timestamp).orElseThrow().getNetwork();
        ToolProvider toolProvider = buildToolProvider(timestamp);
        SensitivityComputer sensitivityComputer = buildSensitivityComputer(timestamp, toolProvider);
        sensitivityComputer.compute(network);
        return new LoadFlowAndSensitivityResult(sensitivityComputer.getBranchResult(network), sensitivityComputer.getSensitivityResult());
    }
}
