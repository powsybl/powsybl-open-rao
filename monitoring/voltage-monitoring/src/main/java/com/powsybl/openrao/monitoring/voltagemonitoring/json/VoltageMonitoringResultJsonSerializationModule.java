/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.voltagemonitoring.json;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.powsybl.openrao.monitoring.voltagemonitoring.VoltageMonitoringResult;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class VoltageMonitoringResultJsonSerializationModule extends SimpleModule {

    public VoltageMonitoringResultJsonSerializationModule() {
        super();
        this.addSerializer(VoltageMonitoringResult.class, new VoltageMonitoringResultSerializer());
    }
}
