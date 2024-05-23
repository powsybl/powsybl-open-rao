/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.cse;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.networkaction.SwitchPair;
import com.powsybl.openrao.data.cracapi.range.RangeType;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.*;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.JsonCracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.api.stdcreationcontext.BranchCnecCreationContext;
import com.powsybl.openrao.data.craccreation.creator.api.stdcreationcontext.InjectionRangeActionCreationContext;
import com.powsybl.openrao.data.craccreation.creator.api.stdcreationcontext.RemedialActionCreationContext;
import com.powsybl.openrao.data.craccreation.creator.cse.criticalbranch.CseCriticalBranchCreationContext;
import com.powsybl.openrao.data.craccreation.creator.cse.outage.CseOutageCreationContext;
import com.powsybl.openrao.data.craccreation.creator.cse.parameters.CseCracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.cse.remedialaction.CsePstCreationContext;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.powsybl.openrao.data.craccreation.creator.api.ImportStatus.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
class CseCracCreatorTest {
    private static final double DOUBLE_TOLERANCE = 0.01;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private final OffsetDateTime offsetDateTime = null;
    private CracCreationParameters parameters = new CracCreationParameters();
    private Crac importedCrac;
    private CseCracCreationContext cracCreationContext;
    private Instant preventiveInstant;
    private Instant outageInstant;
    private Instant curativeInstant;
    private ReportNode reportNode = ReportNode.NO_OP;

    private static ReportNode buildNewRootNode() {
        return ReportNode.newRootReportNode().withMessageTemplate("Test report node", "This is a parent report node for report tests").build();
    }

