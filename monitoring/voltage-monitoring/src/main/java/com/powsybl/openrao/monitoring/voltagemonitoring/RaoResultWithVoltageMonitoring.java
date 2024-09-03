/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.monitoring.voltagemonitoring;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
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
 * class that enhances rao result with voltage monitoring results
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class RaoResultWithVoltageMonitoring extends RaoResultClone {

    private final RaoResult raoResult;
    private final VoltageMonitoringResult voltageMonitoringResult;

    public RaoResultWithVoltageMonitoring(RaoResult raoResult, VoltageMonitoringResult voltageMonitoringResult) {
        super(raoResult);
        this.raoResult = raoResult;
        this.voltageMonitoringResult = voltageMonitoringResult;
    }

    @Override
    public ComputationStatus getComputationStatus() {
        if (!voltageMonitoringResult.getStatus().equals(VoltageMonitoringResult.Status.FAILURE)) {
            return raoResult.getComputationStatus();
        } else {
            return ComputationStatus.FAILURE;
        }
    }

    @Override
    public double getVoltage(Instant optimizationInstant, VoltageCnec voltageCnec, Unit unit) {
        if (!unit.equals(Unit.KILOVOLT)) {
            throw new OpenRaoException("Unexpected unit for voltage monitoring result :  " + unit);
        }
        if (optimizationInstant == null || !optimizationInstant.isCurative()) {
            throw new OpenRaoException("Unexpected optimization instant for voltage monitoring result (only curative instant is supported currently) : " + optimizationInstant);
        }
        double upperBound = voltageCnec.getUpperBound(unit).orElse(Double.MAX_VALUE);
        double lowerBound = voltageCnec.getLowerBound(unit).orElse(-Double.MAX_VALUE);
        double maxVoltage = voltageMonitoringResult.getMaxVoltage(voltageCnec);
        double minVoltage = voltageMonitoringResult.getMinVoltage(voltageCnec);
        if (upperBound - maxVoltage < minVoltage - lowerBound) {
            return maxVoltage;
        } else {
            return minVoltage;
        }
    }

    @Override
    public double getMargin(Instant optimizationInstant, VoltageCnec voltageCnec, Unit unit) {
        if (optimizationInstant == null || !optimizationInstant.isCurative()) {
            throw new OpenRaoException("Unexpected optimization instant for voltage monitoring result (only curative instant is supported currently): " + optimizationInstant);
        }
        return Math.min(voltageCnec.getUpperBound(unit).orElse(Double.MAX_VALUE) - voltageMonitoringResult.getMaxVoltage(voltageCnec),
            voltageMonitoringResult.getMinVoltage(voltageCnec) - voltageCnec.getLowerBound(unit).orElse(-Double.MAX_VALUE));
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        Set<NetworkAction> concatenatedActions = new HashSet<>(raoResult.getActivatedNetworkActionsDuringState(state));
        Set<RemedialAction<?>> voltageMonitoringRas = voltageMonitoringResult.getAppliedRas(state);
        Set<NetworkAction> voltageMonitoringNetworkActions = voltageMonitoringRas.stream().filter(NetworkAction.class::isInstance).map(ra -> (NetworkAction) ra).collect(Collectors.toSet());
        concatenatedActions.addAll(voltageMonitoringNetworkActions);
        return concatenatedActions;
    }

    @Override
    public boolean isActivatedDuringState(State state, RemedialAction<?> remedialAction) {
        return voltageMonitoringResult.getAppliedRas(state).contains(remedialAction) || raoResult.isActivatedDuringState(state, remedialAction);
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        return isActivatedDuringState(state, (RemedialAction<?>) networkAction);
    }

    @Override
    public boolean isSecure(Instant instant, PhysicalParameter... u) {
        List<PhysicalParameter> physicalParameters = new ArrayList<>(Stream.of(u).sorted().toList());
        if (physicalParameters.remove(PhysicalParameter.VOLTAGE)) {
            if (physicalParameters.isEmpty()) {
                return voltageMonitoringResult.isSecure();
            } else {
                return raoResult.isSecure(instant, physicalParameters.toArray(new PhysicalParameter[0])) && voltageMonitoringResult.isSecure();
            }
        } else {
            return raoResult.isSecure(instant, u);
        }
    }

    @Override
    public boolean isSecure(PhysicalParameter... u) {
        List<PhysicalParameter> physicalParameters = new ArrayList<>(Stream.of(u).sorted().toList());
        if (physicalParameters.remove(PhysicalParameter.VOLTAGE)) {
            if (physicalParameters.isEmpty()) {
                return voltageMonitoringResult.isSecure();
            } else {
                return raoResult.isSecure(physicalParameters.toArray(new PhysicalParameter[0])) && voltageMonitoringResult.isSecure();
            }
        } else {
            return raoResult.isSecure(u);
        }
    }

    @Override
    public boolean isSecure() {
        return raoResult.isSecure() && voltageMonitoringResult.isSecure();
    }
}
