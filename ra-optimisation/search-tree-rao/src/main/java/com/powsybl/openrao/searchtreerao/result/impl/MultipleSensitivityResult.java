package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class MultipleSensitivityResult implements SensitivityResult, FlowResult {
    private Map<FlowCnec, SystematicSensitivityResult> systematicSensitivityResults = new HashMap<>();

    public void addResult(SystematicSensitivityResult systematicSensitivityResult, Set<FlowCnec> cnecs) {
        cnecs.forEach(cnec -> systematicSensitivityResults.put(cnec, systematicSensitivityResult));
    }

    @Override
    public double getFlow(FlowCnec flowCnec, Side side, Unit unit) {
        SystematicSensitivityResult systematicSensitivityResult = systematicSensitivityResults.get(flowCnec);
        if (unit == Unit.MEGAWATT) {
            return systematicSensitivityResult.getReferenceFlow(flowCnec, side);
        } else if (unit == Unit.AMPERE) {
            double intensity = systematicSensitivityResult.getReferenceIntensity(flowCnec, side);
            if (Double.isNaN(intensity) || Math.abs(intensity) <= 1e-6) {
                return systematicSensitivityResult.getReferenceFlow(flowCnec, side) * RaoUtil.getFlowUnitMultiplier(flowCnec, side, Unit.MEGAWATT, Unit.AMPERE);
            } else {
                return intensity;
            }
        } else {
            throw new OpenRaoException("Unknown unit for flow.");
        }
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, Side side, Unit unit) {
        return 0;
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, Side side) {
        return 1;
    }

    @Override
    public Map<FlowCnec, Map<Side, Double>> getPtdfZonalSums() {
        //TODO: implement method
        return null;
    }

    @Override
    public ComputationStatus getSensitivityStatus() {
        for (SystematicSensitivityResult systematicSensitivityResult : systematicSensitivityResults.values()) {
            switch (systematicSensitivityResult.getStatus()) {
                case SUCCESS:
                    continue;
                default:
                case FAILURE:
                    return ComputationStatus.FAILURE;
            }

        }
        return ComputationStatus.DEFAULT;
    }

    @Override
    public ComputationStatus getSensitivityStatus(State state) {
        for (SystematicSensitivityResult systematicSensitivityResult : systematicSensitivityResults.values()) {
            switch (systematicSensitivityResult.getStatus(state)) {
                case SUCCESS:
                    continue;
                default:
                case FAILURE:
                    return ComputationStatus.FAILURE;
            }

        }
        return ComputationStatus.DEFAULT;
    }

    @Override
    public Set<String> getContingencies() {
        return systematicSensitivityResults.values().stream()
            .flatMap(sensitivityResult -> sensitivityResult.getContingencies().stream())
            .collect(Collectors.toSet());
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, Side side, RangeAction<?> rangeAction, Unit unit) {
        SystematicSensitivityResult systematicSensitivityResult = systematicSensitivityResults.get(flowCnec);
        if (unit == Unit.MEGAWATT) {
            return systematicSensitivityResult.getSensitivityOnFlow(rangeAction, flowCnec, side);
        } else {
            throw new OpenRaoException(format("Unhandled unit for sensitivity value on range action : %s.", unit));
        }
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, Side side, SensitivityVariableSet linearGlsk, Unit unit) {
        SystematicSensitivityResult systematicSensitivityResult = systematicSensitivityResults.get(flowCnec);
        if (unit == Unit.MEGAWATT) {
            return systematicSensitivityResult.getSensitivityOnFlow(linearGlsk, flowCnec, side);
        } else {
            throw new OpenRaoException(format("Unknown unit for sensitivity value on linear GLSK : %s.", unit));
        }
    }

}
