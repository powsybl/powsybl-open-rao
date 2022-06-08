/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter;

import com.farao_community.farao.data.cne_exporter_commons.CneExporterParameters;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreator;
import com.farao_community.farao.data.crac_creation.creator.cim.importer.CimCracImporter;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SweCneTest {
    private Crac crac;
    private CracCreationContext cracCreationContext;
    private Network network;
    private RaoResult raoResult;

    @Before
    public void setUp() {
        network = Importers.loadNetwork(new File(SweCneTest.class.getResource("/TestCase16NodesWith2Hvdc.xiidm").getFile()).toString());
        InputStream is = getClass().getResourceAsStream("/CIM_CRAC.xml");
        CimCracImporter cracImporter = new CimCracImporter();
        CimCrac cimCrac = cracImporter.importNativeCrac(is);
        CimCracCreator cimCracCreator = new CimCracCreator();
        cracCreationContext = cimCracCreator.createCrac(cimCrac, network, OffsetDateTime.of(2021, 4, 2, 12, 30, 0, 0, ZoneOffset.UTC), new CracCreationParameters());
        crac = cracCreationContext.getCrac();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(SweCneTest.class.getResource("/RaoResult.json").getFile());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        raoResult = new RaoResultImporter().importRaoResult(inputStream, crac);
    }

    @Test
    public void testExport() {
        CneExporterParameters params = new CneExporterParameters(
            "documentId", 3, "domainId", CneExporterParameters.ProcessType.DAY_AHEAD_CC,
            "senderId", CneExporterParameters.RoleType.REGIONAL_SECURITY_COORDINATOR,
            "receiverId", CneExporterParameters.RoleType.CAPACITY_COORDINATOR,
            "2021-04-02T12:00:00Z/2021-04-02T13:00:00Z");
        OutputStream outputStream = new ByteArrayOutputStream();
        new SweCneExporter().exportCne(crac, network, (CimCracCreationContext) cracCreationContext, raoResult, new RaoParameters(), params, outputStream);
        String output = outputStream.toString();
        String expected = "";
        try {
            InputStream inputStream = new FileInputStream(SweCneTest.class.getResource("/SweCNE.xml").getFile());
            expected = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Assert.fail();
        }
        checkCneEquality(expected, output);
    }

    private void checkCneEquality(String expected, String output) {
        String[] splitExpected = expected.split("\n");
        String[] splitOutput = output.split("\n");
        if (splitExpected.length != splitOutput.length) {
            Assert.fail("Generated string has wrong number of lines");
        }
        for (int i = 0; i < splitExpected.length; i++) {
            if (!splitExpected[i].equals(splitOutput[i]) && !splitExpected[i].contains("mRID") && !splitExpected[i].contains("createdDateTime")) {
                 Assert.fail(String.format("Difference at line %d: \"%s\" instead of \"%s\"", i, splitOutput[i], splitExpected[i]));
            }
        }
    }
}
