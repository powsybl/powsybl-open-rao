/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.monitoring.voltage_monitoring;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_api.RaoResultClone;

import java.util.HashSet;
import java.util.Set;

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
        if (!voltageMonitoringResult.getStatus().equals(VoltageMonitoringResult.Status.UNKNOWN)) {
            return raoResult.getComputationStatus();
        } else {
            return ComputationStatus.FAILURE;
        }
    }

    @Override
    public double getVoltage(Instant optimizationInstant, VoltageCnec voltageCnec, Unit unit) {
        if (!unit.equals(Unit.KILOVOLT)) {
            throw new FaraoException("Unexpected unit for voltage monitoring result :  " + unit);
        }
        if (!optimizationInstant.equals(Instant.CURATIVE)) {
            throw new FaraoException("Unexpected optimization instant for voltage monitoring result (only curative instant is supported currently) : " + optimizationInstant);
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
        if (!optimizationInstant.equals(Instant.CURATIVE)) {
            throw new FaraoException("Unexpected optimization instant for voltage monitoring result (only curative instant is supported currently): " + optimizationInstant);
        }
        return Math.min(voltageCnec.getUpperBound(unit).orElse(Double.MAX_VALUE) - voltageMonitoringResult.getMaxVoltage(voltageCnec),
            voltageMonitoringResult.getMinVoltage(voltageCnec) - voltageCnec.getLowerBound(unit).orElse(-Double.MAX_VALUE));
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        Set<NetworkAction> concatenatedActions = new HashSet<>(raoResult.getActivatedNetworkActionsDuringState(state));
        concatenatedActions.addAll(voltageMonitoringResult.getAppliedRas(state));
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
}
