/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

/**
 * Enum representing the instants of a RAO
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public enum Instant {
    PREVENTIVE(0, "preventive"),
    OUTAGE(1, "outage"),
    CURATIVE(2, "curative");

    private final int order;
    private final String name;

    Instant(int order, String name) {
        this.order = order;
        this.name = name;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return name;
    }
}
