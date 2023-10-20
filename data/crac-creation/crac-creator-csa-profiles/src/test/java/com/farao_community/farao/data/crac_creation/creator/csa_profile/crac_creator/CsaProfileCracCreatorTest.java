/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.usage_rule.*;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.CsaProfileCrac;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.importer.CsaProfileCracImporter;
import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static com.farao_community.farao.data.crac_api.Instant.*;

public class CsaProfileCracCreatorTest {

    @Test
    public void testCustomImportCase() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/TestCase_13_5_4.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/TestCase_13_5_4.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());
        Crac importedCrac = cracCreationContext.getCrac();

        assertTrue(cracCreationContext.isCreationSuccessful());

        // Check contingencies
        assertEquals(1, importedCrac.getContingencies().size());
        CsaProfileCracCreationTestUtil.assertContingencyEquality(importedCrac.getContingencies().iterator().next(), "co1_fr2_fr3_1", "RTE_co1_fr2_fr3_1", 1, List.of("FFR2AA1--FFR3AA1--1"));

        // Check Flow Cnecs
        assertEquals(6, importedCrac.getFlowCnecs().size());
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(importedCrac.getFlowCnec("RTE_FFR2AA1--FFR3AA1--2 - RTE_co1_fr2_fr3_1 - curative"), "RTE_FFR2AA1--FFR3AA1--2 - RTE_co1_fr2_fr3_1 - curative", "RTE_FFR2AA1--FFR3AA1--2 - RTE_co1_fr2_fr3_1 - curative",
                "FFR2AA1--FFR3AA1--2", CURATIVE, "co1_fr2_fr3_1", 2500., -2500., Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(importedCrac.getFlowCnec("RTE_FFR3AA1--FFR5AA1--1 - RTE_co1_fr2_fr3_1 - outage"), "RTE_FFR3AA1--FFR5AA1--1 - RTE_co1_fr2_fr3_1 - outage", "RTE_FFR3AA1--FFR5AA1--1 - RTE_co1_fr2_fr3_1 - outage",
                "FFR3AA1--FFR5AA1--1", OUTAGE, "co1_fr2_fr3_1", 1500., -1500., Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(importedCrac.getFlowCnec("RTE_FFR2AA1--DDE3AA1--1 - preventive"), "RTE_FFR2AA1--DDE3AA1--1 - preventive", "RTE_FFR2AA1--DDE3AA1--1 - preventive",
                "FFR2AA1--DDE3AA1--1", PREVENTIVE, null, 1000., -1000., Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(importedCrac.getFlowCnec("RTE_FFR3AA1--FFR5AA1--1 - RTE_co1_fr2_fr3_1 - curative"), "RTE_FFR3AA1--FFR5AA1--1 - RTE_co1_fr2_fr3_1 - curative", "RTE_FFR3AA1--FFR5AA1--1 - RTE_co1_fr2_fr3_1 - curative",
                "FFR3AA1--FFR5AA1--1", CURATIVE, "co1_fr2_fr3_1", 1000., -1000., Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(importedCrac.getFlowCnec("TENNET_TSO_NNL2AA1--BBE3AA1--1 - preventive"), "TENNET_TSO_NNL2AA1--BBE3AA1--1 - preventive", "TENNET_TSO_NNL2AA1--BBE3AA1--1 - preventive",
                "NNL2AA1--BBE3AA1--1", PREVENTIVE, null, 5000., -5000., Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(importedCrac.getFlowCnec("RTE_FFR2AA1--DDE3AA1--1 - RTE_co1_fr2_fr3_1 - outage"), "RTE_FFR2AA1--DDE3AA1--1 - RTE_co1_fr2_fr3_1 - outage", "RTE_FFR2AA1--DDE3AA1--1 - RTE_co1_fr2_fr3_1 - outage",
                "FFR2AA1--DDE3AA1--1", OUTAGE, "co1_fr2_fr3_1", 1200., -1200., Side.RIGHT);

        // Check PST RAs
        CsaProfileCracCreationTestUtil.assertPstRangeActionImported(cracCreationContext, "pst_be", "BBE2AA1--BBE3AA1--1", false, 1);
        CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "pst_be", CURATIVE, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertPstRangeActionImported(cracCreationContext, "pst_fr_cra", "FFR2AA1--FFR4AA1--1", false, 1);
        CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "pst_fr_cra", CURATIVE, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertPstRangeActionImported(cracCreationContext, "pst_fr_pra", "FFR2AA1--FFR4AA1--1", false, 1);
        CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "pst_fr_pra", PREVENTIVE, UsageMethod.AVAILABLE);

        // Check topo RAs
        CsaProfileCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "close_fr1_fr5", Set.of("FFR1AA1Z-FFR1AA1--1"), false, 1);
        CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "close_fr1_fr5", CURATIVE, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "open_fr1_fr2", Set.of("FFR1AA1Y-FFR1AA1--1"), false, 1);
        CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "open_fr1_fr2", PREVENTIVE, UsageMethod.AVAILABLE);
        CsaProfileCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "open_fr1_fr3", Set.of("FFR1AA1X-FFR1AA1--1"), false, 1);
        CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "open_fr1_fr3", PREVENTIVE, UsageMethod.AVAILABLE);
    }

    /*
    // TODO check with Thomas, no contingencies are created
    @Test
    public void checkExcludedCombinationConstraintHandling() {
        //CSA 63_1
        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/CSA_63_1_ValidationTest.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();

        Network network = Mockito.mock(Network.class);
        BusbarSection terminal1Mock = Mockito.mock(BusbarSection.class);
        BusbarSection terminal2Mock = Mockito.mock(BusbarSection.class);
        BusbarSection terminal3Mock = Mockito.mock(BusbarSection.class);
        BusbarSection terminal4Mock = Mockito.mock(BusbarSection.class);
        Switch switchMock = Mockito.mock(Switch.class);
        Branch networkElementMock = Mockito.mock(Branch.class);

        Mockito.when(terminal1Mock.getId()).thenReturn("c2e3e624-8389-4a50-a2f8-be51631ba221");
        Mockito.when(terminal2Mock.getId()).thenReturn("073a3aef-f425-48ec-ba72-d68ad4dd333c");
        Mockito.when(terminal3Mock.getId()).thenReturn("9b476375-4005-46b5-8465-0192dfdbba51");
        Mockito.when(terminal4Mock.getId()).thenReturn("175aa88b-c62a-4411-b4df-474c436692d0");

        Mockito.when(terminal1Mock.getType()).thenReturn(IdentifiableType.BUS);
        Mockito.when(terminal2Mock.getType()).thenReturn(IdentifiableType.BUS);
        Mockito.when(terminal3Mock.getType()).thenReturn(IdentifiableType.BUS);
        Mockito.when(terminal4Mock.getType()).thenReturn(IdentifiableType.BUS);
        Mockito.when(switchMock.getId()).thenReturn("d1db384f-3a27-434b-93f5-5afa3ab23b00");
        Mockito.when(networkElementMock.getId()).thenReturn("ff3c8013-d3f9-4198-a1f2-98d3ebdf30c4");
        Mockito.when(switchMock.isOpen()).thenReturn(false);
        Mockito.when(network.getIdentifiable("c2e3e624-8389-4a50-a2f8-be51631ba221")).thenReturn((Identifiable) terminal1Mock);
        Mockito.when(network.getIdentifiable("073a3aef-f425-48ec-ba72-d68ad4dd333c")).thenReturn((Identifiable) terminal2Mock);
        Mockito.when(network.getIdentifiable("9b476375-4005-46b5-8465-0192dfdbba51")).thenReturn((Identifiable) terminal1Mock);
        Mockito.when(network.getIdentifiable("175aa88b-c62a-4411-b4df-474c436692d0")).thenReturn((Identifiable) terminal2Mock);
        Mockito.when(network.getIdentifiable("d1db384f-3a27-434b-93f5-5afa3ab23b00")).thenReturn((Identifiable) switchMock);
        Mockito.when(network.getIdentifiable("ff3c8013-d3f9-4198-a1f2-98d3ebdf30c4")).thenReturn(networkElementMock);

        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(2, cracCreationContext.getCrac().getAngleCnecs().size());
        List<AngleCnec> angleCnecs = cracCreationContext.getCrac().getAngleCnecs().stream()
                .sorted(Comparator.comparing(AngleCnec::getId)).toList();

        // RTE_AE1 - RTE_CO1 - curative
        AngleCnec angleCnec1 = angleCnecs.iterator().next();
        assertEquals("RTE_AE1 - RTE_CO1 - curative", angleCnec1.getId());
        assertEquals("RTE_AE1 - RTE_CO1 - curative", angleCnec1.getName());
        assertEquals("c2e3e624-8389-4a50-a2f8-be51631ba221", angleCnec1.getImportingNetworkElement().getId());
        assertEquals("073a3aef-f425-48ec-ba72-d68ad4dd333c", angleCnec1.getExportingNetworkElement().getId());
        assertEquals(-60.0, angleCnec1.getLowerBound(Unit.DEGREE).get());
        assertEquals(60.0, angleCnec1.getUpperBound(Unit.DEGREE).get());

        // RTE_AE2 - RTE_CO3 - curative
        AngleCnec angleCnec2 = angleCnecs.get(1);
        assertEquals("RTE_AE2 - RTE_CO3 - curative", angleCnec2.getId());
        assertEquals("RTE_AE2 - RTE_CO3 - curative", angleCnec2.getName());
        assertEquals("9b476375-4005-46b5-8465-0192dfdbba51", angleCnec2.getImportingNetworkElement().getId());
        assertEquals("175aa88b-c62a-4411-b4df-474c436692d0", angleCnec2.getExportingNetworkElement().getId());
        assertEquals(45.0, angleCnec2.getUpperBound(Unit.DEGREE).get());
        assertEquals(-45.0, angleCnec2.getUpperBound(Unit.DEGREE).get());


        assertEquals(3, cracCreationContext.getCrac().getContingencies().size());
        List<Contingency> listContingencies = cracCreationContext.getCrac().getContingencies()
                .stream().sorted(Comparator.comparing(Contingency::getId)).toList();
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.iterator().next(),
                "5bd378fa-700b-4b88-b73a-327100ad18bf", "RTE_CO1",
                1, List.of("ff3c8013-d3f9-4198-a1f2-98d3ebdf30c4"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(1),
                "2a193923-ac8a-40e8-b0f0-50ba28826317", "RTE_CO2",
                1, List.of("ff3c8013-d3f9-4198-a1f2-98d3ebdf30c4"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(2),
                "8f2cac52-ba92-4922-b8e2-2ee0414829f5", "RTE_CO3",
                1, List.of("ff3c8013-d3f9-4198-a1f2-98d3ebdf30c4"));

    }
     */
}
