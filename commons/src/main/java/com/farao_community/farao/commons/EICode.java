/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.commons;

import com.powsybl.iidm.network.Country;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class EICode {

    /**
     * number of character in an EI Code
     */
    public static final int EIC_LENGTH = 16;

    private String areaCode;
    private boolean isCountryCode;

    public EICode(String areaCode) {
        if (areaCode.length() != EIC_LENGTH) {
            throw new IllegalArgumentException(String.format("Unvalid EICode %s, EICode must have %d characters", areaCode, EIC_LENGTH));
        }
        try {
            Country country = new CountryEICode(areaCode).getCountry();
            this.isCountryCode = country != null;

        } catch (IllegalArgumentException e) {
            this.isCountryCode = false;
        }
        this.areaCode = areaCode;
    }

    public EICode(Country country) {
        this.areaCode = new CountryEICode(country).getCode();
        this.isCountryCode = true;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public boolean isCountryCode() {
        return isCountryCode;
    }

    /**
     * @deprecated all EIC which are not country code might not necessary be virtual hubs
     *             prefer using !{@link #isCountryCode()} instead.
     */
    public boolean isVirtualHub() {
        return !isCountryCode;
    }

    @Override
    public String toString() {
        return isCountryCode ? new CountryEICode(areaCode).getCountry().toString() : areaCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EICode eiCode = (EICode) o;
        try {
            Country country = new CountryEICode(this.areaCode).getCountry();
            Country otherCountry = new CountryEICode(eiCode.areaCode).getCountry();
            return country == otherCountry;
        } catch (IllegalArgumentException e) {
            return areaCode.equals(eiCode.getAreaCode());
        }
    }

    @Override
    public int hashCode() {
        int hashcode;
        try {
            Country country = new CountryEICode(areaCode).getCountry();
            hashcode = country.hashCode();
        } catch (IllegalArgumentException e) {
            hashcode = areaCode.hashCode();
        }
        return hashcode;
    }
}
