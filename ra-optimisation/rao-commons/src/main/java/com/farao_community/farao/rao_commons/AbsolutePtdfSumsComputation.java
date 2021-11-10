/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.loopflow_computation.XnodeGlskHandler;
import com.farao_community.farao.rao_api.ZoneToZonePtdfDefinition;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.*;
import java.util.stream.Collectors;

/**
 *  This class computes the absolute PTDF sums on a given set of CNECs
 *  It requires that the sensitivity values be already computed
 *
 *  @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 *  @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class AbsolutePtdfSumsComputation {
    private final ZonalData<LinearGlsk> glskProvider;
    private final List<ZoneToZonePtdfDefinition> zTozPtdfs;
    private final Network network;

    public AbsolutePtdfSumsComputation(ZonalData<LinearGlsk> glskProvider, List<ZoneToZonePtdfDefinition> zTozPtdfs, Network network) {
        this.glskProvider = glskProvider;
        this.zTozPtdfs = zTozPtdfs;
        this.network = network;
    }

    public Map<FlowCnec, Double> computeAbsolutePtdfSums(Set<FlowCnec> flowCnecs, SystematicSensitivityResult sensitivityResult) {

        Set<Contingency> contingencies = flowCnecs.stream()
                .filter(cnec -> cnec.getState().getContingency().isPresent())
                .map(cnec -> cnec.getState().getContingency().get())
                .collect(Collectors.toSet());
        XnodeGlskHandler xnodeGlskHandler = new XnodeGlskHandler(glskProvider, contingencies, network);

        Map<FlowCnec, Double> ptdfSums = new HashMap<>();
        List<EICode> eiCodesInPtdfs = zTozPtdfs.stream().flatMap(zToz -> zToz.getEiCodes().stream()).collect(Collectors.toList());

        for (FlowCnec flowCnec : flowCnecs) {
            Map<EICode, Double> ptdfMap = buildZoneToSlackPtdfMap(flowCnec, glskProvider, eiCodesInPtdfs, sensitivityResult, xnodeGlskHandler);
            double sumOfZToZPtdf = zTozPtdfs.stream().mapToDouble(zToz -> Math.abs(computeZToZPtdf(zToz, ptdfMap))).sum();
            ptdfSums.put(flowCnec, sumOfZToZPtdf);
        }
        return ptdfSums;
    }

    private Map<EICode, Double> buildZoneToSlackPtdfMap(FlowCnec flowCnec, ZonalData<LinearGlsk> glsks, List<EICode> eiCodesInBoundaries, SystematicSensitivityResult sensitivityResult, XnodeGlskHandler xnodeGlskHandler) {
        Map<EICode, Double> ptdfs = new HashMap<>();
        for (EICode eiCode : eiCodesInBoundaries) {
            LinearGlsk linearGlsk = glsks.getData(eiCode.getAreaCode());
            if (linearGlsk != null) {
                double ptdfValue;
                if (xnodeGlskHandler.isLinearGlskValidForCnec(flowCnec, linearGlsk)) {
                    ptdfValue = sensitivityResult.getSensitivityOnFlow(linearGlsk, flowCnec);
                } else {
                    ptdfValue = 0;
                }
                ptdfs.put(eiCode, ptdfValue);
            }
        }
        return ptdfs;
    }

    private double computeZToZPtdf(ZoneToZonePtdfDefinition zToz, Map<EICode, Double> zToSlackPtdfMap) {
        if (zToz.getZoneToSlackPtdfs().stream().anyMatch(zToS -> !zToSlackPtdfMap.containsKey(zToS.getEiCode()))) {
            // If one zone is missing its PTDF, ignore the boundary
            return 0;
        }

        return zToz.getZoneToSlackPtdfs().stream()
            .mapToDouble(zToS -> zToS.getWeight() * zToSlackPtdfMap.get(zToS.getEiCode()))
            .sum();
    }
}
