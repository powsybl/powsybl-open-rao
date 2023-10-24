/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.commons;

import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.iidm.network.Country;

import java.util.Optional;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class EICode {

    /**
     * number of character in an EI Code
     */
    public static final int EIC_LENGTH = 16;

    private final String areaCode;
    private Optional<Country> optionalCountry;

    public EICode(String areaCode) {
        if (areaCode.length() != EIC_LENGTH) {
            throw new IllegalArgumentException(String.format("Unvalid EICode %s, EICode must have %d characters", areaCode, EIC_LENGTH));
        }
        try {
            optionalCountry = Optional.ofNullable(new CountryEICode(areaCode).getCountry());
        } catch (IllegalArgumentException e) {
            this.optionalCountry = Optional.empty();
        }
        this.areaCode = areaCode;
    }

    public EICode(Country country) {
        this.areaCode = new CountryEICode(country).getCode();
        this.optionalCountry = Optional.of(country);
    }

    public String getAreaCode() {
        return areaCode;
    }

    public boolean isCountryCode() {
        return optionalCountry.isPresent();
    }

    @Override
    public String toString() {
        return optionalCountry.isPresent() ? optionalCountry.get().toString() : areaCode;
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
        return this.optionalCountry.map(country -> country.equals(eiCode.optionalCountry.orElse(null))).orElseGet(() -> areaCode.equals(eiCode.getAreaCode()));
    }

    @Override
    public int hashCode() {
        return this.optionalCountry.map(Enum::hashCode).orElseGet(areaCode::hashCode);
    }
}
