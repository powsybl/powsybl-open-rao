/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.impl.VoltageCnecValue;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.RaoResultClone;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * class that enhances rao result with voltage monitoring results
 *
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class RaoResultWithVoltageMonitoring extends RaoResultClone {

    private final RaoResult raoResult;
    private final MonitoringResult voltageMonitoringResult;

    public RaoResultWithVoltageMonitoring(RaoResult raoResult, MonitoringResult voltageMonitoringResult) {
        super(raoResult);
        this.raoResult = raoResult;
        this.voltageMonitoringResult = voltageMonitoringResult;
    }

    @Override
    public ComputationStatus getComputationStatus() {
        if (!voltageMonitoringResult.getStatus().equals(Cnec.SecurityStatus.FAILURE)) {
            return raoResult.getComputationStatus();
        } else {
            return ComputationStatus.FAILURE;
        }
    }

    public Cnec.SecurityStatus getSecurityStatus() {
        return voltageMonitoringResult.getStatus();
    }

    @Override
    public double getMinVoltage(Instant optimizationInstant, VoltageCnec voltageCnec, Unit unit) {
        unit.checkPhysicalParameter(PhysicalParameter.VOLTAGE);
        Optional<CnecResult> voltageCnecResultOpt = getCnecResult(optimizationInstant, voltageCnec);
        if (voltageCnecResultOpt.isPresent()) {
            return ((VoltageCnecValue) voltageCnecResultOpt.get().getValue()).minValue();
        } else {
            return Double.NaN;
        }
    }

    @Override
    public double getMaxVoltage(Instant optimizationInstant, VoltageCnec voltageCnec, Unit unit) {
        unit.checkPhysicalParameter(PhysicalParameter.VOLTAGE);
        Optional<CnecResult> voltageCnecResultOpt = getCnecResult(optimizationInstant, voltageCnec);
        if (voltageCnecResultOpt.isPresent()) {
            return ((VoltageCnecValue) voltageCnecResultOpt.get().getValue()).maxValue();
        } else {
            return Double.NaN;
        }
    }

    private Optional<CnecResult> getCnecResult(Instant optimizationInstant, VoltageCnec voltageCnec) {
        if (optimizationInstant == null || !optimizationInstant.isCurative()) {
            throw new OpenRaoException("Unexpected optimization instant for voltage monitoring result (only curative instant is supported currently) : " + optimizationInstant);
        }
        return voltageMonitoringResult.getCnecResults().stream().filter(voltageCnecRes -> voltageCnecRes.getId().equals(voltageCnec.getId())).findFirst();
    }

    @Override
    public double getMargin(Instant optimizationInstant, VoltageCnec voltageCnec, Unit unit) {
        unit.checkPhysicalParameter(PhysicalParameter.VOLTAGE);
        if (optimizationInstant == null || !optimizationInstant.isCurative()) {
            throw new OpenRaoException("Unexpected optimization instant for voltage monitoring result (only curative instant is supported currently): " + optimizationInstant);
        }

        Optional<CnecResult> voltageCnecResultOpt = getCnecResult(optimizationInstant, voltageCnec);
        return voltageCnecResultOpt.map(CnecResult::getMargin).orElse(Double.NaN);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        Set<NetworkAction> concatenatedActions = new HashSet<>(raoResult.getActivatedNetworkActionsDuringState(state));
        Set<RemedialAction> voltageMonitoringRas = voltageMonitoringResult.getAppliedRas(state);
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
    public boolean isSecure(PhysicalParameter... u) {
        List<PhysicalParameter> physicalParameters = new ArrayList<>(Stream.of(u).sorted().toList());
        if (physicalParameters.remove(PhysicalParameter.VOLTAGE)) {
            return raoResult.isSecure(physicalParameters.toArray(new PhysicalParameter[0])) && voltageMonitoringResult.getStatus().equals(Cnec.SecurityStatus.SECURE);
        } else {
            return raoResult.isSecure(u);
        }
    }
}
