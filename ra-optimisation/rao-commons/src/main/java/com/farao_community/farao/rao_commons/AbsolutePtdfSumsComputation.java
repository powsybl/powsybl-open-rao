/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.farao_community.farao.util.EICode;
import com.powsybl.iidm.network.Country;
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

    public static Map<BranchCnec, Double> computeAbsolutePtdfSums(Set<BranchCnec> cnecs, ZonalData<LinearGlsk> glsk, List<Pair<Country, Country>> boundaries, SystematicSensitivityResult sensitivityResult) {
        Map<BranchCnec, Double> ptdfSums = new HashMap<>();
        Map<String, Map<Country, Double>> ptdfMap = buildPtdfMap(cnecs, glsk, getCountriesInBoundaries(boundaries), sensitivityResult);
        cnecs.forEach(cnec -> {
            double ptdfSum = 0;
            for (Pair<Country, Country> countryPair : boundaries) {
                if (ptdfMap.get(cnec.getId()).containsKey(countryPair.getLeft()) && ptdfMap.get(cnec.getId()).containsKey(countryPair.getRight())) {
                    ptdfSum += Math.abs(ptdfMap.get(cnec.getId()).get(countryPair.getLeft()) - ptdfMap.get(cnec.getId()).get(countryPair.getRight()));
                }
            }
            ptdfSums.put(cnec, ptdfSum);
        });
        return ptdfSums;
    }

    private static Map<String, Map<Country, Double>> buildPtdfMap(Set<BranchCnec> cnecs, ZonalData<LinearGlsk> glsk, List<Country> countriesInBoundaries, SystematicSensitivityResult sensitivityResult) {

        Map<String, Map<Country, Double>> ptdfs = new HashMap<>();
        Map<String, LinearGlsk> mapCountryLinearGlsk = glsk.getDataPerZone();

        for (LinearGlsk linearGlsk: mapCountryLinearGlsk.values()) {
            if (isGlskInBoundaries(linearGlsk.getId(), countriesInBoundaries)) {
                Country country = glskIdToCountry(linearGlsk.getId());
                for (BranchCnec cnec : cnecs) {
                    double ptdfValue = sensitivityResult.getSensitivityOnFlow(linearGlsk, cnec);
                    if (!ptdfs.containsKey(cnec.getId())) {
                        ptdfs.put(cnec.getId(), new HashMap<>());
                    }
                    ptdfs.get(cnec.getId()).put(country, ptdfValue);
                }
            }
        }
        return ptdfs;
    }

    private static boolean isGlskInBoundaries(String glskId, List<Country> countriesInBoundaries) {
        try {
            Country glskCountry = glskIdToCountry(glskId);
            return countriesInBoundaries.contains(glskCountry);
        } catch (IllegalArgumentException | FaraoException e) {
            return false;
        }
    }

    private static Country glskIdToCountry(String glskId) {
        if (glskId.length() < EICode.LENGTH) {
            throw new IllegalArgumentException(String.format("GlskId [%s] should starts with an EI Code", glskId));
        }
        EICode eiCode = new EICode(glskId.substring(0, EICode.LENGTH));
        return eiCode.getCountry();
    }

    private static List<Country> getCountriesInBoundaries(List<Pair<Country, Country>> boundaries) {
        return boundaries.stream()
            .flatMap(countryPair -> Stream.of(countryPair.getLeft(), countryPair.getRight()))
            .distinct()
            .collect(Collectors.toList());
    }
}
