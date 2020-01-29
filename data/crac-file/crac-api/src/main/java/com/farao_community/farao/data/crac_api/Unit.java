/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

/**
 * Physical units
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public enum Unit {
    AMPERE("AMPERE", PhysicalParameter.FLOW),
    DEGREE("DEGREE", PhysicalParameter.ANGLE),
    MEGAWATT("MEGAWATT", PhysicalParameter.FLOW),
    KILOVOLT("KILOVOLT", PhysicalParameter.VOLTAGE);

    private String name;
    private PhysicalParameter physicalParameter;

    Unit(String name, PhysicalParameter physicalParameter) {
        this.name = name;
        this.physicalParameter = physicalParameter;
    }

    public String toString() {
        return name;
    }

    public PhysicalParameter physicalParameter() {
        return physicalParameter;
    }
}
