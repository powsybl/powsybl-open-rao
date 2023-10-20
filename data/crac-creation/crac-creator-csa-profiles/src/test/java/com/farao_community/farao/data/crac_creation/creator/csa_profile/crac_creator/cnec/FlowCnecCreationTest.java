package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.cnec;

import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.CsaProfileCrac;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationTestUtil;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreator;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreatorTest;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.importer.CsaProfileCracImporter;
import com.farao_community.farao.data.crac_impl.OnFlowConstraintImpl;
import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;

import static com.farao_community.farao.data.crac_api.Instant.CURATIVE;
import static com.farao_community.farao.data.crac_api.Instant.PREVENTIVE;
import static org.junit.jupiter.api.Assertions.*;

public class FlowCnecCreationTest {
    @Test
    public void checkOnFlowConstraintUsageRule() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/TestConfiguration_TC1_v29Mar2023.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/TestConfiguration_TC1_v29Mar2023.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        PstRangeAction eliaRa1 = cracCreationContext.getCrac().getPstRangeAction("7fc2fc14-eea6-4e69-b8d9-a3edc218e687");

        assertEquals(1, eliaRa1.getUsageRules().size());

        assertEquals("ELIA_RA1", eliaRa1.getName());
        assertEquals("ELIA", eliaRa1.getOperator());
        assertEquals("36b83adb-3d45-4693-8967-96627b5f9ec9", eliaRa1.getNetworkElement().getId());

        Iterator<UsageRule> usageRuleIterator = eliaRa1.getUsageRules().iterator();
        UsageRule usageRule1 = usageRuleIterator.next();

