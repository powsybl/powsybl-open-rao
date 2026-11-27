/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import java.util.List;
import java.util.Objects;

public record HvdcPole(String id, String eic, List<HvdcConverter> converters, List<HvdcLine> lines) {
    public HvdcPole(final String id, final String eic, final List<HvdcConverter> converters, final List<HvdcLine> lines) {
        this.id = Objects.requireNonNull(id, "HvdcPole configuration does not allow adding null attribute 'id'");
        this.eic = Objects.requireNonNull(eic, "HvdcPole configuration does not allow adding null attribute 'eic'");
        this.converters = Objects.requireNonNull(converters, "HvdcPole configuration does not allow adding null attribute 'converters'");
        this.lines = Objects.requireNonNull(lines, "HvdcPole configuration does not allow adding null attribute 'lines'");
    }
}
