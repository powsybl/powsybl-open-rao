/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.cim.craccreator;

import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.action.GeneratorAction;
import com.powsybl.action.PhaseTapChangerTapPositionAction;
import com.powsybl.action.TerminalsConnectionAction;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyState;
import com.powsybl.openrao.data.crac.api.usagerule.OnFlowConstraintInCountry;
import com.powsybl.openrao.data.crac.api.usagerule.OnInstant;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.crac.io.cim.parameters.CimCracCreationParameters;
import com.powsybl.openrao.data.crac.io.cim.parameters.RangeActionSpeed;
import com.powsybl.openrao.data.crac.io.cim.parameters.VoltageCnecsCreationParameters;
import com.powsybl.openrao.data.crac.io.cim.parameters.VoltageMonitoredContingenciesAndThresholds;
import com.powsybl.openrao.data.crac.io.cim.parameters.VoltageThreshold;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.parameters.RangeActionGroup;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.threshold.BranchThreshold;
import com.powsybl.openrao.data.crac.io.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class CimCracCreatorTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";
    private static final double DOUBLE_TOLERANCE = 1e-6;

    private Crac importedCrac;
    private CimCracCreationContext cracCreationContext;
    private static Network baseNetwork;
    private static Network hvdcNetwork;
    private Instant preventiveInstant;
    private Instant autoInstant;
    private Instant curativeInstant;

    @BeforeAll
    public static void loadNetwork() {
        Properties importParams = new Properties();
        importParams.put("iidm.import.cgmes.source-for-iidm-id", "rdfID");
        baseNetwork = Network.read(Paths.get(new File(CimCracCreatorTest.class.getResource("/networks/MicroGrid_missingImax.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);
    }

    @BeforeAll
    public static void loadHvdcNetwork() {
        Properties importParams = new Properties();
        importParams.put("iidm.import.cgmes.source-for-iidm-id", "rdfID");
        hvdcNetwork = Network.read(Paths.get(new File(CimCracCreatorTest.class.getResource("/networks/TestCase16NodesWith2Hvdc.xiidm").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);
    }

    private void setUp(String fileName, Network network, CracCreationParameters cracCreationParameters) throws IOException {
        InputStream is = getClass().getResourceAsStream(fileName);
        cracCreationContext = (CimCracCreationContext) Crac.readWithContext(fileName, is, network, cracCreationParameters);
        importedCrac = cracCreationContext.getCrac();
        if (!Objects.isNull(importedCrac)) {
            preventiveInstant = importedCrac.getInstant(PREVENTIVE_INSTANT_ID);
            autoInstant = importedCrac.getInstant(AUTO_INSTANT_ID);
            curativeInstant = importedCrac.getInstant(CURATIVE_INSTANT_ID);
        } else {
            preventiveInstant = null;
            autoInstant = null;
            curativeInstant = null;
        }
    }

    private void setUpWithGroupId(String fileName, Network network, OffsetDateTime timestamp, List<List<String>> alignedRangeActions) throws IOException {
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_ONE);
        cracCreationParameters = Mockito.spy(cracCreationParameters);
        CimCracCreationParameters cimCracCreationParameters = Mockito.mock(CimCracCreationParameters.class);
        Mockito.when(cracCreationParameters.getExtension(CimCracCreationParameters.class)).thenReturn(cimCracCreationParameters);
        List<RangeActionGroup> rangeActionGroups = new ArrayList<>();
        alignedRangeActions.forEach(listAlignedRangeActions -> rangeActionGroups.add(new RangeActionGroup(listAlignedRangeActions)));
        Mockito.when(cimCracCreationParameters.getRangeActionGroups()).thenReturn(rangeActionGroups);
        Mockito.when(cimCracCreationParameters.getTimeseriesMrids()).thenReturn(Collections.emptySet());
        Mockito.when(cimCracCreationParameters.getTimestamp()).thenReturn(timestamp);
        InputStream is = getClass().getResourceAsStream(fileName);
        cracCreationContext = (CimCracCreationContext) Crac.readWithContext(fileName, is, network, cracCreationParameters);
        importedCrac = cracCreationContext.getCrac();
        preventiveInstant = importedCrac.getInstant(PREVENTIVE_INSTANT_ID);
        autoInstant = importedCrac.getInstant(AUTO_INSTANT_ID);
        curativeInstant = importedCrac.getInstant(CURATIVE_INSTANT_ID);
    }

    private void setUpWithSpeed(String fileName, Network network, OffsetDateTime timestamp, Set<RangeActionSpeed> rangeActionSpeeds) throws IOException {
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_ONE);
        cracCreationParameters = Mockito.spy(cracCreationParameters);
        CimCracCreationParameters cimCracCreationParameters = Mockito.mock(CimCracCreationParameters.class);
        Mockito.when(cracCreationParameters.getExtension(CimCracCreationParameters.class)).thenReturn(cimCracCreationParameters);
        Mockito.when(cimCracCreationParameters.getRangeActionSpeedSet()).thenReturn(rangeActionSpeeds);
        Mockito.when(cimCracCreationParameters.getTimeseriesMrids()).thenReturn(Collections.emptySet());
        Mockito.when(cimCracCreationParameters.getTimestamp()).thenReturn(timestamp);
        InputStream is = getClass().getResourceAsStream(fileName);
        cracCreationContext = (CimCracCreationContext) Crac.readWithContext(fileName, is, network, cracCreationParameters);
        importedCrac = cracCreationContext.getCrac();
        preventiveInstant = importedCrac.getInstant(PREVENTIVE_INSTANT_ID);
        autoInstant = importedCrac.getInstant(AUTO_INSTANT_ID);
        curativeInstant = importedCrac.getInstant(CURATIVE_INSTANT_ID);
    }

    private void setUpWithTimeseriesMrids(String fileName, Network network, OffsetDateTime timestamp, Set<String> timeseriesMrids) throws IOException {
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_ONE);
        cracCreationParameters = Mockito.spy(cracCreationParameters);
        CimCracCreationParameters cimCracCreationParameters = Mockito.mock(CimCracCreationParameters.class);
        Mockito.when(cracCreationParameters.getExtension(CimCracCreationParameters.class)).thenReturn(cimCracCreationParameters);
        Mockito.when(cimCracCreationParameters.getTimeseriesMrids()).thenReturn(timeseriesMrids);
        Mockito.when(cimCracCreationParameters.getTimestamp()).thenReturn(timestamp);
        InputStream is = getClass().getResourceAsStream(fileName);
        cracCreationContext = (CimCracCreationContext) Crac.readWithContext(fileName, is, network, cracCreationParameters);
        importedCrac = cracCreationContext.getCrac();
        preventiveInstant = importedCrac.getInstant(PREVENTIVE_INSTANT_ID);
        autoInstant = importedCrac.getInstant(AUTO_INSTANT_ID);
        curativeInstant = importedCrac.getInstant(CURATIVE_INSTANT_ID);
    }

    private void setUpWithTimestamp(String fileName, Network network, OffsetDateTime timestamp) throws IOException {
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        CimCracCreationParameters cimCracCreationParameters = Mockito.mock(CimCracCreationParameters.class);
        cracCreationParameters = Mockito.spy(cracCreationParameters);
        Mockito.when(cracCreationParameters.getExtension(CimCracCreationParameters.class)).thenReturn(cimCracCreationParameters);
        Mockito.when(cimCracCreationParameters.getTimeseriesMrids()).thenReturn(Collections.emptySet());
        Mockito.when(cimCracCreationParameters.getTimestamp()).thenReturn(timestamp);
        InputStream is = getClass().getResourceAsStream(fileName);
        cracCreationContext = (CimCracCreationContext) Crac.readWithContext(fileName, is, network, cracCreationParameters);
        importedCrac = cracCreationContext.getCrac();
        if (!Objects.isNull(importedCrac)) {
            preventiveInstant = importedCrac.getInstant(PREVENTIVE_INSTANT_ID);
            autoInstant = importedCrac.getInstant(AUTO_INSTANT_ID);
            curativeInstant = importedCrac.getInstant(CURATIVE_INSTANT_ID);
        } else {
            preventiveInstant = null;
            autoInstant = null;
            curativeInstant = null;
        }
    }

    private void assertContingencyNotImported(String name, String nativeName, ImportStatus importStatus) {
        ElementaryCreationContext context = cracCreationContext.getContingencyCreationContextById(name);
        assertNotNull(context);
        assertEquals(nativeName, context.getNativeObjectName());
        assertFalse(context.isImported());
        assertEquals(importStatus, context.getImportStatus());
    }

    private void assertContingencyImported(String id, String nativeName, Set<String> networkElements, boolean isAltered) {
        ElementaryCreationContext context = cracCreationContext.getContingencyCreationContextById(id);
        assertNotNull(context);
        assertEquals(nativeName, context.getNativeObjectName());
        assertTrue(context.isImported());
        assertEquals(isAltered, context.isAltered());
        if (isAltered) {
            assertNotNull(context.getImportStatusDetail());
        } else {
            assertNull(context.getImportStatusDetail());
        }
        assertEquals(id, context.getCreatedObjectId());
        assertNotNull(importedCrac.getContingency(id));
        Set<String> actualNetworkElements = importedCrac.getContingency(id).getElements().stream().map(ContingencyElement::getId).collect(Collectors.toSet());
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
        Set<String> importedCnecIds =
            monitoredSeriesCreationContext.getMeasurementCreationContexts().stream()
                .filter(MeasurementCreationContext::isImported)
                .map(measurementCreationContext ->
                    measurementCreationContext.getCnecCreationContexts().values().stream()
                        .filter(CnecCreationContext::isImported)
                        .map(CnecCreationContext::getCreatedCnecId)
                        .collect(Collectors.toSet()))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        assertEquals(expectedCnecIds, importedCnecIds);
    }

    private void assertAngleCnecImportedWithContingency(String id, String contingencyId, Set<String> networkElementIds, double max) {
        AngleCnecCreationContext angleCnecCreationContext = cracCreationContext.getAngleCnecCreationContext(id);
        assertNotNull(angleCnecCreationContext);
        assertTrue(angleCnecCreationContext.isImported());
        assertNotNull(importedCrac.getAngleCnec(id));
        Set<String> importedAngleCnecNetworkElements = new HashSet<>();
        importedAngleCnecNetworkElements.add(importedCrac.getAngleCnec(id).getImportingNetworkElement().toString());
        importedAngleCnecNetworkElements.add(importedCrac.getAngleCnec(id).getExportingNetworkElement().toString());
        assertEquals(networkElementIds, importedAngleCnecNetworkElements);
        assertEquals(contingencyId, angleCnecCreationContext.getContingencyId());
        assertEquals(1, importedCrac.getAngleCnec(id).getThresholds().size());
        assertEquals(max, importedCrac.getAngleCnec(id).getThresholds().iterator().next().max().orElseThrow());
        assertTrue(importedCrac.getAngleCnec(id).getThresholds().iterator().next().min().isEmpty());
    }

    private void assertAngleCnecNotImported(String id, ImportStatus importStatus) {
        AngleCnecCreationContext angleCnecCreationContext = cracCreationContext.getAngleCnecCreationContext(id);
        assertNotNull(angleCnecCreationContext);
        assertFalse(angleCnecCreationContext.isImported());
        assertEquals(importStatus, angleCnecCreationContext.getImportStatus());
    }

    private void assertRemedialActionNotImported(String id, ImportStatus importStatus) {
        RemedialActionSeriesCreationContext remedialActionSeriesCreationContext = cracCreationContext.getRemedialActionSeriesCreationContext(id);
        assertNotNull(remedialActionSeriesCreationContext);
        assertFalse(remedialActionSeriesCreationContext.isImported());
        assertEquals(importStatus, remedialActionSeriesCreationContext.getImportStatus());
    }

    private void assertRemedialActionImportedWithOperator(String remedialActionId, String operator) {
        RemedialActionSeriesCreationContext remedialActionSeriesCreationContext = cracCreationContext.getRemedialActionSeriesCreationContext(remedialActionId);
        assertNotNull(remedialActionSeriesCreationContext);
        assertTrue(remedialActionSeriesCreationContext.isImported());
        assertEquals(operator, importedCrac.getRemedialAction(remedialActionId).getOperator());
    }

    private void assertPstRangeActionImported(String id, String networkElement, boolean isAltered) {
        RemedialActionSeriesCreationContext remedialActionSeriesCreationContext = cracCreationContext.getRemedialActionSeriesCreationContext(id);
        assertNotNull(remedialActionSeriesCreationContext);
        assertTrue(remedialActionSeriesCreationContext.isImported());
        assertEquals(isAltered, remedialActionSeriesCreationContext.isAltered());
        assertNotNull(importedCrac.getPstRangeAction(id));
        String actualNetworkElement = importedCrac.getPstRangeAction(id).getNetworkElement().toString();
        assertEquals(networkElement, actualNetworkElement);
    }

    private void assertHvdcRangeActionImported(String expectedNativeId, Set<String> expectedCreatedIds, Set<String> expectedNetworkElements, Set<String> expectedOperators, boolean isInverted) {
        RemedialActionSeriesCreationContext remedialActionSeriesCreationContext = cracCreationContext.getRemedialActionSeriesCreationContext(expectedNativeId);
        assertNotNull(remedialActionSeriesCreationContext);
        assertEquals(expectedCreatedIds, remedialActionSeriesCreationContext.getCreatedObjectsIds());
        assertTrue(remedialActionSeriesCreationContext.isImported());
        expectedCreatedIds.forEach(createdId -> assertNotNull(importedCrac.getHvdcRangeAction(createdId)));
        Set<String> actualNetworkElements = new HashSet<>();
        expectedCreatedIds.forEach(createdId -> actualNetworkElements.add(importedCrac.getHvdcRangeAction(createdId).getNetworkElement().toString()));
        assertEquals(actualNetworkElements, expectedNetworkElements);
        Set<String> actualOperators = new HashSet<>();
        expectedCreatedIds.forEach(createdId -> actualOperators.add(importedCrac.getHvdcRangeAction(createdId).getOperator()));
        assertEquals(actualOperators, expectedOperators);
        assertEquals(isInverted, remedialActionSeriesCreationContext.isInverted());
    }

    private void assertNetworkActionImported(String id, Set<String> networkElements, boolean isAltered) {
        RemedialActionSeriesCreationContext remedialActionSeriesCreationContext = cracCreationContext.getRemedialActionSeriesCreationContext(id);
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
                .filter(OnConstraint.class::isInstance)
                .map(OnConstraint.class::cast)
                .anyMatch(
                    ur -> ur.getInstant().equals(instant)
                        && ur.getCnec() instanceof FlowCnec
                        && ur.getCnec().getId().equals(flowCnecId)
                        && ur.getUsageMethod().equals(instant.isAuto() ? UsageMethod.FORCED : UsageMethod.AVAILABLE)
                ));
    }

    private void assertHasOnAngleUsageRule(String raId, String angleCnecId) {
        RemedialAction<?> ra = importedCrac.getRemedialAction(raId);
        assertTrue(
            ra.getUsageRules().stream()
                .filter(OnConstraint.class::isInstance)
                .map(OnConstraint.class::cast)
                .anyMatch(
                    ur -> ur.getInstant().isCurative()
                        && ur.getCnec() instanceof AngleCnec
                        && ur.getCnec().getId().equals(angleCnecId)
                        && ur.getUsageMethod().equals(UsageMethod.AVAILABLE)
                ));
    }

    private void assertHasOneThreshold(String cnecId, TwoSides side, Unit unit, double min, double max) {
        FlowCnec cnec = importedCrac.getFlowCnec(cnecId);
        assertEquals(1, cnec.getThresholds().size());
        BranchThreshold threshold = cnec.getThresholds().iterator().next();
        assertEquals(side, threshold.getSide());
        assertEquals(unit, threshold.getUnit());
        assertTrue(threshold.limitsByMin());
        assertEquals(min, threshold.min().get(), DOUBLE_TOLERANCE);
        assertTrue(threshold.limitsByMax());
        assertEquals(max, threshold.max().get(), DOUBLE_TOLERANCE);
    }

    private void assertHasTwoThresholds(String cnecId, Unit unit, double min, double max) {
        FlowCnec cnec = importedCrac.getFlowCnec(cnecId);
        assertEquals(2, cnec.getThresholds().size());
        assertTrue(cnec.getThresholds().stream().anyMatch(threshold -> threshold.getSide().equals(TwoSides.ONE)));
        assertTrue(cnec.getThresholds().stream().anyMatch(threshold -> threshold.getSide().equals(TwoSides.TWO)));
        cnec.getThresholds().forEach(threshold -> {
            assertEquals(unit, threshold.getUnit());
            assertTrue(threshold.limitsByMin());
            assertEquals(min, threshold.min().get(), DOUBLE_TOLERANCE);
            assertTrue(threshold.limitsByMax());
            assertEquals(max, threshold.max().get(), DOUBLE_TOLERANCE);
        });
    }

    @Test
    void cracCreationSuccessfulFailureTime() throws IOException {
        CracCreationParameters cracParams = new CracCreationParameters();
        cracParams.addExtension(CimCracCreationParameters.class, new CimCracCreationParameters());
        setUp("/cracs/CIM_21_1_1.xml", baseNetwork, cracParams);
        assertFalse(cracCreationContext.isCreationSuccessful());
    }

    @Test
    void cracCreationFailureWrongTime() throws IOException {
        setUpWithTimestamp("/cracs/CIM_21_1_1.xml", baseNetwork, OffsetDateTime.parse("2020-04-01T22:00Z"));
        assertFalse(cracCreationContext.isCreationSuccessful());
    }

    @Test
    void cracCreationSuccessfulRightTime() throws IOException {
        setUpWithTimestamp("/cracs/CIM_21_1_1.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T22:00Z"));
        assertTrue(cracCreationContext.isCreationSuccessful());
    }

    @Test
    void cracCreationWithParameters() throws IOException {
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        RaUsageLimits raUsageLimits = new RaUsageLimits();
        raUsageLimits.setMaxRa(2);
        cracCreationParameters.addRaUsageLimitsForInstant("preventive", raUsageLimits);
        cracCreationParameters.addExtension(CimCracCreationParameters.class, new CimCracCreationParameters());
        cracCreationParameters.getExtension(CimCracCreationParameters.class).setTimestamp(OffsetDateTime.parse("2021-04-01T22:00Z"));
        setUp("/cracs/CIM_21_1_1.xml", baseNetwork, cracCreationParameters);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(2, cracCreationContext.getCrac().getRaUsageLimits(preventiveInstant).getMaxRa());
        assertEquals(OffsetDateTime.of(2021, 2, 9, 19, 30, 0, 0, ZoneOffset.UTC), cracCreationContext.getNetworkCaseDate());
        assertEquals(14, cracCreationContext.getNetworkBranches().size());
    }

    @Test
    void testImportContingencies() throws IOException {
        setUpWithTimestamp("/cracs/CIM_21_1_1.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"));
        assertEquals(3, importedCrac.getContingencies().size());
        assertContingencyImported("Co-1", "Co-1-name", Set.of("_ffbabc27-1ccd-4fdc-b037-e341706c8d29"), false);
        assertContingencyImported("Co-2", "Co-2-name", Set.of("_b18cd1aa-7808-49b9-a7cf-605eaf07b006 + _e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc", "_df16b3dd-c905-4a6f-84ee-f067be86f5da"), false);
        assertContingencyImported("Co-3", "Co-3-name", Set.of("_b58bf21a-096a-4dae-9a01-3f03b60c24c7"), true);

        assertContingencyNotImported("Co-4", "Co-4-name", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK);
        assertContingencyNotImported("Co-5", "Co-5-name", ImportStatus.INCOMPLETE_DATA);

        assertEquals(4, cracCreationContext.getCreationReport().getReport().size()); // 2 fake contingencies, 1 altered, 1 CRAC content
    }

    @Test
    void testCracPeriodManagementBeforeValidPeriod() throws IOException {
        setUpWithTimestamp("/cracs/CIM_21_1_1_multi_period.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T22:00Z"));
        assertEquals(0, importedCrac.getContingencies().size());
        setUpWithTimestamp("/cracs/CIM_21_1_1_multi_period.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"));
        assertEquals(3, importedCrac.getContingencies().size());
        setUpWithTimestamp("/cracs/CIM_21_1_1_multi_period.xml", baseNetwork, OffsetDateTime.parse("2021-04-02T01:00Z"));
        assertEquals(3, importedCrac.getContingencies().size());
        setUpWithTimestamp("/cracs/CIM_21_1_1_multi_period.xml", baseNetwork, OffsetDateTime.parse("2021-04-02T02:00Z"));
        assertEquals(1, importedCrac.getContingencies().size());
    }

    @Test
    void testImportContingencyOnTieLine() throws IOException {
        setUpWithTimestamp("/cracs/CIM_co_halfline.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"));

        assertEquals(1, importedCrac.getContingencies().size());
        assertContingencyImported("Co-2", "Co-2-name", Set.of("_b18cd1aa-7808-49b9-a7cf-605eaf07b006 + _e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc", "_df16b3dd-c905-4a6f-84ee-f067be86f5da"), false);
    }

    @Test
    void testImportFakeCnecs() throws IOException {
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_ONE);
        cracCreationParameters.addExtension(CimCracCreationParameters.class, new CimCracCreationParameters());
        cracCreationParameters.getExtension(CimCracCreationParameters.class).setTimestamp(OffsetDateTime.parse("2021-04-01T23:00Z"));
        setUp("/cracs/CIM_21_2_1.xml", baseNetwork, cracCreationParameters);

        assertEquals(10, importedCrac.getFlowCnecs().size());

        // CNEC 2
        assertCnecNotImported("CNEC-2", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK);

        // CNEC 3
        assertCnecImported("CNEC-3", Set.of("CNEC-3 - preventive", "CNEC-3 - Co-1 - auto", "CNEC-3 - Co-2 - auto"));
        assertHasOneThreshold("CNEC-3 - preventive", TwoSides.ONE, Unit.MEGAWATT, -3, 3);
        assertHasOneThreshold("CNEC-3 - Co-1 - auto", TwoSides.ONE, Unit.AMPERE, -3, 3);
        assertHasOneThreshold("CNEC-3 - Co-2 - auto", TwoSides.ONE, Unit.AMPERE, -3, 3);

        // CNEC 4
        assertCnecImported("CNEC-4", Set.of("CNEC-4 - preventive", "CNEC-4 - Co-1 - curative", "CNEC-4 - Co-2 - curative"));
        assertHasOneThreshold("CNEC-4 - preventive", TwoSides.ONE, Unit.PERCENT_IMAX, -0.04, 0.04);
        assertHasOneThreshold("CNEC-4 - Co-1 - curative", TwoSides.ONE, Unit.PERCENT_IMAX, -0.04, 0.04);
        assertHasOneThreshold("CNEC-4 - Co-2 - curative", TwoSides.ONE, Unit.PERCENT_IMAX, -0.04, 0.04);

        // CNEC 5
        assertCnecImported("MNEC-1", Set.of("CNEC-5 - ONE - MONITORED - preventive", "CNEC-5 - ONE - MONITORED - Co-1 - curative"));
        assertHasOneThreshold("CNEC-5 - ONE - MONITORED - preventive", TwoSides.ONE, Unit.PERCENT_IMAX, -0.05, 0.05);
        assertHasOneThreshold("CNEC-5 - ONE - MONITORED - Co-1 - curative", TwoSides.ONE, Unit.PERCENT_IMAX, -0.05, 0.05);

        // CNEC 6
        assertCnecImported("MNEC-2", Set.of("CNEC-6 - MONITORED - preventive", "CNEC-6 - MONITORED - Co-1 - outage"));
        assertHasOneThreshold("CNEC-6 - MONITORED - preventive", TwoSides.ONE, Unit.PERCENT_IMAX, -0.06, 0.06);
        assertHasOneThreshold("CNEC-6 - MONITORED - Co-1 - outage", TwoSides.ONE, Unit.PERCENT_IMAX, -0.06, 0.06);
    }

    @Test
    void testImportPstRangeActions() throws IOException {
        setUpWithTimestamp("/cracs/CIM_21_3_1.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"));
        assertPstRangeActionImported("PRA_1", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", false);
        assertRemedialActionImportedWithOperator("PRA_1", "PRA_1");
        assertRemedialActionImportedWithOperator("REE-PRA_1", "REE");
        assertRemedialActionImportedWithOperator("RTE-PRA_1", "RTE");
        assertRemedialActionImportedWithOperator("REN-PRA_1", "REN");
        assertRemedialActionNotImported("RA-Series-2", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_5", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_6", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_7", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_8", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_9", ImportStatus.INCOMPLETE_DATA);
        assertRemedialActionNotImported("PRA_10", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_11", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_12", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_13", ImportStatus.INCOMPLETE_DATA);
        assertRemedialActionNotImported("PRA_14", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_16", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_17", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_18", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_19", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_20", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_21", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK);
        assertPstRangeActionImported("PRA_22", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", true);
        assertEquals(5, importedCrac.getPstRangeActions().size());
    }

    @Test
    void testImportNetworkActions() throws IOException {
        setUpWithTimestamp("/cracs/CIM_21_4_1.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"));
        assertNetworkActionImported("PRA_1", Set.of("_e8a7eaec-51d6-4571-b3d9-c36d52073c33", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", "_b94318f6-6d24-4f56-96b9-df2531ad6543", "_2184f365-8cd5-4b5d-8a28-9d68603bb6a4"), false);
        assertRemedialActionImportedWithOperator("PRA_1", "PRA_1");
        assertRemedialActionImportedWithOperator("REE-PRA_1", "REE");
        assertRemedialActionImportedWithOperator("RTE-PRA_1", "RTE");
        assertRemedialActionImportedWithOperator("REN-PRA_1", "REN");
        // Pst Setpoint
        assertRemedialActionNotImported("PRA_2", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_3", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_4", ImportStatus.INCOMPLETE_DATA);
        assertRemedialActionNotImported("PRA_5", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_6", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK);
        // Injection Setpoint
        assertNetworkActionImported("PRA_7", Set.of("_1dc9afba-23b5-41a0-8540-b479ed8baf4b", "_2844585c-0d35-488d-a449-685bcd57afbf"), false);
        assertRemedialActionNotImported("PRA_8", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_9", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_10", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_11", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_12", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_13", ImportStatus.INCOMPLETE_DATA);
        assertRemedialActionNotImported("PRA_14", ImportStatus.INCOMPLETE_DATA);
        assertRemedialActionNotImported("PRA_15", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_16", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK);
        // Topological
        assertNetworkActionImported("PRA_17", Set.of("_ffbabc27-1ccd-4fdc-b037-e341706c8d29", "_b58bf21a-096a-4dae-9a01-3f03b60c24c7", "_f04ec73d-b94a-4b7e-a3d6-b1234fc37385_SW_fict", "_5a094c9f-0af5-48dc-94e9-89c6c220023c"), false);
        assertRemedialActionNotImported("PRA_18", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_19", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_20", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_21", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_22", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_23", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("PRA_24", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK);
        assertRemedialActionNotImported("PRA_25", ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO);
        // Mix
        assertNetworkActionImported("PRA_26", Set.of("_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", "_2844585c-0d35-488d-a449-685bcd57afbf", "_ffbabc27-1ccd-4fdc-b037-e341706c8d29"), false);
        assertRemedialActionNotImported("PRA_27", ImportStatus.INCONSISTENCY_IN_DATA);
    }

    @Test
    void testImportHvdcRangeActions() throws IOException {
        setUpWithSpeed("/cracs/CIM_21_6_1.xml", hvdcNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), Set.of(new RangeActionSpeed("BBE2AA11 FFR3AA11 1", 1), new RangeActionSpeed("BBE2AA12 FFR3AA12 1", 2)));

        // RA-Series-2
        assertRemedialActionNotImported("HVDC-direction21", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC-direction22", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC-direction2", ImportStatus.INCONSISTENCY_IN_DATA);

        // RA-Series-3
        assertRemedialActionNotImported("HVDC-direction31", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC-direction32", ImportStatus.INCONSISTENCY_IN_DATA);

        // RA-Series-4
        assertRemedialActionNotImported("HVDC-direction41", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC-direction42", ImportStatus.INCONSISTENCY_IN_DATA);

        // RA-Series-5
        assertRemedialActionNotImported("HVDC-direction51", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC-direction52", ImportStatus.INCONSISTENCY_IN_DATA);

        // RA-Series-6
        assertRemedialActionNotImported("HVDC-direction61", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC-direction62", ImportStatus.INCONSISTENCY_IN_DATA);

        // RA-Series-7
        assertRemedialActionNotImported("HVDC-direction71", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC-direction72", ImportStatus.INCONSISTENCY_IN_DATA);

        // RA-Series-8
        assertRemedialActionNotImported("HVDC-direction81", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC-direction82", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK);

        // RA-Series-9
        assertRemedialActionNotImported("HVDC-direction91", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC-direction92", ImportStatus.INCONSISTENCY_IN_DATA);

        Set<String> createdIds1 = Set.of("HVDC-direction11 + HVDC-direction12 - BBE2AA12 FFR3AA12 1", "HVDC-direction11 + HVDC-direction12 - BBE2AA11 FFR3AA11 1");
        Set<String> createdIds2 = Set.of("HVDC-direction11 + HVDC-direction12 - BBE2AA12 FFR3AA12 1", "HVDC-direction11 + HVDC-direction12 - BBE2AA11 FFR3AA11 1");
        assertHvdcRangeActionImported("HVDC-direction11", createdIds1, Set.of("BBE2AA11 FFR3AA11 1", "BBE2AA12 FFR3AA12 1"), Set.of("HVDC"), false);
        assertHvdcRangeActionImported("HVDC-direction12", createdIds2, Set.of("BBE2AA11 FFR3AA11 1", "BBE2AA12 FFR3AA12 1"), Set.of("HVDC"), true);
        assertEquals("BBE2AA11 FFR3AA11 1 + BBE2AA12 FFR3AA12 1", importedCrac.getHvdcRangeAction("HVDC-direction11 + HVDC-direction12 - BBE2AA12 FFR3AA12 1").getGroupId().get());
        assertEquals("BBE2AA11 FFR3AA11 1 + BBE2AA12 FFR3AA12 1", importedCrac.getHvdcRangeAction("HVDC-direction11 + HVDC-direction12 - BBE2AA11 FFR3AA11 1").getGroupId().get());
    }

    @Test
    void testImportKOHvdcRangeActions() throws IOException {
        setUpWithSpeed("/cracs/CIM_21_6_1.xml", hvdcNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), null);
        assertRemedialActionNotImported("HVDC-direction11", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC-direction12", ImportStatus.INCONSISTENCY_IN_DATA);
    }

    @Test
    void testImportAlignedRangeActions() throws IOException {
        setUpWithGroupId("/cracs/CIM_21_3_1.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), List.of(List.of("PRA_1", "PRA_22")));
        assertPstRangeActionImported("PRA_1", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", false);
        assertPstRangeActionImported("PRA_22", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", true);
        assertEquals(5, importedCrac.getPstRangeActions().size());
        assertTrue(importedCrac.getPstRangeAction("PRA_1").getGroupId().isPresent());
        assertTrue(importedCrac.getPstRangeAction("PRA_22").getGroupId().isPresent());
        assertEquals("PRA_1 + PRA_22", importedCrac.getPstRangeAction("PRA_1").getGroupId().get());
        assertEquals("PRA_1 + PRA_22", importedCrac.getPstRangeAction("PRA_22").getGroupId().get());
    }

    @Test
    void testImportAlignedRangeActionsGroupIdNull() throws IOException {
        List<String> groupIds = new ArrayList<>();
        groupIds.add(null);
        setUpWithGroupId("/cracs/CIM_21_3_1.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), List.of(groupIds));
        assertPstRangeActionImported("PRA_1", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", false);
        assertPstRangeActionImported("PRA_22", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", true);
        assertEquals(5, importedCrac.getPstRangeActions().size());
        assertFalse(importedCrac.getPstRangeAction("PRA_1").getGroupId().isPresent());
        assertFalse(importedCrac.getPstRangeAction("PRA_22").getGroupId().isPresent());
    }

    @Test
    void testImportAlignedRangeActionsGroupIdAlreadyDefined() throws IOException {
        setUpWithGroupId("/cracs/CIM_21_3_1.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), List.of(List.of("PRA_1", "PRA_22"), List.of("PRA_1")));
        assertRemedialActionNotImported("PRA_1", ImportStatus.INCONSISTENCY_IN_DATA);
        assertPstRangeActionImported("PRA_22", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", true);
        assertEquals(4, importedCrac.getPstRangeActions().size());
        assertTrue(importedCrac.getPstRangeAction("PRA_22").getGroupId().isPresent());
        assertEquals("PRA_1 + PRA_22", importedCrac.getPstRangeAction("PRA_22").getGroupId().get());
    }

    @Test
    void testImportOnFlowConstraintUsageRules() throws IOException {
        setUpWithSpeed("/cracs/CIM_21_5_1.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), Set.of(new RangeActionSpeed("AUTO_1", 1)));

        // PRA_1
        assertPstRangeActionImported("PRA_1", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", false);
        PstRangeAction pra1 = importedCrac.getPstRangeAction("PRA_1");
        assertEquals(10, pra1.getUsageRules().size());
        assertHasOnFlowConstraintUsageRule(pra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - ONE - preventive");
        assertHasOnFlowConstraintUsageRule(pra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - ONE - Co-one-1 - outage");
        assertHasOnFlowConstraintUsageRule(pra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - ONE - Co-one-1 - auto");
        assertHasOnFlowConstraintUsageRule(pra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - ONE - Co-one-1 - curative");
        assertHasOnFlowConstraintUsageRule(pra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - ONE - Co-one-2 - outage");
        assertHasOnFlowConstraintUsageRule(pra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - ONE - Co-one-2 - auto");
        assertHasOnFlowConstraintUsageRule(pra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - ONE - Co-one-2 - curative");
        assertHasOnFlowConstraintUsageRule(pra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - ONE - Co-one-3 - outage");
        assertHasOnFlowConstraintUsageRule(pra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - ONE - Co-one-3 - auto");
        assertHasOnFlowConstraintUsageRule(pra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - ONE - Co-one-3 - curative");
        assertEquals(1, pra1.getRanges().size());
        assertEquals(RangeType.ABSOLUTE, pra1.getRanges().get(0).getRangeType());
        assertEquals(1, pra1.getRanges().get(0).getMinTap());
        assertEquals(33, pra1.getRanges().get(0).getMaxTap());
        assertEquals(10, pra1.getInitialTap());

        // PRA_CRA_1
        assertPstRangeActionImported("PRA_CRA_1", "_e8a7eaec-51d6-4571-b3d9-c36d52073c33", true);
        PstRangeAction praCra1 = importedCrac.getPstRangeAction("PRA_CRA_1");
        assertEquals(8, praCra1.getUsageRules().size());
        assertHasOnFlowConstraintUsageRule(praCra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - ONE - Co-one-2 - outage");
        assertHasOnFlowConstraintUsageRule(praCra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - ONE - Co-one-2 - auto");
        assertHasOnFlowConstraintUsageRule(praCra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - ONE - Co-one-2 - curative");
        assertHasOnFlowConstraintUsageRule(praCra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - ONE - Co-one-3 - outage");
        assertHasOnFlowConstraintUsageRule(praCra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - ONE - Co-one-3 - auto");
        assertHasOnFlowConstraintUsageRule(praCra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - ONE - Co-one-3 - curative");
        assertHasOnFlowConstraintUsageRule(praCra1, curativeInstant, "GHIOL_QSDFGH_1_220 - ONE - Co-one-2 - curative");
        assertHasOnFlowConstraintUsageRule(praCra1, curativeInstant, "GHIOL_QSDFGH_1_220 - ONE - Co-one-3 - curative");
        assertEquals(1, praCra1.getRanges().size());
        assertEquals(RangeType.RELATIVE_TO_INITIAL_NETWORK, praCra1.getRanges().get(0).getRangeType());
        assertEquals(-10, praCra1.getRanges().get(0).getMinTap());
        assertEquals(10, praCra1.getRanges().get(0).getMaxTap());
        assertEquals(8, praCra1.getInitialTap());

        // AUTO_1
        assertPstRangeActionImported("AUTO_1", "_e8a7eaec-51d6-4571-b3d9-c36d52073c33", true);
        PstRangeAction auto1 = importedCrac.getPstRangeAction("AUTO_1");
        assertEquals(4, auto1.getUsageRules().size());
        assertHasOnFlowConstraintUsageRule(auto1, autoInstant, "GHIOL_QSDFGH_1_220 - ONE - Co-one-2 - auto");
        assertHasOnFlowConstraintUsageRule(auto1, autoInstant, "GHIOL_QSDFGH_1_220 - ONE - Co-one-2 - curative");
        assertHasOnFlowConstraintUsageRule(auto1, autoInstant, "GHIOL_QSDFGH_1_220 - ONE - Co-one-3 - auto");
        assertHasOnFlowConstraintUsageRule(auto1, autoInstant, "GHIOL_QSDFGH_1_220 - ONE - Co-one-3 - curative");
        assertEquals(1, auto1.getRanges().size());
        assertEquals(RangeType.RELATIVE_TO_INITIAL_NETWORK, auto1.getRanges().get(0).getRangeType());
        assertEquals(-10, auto1.getRanges().get(0).getMinTap());
        assertEquals(10, auto1.getRanges().get(0).getMaxTap());
        assertEquals(8, auto1.getInitialTap());
    }

    @Test
    void testImportRasAvailableForSpecificCountry() throws IOException {
        setUpWithTimestamp("/cracs/CIM_21_5_2.xml", baseNetwork, OffsetDateTime.parse("2021-04-02T20:00Z"));

        // RA_1
        assertNetworkActionImported("RA_1", Set.of("_2844585c-0d35-488d-a449-685bcd57afbf", "_ffbabc27-1ccd-4fdc-b037-e341706c8d29"), false);
        NetworkAction ra1 = importedCrac.getNetworkAction("RA_1");
        assertEquals(1, ra1.getUsageRules().size());
        assertTrue(ra1.getUsageRules().iterator().next() instanceof OnFlowConstraintInCountry);
        assertEquals(preventiveInstant, ra1.getUsageRules().iterator().next().getInstant());
        assertEquals(Country.PT, ((OnFlowConstraintInCountry) ra1.getUsageRules().iterator().next()).getCountry());
        assertEquals(2, ra1.getElementaryActions().size());
        assertTrue(ra1.getElementaryActions().stream()
            .filter(GeneratorAction.class::isInstance)
            .map(GeneratorAction.class::cast)
            .anyMatch(is -> is.getGeneratorId().equals("_2844585c-0d35-488d-a449-685bcd57afbf") && is.getActivePowerValue().getAsDouble() == 380)
        );
        assertTrue(ra1.getElementaryActions().stream()
            .filter(TerminalsConnectionAction.class::isInstance)
            .map(TerminalsConnectionAction.class::cast)
            .anyMatch(ta -> ta.getElementId().equals("_ffbabc27-1ccd-4fdc-b037-e341706c8d29") && !ta.isOpen())
        );

        // RA_2
        assertNetworkActionImported("RA_2", Set.of("_e8a7eaec-51d6-4571-b3d9-c36d52073c33", "_b58bf21a-096a-4dae-9a01-3f03b60c24c7"), false);
        NetworkAction ra2 = importedCrac.getNetworkAction("RA_2");
        assertEquals(1, ra2.getUsageRules().size());
        assertTrue(ra2.getUsageRules().iterator().next() instanceof OnFlowConstraintInCountry);
        assertEquals(curativeInstant, ra2.getUsageRules().iterator().next().getInstant());
        assertEquals(Country.ES, ((OnFlowConstraintInCountry) ra2.getUsageRules().iterator().next()).getCountry());
        assertEquals(2, ra2.getElementaryActions().size());
        assertTrue(ra2.getElementaryActions().stream()
            .filter(PhaseTapChangerTapPositionAction.class::isInstance)
            .map(PhaseTapChangerTapPositionAction.class::cast)
            .anyMatch(ps -> ps.getTransformerId().equals("_e8a7eaec-51d6-4571-b3d9-c36d52073c33") && ps.getTapPosition() == -19) // before it was the tap position before normalization, now it is after normalization
        );
        assertTrue(ra2.getElementaryActions().stream()
            .filter(TerminalsConnectionAction.class::isInstance)
            .map(TerminalsConnectionAction.class::cast)
            .anyMatch(ta -> ta.getElementId().equals("_b58bf21a-096a-4dae-9a01-3f03b60c24c7") && ta.isOpen())
        );

        // RA_3
        assertNetworkActionImported("RA_3", Set.of("_b94318f6-6d24-4f56-96b9-df2531ad6543", "_1dc9afba-23b5-41a0-8540-b479ed8baf4b"), false);
        NetworkAction ra3 = importedCrac.getNetworkAction("RA_3");
        assertEquals(2, ra3.getUsageRules().size());
        assertTrue(
            ra3.getUsageRules().stream()
                .filter(OnInstant.class::isInstance)
                .map(OnInstant.class::cast)
                .anyMatch(ur -> ur.getInstant().isPreventive())
        );
        assertTrue(
            ra3.getUsageRules().stream()
                .filter(OnContingencyState.class::isInstance)
                .map(OnContingencyState.class::cast)
                .anyMatch(ur -> ur.getInstant().isCurative() && ur.getContingency().getId().equals("CO_1"))
        );
        assertEquals(2, ra3.getElementaryActions().size());
        assertTrue(ra3.getElementaryActions().stream()
            .filter(PhaseTapChangerTapPositionAction.class::isInstance)
            .map(PhaseTapChangerTapPositionAction.class::cast)
            .anyMatch(ps -> ps.getTransformerId().equals("_b94318f6-6d24-4f56-96b9-df2531ad6543") && ps.getTapPosition() == 13) // before it was the tap position before normalization, now it is after normalization
        );
        assertTrue(ra3.getElementaryActions().stream()
            .filter(GeneratorAction.class::isInstance)
            .map(GeneratorAction.class::cast)
            .anyMatch(is -> is.getGeneratorId().equals("_1dc9afba-23b5-41a0-8540-b479ed8baf4b") && is.getActivePowerValue().getAsDouble() == 480)
        );

        // RA_4
        assertNetworkActionImported("RA_4", Set.of("_b94318f6-6d24-4f56-96b9-df2531ad6543", "_1dc9afba-23b5-41a0-8540-b479ed8baf4b"), false);
        NetworkAction ra4 = importedCrac.getNetworkAction("RA_4");
        assertEquals(2, ra4.getUsageRules().size());
        assertTrue(
            ra4.getUsageRules().stream()
                .filter(OnFlowConstraintInCountry.class::isInstance)
                .map(OnFlowConstraintInCountry.class::cast)
                .anyMatch(ur -> ur.getInstant().isPreventive() && ur.getContingency().isEmpty() && ur.getCountry().equals(Country.FR))
        );
        assertTrue(
            ra4.getUsageRules().stream()
                .filter(OnFlowConstraintInCountry.class::isInstance)
                .map(OnFlowConstraintInCountry.class::cast)
                .anyMatch(ur -> ur.getInstant().isCurative() && ur.getContingency().orElseThrow().getId().equals("CO_1") && ur.getCountry().equals(Country.FR))
        );
        assertEquals(2, ra4.getElementaryActions().size());
        assertTrue(ra4.getElementaryActions().stream()
            .filter(PhaseTapChangerTapPositionAction.class::isInstance)
            .map(PhaseTapChangerTapPositionAction.class::cast)
            .anyMatch(ps -> ps.getTransformerId().equals("_b94318f6-6d24-4f56-96b9-df2531ad6543") && ps.getTapPosition() == 13) // before it was the tap position before normalization, now it is after normalization
        );
        assertTrue(ra4.getElementaryActions().stream()
            .filter(GeneratorAction.class::isInstance)
            .map(GeneratorAction.class::cast)
            .anyMatch(is -> is.getGeneratorId().equals("_1dc9afba-23b5-41a0-8540-b479ed8baf4b") && is.getActivePowerValue().getAsDouble() == 480)
        );

        // RA_5
        assertNetworkActionImported("RA_4", Set.of("_b94318f6-6d24-4f56-96b9-df2531ad6543", "_1dc9afba-23b5-41a0-8540-b479ed8baf4b"), false);
        NetworkAction ra5 = importedCrac.getNetworkAction("RA_5");
        assertEquals(3, ra5.getUsageRules().size());
        assertTrue(
            ra5.getUsageRules().stream()
                .filter(OnFlowConstraintInCountry.class::isInstance)
                .map(OnFlowConstraintInCountry.class::cast)
                .anyMatch(ur -> ur.getInstant().isPreventive() && ur.getContingency().isEmpty() && ur.getCountry().equals(Country.FR))
        );
        assertTrue(
            ra5.getUsageRules().stream()
                .filter(OnFlowConstraintInCountry.class::isInstance)
                .map(OnFlowConstraintInCountry.class::cast)
                .anyMatch(ur -> ur.getInstant().isCurative() && ur.getContingency().orElseThrow().getId().equals("CO_1") && ur.getCountry().equals(Country.FR))
        );
        assertTrue(
            ra5.getUsageRules().stream()
                .filter(OnFlowConstraintInCountry.class::isInstance)
                .map(OnFlowConstraintInCountry.class::cast)
                .anyMatch(ur -> ur.getInstant().isCurative() && ur.getContingency().orElseThrow().getId().equals("CO_2") && ur.getCountry().equals(Country.FR))
        );
        assertEquals(2, ra5.getElementaryActions().size());
        assertTrue(ra5.getElementaryActions().stream()
            .filter(PhaseTapChangerTapPositionAction.class::isInstance)
            .map(PhaseTapChangerTapPositionAction.class::cast)
            .anyMatch(ps -> ps.getTransformerId().equals("_b94318f6-6d24-4f56-96b9-df2531ad6543") && ps.getTapPosition() == 13) // before it was the tap position before normalization, now it is after normalization
        );
        assertTrue(ra5.getElementaryActions().stream()
            .filter(GeneratorAction.class::isInstance)
            .map(GeneratorAction.class::cast)
            .anyMatch(is -> is.getGeneratorId().equals("_1dc9afba-23b5-41a0-8540-b479ed8baf4b") && is.getActivePowerValue().getAsDouble() == 480)
        );
    }

    @Test
    void testImportOnFlowConstraintRepeatedRa() throws IOException {
        setUpWithTimestamp("/cracs/CIM_21_5_3.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"));

        // PRA_CRA_1
        assertPstRangeActionImported("PRA_CRA_1", "_e8a7eaec-51d6-4571-b3d9-c36d52073c33", true);
        PstRangeAction praCra1 = importedCrac.getPstRangeAction("PRA_CRA_1");
        assertEquals(8, praCra1.getUsageRules().size());
        assertHasOnFlowConstraintUsageRule(praCra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - TWO - Co-one-2 - outage");
        assertHasOnFlowConstraintUsageRule(praCra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - TWO - Co-one-2 - auto");
        assertHasOnFlowConstraintUsageRule(praCra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - TWO - Co-one-2 - curative");
        assertHasOnFlowConstraintUsageRule(praCra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - TWO - Co-one-3 - outage");
        assertHasOnFlowConstraintUsageRule(praCra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - TWO - Co-one-3 - auto");
        assertHasOnFlowConstraintUsageRule(praCra1, preventiveInstant, "GHIOL_QSDFGH_1_220 - TWO - Co-one-3 - curative");
        assertHasOnFlowConstraintUsageRule(praCra1, curativeInstant, "GHIOL_QSDFGH_1_220 - TWO - Co-one-2 - curative");
        assertHasOnFlowConstraintUsageRule(praCra1, curativeInstant, "GHIOL_QSDFGH_1_220 - TWO - Co-one-3 - curative");
        assertEquals(1, praCra1.getRanges().size());
        assertEquals(RangeType.RELATIVE_TO_INITIAL_NETWORK, praCra1.getRanges().get(0).getRangeType());
        assertEquals(-10, praCra1.getRanges().get(0).getMinTap());
        assertEquals(10, praCra1.getRanges().get(0).getMaxTap());
        assertEquals(8, praCra1.getInitialTap());
    }

    @Test
    void testImportAngleCnecs() throws IOException {
        setUpWithTimestamp("/cracs/CIM_21_7_1.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"));
        // -- Imported
        // Angle cnec and associated RA imported :
        assertAngleCnecImportedWithContingency("AngleCnec1", "Co-1", Set.of("_8d8a82ba-b5b0-4e94-861a-192af055f2b8", "_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3"), 30.);
        assertNetworkActionImported("RA1", Set.of("_1dc9afba-23b5-41a0-8540-b479ed8baf4b", "_2844585c-0d35-488d-a449-685bcd57afbf"), false);
        assertHasOnAngleUsageRule("RA1", "AngleCnec1");
        assertEquals(1, importedCrac.getRemedialAction("RA1").getUsageRules().size());
        // -- Partially imported
        // Angle cnec without an associated RA :
        assertAngleCnecImportedWithContingency("AngleCnec3", "Co-1", Set.of("_8d8a82ba-b5b0-4e94-861a-192af055f2b8", "_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3"), 40.);
        // Angle cnec with ill defined RA :
        assertAngleCnecImportedWithContingency("AngleCnec11", "Co-1", Set.of("_8d8a82ba-b5b0-4e94-861a-192af055f2b8", "_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3"), 60.);
        assertRemedialActionNotImported("RA11", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK);

        // -- Not imported
        assertAngleCnecNotImported("AngleCnec2", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("RA2", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("Angle4", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("Angle5", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("Angle6", ImportStatus.INCONSISTENCY_IN_DATA);
        assertAngleCnecNotImported("AngleCnec7", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("RA7", ImportStatus.INCONSISTENCY_IN_DATA);
        assertAngleCnecNotImported("AngleCnec8", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK);
        assertRemedialActionNotImported("RA8", ImportStatus.INCONSISTENCY_IN_DATA);
        assertAngleCnecNotImported("AngleCnec9", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("RA9", ImportStatus.INCONSISTENCY_IN_DATA);
        assertAngleCnecNotImported("AngleCnec10", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("RA10", ImportStatus.INCONSISTENCY_IN_DATA);
    }

    @Test
    void testFilterOnTimeseries() throws IOException {
        setUpWithTimeseriesMrids("/cracs/CIM_2_timeseries.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), Collections.emptySet());
        assertEquals(2, importedCrac.getContingencies().size());

        setUpWithTimeseriesMrids("/cracs/CIM_2_timeseries.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), Set.of("TimeSeries1", "TimeSeries2", "TimeSeries3"));
        assertEquals(2, importedCrac.getContingencies().size());
        assertEquals(2, cracCreationContext.getCreationReport().getReport().size());
        assertEquals("[WARN] Requested TimeSeries mRID \"TimeSeries3\" in CimCracCreationParameters was not found in the CRAC file.", cracCreationContext.getCreationReport().getReport().get(0));
        assertEquals("CRAC was successfully imported with 2 contingencies, 0 FlowCNECs, 0 AngleCNECs, 0 VoltageCNECs and 0 remedial actions (0 range actions and 0 network actions).", cracCreationContext.getCreationReport().getReport().get(1));

        setUpWithTimeseriesMrids("/cracs/CIM_2_timeseries.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), Set.of("TimeSeries1"));
        assertEquals(1, importedCrac.getContingencies().size());
        assertNotNull(importedCrac.getContingency("Co-1"));

        setUpWithTimeseriesMrids("/cracs/CIM_2_timeseries.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), Set.of("TimeSeries2"));
        assertEquals(1, importedCrac.getContingencies().size());
        assertNotNull(importedCrac.getContingency("Co-2"));
    }

    @Test
    void testImportCnecsWithSameMsMrid() throws IOException {
        setUpWithTimestamp("/cracs/CIM_21_2_1_mrid.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"));

        assertEquals(10, importedCrac.getFlowCnecs().size());

        // CNEC 3
        assertCnecImported("CNEC-2", Set.of("CNEC-3 - preventive", "CNEC-3 - Co-1 - auto", "CNEC-3 - Co-2 - auto"));
        assertTrue(cracCreationContext.getMonitoredSeriesCreationContext("CNEC-2").isAltered());

        // CNEC 4
        assertCnecImported("CNEC-4", Set.of("CNEC-4 - preventive", "CNEC-4 - Co-1 - curative", "CNEC-4 - Co-2 - curative",
            "CNEC-5 - ONE - MONITORED - preventive", "CNEC-5 - ONE - MONITORED - Co-1 - curative",
            "CNEC-6 - MONITORED - preventive", "CNEC-6 - MONITORED - Co-1 - outage"));
        assertFalse(cracCreationContext.getMonitoredSeriesCreationContext("CNEC-4").isAltered());
    }

    private VoltageThreshold mockVoltageThreshold(Double min, Double max) {
        VoltageThreshold threshold = Mockito.mock(VoltageThreshold.class);
        Mockito.when(threshold.getUnit()).thenReturn(Unit.KILOVOLT);
        Mockito.when(threshold.getMin()).thenReturn(min);
        Mockito.when(threshold.getMax()).thenReturn(max);
        return threshold;
    }

    @Test
    void testImportVoltageCnecs() throws IOException {
        Set<String> monitoredElements = Set.of("_d77b61ef-61aa-4b22-95f6-b56ca080788d", "_2844585c-0d35-488d-a449-685bcd57afbf", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0");

        Map<String, VoltageMonitoredContingenciesAndThresholds> monitoredStatesAndThresholds = Map.of(
            PREVENTIVE_INSTANT_ID, new VoltageMonitoredContingenciesAndThresholds(null, Map.of(220., mockVoltageThreshold(220., 230.))),
            CURATIVE_INSTANT_ID, new VoltageMonitoredContingenciesAndThresholds(Set.of("Co-1-name", "Co-4-name"), Map.of(220., mockVoltageThreshold(210., 240.))),
            OUTAGE_INSTANT_ID, new VoltageMonitoredContingenciesAndThresholds(Set.of("Co-3-name"), Map.of(220., mockVoltageThreshold(200., null))),
            AUTO_INSTANT_ID, new VoltageMonitoredContingenciesAndThresholds(Set.of("Co-2-name"), Map.of(220., mockVoltageThreshold(null, null)))
        );
        VoltageCnecsCreationParameters voltageCnecsCreationParameters = new VoltageCnecsCreationParameters(monitoredStatesAndThresholds, monitoredElements);

        CracCreationParameters params = new CracCreationParameters();
        CimCracCreationParameters cimParams = new CimCracCreationParameters();
        cimParams.setVoltageCnecsCreationParameters(voltageCnecsCreationParameters);
        params.addExtension(CimCracCreationParameters.class, cimParams);
        params.getExtension(CimCracCreationParameters.class).setTimestamp(OffsetDateTime.parse("2021-04-01T23:00Z"));
        setUp("/cracs/CIM_21_1_1.xml", baseNetwork, params);

        assertEquals(3, importedCrac.getVoltageCnecs().size());
        assertNotNull(importedCrac.getVoltageCnec("[VC] _d77b61ef-61aa-4b22-95f6-b56ca080788d - preventive"));
        assertNotNull(importedCrac.getVoltageCnec("[VC] _d77b61ef-61aa-4b22-95f6-b56ca080788d - Co-1 - curative"));
        assertNotNull(importedCrac.getVoltageCnec("[VC] _d77b61ef-61aa-4b22-95f6-b56ca080788d - Co-3 - outage"));
        assertEquals(7, cracCreationContext.getVoltageCnecCreationContexts().size());
        assertEquals(8, cracCreationContext.getCreationReport().getReport().size());
        assertTrue(cracCreationContext.getCreationReport().getReport().contains("[REMOVED] VoltageCnec with network element \"_2844585c-0d35-488d-a449-685bcd57afbf\", instant \"all\" and contingency \"all\" was not imported: INCONSISTENCY_IN_DATA. Element _2844585c-0d35-488d-a449-685bcd57afbf is not a voltage level."));
        assertTrue(cracCreationContext.getCreationReport().getReport().contains("[REMOVED] VoltageCnec with network element \"_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0\", instant \"all\" and contingency \"all\" was not imported: INCONSISTENCY_IN_DATA. Element _a708c3bc-465d-4fe7-b6ef-6fa6408a62b0 is not a voltage level."));
        assertTrue(cracCreationContext.getCreationReport().getReport().contains("[REMOVED] VoltageCnec with network element \"all\", instant \"all\" and contingency \"Co-4-name\" was not imported: OTHER. Contingency does not exist in the CRAC or could not be imported."));
        assertTrue(cracCreationContext.getCreationReport().getReport().contains("[REMOVED] VoltageCnec with network element \"_d77b61ef-61aa-4b22-95f6-b56ca080788d\", instant \"auto\" and contingency \"Co-2-name\" was not imported: INCONSISTENCY_IN_DATA. Cannot add a threshold without min nor max values. Please use withMin() or withMax().."));
    }

    @Test
    void testImportCnecOnRightSide() throws IOException {
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_TWO);
        cracCreationParameters.addExtension(CimCracCreationParameters.class, new CimCracCreationParameters());
        cracCreationParameters.getExtension(CimCracCreationParameters.class).setTimestamp(OffsetDateTime.parse("2021-04-01T23:00Z"));
        setUp("/cracs/CIM_21_2_1.xml", baseNetwork, cracCreationParameters);

        assertEquals(8, importedCrac.getFlowCnecs().size());

        // CNEC 2
        assertCnecNotImported("CNEC-2", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK);

        // CNEC 3
        assertCnecImported("CNEC-3", Set.of("CNEC-3 - preventive", "CNEC-3 - Co-1 - auto", "CNEC-3 - Co-2 - auto"));
        assertHasOneThreshold("CNEC-3 - preventive", TwoSides.TWO, Unit.MEGAWATT, -3, 3);
        assertHasOneThreshold("CNEC-3 - Co-1 - auto", TwoSides.TWO, Unit.AMPERE, -3, 3);
        assertHasOneThreshold("CNEC-3 - Co-2 - auto", TwoSides.TWO, Unit.AMPERE, -3, 3);

        // CNEC 4
        assertCnecImported("CNEC-4", Set.of("CNEC-4 - preventive", "CNEC-4 - Co-1 - curative", "CNEC-4 - Co-2 - curative"));
        assertHasOneThreshold("CNEC-4 - preventive", TwoSides.TWO, Unit.PERCENT_IMAX, -0.04, 0.04);
        assertHasOneThreshold("CNEC-4 - Co-1 - curative", TwoSides.TWO, Unit.PERCENT_IMAX, -0.04, 0.04);
        assertHasOneThreshold("CNEC-4 - Co-2 - curative", TwoSides.TWO, Unit.PERCENT_IMAX, -0.04, 0.04);

        // CNEC 5
        assertCnecImported("MNEC-1", Set.of("CNEC-5 - ONE - MONITORED - preventive", "CNEC-5 - ONE - MONITORED - Co-1 - curative"));
        assertHasOneThreshold("CNEC-5 - ONE - MONITORED - preventive", TwoSides.ONE, Unit.PERCENT_IMAX, -0.05, 0.05);
        assertHasOneThreshold("CNEC-5 - ONE - MONITORED - Co-1 - curative", TwoSides.ONE, Unit.PERCENT_IMAX, -0.05, 0.05);

        // CNEC 6 - Cannot be imported because has no Imax on right side
        assertCnecNotImported("MNEC-2", ImportStatus.OTHER);
    }

    @Test
    void testImportCnecOnBothSides() throws IOException {
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES);
        cracCreationParameters.addExtension(CimCracCreationParameters.class, new CimCracCreationParameters());
        cracCreationParameters.getExtension(CimCracCreationParameters.class).setTimestamp(OffsetDateTime.parse("2021-04-01T23:00Z"));
        setUp("/cracs/CIM_21_2_1.xml", baseNetwork, cracCreationParameters);

        assertEquals(10, importedCrac.getFlowCnecs().size());

        // CNEC 2
        assertCnecNotImported("CNEC-2", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK);

        // CNEC 3
        assertCnecImported("CNEC-3", Set.of("CNEC-3 - preventive", "CNEC-3 - Co-1 - auto", "CNEC-3 - Co-2 - auto"));
        assertHasTwoThresholds("CNEC-3 - preventive", Unit.MEGAWATT, -3, 3);
        assertHasTwoThresholds("CNEC-3 - Co-1 - auto", Unit.AMPERE, -3, 3);
        assertHasTwoThresholds("CNEC-3 - Co-2 - auto", Unit.AMPERE, -3, 3);

        // CNEC 4
        assertCnecImported("CNEC-4", Set.of("CNEC-4 - preventive", "CNEC-4 - Co-1 - curative", "CNEC-4 - Co-2 - curative"));
        assertHasTwoThresholds("CNEC-4 - preventive", Unit.PERCENT_IMAX, -0.04, 0.04);
        assertHasTwoThresholds("CNEC-4 - Co-1 - curative", Unit.PERCENT_IMAX, -0.04, 0.04);
        assertHasTwoThresholds("CNEC-4 - Co-2 - curative", Unit.PERCENT_IMAX, -0.04, 0.04);

        // CNEC 5
        assertCnecImported("MNEC-1", Set.of("CNEC-5 - ONE - MONITORED - preventive", "CNEC-5 - ONE - MONITORED - Co-1 - curative"));
        assertHasOneThreshold("CNEC-5 - ONE - MONITORED - preventive", TwoSides.ONE, Unit.PERCENT_IMAX, -0.05, 0.05);
        assertHasOneThreshold("CNEC-5 - ONE - MONITORED - Co-1 - curative", TwoSides.ONE, Unit.PERCENT_IMAX, -0.05, 0.05);

        // CNEC 6 - Only one threshold because has only Imax on ONE side in network
        assertCnecImported("MNEC-2", Set.of("CNEC-6 - MONITORED - preventive", "CNEC-6 - MONITORED - Co-1 - outage"));
        assertHasOneThreshold("CNEC-6 - MONITORED - preventive", TwoSides.ONE, Unit.PERCENT_IMAX, -0.06, 0.06);
        assertHasOneThreshold("CNEC-6 - MONITORED - Co-1 - outage", TwoSides.ONE, Unit.PERCENT_IMAX, -0.06, 0.06);
    }

    @Test
    void testTransformerCnecThresholds() throws IOException {
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.addExtension(CimCracCreationParameters.class, new CimCracCreationParameters());
        // Preventive threshold is in %Imax, should be created depending on default monitored side
        // Outage threshold is in MW, should be created depending on default monitored side
        // Curative threshold is in A, should be defined on high voltage level side

        cracCreationParameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES);
        cracCreationParameters.getExtension(CimCracCreationParameters.class).setTimestamp(OffsetDateTime.parse("2021-04-02T20:00Z"));
        setUp("/cracs/CIM_21_5_2.xml", baseNetwork, cracCreationParameters);
        assertHasTwoThresholds("OJLJJ_5_400_220 - preventive", Unit.PERCENT_IMAX, -1., 1.);
        assertHasTwoThresholds("OJLJJ_5_400_220 - CO_2 - outage", Unit.MEGAWATT, -1000., 1000.);
        assertHasTwoThresholds("OJLJJ_5_400_220 - CO_3 - outage", Unit.MEGAWATT, -1000., 1000.);
        assertHasOneThreshold("OJLJJ_5_400_220 - CO_2 - curative", TwoSides.TWO, Unit.AMPERE, -2000., 2000.);
        assertHasOneThreshold("OJLJJ_5_400_220 - CO_3 - curative", TwoSides.TWO, Unit.AMPERE, -2000., 2000.);

        cracCreationParameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_ONE);
        cracCreationParameters.getExtension(CimCracCreationParameters.class).setTimestamp(OffsetDateTime.parse("2021-04-02T03:00Z"));
        setUp("/cracs/CIM_21_5_2.xml", baseNetwork, cracCreationParameters);
        assertHasOneThreshold("OJLJJ_5_400_220 - preventive", TwoSides.ONE, Unit.PERCENT_IMAX, -1., 1.);
        assertHasOneThreshold("OJLJJ_5_400_220 - CO_2 - outage", TwoSides.ONE, Unit.MEGAWATT, -1000., 1000.);
        assertHasOneThreshold("OJLJJ_5_400_220 - CO_3 - outage", TwoSides.ONE, Unit.MEGAWATT, -1000., 1000.);
        assertHasOneThreshold("OJLJJ_5_400_220 - CO_2 - curative", TwoSides.TWO, Unit.AMPERE, -2000., 2000.);
        assertHasOneThreshold("OJLJJ_5_400_220 - CO_3 - curative", TwoSides.TWO, Unit.AMPERE, -2000., 2000.);

        cracCreationParameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_TWO);
        cracCreationParameters.getExtension(CimCracCreationParameters.class).setTimestamp(OffsetDateTime.parse("2021-04-02T05:00Z"));
        setUp("/cracs/CIM_21_5_2.xml", baseNetwork, cracCreationParameters);
        assertHasOneThreshold("OJLJJ_5_400_220 - preventive", TwoSides.TWO, Unit.PERCENT_IMAX, -1., 1.);
        assertHasOneThreshold("OJLJJ_5_400_220 - CO_2 - outage", TwoSides.TWO, Unit.MEGAWATT, -1000., 1000.);
        assertHasOneThreshold("OJLJJ_5_400_220 - CO_3 - outage", TwoSides.TWO, Unit.MEGAWATT, -1000., 1000.);
        assertHasOneThreshold("OJLJJ_5_400_220 - CO_2 - curative", TwoSides.TWO, Unit.AMPERE, -2000., 2000.);
        assertHasOneThreshold("OJLJJ_5_400_220 - CO_3 - curative", TwoSides.TWO, Unit.AMPERE, -2000., 2000.);
    }

    private void assertCnecHasOutageDuplicate(String flowCnecId) {
        FlowCnec flowCnec = importedCrac.getFlowCnec(flowCnecId);
        assertNotNull(flowCnec);
        FlowCnec duplicate = importedCrac.getFlowCnec(flowCnec.getId() + " - OUTAGE DUPLICATE");
        assertNotNull(duplicate);
        assertEquals(flowCnec.getNetworkElement().getId(), duplicate.getNetworkElement().getId());
        assertEquals(flowCnec.getState().getContingency(), duplicate.getState().getContingency());
        assertEquals(InstantKind.OUTAGE, duplicate.getState().getInstant().getKind());
        assertEquals(flowCnec.isOptimized(), duplicate.isOptimized());
        assertEquals(flowCnec.isMonitored(), duplicate.isMonitored());
        assertEquals(flowCnec.getReliabilityMargin(), duplicate.getReliabilityMargin(), 1e-6);
        assertEquals(flowCnec.getIMax(TwoSides.ONE), duplicate.getIMax(TwoSides.ONE), 1e-6);
        assertEquals(flowCnec.getIMax(TwoSides.TWO), duplicate.getIMax(TwoSides.TWO), 1e-6);
        assertEquals(flowCnec.getNominalVoltage(TwoSides.ONE), duplicate.getNominalVoltage(TwoSides.ONE), 1e-6);
        assertEquals(flowCnec.getNominalVoltage(TwoSides.TWO), duplicate.getNominalVoltage(TwoSides.TWO), 1e-6);
        assertEquals(flowCnec.getThresholds(), duplicate.getThresholds());
        assertTrue(cracCreationContext.getCreationReport().getReport().contains(String.format("[ADDED] CNEC \"%s\" has no associated automaton. It will be cloned on the OUTAGE instant in order to be secured during preventive RAO.", flowCnecId)));
    }

    @Test
    void importAndDuplicateAutoCnecs() throws IOException {
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES);
        cracCreationParameters.addExtension(CimCracCreationParameters.class, new CimCracCreationParameters());

        cracCreationParameters.getExtension(CimCracCreationParameters.class).setTimestamp(OffsetDateTime.parse("2021-04-01T23:00Z"));
        setUp("/cracs/CIM_21_2_1_ARA.xml", baseNetwork, cracCreationParameters);

        assertEquals(12, importedCrac.getCnecs().size());
        assertCnecHasOutageDuplicate("CNEC-4 - Co-1 - auto");
        assertCnecHasOutageDuplicate("CNEC-4 - Co-2 - auto");
    }

    @Test
    void testPermissiveImports() throws IOException {
        // Test that we can import contingencies from B56 & B57, and CNECs from B56
        setUpWithSpeed("/cracs/CIM_21_5_1_permissive.xml", baseNetwork, OffsetDateTime.parse("2021-04-01T23:00Z"), Set.of(new RangeActionSpeed("AUTO_1", 1)));

        // Contingencies
        assertEquals(3, importedCrac.getContingencies().size());
        assertContingencyImported("Co-one-1", "OIUYTR-QSCV-1 400 kV", Set.of("_ffbabc27-1ccd-4fdc-b037-e341706c8d29"), false);
        assertContingencyImported("Co-one-2", "OIUYTR-LKJHGOI-1 400 kV", Set.of("_b58bf21a-096a-4dae-9a01-3f03b60c24c7"), false);
        assertContingencyImported("Co-one-3", "AZERTY-LKJHG-1 400 kV", Set.of("_df16b3dd-c905-4a6f-84ee-f067be86f5da"), false);

        // FlowCNECs
        assertEquals(14, importedCrac.getFlowCnecs().size());

        assertCnecImported("TUU_MR_31", Set.of(
            "GHIOL_QSDFGH_1_220 - TWO - Co-one-1 - auto", "GHIOL_QSDFGH_1_220 - TWO - preventive", "GHIOL_QSDFGH_1_220 - TWO - Co-one-1 - outage",
            "GHIOL_QSDFGH_1_220 - TWO - Co-one-3 - outage", "GHIOL_QSDFGH_1_220 - TWO - Co-one-3 - curative", "GHIOL_QSDFGH_1_220 - TWO - Co-one-2 - curative",
            "GHIOL_QSDFGH_1_220 - TWO - Co-one-3 - auto", "GHIOL_QSDFGH_1_220 - TWO - Co-one-1 - curative", "GHIOL_QSDFGH_1_220 - TWO - Co-one-2 - auto",
            "GHIOL_QSDFGH_1_220 - TWO - Co-one-2 - outage"
        ));
        assertHasOneThreshold("GHIOL_QSDFGH_1_220 - TWO - preventive", TwoSides.TWO, Unit.PERCENT_IMAX, -1, 1);
        assertHasOneThreshold("GHIOL_QSDFGH_1_220 - TWO - Co-one-1 - outage", TwoSides.TWO, Unit.PERCENT_IMAX, -1.15, 1.15);
        assertHasOneThreshold("GHIOL_QSDFGH_1_220 - TWO - Co-one-2 - auto", TwoSides.TWO, Unit.PERCENT_IMAX, -1.1, 1.1);
        assertHasOneThreshold("GHIOL_QSDFGH_1_220 - TWO - Co-one-3 - curative", TwoSides.TWO, Unit.PERCENT_IMAX, -1.05, 1.05);

        assertCnecImported("TUU_MR_56", Set.of(
            "GHIOL_QSRBJH_1_400 - TWO - Co-one-1 - auto", "GHIOL_QSRBJH_1_400 - TWO - preventive", "GHIOL_QSRBJH_1_400 - TWO - Co-one-1 - outage",
            "GHIOL_QSRBJH_1_400 - TWO - Co-one-1 - curative"
        ));
        assertHasOneThreshold("GHIOL_QSRBJH_1_400 - TWO - preventive", TwoSides.TWO, Unit.PERCENT_IMAX, -1, 1);
        assertHasOneThreshold("GHIOL_QSRBJH_1_400 - TWO - Co-one-1 - outage", TwoSides.TWO, Unit.PERCENT_IMAX, -1.5, 1.5);
        assertHasOneThreshold("GHIOL_QSRBJH_1_400 - TWO - Co-one-1 - auto", TwoSides.TWO, Unit.PERCENT_IMAX, -1.3, 1.3);
        assertHasOneThreshold("GHIOL_QSRBJH_1_400 - TWO - Co-one-1 - curative", TwoSides.TWO, Unit.PERCENT_IMAX, -1.05, 1.05);

        // PRA_1
        assertPstRangeActionImported("PRA_1", "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", false);
        PstRangeAction pra1 = importedCrac.getPstRangeAction("PRA_1");
        assertEquals(10, pra1.getUsageRules().size());

        // PRA_CRA_1
        assertPstRangeActionImported("PRA_CRA_1", "_e8a7eaec-51d6-4571-b3d9-c36d52073c33", true);
        PstRangeAction praCra1 = importedCrac.getPstRangeAction("PRA_CRA_1");
        assertEquals(8, praCra1.getUsageRules().size());

        // AUTO_1
        assertPstRangeActionImported("AUTO_1", "_e8a7eaec-51d6-4571-b3d9-c36d52073c33", true);
        PstRangeAction auto1 = importedCrac.getPstRangeAction("AUTO_1");
        assertEquals(4, auto1.getUsageRules().size());
    }
}
