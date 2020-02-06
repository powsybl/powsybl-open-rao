/*
 * Copyright (c) 20, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.ComplexContingency;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.SimpleState;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CracImportExportTest {

    @Test
    public void cracTest() {
        SimpleCrac simpleCrac = new SimpleCrac("cracId");

        State preventiveState = new SimpleState(Optional.empty(), new Instant("N", 0));
        Contingency contingency = new ComplexContingency("contingencyId", Collections.singleton(new NetworkElement("neId")));
        State postContingencyState = new SimpleState(Optional.of(contingency), new Instant("postContingencyId", 5));

        simpleCrac.addState(preventiveState);
        simpleCrac.addState(postContingencyState);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonExport jsonExport = new JsonExport();
        jsonExport.exportCrac(simpleCrac, outputStream);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
            JsonImport jsonImport = new JsonImport();
            Crac transformedCrac = jsonImport.importCrac(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
