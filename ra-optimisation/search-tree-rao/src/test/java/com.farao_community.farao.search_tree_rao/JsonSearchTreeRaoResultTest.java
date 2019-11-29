/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.ra_optimisation.json.JsonRaoComputationResult;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.Assert.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class JsonSearchTreeRaoResultTest {

    private RaoComputationResult result;
    private SearchTreeRaoResult resultExtension; //searchTreeRaoResult as an extension of RaoComputationResult

    @Test
    public void testImporter() throws IOException {

        resultExtension = SearchTreeRaoResultImporters.importSearchTreeRaoResult(getClass().getResourceAsStream("/search_tree_rao_result.json"));
        result = resultExtension.getExtendable();

        assertNotNull(resultExtension);
        assertEquals("SECURE", resultExtension.getComputationStatus().toString());
        assertEquals("NO_COMPUTATION", resultExtension.getStopCriterion().toString());
        assertEquals(2, result.getPreContingencyResult().getMonitoredBranchResults().size());
        assertEquals("BRANCH_1", result.getPreContingencyResult().getMonitoredBranchResults().get(0).getBranchId());
    }

    @Test
    public void testExporter() throws IOException {
        //use JsonRaoComputationResult.read as import
        result = JsonRaoComputationResult.read(getClass().getResourceAsStream("/search_tree_rao_result.json"));
        resultExtension = result.getExtension(SearchTreeRaoResult.class);

        OutputStream os = new ByteArrayOutputStream();
        SearchTreeRaoResultExporters.exportSearchTreeRaoResult(resultExtension, "Json", os);
        assertTrue(os.toString().contains("SUCCESS"));
    }

}
