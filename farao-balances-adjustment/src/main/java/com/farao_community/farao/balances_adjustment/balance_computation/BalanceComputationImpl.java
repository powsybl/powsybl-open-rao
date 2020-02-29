/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.balances_adjustment.balance_computation;

import com.farao_community.farao.balances_adjustment.util.NetworkArea;
import com.powsybl.action.util.Scalable;
import com.powsybl.commons.PowsyblException;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * This class contains the balance adjustment computation process.
 * <p>
 *     The calculation starts with defined network and areas and consists
 *     of several stages :
 * <ul>
 *     <li>Input data validation</li>
 *     <li>LoadFlow computation</li>
 *     <li>Comparison of network area's net position with the target value</li>
 *     <li>Apply injections scaling</li>
 * </ul>
 * </p>
 *
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class BalanceComputationImpl implements BalanceComputation {

    private static final Logger LOGGER = LoggerFactory.getLogger(BalanceComputationImpl.class);

    private final Network network;

    /**
     * The target net position for each network area
     */
    private final Map<NetworkArea, Double> networkAreaNetPositionTargetMap;

    /**
     * The scalable for each network area.
     * Scalable contains a list of injections (generator or load)
     * @see Scalable;
     */
    private final Map<NetworkArea, Scalable> networkAreasScalableMap;

    private final ComputationManager computationManager;
    private final LoadFlow.Runner loadFlowRunner;

    public BalanceComputationImpl(Network network, Map<NetworkArea, Double> networkAreaNetPositionTargetMap, Map<NetworkArea, Scalable> networkAreasScalableMap, ComputationManager computationManager, LoadFlow.Runner loadFlowRunner) {
        this.network = Objects.requireNonNull(network);
        this.networkAreaNetPositionTargetMap = Objects.requireNonNull(networkAreaNetPositionTargetMap);
        this.networkAreasScalableMap = Objects.requireNonNull(networkAreasScalableMap);
        this.computationManager = Objects.requireNonNull(computationManager);
        this.loadFlowRunner = Objects.requireNonNull(loadFlowRunner);
    }

    /**
     * Run balances adjustment computation in several iterations
     */
    @Override
    public CompletableFuture<BalanceComputationResult> run(String workingStateId, BalanceComputationParameters parameters) {
        Objects.requireNonNull(workingStateId);
        Objects.requireNonNull(parameters);

        BalanceComputationResult result;
        int iterationCounter = 0;

        // Step 1: Input data validation
        List<String> inputDataViolations = this.listInputDataViolations();
        if (!inputDataViolations.isEmpty()) {
            inputDataViolations.forEach(LOGGER::error);
            throw new PowsyblException("The input data for balance computation is not valid");
        }

        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        String workingVariantCopyId = workingStateId + " COPY";
        network.getVariantManager().cloneVariant(workingStateId, workingVariantCopyId);
        network.getVariantManager().setWorkingVariant(workingVariantCopyId);

        Map<NetworkArea, Double> balanceOffsets = new HashMap<>();

        do {
            // Step 2: Perform the scaling
            for (Map.Entry<NetworkArea, Double> entry : balanceOffsets.entrySet()) {
                NetworkArea na = entry.getKey();
                double asked = entry.getValue();

                Scalable scalable = networkAreasScalableMap.get(na);
                double done = scalable.scale(network, balanceOffsets.get(na));
                LOGGER.debug("Scaling for area {}: asked={}, done={}", na.getName(), asked, done);
            }

            // Step 3: compute Loadflow
            LoadFlowResult loadFlowResult = loadFlowRunner.run(network, workingVariantCopyId, computationManager, parameters.getLoadFlowParameters());
            if (!loadFlowResult.isOk()) {
                LOGGER.error("Loadflow on network {} does not converge", network.getId());
                result = new BalanceComputationResult(BalanceComputationResult.Status.FAILED, iterationCounter);
                return CompletableFuture.completedFuture(result);
            }

            // Step 4: Compute balance and mismatch for each area
            double mismatchesNorm = 0.0;
            for (Map.Entry<NetworkArea, Double> entry : networkAreaNetPositionTargetMap.entrySet()) {
                NetworkArea na = entry.getKey();
                double target = entry.getValue();
                double balance = na.getNetPosition(network);
                double oldMismatch = balanceOffsets.computeIfAbsent(na, k -> 0.0);
                double mismatch = target - balance;
                balanceOffsets.put(na, oldMismatch + mismatch);
                LOGGER.debug("Mistmatch for area {}: {} (target={}, balance={})", na.getName(), mismatch, target, balance);

                mismatchesNorm += mismatch * mismatch;
            }

            // Step 5: Checks balance adjustment results
            if (mismatchesNorm < parameters.getThresholdNetPosition()) {
                result = new BalanceComputationResult(BalanceComputationResult.Status.SUCCESS, ++iterationCounter, balanceOffsets);
                network.getVariantManager().cloneVariant(workingVariantCopyId, workingStateId, true);
            } else {
                // Reset current variant with initial state
                network.getVariantManager().cloneVariant(workingStateId, workingVariantCopyId, true);
                result = new BalanceComputationResult(BalanceComputationResult.Status.FAILED, ++iterationCounter, balanceOffsets);
            }
        } while (iterationCounter < parameters.getMaxNumberIterations() && result.getStatus() != BalanceComputationResult.Status.SUCCESS);

        if (result.getStatus() == BalanceComputationResult.Status.SUCCESS) {
            List<String> networkAreasName = networkAreaNetPositionTargetMap.keySet().stream()
                    .map(NetworkArea::getName).collect(Collectors.toList());
            LOGGER.info(" Network areas : {} are balanced after {} iterations", networkAreasName, result.getIterationCount());

        } else {
            LOGGER.error(" Network areas are unbalanced after {} iterations", iterationCounter);
        }

        network.getVariantManager().removeVariant(workingVariantCopyId);
        network.getVariantManager().setWorkingVariant(initialVariantId);

        return CompletableFuture.completedFuture(result);
    }

    /**
     * @return the list of input data violations
     * If this list is empty, the balance computation continue
     */
    List<String> listInputDataViolations() {
        List<String> listOfViolations = new ArrayList<>();

        List<String> listNullElementsErrors = listNullElementsViolations(networkAreaNetPositionTargetMap, networkAreasScalableMap);
        if (!listNullElementsErrors.isEmpty()) {
            return listNullElementsErrors;
        }
        //Areas Voltage levels validation
        for (NetworkArea networkArea : networkAreaNetPositionTargetMap.keySet()) {

            List<VoltageLevel> areaVoltageLevels = networkArea.getAreaVoltageLevels(network);

            //Areas Voltage levels validation
            if (areaVoltageLevels.isEmpty()) {
                listOfViolations.add("The " + networkArea + " is not found in the network " + network);
            } else {
                List<VoltageLevel> networkVoltageLevels = network.getVoltageLevelStream().collect(Collectors.toList());
                if (!networkVoltageLevels.containsAll(areaVoltageLevels)) {
                    listOfViolations.add("The " + network + " doesn't contain all voltage levels of " + networkArea);
                }
            }

            // Injections validation
            listOfViolations.addAll(listNetworkAreaInjectionsViolations(networkArea, networkAreasScalableMap));

        }

        return listOfViolations;
    }

    /**
     * @return the list of input data violations that relate to the injections definition
     */
    private List<String> listNetworkAreaInjectionsViolations(NetworkArea networkArea, Map<NetworkArea, Scalable> networkAreasScalableMap) {
        List<String> listOfViolations = new ArrayList<>();
        if (!networkAreasScalableMap.containsKey(networkArea)) {
            listOfViolations.add("The " + networkArea.getName() + " is not defined in the scalable network areas map");

        } else {
            Scalable scalable = networkAreasScalableMap.get(networkArea);
            List<Injection> injections = new ArrayList<>();
            List<String> injectionsNotFoundInNetwork = new ArrayList<>();
            scalable.filterInjections(network, injections, injectionsNotFoundInNetwork);
            String s = "The scalable of " + networkArea;

            if (!injectionsNotFoundInNetwork.isEmpty()) {
                listOfViolations.add(s + " contains injections " + injectionsNotFoundInNetwork + " not found in the network");
            }
            if (injections.isEmpty()) {
                listOfViolations.add(s + " doesn't contain injections in network");
            } else {
                List injectionsNotInNetworkArea = injections.stream().filter(injection -> !networkArea.getAreaVoltageLevels(network).contains(injection.getTerminal().getVoltageLevel())).collect(Collectors.toList());
                if (!injectionsNotInNetworkArea.isEmpty()) {
                    listOfViolations.add(s + " contains injections " + injectionsNotInNetworkArea + " not found in the network area");
                }
            }
        }
        return listOfViolations;
    }

    /**
     * Check if there is <code>null</code> elements on input data
     * @return the list of null elements violations
     */
    private List<String> listNullElementsViolations(Map<NetworkArea, Double> networkAreaNetPositionTargetMap, Map<NetworkArea, Scalable> networkAreasScalableMap) {
        List<String> listOfViolations = new ArrayList<>();
        String error;
        if (networkAreaNetPositionTargetMap.containsKey(null)) {
            error = "The net position target map contains null network areas";
            listOfViolations.add(error);
            LOGGER.error(error);
        }

        if (networkAreaNetPositionTargetMap.containsValue(null)) {
            error = "The net position target map contains null values";
            listOfViolations.add(error);
            LOGGER.error(error);
        }

        if (networkAreasScalableMap.containsKey(null)) {
            error = "The scalable network areas map contains null network areas";
            listOfViolations.add(error);
            LOGGER.error(error);
        }
        if (networkAreasScalableMap.containsValue(null)) {
            error = "The scalable network areas map contains null values";
            listOfViolations.add(error);
            LOGGER.error(error);
        }
        return listOfViolations;
    }
}
