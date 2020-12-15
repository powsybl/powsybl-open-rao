/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.farao_community.farao.data.refprog.reference_program;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ReferenceProgram {
    private final List<ReferenceExchangeData> referenceExchangeDataList;
    private final Set<ReferenceProgramArea> referenceProgramAreas;
    private final Map<ReferenceProgramArea, Double> netPositions;

    public ReferenceProgram(List<ReferenceExchangeData> referenceExchangeDataList) {
        this.referenceExchangeDataList = referenceExchangeDataList;
        this.referenceProgramAreas = new HashSet<>();
        this.referenceExchangeDataList.stream().forEach(referenceExchangeData -> {
            if (referenceExchangeData.getAreaOut() != null && !referenceExchangeData.getAreaOut().isVirtualHub()) {
                referenceProgramAreas.add(referenceExchangeData.getAreaOut());
            }
            if (referenceExchangeData.getAreaIn() != null && !referenceExchangeData.getAreaIn().isVirtualHub()) {
                referenceProgramAreas.add(referenceExchangeData.getAreaIn());
            }
        });
        netPositions = new HashMap<>();
        referenceProgramAreas.forEach(country -> netPositions.put(country, computeGlobalNetPosition(country)));
    }

    public List<ReferenceExchangeData> getReferenceExchangeDataList() {
        return referenceExchangeDataList;
    }

    public Set<ReferenceProgramArea> getListOfCountries() {
        return referenceProgramAreas;
    }

    private double computeGlobalNetPosition(ReferenceProgramArea referenceProgramArea) {
        double netPosition = 0.;
        netPosition += referenceExchangeDataList.stream()
                .filter(referenceExchangeData -> referenceExchangeData.getAreaOut() != null &&
                        !referenceExchangeData.getAreaOut().isVirtualHub() &&
                        referenceExchangeData.getAreaOut().equals(referenceProgramArea))
                .mapToDouble(ReferenceExchangeData::getFlow).sum();
        netPosition -= referenceExchangeDataList.stream()
                .filter(referenceExchangeData -> referenceExchangeData.getAreaIn() != null &&
                        !referenceExchangeData.getAreaIn().isVirtualHub() &&
                        referenceExchangeData.getAreaIn().equals(referenceProgramArea))
                .mapToDouble(ReferenceExchangeData::getFlow).sum();
        return netPosition;
    }

    public double getGlobalNetPosition(ReferenceProgramArea country) {
        return netPositions.get(country);
    }

    public double getExchange(String areaOrigin, String areaExtremity) {
        return getExchange(new ReferenceProgramArea(areaOrigin), new ReferenceProgramArea(areaExtremity));
    }

    public double getGlobalNetPosition(String area) {
        return getGlobalNetPosition(new ReferenceProgramArea(area));
    }

    public double getExchange(ReferenceProgramArea areaOrigin, ReferenceProgramArea areaExtremity) {
        List<ReferenceExchangeData> entries = referenceExchangeDataList.stream().filter(referenceExchangeData -> referenceExchangeData.isAreaOutToAreaInExchange(areaOrigin, areaExtremity)).collect(Collectors.toList());
        if (!entries.isEmpty()) {
            return entries.stream().mapToDouble(ReferenceExchangeData::getFlow).sum();
        } else {
            return -referenceExchangeDataList.stream().filter(referenceExchangeData -> referenceExchangeData.isAreaOutToAreaInExchange(areaExtremity, areaOrigin)).mapToDouble(ReferenceExchangeData::getFlow).sum();
        }
    }

    public Map<ReferenceProgramArea, Double> getAllGlobalNetPositions() {
        return netPositions;
    }

}
