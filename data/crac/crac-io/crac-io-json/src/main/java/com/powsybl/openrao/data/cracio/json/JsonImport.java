/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.auto.service.AutoService;
import com.networknt.schema.JsonSchema;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracCreationContext;
import com.powsybl.openrao.data.cracapi.io.Importer;
import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;
import com.powsybl.openrao.data.cracio.json.deserializers.CracDeserializer;
import org.apache.commons.lang3.tuple.Pair;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.powsybl.commons.json.JsonUtil.createObjectMapper;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.data.cracio.json.JsonSchemaProvider.getSchema;
import static com.powsybl.openrao.data.cracio.json.JsonSchemaProvider.getValidationErrors;
import static com.powsybl.openrao.data.cracio.json.JsonSchemaProvider.isCracFile;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@AutoService(Importer.class)
public class JsonImport implements Importer {
    @Override
    public String getFormat() {
        return "JSON";
    }

    @Override
    public boolean exists(String filename, InputStream inputStream) {
        if (!filename.endsWith(".json")) {
            return false;
        }
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(inputStream.readAllBytes());
            if (isCracFile(byteArrayInputStream)) {
                byteArrayInputStream.reset();
                Pair<Integer, Integer> cracVersion = readVersion(byteArrayInputStream);
                JsonSchema jsonSchema = getSchema(cracVersion.getLeft(), cracVersion.getRight());
                List<String> validationError = getValidationErrors(jsonSchema, byteArrayInputStream);
                if (validationError.isEmpty()) {
                    return true;
                }
                throw new OpenRaoException("JSON file is not a valid CRAC v%s.%s. Reasons: %s".formatted(cracVersion.getLeft(), cracVersion.getRight(), String.join("; ", validationError)));
            }
            return false;
        } catch (IOException e) {
            TECHNICAL_LOGS.debug("JSON file could not be processed as CRAC. Reason: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public CracCreationContext importData(InputStream inputStream, CracCreationParameters cracCreationParameters, Network network, OffsetDateTime offsetDateTime) {
        if (network == null) {
            throw new OpenRaoException("Network object is null but it is needed to map contingency's elements");
        }
        try {
            ObjectMapper objectMapper = createObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addDeserializer(Crac.class, new CracDeserializer(cracCreationParameters.getCracFactory(), network));
            objectMapper.registerModule(module);
            Crac crac = objectMapper.readValue(inputStream, Crac.class);
            CracCreationContext cracCreationContext = new JsonCracCreationContext(true, crac, network.getNameOrId());
            if (offsetDateTime != null) {
                cracCreationContext.getCreationReport().warn("OffsetDateTime was ignored by the JSON CRAC importer");
            }
            return cracCreationContext;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (OpenRaoException e) {
            CracCreationContext cracCreationContext = new JsonCracCreationContext(false, null, network.getNameOrId());
            cracCreationContext.getCreationReport().error(e.getMessage());
            return cracCreationContext;
        }
    }

    private static Pair<Integer, Integer> readVersion(ByteArrayInputStream cracByteArrayInputStream) {
        String cracContent = new String(cracByteArrayInputStream.readAllBytes(), StandardCharsets.UTF_8);
        cracByteArrayInputStream.reset();
        Pattern versionPattern = Pattern.compile("\"version\"\\s?:\\s?\"([1-9]\\d*)\\.(\\d+)\"");
        Matcher versionMatcher = versionPattern.matcher(cracContent);
        return versionMatcher.find() ? Pair.of(Integer.parseInt(versionMatcher.group(1)), Integer.parseInt(versionMatcher.group(2))) : null;
    }
}
