package com.powsybl.openrao.data.crac.io.csaprofiles.craccreator.remedialaction;

import com.powsybl.openrao.data.crac.io.csaprofiles.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.crac.io.csaprofiles.craccreator.CsaProfileCracCreationTestUtil;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemedialActionCreationTest {

    @Test
    void importRemedialActions() {
        CsaProfileCracCreationContext cracCreationContext = CsaProfileCracCreationTestUtil.getCsaCracCreationContext("/profiles/remedialactions/RemedialActions.zip", CsaProfileCracCreationTestUtil.NETWORK);

        List<RemedialAction<?>> importedRemedialActions = cracCreationContext.getCrac().getRemedialActions().stream().sorted(Comparator.comparing(RemedialAction::getId)).toList();
        assertEquals(7, importedRemedialActions.size());

        CsaProfileCracCreationTestUtil.assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(0), "remedial-action-1", "remedial-action-1", "BBE2AA1  BBE3AA1  1", null, null, null);
        CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-1", CsaProfileCracCreationTestUtil.PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        CsaProfileCracCreationTestUtil.assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(1), "remedial-action-2", "RA2", "BBE2AA1  BBE3AA1  1", null, null, null);
        CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", CsaProfileCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);

        CsaProfileCracCreationTestUtil.assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(2), "remedial-action-3", "remedial-action-3", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(Optional.of(10), importedRemedialActions.get(2).getSpeed());
        CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-3", "contingency-1", CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-3", "contingency-1", CsaProfileCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-3", "contingency-1", CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);

        CsaProfileCracCreationTestUtil.assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(3), "remedial-action-4", "RTE_RA4", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-4", "contingency-2", CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.FORCED);
        CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-4", "contingency-2", CsaProfileCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.FORCED);
        CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-4", "contingency-2", CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID, UsageMethod.FORCED);

        assertEquals(3, ((NetworkAction) importedRemedialActions.get(4)).getElementaryActions().size());
        CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-5", CsaProfileCracCreationTestUtil.PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        CsaProfileCracCreationTestUtil.assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(5), "remedial-action-6", "RTE_RA6", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(6, importedRemedialActions.get(5).getUsageRules().size());
        CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-6", "contingency-1", CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-6", "contingency-1", CsaProfileCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-6", "contingency-1", CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-6", "contingency-2", CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.FORCED);
        CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-6", "contingency-2", CsaProfileCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.FORCED);
        CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-6", "contingency-2", CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID, UsageMethod.FORCED);

        CsaProfileCracCreationTestUtil.assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(6), "remedial-action-9", "RTE_RA9", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(0, importedRemedialActions.get(6).getUsageRules().size());

        assertEquals(3, cracCreationContext.getRemedialActionCreationContexts().stream().filter(context -> !context.isImported()).toList().size());

        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "remedial-action-7", ImportStatus.NOT_FOR_RAO, "Remedial action remedial-action-7 will not be imported because normalAvailable is set to false");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "remedial-action-8", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-8 will not be imported because it is linked to a contingency but is not curative");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "remedial-action-10", ImportStatus.NOT_FOR_RAO, "Remedial action remedial-action-10 will not be imported because it has no elementary action");
    }

    @Test
    void testUsageRulesCreation() {
        CsaProfileCracCreationContext cracCreationContext = CsaProfileCracCreationTestUtil.getCsaCracCreationContext("/profiles/remedialactions/UsageRules.zip", CsaProfileCracCreationTestUtil.NETWORK);

        List<RemedialAction<?>> importedRemedialActions = cracCreationContext.getCrac().getRemedialActions().stream().sorted(Comparator.comparing(RemedialAction::getId)).toList();
        assertEquals(10, importedRemedialActions.size());

        CsaProfileCracCreationTestUtil.assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(0), "remedial-action-1", "RTE_RA1", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(1, importedRemedialActions.get(0).getUsageRules().size());
        CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-1", CsaProfileCracCreationTestUtil.PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        CsaProfileCracCreationTestUtil.assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(2), "remedial-action-2", "RTE_RA2", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(3, importedRemedialActions.get(2).getUsageRules().size());
        CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", CsaProfileCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);

        CsaProfileCracCreationTestUtil.assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(3), "remedial-action-3", "RTE_RA3", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(2, importedRemedialActions.get(3).getUsageRules().size());
        CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-3", "contingency-1", CsaProfileCracCreationTestUtil.AUTO_INSTANT_ID, UsageMethod.FORCED);
        CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-3", "contingency-2", CsaProfileCracCreationTestUtil.AUTO_INSTANT_ID, UsageMethod.FORCED);

        CsaProfileCracCreationTestUtil.assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(4), "remedial-action-4", "RTE_RA4", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(6, importedRemedialActions.get(4).getUsageRules().size());
        CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-4", "contingency-1", CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.FORCED);
        CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-4", "contingency-1", CsaProfileCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.FORCED);
        CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-4", "contingency-1", CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID, UsageMethod.FORCED);
        CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-4", "contingency-2", CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-4", "contingency-2", CsaProfileCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-4", "contingency-2", CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);

        CsaProfileCracCreationTestUtil.assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(5), "remedial-action-5", "RTE_RA5", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(2, importedRemedialActions.get(5).getUsageRules().size());
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-5", "RTE_AE2 (assessed-element-2) - RTE_CO2 - auto - ONE - TATL 900", CsaProfileCracCreationTestUtil.AUTO_INSTANT_ID, UsageMethod.FORCED, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-5", "RTE_AE2 (assessed-element-2) - RTE_CO2 - auto - TWO - TATL 900", CsaProfileCracCreationTestUtil.AUTO_INSTANT_ID, UsageMethod.FORCED, FlowCnec.class);

        CsaProfileCracCreationTestUtil.assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(6), "remedial-action-6", "RTE_RA6", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(12, importedRemedialActions.get(6).getUsageRules().size());
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-6", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 1 - ONE - TATL 900", CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-6", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 1 - TWO - TATL 900", CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-6", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 2 - ONE", CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-6", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 2 - TWO", CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-6", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 3 - ONE", CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-6", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 3 - TWO", CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-6", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 2 - ONE", CsaProfileCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-6", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 2 - TWO", CsaProfileCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-6", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 3 - ONE", CsaProfileCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-6", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 3 - TWO", CsaProfileCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-6", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 3 - ONE", CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-6", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 3 - TWO", CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);

        CsaProfileCracCreationTestUtil.assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(7), "remedial-action-7", "RTE_RA7", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(2, importedRemedialActions.get(7).getUsageRules().size());
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-7", "RTE_AE2 (assessed-element-2) - RTE_CO2 - auto - ONE - TATL 900", CsaProfileCracCreationTestUtil.AUTO_INSTANT_ID, UsageMethod.FORCED, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-7", "RTE_AE2 (assessed-element-2) - RTE_CO2 - auto - TWO - TATL 900", CsaProfileCracCreationTestUtil.AUTO_INSTANT_ID, UsageMethod.FORCED, FlowCnec.class);

        CsaProfileCracCreationTestUtil.assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(8), "remedial-action-8", "RTE_RA8", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(12, importedRemedialActions.get(8).getUsageRules().size());
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-8", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 1 - ONE - TATL 900", CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-8", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 1 - TWO - TATL 900", CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-8", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 2 - ONE", CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-8", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 2 - TWO", CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-8", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 3 - ONE", CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-8", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 3 - TWO", CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-8", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 2 - ONE", CsaProfileCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-8", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 2 - TWO", CsaProfileCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-8", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 3 - ONE", CsaProfileCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-8", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 3 - TWO", CsaProfileCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-8", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 3 - ONE", CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);
        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-8", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 3 - TWO", CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE, FlowCnec.class);

        CsaProfileCracCreationTestUtil.assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(9), "remedial-action-9", "RTE_RA9", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(0, importedRemedialActions.get(9).getUsageRules().size());

        CsaProfileCracCreationTestUtil.assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(1), "remedial-action-10", "RTE_RA10", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertTrue(importedRemedialActions.get(1).getUsageRules().isEmpty());
    }
}
