/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.powsybl.glsk.commons.ZonalData;
import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowComputationImpl implements LoopFlowComputation {
    protected ZonalData<SensitivityVariableSet> glsk;
    protected ReferenceProgram referenceProgram;
    protected Map<EICode, SensitivityVariableSet> glskMap;

    public LoopFlowComputationImpl(ZonalData<SensitivityVariableSet> glsk, ReferenceProgram referenceProgram) {
        this.glsk = requireNonNull(glsk, "glskProvider should not be null");
        this.referenceProgram = requireNonNull(referenceProgram, "referenceProgram should not be null");
        this.glskMap = buildRefProgGlskMap();
    }

    @Override
    public LoopFlowResult calculateLoopFlows(Network network, String sensitivityProvider, SensitivityAnalysisParameters sensitivityAnalysisParameters, Set<FlowCnec> flowCnecs) {
        SystematicSensitivityInterface systematicSensitivityInterface = SystematicSensitivityInterface.builder()
                .withSensitivityProviderName(sensitivityProvider)
                .withParameters(sensitivityAnalysisParameters)
                .withPtdfSensitivities(glsk, flowCnecs, Collections.singleton(Unit.MEGAWATT))
                .build();

        SystematicSensitivityResult ptdfsAndRefFlows = systematicSensitivityInterface.run(network);

        return buildLoopFlowsFromReferenceFlowAndPtdf(ptdfsAndRefFlows, flowCnecs, network);
    }

    @Override
    public LoopFlowResult buildLoopFlowsFromReferenceFlowAndPtdf(SystematicSensitivityResult alreadyCalculatedPtdfAndFlows, Set<FlowCnec> flowCnecs, Network network) {
        LoopFlowResult results = new LoopFlowResult();
        Map<SensitivityVariableSet, Boolean> isInMainComponentMap = computeIsInMainComponentMap(network);
        for (FlowCnec flowCnec : flowCnecs) {
            flowCnec.getMonitoredSides().forEach(side -> {
                double refFlow = alreadyCalculatedPtdfAndFlows.getReferenceFlow(flowCnec, side);
                double commercialFLow = getGlskStream(flowCnec).filter(entry -> isInMainComponentMap.get(entry.getValue()))
                    .mapToDouble(entry -> alreadyCalculatedPtdfAndFlows.getSensitivityOnFlow(entry.getValue(), flowCnec, side) * referenceProgram.getGlobalNetPosition(entry.getKey()))
                    .sum();
                results.addCnecResult(flowCnec, side, refFlow - commercialFLow, commercialFLow, refFlow);
            });
        }
        return results;
    }

    private Map<SensitivityVariableSet, Boolean> computeIsInMainComponentMap(Network network) {
        Map<SensitivityVariableSet, Boolean> map = new HashMap<>();
        glskMap.values().forEach(linearGlsk -> map.putIfAbsent(linearGlsk, isInMainComponent(linearGlsk, network)));
        return map;
    }

    static boolean isInMainComponent(SensitivityVariableSet linearGlsk, Network network) {
        boolean atLeastOneGlskConnected = false;
        for (String glsk : linearGlsk.getVariablesById().keySet()) {
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

    protected Stream<Map.Entry<EICode, SensitivityVariableSet>> getGlskStream(FlowCnec flowCnec) {
        return glskMap.entrySet().stream();
    }

    protected Map<EICode, SensitivityVariableSet> buildRefProgGlskMap() {

        Map<EICode, SensitivityVariableSet> refProgGlskMap = new HashMap<>();

        for (EICode area : referenceProgram.getListOfAreas()) {
            SensitivityVariableSet glskForArea = glsk.getData(area.getAreaCode());
            if (glskForArea == null) {
                FaraoLoggerProvider.BUSINESS_WARNS.warn("No GLSK found for reference area {}", area.getAreaCode());
            } else {
                refProgGlskMap.put(area, glskForArea);
            }
        }
        return refProgGlskMap;
    }
}
