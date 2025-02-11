/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.cse;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.crac.api.usagerule.OnFlowConstraintInCountry;
import com.powsybl.openrao.data.crac.api.usagerule.OnInstant;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.networkaction.SwitchPair;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParameters;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.io.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.commons.api.stdcreationcontext.BranchCnecCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.stdcreationcontext.InjectionRangeActionCreationContext;
import com.powsybl.openrao.data.crac.io.cse.criticalbranch.CseCriticalBranchCreationContext;
import com.powsybl.openrao.data.crac.io.cse.parameters.CseCracCreationParameters;
import com.powsybl.openrao.data.crac.io.cse.remedialaction.CsePstCreationContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
class CseCracCreatorTest {
    private static final double DOUBLE_TOLERANCE = 0.01;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private CracCreationParameters parameters = new CracCreationParameters();
    private Crac importedCrac;
    private CseCracCreationContext cracCreationContext;
    private Instant preventiveInstant;
    private Instant outageInstant;
    private Instant curativeInstant;

    private void setUp(String cracFileName, String networkFileName) throws IOException {
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        InputStream is = getClass().getResourceAsStream(cracFileName);
        cracCreationContext = (CseCracCreationContext) Crac.readWithContext(cracFileName, is, network, parameters);
        importedCrac = cracCreationContext.getCrac();
        preventiveInstant = importedCrac.getInstant(PREVENTIVE_INSTANT_ID);
        outageInstant = importedCrac.getInstant(OUTAGE_INSTANT_ID);
        curativeInstant = importedCrac.getInstant(CURATIVE_INSTANT_ID);
    }

    private void setUp(String cracFileName) throws IOException {
        setUp(cracFileName, "/networks/TestCase12Nodes_with_Xnodes.uct");
    }

    private void setUpWithHvdcNetwork(String cracFileName) throws IOException {
        setUp(cracFileName, "/networks/TestCase16NodesWithUcteHvdc.uct");
    }

    private void setUpWithTransformer(String cracFileName) throws IOException {
        setUp(cracFileName, "/networks/TestCase12NodesTransformer.uct");
    }

    private void assertOutageNotImported(String name, ImportStatus importStatus) {
        ElementaryCreationContext context = cracCreationContext.getOutageCreationContext(name);
        assertNotNull(context);
        assertFalse(context.isImported());
        assertEquals(importStatus, context.getImportStatus());
    }

    private void assertCriticalBranchNotImported(String name, ImportStatus importStatus) {
        BranchCnecCreationContext context = cracCreationContext.getBranchCnecCreationContext(name);
        assertNotNull(context);
        assertFalse(context.isImported());
        assertEquals(importStatus, context.getImportStatus());
        assertTrue(context.getCreatedCnecsIds().isEmpty());
        assertTrue(context.getCreatedObjectsIds().isEmpty());
    }

    private void assertRemedialActionNotImported(String name, ImportStatus importStatus) {
        ElementaryCreationContext context = cracCreationContext.getRemedialActionCreationContext(name);
        assertNotNull(context);
        assertFalse(context.isImported());
        assertEquals(importStatus, context.getImportStatus());
        assertNull(context.getCreatedObjectId());
    }

    private void assertHvdcRangeActionImported(String name, Map<String, String> networkElements, String groupId) {
        InjectionRangeActionCreationContext context = (InjectionRangeActionCreationContext) cracCreationContext.getRemedialActionCreationContext(name);
        assertTrue(context.isImported());
        assertEquals(networkElements, context.getNativeNetworkElementIds());
        assertFalse(context.isAltered());
        assertNotNull(context.getCreatedObjectId());
        assertNotNull(importedCrac.getInjectionRangeAction(context.getCreatedObjectId()));
        assertEquals(groupId, importedCrac.getInjectionRangeAction(context.getCreatedObjectId()).getGroupId().orElseThrow());
    }

    @Test
    void createCrac() throws IOException {
        setUp("/cracs/cse_crac_1.xml");
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(null, cracCreationContext.getTimeStamp());
        assertEquals("/networks/TestCase12Nodes_with_Xnodes", cracCreationContext.getNetworkName());
    }

    @Test
    void createCracWithParameters() throws IOException {
        RaUsageLimits raUsageLimits = new RaUsageLimits();
        raUsageLimits.setMaxRa(4);
        parameters.addRaUsageLimitsForInstant("preventive", raUsageLimits);
        setUp("/cracs/cse_crac_1.xml");
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(null, cracCreationContext.getTimeStamp());
        assertEquals("/networks/TestCase12Nodes_with_Xnodes", cracCreationContext.getNetworkName());
        assertEquals(4, cracCreationContext.getCrac().getRaUsageLimits(preventiveInstant).getMaxRa());
    }

