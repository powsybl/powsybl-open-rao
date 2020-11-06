/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.glsk.api.providers.GlskProvider;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.farao_community.farao.util.EICode;
import com.powsybl.iidm.network.Country;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *  This class computes the absolute PTDF sums on a given set of CNECs
 *  It requires that the sensitivity values be already computed
 *
 *  @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class AbsolutePtdfSumsComputation {
    private AbsolutePtdfSumsComputation() { }

    public static Map<Cnec, Double> computeAbsolutePtdfSums(Set<Cnec> cnecs, GlskProvider glsk, List<Pair<Country, Country>> boundaries, SystematicSensitivityResult sensitivityResult) {
        Map<Cnec, Double> ptdfSums = new HashMap<>();
        Map<String, Map<Country, Double>> ptdfMap = computePtdf(cnecs, glsk, sensitivityResult);
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

    private static Map<String, Map<Country, Double>> computePtdf(Set<Cnec> cnecs, GlskProvider glsk, SystematicSensitivityResult sensitivityResult) {
        Map<String, Map<Country, Double>> ptdfs = new HashMap<>();
        Map<String, LinearGlsk> mapCountryLinearGlsk = glsk.getLinearGlskPerCountry();
        for (Cnec cnec : cnecs) {
            for (LinearGlsk linearGlsk: mapCountryLinearGlsk.values()) {
                double ptdfValue = sensitivityResult.getSensitivityOnFlow(linearGlsk, cnec);
                Country country = glskIdToCountry(linearGlsk.getId());
                if (!ptdfs.containsKey(cnec.getId())) {
                    ptdfs.put(cnec.getId(), new HashMap<>());
                }
                ptdfs.get(cnec.getId()).put(country, ptdfValue);
            }
        }
        return ptdfs;
    }

    private static Country glskIdToCountry(String glskId) {
        if (glskId.length() < EICode.LENGTH) {
            throw new IllegalArgumentException(String.format("GlskId [%s] should starts with an EI Code", glskId));
        }
        EICode eiCode = new EICode(glskId.substring(0, EICode.LENGTH));
        return eiCode.getCountry();
    }
}
