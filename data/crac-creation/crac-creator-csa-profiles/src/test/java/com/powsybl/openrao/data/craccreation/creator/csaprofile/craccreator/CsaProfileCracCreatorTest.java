/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.RaUsageLimits;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.CracCreationParameters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertRaNotImported;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class CsaProfileCracCreatorTest {

    @Test
    void testCustomImportCase() {
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        RaUsageLimits raUsageLimits = new RaUsageLimits();
        raUsageLimits.setMaxRa(33);
        cracCreationParameters.addRaUsageLimitsForInstant("preventive", raUsageLimits);
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/TestCase_13_5_4.zip", cracCreationParameters);
        Crac importedCrac = cracCreationContext.getCrac();
        Instant preventiveInstant = importedCrac.getInstant("preventive");
        Instant outageInstant = importedCrac.getInstant("outage");
        Instant curativeInstant = importedCrac.getInstant("curative");

        assertTrue(cracCreationContext.isCreationSuccessful());

        // Check ra usage limits
        assertEquals(33, importedCrac.getRaUsageLimits(preventiveInstant).getMaxRa());

        // Check contingencies
        assertEquals(1, importedCrac.getContingencies().size());
        CsaProfileCracCreationTestUtil.assertContingencyEquality(importedCrac.getContingencies().iterator().next(), "co1_fr2_fr3_1", "RTE_co1_fr2_fr3_1", Set.of("FFR2AA1--FFR3AA1--1"));

        // Check Flow Cnecs
        assertEquals(6, importedCrac.getFlowCnecs().size());
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(importedCrac.getFlowCnec("RTE_FFR2AA1--FFR3AA1--2 (079f1887-f33e-49ef-b1ff-22e871055fd0) - RTE_co1_fr2_fr3_1 - curative"), "RTE_FFR2AA1--FFR3AA1--2 (079f1887-f33e-49ef-b1ff-22e871055fd0) - RTE_co1_fr2_fr3_1 - curative", "RTE_FFR2AA1--FFR3AA1--2 (079f1887-f33e-49ef-b1ff-22e871055fd0) - RTE_co1_fr2_fr3_1 - curative",
            "FFR2AA1--FFR3AA1--2", curativeInstant, "co1_fr2_fr3_1", 2500., -2500., Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(importedCrac.getFlowCnec("RTE_FFR3AA1--FFR5AA1--1 (755832d8-220a-4e5a-b133-dfd27b3c8a78) - RTE_co1_fr2_fr3_1 - outage - TATL 60"), "RTE_FFR3AA1--FFR5AA1--1 (755832d8-220a-4e5a-b133-dfd27b3c8a78) - RTE_co1_fr2_fr3_1 - outage - TATL 60", "RTE_FFR3AA1--FFR5AA1--1 (755832d8-220a-4e5a-b133-dfd27b3c8a78) - RTE_co1_fr2_fr3_1 - outage - TATL 60",
            "FFR3AA1--FFR5AA1--1", outageInstant, "co1_fr2_fr3_1", 1500., -1500., Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(importedCrac.getFlowCnec("RTE_FFR2AA1--DDE3AA1--1 (77320d6c-7880-43b1-ac28-e27a85ebda82) - preventive"), "RTE_FFR2AA1--DDE3AA1--1 (77320d6c-7880-43b1-ac28-e27a85ebda82) - preventive", "RTE_FFR2AA1--DDE3AA1--1 (77320d6c-7880-43b1-ac28-e27a85ebda82) - preventive",
            "FFR2AA1--DDE3AA1--1", preventiveInstant, null, 1000., -1000., Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(importedCrac.getFlowCnec("RTE_FFR3AA1--FFR5AA1--1 (ec2ba2f9-f230-40f9-b665-71c892a80874) - RTE_co1_fr2_fr3_1 - curative"), "RTE_FFR3AA1--FFR5AA1--1 (ec2ba2f9-f230-40f9-b665-71c892a80874) - RTE_co1_fr2_fr3_1 - curative", "RTE_FFR3AA1--FFR5AA1--1 (ec2ba2f9-f230-40f9-b665-71c892a80874) - RTE_co1_fr2_fr3_1 - curative",
            "FFR3AA1--FFR5AA1--1", curativeInstant, "co1_fr2_fr3_1", 1000., -1000., Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(importedCrac.getFlowCnec("TENNET_TSO_NNL2AA1--BBE3AA1--1 (f0208d08-2ed5-4d92-91a1-4e89ac71e17e) - preventive"), "TENNET_TSO_NNL2AA1--BBE3AA1--1 (f0208d08-2ed5-4d92-91a1-4e89ac71e17e) - preventive", "TENNET_TSO_NNL2AA1--BBE3AA1--1 (f0208d08-2ed5-4d92-91a1-4e89ac71e17e) - preventive",
            "NNL2AA1--BBE3AA1--1", preventiveInstant, null, 5000., -5000., Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(importedCrac.getFlowCnec("RTE_FFR2AA1--DDE3AA1--1 (f7708112-b880-4674-98a1-b005a01a61d5) - RTE_co1_fr2_fr3_1 - outage - TATL 60"), "RTE_FFR2AA1--DDE3AA1--1 (f7708112-b880-4674-98a1-b005a01a61d5) - RTE_co1_fr2_fr3_1 - outage - TATL 60", "RTE_FFR2AA1--DDE3AA1--1 (f7708112-b880-4674-98a1-b005a01a61d5) - RTE_co1_fr2_fr3_1 - outage - TATL 60",
            "FFR2AA1--DDE3AA1--1", outageInstant, "co1_fr2_fr3_1", 1200., -1200., Side.RIGHT);

        // Check PST RAs
        CsaProfileCracCreationTestUtil.assertPstRangeActionImported(cracCreationContext, "pst_be", "BBE2AA1--BBE3AA1--1", false, 1);
        CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "pst_be", curativeInstant, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertPstRangeActionImported(cracCreationContext, "pst_fr_cra", "FFR2AA1--FFR4AA1--1", false, 1);
        CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "pst_fr_cra", curativeInstant, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertPstRangeActionImported(cracCreationContext, "pst_fr_pra", "FFR2AA1--FFR4AA1--1", false, 1);
        CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "pst_fr_pra", preventiveInstant, UsageMethod.AVAILABLE);

        // Check topo RAs
        CsaProfileCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "close_fr1_fr5", Set.of("FFR1AA1Z-FFR1AA1--1"), false, 1);
        CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "close_fr1_fr5", curativeInstant, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "open_fr1_fr2", Set.of("FFR1AA1Y-FFR1AA1--1"), false, 1);
        CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "open_fr1_fr2", preventiveInstant, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "open_fr1_fr3", Set.of("FFR1AA1X-FFR1AA1--1"), false, 1);
        CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "open_fr1_fr3", preventiveInstant, UsageMethod.AVAILABLE);
    }

    @Test
    void checkExcludedCombinationConstraintHandling() {
        //CSA 63_1
        Network network = Mockito.mock(Network.class);
        BusbarSection terminal1Mock = Mockito.mock(BusbarSection.class);
        BusbarSection terminal2Mock = Mockito.mock(BusbarSection.class);
        BusbarSection terminal3Mock = Mockito.mock(BusbarSection.class);
        BusbarSection terminal4Mock = Mockito.mock(BusbarSection.class);
        Switch switchMock = Mockito.mock(Switch.class);
        Branch networkElementMock = Mockito.mock(Branch.class);

        when(terminal1Mock.getId()).thenReturn("c2e3e624-8389-4a50-a2f8-be51631ba221");
        when(terminal2Mock.getId()).thenReturn("073a3aef-f425-48ec-ba72-d68ad4dd333c");
        when(terminal3Mock.getId()).thenReturn("9b476375-4005-46b5-8465-0192dfdbba51");
        when(terminal4Mock.getId()).thenReturn("175aa88b-c62a-4411-b4df-474c436692d0");

        when(terminal1Mock.getType()).thenReturn(IdentifiableType.BUS);
        when(terminal2Mock.getType()).thenReturn(IdentifiableType.BUS);
        when(terminal3Mock.getType()).thenReturn(IdentifiableType.BUS);
        when(terminal4Mock.getType()).thenReturn(IdentifiableType.BUS);
        when(switchMock.getId()).thenReturn("d1db384f-3a27-434b-93f5-5afa3ab23b00");
        when(networkElementMock.getId()).thenReturn("ff3c8013-d3f9-4198-a1f2-98d3ebdf30c4");
        when(switchMock.isOpen()).thenReturn(false);
        when(network.getIdentifiable("c2e3e624-8389-4a50-a2f8-be51631ba221")).thenReturn((Identifiable) terminal1Mock);
        when(network.getIdentifiable("073a3aef-f425-48ec-ba72-d68ad4dd333c")).thenReturn((Identifiable) terminal2Mock);
        when(network.getIdentifiable("9b476375-4005-46b5-8465-0192dfdbba51")).thenReturn((Identifiable) terminal1Mock);
        when(network.getIdentifiable("175aa88b-c62a-4411-b4df-474c436692d0")).thenReturn((Identifiable) terminal2Mock);
        when(network.getSwitch("d1db384f-3a27-434b-93f5-5afa3ab23b00")).thenReturn(switchMock);
        when(network.getIdentifiable("ff3c8013-d3f9-4198-a1f2-98d3ebdf30c4")).thenReturn(networkElementMock);

        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_63_1_ValidationTest.zip", network);
        Instant curativeInstant = cracCreationContext.getCrac().getInstant("curative");

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(2, cracCreationContext.getCrac().getAngleCnecs().size());
        List<AngleCnec> angleCnecs = cracCreationContext.getCrac().getAngleCnecs().stream()
            .sorted(Comparator.comparing(AngleCnec::getId)).toList();

        // RTE_AE1 - RTE_CO1 - curative
        AngleCnec angleCnec1 = angleCnecs.get(0);
        assertEquals("RTE_AE1 (ea2dc9a8-31b1-4791-8016-bd53c2620d55) - RTE_CO1 - curative", angleCnec1.getId());
        assertEquals("RTE_AE1 (ea2dc9a8-31b1-4791-8016-bd53c2620d55) - RTE_CO1 - curative", angleCnec1.getName());
        assertEquals("c2e3e624-8389-4a50-a2f8-be51631ba221", angleCnec1.getImportingNetworkElement().getId());
        assertEquals("073a3aef-f425-48ec-ba72-d68ad4dd333c", angleCnec1.getExportingNetworkElement().getId());
        assertEquals(-60.0, angleCnec1.getLowerBound(Unit.DEGREE).get());
        assertEquals(60.0, angleCnec1.getUpperBound(Unit.DEGREE).get());

        // RTE_AE2 - RTE_CO3 - curative
        AngleCnec angleCnec2 = angleCnecs.get(1);
        assertEquals("RTE_AE2 (ebe2f6f6-ea0a-4361-8740-d4efd68d5fe5) - RTE_CO3 - curative", angleCnec2.getId());
        assertEquals("RTE_AE2 (ebe2f6f6-ea0a-4361-8740-d4efd68d5fe5) - RTE_CO3 - curative", angleCnec2.getName());
        assertEquals("c2e3e624-8389-4a50-a2f8-be51631ba221", angleCnec2.getImportingNetworkElement().getId());
        assertEquals("073a3aef-f425-48ec-ba72-d68ad4dd333c", angleCnec2.getExportingNetworkElement().getId());
        assertEquals(-45.0, angleCnec2.getLowerBound(Unit.DEGREE).get());
        assertEquals(45.0, angleCnec2.getUpperBound(Unit.DEGREE).get());

        assertEquals(3, cracCreationContext.getCrac().getContingencies().size());
        List<Contingency> listContingencies = cracCreationContext.getCrac().getContingencies()
            .stream().sorted(Comparator.comparing(Contingency::getId)).toList();

        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(0),
            "2a193923-ac8a-40e8-b0f0-50ba28826317", "RTE_CO2",
            Set.of("ff3c8013-d3f9-4198-a1f2-98d3ebdf30c4"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(1),
            "5bd378fa-700b-4b88-b73a-327100ad18bf", "RTE_CO1",
            Set.of("ff3c8013-d3f9-4198-a1f2-98d3ebdf30c4"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(2),
            "8f2cac52-ba92-4922-b8e2-2ee0414829f5", "RTE_CO3",
            Set.of("ff3c8013-d3f9-4198-a1f2-98d3ebdf30c4"));

        assertEquals(3, cracCreationContext.getCrac().getRemedialActions().size());
        CsaProfileCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "ac60186a-8ee9-4379-8ce2-48fe335b0357", Set.of("d1db384f-3a27-434b-93f5-5afa3ab23b00"), false, 2);
        CsaProfileCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "4b6f26d2-887e-4bb5-b4a0-d9dbfbca0c7d", Set.of("d1db384f-3a27-434b-93f5-5afa3ab23b00"), false, 2);
        CsaProfileCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "6dccb771-921c-4025-8079-f55590868704", Set.of("d1db384f-3a27-434b-93f5-5afa3ab23b00"), false, 1);

        CsaProfileCracCreationTestUtil.assertHasOnAngleConstraintUsageRule(cracCreationContext, "ac60186a-8ee9-4379-8ce2-48fe335b0357", "RTE_AE1 (ea2dc9a8-31b1-4791-8016-bd53c2620d55) - RTE_CO1 - curative", curativeInstant, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertHasOnAngleConstraintUsageRule(cracCreationContext, "ac60186a-8ee9-4379-8ce2-48fe335b0357", "RTE_AE2 (ebe2f6f6-ea0a-4361-8740-d4efd68d5fe5) - RTE_CO3 - curative", curativeInstant, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertHasOnAngleConstraintUsageRule(cracCreationContext, "4b6f26d2-887e-4bb5-b4a0-d9dbfbca0c7d", "RTE_AE1 (ea2dc9a8-31b1-4791-8016-bd53c2620d55) - RTE_CO1 - curative", curativeInstant, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertHasOnAngleConstraintUsageRule(cracCreationContext, "4b6f26d2-887e-4bb5-b4a0-d9dbfbca0c7d", "RTE_AE2 (ebe2f6f6-ea0a-4361-8740-d4efd68d5fe5) - RTE_CO3 - curative", curativeInstant, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertHasOnAngleConstraintUsageRule(cracCreationContext, "6dccb771-921c-4025-8079-f55590868704", "RTE_AE2 (ebe2f6f6-ea0a-4361-8740-d4efd68d5fe5) - RTE_CO3 - curative", curativeInstant, UsageMethod.AVAILABLE);

        assertRaNotImported(cracCreationContext, "fb21c59d-4268-4ba2-aa1b-ae2767799a36", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action fb21c59d-4268-4ba2-aa1b-ae2767799a36 will not be imported because of an illegal EXCLUDED ElementCombinationConstraintKind");
    }
}
