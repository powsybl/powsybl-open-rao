/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.farao_community.farao.data.refprog.reference_program;

import com.farao_community.farao.util.EICode;
import com.powsybl.iidm.network.Country;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ReferenceProgram {
    private final List<ReferenceExchangeData> referenceExchangeDataList;

    public ReferenceProgram(List<ReferenceExchangeData> referenceExchangeDataList) {
        this.referenceExchangeDataList = referenceExchangeDataList;
    }

    public List<ReferenceExchangeData> getReferenceExchangeDataList() {
        return referenceExchangeDataList;
    }

    public double getGlobalNetPosition(Country areaId) {
        double netPosition = 0.;
        netPosition += referenceExchangeDataList.stream()
                .filter(referenceExchangeData -> referenceExchangeData.getAreaOut() != null && referenceExchangeData.getAreaOut().equals(areaId))
                .mapToDouble(ReferenceExchangeData::getFlow).sum();
        netPosition -= referenceExchangeDataList.stream()
                .filter(referenceExchangeData -> referenceExchangeData.getAreaIn() != null && referenceExchangeData.getAreaIn().equals(areaId))
                .mapToDouble(ReferenceExchangeData::getFlow).sum();
        return netPosition;
    }

    public double getExchange(String areaOrigin, String areaExtremity) {
        return getExchange(new EICode(areaOrigin).getCountry(), new EICode(areaExtremity).getCountry());
    }

    public double getGlobalNetPosition(String area) {
        return getGlobalNetPosition(new EICode(area).getCountry());
    }

    public double getExchange(Country areaOrigin, Country areaExtremity) {
        List<ReferenceExchangeData> entries = referenceExchangeDataList.stream().filter(referenceExchangeData -> referenceExchangeData.isAreaOutToAreaInExchange(areaOrigin, areaExtremity)).collect(Collectors.toList());
        if (!entries.isEmpty()) {
            return entries.stream().mapToDouble(ReferenceExchangeData::getFlow).sum();
        } else {
            return -referenceExchangeDataList.stream().filter(referenceExchangeData -> referenceExchangeData.isAreaOutToAreaInExchange(areaExtremity, areaOrigin)).mapToDouble(ReferenceExchangeData::getFlow).sum();
        }
    }

}
