/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.voltage_monitoring.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.powsybl.commons.json.JsonUtil;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class VoltageMonitoringResultExporter {
    public void export(VoltageMonitoringResult voltageMonitoringResult, OutputStream outputStream) {
        try {
            ObjectMapper objectMapper = JsonUtil.createObjectMapper();
            SimpleModule module = new VoltageMonitoringResultJsonSerializationModule();
            objectMapper.registerModule(module);
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(outputStream, voltageMonitoringResult);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
