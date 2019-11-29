/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.json;

import com.powsybl.commons.AbstractConverterTest;
import com.farao_community.farao.data.crac_file.CracFile;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class JsonCracFileTest extends AbstractConverterTest {

    private static CracFile create(String filePath) throws IOException {
        return JsonCracFile.read(CracFile.class.getResourceAsStream(filePath));
    }

    private static CracFile read(Path jsonFile) {
        Objects.requireNonNull(jsonFile);
        try (InputStream is = Files.newInputStream(jsonFile)) {
            return JsonCracFile.read(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(CracFile cracFile, Path jsonFile) {
        Objects.requireNonNull(cracFile);
        Objects.requireNonNull(jsonFile);

        try (OutputStream os = Files.newOutputStream(jsonFile)) {
            JsonCracFile.write(cracFile, os);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    public void roundTripTest() {
        List<String> filesToTest = Arrays.asList("/cracFileExample.json", "/cracFileExamplePst.json", "/cracFileExampleTopo.json");
        filesToTest.forEach(file -> {
            try {
                roundTripTest(create(file), JsonCracFileTest::write, JsonCracFileTest::read, file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
