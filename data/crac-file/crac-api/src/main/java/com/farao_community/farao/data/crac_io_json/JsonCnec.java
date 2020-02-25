package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_api.Cnec;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public final class JsonCnec {

    /**
     * A configuration loader interface for the RaoComputationResult extensions loaded
     *
     * @param <E> The extension class
     */
    public interface ExtensionSerializer<E extends Extension<Cnec>> extends ExtensionJsonSerializer<Cnec, E> {
    }

    /**
     * Lazily initialized list of extension serializers.
     */
    private static final Supplier<ExtensionProviders<ExtensionSerializer>> SUPPLIER =
        Suppliers.memoize(() -> ExtensionProviders.createProvider(ExtensionSerializer.class, "rao-computation-result"));

    /**
     * Gets the known extension serializers.
     */
    public static ExtensionProviders<ExtensionSerializer> getExtensionSerializers() {
        return SUPPLIER.get();
    }

    private JsonCnec() {
        throw new IllegalStateException("Utility class");
    }


    /**
     * Reads result from a JSON file (will NOT rely on platform config).
     */
    public static Cnec read(Path jsonFile) {
        Objects.requireNonNull(jsonFile);

        try (InputStream is = Files.newInputStream(jsonFile)) {
            return read(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads result from a JSON file (will NOT rely on platform config).
     */
    public static Cnec read(InputStream jsonStream) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            return objectMapper.readerFor(Cnec.class).readValue(jsonStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes result as JSON to a file.
     */
    public static void write(Cnec result, Path jsonFile) {
        Objects.requireNonNull(jsonFile);

        try (OutputStream outputStream = Files.newOutputStream(jsonFile)) {
            write(result, outputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes result as JSON to an output stream.
     */
    public static void write(Cnec result, OutputStream outputStream) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(outputStream, result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ObjectMapper createObjectMapper() {
        return JsonUtil.createObjectMapper()
            .registerModule(new CnecJsonModule());
    }

}
