/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.RaoResultClone;
import com.powsybl.openrao.data.raoresult.api.extension.VoltageResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
        if (voltageMonitoringResult == null) {
            throw new OpenRaoException("VoltageMonitoringResult must not be null");
        }
        this.voltageMonitoringResult = voltageMonitoringResult;
        VoltageResult voltageResult = VoltageMonitoringResultAdapter.convertToVoltageExtension(voltageMonitoringResult);
        this.addExtension(VoltageResult.class, voltageResult);
    }

    @Override
    public String getExecutionDetails() {
        return raoResult.getExecutionDetails() + " and went through voltage monitoring";
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

    Optional<CnecResult> getCnecResult(Instant optimizationInstant, VoltageCnec voltageCnec) {
        if (voltageCnec.getState().getInstant() != optimizationInstant) {
            throw new OpenRaoException(
                "Unexpected optimization instant for voltage monitoring result: "
                    + (optimizationInstant == null ? "initial" : optimizationInstant.getId())
                    + ". Only optimization instant equal to voltage cnec's instant is accepted: "
                    + voltageCnec.getState().getInstant().getId()
            );
        }
        return voltageMonitoringResult.getCnecResults().stream().filter(voltageCnecRes -> voltageCnecRes.getId().equals(voltageCnec.getId())).findFirst();
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
    public boolean isSecure(Instant instant, PhysicalParameter... u) {
        List<PhysicalParameter> physicalParameters = new ArrayList<>(Stream.of(u).sorted().toList());
        if (physicalParameters.remove(PhysicalParameter.VOLTAGE)) {
            return raoResult.isSecure(instant, physicalParameters.toArray(new PhysicalParameter[0])) && voltageMonitoringResult.getStatus().equals(Cnec.SecurityStatus.SECURE);
        } else {
            return raoResult.isSecure(instant, u);
        }
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

    @Override
    public boolean isSecure() {
        return raoResult.isSecure() && voltageMonitoringResult.getStatus().equals(Cnec.SecurityStatus.SECURE);
    }
}
