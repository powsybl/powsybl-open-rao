/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.ElementaryAction;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileElementaryCreationContext;
import com.farao_community.farao.data.crac_impl.InjectionSetpointImpl;
import com.farao_community.farao.data.crac_impl.OnContingencyStateImpl;
import com.farao_community.farao.data.crac_impl.TopologicalActionImpl;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationTestUtil.assertRaNotImported;
import static com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoRemedialActionTest {

    @Test
    void importAutoRemedialActionTC2() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_TestConfiguration_TC2_27Apr2023.zip");
        List<RemedialAction<?>> autoRemedialActionList = cracCreationContext.getCrac().getRemedialActions().stream().filter(ra -> ra.getUsageRules().stream().anyMatch(usageRule -> usageRule.getInstant().isAuto())).toList();
        assertEquals(0, autoRemedialActionList.size());
        assertRaNotImported(cracCreationContext, "31d41e36-11c8-417b-bafb-c410d4391898", ImportStatus.INCONSISTENCY_IN_DATA, "Auto Remedial action 31d41e36-11c8-417b-bafb-c410d4391898 will not be imported because it has no associated RemedialActionScheme");
    }

    @Test
    void importAutoRemedialActions() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA-83.zip");

        // Imported ARAs

        List<RemedialAction<?>> importedSps = cracCreationContext.getCrac().getRemedialActions().stream().filter(ra -> ra.getUsageRules().size() == 1 && ra.getUsageRules().stream().toList().get(0).getInstant().isAuto()).toList();
        assertEquals(2, importedSps.size());

        PstRangeAction pstSps = cracCreationContext.getCrac().getPstRangeAction("pst-sps");
        OnContingencyStateImpl pstSpsUsageRule = (OnContingencyStateImpl) pstSps.getUsageRules().iterator().next();
        assertEquals("PST SPS", pstSps.getName());
        assertEquals("BBE2AA1  BBE3AA1  1", pstSps.getNetworkElement().getId());
        assertEquals(-5, pstSps.getRanges().get(0).getMinTap());
        assertEquals(7, pstSps.getRanges().get(0).getMaxTap());
        assertEquals("contingency", pstSpsUsageRule.getContingency().getId());
        assertEquals(InstantKind.AUTO, pstSpsUsageRule.getInstant().getKind());
        assertEquals(UsageMethod.FORCED, pstSpsUsageRule.getUsageMethod());

        NetworkAction networkSps = cracCreationContext.getCrac().getNetworkAction("network-sps");
        OnContingencyStateImpl networkSpsUsageRule = (OnContingencyStateImpl) networkSps.getUsageRules().iterator().next();
        List<ElementaryAction> elementaryActions = networkSps.getElementaryActions().stream().sorted(Comparator.comparing(ElementaryAction::toString)).toList();
        InjectionSetpointImpl injectionSetpoint = (InjectionSetpointImpl) elementaryActions.get(0);
        TopologicalActionImpl topologicalAction = (TopologicalActionImpl) elementaryActions.get(1);
        assertEquals("Network SPS", networkSps.getName());
        assertEquals(2, elementaryActions.size());
        assertEquals("BBE1AA1  BBE4AA1  1", topologicalAction.getNetworkElement().getId());
        assertEquals(ActionType.OPEN, topologicalAction.getActionType());
        assertEquals("FFR1AA1 _generator", injectionSetpoint.getNetworkElement().getId());
        assertEquals(75.0, injectionSetpoint.getSetpoint());
        assertEquals("contingency", networkSpsUsageRule.getContingency().getId());
        assertEquals(InstantKind.AUTO, networkSpsUsageRule.getInstant().getKind());
        assertEquals(UsageMethod.FORCED, networkSpsUsageRule.getUsageMethod());

        // Not imported ARAs

        List<CsaProfileElementaryCreationContext> notImportedSps = cracCreationContext.getRemedialActionCreationContexts().stream().filter(ra -> !ra.isImported()).toList();
        assertEquals(13, notImportedSps.size());

        assertRaNotImported(cracCreationContext, "sps-with-multiple-remedial-action-schemes", ImportStatus.INCONSISTENCY_IN_DATA, "Auto Remedial action sps-with-multiple-remedial-action-schemes will not be imported because it has several conflictual RemedialActionSchemes");
        assertRaNotImported(cracCreationContext, "sps-with-multiple-stages", ImportStatus.INCONSISTENCY_IN_DATA, "Auto Remedial action sps-with-multiple-stages will not be imported because it has several conflictual Stages");
        assertRaNotImported(cracCreationContext, "pst-sps-without-speed", ImportStatus.INCONSISTENCY_IN_DATA, "Auto Remedial action pst-sps-without-speed will not be imported because an auto PST range action must have a speed defined");
        assertRaNotImported(cracCreationContext, "preventive-sps", ImportStatus.INCONSISTENCY_IN_DATA, "Auto Remedial action preventive-sps will not be imported because auto remedial action musty be of curative kind");
        assertRaNotImported(cracCreationContext, "not-forced-sps", ImportStatus.INCONSISTENCY_IN_DATA, "Auto Remedial action not-forced-sps will not be imported because it must be linked to the contingency contingency with an 'included' ElementCombinationConstraintKind");
        assertRaNotImported(cracCreationContext, "sps-with-disabled-link-to-contingency", ImportStatus.NOT_FOR_RAO, "Remedial action 'sps-with-disabled-link-to-contingency' will not be imported because field 'normalEnabled' in 'ContingencyWithRemedialAction' must be true or empty");
        assertRaNotImported(cracCreationContext, "sps-with-unarmed-remedial-action-scheme", ImportStatus.NOT_FOR_RAO, "Auto Remedial action sps-with-unarmed-remedial-action-scheme will not be imported because RemedialActionScheme 5f7796db-a662-488d-a938-39c8a2b36055 is not armed");
        assertRaNotImported(cracCreationContext, "not-sips-sps", ImportStatus.INCONSISTENCY_IN_DATA, "Auto Remedial action not-sips-sps will not be imported because of an unsupported kind for remedial action schedule (only SIPS allowed)");
        assertRaNotImported(cracCreationContext, "sps-without-remedial-action-scheme", ImportStatus.INCONSISTENCY_IN_DATA, "Auto Remedial action sps-without-remedial-action-scheme will not be imported because it has no associated RemedialActionScheme");
        assertRaNotImported(cracCreationContext, "sps-without-stage", ImportStatus.INCONSISTENCY_IN_DATA, "Auto Remedial action sps-without-stage will not be imported because it has no associated Stage");
        assertRaNotImported(cracCreationContext, "sps-without-grid-state-alteration-collection", ImportStatus.INCONSISTENCY_IN_DATA, "Auto Remedial action sps-without-grid-state-alteration-collection will not be imported because it has no associated GridStateAlterationCollection");
        assertRaNotImported(cracCreationContext, "sps-without-elementary-actions", ImportStatus.INCONSISTENCY_IN_DATA, "Auto Remedial action sps-without-elementary-actions will not be imported because there is no elementary action for that ARA");
        assertRaNotImported(cracCreationContext, "sps-without-contingency", ImportStatus.INCONSISTENCY_IN_DATA, "Auto Remedial action sps-without-contingency will not be imported because no contingency is linked to the remedial action");
    }
}
