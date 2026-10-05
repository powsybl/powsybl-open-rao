/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring;

import com.powsybl.computation.ComputationManager;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.results.MonitoringResult;
import com.powsybl.openrao.monitoring.results.RaoResultWithVoltageMonitoring;

import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class VoltageMonitoring extends AbstractMonitoring {

    public VoltageMonitoring(String loadFlowProvider, LoadFlowParameters loadFlowParameters) {
        super(loadFlowProvider, loadFlowParameters);

    }

    public VoltageMonitoring(String loadFlowProvider, LoadFlowParameters loadFlowParameters, ComputationManager computationManager) {
        super(loadFlowProvider, loadFlowParameters, computationManager);
    }

    /**
     * Main function : runs VoltageMonitoring computation on all VoltageCnecs defined in the CRAC.
     * Returns an RaoResult enhanced with VoltageMonitoringResult
     */
    public static RaoResult runAndUpdateRaoResult(String loadFlowProvider, LoadFlowParameters loadFlowParameters, int numberOfLoadFlowsInParallel, MonitoringInput monitoringInput) {
        return new RaoResultWithVoltageMonitoring(monitoringInput.getRaoResult(), new VoltageMonitoring(loadFlowProvider, loadFlowParameters)
            .runMonitoring(monitoringInput, numberOfLoadFlowsInParallel));
    }

    /**
     * The computation manager can be used by the caller to execute actions before and/or after running the loadflow.
     * In particular, GridCapa relies on it to inject task-id in the MDC in order to bind logs with tasks.
     */
    public static RaoResult runAndUpdateRaoResult(String loadFlowProvider,
                                                         LoadFlowParameters loadFlowParameters,
                                                         ComputationManager computationManager,
                                                         int numberOfLoadFlowsInParallel,
                                                         MonitoringInput monitoringInput) {
        final MonitoringResult voltageMonitoringResult = new VoltageMonitoring(
            loadFlowProvider,
            loadFlowParameters,
            computationManager
        ).runMonitoring(monitoringInput, numberOfLoadFlowsInParallel);
        return new RaoResultWithVoltageMonitoring(monitoringInput.getRaoResult(), voltageMonitoringResult);
    }

    @Override
    protected Set<NetworkAction> getValidNetworkActionsAssociatedToCnec(Network network, Crac crac, Cnec cnec, PhysicalParameter physicalParameter, ZonalData<Scalable> scalableZonalData) {
        return crac.getNetworkActions().stream()
            .filter(networkAction ->
                networkAction.getUsageRules().stream()
                    .filter(OnConstraint.class::isInstance)
                    .map(OnConstraint.class::cast)
                    .anyMatch(onConstraint -> onConstraint.getCnec().equals(cnec)))
            .collect(Collectors.toSet());
    }

    @Override
    protected void rebalanceNetwork(Network network, Set<NetworkAction> networkActionsToApply, ZonalData<Scalable> scalableZonalData) {
        TECHNICAL_LOGS.warn("The network is not rebalanced after applying network actions in voltage monitoring");
    }
}
