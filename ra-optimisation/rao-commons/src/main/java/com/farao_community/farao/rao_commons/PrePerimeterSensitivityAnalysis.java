/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.results.*;
import com.farao_community.farao.rao_commons.result.RangeActionResultImpl;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This class aims at performing the initial sensitivity analysis of a RAO, the one
 * which defines the pre-optimisation variant. It is common to both the Search Tree
 * and the Linear RAO.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class PrePerimeterSensitivityAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrePerimeterSensitivityAnalysis.class);

    private final Set<BranchCnec> cnecs;
    private final Set<RangeAction> rangeActions;
    private final ToolProvider toolProvider;
    private final RaoParameters raoParameters;

    private SensitivityComputer sensitivityComputer;

    public PrePerimeterSensitivityAnalysis(Crac crac, Network network, ToolProvider toolProvider, RaoParameters raoParameters) {
        // it is actually quite strange to ask for a RaoData here, but it is required in
        // order to use the fillResultsWithXXX() methods of the CracResultManager.
        this.toolProvider = toolProvider;
        cnecs = crac.getBranchCnecs();
        rangeActions = new HashSet<>();
        crac.getStates().forEach(state -> rangeActions.addAll(crac.getRangeActions(network, state, UsageMethod.AVAILABLE)));
        this.raoParameters = raoParameters;
    }

    public PrePerimeterResult run(Network network) {
        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = getBuilder();
        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            sensitivityComputerBuilder.withCommercialFlowsResults(toolProvider.getLoopFlowComputation(), toolProvider.getLoopFlowCnecs(cnecs));
        }
        if (raoParameters.getObjectiveFunction().doesRequirePtdf()) {
            sensitivityComputerBuilder.withPtdfsResults(toolProvider.getAbsolutePtdfSumsComputation(), cnecs);
        }
        sensitivityComputer = sensitivityComputerBuilder.build();
        return runAndGetResult(network);
    }

    public PrePerimeterResult runBasedOn(Network network, OptimizationResult optimizationResult) {
        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = getBuilder();
        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            if (raoParameters.getLoopFlowApproximationLevel().shouldUpdatePtdfWithTopologicalChange()) {
                sensitivityComputerBuilder.withCommercialFlowsResults(toolProvider.getLoopFlowComputation(), toolProvider.getLoopFlowCnecs(cnecs));
            } else {
                sensitivityComputerBuilder.withCommercialFlowsResults(optimizationResult);
            }
        }
        if (raoParameters.getObjectiveFunction().doesRequirePtdf()) {
            sensitivityComputerBuilder.withPtdfsResults(optimizationResult);
        }
        sensitivityComputer = sensitivityComputerBuilder.build();
        return runAndGetResult(network);
    }

    private SensitivityComputer.SensitivityComputerBuilder getBuilder() {
        return SensitivityComputer.create()
                .withToolProvider(toolProvider)
                .withCnecs(cnecs)
                .withRangeActions(rangeActions);
    }

    private PrePerimeterResult runAndGetResult(Network network) {
        sensitivityComputer.compute(network);
        return new PrePerimeterSensitivityOutput(
                sensitivityComputer.getBranchResult(),
                sensitivityComputer.getSensitivityResult(),
                new RangeActionResultImpl(network, rangeActions)
        );
    }
}
