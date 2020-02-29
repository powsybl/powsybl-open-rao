/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.impl;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.MonitoredBranch;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationParameters;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;

import java.util.*;
import java.util.stream.Collectors;

/**
 * "Adjustment of Minimum RAM" calculation
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
 * countries: list of countries for CC calculation; not all countries in Network are included in CCR
 *
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class MinRamAdjustment {

    //input parameters
    private Network network; //CGM
    private CracFile cracFile; //list CNE, fmax
    private GlskProvider glskProviderCore; // to calculate F(0,core) see Article 17.2
    private GlskProvider glskProviderAll; // to calculate F(0,all) see Article 17.3
    private Map<String, Double> frmById; //input: FRM (Flow Reliability Margin) for each branch
    private Map<String, Double> ramrById; //input: Ramr (minimum RAM factor) by default 0.7 (see Article 17.8)
    private List<String> countries; //list of countries for CC calculation; not all countries in Network are included in CCR

    private ComputationManager computationManager;
    private FlowBasedComputationParameters parameters;

    public MinRamAdjustment(Network network,
                            CracFile cracFile,
                            GlskProvider glskProviderCore,
                            GlskProvider glskProviderAll,
                            Map<String, Double> frmById,
                            Map<String, Double> ramrById,
                            List<String> countries,
                            ComputationManager computationManager,
                            FlowBasedComputationParameters parameters) {
        this.network = network;
        this.cracFile = cracFile;
        this.glskProviderCore = glskProviderCore;
        this.glskProviderAll = glskProviderAll;
        this.frmById = frmById;
        this.ramrById = ramrById;
        this.countries = countries;
        this.computationManager = computationManager;
        this.parameters = parameters;
    }

    /**
     * @return main function to calculate AMR
     */
    public Map<String, Double> calculateAMR() {

        Map<String, Double> fZeroCore = LoopFlowUtil.calculateLoopFlows(network, cracFile, glskProviderCore, countries, computationManager, parameters); //get F(0,core)
        Map<String, Double> fZeroAll = LoopFlowUtil.calculateLoopFlows(network, cracFile, glskProviderAll, countries, computationManager, parameters); //get F(0,all)
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
     * @param cracFile crac file
     * @return Fmax of each CNE
     */
    private Map<String, Double> fmaxCracById(CracFile cracFile) {
        return cracFile.getPreContingency().getMonitoredBranches().stream()
                .collect(Collectors.toMap(MonitoredBranch::getBranchId, MonitoredBranch::getFmax));
    }

}
