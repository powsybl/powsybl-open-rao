/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.refprog.reference_program;

import com.farao_community.farao.util.EICode;
import com.powsybl.iidm.network.Country;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class ReferenceProgramArea {

    String areaCode;
    boolean isVirtualHub;

    public ReferenceProgramArea(String areaCode) {
        try {
            Country country = new EICode(areaCode).getCountry();
            this.isVirtualHub = false;

        } catch (IllegalArgumentException e) {
            this.isVirtualHub = true;
        }
        this.areaCode = areaCode;
    }

    public ReferenceProgramArea(Country country) {
        String countryCode;
        try {
            countryCode = new EICode(country).getCode();
        } catch (IllegalArgumentException e) {
            countryCode = country.toString();
            this.isVirtualHub = true;
        }
        this.areaCode = countryCode;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public boolean isVirtualHub() {
        return isVirtualHub;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReferenceProgramArea referenceProgramArea = (ReferenceProgramArea) o;
        return areaCode.equals(referenceProgramArea.getAreaCode());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
