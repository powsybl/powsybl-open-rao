/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.usagerule.OnContingencyState;
import com.powsybl.openrao.data.cracapi.usagerule.OnFlowConstraint;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracapi.usagerule.UsageRule;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupRemedialActionTest {

    @Test
    void importGroupedRemedialActions() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_80.zip");
        assertEquals(7, cracCreationContext.getCrac().getRemedialActions().size()); // TODO check with thomas, 5 in US, but 2 standalone ra's still not grouped
        assertRaNotImported(cracCreationContext, "92f45b99-44c2-499d-8c4e-723bd1829dbe", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group 92f45b99-44c2-499d-8c4e-723bd1829dbe will not be imported because all depending the remedial actions must have the same usage rules");
        assertRaNotImported(cracCreationContext, "95ba7539-6067-4f7e-a30b-e53eae7c042a", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group 95ba7539-6067-4f7e-a30b-e53eae7c042a will not be imported because all depending the remedial actions must have the same usage rules");
        assertRaNotImported(cracCreationContext, "a8c92deb-6a3a-49a2-aecd-fb3bccbd005c", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group a8c92deb-6a3a-49a2-aecd-fb3bccbd005c will not be imported because all depending the remedial actions must have the same usage rules");
        assertRaNotImported(cracCreationContext, "b9dc54f8-6f8f-4878-8d7d-5d08751e5977", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group b9dc54f8-6f8f-4878-8d7d-5d08751e5977 will not be imported because all related RemedialActionDependency must be of the same kind");
        assertRaNotImported(cracCreationContext, "fc0403cc-c774-4966-b7cb-2fc75b7ebdbc", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group fc0403cc-c774-4966-b7cb-2fc75b7ebdbc will not be imported because all depending the remedial actions must have the same usage rules");

        assertNetworkActionImported(cracCreationContext, "23ff9c5d-9501-4141-a4b3-f4468b2eb636", Set.of("FFR1AA1X-FFR1AA1--1", "FFR1AA1Y-FFR1AA1--1"), true, 1);
        assertEquals("Topo 1 + Topo 2 - PRA", cracCreationContext.getCrac().getRemedialAction("23ff9c5d-9501-4141-a4b3-f4468b2eb636").getName());
        UsageRule ur0 = cracCreationContext.getCrac().getNetworkAction("23ff9c5d-9501-4141-a4b3-f4468b2eb636").getUsageRules().iterator().next();
        assertEquals(InstantKind.PREVENTIVE, ur0.getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ur0.getUsageMethod());

        assertNetworkActionImported(cracCreationContext, "5569e363-59ab-497a-99b0-c4ae239cbe73", Set.of("FFR1AA1X-FFR1AA1--1", "FFR1AA1Y-FFR1AA1--1"), true, 1);
        assertEquals("Topo 1 + Topo 2 - CRA", cracCreationContext.getCrac().getRemedialAction("5569e363-59ab-497a-99b0-c4ae239cbe73").getName());
        UsageRule ur1 = cracCreationContext.getCrac().getNetworkAction("5569e363-59ab-497a-99b0-c4ae239cbe73").getUsageRules().iterator().next();
        assertEquals(InstantKind.CURATIVE, ur1.getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ur1.getUsageMethod());

        assertNetworkActionImported(cracCreationContext, "f4e1e04c-0184-42ea-a3cb-75dfe70112f5", Set.of("FFR1AA1X-FFR1AA1--1", "FFR1AA1Y-FFR1AA1--1"), true, 1);
        assertEquals("Topo 1 + Topo 2 - CRA x CO1", cracCreationContext.getCrac().getRemedialAction("f4e1e04c-0184-42ea-a3cb-75dfe70112f5").getName());
        UsageRule ur2 = cracCreationContext.getCrac().getNetworkAction("f4e1e04c-0184-42ea-a3cb-75dfe70112f5").getUsageRules().iterator().next();
        assertEquals(InstantKind.CURATIVE, ur2.getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ur2.getUsageMethod());
        assertEquals("co1_fr2_fr3_1", ((OnContingencyState) ur2).getContingency().getId());

        assertNetworkActionImported(cracCreationContext, "7d2833e4-c5a8-4d79-b936-c735a58f1774", Set.of("FFR1AA1X-FFR1AA1--1", "FFR1AA1Y-FFR1AA1--1"), true, 1);
        assertEquals("Topo 1 + Topo 2 - CRA x AE1", cracCreationContext.getCrac().getRemedialAction("7d2833e4-c5a8-4d79-b936-c735a58f1774").getName());
        UsageRule ur3 = cracCreationContext.getCrac().getNetworkAction("7d2833e4-c5a8-4d79-b936-c735a58f1774").getUsageRules().iterator().next();
        assertEquals(InstantKind.CURATIVE, ur3.getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ur3.getUsageMethod());
        assertEquals("RTE_AE1 (f7708112-b880-4674-98a1-b005a01a61d5) - RTE_CO1 - curative", ((OnFlowConstraint) ur3).getFlowCnec().getId());

        assertNetworkActionImported(cracCreationContext, "66979f64-3c52-486c-84f7-b5439cd71765", Set.of("FFR1AA1X-FFR1AA1--1", "FFR1AA1Y-FFR1AA1--1"), true, 1);
        assertEquals("Topo 1 - PRA", cracCreationContext.getCrac().getRemedialAction("66979f64-3c52-486c-84f7-b5439cd71765").getName());
        UsageRule ur4 = cracCreationContext.getCrac().getNetworkAction("66979f64-3c52-486c-84f7-b5439cd71765").getUsageRules().iterator().next();
        assertEquals(InstantKind.PREVENTIVE, ur4.getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ur4.getUsageMethod());
    }

    @Test
    void importGroupedHvdcRemedialActions() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/RemedialActionGroups_HVDC.zip");
        assertNetworkActionImported(cracCreationContext, "hdvc-200-be-de", Set.of("BBE1AA1 _generator", "DDE2AA1 _generator", "BBE1AA1  BBE4AA1  1", "DDE1AA1 _generator", "BBE2AA1 _generator", "DDE3AA1  DDE4AA1  1"), true, 1);
        assertEquals("HDVC Action - 200 MW BE to DE", cracCreationContext.getCrac().getRemedialAction("hdvc-200-be-de").getName());
        UsageRule ur0 = cracCreationContext.getCrac().getNetworkAction("hdvc-200-be-de").getUsageRules().iterator().next();
        assertEquals(InstantKind.CURATIVE, ur0.getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ur0.getUsageMethod());

        assertNetworkActionImported(cracCreationContext, "hdvc-200-de-be", Set.of("BBE1AA1 _generator", "DDE2AA1 _generator", "BBE1AA1  BBE4AA1  1", "DDE1AA1 _generator", "BBE2AA1 _generator", "DDE3AA1  DDE4AA1  1"), true, 1);
        assertEquals("HDVC Action - 200 MW DE to BE", cracCreationContext.getCrac().getRemedialAction("hdvc-200-de-be").getName());
        UsageRule ur1 = cracCreationContext.getCrac().getNetworkAction("hdvc-200-be-de").getUsageRules().iterator().next();
        assertEquals(InstantKind.CURATIVE, ur1.getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ur1.getUsageMethod());

        assertNetworkActionImported(cracCreationContext, "hdvc-0", Set.of("BBE1AA1 _generator", "DDE2AA1 _generator", "BBE1AA1  BBE4AA1  1", "DDE1AA1 _generator", "BBE2AA1 _generator", "DDE3AA1  DDE4AA1  1"), true, 1);
        assertEquals("HDVC Action - 0 MW", cracCreationContext.getCrac().getRemedialAction("hdvc-0").getName());
        UsageRule ur2 = cracCreationContext.getCrac().getNetworkAction("hdvc-0").getUsageRules().iterator().next();
        assertEquals(InstantKind.CURATIVE, ur2.getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ur2.getUsageMethod());
    }

}
