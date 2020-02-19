/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.flowbased_domain.DataDomain;
import com.farao_community.farao.data.flowbased_domain.DataMonitoredBranch;
import com.farao_community.farao.data.flowbased_domain.DataPreContingency;
import com.farao_community.farao.data.flowbased_domain.DataPtdfPerCountry;
import com.farao_community.farao.flowbased_computation.*;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.farao_community.farao.util.FaraoVariantsPool;
import com.farao_community.farao.util.SensitivityComputationService;
import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationResults;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFactorsProvider;
import com.powsybl.sensitivity.SensitivityValue;
import com.powsybl.sensitivity.factors.BranchFlowPerLinearGlsk;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.farao_community.farao.util.SystematicSensitivityAnalysisService.applyContingencyInCrac;

/**
 * Flowbased computation implementation
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(FlowBasedComputationProvider.class)
public class FlowBasedComputationImpl implements FlowBasedComputationProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowBasedComputationImpl.class);

    @Override
    public String getName() {
        return "SimpleIterativeFlowBased";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public CompletableFuture<FlowBasedComputationResult> run(Network network, Crac crac, GlskProvider glskProvider, ComputationManager computationManager, String workingVariantId, FlowBasedComputationParameters parameters) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(crac);
        Objects.requireNonNull(glskProvider);
        Objects.requireNonNull(computationManager);
        Objects.requireNonNull(workingVariantId);
        Objects.requireNonNull(parameters);

        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        network.getVariantManager().setWorkingVariant(workingVariantId);

        Map<String, Double> referenceFlows = Collections.synchronizedMap(new HashMap<>());
        Map<String, Map<String, Double>> ptdfs = Collections.synchronizedMap(new HashMap<>());
        computeAllPtdf(network, crac, glskProvider, computationManager, referenceFlows, ptdfs);

        FlowBasedComputationResult flowBasedComputationResult = new FlowBasedComputationResultImpl(FlowBasedComputationResult.Status.SUCCESS, buildFlowbasedDomain(crac, referenceFlows, ptdfs));

        network.getVariantManager().setWorkingVariant(initialVariantId);
        return CompletableFuture.completedFuture(flowBasedComputationResult);
    }

    private List<SensitivityFactor> generateSensitivityFactorsProvider(Network network, Set<Cnec> cnecs, GlskProvider glskProvider) {
        List<SensitivityFactor> factors = new ArrayList<>();
        Map<String, LinearGlsk> mapCountryLinearGlsk = glskProvider.getAllGlsk(network);
        cnecs.forEach(cnec -> mapCountryLinearGlsk.values().stream()
                .map(linearGlsk -> new BranchFlowPerLinearGlsk(new BranchFlow(cnec.getId(), cnec.getName(), cnec.getNetworkElement().getId()), linearGlsk))
                .forEach(factors::add));
        return factors;
    }

    private Map<String, Map<String, Double>> computeAllPtdf(Network network, Crac crac, GlskProvider glskProvider, ComputationManager computationManager, Map<String, Double> referenceFlows, Map<String, Map<String, Double>> ptdfs) {
        crac.synchronize(network);
        Set<Cnec> preventivecnecs = crac.getCnecs(crac.getPreventiveState());
        computePtdf(network, preventivecnecs, glskProvider, referenceFlows, ptdfs);

        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        try (FaraoVariantsPool variantsPool = new FaraoVariantsPool(network, initialVariantId)) {
            variantsPool.submit(() -> crac.getContingencies().parallelStream().forEach(contingency -> {
                // Create contingency variant
                try {
                    Set<State> states = new HashSet<>();
                    Set<Cnec> cnecscontingency = new HashSet<>();
                    states.addAll(crac.getStates(contingency));
                    states.forEach(state -> cnecscontingency.addAll(crac.getCnecs(state)));
                    LOGGER.info("Running post contingency sensitivity computation for contingency '{}'", contingency.getId());
                    String workingVariant = variantsPool.getAvailableVariant();
                    network.getVariantManager().setWorkingVariant(workingVariant);
                    applyContingencyInCrac(network, computationManager, contingency);

                    computePtdf(network, cnecscontingency, glskProvider, referenceFlows, ptdfs);
                    variantsPool.releaseUsedVariant(workingVariant);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            })).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new FaraoException(e);
        }
        network.getVariantManager().setWorkingVariant(initialVariantId);
        crac.desynchronize();
        return ptdfs;
    }

    private void computePtdf(Network network, Set<Cnec> cnecs, GlskProvider glskProvider, Map<String, Double> referenceFlows, Map<String, Map<String, Double>> ptdfs) {
        SensitivityFactorsProvider factorsProvider = net -> generateSensitivityFactorsProvider(net, cnecs, glskProvider);
        SensitivityComputationResults sensiResults = SensitivityComputationService.runSensitivity(network, network.getVariantManager().getWorkingVariantId(), factorsProvider);
        sensiResults.getSensitivityValues().forEach(sensitivityValue -> addSensitivityValue(sensitivityValue, referenceFlows, ptdfs));
    }

    private void addSensitivityValue(SensitivityValue sensitivityValue, Map<String, Double> referenceFlows, Map<String, Map<String, Double>> ptdfs) {
        String branchId = sensitivityValue.getFactor().getFunction().getId();
        String glskId = sensitivityValue.getFactor().getVariable().getId();
        double ptdfValue = sensitivityValue.getValue();
        double referenceFlow = sensitivityValue.getFunctionReference();

        if (!ptdfs.containsKey(branchId)) {
            ptdfs.put(branchId, new HashMap<>());
        }
        ptdfs.get(branchId).put(glskId, ptdfValue);
        referenceFlows.put(branchId, referenceFlow);
    }

    private DataDomain buildFlowbasedDomain(Crac crac, Map<String, Double> referenceFlows, Map<String, Map<String, Double>> ptdfs) {
        return DataDomain.builder()
                .id(UUID.randomUUID().toString())
                .name("FlowBased results")
                .description("")
                .sourceFormat("code")
                .dataPreContingency(buildDataPreContingency(crac, referenceFlows, ptdfs))
                .build();
    }

    private DataPreContingency buildDataPreContingency(Crac crac, Map<String, Double> referenceFlows, Map<String, Map<String, Double>> ptdfs) {
        return DataPreContingency.builder()
                .dataMonitoredBranches(buildDataMonitoredBranches(crac, referenceFlows, ptdfs))
                .build();
    }

    private List<DataMonitoredBranch> buildDataMonitoredBranches(Crac crac, Map<String, Double> referenceFlows, Map<String, Map<String, Double>> ptdfs) {
        Set<Cnec> preventivecnecs = crac.getCnecs(crac.getPreventiveState());
        List<DataMonitoredBranch> branchResultList = new ArrayList<>();
        preventivecnecs.forEach(cnec -> branchResultList.add(buildDataMonitoredBranch(cnec, referenceFlows, ptdfs)));

        crac.getContingencies().forEach(contingency -> {
            Set<State> states = new HashSet<>();
            Set<Cnec> cnecscontingency = new HashSet<>();
            states.addAll(crac.getStates(contingency));
            states.forEach(state -> cnecscontingency.addAll(crac.getCnecs(state)));
            cnecscontingency.forEach(cnec -> branchResultList.add(buildDataMonitoredBranch(cnec, referenceFlows, ptdfs)));
        });
        return branchResultList;
    }

    private DataMonitoredBranch buildDataMonitoredBranch(Cnec cnec, Map<String, Double> referenceFlows, Map<String, Map<String, Double>> ptdfs) {
        try {
            Optional<Double> maxThreshold = cnec.getThreshold().getMaxThreshold(Unit.MEGAWATT);
            double fmax = maxThreshold.orElse(Double.POSITIVE_INFINITY);
            return new DataMonitoredBranch(
                    cnec.getId(),
                    cnec.getName(),
                    cnec.getNetworkElement().getId(),
                    fmax,
                    referenceFlows.get(cnec.getId()),
                    buildDataPtdfPerCountry(cnec.getId(), ptdfs)
            );
        } catch (SynchronizationException e) {
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    private List<DataPtdfPerCountry> buildDataPtdfPerCountry(String branchId, Map<String, Map<String, Double>> ptdfs) {
        return ptdfs.get(branchId).entrySet().stream()
                .map(entry ->
                        new DataPtdfPerCountry(
                                entry.getKey(),
                                zeroIfNaN(entry.getValue())
                        )
                ).collect(Collectors.toList());
    }

    private double zeroIfNaN(double value) {
        return Double.isNaN(value) ? 0 : value;
    }
}
