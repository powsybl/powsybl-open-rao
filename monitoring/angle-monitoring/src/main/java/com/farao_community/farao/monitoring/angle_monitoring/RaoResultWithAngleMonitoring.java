/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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
import com.farao_community.farao.data.rao_result_api.RaoResultClone;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * class that enhances rao result with angle monitoring results
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class RaoResultWithAngleMonitoring extends RaoResultClone {

    private final RaoResult raoResult;
    private final AngleMonitoringResult angleMonitoringResult;

    public RaoResultWithAngleMonitoring(RaoResult raoResult, AngleMonitoringResult angleMonitoringResult) {
        super(raoResult);
        this.raoResult = raoResult;
        this.angleMonitoringResult = angleMonitoringResult;
    }

    public AngleMonitoringResult getAngleMonitoringResult() {
        return angleMonitoringResult;
    }

    @Override
    public ComputationStatus getComputationStatus() {
        if (Objects.isNull(angleMonitoringResult) || (!angleMonitoringResult.isDivergent() && !angleMonitoringResult.isUnknown())) {
            return raoResult.getComputationStatus();
        }
        return ComputationStatus.FAILURE;
    }

    @Override
    public double getAngle(Instant optimizationInstant, AngleCnec angleCnec, Unit unit) {
        if (!unit.equals(Unit.DEGREE)) {
            throw new FaraoException("Unexpected unit for angle monitoring result : " + unit);
        }
        if (!optimizationInstant.equals(Instant.CURATIVE)) {
            throw new FaraoException("Unexpected optimization instant for angle monitoring result (only curative instant is supported currently) : " + optimizationInstant);
        }
        if (!Objects.isNull(angleMonitoringResult)) {
            return angleMonitoringResult.getAngle(angleCnec, unit);
        }
        throw new FaraoException("The AngleMonitoringResult is empty");
    }

    @Override
    public double getMargin(Instant optimizationInstant, AngleCnec angleCnec, Unit unit) {
        return Math.min(angleCnec.getUpperBound(unit).orElse(Double.MAX_VALUE) - getAngle(optimizationInstant, angleCnec, unit),
            getAngle(optimizationInstant, angleCnec, unit) - angleCnec.getLowerBound(unit).orElse(-Double.MAX_VALUE));
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        Set<NetworkAction> concatenatedActions = new HashSet<>(raoResult.getActivatedNetworkActionsDuringState(state));
        if (!Objects.isNull(angleMonitoringResult)) {
            concatenatedActions.addAll(angleMonitoringResult.getAppliedCras(state));
        }
        return concatenatedActions;
    }

    @Override
    public boolean isActivatedDuringState(State state, RemedialAction<?> remedialAction) {
        if (Objects.isNull(angleMonitoringResult)) {
            return raoResult.isActivatedDuringState(state, remedialAction);
        }
        return angleMonitoringResult.getAppliedCras(state).contains(remedialAction) || raoResult.isActivatedDuringState(state, remedialAction);
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        return isActivatedDuringState(state, (RemedialAction<?>) networkAction);
    }

}
