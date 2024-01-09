/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.monitoring.angle_monitoring;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.Instant;
import com.powsybl.open_rao.data.crac_api.RemedialAction;
import com.powsybl.open_rao.data.crac_api.State;
import com.powsybl.open_rao.data.crac_api.cnec.AngleCnec;
import com.powsybl.open_rao.data.crac_api.network_action.NetworkAction;
import com.powsybl.open_rao.data.rao_result_api.ComputationStatus;
import com.powsybl.open_rao.data.rao_result_api.RaoResult;
import com.powsybl.open_rao.data.rao_result_api.RaoResultClone;

import java.util.HashSet;
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
        if (angleMonitoringResult == null) {
            throw new OpenRaoException("AngleMonitoringResult must not be null");
        }
        this.angleMonitoringResult = angleMonitoringResult;
    }

    @Override
    public ComputationStatus getComputationStatus() {
        if (angleMonitoringResult.isDivergent() || angleMonitoringResult.isUnknown()) {
            return ComputationStatus.FAILURE;
        } else {
            return raoResult.getComputationStatus();
        }
    }

    @Override
    public double getAngle(Instant optimizationInstant, AngleCnec angleCnec, Unit unit) {
        if (!unit.equals(Unit.DEGREE)) {
            throw new OpenRaoException("Unexpected unit for angle monitoring result : " + unit);
        }
        if (!optimizationInstant.isCurative()) {
            throw new OpenRaoException("Unexpected optimization instant for angle monitoring result (only curative instant is supported currently) : " + optimizationInstant);
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
