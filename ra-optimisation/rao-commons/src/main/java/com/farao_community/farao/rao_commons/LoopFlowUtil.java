/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class LoopFlowUtil {

    private LoopFlowUtil() {
    }

    public static Map<BranchCnec, Double> computeCommercialFlows(Network network,
                                                                 Set<BranchCnec> cnecs,
                                                                 ZonalData<LinearGlsk> glskProvider,
                                                                 ReferenceProgram referenceProgram,
                                                                 SystematicSensitivityResult sensitivityAndFlowResult) {
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(glskProvider, referenceProgram);
        LoopFlowResult lfResults = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(network, sensitivityAndFlowResult, cnecs);
        Map<BranchCnec, Double> commercialFlows = new HashMap<>();
        for (BranchCnec cnec : cnecs) {
            commercialFlows.put(cnec, lfResults.getCommercialFlow(cnec));
        }
        return  commercialFlows;
    }

    public static Set<BranchCnec> computeLoopflowCnecs(Set<BranchCnec> allCnecs, Network network, RaoParameters raoParameters) {
        if (!raoParameters.getLoopflowCountries().isEmpty()) {
            return allCnecs.stream()
                    .filter(cnec -> !Objects.isNull(cnec.getExtension(CnecLoopFlowExtension.class)) && cnecIsInCountryList(cnec, network, raoParameters.getLoopflowCountries()))
                    .collect(Collectors.toSet());
        } else {
            return allCnecs.stream()
                    .filter(cnec -> !Objects.isNull(cnec.getExtension(CnecLoopFlowExtension.class)))
                    .collect(Collectors.toSet());
        }
    }

    private static boolean cnecIsInCountryList(Cnec<?> cnec, Network network, Set<Country> loopflowCountries) {
        return cnec.getLocation(network).stream().anyMatch(country -> country.isPresent() && loopflowCountries.contains(country.get()));
    }
}
