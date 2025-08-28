/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.voltage;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.AbstractMonitoring;
import com.powsybl.openrao.monitoring.remedialaction.AppliedNetworkActionsResult;
import com.powsybl.openrao.monitoring.MonitoringInput;
import com.powsybl.openrao.monitoring.SecurityStatus;
import com.powsybl.openrao.monitoring.results.CnecResult;
import com.powsybl.openrao.monitoring.results.MonitoringResult;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class VoltageMonitoring extends AbstractMonitoring<VoltageCnec> {
    public VoltageMonitoring(String loadFlowProvider, LoadFlowParameters loadFlowParameters) {
        super(loadFlowProvider, loadFlowParameters, PhysicalParameter.VOLTAGE, Unit.KILOVOLT);
    }

    @Override
    protected void checkInputs(MonitoringInput monitoringInput) {
        if (monitoringInput.scalableZonalData() != null) {
            throw new OpenRaoException("Scalable Zonal Data is not supported as an input of Voltage Monitoring.");
        }
    }

    @Override
    protected MonitoringResult<VoltageCnec> makeEmptySecureResult() {
        return new VoltageMonitoringResult(Set.of(), Map.of(), SecurityStatus.SECURE);
    }

    @Override
    protected Set<VoltageCnec> getCnecs(Crac crac) {
        return crac.getVoltageCnecs();
    }

    @Override
    protected AppliedNetworkActionsResult applyNetworkActions(Network network, Set<NetworkAction> availableNetworkActions, String cnecId, MonitoringInput monitoringInput) {
        Set<NetworkAction> appliedNetworkActions = new TreeSet<>(Comparator.comparing(RemedialAction::getId));
        for (NetworkAction networkAction : availableNetworkActions) {
            networkAction.apply(network);
            appliedNetworkActions.add(networkAction);
        }
        AppliedNetworkActionsResult appliedNetworkActionsResult = new AppliedNetworkActionsResult.AppliedNetworkActionsResultBuilder().withAppliedNetworkActions(appliedNetworkActions)
            .withNetworkElementsToBeExcluded(new HashSet<>()).withPowerToBeRedispatched(new EnumMap<>(Country.class)).build();
        BUSINESS_LOGS.info("Applied the following remedial action(s) in order to reduce constraints on CNEC \"{}\": {}", cnecId, appliedNetworkActions.stream().map(com.powsybl.openrao.data.crac.api.Identifiable::getId).collect(Collectors.joining(", ")));
        return appliedNetworkActionsResult;
    }

    @Override
    protected MonitoringResult<VoltageCnec> makeMonitoringResult(Set<CnecResult<VoltageCnec>> cnecResults, Map<State, Set<NetworkAction>> appliedRemedialActions, SecurityStatus monitoringResultStatus) {
        return new VoltageMonitoringResult(cnecResults, appliedRemedialActions, monitoringResultStatus);
    }

    @Override
    protected MonitoringResult<VoltageCnec> makeFailedMonitoringResultForState(State state, String failureReason, Set<CnecResult<VoltageCnec>> cnecResults) {
        BUSINESS_WARNS.warn(failureReason);
        return new VoltageMonitoringResult(cnecResults, Map.of(state, Collections.emptySet()), SecurityStatus.FAILURE);
    }

    @Override
    protected CnecResult<VoltageCnec> computeCnecResult(VoltageCnec voltageCnec, Network network, Unit unit) {
        return VoltageCnecResultImpl.compute(voltageCnec, network, unit);
    }

    @Override
    protected CnecResult<VoltageCnec> makeFailedCnecResult(VoltageCnec voltageCnec) {
        return VoltageCnecResultImpl.failed(voltageCnec);
    }

    /**
     * Main function : runs VoltageMonitoring computation on all VoltageCnecs defined in the CRAC.
     * Returns an RaoResult enhanced with VoltageMonitoringResult
     */
    public static RaoResult runAndUpdateRaoResult(String loadFlowProvider, LoadFlowParameters loadFlowParameters, int numberOfLoadFlowsInParallel, MonitoringInput monitoringInput) {
        return new RaoResultWithVoltageMonitoring(monitoringInput.raoResult(), new VoltageMonitoring(loadFlowProvider, loadFlowParameters).runMonitoring(monitoringInput, numberOfLoadFlowsInParallel));
    }
}
