/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.json.deserializers.SimpleCracDeserializer;
import com.farao_community.farao.data.crac_io_api.CracImporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.auto.service.AutoService;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;

import static com.powsybl.commons.json.JsonUtil.createObjectMapper;

/**
 * CRAC object import in json format
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@AutoService(CracImporter.class)
public class JsonImport implements CracImporter {
    private static final String JSON_EXTENSION = "json";
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonImport.class);

    @Override
    public Crac importCrac(InputStream inputStream, OffsetDateTime timeStampFilter) {
        if (timeStampFilter != null) {
            LOGGER.warn("Timestamp filtering is not implemented for json importer. The timestamp will be ignored.");
        }
        try {
            ObjectMapper objectMapper = createObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addDeserializer(SimpleCrac.class, new SimpleCracDeserializer());
            objectMapper.registerModule(module);
            return objectMapper.readValue(inputStream, SimpleCrac.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean exists(String fileName, InputStream inputStream) {
        return validCracFile(fileName);
    }

    private boolean validCracFile(String fileName) {
        return FilenameUtils.getExtension(fileName).equals(JSON_EXTENSION);
    }
}
