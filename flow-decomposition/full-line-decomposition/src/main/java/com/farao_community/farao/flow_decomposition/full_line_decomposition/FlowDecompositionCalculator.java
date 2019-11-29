/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.MonitoredBranch;
import com.farao_community.farao.data.flow_decomposition_results.FlowDecompositionResults;
import com.farao_community.farao.data.flow_decomposition_results.PerBranchResult;
import org.ejml.data.DMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Flow decomposition calculator based on calculated matrix for
 * PEX, PTDF and PSDF
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowDecompositionCalculator {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowDecompositionCalculator.class);
    private CracFile inputs;
    private FullLineDecompositionParameters parameters;
    private DMatrix pexMatrix;
    private DMatrix ptdfMatrix;
    private DMatrix psdfMatrix;
    private Map<Bus, Integer> busMapper;
    private Map<Branch, Integer> branchMapper;
    private Network network;
    List<Bus> busesOfInterest = new ArrayList<>();
    List<TwoWindingsTransformer> branchesOfInterest = new ArrayList<>();

    public FlowDecompositionCalculator(Network network, CracFile inputs, FullLineDecompositionParameters parameters, DMatrix pexMatrix, DMatrix ptdfMatrix, DMatrix psdfMatrix, Map<Bus, Integer> busMapper, Map<Branch, Integer> branchMapper) {
        this.inputs = Objects.requireNonNull(inputs);
        this.parameters = Objects.requireNonNull(parameters);
        this.pexMatrix = Objects.requireNonNull(pexMatrix);
        this.ptdfMatrix = Objects.requireNonNull(ptdfMatrix);
        this.psdfMatrix = Objects.requireNonNull(psdfMatrix);
        this.busMapper = Objects.requireNonNull(busMapper);
        this.branchMapper = Objects.requireNonNull(branchMapper);
        this.network = Objects.requireNonNull(network);
    }

    /**
     * Multi threaded decomposition of the flows per monitored branch
     */
    public FlowDecompositionResults computeDecomposition() {
        busesOfInterest = busMapper.keySet().stream().filter(bus -> NetworkUtil.getInjectionStream(bus).count() > 0).collect(Collectors.toList());
        branchesOfInterest = branchMapper.keySet().stream().filter(NetworkUtil::branchIsPst).map(branch -> (TwoWindingsTransformer) branch).collect(Collectors.toList());
        FlowDecompositionResults results = new FlowDecompositionResults();
        ExecutorService executor = Executors.newFixedThreadPool(parameters.getThreadsNumber());
        List<Future> tasks = inputs.getPreContingency().getMonitoredBranches().stream()
                .map(monitoredBranch -> executor.submit(() -> decomposeFlow(monitoredBranch, results))).collect(Collectors.toList());
        tasks.stream().forEach(task -> {
            try {
                task.get();
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted exception in flow decomposition calculator.");
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                throw new UncheckedExecutionException(e);
            }
        });
        executor.shutdown();
        return results;
    }

    private void decomposeFlow(MonitoredBranch monitoredBranch, FlowDecompositionResults results) {
        LOGGER.info("Decomposing flow on monitored branch {}", monitoredBranch.getId());
        Branch branch = network.getBranch(monitoredBranch.getBranchId());

        if (branch == null) {
            // No decomposition if branch does not exist in network TODO : check at the beginning ?
            return;
        }

        double directionFactor = Math.signum(branch.getTerminal1().getP());

        String branchId = branch.getId();
        String branchCountry1 = NetworkUtil.getBranchSideCountry(branch, Branch.Side.ONE).name();
        String branchCountry2 = NetworkUtil.getBranchSideCountry(branch, Branch.Side.TWO).name();
        double referenceFlows = directionFactor * branch.getTerminal1().getP();
        double maximumFlows = monitoredBranch.getFmax();
        Table<String, String, Double> countryExchangeFlows = HashBasedTable.create();
        Map<String, Double> countryPstFlows = new HashMap<>();

        if (!NetworkUtil.isConnectedAndInMainSynchronous(branch)) {
            // Fill with zeros
            Set<Country> countries = network.getCountries();
            for (Country countryFrom: countries) {
                for (Country countryTo: countries) {
                    countryExchangeFlows.put(countryFrom.name(), countryTo.name(), 0.);
                }
                countryPstFlows.put(countryFrom.name(), 0.);
            }
            PerBranchResult perBranchResult = PerBranchResult.builder()
                    .branchId(branchId)
                    .branchCountry1(branchCountry1)
                    .branchCountry2(branchCountry2)
                    .referenceFlows(0.)
                    .maximumFlows(maximumFlows)
                    .countryExchangeFlows(countryExchangeFlows)
                    .countryPstFlows(countryPstFlows)
                    .build();
            results.addPerBranchResult(branchId, perBranchResult);
            return;
        }

        int branchIndex = branchMapper.get(branch);
        for (Bus busFrom : busesOfInterest) {
            for (Bus busTo : busesOfInterest) {
                Country countryFrom = busFrom.getVoltageLevel().getSubstation().getCountry().orElse(null);
                Country countryTo = busTo.getVoltageLevel().getSubstation().getCountry().orElse(null);
                int busFromIndex = busMapper.get(busFrom);
                int busToIndex = busMapper.get(busTo);

                double increase = directionFactor * (ptdfMatrix.get(branchIndex, busFromIndex) - ptdfMatrix.get(branchIndex, busToIndex)) * pexMatrix.get(busFromIndex, busToIndex);

                if (countryFrom != null && countryTo != null) {
                    if (!countryExchangeFlows.contains(countryFrom.name(), countryTo.name())) {
                        countryExchangeFlows.put(countryFrom.name(), countryTo.name(), increase);
                    } else {
                        countryExchangeFlows.put(countryFrom.name(), countryTo.name(), countryExchangeFlows.get(countryFrom.name(), countryTo.name()) + increase);
                    }
                }
            }
        }

        for (TwoWindingsTransformer pst : branchesOfInterest) {
            Country pstCountry = NetworkUtil.getBranchSideCountry(pst, Branch.Side.ONE);
            float pstAngle = (float) pst.getPhaseTapChanger().getCurrentStep().getAlpha();
            int pstBranchIndex = branchMapper.get(pst);
            countryPstFlows.put(pstCountry.name(), directionFactor * psdfMatrix.get(branchIndex, pstBranchIndex) * pstAngle);
        }

        PerBranchResult perBranchResult = PerBranchResult.builder()
                .branchId(branchId)
                .branchCountry1(branchCountry1)
                .branchCountry2(branchCountry2)
                .referenceFlows(referenceFlows)
                .maximumFlows(maximumFlows)
                .countryExchangeFlows(countryExchangeFlows)
                .countryPstFlows(countryPstFlows)
                .build();
        results.addPerBranchResult(branchId, perBranchResult);
    }
}
