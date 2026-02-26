/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json;

import static com.powsybl.commons.json.JsonUtil.createObjectMapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.opentelemetry.OpenTelemetryReporter;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.io.Exporter;
import com.powsybl.openrao.data.crac.io.json.serializers.CracJsonSerializerModule;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

/**
 * CRAC object export in json format
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@AutoService(Exporter.class)
public class JsonExport implements Exporter {

    private static final String JSON_FORMAT = "JSON";

    @Override
    public String getFormat() {
        return JSON_FORMAT;
    }

    @Override
    public void exportData(Crac crac, OutputStream outputStream) {
        OpenTelemetryReporter.withSpan("rao.exportJsonCrac", cx -> {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            SimpleModule module = new CracJsonSerializerModule();
            objectMapper.registerModule(module);
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(outputStream, crac);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        });
    }
}
