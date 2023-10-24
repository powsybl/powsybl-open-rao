/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.rao_api.ZoneToZonePtdfDefinition;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class computes the absolute PTDF sums on a given set of CNECs
 * It requires that the sensitivity values be already computed
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class AbsolutePtdfSumsComputation {
    private final ZonalData<SensitivityVariableSet> glskProvider;
    private final List<ZoneToZonePtdfDefinition> zTozPtdfs;

    public AbsolutePtdfSumsComputation(ZonalData<SensitivityVariableSet> glskProvider, List<ZoneToZonePtdfDefinition> zTozPtdfs) {
        this.glskProvider = glskProvider;
        this.zTozPtdfs = zTozPtdfs;
    }

    public Map<FlowCnec, Map<Side, Double>> computeAbsolutePtdfSums(Set<FlowCnec> flowCnecs, SystematicSensitivityResult sensitivityResult) {
        Map<FlowCnec, Map<Side, Double>> ptdfSums = new HashMap<>();
        List<EICode> eiCodesInPtdfs = zTozPtdfs.stream().flatMap(zToz -> zToz.getEiCodes().stream()).collect(Collectors.toList());

        for (FlowCnec flowCnec : flowCnecs) {
            flowCnec.getMonitoredSides().forEach(side -> {
                Map<EICode, Double> ptdfMap = buildZoneToSlackPtdfMap(flowCnec, side, glskProvider, eiCodesInPtdfs, sensitivityResult);
                double sumOfZToZPtdf = zTozPtdfs.stream().mapToDouble(zToz -> Math.abs(computeZToZPtdf(zToz, ptdfMap))).sum();
                ptdfSums.computeIfAbsent(flowCnec, k -> new EnumMap<>(Side.class)).put(side, sumOfZToZPtdf);
            });
        }
        return ptdfSums;
    }

    private Map<EICode, Double> buildZoneToSlackPtdfMap(FlowCnec flowCnec, Side side, ZonalData<SensitivityVariableSet> glsks, List<EICode> eiCodesInBoundaries, SystematicSensitivityResult sensitivityResult) {
        Map<EICode, Double> ptdfs = new HashMap<>();
        for (EICode eiCode : eiCodesInBoundaries) {
            SensitivityVariableSet linearGlsk = glsks.getData(eiCode.getAreaCode());
            if (linearGlsk != null) {
                ptdfs.put(eiCode, sensitivityResult.getSensitivityOnFlow(linearGlsk, flowCnec, side));
            }
        }
        return ptdfs;
    }

    private double computeZToZPtdf(ZoneToZonePtdfDefinition zToz, Map<EICode, Double> zToSlackPtdfMap) {

        List<Double> zoneToSlackPtdf = zToz.getZoneToSlackPtdfs().stream()
            .filter(zToS -> zToSlackPtdfMap.containsKey(zToS.getEiCode()))
            .map(zToS -> zToS.getWeight() * zToSlackPtdfMap.get(zToS.getEiCode()))
            .toList();

        if (zoneToSlackPtdf.size() < 2) {
            // the boundary should at least contains two zoneToSlack PTDFs
            return 0.0;
        } else {
            return zoneToSlackPtdf.stream().mapToDouble(v -> v).sum();
        }
    }
}
