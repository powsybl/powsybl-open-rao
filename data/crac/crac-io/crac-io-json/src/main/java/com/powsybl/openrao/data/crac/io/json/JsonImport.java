/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json;

import static com.powsybl.commons.json.JsonUtil.createObjectMapper;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.io.Importer;
import com.powsybl.openrao.data.crac.api.io.utils.SafeFileReader;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.json.deserializers.CracDeserializer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@AutoService(Importer.class)
public class JsonImport implements Importer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonImport.class);

    private static final Pattern JSON_VERSION_PATTERN = Pattern.compile(
        "\"version\"\\s?:\\s?\"([1-9]\\d*)\\.(\\d+)\"");

    @Override
    public String getFormat() {
        return "JSON";
    }

    @Override
    public boolean exists(SafeFileReader inputFile) {
        if (!inputFile.hasFileExtension("json")) {
            return false;
        }
        try {

            if (Boolean.FALSE.equals(inputFile.withReadStream(JsonSchemaProvider::isCracFile))) {
                return false;
            }

            var cracVersion = inputFile.withReadStream(this::readVersion);
            LOGGER.debug("Got Crac version: {}", cracVersion);

            var jsonSchema = JsonSchemaProvider.getSchema(cracVersion);

            var validationError = inputFile.withReadStream(
                is -> JsonSchemaProvider.getValidationErrors(jsonSchema, is));
            if (!validationError.isEmpty()) {
                throw new OpenRaoException(
                    "JSON file is not a valid CRAC v%s.%s. Reasons: %s".formatted(
                        cracVersion.majorVersion(), cracVersion.minorVersion(),
                        String.join("; ", validationError)));
            }

            return true;

        } catch (IOException e) {
            TECHNICAL_LOGS.debug("JSON file could not be processed as CRAC. Reason: {}",
                e.getMessage());
            return false;
        }
    }

    @Override
    public CracCreationContext importData(SafeFileReader inputFile,
        CracCreationParameters cracCreationParameters, Network network) {

        if (network == null) {
            throw new OpenRaoException(
                "Network object is null but it is needed to map contingency's elements");
        }

        LOGGER.debug("Starting import");

        var objectMapper = createObjectMapper();
        SimpleModule module = new SimpleModule();
        //TODO Lui why cracCreationParameters ?
        module.addDeserializer(Crac.class,
            new CracDeserializer(cracCreationParameters.getCracFactory(), network));
        objectMapper.registerModule(module);

        return inputFile.withReadStream(is -> {
            try {
                Crac crac = objectMapper.readValue(is, Crac.class);
                return new JsonCracCreationContext(true, crac, network.getNameOrId());
            } catch (OpenRaoException e) {
                CracCreationContext cracCreationContext = new JsonCracCreationContext(false, null,
                    network.getNameOrId());
                cracCreationContext.getCreationReport().error(e.getMessage());
                return cracCreationContext;
            }
        });

    }

    private Version readVersion(InputStream is) {
        final int maxLines = 10;

        LOGGER.debug("Searching for version. maxLines={}", maxLines);

        try (var isr = new InputStreamReader(is,
            StandardCharsets.UTF_8); var br = new BufferedReader(isr)) {
            String line;
            int linesRead = 0;
            while ((line = br.readLine()) != null && linesRead < maxLines) {
                linesRead++;
                Matcher matcher = JSON_VERSION_PATTERN.matcher(line);
                if (matcher.find()) {
                    return new Version(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2))
                    );
                }
            }
        } catch (Exception e) {
            throw new OpenRaoException("Error reading version", e);
        }

        throw new OpenRaoException("Unable to get version");
    }

}
