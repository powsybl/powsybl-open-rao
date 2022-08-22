/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator;

import com.farao_community.farao.commons.Unit;
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
import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.AngleCnecCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.CnecCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.MeasurementCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.MonitoredSeriesCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.contingency.CimContingencyCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.remedial_action.RemedialActionSeriesCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.importer.CimCracImporter;
import com.farao_community.farao.data.crac_creation.creator.cim.parameters.*;
import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;
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
        Properties importParams = new Properties();
        importParams.put("iidm.import.cgmes.source-for-iidm-id", "rdfID");
        baseNetwork = Importers.loadNetwork(Paths.get(new File(CimCracCreatorTest.class.getResource("/networks/MicroGrid.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);
    }

    @BeforeClass
    public static void loadHvdcNetwork() {
        Properties importParams = new Properties();
        importParams.put("iidm.import.cgmes.source-for-iidm-id", "rdfID");
        hvdcNetwork = Importers.loadNetwork(Paths.get(new File(CimCracCreatorTest.class.getResource("/networks/TestCase16NodesWith2Hvdc.xiidm").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);
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
        Mockito.when(cimCracCreationParameters.getTimeseriesMrids()).thenReturn(Collections.emptySet());

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
        Mockito.when(cimCracCreationParameters.getTimeseriesMrids()).thenReturn(Collections.emptySet());
        InputStream is = getClass().getResourceAsStream(fileName);
        CimCracImporter cracImporter = new CimCracImporter();
        CimCrac cimCrac = cracImporter.importNativeCrac(is);
        CimCracCreator cimCracCreator = new CimCracCreator();
        cracCreationContext = cimCracCreator.createCrac(cimCrac, network, parametrableOffsetDateTime, cracCreationParameters);
        importedCrac = cracCreationContext.getCrac();
    }

    private void setUpWithTimeseriesMrids(String fileName, Network network, OffsetDateTime parametrableOffsetDateTime, Set<String> timeseriesMrids) {
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters = Mockito.spy(cracCreationParameters);
        CimCracCreationParameters  cimCracCreationParameters = Mockito.mock(CimCracCreationParameters.class);
        Mockito.when(cracCreationParameters.getExtension(CimCracCreationParameters.class)).thenReturn(cimCracCreationParameters);
        Mockito.when(cimCracCreationParameters.getTimeseriesMrids()).thenReturn(timeseriesMrids);
        InputStream is = getClass().getResourceAsStream(fileName);
        CimCracImporter cracImporter = new CimCracImporter();
        CimCrac cimCrac = cracImporter.importNativeCrac(is);
        CimCracCreator cimCracCreator = new CimCracCreator();
        cracCreationContext = cimCracCreator.createCrac(cimCrac, network, parametrableOffsetDateTime, cracCreationParameters);
        importedCrac = cracCreationContext.getCrac();
    }

    private void assertContingencyNotImported(String name, String nativeName, ImportStatus importStatus) {
        CimContingencyCreationContext context = cracCreationContext.getContingencyCreationContextById(name);
        assertNotNull(context);
        assertEquals(nativeName, context.getNativeName());
        assertFalse(context.isImported());
        assertEquals(importStatus, context.getImportStatus());
    }

    private void assertContingencyImported(String id, String nativeName, Set<String> networkElements, boolean isAltered) {
        CimContingencyCreationContext context = cracCreationContext.getContingencyCreationContextById(id);
        assertNotNull(context);
        assertEquals(nativeName, context.getNativeName());
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

    private void assertAngleCnecImportedWithContingency(String id, String contingencyId, Set<String> networkElementIds) {
        AngleCnecCreationContext angleCnecCreationContext = cracCreationContext.getAngleCnecCreationContexts(id);
        assertNotNull(angleCnecCreationContext);
        assertTrue(angleCnecCreationContext.isImported());
        assertNotNull(importedCrac.getAngleCnec(id));
        Set<String> importedAngleCnecNetworkElements = new HashSet<>();
        importedAngleCnecNetworkElements.add(importedCrac.getAngleCnec(id).getImportingNetworkElement().toString());
        importedAngleCnecNetworkElements.add(importedCrac.getAngleCnec(id).getExportingNetworkElement().toString());
        assertEquals(networkElementIds, importedAngleCnecNetworkElements);
        assertEquals(contingencyId, angleCnecCreationContext.getContingencyId());
    }

    private void assertAngleCnecNotImported(String id, ImportStatus importStatus) {
        AngleCnecCreationContext angleCnecCreationContext = cracCreationContext.getAngleCnecCreationContexts(id);
        assertNotNull(angleCnecCreationContext);
        assertFalse(angleCnecCreationContext.isImported());
        assertEquals(importStatus, angleCnecCreationContext.getImportStatus());
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

    private void assertHasOnAngleUsageRule(String raId, String angleCnecId) {
        RemedialAction ra = importedCrac.getRemedialAction(raId);
        assertTrue(
                ra.getUsageRules().stream()
                        .filter(OnAngleConstraint.class::isInstance)
                        .anyMatch(
                                ur -> ((OnAngleConstraint) ur).getInstant().equals(Instant.CURATIVE)
                                        && ((OnAngleConstraint) ur).getAngleCnec().getId().equals(angleCnecId)
                                        && ((OnAngleConstraint) ur).getUsageMethod().equals(UsageMethod.TO_BE_EVALUATED)
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
        assertContingencyImported("Co-1", "Co-1-name", Set.of("_ffbabc27-1ccd-4fdc-b037-e341706c8d29"), false);
        assertContingencyImported("Co-2", "Co-2-name", Set.of("_b18cd1aa-7808-49b9-a7cf-605eaf07b006 + _e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc", "_df16b3dd-c905-4a6f-84ee-f067be86f5da"), false);
        assertContingencyImported("Co-3", "Co-3-name", Set.of("_b58bf21a-096a-4dae-9a01-3f03b60c24c7"), true);

        assertContingencyNotImported("Co-4", "Co-4-name", ELEMENT_NOT_FOUND_IN_NETWORK);
        assertContingencyNotImported("Co-5", "Co-5-name", INCOMPLETE_DATA);

        assertEquals(3, cracCreationContext.getCreationReport().getReport().size()); // 2 fake contingencies, 1 altered
    }

    @Test
    public void testImportContingencyOnTieLine() {
        setUp("/cracs/CIM_co_halfline.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T22:00Z"), new CracCreationParameters());

        assertEquals(1, importedCrac.getContingencies().size());
        assertContingencyImported("Co-2", "Co-2-name", Set.of("_b18cd1aa-7808-49b9-a7cf-605eaf07b006 + _e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc", "_df16b3dd-c905-4a6f-84ee-f067be86f5da"), false);
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

    @Test
    public void testImportKOHvdcRangeActions() {
        setUpWithSpeed("/cracs/CIM_21_6_1.xml", hvdcNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), null);
        assertRemedialActionNotImported("HVDC-direction11", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC-direction12", INCONSISTENCY_IN_DATA);
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

    @Test
    public void testImportAngleCnecs() {
        setUp("/cracs/CIM_21_7_1.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), new CracCreationParameters());
        // -- Imported
        // Angle cnec and associated RA imported :
        assertAngleCnecImportedWithContingency("AngleCnec1", "Co-1", Set.of("_8d8a82ba-b5b0-4e94-861a-192af055f2b8", "_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3"));
        assertNetworkActionImported("RA1", Set.of("_1dc9afba-23b5-41a0-8540-b479ed8baf4b", "_2844585c-0d35-488d-a449-685bcd57afbf"), false);
        assertHasOnAngleUsageRule("RA1", "AngleCnec1");
        assertEquals(1, importedCrac.getRemedialAction("RA1").getUsageRules().size());
        // -- Partially imported
        // Angle cnec without an associated RA :
        assertAngleCnecImportedWithContingency("AngleCnec3", "Co-1", Set.of("_8d8a82ba-b5b0-4e94-861a-192af055f2b8", "_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3"));
        // Angle cnec with ill defined RA :
        assertAngleCnecImportedWithContingency("AngleCnec11", "Co-1", Set.of("_8d8a82ba-b5b0-4e94-861a-192af055f2b8", "_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3"));
        assertRemedialActionNotImported("RA11", ELEMENT_NOT_FOUND_IN_NETWORK);

        // -- Not imported
        assertAngleCnecNotImported("AngleCnec2", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("RA2", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("Angle4", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("Angle5", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("Angle6", INCONSISTENCY_IN_DATA);
        assertAngleCnecNotImported("AngleCnec7", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("RA7", INCONSISTENCY_IN_DATA);
        assertAngleCnecNotImported("AngleCnec8", ELEMENT_NOT_FOUND_IN_NETWORK);
        assertRemedialActionNotImported("RA8", INCONSISTENCY_IN_DATA);
        assertAngleCnecNotImported("AngleCnec9", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("RA9", INCONSISTENCY_IN_DATA);
        assertAngleCnecNotImported("AngleCnec10", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("RA10", INCONSISTENCY_IN_DATA);
    }

    @Test
    public void testFilterOnTimeseries() {
        setUpWithTimeseriesMrids("/cracs/CIM_2_timeseries.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), Collections.emptySet());
        assertEquals(2, importedCrac.getContingencies().size());

        setUpWithTimeseriesMrids("/cracs/CIM_2_timeseries.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), Set.of("TimeSeries1", "TimeSeries2", "TimeSeries3"));
        assertEquals(2, importedCrac.getContingencies().size());
        assertEquals(1, cracCreationContext.getCreationReport().getReport().size());
        assertEquals("[WARN] Requested TimeSeries mRID \"TimeSeries3\" in CimCracCreationParameters was not found in the CRAC file.", cracCreationContext.getCreationReport().getReport().get(0));

        setUpWithTimeseriesMrids("/cracs/CIM_2_timeseries.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), Set.of("TimeSeries1"));
        assertEquals(1, importedCrac.getContingencies().size());
        assertNotNull(importedCrac.getContingency("Co-1"));

        setUpWithTimeseriesMrids("/cracs/CIM_2_timeseries.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), Set.of("TimeSeries2"));
        assertEquals(1, importedCrac.getContingencies().size());
        assertNotNull(importedCrac.getContingency("Co-2"));
    }

    private VoltageThreshold mockVoltageThreshold(Double min, Double max) {
        VoltageThreshold threshold = Mockito.mock(VoltageThreshold.class);
        Mockito.when(threshold.getUnit()).thenReturn(Unit.KILOVOLT);
        Mockito.when(threshold.getMin()).thenReturn(min);
        Mockito.when(threshold.getMax()).thenReturn(max);
        return threshold;
    }

    @Test
    public void testImportVoltageCnecs() {
        Set<String> monitoredElements = Set.of("_d77b61ef-61aa-4b22-95f6-b56ca080788d", "_2844585c-0d35-488d-a449-685bcd57afbf", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0");

        Map<Instant, VoltageMonitoredContingenciesAndThresholds> monitoredStatesAndThresholds = Map.of(
            Instant.PREVENTIVE, new VoltageMonitoredContingenciesAndThresholds(null, Map.of(220., mockVoltageThreshold(220., 230.))),
            Instant.CURATIVE, new VoltageMonitoredContingenciesAndThresholds(Set.of("Co-1-name", "Co-4-name"), Map.of(220., mockVoltageThreshold(210., 240.))),
            Instant.OUTAGE, new VoltageMonitoredContingenciesAndThresholds(Set.of("Co-3-name"), Map.of(220., mockVoltageThreshold(200., null))),
            Instant.AUTO, new VoltageMonitoredContingenciesAndThresholds(Set.of("Co-2-name"), Map.of(220., mockVoltageThreshold(null, null)))
        );
        VoltageCnecsCreationParameters voltageCnecsCreationParameters = new VoltageCnecsCreationParameters(monitoredStatesAndThresholds, monitoredElements);

        CracCreationParameters params = new CracCreationParameters();
        CimCracCreationParameters cimParams = new CimCracCreationParameters();
        cimParams.setVoltageCnecsCreationParameters(voltageCnecsCreationParameters);
        params.addExtension(CimCracCreationParameters.class, cimParams);

        setUp("/cracs/CIM_21_1_1.xml", baseNetwork,  OffsetDateTime.parse("2021-04-01T23:00Z"), params);

        assertEquals(3, importedCrac.getVoltageCnecs().size());
        assertNotNull(importedCrac.getVoltageCnec("[VC] _d77b61ef-61aa-4b22-95f6-b56ca080788d - preventive"));
        assertNotNull(importedCrac.getVoltageCnec("[VC] _d77b61ef-61aa-4b22-95f6-b56ca080788d - Co-1 - curative"));
        assertNotNull(importedCrac.getVoltageCnec("[VC] _d77b61ef-61aa-4b22-95f6-b56ca080788d - Co-3 - outage"));
        assertEquals(7, cracCreationContext.getVoltageCnecCreationContexts().size());
        assertEquals(7, cracCreationContext.getCreationReport().getReport().size());
        assertTrue(cracCreationContext.getCreationReport().getReport().contains("[REMOVED] VoltageCnec with network element \"_2844585c-0d35-488d-a449-685bcd57afbf\", instant \"all\" and contingency \"all\" was not imported: INCONSISTENCY_IN_DATA. Element _2844585c-0d35-488d-a449-685bcd57afbf is not a voltage level."));
        assertTrue(cracCreationContext.getCreationReport().getReport().contains("[REMOVED] VoltageCnec with network element \"_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0\", instant \"all\" and contingency \"all\" was not imported: INCONSISTENCY_IN_DATA. Element _a708c3bc-465d-4fe7-b6ef-6fa6408a62b0 is not a voltage level."));
        assertTrue(cracCreationContext.getCreationReport().getReport().contains("[REMOVED] VoltageCnec with network element \"all\", instant \"all\" and contingency \"Co-4-name\" was not imported: OTHER. Contingency does not exist in the CRAC or could not be imported."));
        assertTrue(cracCreationContext.getCreationReport().getReport().contains("[REMOVED] VoltageCnec with network element \"_d77b61ef-61aa-4b22-95f6-b56ca080788d\", instant \"auto\" and contingency \"Co-2-name\" was not imported: INCONSISTENCY_IN_DATA. Cannot add a threshold without min nor max values. Please use withMin() or withMax().."));
    }
}
