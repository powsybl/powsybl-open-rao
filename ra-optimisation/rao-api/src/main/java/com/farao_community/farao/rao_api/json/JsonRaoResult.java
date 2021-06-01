/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.json;

import com.farao_community.farao.rao_api.RaoResultImpl;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A class to import and export Rao Results in a Json format
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public final class JsonRaoResult {

    private JsonRaoResult() { }

    /**
     * A configuration loader interface for the RaoResultImpl extensions loaded
     *
     * @param <E> The extension class
     */
    public interface ExtensionSerializer<E extends Extension<RaoResultImpl>> extends ExtensionJsonSerializer<RaoResultImpl, E> {
    }

    /**
     * Lazily initialized list of extension serializers.
     */
    private static final Supplier<ExtensionProviders<ExtensionSerializer>> SUPPLIER =
        Suppliers.memoize(() -> ExtensionProviders.createProvider(ExtensionSerializer.class, "rao-result"));

    /**
     * @return the known extension serializers.
     */
    public static ExtensionProviders<ExtensionSerializer> getExtensionSerializers() {
        return SUPPLIER.get();
    }

    /**
     * @param jsonFile Path where the RaoResultImpl is read
     * @return RaoResultImpl from a JSON file (will NOT rely on platform config).
     */
    public static RaoResultImpl read(Path jsonFile) {
        Objects.requireNonNull(jsonFile);

        try (InputStream is = Files.newInputStream(jsonFile)) {
            return read(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @param jsonStream InputStream where the RaoResultImpl is read
     * @return RaoResultImpl from a JSON file (will NOT rely on platform config).
     */
    public static RaoResultImpl read(InputStream jsonStream) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            return objectMapper.readerFor(RaoResultImpl.class).readValue(jsonStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @param result RaoResultImpl that is written into the jsonFile
     * @param jsonFile Path where the RaoResultImpl is written
     */
    public static void write(RaoResultImpl result, Path jsonFile) {
        Objects.requireNonNull(jsonFile);

        try (OutputStream outputStream = Files.newOutputStream(jsonFile)) {
            write(result, outputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @param result RaoResultImpl that is written into the OutputStream
     * @param outputStream OutputStream where the RaoResultImpl is written
     */
    public static void write(RaoResultImpl result, OutputStream outputStream) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(outputStream, result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Low level deserialization method, to be used for instance for updating rao results nested in another object.
     * @param parser JsonParser containing the RaoResultImpl data
     * @param context DeserializationContext used for the deserialization
     * @param raoResult RaoResultImpl that will contain the data initially in the JsonParser
     * @return RaoResultImpl object updated while deserialization
     * @throws IOException when an unknown field is found
     */
    public static RaoResultImpl deserialize(JsonParser parser, DeserializationContext context, RaoResultImpl raoResult) throws IOException {
        return new RaoResultDeserializer().deserialize(parser, context, raoResult);
    }

    /**
     * Low level deserialization method, to be used for instance for updating rao results nested in another object.
     * @param parser JsonParser containing the RaoResultImpl data
     * @param context DeserializationContext used for the deserialization
     * @return RaoResultImpl object updated while deserialization
     * @throws IOException when an unknown field is found
     */
    public static RaoResultImpl deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return new RaoResultDeserializer().deserialize(parser, context);
    }

    /**
     * Low level serialization method, to be used for instance for writing rao results nested in another object.
     * @param raoResult RaoResultImpl containing what needs to be serialized
     * @param jsonGenerator JsonGenerator used for the serialization
     * @param serializerProvider SerializerProvider used for the serialization
     * @throws IOException if the serialization fails
     */
    public static void serialize(RaoResultImpl raoResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        new RaoResultSerializer().serialize(raoResult, jsonGenerator, serializerProvider);
    }

    private static ObjectMapper createObjectMapper() {
        return JsonUtil.createObjectMapper()
            .registerModule(new RaoResultJsonModule());
    }
}
