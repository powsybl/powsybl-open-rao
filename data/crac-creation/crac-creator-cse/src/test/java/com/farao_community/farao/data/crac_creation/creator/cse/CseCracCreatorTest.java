/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cse;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.network_action.SwitchPair;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.*;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.JsonCracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.BranchCnecCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.InjectionRangeActionCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.RemedialActionCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.critical_branch.CseCriticalBranchCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.outage.CseOutageCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.parameters.CseCracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cse.remedial_action.CsePstCreationContext;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.data.crac_creation.creator.api.ImportStatus.*;
import static org.junit.Assert.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class CseCracCreatorTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private final OffsetDateTime offsetDateTime = null;
    private CracCreationParameters parameters = new CracCreationParameters();
    private Crac importedCrac;
    private CseCracCreationContext cracCreationContext;

    private void setUp(String cracFileName, String networkFileName) {
        InputStream is = getClass().getResourceAsStream(cracFileName);
        CseCracImporter importer = new CseCracImporter();
        CseCrac cseCrac = importer.importNativeCrac(is);
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        CseCracCreator cseCracCreator = new CseCracCreator();
        cracCreationContext = cseCracCreator.createCrac(cseCrac, network, offsetDateTime, parameters);
        importedCrac = cracCreationContext.getCrac();
    }

    private void setUp(String cracFileName) {
        setUp(cracFileName, "/networks/TestCase12Nodes_with_Xnodes.uct");
    }

    private void setUpWithHvdcNetwork(String cracFileName) {
        setUp(cracFileName, "/networks/TestCase16NodesWithUcteHvdc.uct");
    }

    private void setUpWithTransformer(String cracFileName) {
        setUp(cracFileName, "/networks/TestCase12NodesTransformer.uct");
    }

    private void assertOutageNotImported(String name, ImportStatus importStatus) {
        CseOutageCreationContext context = cracCreationContext.getOutageCreationContext(name);
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
    }

    private void assertRemedialActionNotImported(String name, ImportStatus importStatus) {
        RemedialActionCreationContext context = cracCreationContext.getRemedialActionCreationContext(name);
        assertNotNull(context);
        assertFalse(context.isImported());
        assertEquals(importStatus, context.getImportStatus());
        assertNull(context.getCreatedRAId());
    }

    private void assertHvdcRangeActionImported(String name, Map<String, String> networkElements, String groupId) {
        InjectionRangeActionCreationContext context = (InjectionRangeActionCreationContext) cracCreationContext.getRemedialActionCreationContext(name);
        assertTrue(context.isImported());
        assertEquals(networkElements, context.getNativeNetworkElementIds());
        assertFalse(context.isAltered());
        assertNotNull(context.getCreatedRAId());
        assertNotNull(importedCrac.getInjectionRangeAction(context.getCreatedRAId()));
        assertEquals(groupId, importedCrac.getInjectionRangeAction(context.getCreatedRAId()).getGroupId().orElseThrow());
    }

    @Test
    public void createCrac() {
        setUp("/cracs/cse_crac_1.xml");
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(offsetDateTime, cracCreationContext.getTimeStamp());
        assertEquals("/networks/TestCase12Nodes_with_Xnodes", cracCreationContext.getNetworkName());
    }

    @Test
    public void createCracWithHvdcBasicTest() {
        parameters.addExtension(CseCracCreationParameters.class, new CseCracCreationParameters());
        parameters.getExtension(CseCracCreationParameters.class).setRangeActionGroupsAsString(List.of("PRA_HVDC + CRA_HVDC", "PRA_HVDC + CRA_HVDC_2"));
        setUpWithHvdcNetwork("/cracs/cse_crac_with_hvdc.xml");
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(3, importedCrac.getInjectionRangeActions().size());

        assertHvdcRangeActionImported("PRA_HVDC", Map.of("BBE2AA12", "BBE2AA12_generator", "FFR3AA12", "FFR3AA12_generator"), "PRA_HVDC + CRA_HVDC");
        assertHvdcRangeActionImported("CRA_HVDC", Map.of("BBE2AA12", "BBE2AA12_generator", "FFR3AA12", "FFR3AA12_generator"), "PRA_HVDC + CRA_HVDC");

        assertOutageNotImported("fake_contingency_because_we_have_to", ELEMENT_NOT_FOUND_IN_NETWORK);
        assertCriticalBranchNotImported("fake_because_we_have_to - AAAAAA11 - BBBBBB11 - null", ELEMENT_NOT_FOUND_IN_NETWORK);
        assertRemedialActionNotImported("CRA_HVDC_fake", NOT_YET_HANDLED_BY_FARAO);
        assertRemedialActionNotImported("WEIRD_HVDC_WITH_2_HVDCNODES", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC_WITH_NON_OPPOSITE_GENERATORS", INCONSISTENCY_IN_DATA);
    }

    @Test
    public void createCracWithHvdcWithNoCracCreationParameters() {
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
        assertEquals("AVAILABLE", importedCrac.getInjectionRangeAction("PRA_HVDC").getUsageRules().get(0).getUsageMethod().toString());
        assertEquals(2, importedCrac.getInjectionRangeAction("PRA_HVDC").getInjectionDistributionKeys().size());
        assertEquals(-1., importedCrac.getInjectionRangeAction("PRA_HVDC").getInjectionDistributionKeys().entrySet().stream().filter(e -> e.getKey().getId().equals("BBE2AA12_generator")).findAny().orElseThrow().getValue(), 1e-3);
        assertEquals(1., importedCrac.getInjectionRangeAction("PRA_HVDC").getInjectionDistributionKeys().entrySet().stream().filter(e -> e.getKey().getId().equals("FFR3AA12_generator")).findAny().orElseThrow().getValue(), 1e-3);
    }

    @Test
    public void createCracWithHvdcWithCracCreationParameters() {
        parameters.addExtension(CseCracCreationParameters.class, new CseCracCreationParameters());
        parameters.getExtension(CseCracCreationParameters.class).setRangeActionGroupsAsString(List.of("PRA_HVDC + CRA_HVDC"));
        setUpWithHvdcNetwork("/cracs/cse_crac_with_hvdc.xml");
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals("PRA_HVDC + CRA_HVDC", importedCrac.getInjectionRangeAction("PRA_HVDC").getGroupId().get());
        assertEquals(importedCrac.getInjectionRangeAction("CRA_HVDC").getGroupId().get(), importedCrac.getInjectionRangeAction("PRA_HVDC").getGroupId().get());
        assertEquals("FR", importedCrac.getInjectionRangeAction("PRA_HVDC").getOperator());
        assertEquals(2000, importedCrac.getInjectionRangeAction("PRA_HVDC").getRanges().get(0).getMax(), 1e-1);
        assertEquals(-100, importedCrac.getInjectionRangeAction("PRA_HVDC").getRanges().get(0).getMin(), 1e-1);
        assertEquals("AVAILABLE", importedCrac.getInjectionRangeAction("PRA_HVDC").getUsageRules().get(0).getUsageMethod().toString());
    }

    @Test
    public void createContingencies() {
        setUp("/cracs/cse_crac_1.xml");
        assertEquals(2, importedCrac.getContingencies().size());
    }

    @Test
    public void createPreventiveCnecs() {
        setUp("/cracs/cse_crac_1.xml");
        assertEquals(3, importedCrac.getCnecs(importedCrac.getPreventiveState()).size());
        BranchCnecCreationContext cnec1context = cracCreationContext.getBranchCnecCreationContext("basecase_branch_1 - NNL2AA1  - NNL3AA1  - basecase");
        assertTrue(cnec1context.isBaseCase());
        assertTrue(cnec1context.isImported());
        assertFalse(cnec1context.isDirectionInvertedInNetwork());
        assertTrue(cnec1context.getContingencyId().isEmpty());
        assertEquals(1, cnec1context.getCreatedCnecsIds().size());
        assertEquals("basecase_branch_1 - NNL2AA1 ->NNL3AA1  - preventive", cnec1context.getCreatedCnecsIds().get(Instant.PREVENTIVE));
    }

    @Test
    public void checkOptimizedParameterAccordingToSelected() {
        setUp("/cracs/cse_crac_1.xml");
        BranchCnecCreationContext cnec1context = cracCreationContext.getBranchCnecCreationContext("basecase_branch_1 - NNL2AA1  - NNL3AA1  - basecase");
        BranchCnecCreationContext cnec2context = cracCreationContext.getBranchCnecCreationContext("basecase_branch_2 - NNL1AA1  - NNL3AA1  - basecase");
        BranchCnecCreationContext cnec3context = cracCreationContext.getBranchCnecCreationContext("basecase_branch_3 - NNL1AA1  - NNL2AA1  - basecase");
        assertFalse(((CseCriticalBranchCreationContext) cnec1context).isSelected());
        assertTrue(((CseCriticalBranchCreationContext) cnec2context).isSelected());
        assertTrue(((CseCriticalBranchCreationContext) cnec3context).isSelected());
        assertFalse(importedCrac.getCnec(cnec1context.getCreatedCnecsIds().get(Instant.PREVENTIVE)).isOptimized());
        assertTrue(importedCrac.getCnec(cnec2context.getCreatedCnecsIds().get(Instant.PREVENTIVE)).isOptimized());
        assertTrue(importedCrac.getCnec(cnec3context.getCreatedCnecsIds().get(Instant.PREVENTIVE)).isOptimized());
    }

    @Test
    public void createCurativeCnecs() {
        setUp("/cracs/cse_crac_1.xml");
        BranchCnecCreationContext cnec2context = cracCreationContext.getBranchCnecCreationContext("French line 1 - FFR1AA1  - FFR2AA1  - outage_1");
        assertFalse(cnec2context.isBaseCase());
        assertTrue(cnec2context.isImported());
        assertFalse(cnec2context.isDirectionInvertedInNetwork());
        assertEquals("outage_1", cnec2context.getContingencyId().get());
        assertEquals(2, cnec2context.getCreatedCnecsIds().size());
        assertEquals("French line 1 - FFR1AA1 ->FFR2AA1   - outage_1 - outage", cnec2context.getCreatedCnecsIds().get(Instant.OUTAGE));
        assertEquals("French line 1 - FFR1AA1 ->FFR2AA1   - outage_1 - curative", cnec2context.getCreatedCnecsIds().get(Instant.CURATIVE));
    }

    @Test
    public void doNotCreateAbsentFromNetworkCnec() {
        setUp("/cracs/cse_crac_1.xml");
        assertCriticalBranchNotImported("French line 2 - FFRFAK2 - FFRFAK1 - outage_2", ELEMENT_NOT_FOUND_IN_NETWORK);
    }

    @Test
    public void createNetworkActions() {
        setUp("/cracs/cse_crac_1.xml");
        assertEquals(2, importedCrac.getNetworkActions().size());
    }

    @Test
    public void createRangeActions() {
        setUp("/cracs/cse_crac_1.xml");
        assertEquals(1, importedCrac.getRangeActions().size());
    }

    @Test
    public void doNotCreateAbsentFromNetworkContingency() {
        setUp("/cracs/cse_crac_1.xml");
        assertOutageNotImported("outage_3", ELEMENT_NOT_FOUND_IN_NETWORK);

    }

    @Test
    public void doNotCreateAbsentFromNetworkPstRangeAction() {
        setUp("/cracs/cse_crac_1.xml");
        assertRemedialActionNotImported("cra_4", ELEMENT_NOT_FOUND_IN_NETWORK);
    }

    @Test
    public void doNotCreateAbsentFromNetworkTopologyAction() {
        setUp("/cracs/cse_crac_1.xml");
        assertRemedialActionNotImported("cra_5", ELEMENT_NOT_FOUND_IN_NETWORK);
    }

    @Test
    public void doNotCreateAbsentFromNetworkInjectionSetpointCurative() {
        setUp("/cracs/cse_crac_1.xml");
        assertRemedialActionNotImported("cra_6", ELEMENT_NOT_FOUND_IN_NETWORK);
    }

    @Test
    public void doNotCreateAbsentFromNetworkInjectionSetpointPreventive() {
        setUp("/cracs/cse_crac_2.xml");
        assertRemedialActionNotImported("cra_1", ELEMENT_NOT_FOUND_IN_NETWORK);
    }

    @Test
    public void doNotCreateInjectionSetpointWithOneAbsentFromNetworkNode() {
        setUp("/cracs/cse_crac_2.xml");
        assertRemedialActionNotImported("cra_3", ELEMENT_NOT_FOUND_IN_NETWORK);
    }

    @Test
    public void createInjectionSetpointWithWildcard() {
        setUp("/cracs/cse_crac_2.xml");
        RemedialActionCreationContext raContext = cracCreationContext.getRemedialActionCreationContext("cra_4");
        assertTrue(raContext.isImported());
        NetworkAction na = cracCreationContext.getCrac().getNetworkAction("cra_4");
        assertEquals(2, na.getNetworkElements().size());
        assertTrue(na.getElementaryActions().stream().anyMatch(ea -> ea.getNetworkElements().iterator().next().getId().equals("FFR3AA1 _generator")));
        assertTrue(na.getElementaryActions().stream().anyMatch(ea -> ea.getNetworkElements().iterator().next().getId().equals("FFR2AA1 _generator")));
    }

    @Test
    public void cracCreationContextReport() {
        setUp("/cracs/cse_crac_1.xml");
        List<String> creationReport = cracCreationContext.getCreationReport().getReport();
        assertFalse(creationReport.isEmpty());
        assertEquals(5, creationReport.size());
    }

    @Test
    public void cracCreationContextReport2() {
        setUp("/cracs/cse_crac_2.xml");
        List<String> creationReport = cracCreationContext.getCreationReport().getReport();
        assertFalse(creationReport.isEmpty());
        assertEquals(4, creationReport.size());
    }

    @Test
    public void testRaOnConstraint() {
        setUp("/cracs/cse_crac_onConstraint.xml");

        State preventiveState = importedCrac.getPreventiveState();
        State outageState = importedCrac.getState(importedCrac.getContingency("outage_1"), Instant.OUTAGE);
        State curativeState = importedCrac.getState(importedCrac.getContingency("outage_1"), Instant.CURATIVE);

        FlowCnec outageCnec = importedCrac.getFlowCnec("French line 1 - FFR1AA1 ->FFR2AA1   - outage_1 - outage");
        FlowCnec curativeCnec = importedCrac.getFlowCnec("French line 1 - FFR1AA1 ->FFR2AA1   - outage_1 - curative");

        // PRA
        RemedialAction<?> ra = importedCrac.getRangeAction("PST_pra_3_BBE2AA1  BBE3AA1  1");
        assertEquals(2, ra.getUsageRules().size());
        UsageRule usageRule1 = ra.getUsageRules().get(0);
        UsageRule usageRule2 = ra.getUsageRules().get(1);
        assertTrue(usageRule1 instanceof OnFlowConstraint);
        assertTrue(usageRule2 instanceof OnFlowConstraint);
        assertEquals(Instant.PREVENTIVE, ((OnFlowConstraint) usageRule1).getInstant());
        assertEquals(Instant.PREVENTIVE, ((OnFlowConstraint) usageRule2).getInstant());
        assertTrue(((OnFlowConstraint) usageRule1).getFlowCnec().equals(outageCnec) || ((OnFlowConstraint) usageRule2).getFlowCnec().equals(outageCnec));
        assertTrue(((OnFlowConstraint) usageRule1).getFlowCnec().equals(curativeCnec) || ((OnFlowConstraint) usageRule2).getFlowCnec().equals(curativeCnec));
        assertEquals(UsageMethod.TO_BE_EVALUATED, usageRule1.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.TO_BE_EVALUATED, usageRule2.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, usageRule1.getUsageMethod(outageState));
        assertEquals(UsageMethod.UNDEFINED, usageRule2.getUsageMethod(outageState));
        assertEquals(UsageMethod.UNDEFINED, usageRule1.getUsageMethod(curativeState));
        assertEquals(UsageMethod.UNDEFINED, usageRule2.getUsageMethod(curativeState));

        // CRA
        ra = importedCrac.getNetworkAction("cra_1");
        assertEquals(1, ra.getUsageRules().size());
        usageRule1 = ra.getUsageRules().get(0);
        assertTrue(usageRule1 instanceof OnFlowConstraint);
        assertSame(curativeCnec, ((OnFlowConstraint) usageRule1).getFlowCnec());
        assertEquals(Instant.CURATIVE, ((OnFlowConstraint) usageRule1).getInstant());
        assertEquals(UsageMethod.UNDEFINED, usageRule1.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, usageRule1.getUsageMethod(outageState));
        assertEquals(UsageMethod.TO_BE_EVALUATED, usageRule1.getUsageMethod(curativeState));
    }

    @Test
    public void testPercentageThresholdsOnLeftSide() {
        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_LEFT_SIDE);
        setUp("/cracs/cse_crac_pct_limit.xml");

        FlowCnec flowCnec1 = importedCrac.getFlowCnec("basecase_branch_1 - NNL2AA1 ->NNL3AA1  - preventive");
        assertEquals(1, flowCnec1.getThresholds().size());
        assertEquals(0.7, flowCnec1.getThresholds().iterator().next().max().get(), DOUBLE_TOLERANCE);
        assertTrue(flowCnec1.getThresholds().iterator().next().min().isEmpty());
        assertEquals(0.7 * 5000., flowCnec1.getUpperBound(Side.LEFT, Unit.AMPERE).get(), DOUBLE_TOLERANCE);

        FlowCnec flowCnec2 = importedCrac.getFlowCnec("basecase_branch_2 - NNL1AA1 ->NNL3AA1  - preventive");
        assertEquals(1, flowCnec2.getThresholds().size());
        assertEquals(-1., flowCnec2.getThresholds().iterator().next().min().get(), DOUBLE_TOLERANCE);
        assertTrue(flowCnec2.getThresholds().iterator().next().max().isEmpty());
        assertEquals(-5000., flowCnec2.getLowerBound(Side.LEFT, Unit.AMPERE).get(), DOUBLE_TOLERANCE);

        FlowCnec flowCnec3 = importedCrac.getFlowCnec("basecase_branch_3 - NNL1AA1 ->NNL2AA1  - preventive");
        assertEquals(1, flowCnec3.getThresholds().size());
        assertEquals(-0.2, flowCnec3.getThresholds().iterator().next().min().get(), DOUBLE_TOLERANCE);
        assertEquals(0.2, flowCnec3.getThresholds().iterator().next().max().get(), DOUBLE_TOLERANCE);
        assertEquals(-0.2 * 5000., flowCnec3.getLowerBound(Side.LEFT, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
        assertEquals(0.2 * 5000., flowCnec3.getUpperBound(Side.LEFT, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testPercentageThresholdsOnRightSide() {
        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_RIGHT_SIDE);
        setUp("/cracs/cse_crac_pct_limit.xml");

        FlowCnec flowCnec1 = importedCrac.getFlowCnec("basecase_branch_1 - NNL2AA1 ->NNL3AA1  - preventive");
        assertEquals(1, flowCnec1.getThresholds().size());
        assertEquals(0.7, flowCnec1.getThresholds().iterator().next().max().get(), DOUBLE_TOLERANCE);
        assertTrue(flowCnec1.getThresholds().iterator().next().min().isEmpty());
        assertEquals(0.7 * 5000., flowCnec1.getUpperBound(Side.RIGHT, Unit.AMPERE).get(), DOUBLE_TOLERANCE);

        FlowCnec flowCnec2 = importedCrac.getFlowCnec("basecase_branch_2 - NNL1AA1 ->NNL3AA1  - preventive");
        assertEquals(1, flowCnec2.getThresholds().size());
        assertEquals(-1., flowCnec2.getThresholds().iterator().next().min().get(), DOUBLE_TOLERANCE);
        assertTrue(flowCnec2.getThresholds().iterator().next().max().isEmpty());
        assertEquals(-5000., flowCnec2.getLowerBound(Side.RIGHT, Unit.AMPERE).get(), DOUBLE_TOLERANCE);

        FlowCnec flowCnec3 = importedCrac.getFlowCnec("basecase_branch_3 - NNL1AA1 ->NNL2AA1  - preventive");
        assertEquals(1, flowCnec3.getThresholds().size());
        assertEquals(-0.2, flowCnec3.getThresholds().iterator().next().min().get(), DOUBLE_TOLERANCE);
        assertEquals(0.2, flowCnec3.getThresholds().iterator().next().max().get(), DOUBLE_TOLERANCE);
        assertEquals(-0.2 * 5000., flowCnec3.getLowerBound(Side.RIGHT, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
        assertEquals(0.2 * 5000., flowCnec3.getUpperBound(Side.RIGHT, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testPercentageThresholdsOnBothSides() {
        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES);
        setUp("/cracs/cse_crac_pct_limit.xml");

        FlowCnec flowCnec1 = importedCrac.getFlowCnec("basecase_branch_1 - NNL2AA1 ->NNL3AA1  - preventive");
        assertEquals(2, flowCnec1.getThresholds().size());
        assertEquals(0.7 * 5000., flowCnec1.getUpperBound(Side.LEFT, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
        assertEquals(0.7 * 5000., flowCnec1.getUpperBound(Side.RIGHT, Unit.AMPERE).get(), DOUBLE_TOLERANCE);

        FlowCnec flowCnec2 = importedCrac.getFlowCnec("basecase_branch_2 - NNL1AA1 ->NNL3AA1  - preventive");
        assertEquals(2, flowCnec2.getThresholds().size());
        assertEquals(-5000., flowCnec2.getLowerBound(Side.LEFT, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
        assertEquals(-5000., flowCnec2.getLowerBound(Side.RIGHT, Unit.AMPERE).get(), DOUBLE_TOLERANCE);

        FlowCnec flowCnec3 = importedCrac.getFlowCnec("basecase_branch_3 - NNL1AA1 ->NNL2AA1  - preventive");
        assertEquals(2, flowCnec3.getThresholds().size());
        assertEquals(-0.2 * 5000., flowCnec3.getLowerBound(Side.LEFT, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
        assertEquals(0.2 * 5000., flowCnec3.getUpperBound(Side.LEFT, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
        assertEquals(-0.2 * 5000., flowCnec3.getLowerBound(Side.RIGHT, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
        assertEquals(0.2 * 5000., flowCnec3.getUpperBound(Side.RIGHT, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testRaOnConstraintInSpecificCountry() {
        setUp("/cracs/cse_crac_onConstraintInSpecificCountry.xml");

        // cra_1
        RemedialAction<?> cra1 = importedCrac.getNetworkAction("cra_1");
        assertEquals(1, cra1.getUsageRules().size()); // one OnConstraint on CNEC 1
        assertTrue(cra1.getUsageRules().get(0) instanceof OnFlowConstraint);
        // cra_2
        RemedialAction<?> cra2 = importedCrac.getNetworkAction("cra_2");
        assertEquals(2, cra2.getUsageRules().size()); // one FreeToUse, one OnConstraint on CNEC 1
        assertTrue(cra2.getUsageRules().get(0) instanceof OnFlowConstraint);
        assertTrue(cra2.getUsageRules().get(1) instanceof FreeToUse);
        // cra_3
        RemedialAction<?> cra3 = importedCrac.getNetworkAction("cra_3");
        assertEquals(2, cra3.getUsageRules().size()); // 1 OnConstraint on CNEC 1 and 1 on country FR
        assertTrue(cra3.getUsageRules().get(0) instanceof OnFlowConstraint);
        assertTrue(cra3.getUsageRules().get(1) instanceof OnFlowConstraintInCountry);
        assertEquals(Country.FR, ((OnFlowConstraintInCountry) cra3.getUsageRules().get(1)).getCountry());
        // cra_4
        RemedialAction<?> cra4 = importedCrac.getNetworkAction("cra_4");
        assertEquals(1, cra4.getUsageRules().size()); // on country NL
        assertTrue(cra4.getUsageRules().get(0) instanceof OnFlowConstraintInCountry);
        assertEquals(Instant.CURATIVE, ((OnFlowConstraintInCountry) cra4.getUsageRules().get(0)).getInstant());
        assertEquals(Country.NL, ((OnFlowConstraintInCountry) cra4.getUsageRules().get(0)).getCountry());
        // cra_5
        RemedialAction<?> cra5 = importedCrac.getNetworkAction("cra_5");
        assertEquals(1, cra5.getUsageRules().size()); // on country FR
        assertTrue(cra5.getUsageRules().get(0) instanceof OnFlowConstraintInCountry);
        assertEquals(Instant.CURATIVE, ((OnFlowConstraintInCountry) cra5.getUsageRules().get(0)).getInstant());
        assertEquals(Country.FR, ((OnFlowConstraintInCountry) cra5.getUsageRules().get(0)).getCountry());
        // cra_6
        assertTrue(importedCrac.getNetworkAction("cra_6").getUsageRules().isEmpty());
    }

    @Test
    public void testInvertPstRangeAction() {
        setUp("/cracs/cse_crac_inverted_pst.xml");

        // ra_1 should not be inverted
        assertTrue(cracCreationContext.getRemedialActionCreationContext("ra_1") instanceof CsePstCreationContext);
        CsePstCreationContext pstContext = (CsePstCreationContext) cracCreationContext.getRemedialActionCreationContext("ra_1");
        assertTrue(pstContext.isImported());
        assertFalse(pstContext.isAltered());
        assertEquals("ra_1", pstContext.getNativeId());
        assertEquals("PST_ra_1_BBE2AA1  BBE3AA1  1", pstContext.getCreatedRAId());
        assertFalse(pstContext.isInverted());
        assertFalse(pstContext.isAltered());
        assertEquals("BBE2AA1  BBE3AA1  1", pstContext.getNativeNetworkElementId());
        PstRangeAction pstRangeAction = importedCrac.getPstRangeAction(pstContext.getCreatedRAId());
        assertEquals("BBE2AA1  BBE3AA1  1", pstRangeAction.getNetworkElement().getId());
        assertEquals(3, pstRangeAction.getInitialTap());
        assertEquals(RangeType.ABSOLUTE, pstRangeAction.getRanges().get(0).getRangeType());
        assertEquals(-2, pstRangeAction.getRanges().get(0).getMinTap());
        assertEquals(10, pstRangeAction.getRanges().get(0).getMaxTap());

        // ra_2 should be inverted but range remains the same (just aligns on network direction)
        assertTrue(cracCreationContext.getRemedialActionCreationContext("ra_2") instanceof CsePstCreationContext);
        pstContext = (CsePstCreationContext) cracCreationContext.getRemedialActionCreationContext("ra_2");
        assertTrue(pstContext.isImported());
        assertEquals("ra_2", pstContext.getNativeId());
        assertEquals("PST_ra_2_BBE2AA1  BBE3AA1  1", pstContext.getCreatedRAId());
        assertFalse(pstContext.isInverted());
        assertFalse(pstContext.isAltered());
        assertEquals("BBE3AA1  BBE2AA1  1", pstContext.getNativeNetworkElementId());
        pstRangeAction = importedCrac.getPstRangeAction(pstContext.getCreatedRAId());
        assertEquals("BBE2AA1  BBE3AA1  1", pstRangeAction.getNetworkElement().getId());
        assertEquals(3, pstRangeAction.getInitialTap());
        assertEquals(RangeType.ABSOLUTE, pstRangeAction.getRanges().get(0).getRangeType());
        assertEquals(-2, pstRangeAction.getRanges().get(0).getMinTap());
        assertEquals(10, pstRangeAction.getRanges().get(0).getMaxTap());
    }

    @Test
    public void testImportBusBarChange() {
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
    public void testImportBusBarChangeWithMissingSwitch() {
        parameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/CseCracCreationParameters_15_10_1_3.json"));
        setUp("/cracs/cseCrac_ep15us10-1case1.xml", "/networks/TestCase12Nodes_forCSE.uct");
        assertRemedialActionNotImported("Bus bar ok test", ELEMENT_NOT_FOUND_IN_NETWORK);
    }

    @Test
    public void testImportBusBarChangeWithMissingParameter() {
        parameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/CseCracCreationParameters_15_10_1_4.json"));
        setUp("/cracs/cseCrac_ep15us10-1case1.xml", "/networks/TestCase12Nodes_forCSE.uct");
        assertRemedialActionNotImported("Bus bar ok test", INCOMPLETE_DATA);
    }

    @Test
    public void testImportEmptyRa() {
        setUp("/cracs/cse_crac_empty_ra.xml");
        assertNotNull(cracCreationContext.getCrac());
        assertTrue(cracCreationContext.getCrac().getRemedialActions().isEmpty());
        assertEquals(1, cracCreationContext.getRemedialActionCreationContexts().size());
    }

    private void assertHasOneThreshold(String cnecId, Side side) {
        FlowCnec cnec = importedCrac.getFlowCnec(cnecId);
        assertEquals(1, cnec.getThresholds().size());
        assertEquals(side, cnec.getThresholds().iterator().next().getSide());
    }

    private void assertHasTwoThresholds(String cnecId) {
        FlowCnec cnec = importedCrac.getFlowCnec(cnecId);
        assertEquals(2, cnec.getThresholds().size());
        assertTrue(cnec.getThresholds().stream().anyMatch(branchThreshold -> branchThreshold.getSide().equals(Side.LEFT)));
        assertTrue(cnec.getThresholds().stream().anyMatch(branchThreshold -> branchThreshold.getSide().equals(Side.RIGHT)));
    }

    @Test
    public void testImportThresholdOnHalfLine() {
        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES);
        setUp("/cracs/cse_crac_halflines.xml");
        assertHasOneThreshold("basecase_branch_1 - FFR2AA1 ->X_DEFR1  - preventive", Side.RIGHT);
        assertHasOneThreshold("basecase_branch_2 - DDE2AA1 ->X_NLDE1  - preventive", Side.LEFT);

        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_RIGHT_SIDE);
        setUp("/cracs/cse_crac_halflines.xml");
        assertHasOneThreshold("basecase_branch_1 - FFR2AA1 ->X_DEFR1  - preventive", Side.RIGHT);
        assertHasOneThreshold("basecase_branch_2 - DDE2AA1 ->X_NLDE1  - preventive", Side.LEFT);

        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_LEFT_SIDE);
        setUp("/cracs/cse_crac_halflines.xml");
        assertHasOneThreshold("basecase_branch_1 - FFR2AA1 ->X_DEFR1  - preventive", Side.RIGHT);
        assertHasOneThreshold("basecase_branch_2 - DDE2AA1 ->X_NLDE1  - preventive", Side.LEFT);
    }

    @Test
    public void testTransformerCnecThresholds() {
        // basecase_branch_1 is in A, threshold should be defined on high voltage level side
        // basecase_branch_2 is in %Imax, thresholds should be created depending on default monitored side

        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES);
        setUpWithTransformer("/cracs/cse_crac_transformer_cnec.xml");
        assertHasOneThreshold("basecase_branch_1 - BBE2AA1 ->BBE3AA2  - preventive", Side.LEFT);
        assertHasTwoThresholds("basecase_branch_2 - BBE2AA1 ->BBE3AA2  - preventive");

        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_RIGHT_SIDE);
        setUpWithTransformer("/cracs/cse_crac_transformer_cnec.xml");
        assertHasOneThreshold("basecase_branch_1 - BBE2AA1 ->BBE3AA2  - preventive", Side.LEFT);
        assertHasOneThreshold("basecase_branch_2 - BBE2AA1 ->BBE3AA2  - preventive", Side.RIGHT);

        parameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_LEFT_SIDE);
        setUpWithTransformer("/cracs/cse_crac_transformer_cnec.xml");
        assertHasOneThreshold("basecase_branch_1 - BBE2AA1 ->BBE3AA2  - preventive", Side.LEFT);
        assertHasOneThreshold("basecase_branch_2 - BBE2AA1 ->BBE3AA2  - preventive", Side.LEFT);
    }

    @Test
    public void createCracWithAuto() {
        setUp("/cracs/cse_crac_auto.xml");
        assertRemedialActionNotImported("ara_1", NOT_YET_HANDLED_BY_FARAO);
        assertEquals(9, importedCrac.getFlowCnecs().size());
        assertFalse(cracCreationContext.getCreationReport().getReport().contains("[ADDED] CNEC \"French line 1 - FFR1AA1 ->FFR2AA1   - outage_1 - auto\" has no associated automaton. It will be cloned on the OUTAGE instant in order to be secured during preventive RAO."));
    }
}
