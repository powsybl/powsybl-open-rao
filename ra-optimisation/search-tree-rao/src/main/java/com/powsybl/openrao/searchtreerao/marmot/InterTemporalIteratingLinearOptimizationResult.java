/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalFlowResult;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalRemedialActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.LinearProblemStatus;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RemedialActionActivationResultImpl;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class InterTemporalIteratingLinearOptimizationResult {
    private LinearProblemStatus status;
    private final TemporalData<LinearOptimizationResult> resultPerTimestamp;
    private final ObjectiveFunctionResult globalObjectiveFunctionResult;

    public InterTemporalIteratingLinearOptimizationResult(LinearProblemStatus status, TemporalData<FlowResult> flowResults, TemporalData<SensitivityResult> sensitivityResult, TemporalData<RangeActionActivationResult> rangeActionActivation, ObjectiveFunction objectiveFunction) {
        this.status = status;
        this.resultPerTimestamp = mergeFlowSensitivityAndRangeActionResults(flowResults, sensitivityResult, rangeActionActivation, objectiveFunction, status);
        this.globalObjectiveFunctionResult = evaluateGlobalObjectiveFunction(objectiveFunction, flowResults, rangeActionActivation);
    }

    public void setStatus(LinearProblemStatus status) {
        this.status = status;
    }

    public LinearProblemStatus getStatus() {
        return status;
    }

    public TemporalData<LinearOptimizationResult> getResultPerTimestamp() {
        return resultPerTimestamp;
    }

    public ObjectiveFunctionResult getGlobalObjectiveFunctionResult() {
        return globalObjectiveFunctionResult;
    }

    private static TemporalData<LinearOptimizationResult> mergeFlowSensitivityAndRangeActionResults(TemporalData<FlowResult> flowResults, TemporalData<SensitivityResult> sensitivityResults, TemporalData<RangeActionActivationResult> rangeActionActivationResults, ObjectiveFunction objectiveFunction, LinearProblemStatus status) {
        Map<OffsetDateTime, LinearOptimizationResult> linearOptimizationResults = new HashMap<>();
        List<OffsetDateTime> timestamps = flowResults.getTimestamps();
        for (OffsetDateTime timestamp : timestamps) {
            FlowResult flowResult = flowResults.getData(timestamp).orElseThrow();
            SensitivityResult sensitivityResult = sensitivityResults.getData(timestamp).orElseThrow();
            RangeActionActivationResult rangeActionActivationResult = rangeActionActivationResults.getData(timestamp).orElseThrow();
            LinearOptimizationResult linearOptimizationResult = new LinearOptimizationResultImpl(flowResult, sensitivityResult, rangeActionActivationResult, objectiveFunction.evaluate(flowResult, new RemedialActionActivationResultImpl(rangeActionActivationResult, new NetworkActionsResultImpl(Set.of()))), status);
            linearOptimizationResults.put(timestamp, linearOptimizationResult);
        }
        return new TemporalDataImpl<>(linearOptimizationResults);
    }

    private static ObjectiveFunctionResult evaluateGlobalObjectiveFunction(ObjectiveFunction objectiveFunction, TemporalData<FlowResult> flowResults, TemporalData<RangeActionActivationResult> rangeActionActivationResults) {
        FlowResult globalFlowResult = new GlobalFlowResult(flowResults);
        RemedialActionActivationResult globalRemedialActionActivationResult = new GlobalRemedialActionActivationResult(rangeActionActivationResults);
        return objectiveFunction.evaluate(globalFlowResult, globalRemedialActionActivationResult);
    }
}
