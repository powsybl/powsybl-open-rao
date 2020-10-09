/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.flowbased_domain.*;
import com.farao_community.farao.flowbased_computation.*;
import com.farao_community.farao.data.glsk.import_.glsk_provider.GlskProvider;
import com.farao_community.farao.sensitivity_computation.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_computation.SystematicSensitivityResult;
import com.farao_community.farao.commons.RandomizedString;
import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

        SystematicSensitivityInterface systematicSensitivityInterface = SystematicSensitivityInterface.builder()
                .withDefaultParameters(parameters.getSensitivityComputationParameters())
                .withPtdfSensitivities(glskProvider, crac.getCnecs())
                .build();

        SystematicSensitivityResult result = systematicSensitivityInterface.run(network, Unit.MEGAWATT);
        FlowBasedComputationResult flowBasedComputationResult = new FlowBasedComputationResultImpl(FlowBasedComputationResult.Status.SUCCESS, buildFlowbasedDomain(network, crac, glskProvider, result));

        network.getVariantManager().setWorkingVariant(initialVariantId);
        return CompletableFuture.completedFuture(flowBasedComputationResult);
    }

    private DataDomain buildFlowbasedDomain(Network network, Crac crac, GlskProvider glskProvider, SystematicSensitivityResult result) {
        return DataDomain.builder()
                .id(RandomizedString.getRandomizedString())
                .name("FlowBased results")
                .description("")
                .sourceFormat("code")
                .dataPreContingency(buildDataPreContingency(network, crac, glskProvider, result))
                .dataPostContingency(buildDataPostContingencies(network, crac, glskProvider, result))
                .build();
    }

    private List<DataPostContingency> buildDataPostContingencies(Network network, Crac crac, GlskProvider glskProvider, SystematicSensitivityResult result) {
        List<DataPostContingency> postContingencyList = new ArrayList<>();
        crac.getContingencies().forEach(contingency -> postContingencyList.add(buildDataPostContingency(network, crac, contingency, glskProvider, result)));
        return postContingencyList;
    }

    private DataPostContingency buildDataPostContingency(Network network, Crac crac, Contingency contingency, GlskProvider glskProvider, SystematicSensitivityResult result) {
        return DataPostContingency.builder()
                .contingencyId(contingency.getId())
                .dataMonitoredBranches(buildDataMonitoredBranches(network, crac, crac.getStates(contingency), glskProvider, result))
                .build();
    }

    private DataPreContingency buildDataPreContingency(Network network, Crac crac, GlskProvider glskProvider, SystematicSensitivityResult result) {
        return DataPreContingency.builder()
                .dataMonitoredBranches(buildDataMonitoredBranches(network, crac, Set.of(crac.getPreventiveState()), glskProvider, result))
                .build();
    }

    private List<DataMonitoredBranch> buildDataMonitoredBranches(Network network, Crac crac, Set<State> states, GlskProvider glskProvider, SystematicSensitivityResult result) {
        List<DataMonitoredBranch> branchResultList = new ArrayList<>();
        states.forEach(state -> crac.getCnecs(state).forEach(cnec -> branchResultList.add(buildDataMonitoredBranch(network, cnec, glskProvider, result))));
        return branchResultList;
    }

    private DataMonitoredBranch buildDataMonitoredBranch(Network network, Cnec cnec, GlskProvider glskProvider, SystematicSensitivityResult result) {
        return new DataMonitoredBranch(
                cnec.getId(),
                cnec.getName(),
                cnec.getNetworkElement().getId(),
                cnec.getMaxThreshold(Unit.MEGAWATT).get(),
                zeroIfNaN(result.getReferenceFlow(cnec)),
                buildDataPtdfPerCountry(network, cnec, glskProvider, result)
        );
    }

    private List<DataPtdfPerCountry> buildDataPtdfPerCountry(Network network, Cnec cnec, GlskProvider glskProvider, SystematicSensitivityResult result) {
        Map<String, LinearGlsk> glsks = glskProvider.getAllGlsk(network);
        return glsks.values().stream()
                .map(glsk ->
                        new DataPtdfPerCountry(
                                glsk.getId(),
                                zeroIfNaN(result.getSensitivityOnFlow(glsk.getId(), cnec))
                        )
                ).collect(Collectors.toList());
    }

    private double zeroIfNaN(double value) {
        return Double.isNaN(value) ? 0. : value;
    }
}
