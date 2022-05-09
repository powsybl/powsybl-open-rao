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
import com.farao_community.farao.data.crac_creation.creator.api.parameters.RangeActionGroup;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.CnecCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.MeasurementCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.MonitoredSeriesCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.contingency.CimContingencyCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.remedial_action.RemedialActionSeriesCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.parameters.CimCracCreationParameters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import com.farao_community.farao.data.crac_creation.creator.cim.importer.CimCracImporter;
import org.checkerframework.checker.units.qual.C;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    private void assertRemedialActionNotImported(String id, ImportStatus importStatus) {
        RemedialActionSeriesCreationContext remedialActionSeriesCreationContext = cracCreationContext.getRemedialActionSeriesCreationContexts(id);
        assertNotNull(remedialActionSeriesCreationContext);
        assertFalse(remedialActionSeriesCreationContext.isImported());
        assertEquals(importStatus, remedialActionSeriesCreationContext.getImportStatus());
    }

    private void assertPstRangeActionImported(String id, String networkElement, boolean isAltered) {
        RemedialActionSeriesCreationContext remedialActionSeriesCreationContext = cracCreationContext.getRemedialActionSeriesCreationContexts(id);
        assertNotNull(remedialActionSeriesCreationContext);
        assertTrue(remedialActionSeriesCreationContext.isImported());
        assertEquals(isAltered, remedialActionSeriesCreationContext.isAltered());
        assertNotNull(importedCrac.getPstRangeAction(id));
        String actualNetworkElement = importedCrac.getPstRangeAction(id).getNetworkElement().toString();
        assertEquals(networkElement, actualNetworkElement);
    }

    private void assertNetworkActionImported(String id, Set<String> networkElements, boolean isAltered) {
        RemedialActionSeriesCreationContext remedialActionSeriesCreationContext = cracCreationContext.getRemedialActionSeriesCreationContexts(id);
        assertNotNull(remedialActionSeriesCreationContext);
        assertTrue(remedialActionSeriesCreationContext.isImported());
        assertEquals(isAltered, remedialActionSeriesCreationContext.isAltered());
        assertNotNull(importedCrac.getNetworkAction(id));
        Set<String> actualNetworkElements = importedCrac.getNetworkAction(id).getNetworkElements().stream().map(NetworkElement::getId).collect(Collectors.toSet());
        assertEquals(networkElements, actualNetworkElements);
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
        assertPstRangeActionImported("PRA_1", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", false);
        assertRemedialActionNotImported("RA-Series-2", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("RA-Series-3", NOT_YET_HANDLED_BY_FARAO);
        assertRemedialActionNotImported("RA-Series-4", NOT_YET_HANDLED_BY_FARAO);
        assertRemedialActionNotImported("PRA_5", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_6", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_7", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_8", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_9", INCOMPLETE_DATA);
        assertRemedialActionNotImported("PRA_10", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_11", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_12", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_13", INCOMPLETE_DATA);
        assertRemedialActionNotImported("PRA_14", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_15", NOT_YET_HANDLED_BY_FARAO);
        assertRemedialActionNotImported("PRA_16", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_17", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_18", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_19", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_20", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_21", ELEMENT_NOT_FOUND_IN_NETWORK);
        assertPstRangeActionImported("PRA_22", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", true);
        assertEquals(2, importedCrac.getPstRangeActions().size());
    }

    @Test
    public void testImportNetworkActions() {
        setUp("/cracs/CIM_21_4_1.xml", null);
        assertNetworkActionImported("PRA_1", Set.of("_e8a7eaec-51d6-4571-b3d9-c36d52073c33", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", "_b94318f6-6d24-4f56-96b9-df2531ad6543", "_2184f365-8cd5-4b5d-8a28-9d68603bb6a4"), false);
        // Pst Setpoint
        assertRemedialActionNotImported("PRA_2", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_3", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_4", INCOMPLETE_DATA);
        assertRemedialActionNotImported("PRA_5", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_6", ELEMENT_NOT_FOUND_IN_NETWORK);
        // Injection Setpoint
        assertNetworkActionImported("PRA_7", Set.of("_1dc9afba-23b5-41a0-8540-b479ed8baf4b", "_2844585c-0d35-488d-a449-685bcd57afbf"), false);
        assertRemedialActionNotImported("PRA_8", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_9", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_10", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_11", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_12", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_13", INCOMPLETE_DATA);
        assertRemedialActionNotImported("PRA_14", INCOMPLETE_DATA);
        assertRemedialActionNotImported("PRA_15", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_16", ELEMENT_NOT_FOUND_IN_NETWORK);
        // Topological
        assertNetworkActionImported("PRA_17", Set.of("_ffbabc27-1ccd-4fdc-b037-e341706c8d29", "_b58bf21a-096a-4dae-9a01-3f03b60c24c7", "_f04ec73d-b94a-4b7e-a3d6-b1234fc37385_SW_fict", "_5a094c9f-0af5-48dc-94e9-89c6c220023c"), false);
        assertRemedialActionNotImported("PRA_18", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_19", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_20", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_21", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_22", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_23", INCOMPLETE_DATA);
        assertRemedialActionNotImported("PRA_24", ELEMENT_NOT_FOUND_IN_NETWORK);
        assertRemedialActionNotImported("PRA_25", NOT_YET_HANDLED_BY_FARAO);
        // Mix
        assertNetworkActionImported("PRA_26", Set.of("_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", "_2844585c-0d35-488d-a449-685bcd57afbf", "_ffbabc27-1ccd-4fdc-b037-e341706c8d29"), false);
        assertRemedialActionNotImported("PRA_27", INCONSISTENCY_IN_DATA);
    }

    @Test
    public void testImportAlignedRangeActions() {
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters = Mockito.spy(cracCreationParameters);
        CimCracCreationParameters  cimCracCreationParameters = Mockito.mock(CimCracCreationParameters.class);
        Mockito.when(cracCreationParameters.getExtension(CimCracCreationParameters.class)).thenReturn(cimCracCreationParameters);
        List<RangeActionGroup> rangeActionGroups = new ArrayList<>();
        rangeActionGroups.add(new RangeActionGroup(List.of("PRA_1", "PRA_22")));
        Mockito.when(cimCracCreationParameters.getRangeActionGroups()).thenReturn(rangeActionGroups);

        InputStream is = getClass().getResourceAsStream("/cracs/CIM_21_3_1.xml");
        CimCracImporter cracImporter = new CimCracImporter();
        CimCrac cimCrac = cracImporter.importNativeCrac(is);
        CimCracCreator cimCracCreator = new CimCracCreator();
        cracCreationContext = cimCracCreator.createCrac(cimCrac, network, null, cracCreationParameters);
        importedCrac = cracCreationContext.getCrac();
        assertPstRangeActionImported("PRA_1", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", false);
        assertPstRangeActionImported("PRA_22", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", true);
        assertEquals(2, importedCrac.getPstRangeActions().size());
        assertTrue(importedCrac.getPstRangeAction("PRA_1").getGroupId().isPresent());
        assertTrue(importedCrac.getPstRangeAction("PRA_22").getGroupId().isPresent());
        assertEquals("PRA_1 + PRA_22", importedCrac.getPstRangeAction("PRA_1").getGroupId().get());
        assertEquals("PRA_1 + PRA_22", importedCrac.getPstRangeAction("PRA_22").getGroupId().get());
    }
}
