/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class RaoResultArchiveManagerTest {
    @Test
    void testWriteArchive() throws IOException {
        Network network1 = Network.read("/network/3Nodes.uct", GlobalRaoResultImplTest.class.getResourceAsStream("/network/3Nodes.uct"));
        Network network2 = Network.read("/network/3Nodes.uct", GlobalRaoResultImplTest.class.getResourceAsStream("/network/3Nodes.uct"));
        Network network3 = Network.read("/network/3Nodes.uct", GlobalRaoResultImplTest.class.getResourceAsStream("/network/3Nodes.uct"));
        Crac crac1 = Crac.read("/crac/crac-redispatching-202502141040.json", GlobalRaoResultImplTest.class.getResourceAsStream("/crac/crac-redispatching-202502141040.json"), network1);
        Crac crac2 = Crac.read("/crac/crac-redispatching-202502141140.json", GlobalRaoResultImplTest.class.getResourceAsStream("/crac/crac-redispatching-202502141140.json"), network2);
        Crac crac3 = Crac.read("/crac/crac-redispatching-202502141240.json", GlobalRaoResultImplTest.class.getResourceAsStream("/crac/crac-redispatching-202502141240.json"), network3);
        RaoResult raoResult1 = RaoResult.read(GlobalRaoResultImplTest.class.getResourceAsStream("/raoResult/raoResult1.json"), crac1);
        RaoResult raoResult2 = RaoResult.read(GlobalRaoResultImplTest.class.getResourceAsStream("/raoResult/raoResult2.json"), crac2);
        RaoResult raoResult3 = RaoResult.read(GlobalRaoResultImplTest.class.getResourceAsStream("/raoResult/raoResult3.json"), crac3);
        OffsetDateTime timestamp1 = OffsetDateTime.of(2025, 2, 14, 10, 40, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp2 = OffsetDateTime.of(2025, 2, 14, 11, 40, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp3 = OffsetDateTime.of(2025, 2, 14, 12, 40, 0, 0, ZoneOffset.UTC);

        GlobalLinearOptimizationResult initialLinearOptimizationResult = Mockito.mock(GlobalLinearOptimizationResult.class);
        Mockito.when(initialLinearOptimizationResult.getFunctionalCost()).thenReturn(0.0);
        Mockito.when(initialLinearOptimizationResult.getVirtualCostNames()).thenReturn(Set.of("min-margin-violation-evaluator", "sensitivity-failure-cost"));
        Mockito.when(initialLinearOptimizationResult.getVirtualCost("min-margin-violation-evaluator")).thenReturn(3333333.33);
        Mockito.when(initialLinearOptimizationResult.getVirtualCost("sensitivity-failure-cost")).thenReturn(0.0);

        GlobalLinearOptimizationResult globalLinearOptimizationResult = Mockito.mock(GlobalLinearOptimizationResult.class);
        Mockito.when(globalLinearOptimizationResult.getFunctionalCost()).thenReturn(65030.0);
        Mockito.when(globalLinearOptimizationResult.getVirtualCostNames()).thenReturn(Set.of("min-margin-violation-evaluator", "sensitivity-failure-cost"));
        Mockito.when(globalLinearOptimizationResult.getVirtualCost("min-margin-violation-evaluator")).thenReturn(0.0);
        Mockito.when(globalLinearOptimizationResult.getVirtualCost("sensitivity-failure-cost")).thenReturn(0.0);

        GlobalRaoResultImpl globalRaoResultToExport = new GlobalRaoResultImpl(initialLinearOptimizationResult, globalLinearOptimizationResult, new TemporalDataImpl<>(Map.of(timestamp1, raoResult1, timestamp2, raoResult2, timestamp3, raoResult3)));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);

        Properties properties = new Properties();
        properties.put("rao-result.export.json.flows-in-amperes", "true");
        properties.put("rao-result.export.json.flows-in-megawatts", "true");

        globalRaoResultToExport.write(zos, new TemporalDataImpl<>(Map.of(timestamp1, crac1, timestamp2, crac2, timestamp3, crac3)), properties);

        byte[] zipBytes = baos.toByteArray();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(zipBytes);
        ZipInputStream zis = new ZipInputStream(byteArrayInputStream);

        Set<String> exportedRaoResults = new HashSet<>();
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            exportedRaoResults.add(entry.getName());
        }

        assertEquals(4, exportedRaoResults.size());
        assertTrue(exportedRaoResults.contains("raoResult_202502141040.json"));
        assertTrue(exportedRaoResults.contains("raoResult_202502141140.json"));
        assertTrue(exportedRaoResults.contains("raoResult_202502141240.json"));
        assertTrue(exportedRaoResults.contains("globalRaoSummary.json"));
    }
}
