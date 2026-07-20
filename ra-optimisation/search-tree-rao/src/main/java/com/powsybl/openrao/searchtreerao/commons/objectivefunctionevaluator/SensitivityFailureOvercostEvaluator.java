/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.AbsoluteCostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.CostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.reports.CommonReports;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class SensitivityFailureOvercostEvaluator implements CostEvaluator {
    private final double sensitivityFailureOvercost;
    private final Set<State> states;

    public SensitivityFailureOvercostEvaluator(Set<FlowCnec> flowCnecs, double sensitivityFailureOvercost) {
        this.sensitivityFailureOvercost = sensitivityFailureOvercost;
        this.states = flowCnecs.stream().map(Cnec::getState).collect(Collectors.toSet());
    }

    @Override
    public String getName() {
        return "sensitivity-failure-cost";
    }

    @Override
    public CostEvaluatorResult evaluate(final FlowResult flowResult,
                                        final RemedialActionActivationResult remedialActionActivationResult,
                                        final ReportNode reportNode) {
        if (flowResult.getComputationStatus() == ComputationStatus.FAILURE) {
            CommonReports.reportAssigningVirtualCostToSensitivityFailure(reportNode, sensitivityFailureOvercost);
            return new AbsoluteCostEvaluatorResult(sensitivityFailureOvercost);
        }
        for (State state : states) {
            if (flowResult.getComputationStatus(state) == ComputationStatus.FAILURE) {
                CommonReports.reportAssigningVirtualCostToSensitivityFailureForState(reportNode, state.getId(), sensitivityFailureOvercost);
                return new AbsoluteCostEvaluatorResult(sensitivityFailureOvercost);
            }
        }
        return new AbsoluteCostEvaluatorResult(0.0);
    }
}
