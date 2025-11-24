/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.json;

import com.powsybl.commons.report.ReportNode;
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
     * @param reportNode Parent reportNode.
     * @return parameters from a JSON file (will NOT rely on platform config).
     */
    public static RaoParameters read(final Path jsonFile, final ReportNode reportNode) {
        return update(new RaoParameters(reportNode), jsonFile, reportNode);
    }

    /**
     * @param jsonStream InputStream where the RaoParameters are
     * @param reportNode Parent reportNode.
     * @return  parameters from a JSON file (will NOT rely on platform config).
     */
    public static RaoParameters read(final InputStream jsonStream, final ReportNode reportNode) {
        return update(new RaoParameters(reportNode), jsonStream, reportNode);
    }

    /**
     * @param parameters RaoParameters containing original parameters
     * @param jsonFile Path containing parameters to update
     * @param reportNode Parent reportNode.
     * @return parameters updated with the ones found by reading the content of jsonFile.
     */
    public static RaoParameters update(final RaoParameters parameters, final Path jsonFile, final ReportNode reportNode) {
        Objects.requireNonNull(jsonFile);

        try (InputStream is = Files.newInputStream(jsonFile)) {
            return update(parameters, is, reportNode);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @param parameters RaoParameters containing original parameters
     * @param jsonStream InputStream containing parameters to update
     * @param reportNode Parent reportNode.
     * @return parameters updated with the ones found by reading the content of jsonStream.
     */
    public static RaoParameters update(final RaoParameters parameters, final InputStream jsonStream, final ReportNode reportNode) {
        try {
            ObjectMapper objectMapper = createObjectMapper(reportNode);
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
     * @param reportNode Parent reportNode.
     */
    public static void write(final RaoParameters parameters, final Path jsonFile, final ReportNode reportNode) {
        Objects.requireNonNull(jsonFile);

        try (OutputStream outputStream = Files.newOutputStream(jsonFile)) {
            write(parameters, outputStream, reportNode);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes parameters as JSON to an OutputStream
     *
     * @param parameters RaoParameters containing the parameters that will be exported to an OutputStream
     * @param outputStream OutputStream where the parameters will be exported
     * @param reportNode Parent reportNode.
     */
    public static void write(final RaoParameters parameters, final OutputStream outputStream, final ReportNode reportNode) {
        try {
            ObjectMapper objectMapper = createObjectMapper(reportNode);
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
     * @param reportNode Parent reportNode.
     * @return RaoParameters object updated with the content of the JsonParser
     * @throws IOException when an unexpected field is found
     */
    public static RaoParameters deserialize(final JsonParser parser, final DeserializationContext context, final RaoParameters parameters, final ReportNode reportNode) throws IOException {
        return new RaoParametersDeserializer(reportNode).deserialize(parser, context, parameters);
    }

    /**
     * Low level deserialization method, to be used for instance for updating rao parameters nested in another object.
     *
     * @param parser JsonParser of a file containing a representation of RaoParameters
     * @param context DeserializationContext used in the deserialization
     * @param reportNode Parent reportNode.
     * @return RaoParameters object representing the content of the JsonParser
     * @throws IOException when an unexpected field is found
     */
    public static RaoParameters deserialize(final JsonParser parser, final DeserializationContext context, final ReportNode reportNode) throws IOException {
        return new RaoParametersDeserializer(reportNode).deserialize(parser, context);
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

    private static ObjectMapper createObjectMapper(final ReportNode reportNode) {
        return JsonUtil.createObjectMapper()
                .registerModule(new RaoParametersJsonModule(reportNode))
                .registerModule(new SensitivityJsonModule());
    }
}
