/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class JsonSearchTreeRaoResultExporterTest {

    private SearchTreeRaoResult result;

    @Before
    public void setUp() {
        result = SearchTreeRaoResultExampleBuilder.buildResult();
    }

    @Test
    public void testExport() throws IOException {
        OutputStream os = new ByteArrayOutputStream();
        SearchTreeRaoResultExporters.exportSearchTreeRaoResult(result, "Json", os);
        Assert.assertTrue(os.toString().contains("NO_COMPUTATION"));
    }

}
