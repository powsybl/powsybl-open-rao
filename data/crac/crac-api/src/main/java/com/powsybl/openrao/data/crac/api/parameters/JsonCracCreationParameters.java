/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.parameters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Suppliers;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.extensions.ExtensionJsonSerializer;
import com.powsybl.commons.extensions.ExtensionProviders;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.Objects;

/**
 * Provides methods to read and write CracCreatorParameters from and to JSON.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class JsonCracCreationParameters {

    public interface ExtensionSerializer<E extends Extension<CracCreationParameters>> extends ExtensionJsonSerializer<CracCreationParameters, E> {
    }

    private static final Supplier<ExtensionProviders<ExtensionSerializer>> SUPPLIER =
            Suppliers.memoize(() -> ExtensionProviders.createProvider(ExtensionSerializer.class, CracCreationParameters.MODULE_NAME));

    public static ExtensionProviders<ExtensionSerializer> getExtensionSerializers() {
        return SUPPLIER.get();
    }

    private JsonCracCreationParameters() {
    }

    public static CracCreationParameters read(Path jsonFile) {
        return update(new CracCreationParameters(), jsonFile);
    }

    public static CracCreationParameters read(InputStream jsonStream) {
        return update(new CracCreationParameters(), jsonStream);
    }

    public static CracCreationParameters update(CracCreationParameters parameters, Path jsonFile) {
        Objects.requireNonNull(jsonFile);

        try (InputStream is = Files.newInputStream(jsonFile)) {
            return update(parameters, is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static CracCreationParameters update(CracCreationParameters parameters, InputStream jsonStream) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            return objectMapper.readerForUpdating(parameters).readValue(jsonStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void write(CracCreationParameters parameters, Path jsonFile) {
        Objects.requireNonNull(jsonFile);

        try (OutputStream outputStream = Files.newOutputStream(jsonFile)) {
            write(parameters, outputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void write(CracCreationParameters parameters, OutputStream outputStream) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(outputStream, parameters);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ObjectMapper createObjectMapper() {
        return JsonUtil.createObjectMapper()
                .registerModule(new CracCreationParametersJsonModule());
    }
}
