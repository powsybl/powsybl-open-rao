/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.flowbased_domain.json;

import com.powsybl.commons.AbstractConverterTest;
import com.farao_community.farao.data.flowbased_domain.DataDomain;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class JsonFlowbasedDomainTest extends AbstractConverterTest {

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

        assertNotNull(flowbasedDomain.getDataPreContingency().findMonitoredBranchbyId("FLOWBASED_DATA_DOMAIN_BRANCH_1"));
        assertNull(flowbasedDomain.getDataPreContingency().findMonitoredBranchbyId("FLOWBASED_DATA_DOMAIN_BRANCH_2"));

        assertNotNull(flowbasedDomain.getDataPreContingency().findMonitoredBranchbyId("FLOWBASED_DATA_DOMAIN_BRANCH_1").findPtdfByCountry("France"));
        assertNull(flowbasedDomain.getDataPreContingency().findMonitoredBranchbyId("FLOWBASED_DATA_DOMAIN_BRANCH_1").findPtdfByCountry("Austria"));
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
}
