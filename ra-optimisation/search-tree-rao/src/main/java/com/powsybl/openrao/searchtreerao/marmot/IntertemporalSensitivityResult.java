package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.Map;
import java.util.Set;

public class IntertemporalSensitivityResult implements SensitivityResult, FlowResult {
    private TemporalData<PrePerimeterResult> systematicSensitivityResults;

    public IntertemporalSensitivityResult(TemporalData<PrePerimeterResult> systematicSensitivityResults) {
        this.systematicSensitivityResults = systematicSensitivityResults;
    }

    @Override
    public ComputationStatus getSensitivityStatus() { return null;
    }

    @Override
    public ComputationStatus getSensitivityStatus(State state) {
        return null;
    }

    @Override
    public Set<String> getContingencies() {
        return null;
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, RangeAction<?> rangeAction, Unit unit) {
        return 0;
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, SensitivityVariableSet linearGlsk, Unit unit) {
        return 0;
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        return 0;
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit, Instant optimizedInstant) {
        return 0;
    }

    @Override
    public double getMargin(FlowCnec flowCnec, Unit unit) {
        return 0;
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        return 0;
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, TwoSides side) {
        return 0;
    }

    @Override
    public Map<FlowCnec, Map<TwoSides, Double>> getPtdfZonalSums() {
        return null;
    }

    @Override
    public ComputationStatus getComputationStatus() {
        return null;
    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        return null;
    }
}
