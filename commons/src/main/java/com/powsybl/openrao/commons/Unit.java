/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Physical units
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public enum Unit {
    AMPERE(PhysicalParameter.FLOW, "A"),
    DEGREE(PhysicalParameter.ANGLE, "°"),
    MEGAWATT(PhysicalParameter.FLOW, "MW"),
    KILOVOLT(PhysicalParameter.VOLTAGE, "kV"),
    PERCENT_IMAX(PhysicalParameter.FLOW, "%"),
    TAP(PhysicalParameter.ANGLE, ""),
    SECTION_COUNT(null, "");

    private PhysicalParameter physicalParameter;
    private String symbol;

    Unit(PhysicalParameter physicalParameter, String symbol) {
        this.physicalParameter = physicalParameter;
        this.symbol = symbol;
    }

    public PhysicalParameter getPhysicalParameter() {
        return physicalParameter;
    }

    @Override
    @JsonValue
    public String toString() {
        return symbol;
    }

    public static Unit getEnum(String value) {
        for (Unit v : values()) {
            if (v.toString().equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    public void checkPhysicalParameter(PhysicalParameter requestedPhysicalParameter) {
        if (!requestedPhysicalParameter.equals(physicalParameter)) {
            throw new OpenRaoException(String.format("%s Unit is not suited to measure a %s value.", this.toString(), requestedPhysicalParameter));
        }
    }
}
