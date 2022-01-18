/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.commons.*;
import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowComputationImpl implements LoopFlowComputation {
    protected ZonalData<LinearGlsk> glsk;
    protected ReferenceProgram referenceProgram;
    protected Network network;
    protected Map<EICode, LinearGlsk> glskMap;

    public LoopFlowComputationImpl(ZonalData<LinearGlsk> glsk, ReferenceProgram referenceProgram, Network network) {
        this.glsk = requireNonNull(glsk, "glskProvider should not be null");
        this.referenceProgram = requireNonNull(referenceProgram, "referenceProgram should not be null");
        this.network = network;
        this.glskMap = buildRefProgGlskMap();
    }

    @Override
    public LoopFlowResult calculateLoopFlows(Network network, SensitivityAnalysisParameters sensitivityAnalysisParameters, Set<FlowCnec> flowCnecs) {
        this.network = network;

        SystematicSensitivityInterface systematicSensitivityInterface = SystematicSensitivityInterface.builder()
            .withDefaultParameters(sensitivityAnalysisParameters)
            .withPtdfSensitivities(glsk, flowCnecs, Collections.singleton(Unit.MEGAWATT))
            .build();

        SystematicSensitivityResult ptdfsAndRefFlows = systematicSensitivityInterface.run(network);

        return buildLoopFlowsFromReferenceFlowAndPtdf(ptdfsAndRefFlows, flowCnecs);
    }

    @Override
    public LoopFlowResult buildLoopFlowsFromReferenceFlowAndPtdf(SystematicSensitivityResult alreadyCalculatedPtdfAndFlows, Set<FlowCnec> flowCnecs) {
        LoopFlowResult results = new LoopFlowResult();
        Map<LinearGlsk, Boolean> isInMainComponentMap = computeIsInMainComponentMap();
        for (FlowCnec flowCnec : flowCnecs) {
            double refFlow = alreadyCalculatedPtdfAndFlows.getReferenceFlow(flowCnec);
            double commercialFLow = getGlskStream(flowCnec).filter(entry -> isInMainComponentMap.get(entry.getValue()))
                .mapToDouble(entry -> alreadyCalculatedPtdfAndFlows.getSensitivityOnFlow(entry.getValue(), flowCnec) * referenceProgram.getGlobalNetPosition(entry.getKey()))
                .sum();
            results.addCnecResult(flowCnec, refFlow - commercialFLow, commercialFLow, refFlow);
        }
        return results;
    }

    private Map<LinearGlsk, Boolean> computeIsInMainComponentMap() {
        Map<LinearGlsk, Boolean> map = new HashMap<>();
        glskMap.values().forEach(linearGlsk -> map.putIfAbsent(linearGlsk, isInMainComponent(linearGlsk, network)));
        return map;
    }

    static boolean isInMainComponent(LinearGlsk linearGlsk, Network network) {
        boolean atLeastOneGlskConnected = false;
        for (String glsk : linearGlsk.getGLSKs().keySet()) {
            Generator generator = network.getGenerator(glsk);
            if (generator != null) {
                // If bus is disconnected, then powsybl returns a null bus
                if (generator.getTerminal().getBusView().getBus() != null && generator.getTerminal().getBusView().getBus().isInMainConnectedComponent()) {
                    atLeastOneGlskConnected = true;
                }
            } else {
                Load load = network.getLoad(glsk);
                if (load == null) {
                    throw new FaraoException(String.format("%s is neither a generator nor a load in the network. It is not a valid GLSK.", glsk));
                } else if (load.getTerminal().getBusView().getBus() != null && load.getTerminal().getBusView().getBus().isInMainConnectedComponent()) {
                    atLeastOneGlskConnected = true;
                }
            }
        }
        return atLeastOneGlskConnected;
    }

    protected Stream<Map.Entry<EICode, LinearGlsk>> getGlskStream(FlowCnec flowCnec) {
        return glskMap.entrySet().stream();
    }

    protected Map<EICode, LinearGlsk> buildRefProgGlskMap() {

        Map<EICode, LinearGlsk> refProgGlskMap = new HashMap<>();

        for (EICode area : referenceProgram.getListOfAreas()) {
            LinearGlsk glskForArea = glsk.getData(area.getAreaCode());
            if (glskForArea == null) {
                FaraoLoggerProvider.BUSINESS_WARNS.warn("No GLSK found for reference area {}", area.getAreaCode());
            } else {
                refProgGlskMap.put(area, glskForArea);
            }
        }
        return refProgGlskMap;
    }
}
