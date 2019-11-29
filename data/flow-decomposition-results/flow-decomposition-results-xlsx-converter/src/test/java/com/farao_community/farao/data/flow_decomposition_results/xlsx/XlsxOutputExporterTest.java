/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.flow_decomposition_results.xlsx;

import com.farao_community.farao.data.flow_decomposition_results.FlowDecompositionResults;
import com.farao_community.farao.data.flow_decomposition_results.json.JsonFlowDecompositionResults;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class XlsxOutputExporterTest {
    private FlowDecompositionResults referenceJsonResults;

    @Before
    public void setUp() {
        referenceJsonResults = JsonFlowDecompositionResults.read(XlsxOutputExporterTest.class.getResourceAsStream("/flowDecompositionResults.json"));
    }

    @Test
    public void testExportToStream() throws Exception {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            XlsxOutputExporter.exportInStream(os, referenceJsonResults);
        }
    }
}
