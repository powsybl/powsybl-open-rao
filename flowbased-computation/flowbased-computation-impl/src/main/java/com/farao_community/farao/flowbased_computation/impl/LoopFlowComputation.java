/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.impl;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.farao_community.farao.util.LoadFlowService;
import com.farao_community.farao.util.SensitivityComputationService;
import com.powsybl.balances_adjustment.util.CountryAreaFactory;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowResult;
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

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class LoopFlowComputation {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoopFlowComputation.class);

    private Crac crac;
    private GlskProvider glskProvider;
    private List<String> countries;

    public LoopFlowComputation(Crac crac, CracLoopFlowExtension cracLoopFlowExtension) {
        this.crac = crac;
        this.glskProvider = cracLoopFlowExtension.getGlskProvider();
        this.countries = cracLoopFlowExtension.getCountriesForLoopFlow();
    }

    public LoopFlowComputation(Crac crac, GlskProvider glskProvider, List<String> countries) {
        this.crac = crac;
        this.glskProvider = glskProvider;
        this.countries = countries;
    }

    public LoopFlowComputationResult calculateLoopFlows(Network network) {
        Map<Cnec, Double> frefResults = computeRefFlowOnCurrentNetwork(network); //get reference flow
        Map<Cnec, Map<String, Double>> ptdfResults = computePtdfOnCurrentNetwork(network); // get ptdf
        Map<String, Double> referenceNetPositionByCountry = getRefNetPositionByCountry(network); // get Net positions
        Map<Cnec, Double> loopFlowShifts = buildLoopFlowShift(ptdfResults, referenceNetPositionByCountry); //compute PTDF * NetPosition
        Map<String, Double> loopflows = buildLoopFlowsFromResult(frefResults, loopFlowShifts); //compute loopflow
        return new LoopFlowComputationResult(ptdfResults, referenceNetPositionByCountry, loopFlowShifts, loopflows);
    }

    public Map<Cnec, Map<String, Double>> computePtdfOnCurrentNetwork(Network network) {
        Map<Cnec, Map<String, Double>> ptdfs = new HashMap<>();
        Set<Cnec> preventivecnecs = crac.getCnecs(crac.getPreventiveState());
        SensitivityFactorsProvider factorsProvider = net -> generateSensitivityFactorsProvider(net, preventivecnecs, glskProvider);
        SensitivityComputationResults sensiResults = SensitivityComputationService.runSensitivity(network, network.getVariantManager().getWorkingVariantId(), factorsProvider);
        sensiResults.getSensitivityValues().forEach(sensitivityValue -> addSensitivityValue(sensitivityValue, crac, ptdfs));
        return ptdfs;
    }

    private List<SensitivityFactor> generateSensitivityFactorsProvider(Network network, Set<Cnec> cnecs, GlskProvider glskProvider) {
        List<SensitivityFactor> factors = new ArrayList<>();
        Map<String, LinearGlsk> mapCountryLinearGlsk = glskProvider.getAllGlsk(network);
        cnecs.forEach(cnec -> mapCountryLinearGlsk.values().stream()
                .map(linearGlsk -> new BranchFlowPerLinearGlsk(new BranchFlow(cnec.getId(), cnec.getName(), cnec.getNetworkElement().getId()), linearGlsk))
                .forEach(factors::add));
        return factors;
    }

    private void addSensitivityValue(SensitivityValue sensitivityValue, Crac crac, Map<Cnec, Map<String, Double>> ptdfs) {
        String cnecId = sensitivityValue.getFactor().getFunction().getId();
        Cnec cnec = crac.getCnec(cnecId);
        String glskId = sensitivityValue.getFactor().getVariable().getId();
        double ptdfValue = sensitivityValue.getValue();
        if (!ptdfs.containsKey(cnec)) {
            ptdfs.put(cnec, new HashMap<>());
        }
        ptdfs.get(cnec).put(glskId, ptdfValue);
    }

    public Map<Cnec, Double> computeRefFlowOnCurrentNetwork(Network network) {
        // we need this separate load flow to get reference flow on cnec.
        // because reference flow from sensi is not yet fully implemented in powsybl
        Map<Cnec, Double> cnecFlowMap = new HashMap<>();
        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        // 1. pre
        LoadFlowResult loadFlowResult = LoadFlowService.runLoadFlow(network, initialVariantId);
        if (loadFlowResult.isOk()) {
            buildFlowFromNetwork(network, crac, cnecFlowMap);
        }
        return cnecFlowMap;
    }

    private static void buildFlowFromNetwork(Network network, Crac crac, Map<Cnec, Double> cnecFlowMap) {
        Set<State> states = new HashSet<>();
        states.add(crac.getPreventiveState());
        states.forEach(state -> crac.getCnecs(state).forEach(cnec -> cnecFlowMap.put(cnec, cnec.getP(network))));
    }

    public Map<String, Double> getRefNetPositionByCountry(Network network) {
        //get Net Position of each country from Network
        Map<String, Double> refNpCountry = new HashMap<>();
        for (String country : countries) {
            CountryAreaFactory countryAreaFactory = new CountryAreaFactory(Country.valueOf(country));
            double countryNetPositionValue = countryAreaFactory.create(network).getNetPosition();
            refNpCountry.put(country, countryNetPositionValue);
        }
        return refNpCountry;
    }

    public Map<Cnec, Double> buildLoopFlowShift(Map<Cnec, Map<String, Double>> ptdfResults, Map<String, Double> referenceNetPositionByCountry) {
        Map<Cnec, Double> loopFlowShift = new HashMap<>();
        for (Map.Entry<Cnec, Map<String, Double>> entry : ptdfResults.entrySet()) {
            Cnec cnec = entry.getKey();
            Map<String, Double> cnecptdf = entry.getValue();
            double sum = 0.0;
            // calculate PTDF * NP(ref)
            for (Map.Entry<String, Double> e : cnecptdf.entrySet()) {
                String country = e.getKey();
                sum += cnecptdf.get(country) * referenceNetPositionByCountry.get(country);
            }
            loopFlowShift.put(cnec, sum);
        }
        return loopFlowShift;
    }

    public Map<String, Double> buildLoopFlowsFromResult(Map<Cnec, Double> frefResults, Map<Cnec, Double> loopFlowShifts) {
        Map<String, Double> loopFlows = new HashMap<>();
        for (Map.Entry<Cnec, Double> entry : frefResults.entrySet()) {
            Cnec cnec = entry.getKey();
            loopFlows.put(cnec.getId(), entry.getValue() - loopFlowShifts.get(cnec));
        }
        return loopFlows;
    }
}


