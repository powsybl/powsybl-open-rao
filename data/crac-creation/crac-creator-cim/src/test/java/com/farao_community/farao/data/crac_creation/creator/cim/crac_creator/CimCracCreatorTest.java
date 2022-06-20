/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.network_action.*;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.*;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.RangeActionGroup;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.CnecCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.MeasurementCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.MonitoredSeriesCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.contingency.CimContingencyCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.remedial_action.RemedialActionSeriesCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.parameters.CimCracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cim.parameters.RangeActionSpeed;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import com.farao_community.farao.data.crac_creation.creator.cim.importer.CimCracImporter;
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
    private static Network baseNetwork;
    private static Network hvdcNetwork;

    @BeforeClass
    public static void loadNetwork() {
        baseNetwork = Importers.loadNetwork(new File(CimCracCreatorTest.class.getResource("/networks/MicroGrid.zip").getFile()).toString());
    }

    @BeforeClass
    public static void loadHvdcNetwork() {
        hvdcNetwork = Importers.loadNetwork(new File(CimCracCreatorTest.class.getResource("/networks/TestCase16NodesWith2Hvdc.xiidm").getFile()).toString());
    }

    private void setUp(String fileName, Network network, OffsetDateTime parametrableOffsetDateTime, CracCreationParameters cracCreationParameters) {
        InputStream is = getClass().getResourceAsStream(fileName);
        CimCracImporter cracImporter = new CimCracImporter();
        CimCrac cimCrac = cracImporter.importNativeCrac(is);
        CimCracCreator cimCracCreator = new CimCracCreator();
        cracCreationContext = cimCracCreator.createCrac(cimCrac, network, parametrableOffsetDateTime, cracCreationParameters);
        importedCrac = cracCreationContext.getCrac();
    }

    private void setUpWithGroupId(String fileName, Network network, OffsetDateTime parametrableOffsetDateTime, List<List<String>> alignedRangeActions) {
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters = Mockito.spy(cracCreationParameters);
        CimCracCreationParameters  cimCracCreationParameters = Mockito.mock(CimCracCreationParameters.class);
        Mockito.when(cracCreationParameters.getExtension(CimCracCreationParameters.class)).thenReturn(cimCracCreationParameters);
        List<RangeActionGroup> rangeActionGroups = new ArrayList<>();
        alignedRangeActions.forEach(listAlignedRangeActions -> rangeActionGroups.add(new RangeActionGroup(listAlignedRangeActions)));
        Mockito.when(cimCracCreationParameters.getRangeActionGroups()).thenReturn(rangeActionGroups);

        InputStream is = getClass().getResourceAsStream(fileName);
        CimCracImporter cracImporter = new CimCracImporter();
        CimCrac cimCrac = cracImporter.importNativeCrac(is);
        CimCracCreator cimCracCreator = new CimCracCreator();
        cracCreationContext = cimCracCreator.createCrac(cimCrac, network, parametrableOffsetDateTime, cracCreationParameters);
        importedCrac = cracCreationContext.getCrac();
    }

    private void setUpWithSpeed(String fileName, Network network, OffsetDateTime parametrableOffsetDateTime, Set<RangeActionSpeed> rangeActionSpeeds) {
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters = Mockito.spy(cracCreationParameters);
        CimCracCreationParameters  cimCracCreationParameters = Mockito.mock(CimCracCreationParameters.class);
        Mockito.when(cracCreationParameters.getExtension(CimCracCreationParameters.class)).thenReturn(cimCracCreationParameters);
        Mockito.when(cimCracCreationParameters.getRangeActionSpeedSet()).thenReturn(rangeActionSpeeds);
        InputStream is = getClass().getResourceAsStream(fileName);
        CimCracImporter cracImporter = new CimCracImporter();
        CimCrac cimCrac = cracImporter.importNativeCrac(is);
        CimCracCreator cimCracCreator = new CimCracCreator();
        cracCreationContext = cimCracCreator.createCrac(cimCrac, network, parametrableOffsetDateTime, cracCreationParameters);
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

    private void assertHvdcRangeActionImported(String expectedNativeId, Set<String> expectedCreatedIds, Set<String> expectedNetworkElements, boolean isInverted) {
        RemedialActionSeriesCreationContext remedialActionSeriesCreationContext = cracCreationContext.getRemedialActionSeriesCreationContexts(expectedNativeId);
        assertNotNull(remedialActionSeriesCreationContext);
        assertEquals(expectedCreatedIds, remedialActionSeriesCreationContext.getCreatedIds());
        assertTrue(remedialActionSeriesCreationContext.isImported());
        expectedCreatedIds.forEach(createdId -> assertNotNull(importedCrac.getHvdcRangeAction(createdId)));
        Set<String> actualNetworkElements = new HashSet<>();
        expectedCreatedIds.forEach(createdId -> actualNetworkElements.add(importedCrac.getHvdcRangeAction(createdId).getNetworkElement().toString()));
        assertEquals(actualNetworkElements, expectedNetworkElements);
        assertEquals(isInverted, remedialActionSeriesCreationContext.isInverted());
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

    private void assertHasOnFlowConstraintUsageRule(RemedialAction<?> ra, Instant instant, String flowCnecId) {
        assertTrue(
                ra.getUsageRules().stream()
                        .filter(OnFlowConstraint.class::isInstance)
                        .map(OnFlowConstraint.class::cast)
                        .anyMatch(
                                ur -> ur.getInstant().equals(instant)
                                        && ur.getFlowCnec().getId().equals(flowCnecId)
                                        && ur.getUsageMethod().equals(UsageMethod.TO_BE_EVALUATED)
                        ));
    }

    @Test
    public void cracCreationSuccessfulFailureTime() {
        setUp("/cracs/CIM_21_1_1.xml", baseNetwork, null, new CracCreationParameters());
        assertFalse(cracCreationContext.isCreationSuccessful());
    }

    @Test
    public void cracCreationFailureWrongTime() {
        setUp("/cracs/CIM_21_1_1.xml", baseNetwork, OffsetDateTime.parse("2020-04-01T22:00Z"), new CracCreationParameters());
        assertFalse(cracCreationContext.isCreationSuccessful());
    }

    @Test
    public void cracCreationSuccessfulRightTime() {
        setUp("/cracs/CIM_21_1_1.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T22:00Z"), new CracCreationParameters());
        assertTrue(cracCreationContext.isCreationSuccessful());
    }

    @Test
    public void testImportContingencies() {
        setUp("/cracs/CIM_21_1_1.xml", baseNetwork,  OffsetDateTime.parse("2021-04-01T23:00Z"), new CracCreationParameters());

        assertEquals(3, importedCrac.getContingencies().size());
        assertContingencyImported("Co-1", Set.of("_ffbabc27-1ccd-4fdc-b037-e341706c8d29"), false);
        assertContingencyImported("Co-2", Set.of("_b18cd1aa-7808-49b9-a7cf-605eaf07b006 + _e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc", "_df16b3dd-c905-4a6f-84ee-f067be86f5da"), false);
        assertContingencyImported("Co-3", Set.of("_b58bf21a-096a-4dae-9a01-3f03b60c24c7"), true);

        assertContingencyNotImported("Co-4", ELEMENT_NOT_FOUND_IN_NETWORK);
        assertContingencyNotImported("Co-5", INCOMPLETE_DATA);

        assertEquals(3, cracCreationContext.getCreationReport().getReport().size()); // 2 fake contingencies, 1 altered
    }

    @Test
    public void testImportFakeCnecs() {
        setUp("/cracs/CIM_21_2_1.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), new CracCreationParameters());
        assertCnecNotImported("CNEC-2", ELEMENT_NOT_FOUND_IN_NETWORK);
        assertEquals(10, importedCrac.getFlowCnecs().size());
        assertCnecImported("CNEC-4",
                Set.of("CNEC-4 - preventive",
                        "CNEC-4 - Co-1 - curative",
                        "CNEC-4 - Co-2 - curative"));
    }

    @Test
    public void testImportPstRangeActions() {
        setUp("/cracs/CIM_21_3_1.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), new CracCreationParameters());
        assertPstRangeActionImported("PRA_1", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", false);
        assertRemedialActionNotImported("RA-Series-2", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("RA-Series-3", NOT_YET_HANDLED_BY_FARAO);
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
        setUp("/cracs/CIM_21_4_1.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), new CracCreationParameters());
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
    public void testImportHvdcRangeActions() {
        setUpWithSpeed("/cracs/CIM_21_6_1.xml", hvdcNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), Set.of(new RangeActionSpeed("BBE2AA11 FFR3AA11 1", 1), new RangeActionSpeed("BBE2AA12 FFR3AA12 1", 2)));

        // RA-Series-2
        assertRemedialActionNotImported("HVDC-direction21", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC-direction22", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC-direction2", INCONSISTENCY_IN_DATA);

        // RA-Series-3
        assertRemedialActionNotImported("HVDC-direction31", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC-direction32", INCONSISTENCY_IN_DATA);

        // RA-Series-4
        assertRemedialActionNotImported("HVDC-direction41", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC-direction42", INCONSISTENCY_IN_DATA);

        // RA-Series-5
        assertRemedialActionNotImported("HVDC-direction51", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC-direction52", INCONSISTENCY_IN_DATA);

        // RA-Series-6
        assertRemedialActionNotImported("HVDC-direction61", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC-direction62", INCONSISTENCY_IN_DATA);

        // RA-Series-7
        assertRemedialActionNotImported("HVDC-direction71", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC-direction72", INCONSISTENCY_IN_DATA);

        // RA-Series-8
        assertRemedialActionNotImported("HVDC-direction81", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC-direction82", ELEMENT_NOT_FOUND_IN_NETWORK);

        // RA-Series-9
        assertRemedialActionNotImported("HVDC-direction91", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC-direction92", INCONSISTENCY_IN_DATA);

        assertHvdcRangeActionImported("HVDC-direction11", Set.of("HVDC-direction11 + HVDC-direction12 - BBE2AA12 FFR3AA12 1", "HVDC-direction11 + HVDC-direction12 - BBE2AA11 FFR3AA11 1"), Set.of("BBE2AA11 FFR3AA11 1", "BBE2AA12 FFR3AA12 1"), true);
        assertHvdcRangeActionImported("HVDC-direction12", Set.of("HVDC-direction11 + HVDC-direction12 - BBE2AA12 FFR3AA12 1", "HVDC-direction11 + HVDC-direction12 - BBE2AA11 FFR3AA11 1"), Set.of("BBE2AA11 FFR3AA11 1", "BBE2AA12 FFR3AA12 1"), false);
        assertEquals("BBE2AA11 FFR3AA11 1 + BBE2AA12 FFR3AA12 1", importedCrac.getHvdcRangeAction("HVDC-direction11 + HVDC-direction12 - BBE2AA12 FFR3AA12 1").getGroupId().get());
        assertEquals("BBE2AA11 FFR3AA11 1 + BBE2AA12 FFR3AA12 1", importedCrac.getHvdcRangeAction("HVDC-direction11 + HVDC-direction12 - BBE2AA11 FFR3AA11 1").getGroupId().get());
    }

    @Test (expected = FaraoException.class)
    public void testImportKOHvdcRangeActions() {
        setUpWithSpeed("/cracs/CIM_21_6_1.xml", hvdcNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), null);
    }

    @Test
    public void testImportAlignedRangeActions() {
        setUpWithGroupId("/cracs/CIM_21_3_1.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"),  List.of(List.of("PRA_1", "PRA_22")));
        assertPstRangeActionImported("PRA_1", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", false);
        assertPstRangeActionImported("PRA_22", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", true);
        assertEquals(2, importedCrac.getPstRangeActions().size());
        assertTrue(importedCrac.getPstRangeAction("PRA_1").getGroupId().isPresent());
        assertTrue(importedCrac.getPstRangeAction("PRA_22").getGroupId().isPresent());
        assertEquals("PRA_1 + PRA_22", importedCrac.getPstRangeAction("PRA_1").getGroupId().get());
        assertEquals("PRA_1 + PRA_22", importedCrac.getPstRangeAction("PRA_22").getGroupId().get());
    }

    @Test
    public void testImportAlignedRangeActionsGroupIdNull() {
        List<String> groupIds = new ArrayList<>();
        groupIds.add(null);
        setUpWithGroupId("/cracs/CIM_21_3_1.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), List.of(groupIds));
        assertPstRangeActionImported("PRA_1", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", false);
        assertPstRangeActionImported("PRA_22", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", true);
        assertEquals(2, importedCrac.getPstRangeActions().size());
        assertFalse(importedCrac.getPstRangeAction("PRA_1").getGroupId().isPresent());
        assertFalse(importedCrac.getPstRangeAction("PRA_22").getGroupId().isPresent());
    }

    @Test
    public void testImportAlignedRangeActionsGroupIdAlreadyDefined() {
        setUpWithGroupId("/cracs/CIM_21_3_1.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), List.of(List.of("PRA_1", "PRA_22"), List.of("PRA_1")));
        assertRemedialActionNotImported("PRA_1", INCONSISTENCY_IN_DATA);
        assertPstRangeActionImported("PRA_22", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", true);
        assertEquals(1, importedCrac.getPstRangeActions().size());
        assertTrue(importedCrac.getPstRangeAction("PRA_22").getGroupId().isPresent());
        assertEquals("PRA_1 + PRA_22", importedCrac.getPstRangeAction("PRA_22").getGroupId().get());
    }

    @Test
    public void testImportOnFlowConstraintUsageRules() {
        setUpWithSpeed("/cracs/CIM_21_5_1.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), Set.of(new RangeActionSpeed("AUTO_1", 1)));

        // PRA_1
        assertPstRangeActionImported("PRA_1", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", false);
        PstRangeAction pra1 = importedCrac.getPstRangeAction("PRA_1");
        assertEquals(10, pra1.getUsageRules().size());
        assertHasOnFlowConstraintUsageRule(pra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - preventive");
        assertHasOnFlowConstraintUsageRule(pra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - Co-one-1 - outage");
        assertHasOnFlowConstraintUsageRule(pra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - Co-one-1 - auto");
        assertHasOnFlowConstraintUsageRule(pra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - Co-one-1 - curative");
        assertHasOnFlowConstraintUsageRule(pra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - Co-one-2 - outage");
        assertHasOnFlowConstraintUsageRule(pra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - Co-one-2 - auto");
        assertHasOnFlowConstraintUsageRule(pra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - Co-one-2 - curative");
        assertHasOnFlowConstraintUsageRule(pra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - Co-one-3 - outage");
        assertHasOnFlowConstraintUsageRule(pra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - Co-one-3 - auto");
        assertHasOnFlowConstraintUsageRule(pra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - Co-one-3 - curative");
        assertEquals(1, pra1.getRanges().size());
        assertEquals(RangeType.ABSOLUTE, pra1.getRanges().get(0).getRangeType());
        assertEquals(1, pra1.getRanges().get(0).getMinTap());
        assertEquals(33, pra1.getRanges().get(0).getMaxTap());
        assertEquals(10, pra1.getInitialTap());

        // PRA_CRA_1
        assertPstRangeActionImported("PRA_CRA_1", "_e8a7eaec-51d6-4571-b3d9-c36d52073c33", true);
        PstRangeAction praCra1 = importedCrac.getPstRangeAction("PRA_CRA_1");
        assertEquals(8, praCra1.getUsageRules().size());
        assertHasOnFlowConstraintUsageRule(praCra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - Co-one-2 - outage");
        assertHasOnFlowConstraintUsageRule(praCra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - Co-one-2 - auto");
        assertHasOnFlowConstraintUsageRule(praCra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - Co-one-2 - curative");
        assertHasOnFlowConstraintUsageRule(praCra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - Co-one-3 - outage");
        assertHasOnFlowConstraintUsageRule(praCra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - Co-one-3 - auto");
        assertHasOnFlowConstraintUsageRule(praCra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - Co-one-3 - curative");
        assertHasOnFlowConstraintUsageRule(praCra1, Instant.CURATIVE, "GHIOL_QSDFGH_1_220 - Co-one-2 - curative");
        assertHasOnFlowConstraintUsageRule(praCra1, Instant.CURATIVE, "GHIOL_QSDFGH_1_220 - Co-one-3 - curative");
        assertEquals(1, praCra1.getRanges().size());
        assertEquals(RangeType.RELATIVE_TO_INITIAL_NETWORK, praCra1.getRanges().get(0).getRangeType());
        assertEquals(-10, praCra1.getRanges().get(0).getMinTap());
        assertEquals(10, praCra1.getRanges().get(0).getMaxTap());
        assertEquals(8, praCra1.getInitialTap());

        // AUTO_1
        assertPstRangeActionImported("AUTO_1", "_e8a7eaec-51d6-4571-b3d9-c36d52073c33", true);
        PstRangeAction auto1 = importedCrac.getPstRangeAction("AUTO_1");
        assertEquals(4, auto1.getUsageRules().size());
        assertHasOnFlowConstraintUsageRule(auto1, Instant.AUTO, "GHIOL_QSDFGH_1_220 - Co-one-2 - auto");
        assertHasOnFlowConstraintUsageRule(auto1, Instant.AUTO, "GHIOL_QSDFGH_1_220 - Co-one-2 - curative");
        assertHasOnFlowConstraintUsageRule(auto1, Instant.AUTO, "GHIOL_QSDFGH_1_220 - Co-one-3 - auto");
        assertHasOnFlowConstraintUsageRule(auto1, Instant.AUTO, "GHIOL_QSDFGH_1_220 - Co-one-3 - curative");
        assertEquals(1, auto1.getRanges().size());
        assertEquals(RangeType.RELATIVE_TO_INITIAL_NETWORK, auto1.getRanges().get(0).getRangeType());
        assertEquals(-10, auto1.getRanges().get(0).getMinTap());
        assertEquals(10, auto1.getRanges().get(0).getMaxTap());
        assertEquals(8, auto1.getInitialTap());
    }

    @Test
    public void testImportRasAvailableForSpecificCountry() {
        setUp("/cracs/CIM_21_5_2.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), new CracCreationParameters());

        // RA_1
        assertNetworkActionImported("RA_1", Set.of("_2844585c-0d35-488d-a449-685bcd57afbf", "_ffbabc27-1ccd-4fdc-b037-e341706c8d29"), false);
        NetworkAction ra1 = importedCrac.getNetworkAction("RA_1");
        assertEquals(1, ra1.getUsageRules().size());
        assertTrue(ra1.getUsageRules().get(0) instanceof OnFlowConstraintInCountry);
        assertEquals(Instant.PREVENTIVE, ((OnFlowConstraintInCountry) ra1.getUsageRules().get(0)).getInstant());
        assertEquals(Country.PT, ((OnFlowConstraintInCountry) ra1.getUsageRules().get(0)).getCountry());
        assertEquals(2, ra1.getElementaryActions().size());
        assertTrue(ra1.getElementaryActions().stream()
            .filter(InjectionSetpoint.class::isInstance)
            .map(InjectionSetpoint.class::cast)
            .anyMatch(is -> is.getNetworkElement().getId().equals("_2844585c-0d35-488d-a449-685bcd57afbf") && is.getSetpoint() == 380)
        );
        assertTrue(ra1.getElementaryActions().stream()
            .filter(TopologicalAction.class::isInstance)
            .map(TopologicalAction.class::cast)
            .anyMatch(ta -> ta.getNetworkElement().getId().equals("_ffbabc27-1ccd-4fdc-b037-e341706c8d29") && ta.getActionType().equals(ActionType.CLOSE))
        );

        // RA_2
        assertNetworkActionImported("RA_2", Set.of("_e8a7eaec-51d6-4571-b3d9-c36d52073c33", "_b58bf21a-096a-4dae-9a01-3f03b60c24c7"), false);
        NetworkAction ra2 = importedCrac.getNetworkAction("RA_2");
        assertEquals(1, ra2.getUsageRules().size());
        assertTrue(ra2.getUsageRules().get(0) instanceof OnFlowConstraintInCountry);
        assertEquals(Instant.CURATIVE, ((OnFlowConstraintInCountry) ra2.getUsageRules().get(0)).getInstant());
        assertEquals(Country.ES, ((OnFlowConstraintInCountry) ra2.getUsageRules().get(0)).getCountry());
        assertEquals(2, ra2.getElementaryActions().size());
        assertTrue(ra2.getElementaryActions().stream()
            .filter(PstSetpoint.class::isInstance)
            .map(PstSetpoint.class::cast)
            .anyMatch(ps -> ps.getNetworkElement().getId().equals("_e8a7eaec-51d6-4571-b3d9-c36d52073c33") && ps.getSetpoint() == -19)
        );
        assertTrue(ra2.getElementaryActions().stream()
            .filter(TopologicalAction.class::isInstance)
            .map(TopologicalAction.class::cast)
            .anyMatch(ta -> ta.getNetworkElement().getId().equals("_b58bf21a-096a-4dae-9a01-3f03b60c24c7") && ta.getActionType().equals(ActionType.OPEN))
        );

        // RA_3
        assertNetworkActionImported("RA_3", Set.of("_b94318f6-6d24-4f56-96b9-df2531ad6543", "_1dc9afba-23b5-41a0-8540-b479ed8baf4b"), false);
        NetworkAction ra3 = importedCrac.getNetworkAction("RA_3");
        assertEquals(2, ra3.getUsageRules().size());
        assertTrue(
            ra3.getUsageRules().stream()
                .filter(FreeToUse.class::isInstance)
                .map(FreeToUse.class::cast)
                .anyMatch(ur -> ur.getInstant().equals(Instant.PREVENTIVE))
        );
        assertTrue(
            ra3.getUsageRules().stream()
                .filter(OnState.class::isInstance)
                .map(OnState.class::cast)
                .anyMatch(ur -> ur.getInstant().equals(Instant.CURATIVE) && ur.getContingency().getId().equals("CO_1"))
        );
        assertEquals(2, ra3.getElementaryActions().size());
        assertTrue(ra3.getElementaryActions().stream()
            .filter(PstSetpoint.class::isInstance)
            .map(PstSetpoint.class::cast)
            .anyMatch(ps -> ps.getNetworkElement().getId().equals("_b94318f6-6d24-4f56-96b9-df2531ad6543") && ps.getSetpoint() == 0)
        );
        assertTrue(ra3.getElementaryActions().stream()
            .filter(InjectionSetpoint.class::isInstance)
            .map(InjectionSetpoint.class::cast)
            .anyMatch(is -> is.getNetworkElement().getId().equals("_1dc9afba-23b5-41a0-8540-b479ed8baf4b") && is.getSetpoint() == 480)
        );
    }

    @Test
    public void testImportOnFlowConstraintRepeatedRa() {
        setUp("/cracs/CIM_21_5_3.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), new CracCreationParameters());

        // PRA_CRA_1
        assertPstRangeActionImported("PRA_CRA_1", "_e8a7eaec-51d6-4571-b3d9-c36d52073c33", true);
        PstRangeAction praCra1 = importedCrac.getPstRangeAction("PRA_CRA_1");
        assertEquals(8, praCra1.getUsageRules().size());
        assertHasOnFlowConstraintUsageRule(praCra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - Co-one-2 - outage");
        assertHasOnFlowConstraintUsageRule(praCra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - Co-one-2 - auto");
        assertHasOnFlowConstraintUsageRule(praCra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - Co-one-2 - curative");
        assertHasOnFlowConstraintUsageRule(praCra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - Co-one-3 - outage");
        assertHasOnFlowConstraintUsageRule(praCra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - Co-one-3 - auto");
        assertHasOnFlowConstraintUsageRule(praCra1, Instant.PREVENTIVE, "GHIOL_QSDFGH_1_220 - Co-one-3 - curative");
        assertHasOnFlowConstraintUsageRule(praCra1, Instant.CURATIVE, "GHIOL_QSDFGH_1_220 - Co-one-2 - curative");
        assertHasOnFlowConstraintUsageRule(praCra1, Instant.CURATIVE, "GHIOL_QSDFGH_1_220 - Co-one-3 - curative");
        assertEquals(1, praCra1.getRanges().size());
        assertEquals(RangeType.RELATIVE_TO_INITIAL_NETWORK, praCra1.getRanges().get(0).getRangeType());
        assertEquals(-10, praCra1.getRanges().get(0).getMinTap());
        assertEquals(10, praCra1.getRanges().get(0).getMaxTap());
        assertEquals(8, praCra1.getInitialTap());
    }
}
