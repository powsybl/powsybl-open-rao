/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.InterTemporalRaoInput;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
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
    private final TemporalData<Set<RangeAction<?>>> rangeActionsPerTimestamp;
    private final TemporalData<Set<FlowCnec>> flowCnecsPerTimestamp;

    public InterTemporalSensitivityAnalysis(InterTemporalRaoInput input, RaoParameters parameters) {
        InterTemporalSensitivityAnalysisHelper helper = new InterTemporalSensitivityAnalysisHelper(input);
        this.input = input;
        this.parameters = parameters;
        this.rangeActionsPerTimestamp = helper.getRangeActions();
        this.flowCnecsPerTimestamp = helper.getFlowCnecs();
    }

    public TemporalData<LoadFlowAndSensitivityResult> runInitialSensitivityAnalysis() throws InterruptedException {
        return new InterTemporalPool(input.getTimestampsToRun()).runTasks(this::runForTimestamp);
    }

    private LoadFlowAndSensitivityResult runForTimestamp(OffsetDateTime timestamp) {
        RaoInput raoInput = input.getRaoInputs().getData(timestamp).orElseThrow();
        Network network = raoInput.getNetwork();
        ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, parameters);
        SensitivityComputer sensitivityComputer = buildSensitivityComputer(flowCnecsPerTimestamp.getData(timestamp).orElse(Set.of()), rangeActionsPerTimestamp.getData(timestamp).orElse(Set.of()), raoInput.getCrac().getOutageInstant(), toolProvider);
        sensitivityComputer.compute(network);
        return new LoadFlowAndSensitivityResult(sensitivityComputer.getBranchResult(network), sensitivityComputer.getSensitivityResult());
    }

    private SensitivityComputer buildSensitivityComputer(Set<FlowCnec> flowCnecs, Set<RangeAction<?>> rangeActions, Instant outageInstant, ToolProvider toolProvider) {
        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = SensitivityComputer.create()
                .withToolProvider(toolProvider)
                .withCnecs(flowCnecs)
                .withRangeActions(rangeActions)
                .withOutageInstant(outageInstant);

        if (parameters.hasExtension(LoopFlowParametersExtension.class)) {
            sensitivityComputerBuilder.withCommercialFlowsResults(toolProvider.getLoopFlowComputation(), toolProvider.getLoopFlowCnecs(flowCnecs));
        }
        if (parameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
            sensitivityComputerBuilder.withPtdfsResults(toolProvider.getAbsolutePtdfSumsComputation(), flowCnecs);
        }

        return sensitivityComputerBuilder.build();
    }
}
