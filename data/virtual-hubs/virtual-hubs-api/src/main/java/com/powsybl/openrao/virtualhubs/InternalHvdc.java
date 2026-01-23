/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import java.util.List;
import java.util.Objects;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public record InternalHvdc(List<HvdcConverter> converters, List<HvdcLine> lines) {
    public void addHvdcConverter(HvdcConverter hvdcConverter) {
        converters.add(Objects.requireNonNull(hvdcConverter, "Virtual hubs configuration does not allow adding null hvdc converter"));
    }

    public void addHvdcLine(HvdcLine hvdcLine) {
        lines.add(Objects.requireNonNull(hvdcLine, "Virtual hubs configuration does not allow adding null hvdc line"));
    }

}
