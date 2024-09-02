/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.monitoring.anglemonitoring;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.raoresultapi.RaoResultClone;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.Collectors;

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
        if (optimizationInstant == null || !optimizationInstant.isCurative()) {
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
        Set<RemedialAction<?>> angleMonitoringRas = angleMonitoringResult.getAppliedCras(state);
        Set<NetworkAction> angleMonitoringNetworkActions = angleMonitoringRas.stream().filter(NetworkAction.class::isInstance).map(ra -> (NetworkAction) ra).collect(Collectors.toSet());
        concatenatedActions.addAll(angleMonitoringNetworkActions);
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

    @Override
    public boolean isSecure(Instant instant, PhysicalParameter... u) {
        List<PhysicalParameter> physicalParameters = new ArrayList<>(Stream.of(u).sorted().toList());
        if (physicalParameters.remove(PhysicalParameter.ANGLE)) {
            if (physicalParameters.isEmpty()) {
                return angleMonitoringResult.isSecure();
            } else {
                return raoResult.isSecure(instant, physicalParameters.toArray(new PhysicalParameter[0])) && angleMonitoringResult.isSecure();
            }
        } else {
            return raoResult.isSecure(instant, u);
        }
    }

    @Override
    public boolean isSecure(PhysicalParameter... u) {
        List<PhysicalParameter> physicalParameters = new ArrayList<>(Stream.of(u).sorted().toList());
        if (physicalParameters.remove(PhysicalParameter.ANGLE)) {
            if (physicalParameters.isEmpty()) {
                return angleMonitoringResult.isSecure();
            } else {
                return raoResult.isSecure(physicalParameters.toArray(new PhysicalParameter[0])) && angleMonitoringResult.isSecure();
            }
        } else {
            return raoResult.isSecure(u);
        }
    }

    @Override
    public boolean isSecure() {
        return raoResult.isSecure() && angleMonitoringResult.isSecure();
    }
}