    @Test
    void createCracWithHvdcBasicTest() throws IOException {
        parameters.addExtension(CseCracCreationParameters.class, new CseCracCreationParameters());
        parameters.getExtension(CseCracCreationParameters.class).setRangeActionGroupsAsString(List.of("PRA_HVDC + CRA_HVDC", "PRA_HVDC + CRA_HVDC_2"));
        setUpWithHvdcNetwork("/cracs/cse_crac_with_hvdc.xml");
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(3, importedCrac.getInjectionRangeActions().size());

        assertHvdcRangeActionImported("PRA_HVDC", Map.of("BBE2AA12", "BBE2AA12_generator", "FFR3AA12", "FFR3AA12_generator"), "PRA_HVDC + CRA_HVDC");
        assertHvdcRangeActionImported("CRA_HVDC", Map.of("BBE2AA12", "BBE2AA12_generator", "FFR3AA12", "FFR3AA12_generator"), "PRA_HVDC + CRA_HVDC");

        assertOutageNotImported("fake_contingency_because_we_have_to", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK);
        assertCriticalBranchNotImported("fake_because_we_have_to - AAAAAA11 - BBBBBB11 - null", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK);
        assertRemedialActionNotImported("CRA_HVDC_fake", ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO);
        assertRemedialActionNotImported("WEIRD_HVDC_WITH_2_HVDCNODES", ImportStatus.INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC_WITH_NON_OPPOSITE_GENERATORS", ImportStatus.INCONSISTENCY_IN_DATA);
    }

    @Test
    void createCracWithHvdcWithNoCracCreationParameters() throws IOException {
        parameters.addExtension(CseCracCreationParameters.class, new CseCracCreationParameters());
        setUpWithHvdcNetwork("/cracs/cse_crac_with_hvdc.xml");
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertTrue(importedCrac.getInjectionRangeAction("PRA_HVDC").getGroupId().isEmpty());
        assertTrue(importedCrac.getInjectionRangeAction("CRA_HVDC").getGroupId().isEmpty());
        assertTrue(importedCrac.getInjectionRangeAction("CRA_HVDC_2").getGroupId().isEmpty());
        assertEquals("FR", importedCrac.getInjectionRangeAction("PRA_HVDC").getOperator());
        // range from CRAC
        assertEquals(-100, importedCrac.getInjectionRangeAction("PRA_HVDC").getRanges().get(0).getMin(), 1e-1);
        assertEquals(2000, importedCrac.getInjectionRangeAction("PRA_HVDC").getRanges().get(0).getMax(), 1e-1);
        //range from network
        assertEquals(-500, importedCrac.getInjectionRangeAction("PRA_HVDC").getRanges().get(1).getMin(), 1e-1);
        assertEquals(800, importedCrac.getInjectionRangeAction("PRA_HVDC").getRanges().get(1).getMax(), 1e-1);
        assertEquals("AVAILABLE", importedCrac.getInjectionRangeAction("PRA_HVDC").getUsageRules().iterator().next().getUsageMethod().toString());
        assertEquals(2, importedCrac.getInjectionRangeAction("PRA_HVDC").getInjectionDistributionKeys().size());
        assertEquals(-1., importedCrac.getInjectionRangeAction("PRA_HVDC").getInjectionDistributionKeys().entrySet().stream().filter(e -> e.getKey().getId().equals("BBE2AA12_generator")).findAny().orElseThrow().getValue(), 1e-3);
        assertEquals(1., importedCrac.getInjectionRangeAction("PRA_HVDC").getInjectionDistributionKeys().entrySet().stream().filter(e -> e.getKey().getId().equals("FFR3AA12_generator")).findAny().orElseThrow().getValue(), 1e-3);
    }

    @Test
    void createCracWithHvdcWithCracCreationParameters() throws IOException {
        parameters.addExtension(CseCracCreationParameters.class, new CseCracCreationParameters());
        parameters.getExtension(CseCracCreationParameters.class).setRangeActionGroupsAsString(List.of("PRA_HVDC + CRA_HVDC"));
        setUpWithHvdcNetwork("/cracs/cse_crac_with_hvdc.xml");
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals("PRA_HVDC + CRA_HVDC", importedCrac.getInjectionRangeAction("PRA_HVDC").getGroupId().get());
        assertEquals(importedCrac.getInjectionRangeAction("CRA_HVDC").getGroupId().get(), importedCrac.getInjectionRangeAction("PRA_HVDC").getGroupId().get());
        assertEquals("FR", importedCrac.getInjectionRangeAction("PRA_HVDC").getOperator());
        assertEquals(2000, importedCrac.getInjectionRangeAction("PRA_HVDC").getRanges().get(0).getMax(), 1e-1);
        assertEquals(-100, importedCrac.getInjectionRangeAction("PRA_HVDC").getRanges().get(0).getMin(), 1e-1);
        assertEquals("AVAILABLE", importedCrac.getInjectionRangeAction("PRA_HVDC").getUsageRules().iterator().next().getUsageMethod().toString());
    }

    @Test
    void createContingencies() throws IOException {
        setUp("/cracs/cse_crac_1.xml");
        assertEquals(2, importedCrac.getContingencies().size());
    }

    @Test
    void createPreventiveCnecs() throws IOException {
        setUp("/cracs/cse_crac_1.xml");
        assertEquals(3, importedCrac.getCnecs(importedCrac.getPreventiveState()).size());
        BranchCnecCreationContext cnec1context = cracCreationContext.getBranchCnecCreationContext("basecase_branch_1 - NNL2AA1  - NNL3AA1  - basecase");
        assertTrue(cnec1context.isBaseCase());
        assertTrue(cnec1context.isImported());
        assertFalse(cnec1context.isDirectionInvertedInNetwork());
        assertTrue(cnec1context.getContingencyId().isEmpty());
        assertEquals(1, cnec1context.getCreatedCnecsIds().size());
        assertEquals("basecase_branch_1 - NNL2AA1 ->NNL3AA1  - preventive", cnec1context.getCreatedCnecsIds().get(PREVENTIVE_INSTANT_ID));
    }

    @Test
    void checkOptimizedParameterAccordingToSelected() throws IOException {
        setUp("/cracs/cse_crac_1.xml");
        BranchCnecCreationContext cnec1context = cracCreationContext.getBranchCnecCreationContext("basecase_branch_1 - NNL2AA1  - NNL3AA1  - basecase");
        BranchCnecCreationContext cnec2context = cracCreationContext.getBranchCnecCreationContext("basecase_branch_2 - NNL1AA1  - NNL3AA1  - basecase");
        BranchCnecCreationContext cnec3context = cracCreationContext.getBranchCnecCreationContext("basecase_branch_3 - NNL1AA1  - NNL2AA1  - basecase");
        assertFalse(((CseCriticalBranchCreationContext) cnec1context).isSelected());
        assertTrue(((CseCriticalBranchCreationContext) cnec2context).isSelected());
        assertTrue(((CseCriticalBranchCreationContext) cnec3context).isSelected());
        assertFalse(importedCrac.getCnec(cnec1context.getCreatedCnecsIds().get(PREVENTIVE_INSTANT_ID)).isOptimized());
        assertTrue(importedCrac.getCnec(cnec2context.getCreatedCnecsIds().get(PREVENTIVE_INSTANT_ID)).isOptimized());
        assertTrue(importedCrac.getCnec(cnec3context.getCreatedCnecsIds().get(PREVENTIVE_INSTANT_ID)).isOptimized());
    }

    @Test
    void createCurativeCnecs() throws IOException {
        setUp("/cracs/cse_crac_1.xml");
        BranchCnecCreationContext cnec2context = cracCreationContext.getBranchCnecCreationContext("French line 1 - FFR1AA1  - FFR2AA1  - outage_1");
        assertFalse(cnec2context.isBaseCase());
        assertTrue(cnec2context.isImported());
        assertFalse(cnec2context.isDirectionInvertedInNetwork());
        assertEquals("outage_1", cnec2context.getContingencyId().get());
        assertEquals(2, cnec2context.getCreatedCnecsIds().size());
        assertEquals("French line 1 - FFR1AA1 ->FFR2AA1   - outage_1 - outage", cnec2context.getCreatedCnecsIds().get(OUTAGE_INSTANT_ID));
        assertEquals("French line 1 - FFR1AA1 ->FFR2AA1   - outage_1 - curative", cnec2context.getCreatedCnecsIds().get(CURATIVE_INSTANT_ID));
    }

    @Test
    void doNotCreateAbsentFromNetworkCnec() throws IOException {
        setUp("/cracs/cse_crac_1.xml");
        assertCriticalBranchNotImported("French line 2 - FFRFAK2  - FFRFAK1  - outage_2", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK);
    }

    @Test
    void createNetworkActions() throws IOException {
        setUp("/cracs/cse_crac_1.xml");
        assertEquals(2, importedCrac.getNetworkActions().size());
    }

    @Test
    void createRangeActions() throws IOException {
        setUp("/cracs/cse_crac_1.xml");
        assertEquals(1, importedCrac.getRangeActions().size());
    }

    @Test
    void doNotCreateAbsentFromNetworkContingency() throws IOException {
        setUp("/cracs/cse_crac_1.xml");
        assertOutageNotImported("outage_3", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK);

    }

    @Test
    void doNotCreateAbsentFromNetworkPstRangeAction() throws IOException {
        setUp("/cracs/cse_crac_1.xml");
        assertRemedialActionNotImported("cra_4", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK);
    }

    @Test
    void doNotCreateAbsentFromNetworkTopologyAction() throws IOException {
        setUp("/cracs/cse_crac_1.xml");
        assertRemedialActionNotImported("cra_5", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK);
    }

    @Test
    void doNotCreateAbsentFromNetworkInjectionSetpointCurative() throws IOException {
        setUp("/cracs/cse_crac_1.xml");
        assertRemedialActionNotImported("cra_6", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK);
    }

    @Test
    void doNotCreateAbsentFromNetworkInjectionSetpointPreventive() throws IOException {
        setUp("/cracs/cse_crac_2.xml");
        assertRemedialActionNotImported("cra_1", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK);
    }

    @Test
    void doNotCreateInjectionSetpointWithOneAbsentFromNetworkNode() throws IOException {
        setUp("/cracs/cse_crac_2.xml");
        assertRemedialActionNotImported("cra_3", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK);
    }

    @Test
    void createInjectionSetpointWithWildcard() throws IOException {
        setUp("/cracs/cse_crac_2.xml");
        ElementaryCreationContext raContext = cracCreationContext.getRemedialActionCreationContext("cra_4");
        assertTrue(raContext.isImported());
        NetworkAction na = cracCreationContext.getCrac().getNetworkAction("cra_4");
        assertEquals(2, na.getNetworkElements().size());
        assertTrue(na.getNetworkElements().stream().anyMatch(el -> el.getId().equals("FFR3AA1 _generator")));
        assertTrue(na.getNetworkElements().stream().anyMatch(el -> el.getId().equals("FFR2AA1 _generator")));
    }

    @Test
    void cracCreationContextReport() throws IOException {
        setUp("/cracs/cse_crac_1.xml");
        List<String> creationReport = cracCreationContext.getCreationReport().getReport();
        assertFalse(creationReport.isEmpty());
        assertEquals(6, creationReport.size());
    }

    @Test
    void cracCreationContextReport2() throws IOException {
        setUp("/cracs/cse_crac_2.xml");
        List<String> creationReport = cracCreationContext.getCreationReport().getReport();
        assertFalse(creationReport.isEmpty());
        assertEquals(5, creationReport.size());
    }

    @Test
    void testRaOnConstraint() throws IOException {
        setUp("/cracs/cse_crac_onConstraint.xml");

        State preventiveState = importedCrac.getPreventiveState();
        State outageState = importedCrac.getState(importedCrac.getContingency("outage_1"), outageInstant);
        State curativeState = importedCrac.getState(importedCrac.getContingency("outage_1"), curativeInstant);

        FlowCnec outageCnec = importedCrac.getFlowCnec("French line 1 - FFR1AA1 ->FFR2AA1   - outage_1 - outage");
        FlowCnec curativeCnec = importedCrac.getFlowCnec("French line 1 - FFR1AA1 ->FFR2AA1   - outage_1 - curative");

        // PRA
        RemedialAction<?> ra = importedCrac.getRangeAction("PST_pra_3_BBE2AA1  BBE3AA1  1");
        assertEquals(2, ra.getUsageRules().size());
        List<UsageRule> usageRuleList = ra.getUsageRules().stream().toList();

        UsageRule usageRule1 = usageRuleList.get(0);
        UsageRule usageRule2 = usageRuleList.get(1);
        assertTrue(usageRule1 instanceof OnConstraint<?>);
        assertTrue(usageRule2 instanceof OnConstraint<?>);
        assertEquals(preventiveInstant, usageRule1.getInstant());
        assertEquals(preventiveInstant, usageRule2.getInstant());
        assertTrue(((OnConstraint<?>) usageRule1).getCnec().equals(outageCnec) || ((OnConstraint<?>) usageRule2).getCnec().equals(outageCnec));
        assertTrue(((OnConstraint<?>) usageRule1).getCnec().equals(curativeCnec) || ((OnConstraint<?>) usageRule2).getCnec().equals(curativeCnec));
        System.out.println(usageRule1.getUsageMethod(preventiveState));
        System.out.println(usageRule2.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.AVAILABLE, usageRule1.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.AVAILABLE, usageRule2.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, usageRule1.getUsageMethod(outageState));
        assertEquals(UsageMethod.UNDEFINED, usageRule2.getUsageMethod(outageState));
        assertEquals(UsageMethod.UNDEFINED, usageRule1.getUsageMethod(curativeState));
        assertEquals(UsageMethod.UNDEFINED, usageRule2.getUsageMethod(curativeState));

        // CRA
        ra = importedCrac.getNetworkAction("cra_1");
        assertEquals(1, ra.getUsageRules().size());
        usageRule1 = ra.getUsageRules().iterator().next();
        assertTrue(usageRule1 instanceof OnConstraint<?>);
        assertSame(curativeCnec, ((OnConstraint<?>) usageRule1).getCnec());
        assertEquals(curativeInstant, usageRule1.getInstant());
        assertEquals(UsageMethod.UNDEFINED, usageRule1.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, usageRule1.getUsageMethod(outageState));
        assertEquals(UsageMethod.AVAILABLE, usageRule1.getUsageMethod(curativeState));
    }

    @Test
    void testPercentageThresholdsOnLeftSide() throws IOException {
        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_ONE);
        setUp("/cracs/cse_crac_pct_limit.xml");

        FlowCnec flowCnec1 = importedCrac.getFlowCnec("basecase_branch_1 - NNL2AA1 ->NNL3AA1  - preventive");
        assertEquals(1, flowCnec1.getThresholds().size());
        assertEquals(0.7, flowCnec1.getThresholds().iterator().next().max().get(), DOUBLE_TOLERANCE);
        assertTrue(flowCnec1.getThresholds().iterator().next().min().isEmpty());
        assertEquals(0.7 * 5000., flowCnec1.getUpperBound(TwoSides.ONE, Unit.AMPERE).get(), DOUBLE_TOLERANCE);

        FlowCnec flowCnec2 = importedCrac.getFlowCnec("basecase_branch_2 - NNL1AA1 ->NNL3AA1  - preventive");
        assertEquals(1, flowCnec2.getThresholds().size());
        assertEquals(-1., flowCnec2.getThresholds().iterator().next().min().get(), DOUBLE_TOLERANCE);
        assertTrue(flowCnec2.getThresholds().iterator().next().max().isEmpty());
        assertEquals(-5000., flowCnec2.getLowerBound(TwoSides.ONE, Unit.AMPERE).get(), DOUBLE_TOLERANCE);

        FlowCnec flowCnec3 = importedCrac.getFlowCnec("basecase_branch_3 - NNL1AA1 ->NNL2AA1  - preventive");
        assertEquals(1, flowCnec3.getThresholds().size());
        assertEquals(-0.2, flowCnec3.getThresholds().iterator().next().min().get(), DOUBLE_TOLERANCE);
        assertEquals(0.2, flowCnec3.getThresholds().iterator().next().max().get(), DOUBLE_TOLERANCE);
        assertEquals(-0.2 * 5000., flowCnec3.getLowerBound(TwoSides.ONE, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
        assertEquals(0.2 * 5000., flowCnec3.getUpperBound(TwoSides.ONE, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
    }

    @Test
    void testPercentageThresholdsOnRightSide() throws IOException {
        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_TWO);
        setUp("/cracs/cse_crac_pct_limit.xml");

        FlowCnec flowCnec1 = importedCrac.getFlowCnec("basecase_branch_1 - NNL2AA1 ->NNL3AA1  - preventive");
        assertEquals(1, flowCnec1.getThresholds().size());
        assertEquals(0.7, flowCnec1.getThresholds().iterator().next().max().get(), DOUBLE_TOLERANCE);
        assertTrue(flowCnec1.getThresholds().iterator().next().min().isEmpty());
        assertEquals(0.7 * 5000., flowCnec1.getUpperBound(TwoSides.TWO, Unit.AMPERE).get(), DOUBLE_TOLERANCE);

        FlowCnec flowCnec2 = importedCrac.getFlowCnec("basecase_branch_2 - NNL1AA1 ->NNL3AA1  - preventive");
        assertEquals(1, flowCnec2.getThresholds().size());
        assertEquals(-1., flowCnec2.getThresholds().iterator().next().min().get(), DOUBLE_TOLERANCE);
        assertTrue(flowCnec2.getThresholds().iterator().next().max().isEmpty());
        assertEquals(-5000., flowCnec2.getLowerBound(TwoSides.TWO, Unit.AMPERE).get(), DOUBLE_TOLERANCE);

        FlowCnec flowCnec3 = importedCrac.getFlowCnec("basecase_branch_3 - NNL1AA1 ->NNL2AA1  - preventive");
        assertEquals(1, flowCnec3.getThresholds().size());
        assertEquals(-0.2, flowCnec3.getThresholds().iterator().next().min().get(), DOUBLE_TOLERANCE);
        assertEquals(0.2, flowCnec3.getThresholds().iterator().next().max().get(), DOUBLE_TOLERANCE);
        assertEquals(-0.2 * 5000., flowCnec3.getLowerBound(TwoSides.TWO, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
        assertEquals(0.2 * 5000., flowCnec3.getUpperBound(TwoSides.TWO, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
    }

    @Test
    void testPercentageThresholdsOnBothSides() throws IOException {
        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES);
        setUp("/cracs/cse_crac_pct_limit.xml");

        FlowCnec flowCnec1 = importedCrac.getFlowCnec("basecase_branch_1 - NNL2AA1 ->NNL3AA1  - preventive");
        assertEquals(2, flowCnec1.getThresholds().size());
        assertEquals(0.7 * 5000., flowCnec1.getUpperBound(TwoSides.ONE, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
        assertEquals(0.7 * 5000., flowCnec1.getUpperBound(TwoSides.TWO, Unit.AMPERE).get(), DOUBLE_TOLERANCE);

        FlowCnec flowCnec2 = importedCrac.getFlowCnec("basecase_branch_2 - NNL1AA1 ->NNL3AA1  - preventive");
        assertEquals(2, flowCnec2.getThresholds().size());
        assertEquals(-5000., flowCnec2.getLowerBound(TwoSides.ONE, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
        assertEquals(-5000., flowCnec2.getLowerBound(TwoSides.TWO, Unit.AMPERE).get(), DOUBLE_TOLERANCE);

        FlowCnec flowCnec3 = importedCrac.getFlowCnec("basecase_branch_3 - NNL1AA1 ->NNL2AA1  - preventive");
        assertEquals(2, flowCnec3.getThresholds().size());
        assertEquals(-0.2 * 5000., flowCnec3.getLowerBound(TwoSides.ONE, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
        assertEquals(0.2 * 5000., flowCnec3.getUpperBound(TwoSides.ONE, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
        assertEquals(-0.2 * 5000., flowCnec3.getLowerBound(TwoSides.TWO, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
        assertEquals(0.2 * 5000., flowCnec3.getUpperBound(TwoSides.TWO, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
    }

    @Test
    void testRaOnConstraintInSpecificCountry() throws IOException {
        setUp("/cracs/cse_crac_onConstraintInSpecificCountry.xml");

        // cra_1
        RemedialAction<?> cra1 = importedCrac.getNetworkAction("cra_1");
        assertEquals(1, cra1.getUsageRules().size()); // one OnConstraint on CNEC 1
        Iterator<UsageRule> iterator1 = cra1.getUsageRules().iterator();
        UsageRule crac1UsageRule0 = iterator1.next();
        assertTrue(crac1UsageRule0 instanceof OnConstraint<?>);
        // cra_2
        RemedialAction<?> cra2 = importedCrac.getNetworkAction("cra_2");
        assertEquals(2, cra2.getUsageRules().size()); // one OnInstant, one OnConstraint on CNEC 1
        List<UsageRule> usageRules2List = cra2.getUsageRules().stream().sorted(Comparator.comparing(ur -> ur.getClass().getName())).toList();
        assertTrue(usageRules2List.get(0) instanceof OnConstraint<?>);
        assertTrue(usageRules2List.get(1) instanceof OnInstant);
        // cra_3
        RemedialAction<?> cra3 = importedCrac.getNetworkAction("cra_3");
        assertEquals(2, cra3.getUsageRules().size()); // 1 OnConstraint on CNEC 1 and 1 on country FR
        List<UsageRule> usageRules3List = cra3.getUsageRules().stream().sorted(Comparator.comparing(ur -> ur.getClass().getName())).toList();
        assertTrue(usageRules3List.get(0) instanceof OnConstraint<?>);
        assertTrue(usageRules3List.get(1) instanceof OnFlowConstraintInCountry);
        assertEquals(Country.FR, ((OnFlowConstraintInCountry) usageRules3List.get(1)).getCountry());
        // cra_4
        RemedialAction<?> cra4 = importedCrac.getNetworkAction("cra_4");
        assertEquals(1, cra4.getUsageRules().size()); // on country NL
        Iterator<UsageRule> iterator4 = cra4.getUsageRules().iterator();
        UsageRule crac4UsageRule0 = iterator4.next();
        assertTrue(crac4UsageRule0 instanceof OnFlowConstraintInCountry);
        assertEquals(curativeInstant, crac4UsageRule0.getInstant());
        assertEquals(Country.NL, ((OnFlowConstraintInCountry) crac4UsageRule0).getCountry());
        // cra_5
        RemedialAction<?> cra5 = importedCrac.getNetworkAction("cra_5");
        assertEquals(1, cra5.getUsageRules().size()); // on country FR
        Iterator<UsageRule> iterator5 = cra5.getUsageRules().iterator();
        UsageRule crac5UsageRule0 = iterator5.next();
        assertTrue(crac5UsageRule0 instanceof OnFlowConstraintInCountry);
        assertEquals(curativeInstant, crac5UsageRule0.getInstant());
        assertEquals(Country.FR, ((OnFlowConstraintInCountry) crac5UsageRule0).getCountry());
        // cra_6
        assertTrue(importedCrac.getNetworkAction("cra_6").getUsageRules().isEmpty());
    }

    @Test
    void testInvertPstRangeAction() throws IOException {
        setUp("/cracs/cse_crac_inverted_pst.xml");

        // ra_1 should not be inverted
        assertTrue(cracCreationContext.getRemedialActionCreationContext("ra_1") instanceof CsePstCreationContext);
        CsePstCreationContext pstContext = (CsePstCreationContext) cracCreationContext.getRemedialActionCreationContext("ra_1");
        assertTrue(pstContext.isImported());
        assertFalse(pstContext.isAltered());
        assertEquals("ra_1", pstContext.getNativeObjectId());
        assertEquals("PST_ra_1_BBE2AA1  BBE3AA1  1", pstContext.getCreatedObjectId());
        assertFalse(pstContext.isInverted());
        assertFalse(pstContext.isAltered());
        assertEquals("BBE2AA1  BBE3AA1  1", pstContext.getNativeNetworkElementId());
        PstRangeAction pstRangeAction = importedCrac.getPstRangeAction(pstContext.getCreatedObjectId());
        assertEquals("BBE2AA1  BBE3AA1  1", pstRangeAction.getNetworkElement().getId());
        assertEquals(3, pstRangeAction.getInitialTap());
        assertEquals(RangeType.ABSOLUTE, pstRangeAction.getRanges().get(0).getRangeType());
        assertEquals(-2, pstRangeAction.getRanges().get(0).getMinTap());
        assertEquals(10, pstRangeAction.getRanges().get(0).getMaxTap());

        // ra_2 should be inverted but range remains the same (just aligns on network direction)
        assertTrue(cracCreationContext.getRemedialActionCreationContext("ra_2") instanceof CsePstCreationContext);
        pstContext = (CsePstCreationContext) cracCreationContext.getRemedialActionCreationContext("ra_2");
        assertTrue(pstContext.isImported());
        assertEquals("ra_2", pstContext.getNativeObjectId());
        assertEquals("PST_ra_2_BBE2AA1  BBE3AA1  1", pstContext.getCreatedObjectId());
        assertFalse(pstContext.isInverted());
        assertFalse(pstContext.isAltered());
        assertEquals("BBE3AA1  BBE2AA1  1", pstContext.getNativeNetworkElementId());
        pstRangeAction = importedCrac.getPstRangeAction(pstContext.getCreatedObjectId());
        assertEquals("BBE2AA1  BBE3AA1  1", pstRangeAction.getNetworkElement().getId());
        assertEquals(3, pstRangeAction.getInitialTap());
        assertEquals(RangeType.ABSOLUTE, pstRangeAction.getRanges().get(0).getRangeType());
        assertEquals(-2, pstRangeAction.getRanges().get(0).getMinTap());
        assertEquals(10, pstRangeAction.getRanges().get(0).getMaxTap());
    }

    @Test
    void testImportBusBarChange() throws IOException {
        parameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/CseCracCreationParameters_15_10_1_5.json"));
        setUp("/cracs/cseCrac_ep15us10-1case5.xml", "/networks/TestCase12Nodes_forCSE_3nodes.uct");

        assertEquals(2, importedCrac.getNetworkActions().size());
        assertTrue(cracCreationContext.getRemedialActionCreationContext("RA1").isImported());
        assertTrue(cracCreationContext.getRemedialActionCreationContext("RA2").isImported());

        NetworkAction ra1 = importedCrac.getNetworkAction("RA1");
        assertEquals(1, ra1.getElementaryActions().size());
        assertTrue(ra1.getElementaryActions().iterator().next() instanceof SwitchPair);
        SwitchPair switchPair = (SwitchPair) ra1.getElementaryActions().iterator().next();
        assertEquals("BBE1AA1X BBE1AA11 1", switchPair.getSwitchToOpen().getId());
        assertEquals("BBE1AA1X BBE1AA12 1", switchPair.getSwitchToClose().getId());

        NetworkAction ra2 = importedCrac.getNetworkAction("RA2");
        assertEquals(1, ra2.getElementaryActions().size());
        assertTrue(ra2.getElementaryActions().iterator().next() instanceof SwitchPair);
        switchPair = (SwitchPair) ra2.getElementaryActions().iterator().next();
        assertEquals("BBE1AA1X BBE1AA12 1", switchPair.getSwitchToOpen().getId());
        assertEquals("BBE1AA1X BBE1AA11 1", switchPair.getSwitchToClose().getId());
    }

    @Test
    void testImportBusBarChangeWithMissingSwitch() throws IOException {
        parameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/CseCracCreationParameters_15_10_1_3.json"));
        setUp("/cracs/cseCrac_ep15us10-1case1.xml", "/networks/TestCase12Nodes_forCSE.uct");
        assertRemedialActionNotImported("Bus bar ok test", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK);
    }

    @Test
    void testImportBusBarChangeWithMissingParameter() throws IOException {
        parameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/CseCracCreationParameters_15_10_1_4.json"));
        setUp("/cracs/cseCrac_ep15us10-1case1.xml", "/networks/TestCase12Nodes_forCSE.uct");
        assertRemedialActionNotImported("Bus bar ok test", ImportStatus.INCOMPLETE_DATA);
    }

    @Test
    void testImportEmptyRa() throws IOException {
        setUp("/cracs/cse_crac_empty_ra.xml");
        assertNotNull(cracCreationContext.getCrac());
        assertTrue(cracCreationContext.getCrac().getRemedialActions().isEmpty());
        assertEquals(1, cracCreationContext.getRemedialActionCreationContexts().size());
    }

    private void assertHasOneThreshold(String cnecId, TwoSides side) {
        FlowCnec cnec = importedCrac.getFlowCnec(cnecId);
        assertEquals(1, cnec.getThresholds().size());
        assertEquals(side, cnec.getThresholds().iterator().next().getSide());
    }

    private void assertHasTwoThresholds(String cnecId) {
        FlowCnec cnec = importedCrac.getFlowCnec(cnecId);
        assertEquals(2, cnec.getThresholds().size());
        assertTrue(cnec.getThresholds().stream().anyMatch(branchThreshold -> branchThreshold.getSide().equals(TwoSides.ONE)));
        assertTrue(cnec.getThresholds().stream().anyMatch(branchThreshold -> branchThreshold.getSide().equals(TwoSides.TWO)));
    }

    @Test
    void testImportThresholdOnHalfLine() throws IOException {
        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES);
        setUp("/cracs/cse_crac_halflines.xml");
        assertHasOneThreshold("basecase_branch_1 - FFR2AA1 ->X_DEFR1  - preventive", TwoSides.TWO);
        assertHasOneThreshold("basecase_branch_2 - DDE2AA1 ->X_NLDE1  - preventive", TwoSides.ONE);

        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_TWO);
        setUp("/cracs/cse_crac_halflines.xml");
        assertHasOneThreshold("basecase_branch_1 - FFR2AA1 ->X_DEFR1  - preventive", TwoSides.TWO);
        assertHasOneThreshold("basecase_branch_2 - DDE2AA1 ->X_NLDE1  - preventive", TwoSides.ONE);

        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_ONE);
        setUp("/cracs/cse_crac_halflines.xml");
        assertHasOneThreshold("basecase_branch_1 - FFR2AA1 ->X_DEFR1  - preventive", TwoSides.TWO);
        assertHasOneThreshold("basecase_branch_2 - DDE2AA1 ->X_NLDE1  - preventive", TwoSides.ONE);
    }

    @Test
    void testTransformerCnecThresholds() throws IOException {
        // basecase_branch_1 is in A, threshold should be defined on high voltage level side
        // basecase_branch_2 is in %Imax, thresholds should be created depending on default monitored side

        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES);
        setUpWithTransformer("/cracs/cse_crac_transformer_cnec.xml");
        assertHasOneThreshold("basecase_branch_1 - BBE2AA1 ->BBE3AA2  - preventive", TwoSides.ONE);
        assertHasTwoThresholds("basecase_branch_2 - BBE2AA1 ->BBE3AA2  - preventive");

        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_TWO);
        setUpWithTransformer("/cracs/cse_crac_transformer_cnec.xml");
        assertHasOneThreshold("basecase_branch_1 - BBE2AA1 ->BBE3AA2  - preventive", TwoSides.ONE);
        assertHasOneThreshold("basecase_branch_2 - BBE2AA1 ->BBE3AA2  - preventive", TwoSides.TWO);

        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_ONE);
        setUpWithTransformer("/cracs/cse_crac_transformer_cnec.xml");
        assertHasOneThreshold("basecase_branch_1 - BBE2AA1 ->BBE3AA2  - preventive", TwoSides.ONE);
        assertHasOneThreshold("basecase_branch_2 - BBE2AA1 ->BBE3AA2  - preventive", TwoSides.ONE);
    }

    @Test
    void createCracWithAuto() throws IOException {
        setUp("/cracs/cse_crac_auto.xml");
        assertRemedialActionNotImported("ara_1", ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO);
        assertEquals(9, importedCrac.getFlowCnecs().size());
        assertFalse(cracCreationContext.getCreationReport().getReport().contains("[ADDED] CNEC \"French line 1 - FFR1AA1 ->FFR2AA1   - outage_1 - auto\" has no associated automaton. It will be cloned on the OUTAGE instant in order to be secured during preventive RAO."));
    }
}
