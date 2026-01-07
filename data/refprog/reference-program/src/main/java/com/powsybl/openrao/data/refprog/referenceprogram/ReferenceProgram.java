/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.refprog.referenceprogram;

import com.powsybl.openrao.commons.EICode;

import java.util.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ReferenceProgram {
    private final List<ReferenceExchangeData> referenceExchangeDataList;
    private final Set<EICode> referenceProgramAreas;
    private final Map<EICode, Double> netPositions;

    public ReferenceProgram(List<ReferenceExchangeData> referenceExchangeDataList) {
        this.referenceExchangeDataList = referenceExchangeDataList;
        this.referenceProgramAreas = buildReferenceProgramAreas(referenceExchangeDataList);
        netPositions = new HashMap<>();
        referenceProgramAreas.forEach(country -> netPositions.put(country, computeGlobalNetPosition(country)));
    }

    public List<ReferenceExchangeData> getReferenceExchangeDataList() {
        return referenceExchangeDataList;
    }

    public Set<EICode> getListOfAreas() {
        return referenceProgramAreas;
    }

    private double computeGlobalNetPosition(EICode area) {
        double netPosition = 0.;
        netPosition += referenceExchangeDataList.stream()
            .filter(referenceExchangeData -> referenceExchangeData.getAreaOut() != null &&
                referenceExchangeData.getAreaOut().equals(area))
            .mapToDouble(ReferenceExchangeData::getFlow).sum();
        netPosition -= referenceExchangeDataList.stream()
            .filter(referenceExchangeData -> referenceExchangeData.getAreaIn() != null &&
                referenceExchangeData.getAreaIn().equals(area))
            .mapToDouble(ReferenceExchangeData::getFlow).sum();
        return netPosition;
    }

    public double getGlobalNetPosition(EICode area) {
        return netPositions.get(area);
    }

    public double getExchange(String areaOrigin, String areaExtremity) {
        return getExchange(new EICode(areaOrigin), new EICode(areaExtremity));
    }

    public double getGlobalNetPosition(String area) {
        return getGlobalNetPosition(new EICode(area));
    }

    public double getExchange(EICode areaOrigin, EICode areaExtremity) {
        List<ReferenceExchangeData> entries = referenceExchangeDataList.stream().filter(referenceExchangeData -> referenceExchangeData.isAreaOutToAreaInExchange(areaOrigin, areaExtremity)).toList();
        if (!entries.isEmpty()) {
            return entries.stream().mapToDouble(ReferenceExchangeData::getFlow).sum();
        } else {
            return -referenceExchangeDataList.stream().filter(referenceExchangeData -> referenceExchangeData.isAreaOutToAreaInExchange(areaExtremity, areaOrigin)).mapToDouble(ReferenceExchangeData::getFlow).sum();
        }
    }

    public Map<EICode, Double> getAllGlobalNetPositions() {
        return netPositions;
    }

    private Set<EICode> buildReferenceProgramAreas(List<ReferenceExchangeData> referenceExchangeDataList) {
        Set<EICode> setOfRefProgAreas = new HashSet<>();

        referenceExchangeDataList.forEach(referenceExchangeData -> {
            if (referenceExchangeData.getAreaOut() != null) {
                setOfRefProgAreas.add(referenceExchangeData.getAreaOut());
            }
            if (referenceExchangeData.getAreaIn() != null) {
                setOfRefProgAreas.add(referenceExchangeData.getAreaIn());
            }
        });

        return setOfRefProgAreas;
    }

}
