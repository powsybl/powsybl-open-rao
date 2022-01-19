/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.fb_constraint.crac_creator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeType;
import com.farao_community.farao.data.crac_api.range_action.TapRange;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_api.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_api.usage_rule.OnState;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.BranchCnecCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.RemedialActionCreationContext;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.FbConstraint;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.importer.FbConstraintImporter;
import com.farao_community.farao.data.crac_impl.NetworkActionImpl;
import com.farao_community.farao.data.crac_loopflow_extension.LoopFlowThreshold;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.time.OffsetDateTime;

import static com.farao_community.farao.data.crac_creation.creator.api.ImportStatus.*;
import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class FbConstraintCracCreatorTest {

    private CracCreationParameters parameters;
    private FbConstraintCreationContext creationContext;

    @Before
    public void setUp() {
        parameters = new CracCreationParameters();
        parameters.setCracFactoryName(CracFactory.findDefault().getName());
    }

    private void assertCriticalBranchNotImported(String name, ImportStatus importStatus) {
        BranchCnecCreationContext context = creationContext.getBranchCnecCreationContext(name);
        assertNotNull(context);
        assertFalse(context.isImported());
        assertEquals(importStatus, context.getImportStatus());
        assertTrue(context.getCreatedCnecsIds().isEmpty());
    }

    private void assertComplexVariantNotImported(String name, ImportStatus importStatus) {
        RemedialActionCreationContext context = creationContext.getRemedialActionCreationContext(name);
        assertNotNull(context);
        assertFalse(context.isImported());
        assertEquals(importStatus, context.getImportStatus());
    }

    @Test
    public void importCracWithTimestampFilter() {
        Network network = Importers.loadNetwork("TestCase12Nodes_with_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase12Nodes_with_Xnodes.uct"));
        FbConstraint fbConstraint = new FbConstraintImporter().importNativeCrac(getClass().getResourceAsStream("/merged_cb/without_RA.xml"));

        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        creationContext = new FbConstraintCracCreator().createCrac(fbConstraint, network, timestamp, parameters);
        assertTrue(creationContext.isCreationSuccessful());
        assertEquals(2, creationContext.getCrac().getContingencies().size());
        assertEquals(10, creationContext.getCrac().getFlowCnecs().size());
        assertEquals(5, creationContext.getCrac().getStates().size());

        timestamp = OffsetDateTime.parse("2019-01-08T10:30Z");
        creationContext = new FbConstraintCracCreator().createCrac(fbConstraint, network, timestamp, parameters);
        assertTrue(creationContext.isCreationSuccessful());
        assertEquals(3, creationContext.getCrac().getContingencies().size());
        assertEquals(12, creationContext.getCrac().getFlowCnecs().size());
        assertEquals(7, creationContext.getCrac().getStates().size());

        timestamp = OffsetDateTime.parse("2019-01-10T10:30Z");
        creationContext = new FbConstraintCracCreator().createCrac(fbConstraint, network, timestamp, parameters);
        assertFalse(creationContext.isCreationSuccessful());
    }

    @Test
    public void importCriticalBranches() {
        Network network = Importers.loadNetwork("TestCase12Nodes_with_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase12Nodes_with_Xnodes.uct"));
        FbConstraint fbConstraint = new FbConstraintImporter().importNativeCrac(getClass().getResourceAsStream("/merged_cb/without_RA.xml"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T10:30Z");
        creationContext = new FbConstraintCracCreator().createCrac(fbConstraint, network, timestamp, parameters);
        Crac crac = creationContext.getCrac();

        // BE_CBCO_000001
        assertTrue(creationContext.getBranchCnecCreationContext("BE_CBCO_000001").isImported());
        assertTrue(creationContext.getBranchCnecCreationContext("BE_CBCO_000001").isBaseCase());
        assertEquals(1, creationContext.getBranchCnecCreationContext("BE_CBCO_000001").getCreatedCnecsIds().size());
        assertEquals("BE_CBCO_000001 - preventive", creationContext.getBranchCnecCreationContext("BE_CBCO_000001").getCreatedCnecsIds().get(Instant.PREVENTIVE));

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
        assertEquals("BE_CBCO_000003 - outage", creationContext.getBranchCnecCreationContext("BE_CBCO_000003").getCreatedCnecsIds().get(Instant.OUTAGE));
        assertEquals("BE_CBCO_000003 - curative", creationContext.getBranchCnecCreationContext("BE_CBCO_000003").getCreatedCnecsIds().get(Instant.CURATIVE));

        assertNotNull(crac.getFlowCnec("BE_CBCO_000003 - outage"));
        assertEquals("[BE-BE] BBE3 - BBE2 [DIR]", crac.getFlowCnec("BE_CBCO_000003 - outage").getName());
        assertEquals("BE", crac.getFlowCnec("BE_CBCO_000003 - outage").getOperator());
        assertEquals("BBE2AA1  BBE3AA1  1", crac.getFlowCnec("BE_CBCO_000003 - outage").getNetworkElement().getId());
        assertEquals(150., crac.getFlowCnec("BE_CBCO_000003 - outage").getReliabilityMargin(), 1e-6);
        assertEquals("outage", crac.getFlowCnec("BE_CBCO_000003 - outage").getState().getInstant().toString());
        assertEquals("BE_CO_00001", crac.getFlowCnec("BE_CBCO_000003 - outage").getState().getContingency().get().getId());

        assertNotNull(crac.getFlowCnec("BE_CBCO_000003 - curative"));
        assertEquals("[BE-BE] BBE3 - BBE2 [DIR]", crac.getFlowCnec("BE_CBCO_000003 - curative").getName());
        assertEquals("BE", crac.getFlowCnec("BE_CBCO_000003 - curative").getOperator());
        assertEquals("BBE2AA1  BBE3AA1  1", crac.getFlowCnec("BE_CBCO_000003 - curative").getNetworkElement().getId());
        assertEquals(150., crac.getFlowCnec("BE_CBCO_000003 - curative").getReliabilityMargin(), 1e-6);
        assertEquals("curative", crac.getFlowCnec("BE_CBCO_000003 - curative").getState().getInstant().toString());
        assertEquals("BE_CO_00001", crac.getFlowCnec("BE_CBCO_000003 - curative").getState().getContingency().get().getId());

        // number of critical branches vs. number of Cnecs
        assertEquals(7, creationContext.getBranchCnecCreationContexts().size());  // 2 preventive, 5 curative
        assertTrue(creationContext.getBranchCnecCreationContexts().stream().allMatch(BranchCnecCreationContext::isImported));
        assertEquals(12, crac.getFlowCnecs().size());  // 2 + 5*2
    }

    @Test
    public void importComplexVariants() {
        Network network = Importers.loadNetwork("TestCase12Nodes_with_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase12Nodes_with_Xnodes.uct"));
        FbConstraint fbConstraint = new FbConstraintImporter().importNativeCrac(getClass().getResourceAsStream("/merged_cb/with_RA.xml"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        creationContext = new FbConstraintCracCreator().createCrac(fbConstraint, network, timestamp, parameters);
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
        assertEquals(UsageMethod.AVAILABLE, rangeAction.getUsageRules().get(0).getUsageMethod());
        assertTrue(rangeAction.getUsageRules().get(0) instanceof FreeToUse);
        assertEquals(crac.getPreventiveState().getInstant(), ((FreeToUse) rangeAction.getUsageRules().get(0)).getInstant());
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
        assertEquals(UsageMethod.AVAILABLE, topoPra.getUsageRules().get(0).getUsageMethod());
        assertTrue(topoPra.getUsageRules().get(0) instanceof FreeToUse);
        assertEquals(crac.getPreventiveState().getInstant(), ((FreeToUse) topoPra.getUsageRules().get(0)).getInstant());
        assertEquals(NetworkActionImpl.class, topoPra.getClass());
        assertEquals(2, topoPra.getElementaryActions().size());

        // TOPO CRA
        NetworkAction topoCra = crac.getNetworkAction("RA_FR_0002");
        assertEquals(1, topoCra.getNetworkElements().size());
        assertEquals("CRA_TOPO_FR", topoCra.getName());
        assertEquals("FR", topoCra.getOperator());
        assertEquals(2, topoCra.getUsageRules().size());
        assertEquals(UsageMethod.AVAILABLE, topoCra.getUsageRules().get(0).getUsageMethod());
        assertTrue(topoCra.getUsageRules().get(0) instanceof OnState);
        assertEquals(NetworkActionImpl.class, topoCra.getClass());
        assertEquals(1, topoCra.getElementaryActions().size());

        // Creation Context
        assertEquals(3, creationContext.getRemedialActionCreationContexts().size());
        assertTrue(creationContext.getRemedialActionCreationContext("RA_BE_0001").isImported());
        assertTrue(creationContext.getRemedialActionCreationContext("RA_FR_0001").isImported());
        assertTrue(creationContext.getRemedialActionCreationContext("RA_FR_0002").isImported());

        ComplexVariantCreationContext pstContext = creationContext.getRemedialActionCreationContext("RA_BE_0001");
        assertNotNull(pstContext);
        assertEquals(ImportStatus.IMPORTED, pstContext.getImportStatus());
        assertEquals("RA_BE_0001", pstContext.getNativeId());
        assertEquals("RA_BE_0001", pstContext.getCreatedRAId());
        assertTrue(pstContext.isImported());
        assertFalse(pstContext.isAltered());
        assertTrue(pstContext instanceof PstComplexVariantCreationContext);
        assertFalse(((PstComplexVariantCreationContext) pstContext).isInverted());
        assertEquals("BBE2AA1  BBE3AA1  1", ((PstComplexVariantCreationContext) pstContext).getNativeNetworkElementId());

        ComplexVariantCreationContext topoContext = creationContext.getRemedialActionCreationContext("RA_FR_0001");
        assertNotNull(topoContext);
        assertEquals(ImportStatus.IMPORTED, topoContext.getImportStatus());
        assertEquals("RA_FR_0001", topoContext.getNativeId());
        assertEquals("RA_FR_0001", topoContext.getCreatedRAId());
        assertTrue(topoContext.isImported());
        assertFalse(topoContext.isAltered());
    }

    @Test
    public void importMnecs() {
        Network network = Importers.loadNetwork("TestCase12Nodes_with_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase12Nodes_with_Xnodes.uct"));
        FbConstraint fbConstraint = new FbConstraintImporter().importNativeCrac(getClass().getResourceAsStream("/merged_cb/MNEC_test.xml"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        creationContext = new FbConstraintCracCreator().createCrac(fbConstraint, network, timestamp, parameters);
        Crac crac = creationContext.getCrac();

        assertEquals(3, crac.getFlowCnecs().size());
        assertTrue(crac.getFlowCnec("BE_CBCO_000001 - preventive").isOptimized());
        assertFalse(crac.getFlowCnec("BE_CBCO_000001 - preventive").isMonitored());
        assertFalse(crac.getFlowCnec("BE_CBCO_000002 - preventive").isOptimized());
        assertTrue(crac.getFlowCnec("BE_CBCO_000002 - preventive").isMonitored());
        assertTrue(crac.getFlowCnec("BE_CBCO_000003 - preventive").isOptimized());
        assertTrue(crac.getFlowCnec("BE_CBCO_000003 - preventive").isMonitored());

        assertCriticalBranchNotImported("BE_CBCO_000004", NOT_FOR_RAO);
    }

    @Test
    public void importThresholds() {
        Network network = Importers.loadNetwork("TestCase12Nodes_for_thresholds_test.uct", getClass().getResourceAsStream("/network/TestCase12Nodes_for_thresholds_test.uct"));
        FbConstraint fbConstraint = new FbConstraintImporter().importNativeCrac(getClass().getResourceAsStream("/merged_cb/thresholds_test.xml"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        creationContext = new FbConstraintCracCreator().createCrac(fbConstraint, network, timestamp, parameters);
        Crac crac = creationContext.getCrac();

        assertEquals(9, crac.getFlowCnecs().size());

        BranchThreshold threshold;
        // No threshold specification will be set to default relative-100
        assertEquals(1, crac.getFlowCnec("CBCO_000001 - preventive").getThresholds().size());
        threshold = crac.getFlowCnec("CBCO_000001 - preventive").getThresholds().iterator().next();
        assertEquals(Unit.PERCENT_IMAX, threshold.getUnit());
        assertEquals(1., threshold.max().orElseThrow(), 0.1);

        // ImaxA set to 200
        assertEquals(1, crac.getFlowCnec("CBCO_000002 - preventive").getThresholds().size());
        threshold = crac.getFlowCnec("CBCO_000002 - preventive").getThresholds().iterator().next();
        assertEquals(Unit.AMPERE, threshold.getUnit());
        assertEquals(200., threshold.max().orElseThrow(), 0.1);

        // ImaxFactor set to 0.8
        assertEquals(1, crac.getFlowCnec("CBCO_000003 - preventive").getThresholds().size());
        threshold = crac.getFlowCnec("CBCO_000003 - preventive").getThresholds().iterator().next();
        assertEquals(Unit.PERCENT_IMAX, threshold.getUnit());
        assertEquals(0.8, threshold.max().orElseThrow(), 0.1);

        // PermanentImaxA set to 300
        assertEquals(1, crac.getFlowCnec("CBCO_000004 - preventive").getThresholds().size());
        threshold = crac.getFlowCnec("CBCO_000004 - preventive").getThresholds().iterator().next();
        assertEquals(Unit.AMPERE, threshold.getUnit());
        assertEquals(300., threshold.max().orElseThrow(), 0.1);

        // PermanentImaxFactor set to 1.2
        assertEquals(1, crac.getFlowCnec("CBCO_000005 - preventive").getThresholds().size());
        threshold = crac.getFlowCnec("CBCO_000005 - preventive").getThresholds().iterator().next();
        assertEquals(Unit.PERCENT_IMAX, threshold.getUnit());
        assertEquals(1.2, threshold.max().orElseThrow(), 0.1);

        // TemporaryImaxA set to 200 will be set to default relative-100
        assertEquals(1, crac.getFlowCnec("CBCO_000006 - preventive").getThresholds().size());
        threshold = crac.getFlowCnec("CBCO_000006 - preventive").getThresholds().iterator().next();
        assertEquals(Unit.PERCENT_IMAX, threshold.getUnit());
        assertEquals(1., threshold.max().orElseThrow(), 0.1);

        // TemporaryImaxFactor set to 0.8 will be set to default relative-100
        assertEquals(1, crac.getFlowCnec("CBCO_000007 - preventive").getThresholds().size());
        threshold = crac.getFlowCnec("CBCO_000007 - preventive").getThresholds().iterator().next();
        assertEquals(Unit.PERCENT_IMAX, threshold.getUnit());
        assertEquals(1., threshold.max().orElseThrow(), 0.1);

        // ImaxA, PermanentImaxA and TemporaryImaxA set to 300, 200 and 100
        assertEquals(1, crac.getFlowCnec("CBCO_000008 - preventive").getThresholds().size());
        assertEquals(300., crac.getFlowCnec("CBCO_000008 - preventive").getUpperBound(Side.LEFT, Unit.AMPERE).orElseThrow(), 0.1);
    }

    @Test
    public void importLoopFlowExtensions() {
        Network network = Importers.loadNetwork("TestCase12Nodes_with_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase12Nodes_with_Xnodes.uct"));
        FbConstraint fbConstraint = new FbConstraintImporter().importNativeCrac(getClass().getResourceAsStream("/merged_cb/with_crosszonal_branches.xml"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        creationContext = new FbConstraintCracCreator().createCrac(fbConstraint, network, timestamp, parameters);
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
    public void testImportHvdcvhOutage() {
        Network network = Importers.loadNetwork("TestCase12NodesHvdc.uct", getClass().getResourceAsStream("/network/TestCase12NodesHvdc.uct"));
        FbConstraint fbConstraint = new FbConstraintImporter().importNativeCrac(getClass().getResourceAsStream("/merged_cb/hvdcvh-outage.xml"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        creationContext = new FbConstraintCracCreator().createCrac(fbConstraint, network, timestamp, parameters);
        Crac crac = creationContext.getCrac();

        Contingency contingency = crac.getFlowCnec("Cnec1 - curative").getState().getContingency().orElse(null);
        assertNotNull(contingency);
        assertEquals(2, contingency.getNetworkElements().size());
        assertTrue(contingency.getNetworkElements().stream().anyMatch(ne -> ne.getId().equals("DDE3AA1  XLI_OB1A 1")));
        assertTrue(contingency.getNetworkElements().stream().anyMatch(ne -> ne.getId().equals("BBE2AA1  XLI_OB1B 1")));
    }

    @Test
    public void testImportAndCleanCriticalBranches() {
        Network network = Importers.loadNetwork("TestCase_severalVoltageLevels_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase_severalVoltageLevels_Xnodes.uct"));
        FbConstraint fbConstraint = new FbConstraintImporter().importNativeCrac(getClass().getResourceAsStream("/merged_cb/critical_branches.xml"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        creationContext = new FbConstraintCracCreator().createCrac(fbConstraint, network, timestamp, parameters);
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

        assertCriticalBranchNotImported("FR_CBCO_000001", ELEMENT_NOT_FOUND_IN_NETWORK); //unknown branch
        assertCriticalBranchNotImported("FR_CBCO_000002", INCONSISTENCY_IN_DATA); //unknown outage
        assertCriticalBranchNotImported("FR_CBCO_000003", NOT_FOR_RAO); //not a MNEC, not a Cnec

        assertEquals(6, crac.getFlowCnecs().size());
    }

    @Test
    public void testImportAndCleanComplexVariants() {
        Network network = Importers.loadNetwork("TestCase_severalVoltageLevels_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase_severalVoltageLevels_Xnodes.uct"));
        FbConstraint fbConstraint = new FbConstraintImporter().importNativeCrac(getClass().getResourceAsStream("/merged_cb/complex_variants.xml"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        creationContext = new FbConstraintCracCreator().createCrac(fbConstraint, network, timestamp, parameters);
        Crac crac = creationContext.getCrac();

        assertEquals(13, creationContext.getRemedialActionCreationContexts().size());
        assertTrue(creationContext.getRemedialActionCreationContext("RA_BE_0001").isImported());
        assertComplexVariantNotImported("RA_BE_0002", ELEMENT_NOT_FOUND_IN_NETWORK); //unknown branch
        assertComplexVariantNotImported("RA_BE_0003", INCONSISTENCY_IN_DATA); //two PST actions
        assertComplexVariantNotImported("RA_BE_0004", INCOMPLETE_DATA); //two action set
        assertTrue(creationContext.getRemedialActionCreationContext("RA_FR_0001").isImported());
        assertTrue(creationContext.getRemedialActionCreationContext("RA_FR_0002").isImported()); //same network element/action/usage rule as RA_FR_0005, prioritized due to alphabetical order
        assertComplexVariantNotImported("RA_FR_0003", INCOMPLETE_DATA); //no CO list
        assertComplexVariantNotImported("RA_FR_0004", INCONSISTENCY_IN_DATA); //preventive and curative
        assertComplexVariantNotImported("RA_FR_0005", INCONSISTENCY_IN_DATA); //same network element/usage rule as RA_FR_0002
        assertComplexVariantNotImported("RA_FR_0006", INCONSISTENCY_IN_DATA); //all outage in CO list not ok
        assertComplexVariantNotImported("RA_FR_0007", ELEMENT_NOT_FOUND_IN_NETWORK); //unknown branch
        assertTrue(creationContext.getRemedialActionCreationContext("RA_FR_0008").isImported()); //same network element/action as RA_FR_0002, but not same usage rule
        assertTrue(creationContext.getRemedialActionCreationContext("RA_FR_0009").isImported()); //same network element/usage rule as RA_FR_0002, but not same action

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(4, crac.getNetworkActions().size());

        assertEquals(1, crac.getNetworkAction("RA_FR_0002").getUsageRules().size()); // one cannot be interpreted
    }

    @Test
    public void testInvertPstRangeAction() {
        Network network = Importers.loadNetwork("TestCase_severalVoltageLevels_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase_severalVoltageLevels_Xnodes.uct"));
        FbConstraint fbConstraint = new FbConstraintImporter().importNativeCrac(getClass().getResourceAsStream("/merged_cb/complex_variants_invert.xml"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        creationContext = new FbConstraintCracCreator().createCrac(fbConstraint, network, timestamp, parameters);
        Crac crac = creationContext.getCrac();

        // RA_BE_0001 should not be inverted
        assertTrue(creationContext.getRemedialActionCreationContext("RA_BE_0001") instanceof PstComplexVariantCreationContext);
        PstComplexVariantCreationContext pstContext = (PstComplexVariantCreationContext) creationContext.getRemedialActionCreationContext("RA_BE_0001");
        assertTrue(pstContext.isImported());
        assertEquals("RA_BE_0001", pstContext.getNativeId());
        assertEquals("RA_BE_0001", pstContext.getCreatedRAId());
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
        assertEquals("RA_BE_0002", pstContext.getNativeId());
        assertEquals("RA_BE_0002", pstContext.getCreatedRAId());
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
    public void testWrongTsCreationContext() {
        Network network = Importers.loadNetwork("TestCase12Nodes_with_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase12Nodes_with_Xnodes.uct"));
        FbConstraint fbConstraint = new FbConstraintImporter().importNativeCrac(getClass().getResourceAsStream("/merged_cb/wrong_ts.xml"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T10:30Z");
        creationContext = new FbConstraintCracCreator().createCrac(fbConstraint, network, timestamp, parameters);
        Crac crac = creationContext.getCrac();

        assertEquals(2, creationContext.getCreationReport().getReport().size());

        assertEquals(1, crac.getCnecs().size());
        assertCriticalBranchNotImported("BE_CBCO_000001", NOT_FOR_REQUESTED_TIMESTAMP);
        assertTrue(creationContext.getBranchCnecCreationContext("BE_CBCO_000002").isImported());

        assertEquals(1, crac.getRemedialActions().size());
        assertComplexVariantNotImported("RA_BE_0001", NOT_FOR_REQUESTED_TIMESTAMP);
        assertTrue(creationContext.getRemedialActionCreationContext("RA_FR_0001").isImported());
    }

    @Test
    public void testDuplicatePsts() {
        Network network = Importers.loadNetwork("TestCase_severalVoltageLevels_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase_severalVoltageLevels_Xnodes.uct"));
        FbConstraint fbConstraint = new FbConstraintImporter().importNativeCrac(getClass().getResourceAsStream("/merged_cb/complex_variants_duplicate_psts.xml"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T00:30Z");
        creationContext = new FbConstraintCracCreator().createCrac(fbConstraint, network, timestamp, parameters);
        Crac crac = creationContext.getCrac();

        assertEquals(2, creationContext.getCreationReport().getReport().size());

        // RA_BE_0001 is one same PST as RA_BE_0002
        // RA_BE_0002 has been prioritized due to alphabetical order
        assertTrue(creationContext.getRemedialActionCreationContext("RA_BE_0001").isImported());
        assertNotNull(crac.getRemedialAction("RA_BE_0001"));

        assertFalse(creationContext.getRemedialActionCreationContext("RA_BE_0002").isImported());
        assertComplexVariantNotImported("RA_BE_0002", INCONSISTENCY_IN_DATA);
        assertNull(creationContext.getRemedialActionCreationContext("RA_BE_0002").getCreatedRAId());
        assertNull(crac.getRemedialAction("RA_BE_0002"));

        // RA_BE_0003 is one same PST as RA_BE_0004
        // RA_BE_0004 has been prioritized as it has a groupId
        assertFalse(creationContext.getRemedialActionCreationContext("RA_BE_0003").isImported());
        assertComplexVariantNotImported("RA_BE_0003", INCONSISTENCY_IN_DATA);
        assertNull(creationContext.getRemedialActionCreationContext("RA_BE_0003").getCreatedRAId());
        assertNull(crac.getRemedialAction("RA_BE_0003"));

        assertTrue(creationContext.getRemedialActionCreationContext("RA_BE_0004").isImported());
        assertNotNull(crac.getRemedialAction("RA_BE_0004"));
    }
}

