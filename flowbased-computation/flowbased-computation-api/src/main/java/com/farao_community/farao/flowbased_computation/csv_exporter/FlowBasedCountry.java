/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.csv_exporter;

import com.farao_community.farao.util.EICode;
import org.slf4j.LoggerFactory;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class FlowBasedCountry {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FlowBasedCountry.class);
    private static int NUMBER_OF_CHARACTERS_IN_EI_CODE = 16;

    private String eiCode;
    private String name;

    FlowBasedCountry(String eiCode) {
        this.eiCode = eiCode;
        this.name = convertEICodeToCountryNameIfPossible(eiCode);
    }

    private String convertEICodeToCountryNameIfPossible(String countryEICode) {
        try {
            String eiCodeWith16Characters = countryEICode.substring(0, Math.min(countryEICode.length(), NUMBER_OF_CHARACTERS_IN_EI_CODE));
            return new EICode(eiCodeWith16Characters).getCountry().getName();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown country EI Code {}", countryEICode);
            return countryEICode;
        }
    }

    String getEiCode() {
        return eiCode;
    }

    String getName() {
        return name;
    }
}
