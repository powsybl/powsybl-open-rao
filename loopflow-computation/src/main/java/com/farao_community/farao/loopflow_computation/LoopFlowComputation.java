/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.data.glsk.import_.glsk_provider.GlskProvider;
import com.farao_community.farao.sensitivity_computation.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_computation.SystematicSensitivityResult;
import com.farao_community.farao.util.EICode;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowComputation {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoopFlowComputation.class);

    private Crac crac;
    private GlskProvider glskProvider;
    private ReferenceProgram referenceProgram;

    /**
     * @param crac             loop-flows will be computed for all the Cnecs of the Crac
     */
    public LoopFlowComputation(Crac crac, GlskProvider glskProvider, ReferenceProgram referenceProgram) {
        this.crac = requireNonNull(crac, "crac should not be null");
        this.glskProvider = requireNonNull(glskProvider, "glskProvider should not be null");
        this.referenceProgram = requireNonNull(referenceProgram, "referenceProgram should not be null");
    }

    public LoopFlowResult calculateLoopFlows(Network network, SensitivityComputationParameters sensitivityComputationParameters) {

        Set<Cnec> cnecs = crac.getCnecs(crac.getPreventiveState());
        // todo : add this Set of Cnecs in argument of the method calculateLoopFlows()
        //  required : loop-flows can be computed for N-1 states

        SystematicSensitivityInterface systematicSensitivityInterface = SystematicSensitivityInterface.builder()
            .withDefaultParameters(sensitivityComputationParameters)
            .withPtdfSensitivities(glskProvider, cnecs)
            .build();

        SystematicSensitivityResult ptdfsAndRefFlows = systematicSensitivityInterface.run(network, Unit.MEGAWATT);

        return buildLoopFlowsFromReferenceFlowAndPtdf(ptdfsAndRefFlows, network);
    }

    public LoopFlowResult buildLoopFlowsFromReferenceFlowAndPtdf(SystematicSensitivityResult alreadyCalculatedPtdfAndFlows, Network network) {
        return buildLoopFlowsFromReferenceFlowAndPtdf(alreadyCalculatedPtdfAndFlows, alreadyCalculatedPtdfAndFlows, network);

    }

    public LoopFlowResult buildLoopFlowsFromReferenceFlowAndPtdf(SystematicSensitivityResult alreadyCalculatedFlows, SystematicSensitivityResult alreadyCalculatedPtdfs, Network network) {

        Set<Cnec> cnecs = crac.getCnecs(crac.getPreventiveState());
        // todo : add this Set of Cnecs in argument of the method calculateLoopFlows()
        //  required : loop-flows can be computed for N-1 states

        List<LinearGlsk> glsks = getValidGlsks(network);
        LoopFlowResult results = new LoopFlowResult();

        for (Cnec cnec : cnecs) {
            double refFlow = alreadyCalculatedFlows.getReferenceFlow(cnec);
            double commercialFLow = glsks.stream()
                .mapToDouble(glsk -> alreadyCalculatedPtdfs.getSensitivityOnFlow(glsk, cnec) * referenceProgram.getGlobalNetPosition(glskToCountry(glsk)))
                .sum();
            results.addCnecResult(cnec, refFlow - commercialFLow, commercialFLow, refFlow);
        }
        return results;
    }

    private Country glskToCountry(LinearGlsk glsk) {
        if (glsk.getId().length() < EICode.LENGTH) {
            throw new IllegalArgumentException(String.format("Glsk [%s] should starts with an EI Code", glsk.getId()));
        }
        EICode eiCode = new EICode(glsk.getId().substring(0, EICode.LENGTH));
        return eiCode.getCountry();
    }

    private List<LinearGlsk> getValidGlsks(Network network) {
        return glskProvider.getAllGlsk(network).values().stream().filter(linearGlsk -> {
            if (!referenceProgram.getListOfCountries().contains(glskToCountry(linearGlsk))) {
                LOGGER.warn(String.format("Glsk [%s] is ignored as no corresponding country was found in the ReferenceProgram", linearGlsk.getId()));
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }
}


