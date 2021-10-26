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
import com.farao_community.farao.rao_api.ZoneToZonePtdfDefinition;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
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

    public AbsolutePtdfSumsComputation(ZonalData<LinearGlsk> glskProvider, List<ZoneToZonePtdfDefinition> zTozPtdfs) {
        this.glskProvider = glskProvider;
        this.zTozPtdfs = zTozPtdfs;
    }

    public Map<FlowCnec, Double> computeAbsolutePtdfSums(Set<FlowCnec> flowCnecs, SystematicSensitivityResult sensitivityResult) {
        Map<FlowCnec, Double> ptdfSums = new HashMap<>();
        List<EICode> eiCodesInPtdfs = zTozPtdfs.stream().flatMap(zToz -> zToz.getEiCodes().stream()).collect(Collectors.toList());
        for (FlowCnec flowCnec : flowCnecs) {
            Map<EICode, Double> ptdfMap = buildZoneToSlackPtdfMap(flowCnec, glskProvider, eiCodesInPtdfs, sensitivityResult);
            double sumOfZToZPtdf = zTozPtdfs.stream().mapToDouble(zToz -> Math.abs(computeZToZPtdf(zToz, ptdfMap, isAlegroDisconnected(flowCnec)))).sum();
            ptdfSums.put(flowCnec, sumOfZToZPtdf);
        }
        return ptdfSums;
    }

    private boolean isAlegroDisconnected(FlowCnec flowCnec) {
        Optional<Contingency> contingency = flowCnec.getState().getContingency();
        if (contingency.isEmpty()) {
            return false;
        } else {
            return contingency.get().getNetworkElements().stream().anyMatch(ne -> ne.getId().contains("XLI_OB1A"));
        }
    }

    private Map<EICode, Double> buildZoneToSlackPtdfMap(FlowCnec flowCnec, ZonalData<LinearGlsk> glsks, List<EICode> eiCodesInBoundaries, SystematicSensitivityResult sensitivityResult) {
        Map<EICode, Double> ptdfs = new HashMap<>();
        for (EICode eiCode : eiCodesInBoundaries) {
            LinearGlsk linearGlsk = glsks.getData(eiCode.getAreaCode());
            if (linearGlsk != null) {
                double ptdfValue = sensitivityResult.getSensitivityOnFlow(linearGlsk, flowCnec);
                ptdfs.put(eiCode, ptdfValue);
            }
        }
        return ptdfs;
    }

    private double computeZToZPtdf(ZoneToZonePtdfDefinition zToz, Map<EICode, Double> zToSlackPtdfMap, boolean alegroDisconnected) {
        if (zToz.getZoneToSlackPtdfs().stream().anyMatch(zToS -> !zToSlackPtdfMap.containsKey(zToS.getEiCode()))) {
            // If one zone is missing its PTDF, ignore the boundary
            return 0;
        }
        if (zToz.getZoneToSlackPtdfs().stream().anyMatch(zToS -> zToS.getEiCode().getAreaCode().equals("22Y201903144---9"))) {
            // Alegro temporary patch : remove alegro z2s if Alegro is disconnected
            if (alegroDisconnected) {
                return zToz.getZoneToSlackPtdfs().stream()
                    .filter(zToS -> !zToS.getEiCode().getAreaCode().equals("22Y201903144---9") && !zToS.getEiCode().getAreaCode().equals("22Y201903145---4"))
                    .mapToDouble(zToS -> zToS.getWeight() * zToSlackPtdfMap.get(zToS.getEiCode()))
                    .sum();
            }
        }
        return zToz.getZoneToSlackPtdfs().stream()
            .mapToDouble(zToS -> zToS.getWeight() * zToSlackPtdfMap.get(zToS.getEiCode()))
            .sum();
    }
}