        assertEquals(CURATIVE, usageRule1.getInstant());
        assertEquals("ELIA_AE1 - ELIA_CO1 - curative", ((OnFlowConstraintImpl) usageRule1).getFlowCnec().getId());
        // TODO assert that UsageMethod.FORCED
        // assertEquals(UsageMethod.FORCED, usageRule1.getUsageMethod());
        assertEquals("5c1e945b-4598-437f-b8ae-7c6d4b475a6c", cracCreationContext.getRemedialActionCreationContexts().stream().filter(raC -> !raC.isImported()).findAny().get().getNativeId());

    }

    @Test
    public void checkNoOnConstraintUsageRuleIsCreated() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/csa-9/CSA_TestConfiguration_TC2_27Apr2023.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-9/CSA_TestConfiguration_TC2_27Apr2023.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-04-27T12:00Z"), new CracCreationParameters());

        cracCreationContext.getCrac().getRemedialActions()
                .forEach(ra -> assertTrue(ra.getUsageRules().stream().noneMatch(usageRule -> usageRule instanceof OnFlowConstraintImpl)));
    }

    @Test
    public void checkOnConstraintWith4FlowCnecs() {
        Network network = Mockito.spy(Network.create("Test", "code"));

        VoltageLevel voltageLevel1 = Mockito.mock(VoltageLevel.class);
        Mockito.when(voltageLevel1.getNominalV()).thenReturn(400.0);
        VoltageLevel voltageLevel2 = Mockito.mock(VoltageLevel.class);
        Mockito.when(voltageLevel2.getNominalV()).thenReturn(400.0);

        Terminal terminal1 = Mockito.mock(Terminal.class);
        Mockito.when(terminal1.getVoltageLevel()).thenReturn(voltageLevel1);

        Terminal terminal2 = Mockito.mock(Terminal.class);
        Mockito.when(terminal2.getVoltageLevel()).thenReturn(voltageLevel2);

        CurrentLimits currentLimits = Mockito.mock(CurrentLimits.class);
        Mockito.when(currentLimits.getPermanentLimit()).thenReturn(400.);

        Branch networkElementMock1 = Mockito.mock(Branch.class);
        Mockito.when(networkElementMock1.getId()).thenReturn("60038442-5c02-21a9-22ad-f0554a65a466");
        Mockito.when(network.getIdentifiable("60038442-5c02-21a9-22ad-f0554a65a466")).thenReturn(networkElementMock1);
        Mockito.when(networkElementMock1.getTerminal1()).thenReturn(terminal1);
        Mockito.when(networkElementMock1.getTerminal2()).thenReturn(terminal2);

        Mockito.when(networkElementMock1.getCurrentLimits(Branch.Side.ONE)).thenReturn(Optional.of(currentLimits));
        Mockito.when(networkElementMock1.getCurrentLimits(Branch.Side.TWO)).thenReturn(Optional.of(currentLimits));
        Mockito.when(networkElementMock1.getAliasFromType("CGMES.Terminal1")).thenReturn(Optional.of("60038442-5c02-21a9-22ad-f0554a65a466"));

        Branch networkElementMock2 = Mockito.mock(Branch.class);
        Mockito.when(networkElementMock2.getId()).thenReturn("65e9a6a7-8488-7b17-6344-cb7d61b7920b");
        Mockito.when(network.getIdentifiable("65e9a6a7-8488-7b17-6344-cb7d61b7920b")).thenReturn(networkElementMock2);
        Mockito.when(networkElementMock2.getTerminal1()).thenReturn(terminal1);
        Mockito.when(networkElementMock2.getTerminal2()).thenReturn(terminal2);

        Mockito.when(networkElementMock2.getCurrentLimits(Branch.Side.ONE)).thenReturn(Optional.of(currentLimits));
        Mockito.when(networkElementMock2.getCurrentLimits(Branch.Side.TWO)).thenReturn(Optional.of(currentLimits));
        Mockito.when(networkElementMock2.getAliasFromType("CGMES.Terminal2")).thenReturn(Optional.of("65e9a6a7-8488-7b17-6344-cb7d61b7920b"));

        Branch networkElementLinkedToContingencies = Mockito.mock(Branch.class);
        Mockito.when(networkElementLinkedToContingencies.getId()).thenReturn("3a88a6a7-66fe-4988-9019-b3b288fd54ee");
        Mockito.when(network.getIdentifiable("3a88a6a7-66fe-4988-9019-b3b288fd54ee")).thenReturn(networkElementLinkedToContingencies);

        Switch switch1 = Mockito.mock(Switch.class);
        Mockito.when(switch1.isOpen()).thenReturn(false);
        Mockito.when(network.getSwitch("f9c8d9ce-6c44-4293-b60e-93c658411d68")).thenReturn(switch1);
        Switch switch2 = Mockito.mock(Switch.class);
        Mockito.when(switch2.isOpen()).thenReturn(false);
        Mockito.when(network.getSwitch("468fdb4a-49d6-4ea9-b216-928d057b65f0")).thenReturn(switch2);
        Switch switch3 = Mockito.mock(Switch.class);
        Mockito.when(switch3.isOpen()).thenReturn(false);
        Mockito.when(network.getSwitch("c8fcaef5-67f2-42c5-b736-ca91dcbcfe59")).thenReturn(switch3);
        Switch switch4 = Mockito.mock(Switch.class);
        Mockito.when(switch4.isOpen()).thenReturn(false);
        Mockito.when(network.getSwitch("50719289-6406-4d69-9dd7-6de60aecd2d4")).thenReturn(switch4);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-11/CSA_11_3_OnFlowConstraint.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-04-27T12:00Z"), new CracCreationParameters());

        // Check Flow Cnecs
        assertEquals(4, cracCreationContext.getCrac().getFlowCnecs().size());
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(cracCreationContext.getCrac().getFlowCnec("RTE_AE1 - RTE_CO1 - curative"),
                "RTE_AE1 - RTE_CO1 - curative",
                "RTE_AE1 - RTE_CO1 - curative",
                "60038442-5c02-21a9-22ad-f0554a65a466",
                CURATIVE,
                "6c9656a6-84c2-4967-aabc-51f63a7abdf1",
                1000.,
                -1000.,
                Side.LEFT);

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(cracCreationContext.getCrac().getFlowCnec("RTE_AE1 - preventive"),
                "RTE_AE1 - preventive",
                "RTE_AE1 - preventive",
                "60038442-5c02-21a9-22ad-f0554a65a466",
                PREVENTIVE,
                null,
                1000.,
                -1000.,
                Side.LEFT);

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(cracCreationContext.getCrac().getFlowCnec("RTE_AE2 - RTE_CO2 - curative"),
                "RTE_AE2 - RTE_CO2 - curative",
                "RTE_AE2 - RTE_CO2 - curative",
                "65e9a6a7-8488-7b17-6344-cb7d61b7920b",
                CURATIVE,
                "410a7075-51df-4c5c-aa80-0bb1bbe41190",
                1000.,
                -1000.,
                Side.RIGHT);

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(cracCreationContext.getCrac().getFlowCnec("RTE_AE2 - preventive"),
                "RTE_AE2 - preventive",
                "RTE_AE2 - preventive",
                "65e9a6a7-8488-7b17-6344-cb7d61b7920b",
                PREVENTIVE,
                null,
                1000.,
                -1000.,
                Side.RIGHT);

        //4 remedial actions and a total of 8 onFlowConstraint usage rules.
        assertEquals(4, cracCreationContext.getCrac().getRemedialActions().size());
        CsaProfileCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "6c283463-9aac-4d9b-9d0b-6710c5b2aa00", Set.of("f9c8d9ce-6c44-4293-b60e-93c658411d68"), true, 2);
        CsaProfileCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "0af9ce7e-8013-4362-96a0-40ac0a970eb6", Set.of("c8fcaef5-67f2-42c5-b736-ca91dcbcfe59"), false, 2);
        CsaProfileCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "f17a745b-60a1-4acd-887f-ebc8349b4597", Set.of("50719289-6406-4d69-9dd7-6de60aecd2d4"), true, 2);
        CsaProfileCracCreationTestUtil.assertNetworkActionImported(cracCreationContext, "a8f21a9a-49dc-4c2a-9745-405392f0d87b", Set.of("468fdb4a-49d6-4ea9-b216-928d057b65f0"), false, 2);

        CsaProfileCracCreationTestUtil.assertHasOnFlowConstraintUsageRule(cracCreationContext, "6c283463-9aac-4d9b-9d0b-6710c5b2aa00", "RTE_AE1 - preventive", PREVENTIVE, UsageMethod.TO_BE_EVALUATED); // TODO change TO_BE_EVALUATED by AVAILABLE
        CsaProfileCracCreationTestUtil.assertHasOnFlowConstraintUsageRule(cracCreationContext, "6c283463-9aac-4d9b-9d0b-6710c5b2aa00", "RTE_AE1 - RTE_CO1 - curative", PREVENTIVE, UsageMethod.TO_BE_EVALUATED);

        CsaProfileCracCreationTestUtil.assertHasOnFlowConstraintUsageRule(cracCreationContext, "0af9ce7e-8013-4362-96a0-40ac0a970eb6", "RTE_AE2 - preventive", PREVENTIVE, UsageMethod.TO_BE_EVALUATED); // TODO change TO_BE_EVALUATED by AVAILABLE
        CsaProfileCracCreationTestUtil.assertHasOnFlowConstraintUsageRule(cracCreationContext, "0af9ce7e-8013-4362-96a0-40ac0a970eb6", "RTE_AE2 - RTE_CO2 - curative", PREVENTIVE, UsageMethod.TO_BE_EVALUATED);

        CsaProfileCracCreationTestUtil.assertHasOnFlowConstraintUsageRule(cracCreationContext, "f17a745b-60a1-4acd-887f-ebc8349b4597", "RTE_AE2 - preventive", PREVENTIVE, UsageMethod.TO_BE_EVALUATED); // TODO change TO_BE_EVALUATED by AVAILABLE
        CsaProfileCracCreationTestUtil.assertHasOnFlowConstraintUsageRule(cracCreationContext, "f17a745b-60a1-4acd-887f-ebc8349b4597", "RTE_AE2 - RTE_CO2 - curative", PREVENTIVE, UsageMethod.TO_BE_EVALUATED);

        CsaProfileCracCreationTestUtil.assertHasOnFlowConstraintUsageRule(cracCreationContext, "a8f21a9a-49dc-4c2a-9745-405392f0d87b", "RTE_AE1 - RTE_CO1 - curative", CURATIVE, UsageMethod.TO_BE_EVALUATED); // TODO change TO_BE_EVALUATED by AVAILABLE
        CsaProfileCracCreationTestUtil.assertHasOnFlowConstraintUsageRule(cracCreationContext, "a8f21a9a-49dc-4c2a-9745-405392f0d87b", "RTE_AE2 - RTE_CO2 - curative", CURATIVE, UsageMethod.TO_BE_EVALUATED);
    }

    @Test
    public void testCustomForAssessedElementWithContingencyRejection() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/CSA_42_CustomExample.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/CSA_42_CustomExample.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(4, cracCreationContext.getCrac().getFlowCnecs().size());
        List<FlowCnec> listFlowCnecs = cracCreationContext.getCrac().getFlowCnecs()
                .stream().sorted(Comparator.comparing(FlowCnec::getId)).toList();

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.iterator().next(),
                "RTE_AE - RTE_CO1 - curative",
                "RTE_AE - RTE_CO1 - curative",
                "FFR3AA1--FFR5AA1--1",
                CURATIVE, "0451f8be-83d7-45da-b80b-4014259ff624",
                +1000., -1000., Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(1),
                "RTE_AE - RTE_CO3 - curative",
                "RTE_AE - RTE_CO3 - curative",
                "FFR3AA1--FFR5AA1--1",
                CURATIVE, "4491d904-93c4-41d4-a509-57f9fed2e31c",
                +1000., -1000., Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(2),
                "RTE_AE - preventive",
                "RTE_AE - preventive",
                "FFR3AA1--FFR5AA1--1",
                PREVENTIVE, null,
                +1000., -1000., Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(3),
                "RTE_AE2 - preventive",
                "RTE_AE2 - preventive",
                "FFR3AA1--FFR5AA1--1",
                PREVENTIVE, null,
                +1000., -1000., Side.RIGHT);
    }

}
