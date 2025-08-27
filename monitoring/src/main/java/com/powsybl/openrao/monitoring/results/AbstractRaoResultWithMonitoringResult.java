/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
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
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.RaoResultClone;
import com.powsybl.openrao.monitoring.SecurityStatus;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public abstract class AbstractRaoResultWithMonitoringResult<I extends Cnec<?>> extends RaoResultClone implements RaoResultWithMonitoringResult<I> {
    protected final RaoResult raoResult;
    protected final MonitoringResult<I> monitoringResult;

    protected AbstractRaoResultWithMonitoringResult(RaoResult raoResult, MonitoringResult<I> monitoringResult) {
        super(raoResult);
        this.raoResult = raoResult;
        if (monitoringResult == null) {
            throw new OpenRaoException("Monitoring result must not be null");
        }
        this.monitoringResult = monitoringResult;
    }

    @Override
    public ComputationStatus getComputationStatus() {
        if (!monitoringResult.getStatus().equals(SecurityStatus.FAILURE)) {
            return raoResult.getComputationStatus();
        } else {
            return ComputationStatus.FAILURE;
        }
    }

    @Override
    public SecurityStatus getSecurityStatus() {
        return monitoringResult.getStatus();
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        Set<NetworkAction> concatenatedActions = new HashSet<>(raoResult.getActivatedNetworkActionsDuringState(state));
        Set<RemedialAction<?>> angleMonitoringRas = monitoringResult.getAppliedRas(state);
        Set<NetworkAction> angleMonitoringNetworkActions = angleMonitoringRas.stream().filter(NetworkAction.class::isInstance).map(ra -> (NetworkAction) ra).collect(Collectors.toSet());
        concatenatedActions.addAll(angleMonitoringNetworkActions);
        return concatenatedActions;
    }

    @Override
    public boolean isActivatedDuringState(State state, RemedialAction<?> remedialAction) {
        return monitoringResult.getAppliedRas(state).contains(remedialAction) || raoResult.isActivatedDuringState(state, remedialAction);
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        return isActivatedDuringState(state, (RemedialAction<?>) networkAction);
    }

    @Override
    public boolean isSecure() {
        return raoResult.isSecure() && monitoringResult.getStatus().equals(SecurityStatus.SECURE);
    }

    protected static void checkInstant(Instant instant, PhysicalParameter physicalParameter) {
        if (instant == null || !instant.isCurative()) {
            throw new OpenRaoException("Unexpected optimization instant for %s monitoring result (only curative instant is currently supported): %s.".formatted(physicalParameter.toString().toLowerCase(), instant));
        }
    }
}
