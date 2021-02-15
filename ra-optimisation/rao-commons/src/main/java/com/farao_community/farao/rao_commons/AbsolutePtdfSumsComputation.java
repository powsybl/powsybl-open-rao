/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *  This class computes the absolute PTDF sums on a given set of CNECs
 *  It requires that the sensitivity values be already computed
 *
 *  @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class AbsolutePtdfSumsComputation {
    private AbsolutePtdfSumsComputation() { }

    public static Map<BranchCnec, Double> computeAbsolutePtdfSums(Set<BranchCnec> cnecs, ZonalData<LinearGlsk> glsk, List<Pair<EICode, EICode>> boundaries, SystematicSensitivityResult sensitivityResult) {
        Map<BranchCnec, Double> ptdfSums = new HashMap<>();
        Map<String, Map<EICode, Double>> ptdfMap = buildPtdfMap(cnecs, glsk, getEICodesInBoundaries(boundaries), sensitivityResult);
        cnecs.forEach(cnec -> {
            double ptdfSum = 0;
            for (Pair<EICode, EICode> eiCodePair : boundaries) {
                if (ptdfMap.get(cnec.getId()).containsKey(eiCodePair.getLeft()) && ptdfMap.get(cnec.getId()).containsKey(eiCodePair.getRight())) {
                    ptdfSum += Math.abs(ptdfMap.get(cnec.getId()).get(eiCodePair.getLeft()) - ptdfMap.get(cnec.getId()).get(eiCodePair.getRight()));
                }
            }
            ptdfSums.put(cnec, ptdfSum);
        });
        return ptdfSums;
    }

    private static Map<String, Map<EICode, Double>> buildPtdfMap(Set<BranchCnec> cnecs, ZonalData<LinearGlsk> glsk, List<EICode> eiCodesInBoundaries, SystematicSensitivityResult sensitivityResult) {

        Map<String, Map<EICode, Double>> ptdfs = new HashMap<>();
        Map<String, LinearGlsk> mapCountryLinearGlsk = glsk.getDataPerZone();

        for (LinearGlsk linearGlsk: mapCountryLinearGlsk.values()) {
            if (isGlskInBoundaries(linearGlsk.getId(), eiCodesInBoundaries)) {
                EICode area = glskToEic(linearGlsk.getId());
                for (BranchCnec cnec : cnecs) {
                    double ptdfValue = sensitivityResult.getSensitivityOnFlow(linearGlsk, cnec);
                    if (!ptdfs.containsKey(cnec.getId())) {
                        ptdfs.put(cnec.getId(), new HashMap<>());
                    }
                    ptdfs.get(cnec.getId()).put(area, ptdfValue);
                }
            }
        }
        return ptdfs;
    }

    private static boolean isGlskInBoundaries(String glskId, List<EICode> countriesInBoundaries) {
        try {
            EICode glskEic = glskToEic(glskId);
            return countriesInBoundaries.contains(glskEic);
        } catch (IllegalArgumentException | FaraoException e) {
            return false;
        }
    }

    private static EICode glskToEic(String glskId) {
        if (glskId.length() < EICode.EIC_LENGTH) {
            throw new IllegalArgumentException(String.format("GlskId [%s] should starts with an EI Code", glskId));
        }
        return new EICode(glskId.substring(0, EICode.EIC_LENGTH));
    }

    private static List<EICode> getEICodesInBoundaries(List<Pair<EICode, EICode>> boundaries) {
        return boundaries.stream()
            .flatMap(eiCodePair -> Stream.of(eiCodePair.getLeft(), eiCodePair.getRight()))
            .distinct()
            .collect(Collectors.toList());
    }
}
