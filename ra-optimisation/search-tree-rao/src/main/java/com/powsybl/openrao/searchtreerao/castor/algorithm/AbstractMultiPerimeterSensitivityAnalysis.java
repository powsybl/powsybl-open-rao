/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoLoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public abstract class AbstractMultiPerimeterSensitivityAnalysis {

    // actual input
    protected final Crac crac;
    protected final Set<FlowCnec> flowCnecs;
    protected final Set<RangeAction<?>> rangeActions;
    protected final RaoParameters raoParameters;
    protected final ToolProvider toolProvider;

    protected AbstractMultiPerimeterSensitivityAnalysis(Crac crac,
                                                        Set<FlowCnec> flowCnecs,
                                                        Set<RangeAction<?>> rangeActions,
                                                        RaoParameters raoParameters,
                                                        ToolProvider toolProvider) {
        this.crac = crac;
        this.flowCnecs = flowCnecs;
        this.rangeActions = rangeActions;
        this.raoParameters = raoParameters;
        this.toolProvider = toolProvider;
    }

    protected AbstractMultiPerimeterSensitivityAnalysis(Crac crac,
                                                        Set<State> states,
                                                        RaoParameters raoParameters,
                                                        ToolProvider toolProvider) {
        this.crac = crac;
        this.rangeActions = new HashSet<>();
        this.flowCnecs = new HashSet<>();
        for (State state : states) {
            this.rangeActions.addAll(crac.getRangeActions(state));
            this.flowCnecs.addAll(crac.getFlowCnecs(state));
        }
        this.raoParameters = raoParameters;
        this.toolProvider = toolProvider;
    }

    protected SensitivityComputer buildSensitivityComputer(FlowResult initialFlowResult, AppliedRemedialActions appliedCurativeRemedialActions) {
        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = SensitivityComputer.create()
            .withToolProvider(toolProvider)
            .withCnecs(flowCnecs)
            .withRangeActions(rangeActions)
            .withOutageInstant(crac.getOutageInstant());

        OpenRaoSearchTreeParameters searchTreeParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class);
        if (Objects.nonNull(searchTreeParameters)) {
            Optional<SearchTreeRaoLoopFlowParameters> optionalLoopFlowParameters = searchTreeParameters.getLoopFlowParameters();
            if (optionalLoopFlowParameters.isPresent()) {
                if (optionalLoopFlowParameters.get().getPtdfApproximation().shouldUpdatePtdfWithTopologicalChange()) {
                    sensitivityComputerBuilder.withCommercialFlowsResults(toolProvider.getLoopFlowComputation(), toolProvider.getLoopFlowCnecs(flowCnecs));
                } else {
                    sensitivityComputerBuilder.withCommercialFlowsResults(initialFlowResult);
                }
            }
            Optional<SearchTreeRaoRelativeMarginsParameters> optionalRelativeMarginParameters = searchTreeParameters.getRelativeMarginsParameters();
            if (optionalRelativeMarginParameters.isPresent()) {
                if (optionalRelativeMarginParameters.get().getPtdfApproximation().shouldUpdatePtdfWithTopologicalChange()) {
                    sensitivityComputerBuilder.withPtdfsResults(toolProvider.getAbsolutePtdfSumsComputation(), flowCnecs);
                } else {
                    sensitivityComputerBuilder.withPtdfsResults(initialFlowResult);
                }
            }
        }
        if (appliedCurativeRemedialActions != null) {
            sensitivityComputerBuilder.withAppliedRemedialActions(appliedCurativeRemedialActions);
        }
        return sensitivityComputerBuilder.build();
    }
}
