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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.powsybl.commons.json.JsonUtil.createObjectMapper;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

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
            Map<Pair<Integer, Integer>, List<String>> validationErrorsPerVersion = new HashMap<>();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(inputStream.readAllBytes());
            for (String schemaFile : JsonSchemaProvider.getAllSchemaFiles()) {
                List<String> validationErrors = JsonSchemaProvider.getValidationErrors(JsonSchemaProvider.getSchema(schemaFile), byteArrayInputStream);
                if (validationErrors.isEmpty()) {
                    return true;
                }
                validationErrorsPerVersion.put(JsonSchemaProvider.getCracVersionFromSchema(schemaFile), new ArrayList<>(validationErrors));
                byteArrayInputStream.reset();
            }
            validationErrorsPerVersion.forEach((version, validationErrors) -> TECHNICAL_LOGS.debug("JSON file is not a valid CRAC v{}.{}. Reasons: {}", version.getLeft(), version.getRight(), String.join("; ", validationErrors)));
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

}
