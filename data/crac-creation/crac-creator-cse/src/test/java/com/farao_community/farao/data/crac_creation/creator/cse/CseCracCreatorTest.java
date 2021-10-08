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
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.network_action.TopologicalAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeType;
import com.farao_community.farao.data.crac_api.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.JsonCracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.BranchCnecCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.HvdcRangeActionCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.RemedialActionCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.outage.CseOutageCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.parameters.CseCracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cse.remedial_action.CsePstCreationContext;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;

import static com.farao_community.farao.data.crac_creation.creator.api.ImportStatus.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class CseCracCreatorTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private OffsetDateTime offsetDateTime;
    private CracCreationParameters parameters = new CracCreationParameters();
    private Crac importedCrac;
    private CseCracCreationContext cracCreationContext;

    private void setUp(String cracFileName, String networkFileName) {
        InputStream is = getClass().getResourceAsStream(cracFileName);
        CseCracImporter importer = new CseCracImporter();
        CseCrac cseCrac = importer.importNativeCrac(is);
        Network network = Importers.loadNetwork(networkFileName, getClass().getResourceAsStream(networkFileName));
        CseCracCreator cseCracCreator = new CseCracCreator();
        cracCreationContext = cseCracCreator.createCrac(cseCrac, network, offsetDateTime, parameters);
        importedCrac = cracCreationContext.getCrac();
    }

    private void setUp(String cracFileName) {
        setUp(cracFileName, "/networks/TestCase12Nodes_with_Xnodes.uct");
    }

    private void setUpWithHvdcNetwork(String cracFileName) {
        setUp(cracFileName, "/networks/TestCase16NodesWithHvdc.xiidm");
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

    private void assertHvdcRangeActionImported(String name, String networkElementName, String groupId, boolean inverted) {
        HvdcRangeActionCreationContext context = (HvdcRangeActionCreationContext) cracCreationContext.getRemedialActionCreationContext(name);
        assertTrue(context.isImported());
        assertEquals(networkElementName, context.getNativeNetworkElementId());
        assertEquals(inverted, context.isInverted());
        assertFalse(context.isAltered());
        assertNotNull(context.getCreatedRAId());
        assertNotNull(importedCrac.getHvdcRangeAction(context.getCreatedRAId()));
        assertEquals(groupId, importedCrac.getHvdcRangeAction(context.getCreatedRAId()).getGroupId().orElseThrow());
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
        assertEquals(3, importedCrac.getHvdcRangeActions().size());

        assertHvdcRangeActionImported("PRA_HVDC", "BBE2AA11 FFR3AA11 1", "PRA_HVDC + CRA_HVDC", false);
        assertHvdcRangeActionImported("CRA_HVDC", "BBE2AA11 FFR3AA11 1", "PRA_HVDC + CRA_HVDC", true);
        assertHvdcRangeActionImported("CRA_HVDC_2", "BBE2AA11 FFR3AA11 1", "PRA_HVDC + CRA_HVDC_2", true);

        assertOutageNotImported("fake_contingency_because_we_have_to", ELEMENT_NOT_FOUND_IN_NETWORK);
        assertCriticalBranchNotImported("fake_because_we_have_to", ELEMENT_NOT_FOUND_IN_NETWORK);
        assertRemedialActionNotImported("CRA_HVDC_fake", NOT_YET_HANDLED_BY_FARAO);
        assertRemedialActionNotImported("WEIRD_HVDC_WITH_2_HVDCNODES", INCONSISTENCY_IN_DATA);
    }

    @Test
    public void createCracWithHvdcWithNoCracCreationParameters() {
        parameters.addExtension(CseCracCreationParameters.class, new CseCracCreationParameters());
        setUpWithHvdcNetwork("/cracs/cse_crac_with_hvdc.xml");
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertTrue(importedCrac.getHvdcRangeAction("PRA_HVDC").getGroupId().isEmpty());
        assertTrue(importedCrac.getHvdcRangeAction("CRA_HVDC").getGroupId().isEmpty());
        assertTrue(importedCrac.getHvdcRangeAction("CRA_HVDC_2").getGroupId().isEmpty());
        assertEquals("FR", importedCrac.getHvdcRangeAction("PRA_HVDC").getOperator());
        assertEquals(2000, importedCrac.getHvdcRangeAction("PRA_HVDC").getRanges().get(0).getMax(), 1e-1);
        assertEquals(-100, importedCrac.getHvdcRangeAction("PRA_HVDC").getRanges().get(0).getMin(), 1e-1);
        assertEquals("AVAILABLE", importedCrac.getHvdcRangeAction("PRA_HVDC").getUsageRules().get(0).getUsageMethod().toString());
        assertEquals("BBE2AA11 FFR3AA11 1", importedCrac.getHvdcRangeAction("PRA_HVDC").getNetworkElement().getId());
    }

    @Test
    public void createCracWithHvdcWithCracCreationParameters() {
        parameters.addExtension(CseCracCreationParameters.class, new CseCracCreationParameters());
        parameters.getExtension(CseCracCreationParameters.class).setRangeActionGroupsAsString(List.of("PRA_HVDC + CRA_HVDC"));
        setUpWithHvdcNetwork("/cracs/cse_crac_with_hvdc.xml");
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals("PRA_HVDC + CRA_HVDC", importedCrac.getHvdcRangeAction("PRA_HVDC").getGroupId().get());
        assertEquals(importedCrac.getHvdcRangeAction("CRA_HVDC").getGroupId().get(), importedCrac.getHvdcRangeAction("PRA_HVDC").getGroupId().get());
        assertEquals("FR", importedCrac.getHvdcRangeAction("PRA_HVDC").getOperator());
        assertEquals(2000, importedCrac.getHvdcRangeAction("PRA_HVDC").getRanges().get(0).getMax(), 1e-1);
        assertEquals(-100, importedCrac.getHvdcRangeAction("PRA_HVDC").getRanges().get(0).getMin(), 1e-1);
        assertEquals("AVAILABLE", importedCrac.getHvdcRangeAction("PRA_HVDC").getUsageRules().get(0).getUsageMethod().toString());
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
        BranchCnecCreationContext cnec1context = cracCreationContext.getBranchCnecCreationContext("basecase_branch_1");
        assertTrue(cnec1context.isBaseCase());
        assertTrue(cnec1context.isImported());
        assertFalse(cnec1context.isDirectionInvertedInNetwork());
        assertTrue(cnec1context.getContingencyId().isEmpty());
        assertEquals(1, cnec1context.getCreatedCnecsIds().size());
        assertEquals("basecase_branch_1 - NNL2AA1 ->NNL3AA1  - preventive", cnec1context.getCreatedCnecsIds().get(Instant.PREVENTIVE));
    }

    @Test
    public void createCurativeCnecs() {
        setUp("/cracs/cse_crac_1.xml");
        BranchCnecCreationContext cnec2context = cracCreationContext.getBranchCnecCreationContext("Albertville - Grande Ile 1");
        assertFalse(cnec2context.isBaseCase());
        assertTrue(cnec2context.isImported());
        assertFalse(cnec2context.isDirectionInvertedInNetwork());
        assertEquals("outage_1", cnec2context.getContingencyId().get());
        assertEquals(2, cnec2context.getCreatedCnecsIds().size());
        assertEquals("Albertville - Grande Ile 1 - FFR1AA1 ->FFR2AA1   - outage_1 - outage", cnec2context.getCreatedCnecsIds().get(Instant.OUTAGE));
        assertEquals("Albertville - Grande Ile 1 - FFR1AA1 ->FFR2AA1   - outage_1 - curative", cnec2context.getCreatedCnecsIds().get(Instant.CURATIVE));
    }

    @Test
    public void doNotCreateAbsentFromNetworkCnec() {
        setUp("/cracs/cse_crac_1.xml");
        BranchCnecCreationContext cnec3context = cracCreationContext.getBranchCnecCreationContext("Albertville - La Coche");
        assertCriticalBranchNotImported("Albertville - La Coche", ELEMENT_NOT_FOUND_IN_NETWORK);
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
        assertTrue(na.getElementaryActions().stream().anyMatch(ea -> ea.getNetworkElement().getId().equals("FFR3AA1 _generator")));
        assertTrue(na.getElementaryActions().stream().anyMatch(ea -> ea.getNetworkElement().getId().equals("FFR2AA1 _generator")));
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

        FlowCnec outageCnec = importedCrac.getFlowCnec("Albertville - Grande Ile 1 - FFR1AA1 ->FFR2AA1   - outage_1 - outage");
        FlowCnec curativeCnec = importedCrac.getFlowCnec("Albertville - Grande Ile 1 - FFR1AA1 ->FFR2AA1   - outage_1 - curative");

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
    public void testPercentageThresholds() {
        setUp("/cracs/cse_crac_pct_limit.xml");

        FlowCnec flowCnec1 = importedCrac.getFlowCnec("basecase_branch_1 - NNL2AA1 ->NNL3AA1  - preventive");
        assertEquals(0.7, flowCnec1.getThresholds().iterator().next().max().get(), DOUBLE_TOLERANCE);
        assertTrue(flowCnec1.getThresholds().iterator().next().min().isEmpty());
        assertEquals(0.7 * 5000., flowCnec1.getUpperBound(Side.LEFT, Unit.AMPERE).get(), DOUBLE_TOLERANCE);

        FlowCnec flowCnec2 = importedCrac.getFlowCnec("basecase_branch_2 - NNL1AA1 ->NNL3AA1  - preventive");
        assertEquals(-1., flowCnec2.getThresholds().iterator().next().min().get(), DOUBLE_TOLERANCE);
        assertTrue(flowCnec2.getThresholds().iterator().next().max().isEmpty());
        assertEquals(-5000., flowCnec2.getLowerBound(Side.LEFT, Unit.AMPERE).get(), DOUBLE_TOLERANCE);

        FlowCnec flowCnec3 = importedCrac.getFlowCnec("basecase_branch_3 - NNL1AA1 ->NNL2AA1  - preventive");
        assertEquals(-0.2, flowCnec3.getThresholds().iterator().next().min().get(), DOUBLE_TOLERANCE);
        assertEquals(0.2, flowCnec3.getThresholds().iterator().next().max().get(), DOUBLE_TOLERANCE);
        assertEquals(-0.2 * 5000., flowCnec3.getLowerBound(Side.LEFT, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
        assertEquals(0.2 * 5000., flowCnec3.getUpperBound(Side.LEFT, Unit.AMPERE).get(), DOUBLE_TOLERANCE);
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
        assertEquals(2, cra3.getUsageRules().size()); // 2 OnConstraint on CNEC 1 and CNEC 2
        assertTrue(cra3.getUsageRules().get(0) instanceof OnFlowConstraint);
        assertTrue(cra3.getUsageRules().get(1) instanceof OnFlowConstraint);
        // cra_4
        RemedialAction<?> cra4 = importedCrac.getNetworkAction("cra_4");
        assertEquals(0, cra4.getUsageRules().size());
        // cra_5
        RemedialAction<?> cra5 = importedCrac.getNetworkAction("cra_5");
        assertEquals(1, cra5.getUsageRules().size()); // one OnConstraint on CNEC 2
        assertTrue(cra5.getUsageRules().get(0) instanceof OnFlowConstraint);
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

        // ra_2 should be inverted
        assertTrue(cracCreationContext.getRemedialActionCreationContext("ra_2") instanceof CsePstCreationContext);
        pstContext = (CsePstCreationContext) cracCreationContext.getRemedialActionCreationContext("ra_2");
        assertTrue(pstContext.isImported());
        assertEquals("ra_2", pstContext.getNativeId());
        assertEquals("PST_ra_2_BBE2AA1  BBE3AA1  1", pstContext.getCreatedRAId());
        assertTrue(pstContext.isInverted());
        assertFalse(pstContext.isAltered());
        assertEquals("BBE3AA1  BBE2AA1  1", pstContext.getNativeNetworkElementId());
        pstRangeAction = importedCrac.getPstRangeAction(pstContext.getCreatedRAId());
        assertEquals("BBE2AA1  BBE3AA1  1", pstRangeAction.getNetworkElement().getId());
        assertEquals(3, pstRangeAction.getInitialTap());
        assertEquals(RangeType.ABSOLUTE, pstRangeAction.getRanges().get(0).getRangeType());
        assertEquals(-10, pstRangeAction.getRanges().get(0).getMinTap());
        assertEquals(2, pstRangeAction.getRanges().get(0).getMaxTap());
    }

    @Test
    public void testImportBusBarChange() {
        parameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/CseCracCreationParameters_15_10_1_5.json"));
        setUp("/cracs/cseCrac_ep15us10-1case5.xml", "/networks/TestCase12Nodes_forCSE_3nodes.uct");

        assertEquals(2, importedCrac.getNetworkActions().size());
        assertTrue(cracCreationContext.getRemedialActionCreationContext("RA1").isImported());
        assertTrue(cracCreationContext.getRemedialActionCreationContext("RA2").isImported());

        NetworkAction ra1 = importedCrac.getNetworkAction("RA1");
        assertEquals(2, ra1.getElementaryActions().size());
        assertTrue(ra1.getElementaryActions().stream().allMatch(elementaryAction -> elementaryAction instanceof TopologicalAction));
        assertTrue(ra1.getElementaryActions().stream().anyMatch(elementaryAction ->
            ((TopologicalAction) elementaryAction).getActionType().equals(ActionType.OPEN) && elementaryAction.getNetworkElement().getId().equals("BBE1AA1X BBE1AA11 1")
        ));
        assertTrue(ra1.getElementaryActions().stream().anyMatch(elementaryAction ->
            ((TopologicalAction) elementaryAction).getActionType().equals(ActionType.CLOSE) && elementaryAction.getNetworkElement().getId().equals("BBE1AA1X BBE1AA12 1")
        ));

        NetworkAction ra2 = importedCrac.getNetworkAction("RA2");
        assertEquals(2, ra2.getElementaryActions().size());
        assertTrue(ra2.getElementaryActions().stream().allMatch(elementaryAction -> elementaryAction instanceof TopologicalAction));
        assertTrue(ra2.getElementaryActions().stream().anyMatch(elementaryAction ->
            ((TopologicalAction) elementaryAction).getActionType().equals(ActionType.OPEN) && elementaryAction.getNetworkElement().getId().equals("BBE1AA1X BBE1AA12 1")
        ));
        assertTrue(ra2.getElementaryActions().stream().anyMatch(elementaryAction ->
            ((TopologicalAction) elementaryAction).getActionType().equals(ActionType.CLOSE) && elementaryAction.getNetworkElement().getId().equals("BBE1AA1X BBE1AA11 1")
        ));
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
}
