package com.farao_community.farao.monitoring.voltage_monitoring;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_api.AbstractRaoResultClone;

import java.util.HashSet;
import java.util.Set;

public class RaoResultWithVoltageMonitoring extends AbstractRaoResultClone {

    private final RaoResult raoResult;
    private final VoltageMonitoringResult voltageMonitoringResult;

    public RaoResultWithVoltageMonitoring(RaoResult raoResult, VoltageMonitoringResult voltageMonitoringResult) {
        super(raoResult);
        this.raoResult = raoResult;
        this.voltageMonitoringResult = voltageMonitoringResult;
    }

    @Override
    public ComputationStatus getComputationStatus() {
        if (!voltageMonitoringResult.getStatus().equals(VoltageMonitoringResult.Status.UNKNOWN)) {
            return ComputationStatus.DEFAULT;
        } else {
            return ComputationStatus.FAILURE;
        }
    }

    @Override
    public double getVoltage(Instant optimizationState, VoltageCnec voltageCnec, Unit unit) {
        double upperBound = voltageCnec.getUpperBound(unit).orElseThrow();
        double lowerBound = voltageCnec.getLowerBound(unit).orElseThrow();
        double maxVoltage = voltageMonitoringResult.getMaxVoltage(voltageCnec);
        double minVoltage = voltageMonitoringResult.getMinVoltage(voltageCnec);
        if (upperBound - maxVoltage < minVoltage - lowerBound) {
            return maxVoltage;
        } else {
            return minVoltage;
        }
    }

    @Override
    public double getMargin(Instant optimizationState, VoltageCnec voltageCnec, Unit unit) {
        return Math.min(voltageCnec.getUpperBound(unit).orElse(Double.MAX_VALUE) - getVoltage(optimizationState, voltageCnec, unit),
            getVoltage(optimizationState, voltageCnec, unit) - voltageCnec.getLowerBound(unit).orElse(-Double.MAX_VALUE));
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        Set<NetworkAction> concatenatedActions = new HashSet<>(raoResult.getActivatedNetworkActionsDuringState(state));
        concatenatedActions.addAll(voltageMonitoringResult.getAppliedRas(state));
        return concatenatedActions;
    }

    @Override
    public boolean isActivatedDuringState(State state, RemedialAction<?> remedialAction) {
        return raoResult.isActivatedDuringState(state, remedialAction) || voltageMonitoringResult.getAppliedRas(state).contains(remedialAction);
    }

}
