/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyState;
import com.powsybl.openrao.data.crac.api.usagerule.OnInstant;
import com.powsybl.openrao.data.crac.io.nc.parameters.NcCracCreationParameters;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class ReliCapGridDatasetTest {
    private static final String NETWORK_PATH = "/networks/ReliCapGrid.zip";
    private static final String NC_PATH = "/profiles/ReliCapGrid_CGMES_NC.zip";
    private static final OffsetDateTime TIMESTAMP = OffsetDateTime.parse("2022-06-15T22:30:00Z");

    private final CracCreationParameters cracCreationParameters = new CracCreationParameters();
    private final NcCracCreationParameters ncCracCreationParameters = new NcCracCreationParameters();

    {
        ncCracCreationParameters.setCreateAutoInstant(false);
        ncCracCreationParameters.setCurativeInstants(Map.of("curative", 300));
        ncCracCreationParameters.setCapacityCalculationRegion(null);
        cracCreationParameters.addExtension(NcCracCreationParameters.class, ncCracCreationParameters);
        cracCreationParameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES);
    }

    // TODO: add test to check network content

    @Test
    void testImportReliCapGridDatasetTest() {
        Network network = NcCracCreationTestUtil.getNetworkFromResource(NETWORK_PATH);
        NcCracCreationContext cracCreationContext = NcCracCreationTestUtil.getNcCracCreationContext(NC_PATH, network, TIMESTAMP, cracCreationParameters);
        Crac crac = cracCreationContext.getCrac();
        assertNotNull(crac);

        // check base CRAC data
        List<Instant> instants = crac.getSortedInstants();
        assertEquals(3, instants.size());
        assertInstant(instants.getFirst(), InstantKind.PREVENTIVE, "preventive");
        assertInstant(instants.get(1), InstantKind.OUTAGE, "outage");
        assertInstant(instants.getLast(), InstantKind.CURATIVE, "curative");

        assertEquals(16, crac.getContingencies().size()); // as expected
        assertEquals(12, crac.getPstRangeActions().size()); // as expected
        // TODO: implement and check -> assertEquals(32, crac.getInjectionRangeActions().size());
        // TODO: allow curative CT + FIXME why 0? -> assertEquals(8, crac.getCounterTradeRangeActions().size());

        // check PST range actions
        List<PstRangeAction> pstRangeActions = crac.getPstRangeActions().stream().sorted(Comparator.comparing(PstRangeAction::getId)).toList();
        assertEquals(12, pstRangeActions.size());

        PstRangeAction pstRangeAction = pstRangeActions.getFirst();
        assertPstRangeAction(pstRangeAction, "424db5b8-dd9f-500b-83ef-e365b2a6cbfc", "CRA_PST_Aac", "2c9a0753-421a-489b-9761-9452f60b3157", 0, 300, 5);
        assertHasOnContingencyStateUsageRule(pstRangeAction, "curative", "9d17b84c-33b5-4a68-b8b9-ed5b31038d40");
        assertHasOnContingencyStateUsageRule(pstRangeAction, "curative", "13334fdf-9cc2-4341-adb6-1281269040b4");
        assertHasOnContingencyStateUsageRule(pstRangeAction, "curative", "62eac668-82a1-4739-8cfc-de929b78ef7e");
        assertHasOnContingencyStateUsageRule(pstRangeAction, "curative", "b6b780cb-9fe5-4c45-989d-447a927c3874");
        assertHasOnContingencyStateUsageRule(pstRangeAction, "curative", "ce19dd34-429e-4b72-8813-7615cc57b4a4");

        pstRangeAction = pstRangeActions.get(1);
        assertPstRangeAction(pstRangeAction, "57100086-3478-5721-bae8-2ca3aa730868", "PRA_PST_Aaa1", "703f5b86-b6c6-4c47-8f6b-55d7d93838c9", 6, null, 1);
        assetHasOnInstantUsageRule(pstRangeAction, "preventive");

        pstRangeAction = pstRangeActions.get(2);
        assertPstRangeAction(pstRangeAction, "64817939-33a6-5ce0-a754-8f84768d72b3", "PRA_PST_Aac", "2c9a0753-421a-489b-9761-9452f60b3157", 0, null, 1);
        assetHasOnInstantUsageRule(pstRangeAction, "preventive");

        pstRangeAction = pstRangeActions.get(3);
        assertPstRangeAction(pstRangeAction, "6fb7ac93-287a-56aa-93f1-a6c22a1c19f9", "PRA_PST_GA_TR2_1", "e8a7eaec-51d6-4571-b3d9-c36d52073c33", 3, null, 1);
        assetHasOnInstantUsageRule(pstRangeAction, "preventive");

        pstRangeAction = pstRangeActions.get(4);
        assertPstRangeAction(pstRangeAction, "8cb4ebb9-7ede-5bb6-b45c-47445b87fb68", "CRA_PST_BO_TR2_2", "b94318f6-6d24-4f56-96b9-df2531ad6543", 10, 300, 1);
        assertHasOnContingencyStateUsageRule(pstRangeAction, "curative", "37997e71-cb7d-4a8c-baa6-2a1594956da9");

        pstRangeAction = pstRangeActions.get(5);
        assertPstRangeAction(pstRangeAction, "a3169960-996a-5838-8372-307036b7cf0f", "CRA_PST_BO_TR2_1", "a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", 10, 300, 1);
        assertHasOnContingencyStateUsageRule(pstRangeAction, "curative", "37997e71-cb7d-4a8c-baa6-2a1594956da9");

        pstRangeAction = pstRangeActions.get(6);
        assertPstRangeAction(pstRangeAction, "c4029f66-0349-5b2f-afbd-3c6219a0f4fd", "CRA_PST_GA_TR2_1", "e8a7eaec-51d6-4571-b3d9-c36d52073c33", 3, 300, 1);
        assertHasOnContingencyStateUsageRule(pstRangeAction, "curative", "bd7bb012-f7b9-45e0-9e15-4e2aa3592829");

        pstRangeAction = pstRangeActions.get(7);
        assertPstRangeAction(pstRangeAction, "ca0064b9-f037-58e5-96e2-4e3d632b3f22", "PRA_PST_BO_TR2_1", "a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", 10, null, 1);
        assetHasOnInstantUsageRule(pstRangeAction, "preventive");

        pstRangeAction = pstRangeActions.get(8);
        assertPstRangeAction(pstRangeAction, "e7438a6e-89f6-51a6-88f6-f44b1cdd5490", "PRA_PST_BO_TR2_2", "b94318f6-6d24-4f56-96b9-df2531ad6543", 10, null, 1);
        assetHasOnInstantUsageRule(pstRangeAction, "preventive");

        pstRangeAction = pstRangeActions.get(9);
        assertPstRangeAction(pstRangeAction, "ea8a628a-97c6-5d5a-acfc-707d6a653e25", "CRA_PST_GA_TR2_2", "2184f365-8cd5-4b5d-8a28-9d68603bb6a4", 17, 300, 1);
        assertHasOnContingencyStateUsageRule(pstRangeAction, "curative", "bd7bb012-f7b9-45e0-9e15-4e2aa3592829");

        pstRangeAction = pstRangeActions.get(10);
        assertPstRangeAction(pstRangeAction, "f313478c-6b71-595c-8e52-16c5892d540d", "PRA_PST_GA_TR2_2", "2184f365-8cd5-4b5d-8a28-9d68603bb6a4", 17, null, 1);
        assetHasOnInstantUsageRule(pstRangeAction, "preventive");

        pstRangeAction = pstRangeActions.get(11);
        assertPstRangeAction(pstRangeAction, "f7693938-7a8a-5e31-a8d7-64c45487dde9", "CRA_PST_Aaa1", "703f5b86-b6c6-4c47-8f6b-55d7d93838c9", 6, 300, 5);
        assertHasOnContingencyStateUsageRule(pstRangeAction, "curative", "9d17b84c-33b5-4a68-b8b9-ed5b31038d40");
        assertHasOnContingencyStateUsageRule(pstRangeAction, "curative", "13334fdf-9cc2-4341-adb6-1281269040b4");
        assertHasOnContingencyStateUsageRule(pstRangeAction, "curative", "62eac668-82a1-4739-8cfc-de929b78ef7e");
        assertHasOnContingencyStateUsageRule(pstRangeAction, "curative", "b6b780cb-9fe5-4c45-989d-447a927c3874");
        assertHasOnContingencyStateUsageRule(pstRangeAction, "curative", "ce19dd34-429e-4b72-8813-7615cc57b4a4");

        List<InjectionRangeAction> equivalentHvdcRangeActions = crac.getInjectionRangeActions().stream().sorted(Comparator.comparing(InjectionRangeAction::getId)).toList();
        assertEquals(2, equivalentHvdcRangeActions.size());
    }

    private static void assertInstant(Instant instant, InstantKind expectedInstantKind, String expectedId) {
        assertEquals(expectedInstantKind, instant.getKind());
        assertEquals(expectedId, instant.getId());
    }

    private static void assertPstRangeAction(PstRangeAction pstRangeAction,
                                             String expectedId,
                                             String expectedName,
                                             String expectedNetworkElement,
                                             int expectedInitialTap,
                                             Integer expectedSpeed,
                                             int expectedNumberOfUsageRules) {
        assertEquals(expectedId, pstRangeAction.getId());
        assertEquals(expectedName, pstRangeAction.getName());
        assertEquals(expectedNetworkElement, pstRangeAction.getNetworkElementId());
        assertEquals(expectedInitialTap, pstRangeAction.getInitialTap());
        if (expectedSpeed == null) {
            assertTrue(pstRangeAction.getSpeed().isEmpty());
        } else {
            assertTrue(pstRangeAction.getSpeed().isPresent());
            assertEquals(expectedSpeed, pstRangeAction.getSpeed().get());
        }
        assertEquals(expectedNumberOfUsageRules, pstRangeAction.getUsageRules().size());
    }

    private static void assetHasOnInstantUsageRule(RemedialAction<?> remedialAction, String instant) {
        remedialAction.getUsageRules()
            .stream()
            .filter(OnInstant.class::isInstance)
            .map(OnInstant.class::cast)
            .filter(usageRule -> usageRule.getInstant().getId().equals(instant))
            .findAny()
            .orElseThrow(() -> new AssertionError("No OnInstant usage rule found for instant " + instant));
    }

    private static void assertHasOnContingencyStateUsageRule(RemedialAction<?> remedialAction, String instantId, String contingencyId) {
        remedialAction.getUsageRules()
            .stream()
            .filter(OnContingencyState.class::isInstance)
            .map(OnContingencyState.class::cast)
            .filter(usageRule -> usageRule.getInstant().getId().equals(instantId))
            .filter(usageRule -> usageRule.getContingency().getId().equals(contingencyId))
            .findAny()
            .orElseThrow(() -> new AssertionError("No OnContingencyState usage rule found for contingency " + contingencyId));
    }

}
