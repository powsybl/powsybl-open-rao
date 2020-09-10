/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgramBuilder;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.farao_community.farao.util.EICode;
import com.farao_community.farao.util.LoadFlowService;
import com.farao_community.farao.util.SensitivityComputationService;
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

import static java.util.Objects.requireNonNull;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class LoopFlowComputation {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoopFlowComputation.class);

    private Crac crac;
    private GlskProvider glskProvider;
    private List<Country> countries;
    private ReferenceProgram referenceProgram;

    /**
     * @param crac             CracLoopFlowExtension is added to crac
     * @param glskProvider     use list of countires in GlskProvider
     * @param network          necessary to get list of countries from GlskProvider
     * @param referenceProgram reference program containing net positions of countries
     */
    public LoopFlowComputation(Crac crac, GlskProvider glskProvider, Network network, ReferenceProgram referenceProgram) {
        requireNonNull(glskProvider, "glskProvider should not be null");
        requireNonNull(referenceProgram, "referenceProgram should not be null");
        this.glskProvider = glskProvider;
        Set<Country> glskCountries = new HashSet<>();
        glskProvider.getAllGlsk(network).keySet().forEach(key -> {
            try {
                glskCountries.add(new EICode(key).getCountry());
            } catch (IllegalArgumentException e) {
                glskCountries.add(Country.valueOf(key));
            }
        });
        glskCountries.retainAll(referenceProgram.getListOfCountries());
        this.countries = new ArrayList<>(glskCountries);
        this.crac = crac;
        this.referenceProgram = referenceProgram;
    }

    public List<Country> getCountries() {
        return this.countries;
    }

    public Map<String, Double> calculateLoopFlows(Network network) {
        Map<Cnec, Double> frefResults = computeRefFlowOnCurrentNetwork(network); //get reference flow
        Map<Cnec, Double> loopFlowShifts = buildZeroBalanceFlowShift(network); //compute PTDF * NetPosition
        return buildLoopFlowsFromReferenceFlowAndLoopflowShifts(frefResults, loopFlowShifts); //compute loopflow
    }

    public Map<String, Double> calculateLoopFlowsApproximation(Network network) {
        Map<Cnec, Double> frefResults = computeRefFlowOnCurrentNetwork(network); //get reference flow
        Map<Cnec, Double> loopFlowShifts = buildLoopflowShiftsApproximation(crac);
        return buildLoopFlowsFromReferenceFlowAndLoopflowShifts(frefResults, loopFlowShifts); //compute loopflow
    }

    public Map<Cnec, Map<Country, Double>> computePtdfOnCurrentNetwork(Network network) {
        Map<Cnec, Map<Country, Double>> ptdfs = new HashMap<>();
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

    private void addSensitivityValue(SensitivityValue sensitivityValue, Crac crac, Map<Cnec, Map<Country, Double>> ptdfs) {
        String cnecId = sensitivityValue.getFactor().getFunction().getId();
        Cnec cnec = crac.getCnec(cnecId);
        String glskId = sensitivityValue.getFactor().getVariable().getId();
        double ptdfValue = sensitivityValue.getValue();
        if (!ptdfs.containsKey(cnec)) {
            ptdfs.put(cnec, new HashMap<>());
        }
        ptdfs.get(cnec).put(glskIdToCountry(glskId), ptdfValue);
    }

    private Country glskIdToCountry(String glskId) {
        if (glskId.length() < EICode.LENGTH) {
            throw new IllegalArgumentException(String.format("GlskId [%s] should starts with an EI Code", glskId));
        }
        EICode eiCode = new EICode(glskId.substring(0, EICode.LENGTH));
        return eiCode.getCountry();
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

    public Map<Country, Double> getRefNetPositionByCountry(Network network) {
        if (referenceProgram == null) {
            referenceProgram = ReferenceProgramBuilder.buildReferenceProgram(network);
        }
        return referenceProgram.getAllGlobalNetPositions();
    }

    public Map<Cnec, Double> buildZeroBalanceFlowShift(Map<Cnec, Map<Country, Double>> ptdfResults) {
        Map<Cnec, Double> loopFlowShift = new HashMap<>();
        for (Map.Entry<Cnec, Map<Country, Double>> entry : ptdfResults.entrySet()) {
            Cnec cnec = entry.getKey();
            Map<Country, Double> cnecptdf = entry.getValue();
            double sum = 0.0;
            // calculate PTDF * NP(ref)
            for (Map.Entry<Country, Double> e : cnecptdf.entrySet()) {
                Country country = e.getKey();
                sum += cnecptdf.get(country) * referenceProgram.getGlobalNetPosition(country);
            }
            loopFlowShift.put(cnec, sum);
        }
        return loopFlowShift;
    }

    public Map<Cnec, Double> buildLoopflowShiftsApproximation(Crac crac) {
        Map<Cnec, Double> loopflowShifts = new HashMap<>();
        for (Cnec cnec : crac.getCnecs(crac.getPreventiveState())) {
            if (!Objects.isNull(cnec.getExtension(CnecLoopFlowExtension.class))
                    && cnec.getExtension(CnecLoopFlowExtension.class).hasLoopflowShift()) {
                loopflowShifts.put(cnec, cnec.getExtension(CnecLoopFlowExtension.class).getLoopflowShift());
            }
        }
        return loopflowShifts;
    }

    public Map<String, Double> buildLoopFlowsFromReferenceFlowAndLoopflowShifts(Map<Cnec, Double> frefResults, Map<Cnec, Double> loopFlowShifts) {
        Map<String, Double> loopFlows = new HashMap<>();
        for (Map.Entry<Cnec, Double> entry : loopFlowShifts.entrySet()) {
            Cnec cnec = entry.getKey();
            double fref = requireNonNull(frefResults.get(cnec), () -> "No reference flow value for " + cnec.getId());
            loopFlows.put(cnec.getId(), fref - entry.getValue());
        }
        return loopFlows;
    }

    public Map<Cnec, Double> buildZeroBalanceFlowShift(Network network) {
        if (referenceProgram == null) {
            referenceProgram = ReferenceProgramBuilder.buildReferenceProgram(network);
        }
        Map<Cnec, Map<Country, Double>> ptdfResults = computePtdfOnCurrentNetwork(network); // get ptdf
        return buildZeroBalanceFlowShift(ptdfResults); //compute PTDF * NetPosition
    }
}


