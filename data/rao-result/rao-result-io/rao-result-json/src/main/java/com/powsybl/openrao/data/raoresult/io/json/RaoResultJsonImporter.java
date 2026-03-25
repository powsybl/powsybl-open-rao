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
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.io.utils.SafeFileReader;
import com.powsybl.openrao.data.crac.api.io.utils.SafeFileReader.RunException;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.io.Importer;
import com.powsybl.openrao.data.raoresult.io.json.deserializers.RaoResultDeserializer;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(Importer.class)
public class RaoResultJsonImporter implements Importer {

    private static final ObjectMapper JSON_MAPPER_READ = initReader();

    private static ObjectMapper initReader() {
        ObjectMapper mapper = JsonUtil.createObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(RaoResult.class, new RaoResultDeserializer(true));
        mapper.registerModule(module);
        return mapper;
    }

    @Override
    public String getFormat() {
        return "JSON";
    }

    @Override
    public boolean exists(SafeFileReader inputFile) {
        return inputFile.withReadStream(is -> {
            try {
                // TODO: replace this by a call to RaoResultDeserializer.isValid
                JSON_MAPPER_READ.readValue(is, RaoResult.class);
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Override
    public RaoResult importData(SafeFileReader inputFile, Crac crac) {
        ObjectMapper mapper = JsonUtil.createObjectMapper();
        SimpleModule module = new SimpleModule();
        //TODO Lui why crac parameter?
        module.addDeserializer(RaoResult.class, new RaoResultDeserializer(crac));
        mapper.registerModule(module);

        try {
            return inputFile.withReadStream(is -> mapper.readValue(is, RaoResult.class));
        } catch (RunException e) {
            if (e.getCause() instanceof OpenRaoException oe) {
                throw oe;
            }
            throw new RuntimeException(e.getCause());
        }

    }

}