    private void setUp(String cracFileName, String networkFileName) {
        InputStream is = getClass().getResourceAsStream(cracFileName);
        CseCracImporter importer = new CseCracImporter();
        CseCrac cseCrac = importer.importNativeCrac(is, reportNode);
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        CseCracCreator cseCracCreator = new CseCracCreator();
        cracCreationContext = cseCracCreator.createCrac(cseCrac, network, offsetDateTime, parameters, reportNode);
        importedCrac = cracCreationContext.getCrac();
        preventiveInstant = importedCrac.getInstant(PREVENTIVE_INSTANT_ID);
        outageInstant = importedCrac.getInstant(OUTAGE_INSTANT_ID);
        curativeInstant = importedCrac.getInstant(CURATIVE_INSTANT_ID);
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
    void createCrac() {
        setUp("/cracs/cse_crac_1.xml");
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(offsetDateTime, cracCreationContext.getTimeStamp());
        assertEquals("/networks/TestCase12Nodes_with_Xnodes", cracCreationContext.getNetworkName());
    }

    @Test
    void createCracWithParameters() {
        RaUsageLimits raUsageLimits = new RaUsageLimits();
        raUsageLimits.setMaxRa(4);
        parameters.addRaUsageLimitsForInstant("preventive", raUsageLimits);
        setUp("/cracs/cse_crac_1.xml");
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(offsetDateTime, cracCreationContext.getTimeStamp());
        assertEquals("/networks/TestCase12Nodes_with_Xnodes", cracCreationContext.getNetworkName());
        assertEquals(4, cracCreationContext.getCrac().getRaUsageLimits(preventiveInstant).getMaxRa());
    }

    @Test
    void createCracWithHvdcBasicTest() {
        parameters.addExtension(CseCracCreationParameters.class, new CseCracCreationParameters());
        parameters.getExtension(CseCracCreationParameters.class).setRangeActionGroupsAsString(List.of("PRA_HVDC + CRA_HVDC", "PRA_HVDC + CRA_HVDC_2"));
        setUpWithHvdcNetwork("/cracs/cse_crac_with_hvdc.xml");
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(3, importedCrac.getInjectionRangeActions().size());

        assertHvdcRangeActionImported("PRA_HVDC", Map.of("BBE2AA12", "BBE2AA12_generator", "FFR3AA12", "FFR3AA12_generator"), "PRA_HVDC + CRA_HVDC");
        assertHvdcRangeActionImported("CRA_HVDC", Map.of("BBE2AA12", "BBE2AA12_generator", "FFR3AA12", "FFR3AA12_generator"), "PRA_HVDC + CRA_HVDC");

        assertOutageNotImported("fake_contingency_because_we_have_to", ELEMENT_NOT_FOUND_IN_NETWORK);
        assertCriticalBranchNotImported("fake_because_we_have_to - AAAAAA11 - BBBBBB11 - null", ELEMENT_NOT_FOUND_IN_NETWORK);
        assertRemedialActionNotImported("CRA_HVDC_fake", NOT_YET_HANDLED_BY_OPEN_RAO);
        assertRemedialActionNotImported("WEIRD_HVDC_WITH_2_HVDCNODES", INCONSISTENCY_IN_DATA);
        assertRemedialActionNotImported("HVDC_WITH_NON_OPPOSITE_GENERATORS", INCONSISTENCY_IN_DATA);
    }

    @Test
    void createCracWithHvdcWithNoCracCreationParameters() {
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
    void createCracWithHvdcWithCracCreationParameters() {
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
    void createContingencies() {
        setUp("/cracs/cse_crac_1.xml");
        assertEquals(2, importedCrac.getContingencies().size());
    }

    @Test
    void createPreventiveCnecs() {
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
    void checkOptimizedParameterAccordingToSelected() {
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
    void createCurativeCnecs() {
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
    void doNotCreateAbsentFromNetworkCnec() {
        setUp("/cracs/cse_crac_1.xml");
        assertCriticalBranchNotImported("French line 2 - FFRFAK2 - FFRFAK1 - outage_2", ELEMENT_NOT_FOUND_IN_NETWORK);
    }

    @Test
    void createNetworkActions() {
        setUp("/cracs/cse_crac_1.xml");
        assertEquals(2, importedCrac.getNetworkActions().size());
    }

    @Test
    void createRangeActions() {
        setUp("/cracs/cse_crac_1.xml");
        assertEquals(1, importedCrac.getRangeActions().size());
    }

    @Test
    void doNotCreateAbsentFromNetworkContingency() {
        setUp("/cracs/cse_crac_1.xml");
        assertOutageNotImported("outage_3", ELEMENT_NOT_FOUND_IN_NETWORK);

    }

    @Test
    void doNotCreateAbsentFromNetworkPstRangeAction() {
        setUp("/cracs/cse_crac_1.xml");
        assertRemedialActionNotImported("cra_4", ELEMENT_NOT_FOUND_IN_NETWORK);
    }

    @Test
    void doNotCreateAbsentFromNetworkTopologyAction() {
        setUp("/cracs/cse_crac_1.xml");
        assertRemedialActionNotImported("cra_5", ELEMENT_NOT_FOUND_IN_NETWORK);
    }

    @Test
    void doNotCreateAbsentFromNetworkInjectionSetpointCurative() {
        setUp("/cracs/cse_crac_1.xml");
        assertRemedialActionNotImported("cra_6", ELEMENT_NOT_FOUND_IN_NETWORK);
    }

    @Test
    void doNotCreateAbsentFromNetworkInjectionSetpointPreventive() {
        setUp("/cracs/cse_crac_2.xml");
        assertRemedialActionNotImported("cra_1", ELEMENT_NOT_FOUND_IN_NETWORK);
    }

    @Test
    void doNotCreateInjectionSetpointWithOneAbsentFromNetworkNode() {
        setUp("/cracs/cse_crac_2.xml");
        assertRemedialActionNotImported("cra_3", ELEMENT_NOT_FOUND_IN_NETWORK);
    }

    @Test
    void createInjectionSetpointWithWildcard() {
        setUp("/cracs/cse_crac_2.xml");
        RemedialActionCreationContext raContext = cracCreationContext.getRemedialActionCreationContext("cra_4");
        assertTrue(raContext.isImported());
        NetworkAction na = cracCreationContext.getCrac().getNetworkAction("cra_4");
        assertEquals(2, na.getNetworkElements().size());
        assertTrue(na.getElementaryActions().stream().anyMatch(ea -> ea.getNetworkElements().iterator().next().getId().equals("FFR3AA1 _generator")));
        assertTrue(na.getElementaryActions().stream().anyMatch(ea -> ea.getNetworkElements().iterator().next().getId().equals("FFR2AA1 _generator")));
    }

    @Test
    void cracCreationContextReport() {
        setUp("/cracs/cse_crac_1.xml");
        List<String> creationReport = cracCreationContext.getCreationReport().getReport();
        assertFalse(creationReport.isEmpty());
        assertEquals(5, creationReport.size());
    }

    @Test
    void cracCreationContextReport2() {
        setUp("/cracs/cse_crac_2.xml");
        List<String> creationReport = cracCreationContext.getCreationReport().getReport();
        assertFalse(creationReport.isEmpty());
        assertEquals(4, creationReport.size());
    }

    @Test
    void testRaOnConstraint() {
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
        assertTrue(usageRule1 instanceof OnFlowConstraint);
        assertTrue(usageRule2 instanceof OnFlowConstraint);
        assertEquals(preventiveInstant, usageRule1.getInstant());
        assertEquals(preventiveInstant, usageRule2.getInstant());
        assertTrue(((OnFlowConstraint) usageRule1).getFlowCnec().equals(outageCnec) || ((OnFlowConstraint) usageRule2).getFlowCnec().equals(outageCnec));
        assertTrue(((OnFlowConstraint) usageRule1).getFlowCnec().equals(curativeCnec) || ((OnFlowConstraint) usageRule2).getFlowCnec().equals(curativeCnec));
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
        assertTrue(usageRule1 instanceof OnFlowConstraint);
        assertSame(curativeCnec, ((OnFlowConstraint) usageRule1).getFlowCnec());
        assertEquals(curativeInstant, usageRule1.getInstant());
        assertEquals(UsageMethod.UNDEFINED, usageRule1.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, usageRule1.getUsageMethod(outageState));
        assertEquals(UsageMethod.AVAILABLE, usageRule1.getUsageMethod(curativeState));
    }

    @Test
    void testPercentageThresholdsOnLeftSide() {
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
    void testPercentageThresholdsOnRightSide() {
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
    void testPercentageThresholdsOnBothSides() {
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
    void testRaOnConstraintInSpecificCountry() {
        setUp("/cracs/cse_crac_onConstraintInSpecificCountry.xml");

        // cra_1
        RemedialAction<?> cra1 = importedCrac.getNetworkAction("cra_1");
        assertEquals(1, cra1.getUsageRules().size()); // one OnConstraint on CNEC 1
        Iterator<UsageRule> iterator1 = cra1.getUsageRules().iterator();
        UsageRule crac1UsageRule0 = iterator1.next();
        assertTrue(crac1UsageRule0 instanceof OnFlowConstraint);
        // cra_2
        RemedialAction<?> cra2 = importedCrac.getNetworkAction("cra_2");
        assertEquals(2, cra2.getUsageRules().size()); // one OnInstant, one OnConstraint on CNEC 1
        List<UsageRule> usageRules2List = cra2.getUsageRules().stream().sorted(Comparator.comparing(ur -> ur.getClass().getName())).toList();
        assertTrue(usageRules2List.get(0) instanceof OnFlowConstraint);
        assertTrue(usageRules2List.get(1) instanceof OnInstant);
        // cra_3
        RemedialAction<?> cra3 = importedCrac.getNetworkAction("cra_3");
        assertEquals(2, cra3.getUsageRules().size()); // 1 OnConstraint on CNEC 1 and 1 on country FR
        List<UsageRule> usageRules3List = cra3.getUsageRules().stream().sorted(Comparator.comparing(ur -> ur.getClass().getName())).toList();
        assertTrue(usageRules3List.get(0) instanceof OnFlowConstraint);
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
    void testInvertPstRangeAction() {
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
    void testImportBusBarChange() {
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
    void testImportBusBarChangeWithMissingSwitch() {
        parameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/CseCracCreationParameters_15_10_1_3.json"));
        setUp("/cracs/cseCrac_ep15us10-1case1.xml", "/networks/TestCase12Nodes_forCSE.uct");
        assertRemedialActionNotImported("Bus bar ok test", ELEMENT_NOT_FOUND_IN_NETWORK);
    }

    @Test
    void testImportBusBarChangeWithMissingParameter() {
        parameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/CseCracCreationParameters_15_10_1_4.json"));
        setUp("/cracs/cseCrac_ep15us10-1case1.xml", "/networks/TestCase12Nodes_forCSE.uct");
        assertRemedialActionNotImported("Bus bar ok test", INCOMPLETE_DATA);
    }

    @Test
    void testImportEmptyRa() {
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
    void testImportThresholdOnHalfLine() {
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
    void testTransformerCnecThresholds() {
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
    void createCracWithAuto() {
        setUp("/cracs/cse_crac_auto.xml");
        assertRemedialActionNotImported("ara_1", NOT_YET_HANDLED_BY_OPEN_RAO);
        assertEquals(9, importedCrac.getFlowCnecs().size());
        assertFalse(cracCreationContext.getCreationReport().getReport().contains("[ADDED] CNEC \"French line 1 - FFR1AA1 ->FFR2AA1   - outage_1 - auto\" has no associated automaton. It will be cloned on the OUTAGE instant in order to be secured during preventive RAO."));
    }

    public static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of("/cracs/cseCrac_ep15us10-1case1.xml", "/reports/expectedReportNodeContent_cse_crac_ep15us10-1case1.txt"),
            Arguments.of("/cracs/cseCrac_ep15us10-1case5.xml", "/reports/expectedReportNodeContent_cse_crac_ep15us10-1case5.txt"),
            Arguments.of("/cracs/cse_crac_1.xml", "/reports/expectedReportNodeContent_cse_crac_1.txt"),
            Arguments.of("/cracs/cse_crac_2.xml", "/reports/expectedReportNodeContent_cse_crac_2.txt"),
            Arguments.of("/cracs/cse_crac_auto.xml", "/reports/expectedReportNodeContent_cse_crac_auto.txt"),
            Arguments.of("/cracs/cse_crac_empty_ra.xml", "/reports/expectedReportNodeContent_cse_crac_empty_ra.txt"),
            Arguments.of("/cracs/cse_crac_halflines.xml", "/reports/expectedReportNodeContent_cse_crac_halflines.txt"),
            Arguments.of("/cracs/cse_crac_inverted_pst.xml", "/reports/expectedReportNodeContent_cse_crac_inverted_pst.txt"),
            Arguments.of("/cracs/cse_crac_onConstraint.xml", "/reports/expectedReportNodeContent_cse_crac_onConstraint.txt"),
            Arguments.of("/cracs/cse_crac_onConstraintInSpecificCountry.xml", "/reports/expectedReportNodeContent_cse_crac_onConstraintInSpecificCountry.txt"),
            Arguments.of("/cracs/cse_crac_pct_limit.xml", "/reports/expectedReportNodeContent_cse_crac_pct_limit.txt"),
            Arguments.of("/cracs/cse_crac_transformer_cnec.xml", "/reports/expectedReportNodeContent_cse_crac_transformer_cnec.txt"),
            Arguments.of("/cracs/cse_crac_with_MNE.xml", "/reports/expectedReportNodeContent_cse_crac_with_MNE.txt"),
            Arguments.of("/cracs/cse_crac_with_hvdc.xml", "/reports/expectedReportNodeContent_cse_crac_with_hvdc.txt")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideParameters")
    void createCracwithReports(String cracFileName, String expectedReportFileName) throws IOException, URISyntaxException {
        reportNode = buildNewRootNode();

        setUp(cracFileName);

        String expected = Files.readString(Path.of(getClass().getResource(expectedReportFileName).toURI()));
        try (StringWriter writer = new StringWriter()) {
            reportNode.print(writer);
            String actual = writer.toString();
            assertEquals(expected, actual);
        }
    }
}
