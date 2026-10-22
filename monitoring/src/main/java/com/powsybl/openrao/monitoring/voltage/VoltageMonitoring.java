/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.voltage;

import com.powsybl.computation.ComputationManager;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.AbstractMonitoring;
import com.powsybl.openrao.monitoring.MonitoringInput;
import com.powsybl.openrao.monitoring.results.MonitoringResult;
import com.powsybl.openrao.monitoring.results.RaoResultWithVoltageMonitoring;

import java.util.Set;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class VoltageMonitoring extends AbstractMonitoring<VoltageCnec> {

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
    public static RaoResult runAndUpdateRaoResult(String loadFlowProvider,
                                                  LoadFlowParameters loadFlowParameters,
                                                  int numberOfLoadFlowsInParallel,
                                                  MonitoringInput monitoringInput) {

        checkPhysicalParameter(monitoringInput, PhysicalParameter.VOLTAGE);
        final MonitoringResult voltageMonitoringResult = new VoltageMonitoring(loadFlowProvider, loadFlowParameters).runMonitoring(monitoringInput, numberOfLoadFlowsInParallel);
        return new RaoResultWithVoltageMonitoring(monitoringInput.getRaoResult(), voltageMonitoringResult);
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

        checkPhysicalParameter(monitoringInput, PhysicalParameter.VOLTAGE);
        final MonitoringResult voltageMonitoringResult = new VoltageMonitoring(
            loadFlowProvider,
            loadFlowParameters,
            computationManager
        ).runMonitoring(monitoringInput, numberOfLoadFlowsInParallel);
        return new RaoResultWithVoltageMonitoring(monitoringInput.getRaoResult(), voltageMonitoringResult);
    }

    @Override
    protected Set<NetworkAction> getValidNetworkActionsAssociatedToCnec(Network network, Crac crac, Cnec cnec, PhysicalParameter physicalParameter, ZonalData<Scalable> scalableZonalData) {
        BUSINESS_WARNS.warn("No actions can be applied in voltage monitoring");
        return Set.of();
    }

    @Override
    protected void applyNetworkActions(Network network, Set<NetworkAction> networkActionsToApply, ZonalData<Scalable> scalableZonalData, State state) {
        BUSINESS_WARNS.warn("No actions can be applied in voltage monitoring");
    }

    @Override
    protected VoltageCnecMonitoringResult computeCnecResult(VoltageCnec cnec, Network network, Unit unit) {
        return new VoltageCnecMonitoringResult(cnec, unit, network);
    }

    @Override
    protected Set<VoltageCnec> getCnecs(Crac crac) {
        return crac.getVoltageCnecs();
    }

    @Override
    protected VoltageCnecMonitoringResult makeFailedCnecResult(VoltageCnec cnec) {
        return new VoltageCnecMonitoringResult(cnec, Unit.KILOVOLT, Double.NaN, Double.NaN);
    }

}
