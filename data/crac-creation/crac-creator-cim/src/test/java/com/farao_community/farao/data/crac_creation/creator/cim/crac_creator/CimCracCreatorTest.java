/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import com.farao_community.farao.data.crac_creation.creator.cim.importer.CimCracImporter;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_creation.creator.api.ImportStatus.*;
import static org.junit.Assert.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class CimCracCreatorTest {
    private Crac importedCrac;
    private CimCracCreationContext cracCreationContext;
    private static Network network;

    @BeforeClass
    public static void loadNetwork() {
        network = Importers.loadNetwork(new File(CimCracCreatorTest.class.getResource("/networks/MicroGrid.zip").getFile()).toString());
    }

    private void setUp(String fileName, OffsetDateTime parametrableOffsetDateTime) {
        InputStream is = getClass().getResourceAsStream(fileName);
        CimCracImporter cracImporter = new CimCracImporter();
        CimCrac cimCrac = cracImporter.importNativeCrac(is);
        CimCracCreator cimCracCreator = new CimCracCreator();
        cracCreationContext = cimCracCreator.createCrac(cimCrac, network, parametrableOffsetDateTime, new CracCreationParameters());
        importedCrac = cracCreationContext.getCrac();
    }

    private void assertContingencyNotImported(String name, ImportStatus importStatus) {
        CimContingencyCreationContext context = cracCreationContext.getContingencyCreationContext(name);
        assertNotNull(context);
        assertFalse(context.isImported());
        assertEquals(importStatus, context.getImportStatus());
    }

    private void assertContingencyImported(String id, Set<String> networkElements, boolean isAltered) {
        CimContingencyCreationContext context = cracCreationContext.getContingencyCreationContext(id);
        assertNotNull(context);
        assertTrue(context.isImported());
        assertEquals(isAltered, context.isAltered());
        if (isAltered) {
            assertNotNull(context.getImportStatusDetail());
        } else {
            assertNull(context.getImportStatusDetail());
        }
        assertEquals(id, context.getCreatedContingencyId());
        assertNotNull(importedCrac.getContingency(id));
        Set<String> actualNetworkElements = importedCrac.getContingency(id).getNetworkElements().stream().map(NetworkElement::getId).collect(Collectors.toSet());
        assertEquals(networkElements, actualNetworkElements);
    }

    private void assertCnecNotImported(String monitoredSeriesId, ImportStatus importStatus) {
        MonitoredSeriesCreationContext monitoredSeriesCreationContext = cracCreationContext.getMonitoredSeriesCreationContext(monitoredSeriesId);
        assertNotNull(monitoredSeriesCreationContext);
        assertFalse(monitoredSeriesCreationContext.isImported());
        assertEquals(importStatus, monitoredSeriesCreationContext.getImportStatus());
    }

    private void assertCnecImported(String monitoredSeriesId, Set<String> expectedCnecIds) {
        MonitoredSeriesCreationContext monitoredSeriesCreationContext = cracCreationContext.getMonitoredSeriesCreationContext(monitoredSeriesId);
        assertNotNull(monitoredSeriesCreationContext);
        Set<String> importedCnecIds = new HashSet<>();
        monitoredSeriesCreationContext.getMeasurementCreationContexts().stream()
            .filter(MeasurementCreationContext::isImported)
            .forEach(measurementCreationContext ->
                measurementCreationContext.getCnecCreationContexts().values().stream()
                    .filter(CnecCreationContext::isImported)
                    .forEach(cnecCreationContext ->
                        importedCnecIds.add(cnecCreationContext.getCreatedCnecId())));

        assertEquals(expectedCnecIds, importedCnecIds);
    }

    private void assertPstNotImported(String id, ImportStatus importStatus) {
        RemedialActionSeriesCreationContext remedialActionSeriesCreationContext = cracCreationContext.getRemedialActionSeriesCreationContexts(id);
        assertNotNull(remedialActionSeriesCreationContext);
        assertFalse(remedialActionSeriesCreationContext.isImported());
        assertEquals(importStatus, remedialActionSeriesCreationContext.getImportStatus());
    }

    private void assertPstImported(String id, String networkElement, boolean isAltered) {
        RemedialActionSeriesCreationContext remedialActionSeriesCreationContext = cracCreationContext.getRemedialActionSeriesCreationContexts(id);
        assertNotNull(remedialActionSeriesCreationContext);
        assertTrue(remedialActionSeriesCreationContext.isImported());
        assertEquals(isAltered, remedialActionSeriesCreationContext.isAltered());
        assertNotNull(importedCrac.getPstRangeAction(id));
        String actualNetworkElement = importedCrac.getPstRangeAction(id).getNetworkElement().toString();
        assertEquals(networkElement, actualNetworkElement);
    }

    @Test
    public void cracCreationSuccessfulNullTime() {
        setUp("/cracs/CIM_21_1_1.xml", null);
        assertTrue(cracCreationContext.isCreationSuccessful());
    }

    @Test
    public void cracCreationFailureWrongTime() {
        setUp("/cracs/CIM_21_1_1.xml", OffsetDateTime.parse("2020-04-01T22:00Z"));
        assertFalse(cracCreationContext.isCreationSuccessful());
    }

    @Test
    public void cracCreationSuccessfulRightTime() {
        setUp("/cracs/CIM_21_1_1.xml", OffsetDateTime.parse("2021-04-01T22:00Z"));
        assertTrue(cracCreationContext.isCreationSuccessful());
    }

    @Test
    public void testImportContingencies() {
        setUp("/cracs/CIM_21_1_1.xml", null);

        assertEquals(3, importedCrac.getContingencies().size());
        assertContingencyImported("Co-1", Set.of("_ffbabc27-1ccd-4fdc-b037-e341706c8d29"), false);
        assertContingencyImported("Co-2", Set.of("_b18cd1aa-7808-49b9-a7cf-605eaf07b006 + _e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc", "_df16b3dd-c905-4a6f-84ee-f067be86f5da"), false);
        assertContingencyImported("Co-3", Set.of("_b58bf21a-096a-4dae-9a01-3f03b60c24c7"), true);

        assertContingencyNotImported("Co-4", ELEMENT_NOT_FOUND_IN_NETWORK);
        assertContingencyNotImported("Co-5", INCOMPLETE_DATA);

        assertEquals(4, cracCreationContext.getCreationReport().getReport().size()); // 2 fake contingencies, 1 altered, null offsetDateTime
    }

    @Test
    public void testImportFakeCnecs() {
        setUp("/cracs/CIM_21_2_1.xml", null);
        assertCnecNotImported("CNEC-2", ELEMENT_NOT_FOUND_IN_NETWORK);
        assertEquals(10, importedCrac.getFlowCnecs().size());
        assertCnecImported("CNEC-4",
                Set.of("CNEC-4 - preventive",
                        "CNEC-4 - Co-1 - curative",
                        "CNEC-4 - Co-2 - curative"));
    }

    @Test
    public void testImportPstRangeActions() {
        setUp("/cracs/CIM_21_3_1.xml", null);
        assertPstImported("PRA_1", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", false);
        assertPstNotImported("RA-Series-2", INCONSISTENCY_IN_DATA);
        assertPstNotImported("RA-Series-3", NOT_YET_HANDLED_BY_FARAO);
        assertPstNotImported("RA-Series-4", NOT_YET_HANDLED_BY_FARAO);
        assertPstNotImported("PRA_5", INCONSISTENCY_IN_DATA);
        assertPstNotImported("PRA_6", INCONSISTENCY_IN_DATA);
        assertPstNotImported("PRA_7", INCONSISTENCY_IN_DATA);
        assertPstNotImported("PRA_8", INCONSISTENCY_IN_DATA);
        assertPstNotImported("PRA_9", INCOMPLETE_DATA);
        assertPstNotImported("PRA_10", INCONSISTENCY_IN_DATA);
        assertPstNotImported("PRA_11", INCONSISTENCY_IN_DATA);
        assertPstNotImported("PRA_12", INCONSISTENCY_IN_DATA);
        assertPstNotImported("PRA_13", INCOMPLETE_DATA);
        assertPstNotImported("PRA_14", INCONSISTENCY_IN_DATA);
        assertPstNotImported("PRA_15", NOT_YET_HANDLED_BY_FARAO);
        assertPstNotImported("PRA_16", INCONSISTENCY_IN_DATA);
        assertPstNotImported("PRA_17", INCONSISTENCY_IN_DATA);
        assertPstNotImported("PRA_18", INCONSISTENCY_IN_DATA);
        assertPstNotImported("PRA_19", INCONSISTENCY_IN_DATA);
        assertPstNotImported("PRA_20", INCONSISTENCY_IN_DATA);
        assertPstNotImported("PRA_21", ELEMENT_NOT_FOUND_IN_NETWORK);
        assertPstImported("PRA_22", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", true);
        assertEquals(2, importedCrac.getPstRangeActions().size());
    }
}
