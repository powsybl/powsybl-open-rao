/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public record SynchronousArea(String code,
                              String eic,
                              boolean isLive,
                              boolean isMainArea,
                              Map<String, String> localLOT3Parameters,
                              List<Voltage> initialVoltages) {
    public SynchronousArea(final String code,
                           final String eic,
                           final boolean isLive,
                           final boolean isMainArea,
                           final Map<String, String> localLOT3Parameters,
                           final List<Voltage> initialVoltages) {
        this.code = Objects.requireNonNull(code, "SynchronousArea creation does not allow null attribute 'code'");
        this.eic = Objects.requireNonNull(eic, "SynchronousArea creation does not allow null attribute 'eic'");
        this.isLive = isLive;
        this.isMainArea = isMainArea;
        this.localLOT3Parameters = localLOT3Parameters;
        this.initialVoltages = initialVoltages;
    }
}
