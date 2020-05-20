/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.commons.FaraoException;

/**
 * Physical units
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public enum Unit {
    AMPERE(PhysicalParameter.FLOW),
    DEGREE(PhysicalParameter.ANGLE),
    MEGAWATT(PhysicalParameter.FLOW),
    KILOVOLT(PhysicalParameter.VOLTAGE),
    PERCENT(PhysicalParameter.FLOW),
    TAP(PhysicalParameter.ANGLE);

    private PhysicalParameter physicalParameter;

    Unit(PhysicalParameter physicalParameter) {
        this.physicalParameter = physicalParameter;
    }

    public PhysicalParameter getPhysicalParameter() {
        return physicalParameter;
    }

    public void checkPhysicalParameter(PhysicalParameter requestedPhysicalParameter) {
        if (!requestedPhysicalParameter.equals(physicalParameter)) {
            throw new FaraoException(String.format("%s Unit is not suited to measure a %s value.", this.toString(), requestedPhysicalParameter.toString()));
        }
    }
}
