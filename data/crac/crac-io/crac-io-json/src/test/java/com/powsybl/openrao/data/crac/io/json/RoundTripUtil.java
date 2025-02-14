/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.json.serializers.CracJsonSerializerModule;

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
    static Crac implicitJsonRoundTrip(Crac object, Network network) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        object.write("JSON", outputStream);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
            return Crac.read("crac.json", inputStream, network);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Crac explicitJsonRoundTrip(Crac object, Network network) {
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
            return new JsonImport().importData(inputStream, CracCreationParameters.load(), network).getCrac();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
