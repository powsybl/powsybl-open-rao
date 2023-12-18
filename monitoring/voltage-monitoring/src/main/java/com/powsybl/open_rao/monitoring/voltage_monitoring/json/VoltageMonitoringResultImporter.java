/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.monitoring.voltage_monitoring.json;

import com.powsybl.open_rao.data.crac_api.Crac;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.powsybl.open_rao.monitoring.voltage_monitoring.VoltageMonitoringResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import static com.powsybl.commons.json.JsonUtil.createObjectMapper;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class VoltageMonitoringResultImporter {
    public VoltageMonitoringResult importVoltageMonitoringResult(InputStream inputStream, Crac crac) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addDeserializer(VoltageMonitoringResult.class, new VoltageMonitoringResultDeserializer(crac));
            objectMapper.registerModule(module);
            return objectMapper.readValue(inputStream, VoltageMonitoringResult.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
