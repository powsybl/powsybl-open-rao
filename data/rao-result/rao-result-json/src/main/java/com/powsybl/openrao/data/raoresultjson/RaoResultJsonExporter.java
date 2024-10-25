/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresultjson;

import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracCreationContext;
import com.powsybl.openrao.data.cracio.json.JsonCracCreationContext;
import com.powsybl.openrao.data.raoresultapi.io.Exporter;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.raoresultjson.serializers.RaoResultJsonSerializerModule;
import com.powsybl.commons.json.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Rao Result exporter in JSON format.
 * <p/>
 * Optional properties:
 * <ul>
 *     <li>
 *         <i>flows-in-amperes</i>: boolean (default is "false").
 *     </li>
 *     <li>
 *         <i>flows-in-megawatts</i>: boolean (default is "false").
 *     </li>
 * </ul>
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(Exporter.class)
public class RaoResultJsonExporter implements Exporter<JsonCracCreationContext> {
    private static final String FLOWS_IN_AMPERES = "flows-in-amperes";
    private static final String FLOWS_IN_MEGAWATTS = "flows-in-megawatts";

    @Override
    public String getFormat() {
        return "JSON";
    }

    @Override
    public void exportData(RaoResult raoResult, JsonCracCreationContext cracCreationContext, Properties properties, OutputStream outputStream) {
        exportData(raoResult, cracCreationContext.getCrac(), properties, outputStream);
    }

    @Override
    public void exportData(RaoResult raoResult, Crac crac, Properties properties, OutputStream outputStream) {
        boolean flowsInAmperes = Boolean.parseBoolean(properties.getProperty(FLOWS_IN_AMPERES, "false"));
        boolean flowsInMegawatts = Boolean.parseBoolean(properties.getProperty(FLOWS_IN_MEGAWATTS, "false"));
        if (!flowsInAmperes && !flowsInMegawatts) {
            throw new OpenRaoException("At least one flow unit should be used. Please provide %s and/or %s in the properties.".formatted(FLOWS_IN_AMPERES, FLOWS_IN_MEGAWATTS));
        }
        Set<Unit> flowUnits = new HashSet<>();
        if (flowsInAmperes) {
            flowUnits.add(Unit.AMPERE);
        }
        if (flowsInMegawatts) {
            flowUnits.add(Unit.MEGAWATT);
        }
        try {
            ObjectMapper objectMapper = JsonUtil.createObjectMapper();
            SimpleModule module = new RaoResultJsonSerializerModule(crac, flowUnits);
            objectMapper.registerModule(module);
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(outputStream, raoResult);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
