/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_io_api.CracImporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.auto.service.AutoService;

import java.io.*;

import org.apache.commons.io.FilenameUtils;
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
@AutoService(CracImporter.class)
public class JsonImport implements CracImporter {

    private static final String CRAC_FILE_SCHEMA_JSON = "/CracSchema.json";
    private static final String JSON_EXTENSION = "json";
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
            objectMapper.registerModule(new Jdk8Module());
            return objectMapper.readValue(inputStream, SimpleCrac.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean exists(String fileName, InputStream inputStream) {
        return validCracFile(fileName, inputStream);
    }

    private boolean validCracFile(String fileName, InputStream inputStream) {
        return FilenameUtils.getExtension(fileName).equals(JSON_EXTENSION);
    }
}
