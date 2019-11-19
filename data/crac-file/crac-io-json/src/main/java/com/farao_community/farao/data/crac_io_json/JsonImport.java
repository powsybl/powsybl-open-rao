/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracExporter;
import com.farao_community.farao.data.crac_io_api.CracImporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;

import java.io.*;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import static com.powsybl.commons.json.JsonUtil.createObjectMapper;

/**
 * CRAC object import in json format
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@AutoService(CracExporter.class)
public class JsonImport implements CracImporter {

    private static final String CRAC_FILE_SCHEMA_JSON = "/CracSchema.json";
    private static final Schema SCHEMA_JSON;

    static {
        JSONObject jsonSchema = new JSONObject(
            new JSONTokener(JsonImport.class.getResourceAsStream(CRAC_FILE_SCHEMA_JSON)));
        SCHEMA_JSON = SchemaLoader.load(jsonSchema);
    }

    @Override
    public Crac importCrac(InputStream inputStream) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            return objectMapper.readValue(inputStream, Crac.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean exists(InputStream inputStream) {
        return validCracFile(inputStream);
    }

    private boolean validCracFile(InputStream inputStream) {
        try {
            JSONObject jsonSubject = new JSONObject(
                new JSONTokener(inputStream));
            SCHEMA_JSON.validate(jsonSubject);
            return true;
        } catch (Exception ve) {
            return false;
        }
    }
}
