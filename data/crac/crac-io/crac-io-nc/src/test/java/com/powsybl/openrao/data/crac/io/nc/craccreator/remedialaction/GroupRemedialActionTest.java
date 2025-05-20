/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction;

import com.powsybl.action.Action;
import com.powsybl.action.GeneratorAction;
import com.powsybl.action.SwitchAction;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationContext;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationTestUtil;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.usagerule.OnInstant;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupRemedialActionTest {

    @Test
    void importGroupedRemedialActions() {
        NcCracCreationContext cracCreationContext = NcCracCreationTestUtil.getNcCracCreationContext("/profiles/remedialactions/RemedialActionGroups.zip", NcCracCreationTestUtil.NETWORK);
        assertEquals(5, cracCreationContext.getCrac().getRemedialActions().size());
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "92f45b99-44c2-499d-8c4e-723bd1829dbe", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group 92f45b99-44c2-499d-8c4e-723bd1829dbe will not be imported because all depending remedial actions must have the same usage rules. All RA's depending in that group will be ignored: open_be1_be4_cra, open_be1_be4_pra");
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "95ba7539-6067-4f7e-a30b-e53eae7c042a", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group 95ba7539-6067-4f7e-a30b-e53eae7c042a will not be imported because all depending remedial actions must have the same usage rules. All RA's depending in that group will be ignored: open_be1_be4_cra_ae1, open_be1_be4_pra");
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "a8c92deb-6a3a-49a2-aecd-fb3bccbd005c", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group a8c92deb-6a3a-49a2-aecd-fb3bccbd005c will not be imported because all depending remedial actions must have the same usage rules. All RA's depending in that group will be ignored: open_be1_be4_cra, open_be1_be4_cra_co1");
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "b9dc54f8-6f8f-4878-8d7d-5d08751e5977", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group b9dc54f8-6f8f-4878-8d7d-5d08751e5977 will not be imported because all related RemedialActionDependency must be of the same kind. All RA's depending in that group will be ignored: open_be1_be4_pra, open_de3_de4_pra");
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "fc0403cc-c774-4966-b7cb-2fc75b7ebdbc", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group fc0403cc-c774-4966-b7cb-2fc75b7ebdbc will not be imported because all depending remedial actions must have the same usage rules. All RA's depending in that group will be ignored: open_be1_be4_cra_ae1, open_be1_be4_cra_ae2");

        NcCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "23ff9c5d-9501-4141-a4b3-f4468b2eb636", Set.of("DDE3AA1  DDE4AA1  1", "BBE1AA1  BBE4AA1  1"), true, 1, "RTE");
        assertEquals("Topo 1 + Topo 2 - PRA", cracCreationContext.getCrac().getRemedialAction("23ff9c5d-9501-4141-a4b3-f4468b2eb636").getName());
        UsageRule ur0 = cracCreationContext.getCrac().getNetworkAction("23ff9c5d-9501-4141-a4b3-f4468b2eb636").getUsageRules().iterator().next();
        assertTrue(ur0 instanceof OnInstant);
        assertEquals(InstantKind.PREVENTIVE, ur0.getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ur0.getUsageMethod());

        NcCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "5569e363-59ab-497a-99b0-c4ae239cbe73", Set.of("DDE3AA1  DDE4AA1  1", "BBE1AA1  BBE4AA1  1"), true, 3, "RTE");
        assertEquals("Topo 1 + Topo 2 - CRA", cracCreationContext.getCrac().getRemedialAction("5569e363-59ab-497a-99b0-c4ae239cbe73").getName());
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "5569e363-59ab-497a-99b0-c4ae239cbe73", NcCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "5569e363-59ab-497a-99b0-c4ae239cbe73", NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "5569e363-59ab-497a-99b0-c4ae239cbe73", NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);

        NcCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "f4e1e04c-0184-42ea-a3cb-75dfe70112f5", Set.of("DDE3AA1  DDE4AA1  1", "BBE1AA1  BBE4AA1  1"), true, 3, "RTE");
        assertEquals("Topo 1 + Topo 2 - CRA x CO1", cracCreationContext.getCrac().getRemedialAction("f4e1e04c-0184-42ea-a3cb-75dfe70112f5").getName());
        NcCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "f4e1e04c-0184-42ea-a3cb-75dfe70112f5", "co1_fr2_fr3_1", NcCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        NcCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "f4e1e04c-0184-42ea-a3cb-75dfe70112f5", "co1_fr2_fr3_1", NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        NcCracCreationTestUtil.assertHasOnContingencyStateUsageRule(cracCreationContext, "f4e1e04c-0184-42ea-a3cb-75dfe70112f5", "co1_fr2_fr3_1", NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);

        NcCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "7d2833e4-c5a8-4d79-b936-c735a58f1774", Set.of("DDE3AA1  DDE4AA1  1", "BBE1AA1  BBE4AA1  1"), true, 6, "RTE");
        assertEquals("Topo 1 + Topo 2 - CRA x AE1", cracCreationContext.getCrac().getRemedialAction("7d2833e4-c5a8-4d79-b936-c735a58f1774").getName());
        NcCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "7d2833e4-c5a8-4d79-b936-c735a58f1774", "RTE_AE1 (f7708112-b880-4674-98a1-b005a01a61d5) - RTE_CO1 - curative 1", cracCreationContext.getCrac().getInstant(NcCracCreationTestUtil.CURATIVE_1_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        NcCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "7d2833e4-c5a8-4d79-b936-c735a58f1774", "RTE_AE1 (f7708112-b880-4674-98a1-b005a01a61d5) - RTE_CO1 - curative 2", cracCreationContext.getCrac().getInstant(NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        NcCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "7d2833e4-c5a8-4d79-b936-c735a58f1774", "RTE_AE1 (f7708112-b880-4674-98a1-b005a01a61d5) - RTE_CO1 - curative 2", cracCreationContext.getCrac().getInstant(NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        NcCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "7d2833e4-c5a8-4d79-b936-c735a58f1774", "RTE_AE1 (f7708112-b880-4674-98a1-b005a01a61d5) - RTE_CO1 - curative 3", cracCreationContext.getCrac().getInstant(NcCracCreationTestUtil.CURATIVE_1_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        NcCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "7d2833e4-c5a8-4d79-b936-c735a58f1774", "RTE_AE1 (f7708112-b880-4674-98a1-b005a01a61d5) - RTE_CO1 - curative 3", cracCreationContext.getCrac().getInstant(NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        NcCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "7d2833e4-c5a8-4d79-b936-c735a58f1774", "RTE_AE1 (f7708112-b880-4674-98a1-b005a01a61d5) - RTE_CO1 - curative 3", cracCreationContext.getCrac().getInstant(NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);

        NcCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "66979f64-3c52-486c-84f7-b5439cd71765", Set.of("BBE1AA1  BBE4AA1  1"), true, 1, "RTE");
        assertEquals("Topo 1 - PRA", cracCreationContext.getCrac().getRemedialAction("66979f64-3c52-486c-84f7-b5439cd71765").getName());
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "66979f64-3c52-486c-84f7-b5439cd71765", NcCracCreationTestUtil.PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);
    }

    @Test
    void importGroupedHvdcRemedialActions() {
        NcCracCreationContext cracCreationContext = NcCracCreationTestUtil.getNcCracCreationContext("/profiles/remedialactions/RemedialActionGroups_HVDC.zip", NcCracCreationTestUtil.NETWORK);
        NcCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "hdvc-200-be-de", Set.of("BBE1AA1 _generator", "DDE2AA1 _generator", "BBE1AA1  BBE4AA1  1", "DDE1AA1 _generator", "BBE2AA1 _generator", "DDE3AA1  DDE4AA1  1"), true, 3, "RTE");
        NetworkAction networkAction1 = cracCreationContext.getCrac().getNetworkAction("hdvc-200-be-de");
        assertEquals("HDVC Action - 200 MW BE to DE", networkAction1.getName());
        assertEquals(6, networkAction1.getElementaryActions().size());
        assertTrue(hasSwitchAction(networkAction1.getElementaryActions(), "BBE1AA1  BBE4AA1  1", ActionType.OPEN));
        assertTrue(hasSwitchAction(networkAction1.getElementaryActions(), "DDE3AA1  DDE4AA1  1", ActionType.OPEN));
        assertTrue(hasGeneratorAction(networkAction1.getElementaryActions(), "BBE1AA1 _generator", -200));
        assertTrue(hasGeneratorAction(networkAction1.getElementaryActions(), "BBE2AA1 _generator", -200));
        assertTrue(hasGeneratorAction(networkAction1.getElementaryActions(), "DDE1AA1 _generator", 200));
        assertTrue(hasGeneratorAction(networkAction1.getElementaryActions(), "DDE2AA1 _generator", 200));
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "hdvc-200-be-de", NcCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "hdvc-200-be-de", NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "hdvc-200-be-de", NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);

        NcCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "hdvc-200-de-be", Set.of("BBE1AA1 _generator", "DDE2AA1 _generator", "BBE1AA1  BBE4AA1  1", "DDE1AA1 _generator", "BBE2AA1 _generator", "DDE3AA1  DDE4AA1  1"), true, 3, "RTE");
        NetworkAction networkAction2 = cracCreationContext.getCrac().getNetworkAction("hdvc-200-de-be");
        assertEquals("HDVC Action - 200 MW DE to BE", networkAction2.getName());
        assertEquals(6, networkAction2.getElementaryActions().size());
        assertTrue(hasSwitchAction(networkAction2.getElementaryActions(), "BBE1AA1  BBE4AA1  1", ActionType.OPEN));
        assertTrue(hasSwitchAction(networkAction2.getElementaryActions(), "DDE3AA1  DDE4AA1  1", ActionType.OPEN));
        assertTrue(hasGeneratorAction(networkAction2.getElementaryActions(), "BBE1AA1 _generator", 200));
        assertTrue(hasGeneratorAction(networkAction2.getElementaryActions(), "BBE2AA1 _generator", 200));
        assertTrue(hasGeneratorAction(networkAction2.getElementaryActions(), "DDE1AA1 _generator", -200));
        assertTrue(hasGeneratorAction(networkAction2.getElementaryActions(), "DDE2AA1 _generator", -200));
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "hdvc-200-de-be", NcCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "hdvc-200-de-be", NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "hdvc-200-de-be", NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);

        NcCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "hdvc-0", Set.of("BBE1AA1 _generator", "DDE2AA1 _generator", "BBE1AA1  BBE4AA1  1", "DDE1AA1 _generator", "BBE2AA1 _generator", "DDE3AA1  DDE4AA1  1"), true, 3, "RTE");
        NetworkAction networkAction3 = cracCreationContext.getCrac().getNetworkAction("hdvc-0");
        assertEquals("HDVC Action - 0 MW", networkAction3.getName());
        assertEquals(6, networkAction3.getElementaryActions().size());
        assertTrue(hasSwitchAction(networkAction3.getElementaryActions(), "BBE1AA1  BBE4AA1  1", ActionType.OPEN));
        assertTrue(hasSwitchAction(networkAction3.getElementaryActions(), "DDE3AA1  DDE4AA1  1", ActionType.OPEN));
        assertTrue(hasGeneratorAction(networkAction3.getElementaryActions(), "BBE1AA1 _generator", 0));
        assertTrue(hasGeneratorAction(networkAction3.getElementaryActions(), "BBE2AA1 _generator", 0));
        assertTrue(hasGeneratorAction(networkAction3.getElementaryActions(), "DDE1AA1 _generator", 0));
        assertTrue(hasGeneratorAction(networkAction3.getElementaryActions(), "DDE2AA1 _generator", 0));
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "hdvc-0", NcCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "hdvc-0", NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "hdvc-0", NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);
    }

    private boolean hasGeneratorAction(Set<Action> elementaryActions, String elementId, double activePowerValue) {
        return elementaryActions.stream()
            .filter(GeneratorAction.class::isInstance)
            .map(GeneratorAction.class::cast)
            .anyMatch(action -> action.getGeneratorId().equals(elementId) && action.getActivePowerValue().getAsDouble() == activePowerValue);
    }

    private boolean hasSwitchAction(Set<Action> elementaryActions, String elementId, ActionType actionType) {
        return elementaryActions.stream()
            .filter(SwitchAction.class::isInstance)
            .map(SwitchAction.class::cast)
            .anyMatch(action -> action.getSwitchId().equals(elementId) && action.isOpen() == (actionType == ActionType.OPEN));
    }

    @Test
    void testImportRemedialActionGroupFromInvalidTopologyAction() {
        NcCracCreationContext cracCreationContext = NcCracCreationTestUtil.getNcCracCreationContext("/profiles/remedialactions/RemedialActionGroupFromInvalidTopologyAction.zip", NcCracCreationTestUtil.NETWORK);
        assertTrue(cracCreationContext.getCrac().getNetworkActions().isEmpty());
    }

}
