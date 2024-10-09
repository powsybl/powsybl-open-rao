/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.commons.ucte;

/**
 * Properties to customize UcteNetworkHelper behavior
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UcteNetworkAnalyzerProperties {

    // For Bus IDs with 7 characters, either complete them with white spaces or wildcards
    public enum BusIdMatchPolicy {
        COMPLETE_WITH_WHITESPACES,
        COMPLETE_WITH_WILDCARDS,
        REPLACE_8TH_CHARACTER_WITH_WILDCARD
    }

    private BusIdMatchPolicy busIdMatchPolicy;

    public UcteNetworkAnalyzerProperties(BusIdMatchPolicy busIdMatchPolicy) {
        this.busIdMatchPolicy = busIdMatchPolicy;
    }

    public BusIdMatchPolicy getBusIdMatchPolicy() {
        return busIdMatchPolicy;
    }
}
