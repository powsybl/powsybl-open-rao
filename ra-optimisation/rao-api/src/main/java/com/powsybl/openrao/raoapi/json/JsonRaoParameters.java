/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.json;

import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.extensions.ExtensionJsonSerializer;
import com.powsybl.commons.extensions.ExtensionProviders;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.sensitivity.json.SensitivityJsonModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Provides methods to read and write JsonRaoParameters from and to JSON.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class JsonRaoParameters {

    /**
     * A configuration loader interface for the RaoParameters extensions loaded from the platform configuration
     *
     * @param <E> The extension class
     */
    public interface ExtensionSerializer<E extends Extension<RaoParameters>> extends ExtensionJsonSerializer<RaoParameters, E> {
    }

    /**
     * Lazily initialized list of extension serializers.
     */
    private static final Supplier<ExtensionProviders<ExtensionSerializer>> SUPPLIER =
            Suppliers.memoize(() -> ExtensionProviders.createProvider(ExtensionSerializer.class, "rao-parameters"));

    /**
     * @return the known extension serializers.
     */
    public static ExtensionProviders<ExtensionSerializer> getExtensionSerializers() {
        return SUPPLIER.get();
    }

    private JsonRaoParameters() {
    }

    /**
     * @param jsonFile Path where the RaoParameters should be read from
     * @return parameters from a JSON file (will NOT rely on platform config).
     */
    public static RaoParameters read(Path jsonFile) {
        return update(new RaoParameters(), jsonFile);
    }

    /**
     * @param jsonStream InputStream where the RaoParameters are
     * @return  parameters from a JSON file (will NOT rely on platform config).
     */
    public static RaoParameters read(InputStream jsonStream) {
        return update(new RaoParameters(), jsonStream);
    }

    /**
     * @param parameters RaoParameters containing original parameters
     * @param jsonFile Path containing parameters to update
     * @return parameters updated with the ones found by reading the content of jsonFile.
     */
    public static RaoParameters update(RaoParameters parameters, Path jsonFile) {
        Objects.requireNonNull(jsonFile);

        try (InputStream is = Files.newInputStream(jsonFile)) {
            return update(parameters, is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @param parameters RaoParameters containing original parameters
     * @param jsonStream InputStream containing parameters to update
     * @return parameters updated with the ones found by reading the content of jsonStream.
     */
    public static RaoParameters update(RaoParameters parameters, InputStream jsonStream) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            return objectMapper.readerForUpdating(parameters).readValue(jsonStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes parameters as JSON to a file.
     *
     * @param parameters RaoParameters containing the parameters that will be exported to a file
     * @param jsonFile Path containing the file where the parameters will be exported
     */
    public static void write(RaoParameters parameters, Path jsonFile) {
        Objects.requireNonNull(jsonFile);

        try (OutputStream outputStream = Files.newOutputStream(jsonFile)) {
            write(parameters, outputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes parameters as JSON to an OutputStream
     *
     * @param parameters RaoParameters containing the parameters that will be exported to an OutputStream
     * @param outputStream OutputStream where the parameters will be exported
     */
    public static void write(RaoParameters parameters, OutputStream outputStream) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(outputStream, parameters);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Low level deserialization method, to be used for instance for reading rao parameters nested in another object.
     *
     * @param parser JsonParser of a file containing a representation of RaoParameters
     * @param context DeserializationContext used in the deserialization
     * @param parameters RaoParameters to be updated
     * @return RaoParameters object updated with the content of the JsonParser
     * @throws IOException when an unexpected field is found
     */
    public static RaoParameters deserialize(JsonParser parser, DeserializationContext context, RaoParameters parameters) throws IOException {
        return new RaoParametersDeserializer().deserialize(parser, context, parameters);
    }

    /**
     * Low level deserialization method, to be used for instance for updating rao parameters nested in another object.
     *
     * @param parser JsonParser of a file containing a representation of RaoParameters
     * @param context DeserializationContext used in the deserialization
     * @return RaoParameters object representing the content of the JsonParser
     * @throws IOException when an unexpected field is found
     */
    public static RaoParameters deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return new RaoParametersDeserializer().deserialize(parser, context);
    }

    /**
     * Low level serialization method, to be used for instance for writing Rao Parameters nested in another object.
     *
     * @param parameters RaoParameters containing what needs to be serialized
     * @param jsonGenerator JsonGenerator used for the serialization
     * @param serializerProvider SerializerProvider used for the serialization
     * @throws IOException if the serialization fails
     */
    public static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        new RaoParametersSerializer().serialize(parameters, jsonGenerator, serializerProvider);
    }

    private static ObjectMapper createObjectMapper() {
        return JsonUtil.createObjectMapper()
                .registerModule(new RaoParametersJsonModule())
                .registerModule(new SensitivityJsonModule());
    }
}
