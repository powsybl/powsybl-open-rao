/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.fbconstraint;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.range.TapRange;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyState;
import com.powsybl.openrao.data.crac.api.usagerule.OnInstant;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.crac.io.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.commons.api.stdcreationcontext.BranchCnecCreationContext;
import com.powsybl.openrao.data.crac.impl.NetworkActionImpl;
import com.powsybl.openrao.data.crac.io.fbconstraint.parameters.FbConstraintCracCreationParameters;
import com.powsybl.openrao.data.crac.loopflowextension.LoopFlowThreshold;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
class FbConstraintCracCreatorTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private CracCreationParameters parameters;
    private FbConstraintCreationContext creationContext;

    @BeforeEach
    public void setUp() {
        parameters = new CracCreationParameters();
        parameters.setCracFactoryName(CracFactory.findDefault().getName());
        parameters.addExtension(FbConstraintCracCreationParameters.class, new FbConstraintCracCreationParameters());
    }

    private void assertCriticalBranchNotImported(String name, ImportStatus importStatus) {
        BranchCnecCreationContext context = creationContext.getBranchCnecCreationContext(name);
        assertNotNull(context);
        assertFalse(context.isImported());
        assertEquals(importStatus, context.getImportStatus());
        assertTrue(context.getCreatedCnecsIds().isEmpty());
    }

    private void assertComplexVariantNotImported(String name, ImportStatus importStatus) {
        ElementaryCreationContext context = creationContext.getRemedialActionCreationContext(name);
        assertNotNull(context);
        assertFalse(context.isImported());
        assertEquals(importStatus, context.getImportStatus());
    }

    @Test
    void importCracWithParameters() throws IOException {
        Network network = Network.read("TestCase12Nodes_with_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase12Nodes_with_Xnodes.uct"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        RaUsageLimits raUsageLimits = new RaUsageLimits();
        raUsageLimits.setMaxRa(12);
        parameters.addRaUsageLimitsForInstant("preventive", raUsageLimits);
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(timestamp);
        creationContext = (FbConstraintCreationContext) Crac.readWithContext("without_RA.xml", getClass().getResourceAsStream("/merged_cb/without_RA.xml"), network, parameters);
        assertTrue(creationContext.isCreationSuccessful());
        assertEquals(12, creationContext.getCrac().getRaUsageLimits(creationContext.getCrac().getInstant("preventive")).getMaxRa());
    }

    @Test
    void importCracWithTimestampFilter() throws IOException {
        Network network = Network.read("TestCase12Nodes_with_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase12Nodes_with_Xnodes.uct"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(timestamp);
        creationContext = (FbConstraintCreationContext) Crac.readWithContext("without_RA.xml", getClass().getResourceAsStream("/merged_cb/without_RA.xml"), network, parameters);
        assertTrue(creationContext.isCreationSuccessful());
        assertEquals(2, creationContext.getCrac().getContingencies().size());
        assertEquals(10, creationContext.getCrac().getFlowCnecs().size());
        assertEquals(5, creationContext.getCrac().getStates().size());

        timestamp = OffsetDateTime.parse("2019-01-08T10:30Z");
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(timestamp);
        creationContext = (FbConstraintCreationContext) Crac.readWithContext("without_RA.xml", getClass().getResourceAsStream("/merged_cb/without_RA.xml"), network, parameters);
        assertTrue(creationContext.isCreationSuccessful());
        assertEquals(3, creationContext.getCrac().getContingencies().size());
        assertEquals(12, creationContext.getCrac().getFlowCnecs().size());
        assertEquals(7, creationContext.getCrac().getStates().size());

        timestamp = OffsetDateTime.parse("2019-01-10T10:30Z");
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(timestamp);
        creationContext = (FbConstraintCreationContext) Crac.readWithContext("without_RA.xml", getClass().getResourceAsStream("/merged_cb/without_RA.xml"), network, parameters);
        assertFalse(creationContext.isCreationSuccessful());
    }

    @Test
    void importCriticalBranches() throws IOException {
        Network network = Network.read("TestCase12Nodes_with_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase12Nodes_with_Xnodes.uct"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T10:30Z");
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(timestamp);
        creationContext = (FbConstraintCreationContext) Crac.readWithContext("without_RA.xml", getClass().getResourceAsStream("/merged_cb/without_RA.xml"), network, parameters);
        Crac crac = creationContext.getCrac();

        // BE_CBCO_000001
        assertTrue(creationContext.getBranchCnecCreationContext("BE_CBCO_000001").isImported());
        assertTrue(creationContext.getBranchCnecCreationContext("BE_CBCO_000001").isBaseCase());
        assertEquals(1, creationContext.getBranchCnecCreationContext("BE_CBCO_000001").getCreatedCnecsIds().size());
        assertEquals("BE_CBCO_000001 - preventive", creationContext.getBranchCnecCreationContext("BE_CBCO_000001").getCreatedCnecsIds().get(PREVENTIVE_INSTANT_ID));

        assertNotNull(crac.getFlowCnec("BE_CBCO_000001 - preventive"));
        assertEquals("[BE-BE] BBE1 - BBE2 [DIR]", crac.getFlowCnec("BE_CBCO_000001 - preventive").getName());
        assertEquals("BE", crac.getFlowCnec("BE_CBCO_000001 - preventive").getOperator());
        assertEquals("BBE1AA1  BBE2AA1  1", crac.getFlowCnec("BE_CBCO_000001 - preventive").getNetworkElement().getId());
        assertEquals(138., crac.getFlowCnec("BE_CBCO_000001 - preventive").getReliabilityMargin(), 1e-6);
        assertEquals(crac.getPreventiveState(), crac.getFlowCnec("BE_CBCO_000001 - preventive").getState());

        // BE_CBCO_000003
        assertTrue(creationContext.getBranchCnecCreationContext("BE_CBCO_000003").isImported());
        assertFalse(creationContext.getBranchCnecCreationContext("BE_CBCO_000003").isBaseCase());
        assertEquals(2, creationContext.getBranchCnecCreationContext("BE_CBCO_000003").getCreatedCnecsIds().size());
        assertEquals("BE_CBCO_000003 - outage", creationContext.getBranchCnecCreationContext("BE_CBCO_000003").getCreatedCnecsIds().get(OUTAGE_INSTANT_ID));
        assertEquals("BE_CBCO_000003 - curative", creationContext.getBranchCnecCreationContext("BE_CBCO_000003").getCreatedCnecsIds().get(CURATIVE_INSTANT_ID));

        assertNotNull(crac.getFlowCnec("BE_CBCO_000003 - outage"));
        assertEquals("[BE-BE] BBE3 - BBE2 [DIR]", crac.getFlowCnec("BE_CBCO_000003 - outage").getName());
        assertEquals("BE", crac.getFlowCnec("BE_CBCO_000003 - outage").getOperator());
        assertEquals("BBE2AA1  BBE3AA1  1", crac.getFlowCnec("BE_CBCO_000003 - outage").getNetworkElement().getId());
        assertEquals(150., crac.getFlowCnec("BE_CBCO_000003 - outage").getReliabilityMargin(), 1e-6);
        assertEquals(OUTAGE_INSTANT_ID, crac.getFlowCnec("BE_CBCO_000003 - outage").getState().getInstant().toString());
        assertEquals("BE_CO_00001", crac.getFlowCnec("BE_CBCO_000003 - outage").getState().getContingency().orElseThrow().getId());

        assertNotNull(crac.getFlowCnec("BE_CBCO_000003 - curative"));
        assertEquals("[BE-BE] BBE3 - BBE2 [DIR]", crac.getFlowCnec("BE_CBCO_000003 - curative").getName());
        assertEquals("BE", crac.getFlowCnec("BE_CBCO_000003 - curative").getOperator());
        assertEquals("BBE2AA1  BBE3AA1  1", crac.getFlowCnec("BE_CBCO_000003 - curative").getNetworkElement().getId());
        assertEquals(150., crac.getFlowCnec("BE_CBCO_000003 - curative").getReliabilityMargin(), 1e-6);
        assertEquals(CURATIVE_INSTANT_ID, crac.getFlowCnec("BE_CBCO_000003 - curative").getState().getInstant().toString());
        assertEquals("BE_CO_00001", crac.getFlowCnec("BE_CBCO_000003 - curative").getState().getContingency().orElseThrow().getId());

        // number of critical branches vs. number of Cnecs
        assertEquals(7, creationContext.getBranchCnecCreationContexts().size());  // 2 preventive, 5 curative
        assertTrue(creationContext.getBranchCnecCreationContexts().stream().allMatch(BranchCnecCreationContext::isImported));
        assertEquals(12, crac.getFlowCnecs().size());  // 2 + 5*2
    }

    @Test
    void importComplexVariants() throws IOException {
        Network network = Network.read("TestCase12Nodes_with_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase12Nodes_with_Xnodes.uct"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(timestamp);
        creationContext = (FbConstraintCreationContext) Crac.readWithContext("with_RA.xml", getClass().getResourceAsStream("/merged_cb/with_RA.xml"), network, parameters);
        Crac crac = creationContext.getCrac();

        // CNECs
        assertEquals(6, crac.getFlowCnecs().size());

        // PST
        assertEquals(1, crac.getPstRangeActions().size());
        PstRangeAction rangeAction = crac.getPstRangeActions().iterator().next();
        assertEquals("RA_BE_0001", rangeAction.getId());
        assertEquals("PRA_PST_BE", rangeAction.getName());
        assertEquals("BE", rangeAction.getOperator());
        assertEquals(1, rangeAction.getUsageRules().size());
        assertEquals(UsageMethod.AVAILABLE, rangeAction.getUsageRules().iterator().next().getUsageMethod());
        assertTrue(rangeAction.getUsageRules().iterator().next() instanceof OnInstant);
        assertEquals(crac.getPreventiveState().getInstant(), rangeAction.getUsageRules().iterator().next().getInstant());
        assertTrue(rangeAction.getGroupId().isPresent());
        assertEquals("1", rangeAction.getGroupId().get());
        assertEquals(1, rangeAction.getRanges().size());
        TapRange absoluteRange = rangeAction.getRanges().stream().filter(range -> range.getRangeType().equals(RangeType.ABSOLUTE)).findAny().orElse(null);
        assertNotNull(absoluteRange);
        assertEquals(6, absoluteRange.getMaxTap());
        assertEquals(-6, absoluteRange.getMinTap());

        // TOPOs
        assertEquals(2, crac.getNetworkActions().size());

        // TOPO PRA
        NetworkAction topoPra = crac.getNetworkAction("RA_FR_0001");
        assertEquals(2, topoPra.getNetworkElements().size());
        assertEquals("PRA_TOPO_FR", topoPra.getName());
        assertEquals("FR", topoPra.getOperator());
        assertEquals(1, topoPra.getUsageRules().size());
        assertEquals(UsageMethod.AVAILABLE, topoPra.getUsageRules().iterator().next().getUsageMethod());
        assertTrue(topoPra.getUsageRules().iterator().next() instanceof OnInstant);
        assertEquals(crac.getPreventiveState().getInstant(), topoPra.getUsageRules().iterator().next().getInstant());
        assertEquals(NetworkActionImpl.class, topoPra.getClass());
        assertEquals(2, topoPra.getElementaryActions().size());

        // TOPO CRA
        NetworkAction topoCra = crac.getNetworkAction("RA_FR_0002");
        assertEquals(1, topoCra.getNetworkElements().size());
        assertEquals("CRA_TOPO_FR", topoCra.getName());
        assertEquals("FR", topoCra.getOperator());
        assertEquals(2, topoCra.getUsageRules().size());
        assertEquals(UsageMethod.AVAILABLE, topoCra.getUsageRules().iterator().next().getUsageMethod());
        assertTrue(topoCra.getUsageRules().iterator().next() instanceof OnContingencyState);
        assertEquals(NetworkActionImpl.class, topoCra.getClass());
        assertEquals(1, topoCra.getElementaryActions().size());

        // Creation Context
        assertEquals(3, creationContext.getRemedialActionCreationContexts().size());
        assertTrue(creationContext.getRemedialActionCreationContext("RA_BE_0001").isImported());
        assertTrue(creationContext.getRemedialActionCreationContext("RA_FR_0001").isImported());
        assertTrue(creationContext.getRemedialActionCreationContext("RA_FR_0002").isImported());

        ElementaryCreationContext pstContext = creationContext.getRemedialActionCreationContext("RA_BE_0001");
        assertNotNull(pstContext);
        assertEquals(ImportStatus.IMPORTED, pstContext.getImportStatus());
        assertEquals("RA_BE_0001", pstContext.getNativeObjectId());
        assertEquals("RA_BE_0001", pstContext.getCreatedObjectId());
        assertTrue(pstContext.isImported());
        assertFalse(pstContext.isAltered());
        assertTrue(pstContext instanceof PstComplexVariantCreationContext);
        assertFalse(((PstComplexVariantCreationContext) pstContext).isInverted());
        assertEquals("BBE2AA1  BBE3AA1  1", ((PstComplexVariantCreationContext) pstContext).getNativeNetworkElementId());

        ElementaryCreationContext topoContext = creationContext.getRemedialActionCreationContext("RA_FR_0001");
        assertNotNull(topoContext);
        assertEquals(ImportStatus.IMPORTED, topoContext.getImportStatus());
        assertEquals("RA_FR_0001", topoContext.getNativeObjectId());
        assertEquals("RA_FR_0001", topoContext.getCreatedObjectId());
        assertTrue(topoContext.isImported());
        assertFalse(topoContext.isAltered());
    }

    @Test
    void importMnecs() throws IOException {
        Network network = Network.read("TestCase12Nodes_with_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase12Nodes_with_Xnodes.uct"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(timestamp);
        creationContext = (FbConstraintCreationContext) Crac.readWithContext("MNEC_test.xml", getClass().getResourceAsStream("/merged_cb/MNEC_test.xml"), network, parameters);
        Crac crac = creationContext.getCrac();

        assertEquals(3, crac.getFlowCnecs().size());
        assertTrue(crac.getFlowCnec("BE_CBCO_000001 - preventive").isOptimized());
        assertFalse(crac.getFlowCnec("BE_CBCO_000001 - preventive").isMonitored());
        assertFalse(crac.getFlowCnec("BE_CBCO_000002 - preventive").isOptimized());
        assertTrue(crac.getFlowCnec("BE_CBCO_000002 - preventive").isMonitored());
        assertTrue(crac.getFlowCnec("BE_CBCO_000003 - preventive").isOptimized());
        assertTrue(crac.getFlowCnec("BE_CBCO_000003 - preventive").isMonitored());

        assertCriticalBranchNotImported("BE_CBCO_000004", ImportStatus.NOT_FOR_RAO);
    }

    @Test
    void importWithoutMnecs() throws IOException {
        Network network = Network.read("TestCase12Nodes_with_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase12Nodes_with_Xnodes.uct"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(timestamp);
        creationContext = (FbConstraintCreationContext) Crac.readWithContext("no_MNEC_test.xml", getClass().getResourceAsStream("/merged_cb/no_MNEC_test.xml"), network, parameters);
        Crac crac = creationContext.getCrac();

        assertEquals(1, crac.getFlowCnecs().size());
        assertTrue(crac.getFlowCnec("BE_CBCO_000001 - preventive").isOptimized());
        assertFalse(crac.getFlowCnec("BE_CBCO_000001 - preventive").isMonitored());

        assertCriticalBranchNotImported("BE_CBCO_000002", ImportStatus.NOT_FOR_RAO);
    }

    private void assertHasThresholds(FlowCnec cnec, Set<TwoSides> monitoredSides, Unit unit, Double min, Double max) {
        assertEquals(monitoredSides.size(), cnec.getThresholds().size());
        monitoredSides.forEach(side -> assertTrue(cnec.getThresholds().stream().anyMatch(branchThreshold -> branchThreshold.getSide().equals(side))));
        cnec.getThresholds().forEach(branchThreshold -> {
            assertEquals(unit, branchThreshold.getUnit());
            assertEquals(min, branchThreshold.min().orElse(null));
            assertEquals(max, branchThreshold.max().orElse(null));
        });
    }

    @Test
    void importThresholdsOnLeftSide() throws IOException {
        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_ONE);
        Network network = Network.read("TestCase12Nodes_for_thresholds_test.uct", getClass().getResourceAsStream("/network/TestCase12Nodes_for_thresholds_test.uct"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(timestamp);
        creationContext = (FbConstraintCreationContext) Crac.readWithContext("thresholds_test.xml", getClass().getResourceAsStream("/merged_cb/thresholds_test.xml"), network, parameters);
        Crac crac = creationContext.getCrac();

        assertEquals(9, crac.getFlowCnecs().size());

        // No threshold specification will be set to default relative-100
        assertHasThresholds(crac.getFlowCnec("CBCO_000001 - preventive"), Set.of(TwoSides.ONE), Unit.PERCENT_IMAX, null, 1.);

        // ImaxA set to 200
        assertHasThresholds(crac.getFlowCnec("CBCO_000002 - preventive"), Set.of(TwoSides.ONE), Unit.AMPERE, null, 200.);

        // ImaxFactor set to 0.8
        assertHasThresholds(crac.getFlowCnec("CBCO_000003 - preventive"), Set.of(TwoSides.ONE), Unit.PERCENT_IMAX, null, 0.8);

        // PermanentImaxA set to 300
        assertHasThresholds(crac.getFlowCnec("CBCO_000004 - preventive"), Set.of(TwoSides.ONE), Unit.AMPERE, null, 300.);

        // PermanentImaxFactor set to 1.2
        assertHasThresholds(crac.getFlowCnec("CBCO_000005 - preventive"), Set.of(TwoSides.ONE), Unit.PERCENT_IMAX, null, 1.2);

        // TemporaryImaxA set to 200 will be set to default relative-100
        assertHasThresholds(crac.getFlowCnec("CBCO_000006 - preventive"), Set.of(TwoSides.ONE), Unit.PERCENT_IMAX, null, 1.);

        // TemporaryImaxFactor set to 0.8 will be set to default relative-100
        assertHasThresholds(crac.getFlowCnec("CBCO_000006 - preventive"), Set.of(TwoSides.ONE), Unit.PERCENT_IMAX, null, 1.);

        // ImaxA, PermanentImaxA and TemporaryImaxA set to 300, 200 and 100
        assertEquals(1, crac.getFlowCnec("CBCO_000008 - preventive").getThresholds().size());
        assertEquals(300., crac.getFlowCnec("CBCO_000008 - preventive").getUpperBound(TwoSides.ONE, Unit.AMPERE).orElseThrow(), 0.1);
    }

    @Test
    void importThresholdsOnRightSide() throws IOException {
        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_TWO);
        Network network = Network.read("TestCase12Nodes_for_thresholds_test.uct", getClass().getResourceAsStream("/network/TestCase12Nodes_for_thresholds_test.uct"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(timestamp);
        Crac crac = Crac.read("thresholds_test.xml", getClass().getResourceAsStream("/merged_cb/thresholds_test.xml"), network, parameters);

        // No threshold specification will be set to default relative-100
        assertHasThresholds(crac.getFlowCnec("CBCO_000001 - preventive"), Set.of(TwoSides.TWO), Unit.PERCENT_IMAX, null, 1.);

        // ImaxA set to 200
        assertHasThresholds(crac.getFlowCnec("CBCO_000002 - preventive"), Set.of(TwoSides.TWO), Unit.AMPERE, null, 200.);

        // ImaxFactor set to 0.8
        assertHasThresholds(crac.getFlowCnec("CBCO_000003 - preventive"), Set.of(TwoSides.TWO), Unit.PERCENT_IMAX, null, 0.8);

        // PermanentImaxA set to 300
        assertHasThresholds(crac.getFlowCnec("CBCO_000004 - preventive"), Set.of(TwoSides.TWO), Unit.AMPERE, null, 300.);

        // PermanentImaxFactor set to 1.2
        assertHasThresholds(crac.getFlowCnec("CBCO_000005 - preventive"), Set.of(TwoSides.TWO), Unit.PERCENT_IMAX, null, 1.2);

        // TemporaryImaxA set to 200 will be set to default relative-100
        assertHasThresholds(crac.getFlowCnec("CBCO_000006 - preventive"), Set.of(TwoSides.TWO), Unit.PERCENT_IMAX, null, 1.);

        // TemporaryImaxFactor set to 0.8 will be set to default relative-100
        assertHasThresholds(crac.getFlowCnec("CBCO_000006 - preventive"), Set.of(TwoSides.TWO), Unit.PERCENT_IMAX, null, 1.);

        // ImaxA, PermanentImaxA and TemporaryImaxA set to 300, 200 and 100
        assertEquals(1, crac.getFlowCnec("CBCO_000008 - preventive").getThresholds().size());
        assertEquals(300., crac.getFlowCnec("CBCO_000008 - preventive").getUpperBound(TwoSides.TWO, Unit.AMPERE).orElseThrow(), 0.1);
    }

    @Test
    void importThresholdsOnBothSides() throws IOException {
        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES);
        Network network = Network.read("TestCase12Nodes_for_thresholds_test.uct", getClass().getResourceAsStream("/network/TestCase12Nodes_for_thresholds_test.uct"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(timestamp);
        Crac crac = Crac.read("thresholds_test.xml", getClass().getResourceAsStream("/merged_cb/thresholds_test.xml"), network, parameters);

        // No threshold specification will be set to default relative-100
        assertHasThresholds(crac.getFlowCnec("CBCO_000001 - preventive"), Set.of(TwoSides.ONE, TwoSides.TWO), Unit.PERCENT_IMAX, null, 1.);

        // ImaxA set to 200
        assertHasThresholds(crac.getFlowCnec("CBCO_000002 - preventive"), Set.of(TwoSides.ONE, TwoSides.TWO), Unit.AMPERE, null, 200.);

        // ImaxFactor set to 0.8
        assertHasThresholds(crac.getFlowCnec("CBCO_000003 - preventive"), Set.of(TwoSides.ONE, TwoSides.TWO), Unit.PERCENT_IMAX, null, 0.8);

        // PermanentImaxA set to 300
        assertHasThresholds(crac.getFlowCnec("CBCO_000004 - preventive"), Set.of(TwoSides.ONE, TwoSides.TWO), Unit.AMPERE, null, 300.);

        // PermanentImaxFactor set to 1.2
        assertHasThresholds(crac.getFlowCnec("CBCO_000005 - preventive"), Set.of(TwoSides.ONE, TwoSides.TWO), Unit.PERCENT_IMAX, null, 1.2);

        // TemporaryImaxA set to 200 will be set to default relative-100
        assertHasThresholds(crac.getFlowCnec("CBCO_000006 - preventive"), Set.of(TwoSides.ONE, TwoSides.TWO), Unit.PERCENT_IMAX, null, 1.);

        // TemporaryImaxFactor set to 0.8 will be set to default relative-100
        assertHasThresholds(crac.getFlowCnec("CBCO_000006 - preventive"), Set.of(TwoSides.ONE, TwoSides.TWO), Unit.PERCENT_IMAX, null, 1.);

        // ImaxA, PermanentImaxA and TemporaryImaxA set to 300, 200 and 100
        assertEquals(2, crac.getFlowCnec("CBCO_000008 - preventive").getThresholds().size());
        assertEquals(300., crac.getFlowCnec("CBCO_000008 - preventive").getUpperBound(TwoSides.ONE, Unit.AMPERE).orElseThrow(), 0.1);
        assertEquals(300., crac.getFlowCnec("CBCO_000008 - preventive").getUpperBound(TwoSides.TWO, Unit.AMPERE).orElseThrow(), 0.1);
    }

    @Test
    void importLoopFlowExtensions() throws IOException {
        Network network = Network.read("TestCase12Nodes_with_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase12Nodes_with_Xnodes.uct"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(timestamp);
        creationContext = (FbConstraintCreationContext) Crac.readWithContext("with_crosszonal_branches.xml", getClass().getResourceAsStream("/merged_cb/with_crosszonal_branches.xml"), network, parameters);
        Crac crac = creationContext.getCrac();

        assertEquals(6, crac.getFlowCnecs().size());

        assertNull(crac.getFlowCnec("INTRA_ZONAL_PREVENTIVE - preventive").getExtension(LoopFlowThreshold.class));
        assertNull(crac.getFlowCnec("INTRA_ZONAL_CURATIVE - curative").getExtension(LoopFlowThreshold.class));
        assertNotNull(crac.getFlowCnec("CROSS_ZONAL_PREVENTIVE - preventive").getExtension(LoopFlowThreshold.class));
        assertNotNull(crac.getFlowCnec("CROSS_ZONAL_CURATIVE - curative").getExtension(LoopFlowThreshold.class));

        assertEquals(.75, crac.getFlowCnec("CROSS_ZONAL_PREVENTIVE - preventive").getExtension(LoopFlowThreshold.class).getThreshold(Unit.PERCENT_IMAX), 1e-3);
        assertEquals(Unit.PERCENT_IMAX, crac.getFlowCnec("CROSS_ZONAL_PREVENTIVE - preventive").getExtension(LoopFlowThreshold.class).getUnit());
        assertEquals(.30, crac.getFlowCnec("CROSS_ZONAL_CURATIVE - curative").getExtension(LoopFlowThreshold.class).getThreshold(Unit.PERCENT_IMAX), 1e-3);
        assertEquals(Unit.PERCENT_IMAX, crac.getFlowCnec("CROSS_ZONAL_CURATIVE - curative").getExtension(LoopFlowThreshold.class).getUnit());

        assertTrue(creationContext.getBranchCnecCreationContext("CROSS_ZONAL_PREVENTIVE").isDirectionInvertedInNetwork());
        assertFalse(creationContext.getBranchCnecCreationContext("CROSS_ZONAL_PREVENTIVE").isAltered());
        assertTrue(creationContext.getBranchCnecCreationContext("CROSS_ZONAL_CURATIVE").isDirectionInvertedInNetwork());
        assertFalse(creationContext.getBranchCnecCreationContext("CROSS_ZONAL_CURATIVE").isAltered());
    }

    @Test
    void testImportHvdcVhOutage() throws IOException {
        Network network = Network.read("TestCase12NodesHvdc.uct", getClass().getResourceAsStream("/network/TestCase12NodesHvdc.uct"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(timestamp);
        creationContext = (FbConstraintCreationContext) Crac.readWithContext("hvdcvh-outage.xml", getClass().getResourceAsStream("/merged_cb/hvdcvh-outage.xml"), network, parameters);
        Crac crac = creationContext.getCrac();

        Contingency contingency = crac.getFlowCnec("Cnec1 - curative").getState().getContingency().orElse(null);
        assertNotNull(contingency);
        assertEquals(2, contingency.getElements().size());
        assertTrue(contingency.getElements().stream().anyMatch(ne -> ne.getId().equals("DDE3AA1  XLI_OB1A 1")));
        assertTrue(contingency.getElements().stream().anyMatch(ne -> ne.getId().equals("BBE2AA1  XLI_OB1B 1")));
    }

    @Test
    void testImportAndCleanCriticalBranches() throws IOException {
        Network network = Network.read("TestCase_severalVoltageLevels_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase_severalVoltageLevels_Xnodes.uct"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(timestamp);
        creationContext = (FbConstraintCreationContext) Crac.readWithContext("critical_branches.xml", getClass().getResourceAsStream("/merged_cb/critical_branches.xml"), network, parameters);
        Crac crac = creationContext.getCrac();

        assertEquals(7, creationContext.getBranchCnecCreationContexts().size());

        assertTrue(creationContext.getBranchCnecCreationContext("BE_CBCO_000001").isImported());
        assertTrue(creationContext.getBranchCnecCreationContext("BE_CBCO_000002").isImported());
        assertTrue(creationContext.getBranchCnecCreationContext("BE_CBCO_000003").isImported());
        assertTrue(creationContext.getBranchCnecCreationContext("BE_CBCO_000004").isImported());

        assertEquals(1, creationContext.getBranchCnecCreationContext("BE_CBCO_000001").getCreatedCnecsIds().size());
        assertEquals(1, creationContext.getBranchCnecCreationContext("BE_CBCO_000002").getCreatedCnecsIds().size());
        assertEquals(2, creationContext.getBranchCnecCreationContext("BE_CBCO_000003").getCreatedCnecsIds().size());
        assertEquals(2, creationContext.getBranchCnecCreationContext("BE_CBCO_000004").getCreatedCnecsIds().size());

        assertCriticalBranchNotImported("FR_CBCO_000001", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK); //unknown branch
        assertCriticalBranchNotImported("FR_CBCO_000002", ImportStatus.INCONSISTENCY_IN_DATA); //unknown outage
        assertCriticalBranchNotImported("FR_CBCO_000003", ImportStatus.NOT_FOR_RAO); //not a MNEC, not a Cnec

        assertEquals(6, crac.getFlowCnecs().size());
    }

    @Test
    void testImportAndCleanComplexVariants() throws IOException {
        Network network = Network.read("TestCase_severalVoltageLevels_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase_severalVoltageLevels_Xnodes.uct"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(timestamp);
        creationContext = (FbConstraintCreationContext) Crac.readWithContext("complex_variants.xml", getClass().getResourceAsStream("/merged_cb/complex_variants.xml"), network, parameters);
        Crac crac = creationContext.getCrac();

        assertEquals(13, creationContext.getRemedialActionCreationContexts().size());
        assertTrue(creationContext.getRemedialActionCreationContext("RA_BE_0001").isImported());
        assertComplexVariantNotImported("RA_BE_0002", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK); //unknown branch
        assertComplexVariantNotImported("RA_BE_0003", ImportStatus.INCONSISTENCY_IN_DATA); //two PST actions
        assertComplexVariantNotImported("RA_BE_0004", ImportStatus.INCOMPLETE_DATA); //two action set
        assertTrue(creationContext.getRemedialActionCreationContext("RA_FR_0001").isImported());
        assertTrue(creationContext.getRemedialActionCreationContext("RA_FR_0002").isImported()); //same network element/action/usage rule as RA_FR_0005, prioritized due to alphabetical order
        assertComplexVariantNotImported("RA_FR_0003", ImportStatus.INCOMPLETE_DATA); //no CO list
        assertComplexVariantNotImported("RA_FR_0004", ImportStatus.INCONSISTENCY_IN_DATA); //preventive and curative
        assertComplexVariantNotImported("RA_FR_0005", ImportStatus.INCONSISTENCY_IN_DATA); //same network element/usage rule as RA_FR_0002
        assertComplexVariantNotImported("RA_FR_0006", ImportStatus.INCONSISTENCY_IN_DATA); //all outage in CO list not ok
        assertComplexVariantNotImported("RA_FR_0007", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK); //unknown branch
        assertTrue(creationContext.getRemedialActionCreationContext("RA_FR_0008").isImported()); //same network element/action as RA_FR_0002, but not same usage rule
        assertTrue(creationContext.getRemedialActionCreationContext("RA_FR_0009").isImported()); //same network element/usage rule as RA_FR_0002, but not same action

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(4, crac.getNetworkActions().size());

        assertEquals(1, crac.getNetworkAction("RA_FR_0002").getUsageRules().size()); // one cannot be interpreted
    }

    @Test
    void testInvertPstRangeAction() throws IOException {
        Network network = Network.read("TestCase_severalVoltageLevels_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase_severalVoltageLevels_Xnodes.uct"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(timestamp);
        creationContext = (FbConstraintCreationContext) Crac.readWithContext("complex_variants_invert.xml", getClass().getResourceAsStream("/merged_cb/complex_variants_invert.xml"), network, parameters);
        Crac crac = creationContext.getCrac();

        // RA_BE_0001 should not be inverted
        assertTrue(creationContext.getRemedialActionCreationContext("RA_BE_0001") instanceof PstComplexVariantCreationContext);
        PstComplexVariantCreationContext pstContext = (PstComplexVariantCreationContext) creationContext.getRemedialActionCreationContext("RA_BE_0001");
        assertTrue(pstContext.isImported());
        assertEquals("RA_BE_0001", pstContext.getNativeObjectId());
        assertEquals("RA_BE_0001", pstContext.getCreatedObjectId());
        assertFalse(pstContext.isInverted());
        assertEquals("BBE2AA1  BBE3AA1  PST BE", pstContext.getNativeNetworkElementId());
        PstRangeAction pstRangeAction = crac.getPstRangeAction("RA_BE_0001");
        assertEquals("BBE2AA1  BBE3AA1  1", pstRangeAction.getNetworkElement().getId());
        assertEquals(3, pstRangeAction.getInitialTap());
        assertEquals(RangeType.ABSOLUTE, pstRangeAction.getRanges().get(0).getRangeType());
        assertEquals(-16, pstRangeAction.getRanges().get(0).getMinTap());
        assertEquals(6, pstRangeAction.getRanges().get(0).getMaxTap());

        // RA_BE_0002 should be inverted
        assertTrue(creationContext.getRemedialActionCreationContext("RA_BE_0002") instanceof PstComplexVariantCreationContext);
        pstContext = (PstComplexVariantCreationContext) creationContext.getRemedialActionCreationContext("RA_BE_0002");
        assertTrue(pstContext.isImported());
        assertEquals("RA_BE_0002", pstContext.getNativeObjectId());
        assertEquals("RA_BE_0002", pstContext.getCreatedObjectId());
        assertTrue(pstContext.isInverted());
        assertFalse(pstContext.isAltered());
        assertEquals("BBE3AA1  BBE2AA1  PST BE", pstContext.getNativeNetworkElementId());
        pstRangeAction = crac.getPstRangeAction("RA_BE_0002");
        assertEquals("BBE2AA1  BBE3AA1  1", pstRangeAction.getNetworkElement().getId());
        assertEquals(3, pstRangeAction.getInitialTap());
        assertEquals(RangeType.ABSOLUTE, pstRangeAction.getRanges().get(0).getRangeType());
        assertEquals(-6, pstRangeAction.getRanges().get(0).getMinTap());
        assertEquals(16, pstRangeAction.getRanges().get(0).getMaxTap());
        assertEquals(RangeType.RELATIVE_TO_PREVIOUS_INSTANT, pstRangeAction.getRanges().get(1).getRangeType());
        assertEquals(-4, pstRangeAction.getRanges().get(1).getMinTap());
        assertEquals(10, pstRangeAction.getRanges().get(1).getMaxTap());

    }

    @Test
    void testWrongTsCreationContext() throws IOException {
        Network network = Network.read("TestCase12Nodes_with_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase12Nodes_with_Xnodes.uct"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T10:30Z");
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(timestamp);
        creationContext = (FbConstraintCreationContext) Crac.readWithContext("wrong_ts.xml", getClass().getResourceAsStream("/merged_cb/wrong_ts.xml"), network, parameters);
        Crac crac = creationContext.getCrac();

        assertEquals(3, creationContext.getCreationReport().getReport().size());

        assertEquals(1, crac.getCnecs().size());
        assertCriticalBranchNotImported("BE_CBCO_000001", ImportStatus.NOT_FOR_REQUESTED_TIMESTAMP);
        assertTrue(creationContext.getBranchCnecCreationContext("BE_CBCO_000002").isImported());

        assertEquals(1, crac.getRemedialActions().size());
        assertComplexVariantNotImported("RA_BE_0001", ImportStatus.NOT_FOR_REQUESTED_TIMESTAMP);
        assertTrue(creationContext.getRemedialActionCreationContext("RA_FR_0001").isImported());
    }

    @Test
    void testDuplicatePsts() throws IOException {
        Network network = Network.read("TestCase_severalVoltageLevels_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase_severalVoltageLevels_Xnodes.uct"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(timestamp);
        creationContext = (FbConstraintCreationContext) Crac.readWithContext("complex_variants_duplicate_psts.xml", getClass().getResourceAsStream("/merged_cb/complex_variants_duplicate_psts.xml"), network, parameters);
        Crac crac = creationContext.getCrac();

        assertEquals(3, creationContext.getCreationReport().getReport().size());

        // RA_BE_0001 is one same PST as RA_BE_0002
        // RA_BE_0002 has been prioritized due to alphabetical order
        assertTrue(creationContext.getRemedialActionCreationContext("RA_BE_0001").isImported());
        assertNotNull(crac.getRemedialAction("RA_BE_0001"));

        assertFalse(creationContext.getRemedialActionCreationContext("RA_BE_0002").isImported());
        assertComplexVariantNotImported("RA_BE_0002", ImportStatus.INCONSISTENCY_IN_DATA);
        assertNull(creationContext.getRemedialActionCreationContext("RA_BE_0002").getCreatedObjectId());
        assertNull(crac.getRemedialAction("RA_BE_0002"));

        // RA_BE_0003 is one same PST as RA_BE_0004
        // RA_BE_0004 has been prioritized as it has a groupId
        assertFalse(creationContext.getRemedialActionCreationContext("RA_BE_0003").isImported());
        assertComplexVariantNotImported("RA_BE_0003", ImportStatus.INCONSISTENCY_IN_DATA);
        assertNull(creationContext.getRemedialActionCreationContext("RA_BE_0003").getCreatedObjectId());
        assertNull(crac.getRemedialAction("RA_BE_0003"));

        assertTrue(creationContext.getRemedialActionCreationContext("RA_BE_0004").isImported());
        assertNotNull(crac.getRemedialAction("RA_BE_0004"));
    }

    @Test
    void importHalflineThresholds() throws IOException {
        Network network = Network.read("TestCase12Nodes_with_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase12Nodes_with_Xnodes.uct"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T10:30Z");
        Crac crac;

        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES);
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(timestamp);
        crac = Crac.read("halflines.xml", getClass().getResourceAsStream("/merged_cb/halflines.xml"), network, parameters);
        assertHasThresholds(crac.getFlowCnec("FR_CBCO_000001 - preventive"), Set.of(TwoSides.TWO), Unit.PERCENT_IMAX, -1., null);
        assertHasThresholds(crac.getFlowCnec("FR_CBCO_000002 - preventive"), Set.of(TwoSides.ONE), Unit.PERCENT_IMAX, null, 1.);

        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_ONE);
        crac = Crac.read("halflines.xml", getClass().getResourceAsStream("/merged_cb/halflines.xml"), network, parameters);
        assertHasThresholds(crac.getFlowCnec("FR_CBCO_000001 - preventive"), Set.of(TwoSides.TWO), Unit.PERCENT_IMAX, -1., null);
        assertHasThresholds(crac.getFlowCnec("FR_CBCO_000002 - preventive"), Set.of(TwoSides.ONE), Unit.PERCENT_IMAX, null, 1.);

        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_TWO);
        crac = Crac.read("halflines.xml", getClass().getResourceAsStream("/merged_cb/halflines.xml"), network, parameters);
        assertHasThresholds(crac.getFlowCnec("FR_CBCO_000001 - preventive"), Set.of(TwoSides.TWO), Unit.PERCENT_IMAX, -1., null);
        assertHasThresholds(crac.getFlowCnec("FR_CBCO_000002 - preventive"), Set.of(TwoSides.ONE), Unit.PERCENT_IMAX, null, 1.);
    }

    @Test
    void testTransformerCnecThresholds() throws IOException {
        Network network = Network.read("TestCase_severalVoltageLevels_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase_severalVoltageLevels_Xnodes.uct"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");

        // CBCO_1 is in %Imax, thresholds should be created depending on default monitored side
        // CBCO_2 is in A, threshold should be defined on high voltage level side
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(timestamp);

        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES);
        Crac crac = Crac.read("transformers.xml", getClass().getResourceAsStream("/merged_cb/transformers.xml"), network, parameters);
        assertHasThresholds(crac.getFlowCnec("CBCO_1 - preventive"), Set.of(TwoSides.ONE, TwoSides.TWO), Unit.PERCENT_IMAX, -1.0, null);
        assertHasThresholds(crac.getFlowCnec("CBCO_2 - preventive"), Set.of(TwoSides.ONE), Unit.AMPERE, -100., null);

        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_ONE);
        crac = Crac.read("transformers.xml", getClass().getResourceAsStream("/merged_cb/transformers.xml"), network, parameters);
        assertHasThresholds(crac.getFlowCnec("CBCO_1 - preventive"), Set.of(TwoSides.ONE), Unit.PERCENT_IMAX, -1.0, null);
        assertHasThresholds(crac.getFlowCnec("CBCO_2 - preventive"), Set.of(TwoSides.ONE), Unit.AMPERE, -100., null);

        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_TWO);
        crac = Crac.read("transformers.xml", getClass().getResourceAsStream("/merged_cb/transformers.xml"), network, parameters);
        assertHasThresholds(crac.getFlowCnec("CBCO_1 - preventive"), Set.of(TwoSides.TWO), Unit.PERCENT_IMAX, -1.0, null);
        assertHasThresholds(crac.getFlowCnec("CBCO_2 - preventive"), Set.of(TwoSides.ONE), Unit.AMPERE, -100., null);
    }
}

