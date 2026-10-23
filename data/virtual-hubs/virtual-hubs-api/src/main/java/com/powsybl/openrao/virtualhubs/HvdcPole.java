/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import java.util.List;
import java.util.Objects;

public record HvdcPole(String id, List<HvdcConverter> converters, List<HvdcLine> lines) {
    public HvdcPole(final String id, final List<HvdcConverter> converters, final List<HvdcLine> lines) {
        this.id = Objects.requireNonNull(id);
        this.converters = Objects.requireNonNull(
            converters, "Virtual hubs configuration does not allow adding null hvdc converters"
        );
        this.lines = Objects.requireNonNull(
            lines, "Virtual hubs configuration does not allow adding null hvdc lines"
        );
    }
}
