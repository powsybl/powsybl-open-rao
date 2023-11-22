package com.farao_community.farao.monitoring.angle_monitoring;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_api.AbstractRaoResultClone;

import java.util.HashSet;
import java.util.Set;

public class RaoResultWithAngleMonitoring extends AbstractRaoResultClone {

    private final RaoResult raoResult;
    private final AngleMonitoringResult angleMonitoringResult;

    public RaoResultWithAngleMonitoring(RaoResult raoResult, AngleMonitoringResult angleMonitoringResult) {
        super(raoResult);
        this.raoResult = raoResult;
        this.angleMonitoringResult = angleMonitoringResult;
    }

    @Override
    public ComputationStatus getComputationStatus() {
        if (angleMonitoringResult.isSecure() || angleMonitoringResult.isUnsecure()) {
            return ComputationStatus.DEFAULT;
        } else {
            return ComputationStatus.FAILURE;
        }
    }

    @Override
    public double getAngle(Instant optimizationInstant, AngleCnec angleCnec, Unit unit) {
        if (!unit.equals(Unit.DEGREE)) {
            throw new FaraoException("Unexpected unit for angle monitoring result : " + unit);
        }
        if (!optimizationInstant.equals(Instant.CURATIVE)) {
            throw new FaraoException("Unexpected optimization instant for angle monitoring result : " + optimizationInstant);
        }
        return angleMonitoringResult.getAngle(angleCnec, unit);
    }

    @Override
    public double getMargin(Instant optimizationInstant, AngleCnec angleCnec, Unit unit) {
        return Math.min(angleCnec.getUpperBound(unit).orElse(Double.MAX_VALUE) - getAngle(optimizationInstant, angleCnec, unit),
            getAngle(optimizationInstant, angleCnec, unit) - angleCnec.getLowerBound(unit).orElse(-Double.MAX_VALUE));
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        Set<NetworkAction> concatenatedActions = new HashSet<>(raoResult.getActivatedNetworkActionsDuringState(state));
        concatenatedActions.addAll(angleMonitoringResult.getAppliedCras(state));
        return concatenatedActions;
    }

    @Override
    public boolean isActivatedDuringState(State state, RemedialAction<?> remedialAction) {
        return angleMonitoringResult.getAppliedCras(state).contains(remedialAction) || raoResult.isActivatedDuringState(state, remedialAction);
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        return isActivatedDuringState(state, (RemedialAction<?>) networkAction);
    }

}
