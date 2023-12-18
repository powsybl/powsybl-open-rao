/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.rao_result_json;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.rao_result_api.RaoResult;
import com.powsybl.open_rao.data.rao_result_json.serializers.RaoResultJsonSerializerModule;
import com.powsybl.commons.json.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoResultExporter {

    public void export(RaoResult raoResult, Crac crac, Set<Unit> flowUnits, OutputStream outputStream) {
        if (flowUnits.isEmpty()) {
            throw new FaraoException("At least one flow unit should be defined");
        }
        if (flowUnits.stream().anyMatch(unit -> !unit.equals(Unit.AMPERE) && !unit.equals(Unit.MEGAWATT))) {
            // TODO : we can actually handle all units with PhysicalParameter.FLOW
            // but we'll have to add export feature for %Imax
            throw new FaraoException("Flow unit should be AMPERE and/or MEGAWATT");
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
