/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.flowbased_domain.json;

import com.farao_community.farao.data.flowbased_domain.DataGlskFactors;
import com.farao_community.farao.data.flowbased_domain.DataMonitoredBranch;
import com.farao_community.farao.data.flowbased_domain.DataPostContingency;
import com.powsybl.commons.AbstractConverterTest;
import com.farao_community.farao.data.flowbased_domain.DataDomain;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class JsonFlowbasedDomainTest extends AbstractConverterTest {

    private static final double EPSILON = 1e-3;

    private static DataDomain create() {
        return JsonFlowbasedDomain.read(JsonFlowbasedDomainTest.class.getResourceAsStream("/dataDomain.json"));
    }

    private static DataDomain read(Path jsonFile) {
        Objects.requireNonNull(jsonFile);

        try (InputStream is = Files.newInputStream(jsonFile)) {
            return JsonFlowbasedDomain.read(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(DataDomain results, Path jsonFile) {
        Objects.requireNonNull(results);
        Objects.requireNonNull(jsonFile);

        try (OutputStream os = Files.newOutputStream(jsonFile)) {
            JsonFlowbasedDomain.write(results, os);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    public void roundTripTest() throws IOException {
        roundTripTest(create(), JsonFlowbasedDomainTest::write, JsonFlowbasedDomainTest::read, "/dataDomain.json");
    }

    @Test
    public void testUtilityMethods() {
        DataDomain flowbasedDomain = JsonFlowbasedDomainTest.create();

        assertNotNull(flowbasedDomain.getDataPreContingency().findMonitoredBranchById("FLOWBASED_DATA_DOMAIN_BRANCH_1"));
        assertNull(flowbasedDomain.getDataPreContingency().findMonitoredBranchById("FLOWBASED_DATA_DOMAIN_BRANCH_2"));

        assertNotNull(flowbasedDomain.getDataPreContingency().findMonitoredBranchById("FLOWBASED_DATA_DOMAIN_BRANCH_1").findPtdfByCountry("France"));
        assertNull(flowbasedDomain.getDataPreContingency().findMonitoredBranchById("FLOWBASED_DATA_DOMAIN_BRANCH_1").findPtdfByCountry("Austria"));
    }

    @Test
    public void testExceptionCases() {
        try {
            JsonFlowbasedDomain.read(getClass().getResourceAsStream("/notExistingFile.json"));
            fail();
        } catch (Throwable e) {
            // Should throw
        }
    }

    @Test
    public void testGetters() {
        DataDomain flowbasedDomain = JsonFlowbasedDomainTest.create();
        assertEquals("FLOWBASED_DATA_DOMAIN_ID", flowbasedDomain.getId());
        assertEquals("This is an example of Flow-based data domain inputs for FARAO", flowbasedDomain.getName());
        assertEquals("JSON", flowbasedDomain.getSourceFormat());
        assertEquals("This is an example of Flow-based inputs for FARAO", flowbasedDomain.getDescription());

        assertNotNull(flowbasedDomain.getDataPreContingency());
        assertEquals(1, flowbasedDomain.getDataPreContingency().getDataMonitoredBranches().size());
        DataMonitoredBranch preventiveBranch = flowbasedDomain.getDataPreContingency().getDataMonitoredBranches().get(0);
        assertEquals("FLOWBASED_DATA_DOMAIN_BRANCH_1", preventiveBranch.getId());
        assertEquals("France-Germany interconnector", preventiveBranch.getName());
        assertEquals("FFR2AA1  DDE3AA1  1", preventiveBranch.getBranchId());
        assertEquals(2300., preventiveBranch.getFmax(), EPSILON);
        assertEquals(123456., preventiveBranch.getFref(), EPSILON);
        assertEquals(5, preventiveBranch.getPtdfList().size());

        assertEquals(1, flowbasedDomain.getDataPostContingency().size());
        DataPostContingency postContingency = flowbasedDomain.getDataPostContingency().get(0);
        assertEquals("CONTINGENCY", postContingency.getContingencyId());
        assertEquals(1, postContingency.getDataMonitoredBranches().size());
        DataMonitoredBranch curativeBranch = postContingency.getDataMonitoredBranches().get(0);
        assertEquals("FLOWBASED_DATA_DOMAIN_BRANCH_N_1_1", curativeBranch.getId());
        assertEquals("France-Germany interconnector", curativeBranch.getName());
        assertEquals("FFR2AA1  DDE3AA1  1", curativeBranch.getBranchId());
        assertEquals(2300., curativeBranch.getFmax(), EPSILON);
        assertEquals(1234567., curativeBranch.getFref(), EPSILON);
        assertEquals(5, curativeBranch.getPtdfList().size());

        assertEquals(3, flowbasedDomain.getGlskData().size());
        DataGlskFactors dataGlskFactors = flowbasedDomain.getGlskData().get(0);
        assertEquals("France", dataGlskFactors.getAreaId());
        assertEquals(4, dataGlskFactors.getGlskFactors().size());
        Map<String, Float> glskMap = dataGlskFactors.getGlskFactors();
        assertEquals(0.2, glskMap.get("FR1 _generator"), 0.001);
    }
}
