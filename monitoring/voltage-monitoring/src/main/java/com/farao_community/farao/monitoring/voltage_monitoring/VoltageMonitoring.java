/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.voltage_monitoring;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.util.AbstractNetworkPool;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Monitors voltage of VoltageCnecs
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class VoltageMonitoring {
    public static final String CONTINGENCY_ERROR = "At least one contingency could not be monitored within the given time (24 hours). This should not happen.";
    private final Crac crac;
    private final Network network;
    private final RaoResult raoResult;

    public VoltageMonitoring(Crac crac, Network network, RaoResult raoResult) {
        this.crac = crac;
        this.network = network;
        this.raoResult = raoResult;
    }

    public VoltageMonitoringResult run(String loadFlowProvider, LoadFlowParameters loadFlowParameters, int numberOfLoadFlowsInParallel) {
        applyOptimalRemedialActions(crac.getPreventiveState(), network);
        Map<VoltageCnec, ExtremeVoltageValues> voltageValues = new ConcurrentHashMap<>(computeVoltages(crac.getVoltageCnecs(crac.getPreventiveState()), network, loadFlowProvider, loadFlowParameters));

        Set<State> contingencyStates = crac.getVoltageCnecs().stream().map(Cnec::getState).filter(state -> !state.isPreventive()).collect(Collectors.toSet());

        if (contingencyStates.isEmpty()) {
            return new VoltageMonitoringResult(voltageValues);
        }

        try {

            int numberOfClones = Math.min(numberOfLoadFlowsInParallel, contingencyStates.size());

            try (AbstractNetworkPool networkPool =
                     AbstractNetworkPool.create(network, network.getVariantManager().getWorkingVariantId(), numberOfClones)
            ) {

                if (numberOfClones != 1) {
                    networkPool.addNetworkClones(numberOfClones);
                }

                CountDownLatch stateCountDownLatch = new CountDownLatch(contingencyStates.size());
                contingencyStates.forEach(state ->
                    networkPool.submit(() -> {
                        Network networkClone = null;
                        try {
                            networkClone = networkPool.getAvailableNetwork();
                        } catch (InterruptedException e) {
                            stateCountDownLatch.countDown();
                            Thread.currentThread().interrupt();
                            throw new FaraoException(CONTINGENCY_ERROR, e);
                        }
                        try {
                            state.getContingency().orElseThrow().apply(networkClone, null);
                            applyOptimalRemedialActionsOnContingencyState(state, networkClone);
                            voltageValues.putAll(computeVoltages(crac.getVoltageCnecs(state), networkClone, loadFlowProvider, loadFlowParameters));
                        } catch (Exception e) {
                            stateCountDownLatch.countDown();
                            Thread.currentThread().interrupt();
                            throw new FaraoException(CONTINGENCY_ERROR, e);
                        }
                        try {
                            networkPool.releaseUsedNetwork(networkClone);
                            stateCountDownLatch.countDown();
                        } catch (InterruptedException ex) {
                            stateCountDownLatch.countDown();
                            Thread.currentThread().interrupt();
                            throw new FaraoException(ex);
                        }
                    }
                ));
                boolean success = stateCountDownLatch.await(24, TimeUnit.HOURS);
                if (!success) {
                    throw new FaraoException(CONTINGENCY_ERROR);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new VoltageMonitoringResult(voltageValues);
    }

    private void applyOptimalRemedialActionsOnContingencyState(State state, Network networkClone) {
        if (state.getInstant().equals(Instant.CURATIVE)) {
            Optional<Contingency> contingency = state.getContingency();
            if (contingency.isPresent()) {
                crac.getStates(contingency.get()).forEach(contingencyState ->
                        applyOptimalRemedialActions(state, networkClone));
            } else {
                throw new FaraoException(String.format("Curative state %s was defined without a contingency", state.getId()));

            }
        } else {
            applyOptimalRemedialActions(state, networkClone);
        }
    }

    private void applyOptimalRemedialActions(State state, Network network) {
        raoResult.getActivatedNetworkActionsDuringState(state)
            .forEach(na -> na.apply(network));
        raoResult.getActivatedRangeActionsDuringState(state)
            .forEach(ra -> ra.apply(network, raoResult.getOptimizedSetPointOnState(state, ra)));
    }

    private Map<VoltageCnec, ExtremeVoltageValues> computeVoltages(Set<VoltageCnec> voltageCnecs, Network network, String loadFlowProvider, LoadFlowParameters loadFlowParameters) {
        LoadFlowResult loadFlowResult = LoadFlow.find(loadFlowProvider)
            .run(network, loadFlowParameters);
        if (!loadFlowResult.isOk()) {
            throw new FaraoException("LoadFlow error");
        }

        Map<VoltageCnec, ExtremeVoltageValues> voltagePerCnec = new HashMap<>();
        voltageCnecs.forEach(vc -> {
            VoltageLevel voltageLevel = network.getVoltageLevel(vc.getNetworkElement().getId());
            Set<Double> voltages = voltageLevel.getBusView().getBusStream().map(Bus::getV).collect(Collectors.toSet());
            voltagePerCnec.put(vc, new ExtremeVoltageValues(voltages));
        });
        return voltagePerCnec;
    }
}
