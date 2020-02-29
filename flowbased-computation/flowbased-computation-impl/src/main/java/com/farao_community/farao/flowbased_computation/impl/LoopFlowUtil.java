/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.impl;

import com.farao_community.farao.balances_adjustment.util.CountryArea;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.flowbased_domain.DataMonitoredBranch;
import com.farao_community.farao.data.flowbased_domain.DataPtdfPerCountry;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationParameters;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationProvider;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationResult;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public final class LoopFlowUtil {
    private LoopFlowUtil() {
    }

    public static Map<String, Double> calculateLoopFlows(Network network,
                                                         CracFile cracFile,
                                                         GlskProvider glskProvider,
                                                         List<String> countries,
                                                         ComputationManager computationManager,
                                                         FlowBasedComputationParameters flowBasedComputationParameters
    ) {
        //Call flowbased computation on given glskProvider
        FlowBasedComputationProvider flowBasedComputationProvider = new FlowBasedComputationImpl();
        FlowBasedComputationResult flowBasedComputationResult = flowBasedComputationProvider.run(network,
                cracFile, glskProvider, computationManager, network.getVariantManager().getWorkingVariantId(), flowBasedComputationParameters)
                .join();

        Map<String, Double> frefResults = frefResultById(flowBasedComputationResult); //get reference flow
        Map<String, Map<String, Double>> ptdfResults = ptdfResultById(flowBasedComputationResult); // get ptdf
        Map<String, Double> referenceNetPositionByCountry = getRefNetPositionByCountry(network, countries); // get Net positions
        return buildLoopFlowsFromResult(flowBasedComputationResult, frefResults, ptdfResults, referenceNetPositionByCountry);
    }

    public static Map<String, Double> calculateLoopFlows(Network network,
                                                         Crac crac,
                                                         GlskProvider glskProvider,
                                                         List<String> countries,
                                                         ComputationManager computationManager,
                                                         FlowBasedComputationParameters flowBasedComputationParameters
    ) {
        //Call flowbased computation on given glskProvider
        FlowBasedComputationImpl flowBasedComputationProvider = new FlowBasedComputationImpl();
        FlowBasedComputationResult flowBasedComputationResult = flowBasedComputationProvider.run(network,
                crac, glskProvider, computationManager, network.getVariantManager().getWorkingVariantId(), flowBasedComputationParameters)
                .join();

        Map<String, Double> frefResults = frefResultById(flowBasedComputationResult); //get reference flow
        Map<String, Map<String, Double>> ptdfResults = ptdfResultById(flowBasedComputationResult); // get ptdf
        Map<String, Double> referenceNetPositionByCountry = getRefNetPositionByCountry(network, countries); // get Net positions
        return buildLoopFlowsFromResult(flowBasedComputationResult, frefResults, ptdfResults, referenceNetPositionByCountry);
    }

    private static Map<String, Double> buildLoopFlowsFromResult(FlowBasedComputationResult flowBasedComputationResult,
                                                                Map<String, Double> frefResults,
                                                                Map<String, Map<String, Double>> ptdfResults,
                                                                Map<String, Double> referenceNetPositionByCountry
    ) {
        //calculate equation 10 and equation 11 in Article 17
        Map<String, Double> fzeroNpResults = new HashMap<>();
        for (DataMonitoredBranch branch : flowBasedComputationResult.getFlowBasedDomain().getDataPreContingency().getDataMonitoredBranches()) {
            Map<String, Double> ptdfBranch = ptdfResults.get(branch.getBranchId());
            Double sum = 0.0;
            // calculate PTDF * NP(ref)
            for (String country : ptdfBranch.keySet()) {
                sum += ptdfBranch.get(country) * referenceNetPositionByCountry.get(country);
            }

            //F(0) = F(ref) - PTDF * NP(ref)
            fzeroNpResults.put(branch.getBranchId(), frefResults.get(branch.getBranchId()) - sum);
        }
        return fzeroNpResults;
    }

    /**
     * @param network get net position of countries in network
     * @return net positions
     */
    private static Map<String, Double> getRefNetPositionByCountry(Network network, List<String> countries) {
        //get Net Position of each country from Network

        Map<String, Double> refNpCountry = new HashMap<>();

        for (String country : countries) {
            CountryArea countryArea = new CountryArea(Country.valueOf(country));
            double countryNetPositionValue = countryArea.getNetPosition(network);
            refNpCountry.put(country, countryNetPositionValue);
        }

        return refNpCountry;
    }

    /**
     * @param flowBasedComputationResult flowbased computation result
     * @return Reference flow of each CNE
     */
    private static Map<String, Double> frefResultById(FlowBasedComputationResult flowBasedComputationResult) {
        return flowBasedComputationResult.getFlowBasedDomain().getDataPreContingency().getDataMonitoredBranches().stream()
            .collect(Collectors.toMap(
                DataMonitoredBranch::getId,
                DataMonitoredBranch::getFref));
    }

    /**
     * @param flowBasedComputationResult flowbased computation result
     * @return PTDF values
     */
    private static Map<String, Map<String, Double>> ptdfResultById(FlowBasedComputationResult flowBasedComputationResult) {
        return flowBasedComputationResult.getFlowBasedDomain().getDataPreContingency().getDataMonitoredBranches().stream()
            .collect(Collectors.toMap(
                DataMonitoredBranch::getId,
                dataMonitoredBranch -> dataMonitoredBranch.getPtdfList().stream()
                    .collect(Collectors.toMap(
                        DataPtdfPerCountry::getCountry,
                        DataPtdfPerCountry::getPtdf
                    ))
            ));
    }

}


