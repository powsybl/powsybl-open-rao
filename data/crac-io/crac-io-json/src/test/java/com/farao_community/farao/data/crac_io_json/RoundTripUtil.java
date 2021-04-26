/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_json.serializers.CracJsonSerializerModule;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import static com.powsybl.commons.json.JsonUtil.createObjectMapper;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RoundTripUtil {
    private RoundTripUtil() {

    }

    /**
     * This utilitary class enable to export an object through ObjectMapper in an OutputStream
     * and then re-import this stream as the object. The purpose is to see if the whole export/import
     * process works fine.
     *
     * @param object: object to export/import
     * @return the object exported and re-imported
     */
    static Crac roundTrip(Crac object) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ObjectMapper objectMapper = createObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            SimpleModule module = new CracJsonSerializerModule();
            objectMapper.registerModule(module);
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(outputStream, object);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
            return (new JsonImport()).importCrac(inputStream, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
