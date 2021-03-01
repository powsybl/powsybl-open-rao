/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
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
public final class AbsolutePtdfSumsComputation {
    private AbsolutePtdfSumsComputation() { }

    public static Map<BranchCnec, Double> computeAbsolutePtdfSums(Set<BranchCnec> cnecs, ZonalData<LinearGlsk> glsk, List<ZoneToZonePtdfDefinition> zTozPtdfs, SystematicSensitivityResult sensitivityResult) {

        Map<BranchCnec, Double> ptdfSums = new HashMap<>();
        List<EICode> eiCodesInPtdfs = zTozPtdfs.stream().flatMap(zToz -> zToz.getEiCodes().stream()).collect(Collectors.toList());
        for (BranchCnec cnec : cnecs) {
            Map<EICode, Double> ptdfMap = buildZoneToSlackPtdfMap(cnec, glsk, eiCodesInPtdfs, sensitivityResult);
            double sumOfZToZPtdf = zTozPtdfs.stream().mapToDouble(zToz -> Math.abs(computeZToZPtdf(zToz, ptdfMap))).sum();
            ptdfSums.put(cnec, sumOfZToZPtdf);
        }
        return ptdfSums;
    }

    private static Map<EICode, Double> buildZoneToSlackPtdfMap(BranchCnec cnec, ZonalData<LinearGlsk> glsks, List<EICode> eiCodesInBoundaries, SystematicSensitivityResult sensitivityResult) {

        Map<EICode, Double> ptdfs = new HashMap<>();
        for (EICode eiCode : eiCodesInBoundaries) {
            LinearGlsk linearGlsk = glsks.getData(eiCode.getAreaCode());
            if (linearGlsk != null) {
                double ptdfValue = sensitivityResult.getSensitivityOnFlow(linearGlsk, cnec);
                ptdfs.put(eiCode, ptdfValue);
            }
        }
        return ptdfs;
    }

    private static double computeZToZPtdf(ZoneToZonePtdfDefinition zToz, Map<EICode, Double> zToSlackPtdfMap) {
        if (zToz.getZoneToSlackPtdfs().stream().anyMatch(zToS -> !zToSlackPtdfMap.containsKey(zToS.getEiCode()))) {
            return 0;
        }
        return zToz.getZoneToSlackPtdfs().stream()
            .mapToDouble(zToS -> zToS.getWeight() * zToSlackPtdfMap.get(zToS.getEiCode()))
            .sum();
    }
}
