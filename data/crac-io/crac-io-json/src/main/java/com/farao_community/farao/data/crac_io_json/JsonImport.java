/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracImporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;

import static com.powsybl.commons.json.JsonUtil.createObjectMapper;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class JsonImport implements CracImporter {
    private static final String JSON_EXTENSION = "json";
    @Override
    public Crac importCrac(InputStream inputStream, @Nullable OffsetDateTime timeStampFilter) {
        if (timeStampFilter != null) {
            //LOGGER.warn("Timestamp filtering is not implemented for json importer. The timestamp will be ignored.");
        }
        try {
            ObjectMapper objectMapper = createObjectMapper();
            SimpleModule module = new CracJsonModule();
            objectMapper.registerModule(module);
            return objectMapper.readValue(inputStream, Crac.class);
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
