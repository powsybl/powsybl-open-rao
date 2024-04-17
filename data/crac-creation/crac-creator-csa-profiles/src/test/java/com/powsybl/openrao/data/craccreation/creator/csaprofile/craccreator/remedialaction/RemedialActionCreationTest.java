package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.AUTO_INSTANT_ID;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.CURATIVE_INSTANT_ID;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.NETWORK;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.PREVENTIVE_INSTANT_ID;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertHasOnFlowConstraintUsageRule;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertPstRangeActionImported;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertRaNotImported;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemedialActionCreationTest {

    @Test
    void importRemedialActions() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/profiles/remedialactions/RemedialActions.zip", NETWORK);

        List<RemedialAction<?>> importedRemedialActions = cracCreationContext.getCrac().getRemedialActions().stream().sorted(Comparator.comparing(RemedialAction::getId)).toList();
        assertEquals(7, importedRemedialActions.size());

        assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(0), "remedial-action-1", "remedial-action-1", "BBE2AA1  BBE3AA1  1", null, null, null);
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-1", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(1), "remedial-action-2", "RA2", "BBE2AA1  BBE3AA1  1", null, null, null);
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", CURATIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(2), "remedial-action-3", "remedial-action-3", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(Optional.of(10), importedRemedialActions.get(2).getSpeed());
        assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-3", "contingency-1", CURATIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(3), "remedial-action-4", "RTE_RA4", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-4", "contingency-2", CURATIVE_INSTANT_ID, UsageMethod.FORCED);

        assertEquals(3, ((NetworkAction) importedRemedialActions.get(4)).getElementaryActions().size());
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-5", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(5), "remedial-action-6", "RTE_RA6", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(2, importedRemedialActions.get(5).getUsageRules().size());
        assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-6", "contingency-1", CURATIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-6", "contingency-2", CURATIVE_INSTANT_ID, UsageMethod.FORCED);

        assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(6), "remedial-action-9", "RTE_RA9", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(0, importedRemedialActions.get(6).getUsageRules().size());

        assertEquals(4, cracCreationContext.getRemedialActionCreationContexts().stream().filter(context -> !context.isImported()).toList().size());

        assertRaNotImported(cracCreationContext, "remedial-action-7", ImportStatus.NOT_FOR_RAO, "Remedial action remedial-action-7 will not be imported because normalAvailable is set to false");
        assertRaNotImported(cracCreationContext, "remedial-action-8", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-8 will not be imported because it is linked to a contingency but is not curative");
        assertRaNotImported(cracCreationContext, "remedial-action-10", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-10 will not be imported because of an irregular timeToImplement pattern");
        assertRaNotImported(cracCreationContext, "remedial-action-11", ImportStatus.NOT_FOR_RAO, "Remedial action remedial-action-11 will not be imported because it has no elementary action");
    }

    @Test
    void testUsageRulesCreation() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/profiles/remedialactions/UsageRules.zip", NETWORK);

        List<RemedialAction<?>> importedRemedialActions = cracCreationContext.getCrac().getRemedialActions().stream().sorted(Comparator.comparing(RemedialAction::getId)).toList();
        assertEquals(10, importedRemedialActions.size());

        assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(0), "remedial-action-1", "RTE_RA1", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(8, importedRemedialActions.get(0).getUsageRules().size());
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-1", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (assessed-element-1) - preventive", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (assessed-element-1) - RTE_CO1 - outage - TATL 60", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (assessed-element-1) - RTE_CO1 - auto - TATL 900", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (assessed-element-1) - RTE_CO2 - outage - TATL 60", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (assessed-element-1) - RTE_CO2 - auto - TATL 900", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (assessed-element-1) - RTE_CO2 - curative", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(2), "remedial-action-2", "RTE_RA2", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(3, importedRemedialActions.get(2).getUsageRules().size());
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", CURATIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-2", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative", CURATIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-2", "RTE_AE1 (assessed-element-1) - RTE_CO2 - curative", CURATIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(3), "remedial-action-3", "RTE_RA3", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(4, importedRemedialActions.get(3).getUsageRules().size());
        assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-3", "contingency-1", AUTO_INSTANT_ID, UsageMethod.FORCED);
        assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-3", "contingency-2", AUTO_INSTANT_ID, UsageMethod.FORCED);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-3", "RTE_AE1 (assessed-element-1) - RTE_CO1 - auto - TATL 900", AUTO_INSTANT_ID, UsageMethod.FORCED);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-3", "RTE_AE1 (assessed-element-1) - RTE_CO2 - auto - TATL 900", AUTO_INSTANT_ID, UsageMethod.FORCED);

        assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(4), "remedial-action-4", "RTE_RA4", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(4, importedRemedialActions.get(4).getUsageRules().size());
        assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-4", "contingency-1", CURATIVE_INSTANT_ID, UsageMethod.FORCED);
        assertHasOnContingencyStateUsageRule(cracCreationContext, "remedial-action-4", "contingency-2", CURATIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-4", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative", CURATIVE_INSTANT_ID, UsageMethod.FORCED);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-4", "RTE_AE1 (assessed-element-1) - RTE_CO2 - curative", CURATIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(5), "remedial-action-5", "RTE_RA5", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(2, importedRemedialActions.get(5).getUsageRules().size());
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-5", "RTE_AE1 (assessed-element-1) - RTE_CO2 - auto - TATL 900", AUTO_INSTANT_ID, UsageMethod.FORCED);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-5", "RTE_AE2 (assessed-element-2) - RTE_CO2 - auto - TATL 900", AUTO_INSTANT_ID, UsageMethod.FORCED);

        assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(6), "remedial-action-6", "RTE_RA6", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(2, importedRemedialActions.get(6).getUsageRules().size());
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-6", "RTE_AE1 (assessed-element-1) - RTE_CO2 - curative", CURATIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-6", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative", CURATIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(7), "remedial-action-7", "RTE_RA7", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(3, importedRemedialActions.get(7).getUsageRules().size());
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-7", "RTE_AE1 (assessed-element-1) - RTE_CO1 - auto - TATL 900", AUTO_INSTANT_ID, UsageMethod.FORCED);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-7", "RTE_AE1 (assessed-element-1) - RTE_CO2 - auto - TATL 900", AUTO_INSTANT_ID, UsageMethod.FORCED);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-7", "RTE_AE2 (assessed-element-2) - RTE_CO2 - auto - TATL 900", AUTO_INSTANT_ID, UsageMethod.FORCED);

        assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(8), "remedial-action-8", "RTE_RA8", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(3, importedRemedialActions.get(8).getUsageRules().size());
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-8", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative", CURATIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-8", "RTE_AE1 (assessed-element-1) - RTE_CO2 - curative", CURATIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-8", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative", CURATIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(9), "remedial-action-9", "RTE_RA9", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals(2, importedRemedialActions.get(9).getUsageRules().size());
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-9", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative", CURATIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-9", "RTE_AE1 (assessed-element-1) - RTE_CO2 - curative", CURATIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertPstRangeActionImported((PstRangeAction) importedRemedialActions.get(1), "remedial-action-10", "RTE_RA10", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertTrue(importedRemedialActions.get(1).getUsageRules().isEmpty());
    }
}
