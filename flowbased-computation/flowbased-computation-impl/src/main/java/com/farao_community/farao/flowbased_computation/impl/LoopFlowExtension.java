/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.impl;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.MonitoredBranch;
import com.farao_community.farao.data.flowbased_domain.DataMonitoredBranch;
import com.farao_community.farao.data.flowbased_domain.DataPtdfPerCountry;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationParameters;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationProvider;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationResult;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * "Adjustment of Minimum RAM" (AMR a.k.a "LoopFlow") calculation
 * Detailed requirement is in Article 17 of "Decision on Core CCM - Annex I"
 * We use flowbased computation API to get different flows required in doc
 *
 * Input:
 * Network: CGM
 * CracFile: list CNE, fmax, etc.
 * Glsk CORE: Glsk of CCR CORE, to calculate F(0,core)
 * Glsk All: Glsk described in Article 17.3 to calculate F(0,all)
 * frmById: FRM (Flow Reliability Margin) for each CNE
 * ramrById: Ramr (minimum RAM factor) by default 0.7 (see Article 17.8)
 *
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class LoopFlowExtension {

    //input parameters
    private Network network; //CGM
    private CracFile cracFile; //list CNE, fmax
    private GlskProvider glskProviderCore; // to calculate F(0,core) see Article 17.2
    private GlskProvider glskProviderAll; // to calculate F(0,all) see Article 17.3
    private Map<String, Double> frmById; //input: FRM (Flow Reliability Margin) for each branch
    private Map<String, Double> ramrById; //input: Ramr (minimum RAM factor) by default 0.7 (see Article 17.8)

    private ComputationManager computationManager;
    private FlowBasedComputationParameters parameters;


    /**
     * @return main function to calculate AMR
     */
    public Map<String, Double> calculateAMR() {

        Map<String, Double> fZeroCore = calculateZeroNetPositionFlows(glskProviderCore); //get F(0,core)
        Map<String, Double> fZeroAll = calculateZeroNetPositionFlows(glskProviderAll); //get F(0,all)
        Map<String, Double> fmaxById = fmaxCracById(cracFile); //get Fmax

        // Equation 15 in Article 17:
        // AMR : Adjustment of minimum RAM, see Article 17.6
        // AMR = max ( 0, alpha, beta ), where:
        //   - alpha = Ramr*Fmax - Fuaf - (Fmax - FRM - F(0,core)) = (Ramr - 1)*Fmax + FRM + F(0,all)
        //   - beta  = - 0.8*Fmax + FRM + F(0,core)
        Map<String, Double> amr = new HashMap<>();
        for (String branchId : fZeroCore.keySet()) {
            double alpha = (ramrById.get(branchId) - 1) * fmaxById.get(branchId)
                    + frmById.get(branchId)
                    + fZeroAll.get(branchId);
            double beta = -0.8 * fmaxById.get(branchId)
                    + frmById.get(branchId)
                    + fZeroCore.get(branchId);
            // AMR = max ( 0, alpha, beta )
            amr.put(branchId, Math.max(0, Math.max(alpha, beta)));
        }
        return amr;
    }

    /**
     * @param glskProvider GLSK: can be GLSK(core) or GLSK(all)
     * @return Flows on CNE under condition that Net Positions are zero
     */
    private Map<String, Double> calculateZeroNetPositionFlows(GlskProvider glskProvider) {
        //Call flowbased computation on given glskProvider
        FlowBasedComputationProvider flowBasedComputationProvider = new FlowBasedComputationImpl();
        FlowBasedComputationResult flowBasedComputationResult = flowBasedComputationProvider.run(network,
                cracFile, glskProvider, computationManager, network.getVariantManager().getWorkingVariantId(), parameters)
                .join();

        Map<String, Double> frefResults = frefResultById(flowBasedComputationResult); //get reference flow
        Map<String, Map<String, Double>> ptdfResults = ptdfResultById(flowBasedComputationResult); // get ptdf
        Map<String, Double> referenceNetPositionByCountry = getRefNetPositionByCountry(network); // get Net positions

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
    private Map<String, Double> getRefNetPositionByCountry(Network network) {
        //todo get Net Position of each country from Network
        Map<String, Double> refNpCountry = new HashMap<>();
        return refNpCountry;
    }

    /**
     * @param flowBasedComputationResult flowbased computation result
     * @return Reference flow of each CNE
     */
    private Map<String, Double> frefResultById(FlowBasedComputationResult flowBasedComputationResult) {
        return flowBasedComputationResult.getFlowBasedDomain().getDataPreContingency().getDataMonitoredBranches().stream()
                .collect(Collectors.toMap(
                        DataMonitoredBranch::getId,
                        DataMonitoredBranch::getFref));
    }

    /**
     * @param cracFile crac file
     * @return Fmax of each CNE
     */
    private Map<String, Double> fmaxCracById(CracFile cracFile) {
        return cracFile.getPreContingency().getMonitoredBranches().stream()
                .collect(Collectors.toMap(MonitoredBranch::getBranchId, MonitoredBranch::getFmax));
    }

    /**
     * @param flowBasedComputationResult flowbased computation result
     * @return PTDF values
     */
    private Map<String, Map<String, Double>> ptdfResultById(FlowBasedComputationResult flowBasedComputationResult) {
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
