package com.farao_community.farao.rao_api.json;

import com.farao_community.farao.rao_api.RaoResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class JsonRaoResult extends SimpleModule {
    /**
     * Reads result from a JSON file (will NOT rely on platform config).
     */
    public static RaoResult read(Path jsonFile) {
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
    public static RaoResult read(InputStream jsonStream) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readerFor(RaoResult.class).readValue(jsonStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes result as JSON to a file.
     */
    public static void write(RaoResult result, Path jsonFile) {
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
    public static void write(RaoResult result, OutputStream outputStream) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(outputStream, result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
