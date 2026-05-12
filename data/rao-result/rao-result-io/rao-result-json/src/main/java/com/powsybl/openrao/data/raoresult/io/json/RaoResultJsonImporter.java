/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.io.Importer;
import com.powsybl.openrao.data.raoresult.io.json.deserializers.RaoResultDeserializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import static com.powsybl.commons.json.JsonUtil.createObjectMapper;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(Importer.class)
public class RaoResultJsonImporter implements Importer {
    @Override
    public String getFormat() {
        return "JSON";
    }

    @Override
    public boolean exists(InputStream inputStream) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addDeserializer(RaoResult.class, new RaoResultDeserializer(true));
            objectMapper.registerModule(module);
            // TODO: replace this by a call to RaoResultDeserializer.isValid
            objectMapper.readValue(inputStream, RaoResult.class);
            return true;
        } catch (OpenRaoException | IOException e) {
            return false;
        }
    }

    @Override
    public RaoResult importData(InputStream inputStream, Crac crac) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addDeserializer(RaoResult.class, new RaoResultDeserializer(crac));
            objectMapper.registerModule(module);
            return objectMapper.readValue(inputStream, RaoResult.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
