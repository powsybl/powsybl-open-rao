/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util;

/**
 * Properties to customize UcteNetworkHelper behavior
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UcteNetworkHelperProperties {
    // For Bus IDs with less than 8 characters, either complete them with white spaces or wildcards
    public enum BusIdMatchPolicy {
        COMPLETE_WITH_WHITESPACES,
        COMPLETE_WITH_WILDCARDS
    }

    private BusIdMatchPolicy busIdMatchPolicy;

    public UcteNetworkHelperProperties(BusIdMatchPolicy busIdMatchPolicy) {
        this.busIdMatchPolicy = busIdMatchPolicy;
    }

    public BusIdMatchPolicy getBusIdMatchPolicy() {
        return busIdMatchPolicy;
    }
}
