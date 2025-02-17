/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.openrao.commons.EICode;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.raoapi.ZoneToZonePtdfDefinition;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.*;

/**
 *  This class computes the absolute PTDF sums on a given set of CNECs
 *  It requires that the sensitivity values be already computed
 *
 *  @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 *  @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class AbsolutePtdfSumsComputation {
    private final ZonalData<SensitivityVariableSet> glskProvider;
    private final List<ZoneToZonePtdfDefinition> zTozPtdfs;
    private final double ptdfSumLowerBound;

    public AbsolutePtdfSumsComputation(ZonalData<SensitivityVariableSet> glskProvider, List<ZoneToZonePtdfDefinition> zTozPtdfs, double ptdfSumLowerBound) {
        this.glskProvider = glskProvider;
        this.zTozPtdfs = zTozPtdfs;
        this.ptdfSumLowerBound = ptdfSumLowerBound;
    }

    public Map<FlowCnec, Map<TwoSides, Double>> computeAbsolutePtdfSums(Set<FlowCnec> flowCnecs, SystematicSensitivityResult sensitivityResult) {
        Map<FlowCnec, Map<TwoSides, Double>> ptdfSums = new HashMap<>();
        List<EICode> eiCodesInPtdfs = zTozPtdfs.stream().flatMap(zToz -> zToz.getEiCodes().stream()).toList();

        for (FlowCnec flowCnec : flowCnecs) {
            flowCnec.getMonitoredSides().forEach(side -> {
                Map<EICode, Double> ptdfMap = buildZoneToSlackPtdfMap(flowCnec, side, glskProvider, eiCodesInPtdfs, sensitivityResult);
                double sumOfZToZPtdf = zTozPtdfs.stream().mapToDouble(zToz -> Math.abs(computeZToZPtdf(zToz, ptdfMap))).sum();
                sumOfZToZPtdf = Math.max(sumOfZToZPtdf, ptdfSumLowerBound);
                ptdfSums.computeIfAbsent(flowCnec, k -> new EnumMap<>(TwoSides.class)).put(side, sumOfZToZPtdf);
            });
        }
        return ptdfSums;
    }

    private Map<EICode, Double> buildZoneToSlackPtdfMap(FlowCnec flowCnec, TwoSides side, ZonalData<SensitivityVariableSet> glsks, List<EICode> eiCodesInBoundaries, SystematicSensitivityResult sensitivityResult) {
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
