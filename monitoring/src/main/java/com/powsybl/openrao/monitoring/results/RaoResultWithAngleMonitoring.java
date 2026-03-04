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
import com.powsybl.openrao.data.crac.api.cnec.Cnec.SecurityStatus;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.RaoResultClone;
import com.powsybl.openrao.data.raoresult.api.extension.AngleExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * class that enhances rao result with angle monitoring results
 *
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class RaoResultWithAngleMonitoring extends RaoResultClone {

    private final RaoResult raoResult;
    private final MonitoringResult angleMonitoringResult;

    public RaoResultWithAngleMonitoring(RaoResult raoResult, MonitoringResult angleMonitoringResult) {
        super(raoResult);
        this.raoResult = raoResult;
        if (angleMonitoringResult == null) {
            throw new OpenRaoException("AngleMonitoringResult must not be null");
        }
        this.angleMonitoringResult = angleMonitoringResult;
        AngleExtension angleExtension = AngleMonitoringResultAdapter.convertToAngleExtension(angleMonitoringResult);
        this.addExtension(AngleExtension.class, angleExtension);
    }

    @Override
    public ComputationStatus getComputationStatus() {
        if (!angleMonitoringResult.getStatus().equals(SecurityStatus.FAILURE)) {
            return raoResult.getComputationStatus();
        } else {
            return ComputationStatus.FAILURE;
        }
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        Set<NetworkAction> concatenatedActions = new HashSet<>(raoResult.getActivatedNetworkActionsDuringState(state));
        Set<RemedialAction> angleMonitoringRas = angleMonitoringResult.getAppliedRas(state);
        Set<NetworkAction> angleMonitoringNetworkActions = angleMonitoringRas.stream().filter(NetworkAction.class::isInstance).map(ra -> (NetworkAction) ra).collect(Collectors.toSet());
        concatenatedActions.addAll(angleMonitoringNetworkActions);
        return concatenatedActions;
    }

    @Override
    public boolean isActivatedDuringState(State state, RemedialAction<?> remedialAction) {
        return angleMonitoringResult.getAppliedRas(state).contains(remedialAction) || raoResult.isActivatedDuringState(state, remedialAction);
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        return isActivatedDuringState(state, (RemedialAction<?>) networkAction);
    }

    @Override
    public boolean isSecure(Instant instant, PhysicalParameter... u) {
        List<PhysicalParameter> physicalParameters = new ArrayList<>(Stream.of(u).sorted().toList());
        if (physicalParameters.remove(PhysicalParameter.ANGLE)) {
            return raoResult.isSecure(instant, physicalParameters.toArray(new PhysicalParameter[0])) && angleMonitoringResult.getStatus().equals(SecurityStatus.SECURE);
        } else {
            return raoResult.isSecure(instant, u);
        }
    }

    @Override
    public boolean isSecure(PhysicalParameter... u) {
        List<PhysicalParameter> physicalParameters = new ArrayList<>(Stream.of(u).sorted().toList());
        if (physicalParameters.remove(PhysicalParameter.ANGLE)) {
            return raoResult.isSecure(physicalParameters.toArray(new PhysicalParameter[0])) && angleMonitoringResult.getStatus().equals(SecurityStatus.SECURE);
        } else {
            return raoResult.isSecure(u);
        }
    }

    @Override
    public boolean isSecure() {
        return raoResult.isSecure() && angleMonitoringResult.getStatus().equals(SecurityStatus.SECURE);
    }
}
