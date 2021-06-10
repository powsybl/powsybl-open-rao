/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;

import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;

import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowComputation {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoopFlowComputation.class);

    private ZonalData<LinearGlsk> glsk;
    private ReferenceProgram referenceProgram;

    public LoopFlowComputation(ZonalData<LinearGlsk> glsk, ReferenceProgram referenceProgram) {
        this.glsk = requireNonNull(glsk, "glskProvider should not be null");
        this.referenceProgram = requireNonNull(referenceProgram, "referenceProgram should not be null");
    }

    public LoopFlowResult calculateLoopFlows(Network network, SensitivityAnalysisParameters sensitivityAnalysisParameters, Set<BranchCnec> cnecs) {
        SystematicSensitivityInterface systematicSensitivityInterface = SystematicSensitivityInterface.builder()
            .withDefaultParameters(sensitivityAnalysisParameters)
            .withPtdfSensitivities(glsk, cnecs, Collections.singleton(Unit.MEGAWATT))
            .build();

        SystematicSensitivityResult ptdfsAndRefFlows = systematicSensitivityInterface.run(network);

        return buildLoopFlowsFromReferenceFlowAndPtdf(network, ptdfsAndRefFlows, cnecs);
    }

    public LoopFlowResult buildLoopFlowsFromReferenceFlowAndPtdf(Network network, SystematicSensitivityResult alreadyCalculatedPtdfAndFlows, Set<BranchCnec> cnecs) {

        LoopFlowResult results = new LoopFlowResult();
        Map<EICode, LinearGlsk> refProgGlskMap = buildRefProgGlskMap();
        XnodeGlskHandler xnodeGlskHandler = new XnodeGlskHandler(glsk, cnecs, network);

        for (BranchCnec cnec : cnecs) {
            double refFlow = alreadyCalculatedPtdfAndFlows.getReferenceFlow(cnec);
            double commercialFLow = refProgGlskMap.entrySet().stream().filter(entry -> isInMainComponent(entry.getValue(), network))
                .filter(entry -> xnodeGlskHandler.isLinearGlskValidForCnec(cnec, entry.getValue()))
                .mapToDouble(entry -> alreadyCalculatedPtdfAndFlows.getSensitivityOnFlow(entry.getValue(), cnec) * referenceProgram.getGlobalNetPosition(entry.getKey()))
                .sum();
            results.addCnecResult(cnec, refFlow - commercialFLow, commercialFLow, refFlow);
        }
        return results;
    }

    static boolean isInMainComponent(LinearGlsk linearGlsk, Network network) {
        for (String glsk : linearGlsk.getGLSKs().keySet()) {
            Generator generator = network.getGenerator(glsk);
            if (generator != null && generator.getTerminal().getBusView().getBus().isInMainConnectedComponent()) {
                return true;
            } else {
                Load load = network.getLoad(glsk);
                if (load != null && load.getTerminal().getBusView().getBus().isInMainConnectedComponent()) {
                    return true;
                }
            }
        }
        return false;
    }

    private Map<EICode, LinearGlsk> buildRefProgGlskMap() {

        Map<EICode, LinearGlsk> refProgGlskMap = new HashMap<>();

        for (EICode area : referenceProgram.getListOfAreas()) {
            LinearGlsk glskForArea = glsk.getData(area.getAreaCode());
            if (glskForArea == null) {
                LOGGER.warn("No GLSK found for reference area {}", area.getAreaCode());
            } else {
                refProgGlskMap.put(area, glskForArea);
            }
        }
        return refProgGlskMap;
    }
}
