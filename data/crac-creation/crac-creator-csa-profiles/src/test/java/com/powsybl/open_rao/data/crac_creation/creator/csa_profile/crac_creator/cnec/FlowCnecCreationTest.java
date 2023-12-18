package com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.cnec;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.open_rao.commons.logs.RaoBusinessWarns;
import com.powsybl.open_rao.data.crac_api.Instant;
import com.powsybl.open_rao.data.crac_api.cnec.FlowCnec;
import com.powsybl.open_rao.data.crac_api.cnec.Side;
import com.powsybl.open_rao.data.crac_api.range_action.PstRangeAction;
import com.powsybl.open_rao.data.crac_api.usage_rule.UsageMethod;
import com.powsybl.open_rao.data.crac_api.usage_rule.UsageRule;
import com.powsybl.open_rao.data.crac_creation.creator.api.ImportStatus;
import com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationTestUtil;
import com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileElementaryCreationContext;
import com.powsybl.open_rao.data.crac_impl.OnFlowConstraintImpl;
import com.powsybl.iidm.network.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationTestUtil.getLogs;
import static org.junit.jupiter.api.Assertions.*;

class FlowCnecCreationTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    @Test
    void checkOnFlowConstraintUsageRule() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/TestConfiguration_TC1_v29Mar2023.zip");
        Instant curativeInstant = cracCreationContext.getCrac().getInstant(CURATIVE_INSTANT_ID);

        PstRangeAction eliaRa1 = cracCreationContext.getCrac().getPstRangeAction("7fc2fc14-eea6-4e69-b8d9-a3edc218e687");

        assertEquals(1, eliaRa1.getUsageRules().size());

        assertEquals("ELIA_RA1", eliaRa1.getName());
        assertEquals("ELIA", eliaRa1.getOperator());
        assertEquals("36b83adb-3d45-4693-8967-96627b5f9ec9", eliaRa1.getNetworkElement().getId());

        Iterator<UsageRule> usageRuleIterator = eliaRa1.getUsageRules().iterator();
        UsageRule usageRule1 = usageRuleIterator.next();

        assertEquals(curativeInstant, usageRule1.getInstant());
        assertEquals("ELIA_AE1 (dd5247a7-3cb1-43f8-8ce1-12f285653f06) - ELIA_CO1 - curative", ((OnFlowConstraintImpl) usageRule1).getFlowCnec().getId());
        // TODO assert that UsageMethod.FORCED
        // assertEquals(UsageMethod.FORCED, usageRule1.getUsageMethod());
        assertEquals("5c1e945b-4598-437f-b8ae-7c6d4b475a6c", cracCreationContext.getRemedialActionCreationContexts().stream().filter(raC -> !raC.isImported()).findAny().get().getNativeId());

    }

    @Test
    void checkNoOnConstraintUsageRuleIsCreated() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_TestConfiguration_TC2_27Apr2023.zip");

        cracCreationContext.getCrac().getRemedialActions()
                .forEach(ra -> assertTrue(ra.getUsageRules().stream().noneMatch(usageRule -> usageRule instanceof OnFlowConstraintImpl)));
    }

    @Test
    void checkOnConstraintWith4FlowCnecs() {
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

        Mockito.when(networkElementMock1.getCurrentLimits(TwoSides.ONE)).thenReturn(Optional.of(currentLimits));
        Mockito.when(networkElementMock1.getCurrentLimits(TwoSides.TWO)).thenReturn(Optional.of(currentLimits));
        Mockito.when(networkElementMock1.getAliasFromType("CGMES.Terminal1")).thenReturn(Optional.of("60038442-5c02-21a9-22ad-f0554a65a466"));

        Branch networkElementMock2 = Mockito.mock(Branch.class);
        Mockito.when(networkElementMock2.getId()).thenReturn("65e9a6a7-8488-7b17-6344-cb7d61b7920b");
        Mockito.when(network.getIdentifiable("65e9a6a7-8488-7b17-6344-cb7d61b7920b")).thenReturn(networkElementMock2);
        Mockito.when(networkElementMock2.getTerminal1()).thenReturn(terminal1);
        Mockito.when(networkElementMock2.getTerminal2()).thenReturn(terminal2);

        Mockito.when(networkElementMock2.getCurrentLimits(TwoSides.ONE)).thenReturn(Optional.of(currentLimits));
        Mockito.when(networkElementMock2.getCurrentLimits(TwoSides.TWO)).thenReturn(Optional.of(currentLimits));
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

        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/csa-11/CSA_11_3_OnFlowConstraint.zip", network);
        Instant preventiveInstant = cracCreationContext.getCrac().getInstant(PREVENTIVE_INSTANT_ID);
        Instant curativeInstant = cracCreationContext.getCrac().getInstant(CURATIVE_INSTANT_ID);

        // Check Flow Cnecs
        assertEquals(4, cracCreationContext.getCrac().getFlowCnecs().size());
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(cracCreationContext.getCrac().getFlowCnec("RTE_AE1 (e2b71e64-ce03-4aa3-9adc-7ff910adae36) - RTE_CO1 - curative"),
                "RTE_AE1 (e2b71e64-ce03-4aa3-9adc-7ff910adae36) - RTE_CO1 - curative",
                "RTE_AE1 (e2b71e64-ce03-4aa3-9adc-7ff910adae36) - RTE_CO1 - curative",
                "60038442-5c02-21a9-22ad-f0554a65a466",
                curativeInstant,
                "6c9656a6-84c2-4967-aabc-51f63a7abdf1",
                1000.,
                -1000.,
                Side.LEFT);

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(cracCreationContext.getCrac().getFlowCnec("RTE_AE1 (e2b71e64-ce03-4aa3-9adc-7ff910adae36) - preventive"),
                "RTE_AE1 (e2b71e64-ce03-4aa3-9adc-7ff910adae36) - preventive",
                "RTE_AE1 (e2b71e64-ce03-4aa3-9adc-7ff910adae36) - preventive",
                "60038442-5c02-21a9-22ad-f0554a65a466",
                preventiveInstant,
                null,
                1000.,
                -1000.,
                Side.LEFT);

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(cracCreationContext.getCrac().getFlowCnec("RTE_AE2 (a418e290-0d0c-4f40-b7fa-31fca1a2607d) - RTE_CO2 - curative"),
                "RTE_AE2 (a418e290-0d0c-4f40-b7fa-31fca1a2607d) - RTE_CO2 - curative",
                "RTE_AE2 (a418e290-0d0c-4f40-b7fa-31fca1a2607d) - RTE_CO2 - curative",
                "65e9a6a7-8488-7b17-6344-cb7d61b7920b",
                curativeInstant,
                "410a7075-51df-4c5c-aa80-0bb1bbe41190",
                1000.,
                -1000.,
                Side.RIGHT);

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(cracCreationContext.getCrac().getFlowCnec("RTE_AE2 (a418e290-0d0c-4f40-b7fa-31fca1a2607d) - preventive"),
                "RTE_AE2 (a418e290-0d0c-4f40-b7fa-31fca1a2607d) - preventive",
                "RTE_AE2 (a418e290-0d0c-4f40-b7fa-31fca1a2607d) - preventive",
                "65e9a6a7-8488-7b17-6344-cb7d61b7920b",
                preventiveInstant,
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

        CsaProfileCracCreationTestUtil.assertHasOnFlowConstraintUsageRule(cracCreationContext, "6c283463-9aac-4d9b-9d0b-6710c5b2aa00", "RTE_AE1 (e2b71e64-ce03-4aa3-9adc-7ff910adae36) - preventive", preventiveInstant, UsageMethod.TO_BE_EVALUATED); // TODO change TO_BE_EVALUATED by AVAILABLE
        CsaProfileCracCreationTestUtil.assertHasOnFlowConstraintUsageRule(cracCreationContext, "6c283463-9aac-4d9b-9d0b-6710c5b2aa00", "RTE_AE1 (e2b71e64-ce03-4aa3-9adc-7ff910adae36) - RTE_CO1 - curative", preventiveInstant, UsageMethod.TO_BE_EVALUATED);

        CsaProfileCracCreationTestUtil.assertHasOnFlowConstraintUsageRule(cracCreationContext, "0af9ce7e-8013-4362-96a0-40ac0a970eb6", "RTE_AE2 (a418e290-0d0c-4f40-b7fa-31fca1a2607d) - preventive", preventiveInstant, UsageMethod.TO_BE_EVALUATED); // TODO change TO_BE_EVALUATED by AVAILABLE
        CsaProfileCracCreationTestUtil.assertHasOnFlowConstraintUsageRule(cracCreationContext, "0af9ce7e-8013-4362-96a0-40ac0a970eb6", "RTE_AE2 (a418e290-0d0c-4f40-b7fa-31fca1a2607d) - RTE_CO2 - curative", preventiveInstant, UsageMethod.TO_BE_EVALUATED);

        CsaProfileCracCreationTestUtil.assertHasOnFlowConstraintUsageRule(cracCreationContext, "f17a745b-60a1-4acd-887f-ebc8349b4597", "RTE_AE2 (a418e290-0d0c-4f40-b7fa-31fca1a2607d) - preventive", preventiveInstant, UsageMethod.TO_BE_EVALUATED); // TODO change TO_BE_EVALUATED by AVAILABLE
        CsaProfileCracCreationTestUtil.assertHasOnFlowConstraintUsageRule(cracCreationContext, "f17a745b-60a1-4acd-887f-ebc8349b4597", "RTE_AE2 (a418e290-0d0c-4f40-b7fa-31fca1a2607d) - RTE_CO2 - curative", preventiveInstant, UsageMethod.TO_BE_EVALUATED);

        CsaProfileCracCreationTestUtil.assertHasOnFlowConstraintUsageRule(cracCreationContext, "a8f21a9a-49dc-4c2a-9745-405392f0d87b", "RTE_AE1 (e2b71e64-ce03-4aa3-9adc-7ff910adae36) - RTE_CO1 - curative", curativeInstant, UsageMethod.TO_BE_EVALUATED); // TODO change TO_BE_EVALUATED by AVAILABLE
        CsaProfileCracCreationTestUtil.assertHasOnFlowConstraintUsageRule(cracCreationContext, "a8f21a9a-49dc-4c2a-9745-405392f0d87b", "RTE_AE2 (a418e290-0d0c-4f40-b7fa-31fca1a2607d) - RTE_CO2 - curative", curativeInstant, UsageMethod.TO_BE_EVALUATED);
    }

    @Test
    void testCustomForAssessedElementWithContingencyRejection() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_42_CustomExample.zip");
        Instant preventiveInstant = cracCreationContext.getCrac().getInstant(PREVENTIVE_INSTANT_ID);
        Instant curativeInstant = cracCreationContext.getCrac().getInstant(CURATIVE_INSTANT_ID);

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(4, cracCreationContext.getCrac().getFlowCnecs().size());
        List<FlowCnec> listFlowCnecs = cracCreationContext.getCrac().getFlowCnecs()
                .stream().sorted(Comparator.comparing(FlowCnec::getId)).toList();

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.iterator().next(),
                "RTE_AE (183829bd-5c60-4c04-ad57-c72a15a75047) - RTE_CO1 - curative",
                "RTE_AE (183829bd-5c60-4c04-ad57-c72a15a75047) - RTE_CO1 - curative",
                "FFR3AA1--FFR5AA1--1",
                curativeInstant, "0451f8be-83d7-45da-b80b-4014259ff624",
                +1000., -1000., Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(1),
                "RTE_AE (183829bd-5c60-4c04-ad57-c72a15a75047) - RTE_CO3 - curative",
                "RTE_AE (183829bd-5c60-4c04-ad57-c72a15a75047) - RTE_CO3 - curative",
                "FFR3AA1--FFR5AA1--1",
                curativeInstant, "4491d904-93c4-41d4-a509-57f9fed2e31c",
                +1000., -1000., Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(2),
                "RTE_AE (183829bd-5c60-4c04-ad57-c72a15a75047) - preventive",
                "RTE_AE (183829bd-5c60-4c04-ad57-c72a15a75047) - preventive",
                "FFR3AA1--FFR5AA1--1",
                preventiveInstant, null,
                +1000., -1000., Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(3),
                "RTE_AE2 (283829bd-5c60-4c04-ad57-c72a15a75047) - preventive",
                "RTE_AE2 (283829bd-5c60-4c04-ad57-c72a15a75047) - preventive",
                "FFR3AA1--FFR5AA1--1",
                preventiveInstant, null,
                +1000., -1000., Side.RIGHT);
    }

    @Test
    void testTC1FlowCnecs() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/TestConfiguration_TC1_v29Mar2023.zip");
        Instant preventiveInstant = cracCreationContext.getCrac().getInstant(PREVENTIVE_INSTANT_ID);
        Instant curativeInstant = cracCreationContext.getCrac().getInstant(CURATIVE_INSTANT_ID);

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(4, cracCreationContext.getCrac().getFlowCnecs().size());
        List<FlowCnec> listFlowCnecs = cracCreationContext.getCrac().getFlowCnecs()
                .stream().sorted(Comparator.comparing(FlowCnec::getId)).toList();

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.iterator().next(),
                "ELIA_AE1 (dd5247a7-3cb1-43f8-8ce1-12f285653f06) - ELIA_CO1 - curative",
                "ELIA_AE1 (dd5247a7-3cb1-43f8-8ce1-12f285653f06) - ELIA_CO1 - curative",
                "ffbabc27-1ccd-4fdc-b037-e341706c8d29",
                curativeInstant, "493480ba-93c3-426e-bee5-347d8dda3749",
                +1312., -1312., Side.LEFT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(1),
                "ELIA_AE1 (dd5247a7-3cb1-43f8-8ce1-12f285653f06) - preventive",
                "ELIA_AE1 (dd5247a7-3cb1-43f8-8ce1-12f285653f06) - preventive",
                "ffbabc27-1ccd-4fdc-b037-e341706c8d29",
                preventiveInstant, null,
                +1312., -1312., Side.LEFT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(2),
                "TENNET_TSO_AE1NL (adad76ed-79e7-4985-84e1-eb493f168c85) - TENNET_TSO_CO1 - curative",
                "TENNET_TSO_AE1NL (adad76ed-79e7-4985-84e1-eb493f168c85) - TENNET_TSO_CO1 - curative",
                "b18cd1aa-7808-49b9-a7cf-605eaf07b006 + e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc",
                curativeInstant, "c0a25fd7-eee0-4191-98a5-71a74469d36e",
                +1876., -1876., Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(3),
                "TENNET_TSO_AE1NL (adad76ed-79e7-4985-84e1-eb493f168c85) - preventive",
                "TENNET_TSO_AE1NL (adad76ed-79e7-4985-84e1-eb493f168c85) - preventive",
                "b18cd1aa-7808-49b9-a7cf-605eaf07b006 + e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc",
                preventiveInstant, null,
                +1876., -1876., Side.RIGHT);

        // csa-9-1
        assertTrue(cracCreationContext.getCrac().getNetworkActions().isEmpty());
    }

    @Test
    void testTC2FlowCnecs() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_TestConfiguration_TC2_Draft_v14Apr2023.zip");

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertTrue(cracCreationContext.getCrac().getFlowCnecs().isEmpty());
    }

    @Test
    void testTC2FlowCnecsWithOperationalLimits() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/TC2_OperationalLimits.zip");
        Instant preventiveInstant = cracCreationContext.getCrac().getInstant(PREVENTIVE_INSTANT_ID);
        Instant autoInstant = cracCreationContext.getCrac().getInstant(AUTO_INSTANT_ID);
        Instant curativeInstant = cracCreationContext.getCrac().getInstant(CURATIVE_INSTANT_ID);

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(15, cracCreationContext.getCrac().getContingencies().size());
        assertEquals(12, cracCreationContext.getCrac().getFlowCnecs().size());

        List<FlowCnec> listFlowCnecs = cracCreationContext.getCrac().getFlowCnecs()
                .stream().sorted(Comparator.comparing(FlowCnec::getId)).collect(Collectors.toList());

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(0),
                "ELIA_AE2 (d463cbba-c89c-4199-bbb9-1a33d90cae2c) - preventive",
                "ELIA_AE2 (d463cbba-c89c-4199-bbb9-1a33d90cae2c) - preventive",
                "b58bf21a-096a-4dae-9a01-3f03b60c24c7",
                preventiveInstant, null,
                +1574d, -1574d, Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(1),
                "REE_AE1 (2cc84c47-b6b7-481c-aa0e-6c46589d458a) - REE_CO1 - curative",
                "REE_AE1 (2cc84c47-b6b7-481c-aa0e-6c46589d458a) - REE_CO1 - curative",
                "891e77ff-39c6-4648-8eda-d81f730271f9 + a04e4e41-c0b4-496e-9ef3-390ea089411f",
                curativeInstant, "8cdec4c6-10c3-40c1-9eeb-7f6ae8d9b3fe",
                +1000d, -1000d, Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(2),
                "REE_AE1 (2cc84c47-b6b7-481c-aa0e-6c46589d458a) - preventive",
                "REE_AE1 (2cc84c47-b6b7-481c-aa0e-6c46589d458a) - preventive",
                "891e77ff-39c6-4648-8eda-d81f730271f9 + a04e4e41-c0b4-496e-9ef3-390ea089411f",
                preventiveInstant, null,
                +1000d, -1000d, Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(3),
                "REE_AE2 (3a364a5b-7aca-4e48-abd5-4cd79e2e8676) - REE_CO2 - curative",
                "REE_AE2 (3a364a5b-7aca-4e48-abd5-4cd79e2e8676) - REE_CO2 - curative",
                "044cd003-c766-11e1-8775-005056c00008",
                curativeInstant, "b6b780cb-9fe5-4c45-989d-447a927c3874",
                +1000d, -1000d, Side.LEFT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(4),
                "REE_AE2 (3a364a5b-7aca-4e48-abd5-4cd79e2e8676) - preventive",
                "REE_AE2 (3a364a5b-7aca-4e48-abd5-4cd79e2e8676) - preventive",
                "044cd003-c766-11e1-8775-005056c00008",
                preventiveInstant, null,
                +1000d, -1000d, Side.LEFT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(5),
                "REE_AE3 (989535e7-3789-47e7-8ba7-da7be9962a15) - REE_CO3 - auto",
                "REE_AE3 (989535e7-3789-47e7-8ba7-da7be9962a15) - REE_CO3 - auto",
                "048badc5-c766-11e1-8775-005056c00008",
                autoInstant, "13334fdf-9cc2-4341-adb6-1281269040b4",
                +500.0, -500.0, Side.LEFT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(6),
                "REE_AE3 (989535e7-3789-47e7-8ba7-da7be9962a15) - preventive",
                "REE_AE3 (989535e7-3789-47e7-8ba7-da7be9962a15) - preventive",
                "048badc5-c766-11e1-8775-005056c00008",
                preventiveInstant, null,
                +500d, -500d, Side.LEFT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(7),
                "REE_AE4 (ea577780-24d2-4167-a5d2-fa56c56a9481) - preventive",
                "REE_AE4 (ea577780-24d2-4167-a5d2-fa56c56a9481) - preventive",
                "0478c207-c766-11e1-8775-005056c00008",
                preventiveInstant, null,
                +1000d, -1000d, Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(8),
                "REE_AE5 (2bd363ca-bab7-4571-a59a-b9193dc9fc9d) - REE_CO4 - curative",
                "REE_AE5 (2bd363ca-bab7-4571-a59a-b9193dc9fc9d) - REE_CO4 - curative",
                "048badc5-c766-11e1-8775-005056c00008",
                curativeInstant, "9d17b84c-33b5-4a68-b8b9-ed5b31038d40",
                +1000d, -1000d, Side.LEFT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(9),
                "REE_AE5 (2bd363ca-bab7-4571-a59a-b9193dc9fc9d) - preventive",
                "REE_AE5 (2bd363ca-bab7-4571-a59a-b9193dc9fc9d) - preventive",
                "048badc5-c766-11e1-8775-005056c00008",
                preventiveInstant, null,
                +1000d, -1000d, Side.LEFT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(10),
                "REE_AE6 (3bc10ae6-6e25-4e4d-9a3a-ec2c4e2436b0) - REE_CO5 - curative",
                "REE_AE6 (3bc10ae6-6e25-4e4d-9a3a-ec2c4e2436b0) - REE_CO5 - curative",
                "044a5f09-c766-11e1-8775-005056c00008",
                curativeInstant, "96c96ad8-844c-4f3b-8b38-c886ba2c0214",
                +2000d, -2000d, Side.LEFT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(11),
                "REE_AE6 (3bc10ae6-6e25-4e4d-9a3a-ec2c4e2436b0) - preventive",
                "REE_AE6 (3bc10ae6-6e25-4e4d-9a3a-ec2c4e2436b0) - preventive",
                "044a5f09-c766-11e1-8775-005056c00008",
                preventiveInstant, null,
                +2000d, -2000d, Side.LEFT);

        List<CsaProfileElementaryCreationContext> alteredCnecs = cracCreationContext.getCnecCreationContexts().stream().filter(CsaProfileElementaryCreationContext::isAltered).toList();
        assertEquals(1, alteredCnecs.size());
        assertEquals("REE_AE3 (989535e7-3789-47e7-8ba7-da7be9962a15) - preventive", alteredCnecs.get(0).getElementId());
        assertEquals(ImportStatus.IMPORTED, alteredCnecs.get(0).getImportStatus());
        assertEquals("the AssessedElement was pointing to a TATL and used inBaseCase. For the preventive instant, this TATL was also used as a PATL to create the CNEC", alteredCnecs.get(0).getImportStatusDetail());
    }

    @Test
    void testCreateCracCSATestWithRejectedFiles() {
        ListAppender<ILoggingEvent> listAppender = getLogs(RaoBusinessWarns.class);
        List<ILoggingEvent> logsList = listAppender.list;

        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_Test_With_Rejected_Files.zip");

        List<ILoggingEvent> logListSorted = logsList.stream().filter(log -> log.getFormattedMessage().contains("Its dates are not consistent")).sorted(Comparator.comparing(ILoggingEvent::getMessage)).toList();
        assertEquals(2, logListSorted.size());
        assertEquals("[WARN] [REMOVED] The file : contexts:ELIA_AE.xml will be ignored. Its dates are not consistent with the import date : 2023-03-29T12:00Z", logListSorted.get(0).toString());
        assertEquals("[WARN] [REMOVED] The file : contexts:REE_CO.xml will be ignored. Its dates are not consistent with the import date : 2023-03-29T12:00Z", logListSorted.get(1).toString());

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertTrue(cracCreationContext.getCrac().getFlowCnecs().isEmpty());
    }

    @Test
    void testCreateCracCSATestWithRejectedFilesOperationalLimits() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_Test_With_Rejected_Files_Operational_Limits.zip");
        Instant preventiveInstant = cracCreationContext.getCrac().getInstant(PREVENTIVE_INSTANT_ID);

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(7, cracCreationContext.getCrac().getContingencies().size());
        assertEquals(4, cracCreationContext.getCrac().getFlowCnecs().size());

        List<FlowCnec> listFlowCnecs = cracCreationContext.getCrac().getFlowCnecs()
                .stream().sorted(Comparator.comparing(FlowCnec::getId)).toList();

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(0),
                "REE_AE3 (989535e7-3789-47e7-8ba7-da7be9962a15) - preventive",
                "REE_AE3 (989535e7-3789-47e7-8ba7-da7be9962a15) - preventive",
                "048badc5-c766-11e1-8775-005056c00008",
                preventiveInstant, null,
                +500d, -500d, Side.LEFT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(1),
                "REE_AE4 (ea577780-24d2-4167-a5d2-fa56c56a9481) - preventive",
                "REE_AE4 (ea577780-24d2-4167-a5d2-fa56c56a9481) - preventive",
                "0478c207-c766-11e1-8775-005056c00008",
                preventiveInstant, null,
                +1000d, -1000d, Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(2),
                "REE_AE5 (2bd363ca-bab7-4571-a59a-b9193dc9fc9d) - preventive",
                "REE_AE5 (2bd363ca-bab7-4571-a59a-b9193dc9fc9d) - preventive",
                "048badc5-c766-11e1-8775-005056c00008",
                preventiveInstant, null,
                +1000d, -1000d, Side.LEFT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(3),
                "REE_AE6 (3bc10ae6-6e25-4e4d-9a3a-ec2c4e2436b0) - preventive",
                "REE_AE6 (3bc10ae6-6e25-4e4d-9a3a-ec2c4e2436b0) - preventive",
                "044a5f09-c766-11e1-8775-005056c00008",
                preventiveInstant, null,
                +2000.0, -2000.0, Side.LEFT);
    }

    @Test
    void testCreateCracCSATestWithRefusedContingencies() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/Test_With_Refused_Contingencies.zip");
        Instant preventiveInstant = cracCreationContext.getCrac().getInstant(PREVENTIVE_INSTANT_ID);
        Instant curativeInstant = cracCreationContext.getCrac().getInstant(CURATIVE_INSTANT_ID);

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(4, cracCreationContext.getCrac().getFlowCnecs().size());

        List<FlowCnec> listFlowCnecs = cracCreationContext.getCrac().getFlowCnecs()
                .stream().sorted(Comparator.comparing(FlowCnec::getId)).toList();

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(0),
                "ELIA_AE1 (dd5247a7-3cb1-43f8-8ce1-12f285653f06) - ELIA_CO1 - curative",
                "ELIA_AE1 (dd5247a7-3cb1-43f8-8ce1-12f285653f06) - ELIA_CO1 - curative",
                "ffbabc27-1ccd-4fdc-b037-e341706c8d29",
                curativeInstant, "493480ba-93c3-426e-bee5-347d8dda3749",
                +1312d, -1312d, Side.LEFT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(1),
                "ELIA_AE1 (dd5247a7-3cb1-43f8-8ce1-12f285653f06) - preventive",
                "ELIA_AE1 (dd5247a7-3cb1-43f8-8ce1-12f285653f06) - preventive",
                "ffbabc27-1ccd-4fdc-b037-e341706c8d29",
                preventiveInstant, null,
                +1312d, -1312d, Side.LEFT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(2),
                "TENNET_TSO_AE1NL (adad76ed-79e7-4985-84e1-eb493f168c85) - TENNET_TSO_CO1 - curative",
                "TENNET_TSO_AE1NL (adad76ed-79e7-4985-84e1-eb493f168c85) - TENNET_TSO_CO1 - curative",
                "b18cd1aa-7808-49b9-a7cf-605eaf07b006 + e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc",
                curativeInstant, "c0a25fd7-eee0-4191-98a5-71a74469d36e",
                +1876d, -1876d, Side.RIGHT);
        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(listFlowCnecs.get(3),
                "TENNET_TSO_AE1NL (adad76ed-79e7-4985-84e1-eb493f168c85) - preventive",
                "TENNET_TSO_AE1NL (adad76ed-79e7-4985-84e1-eb493f168c85) - preventive",
                "b18cd1aa-7808-49b9-a7cf-605eaf07b006 + e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc",
                preventiveInstant, null,
                +1876d, -1876d, Side.RIGHT);
    }

    @Test
    void createFlowCnecsFromAssessedElementsWithConductingEquipment() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_79_1.zip");
        Instant preventiveInstant = cracCreationContext.getCrac().getInstant(PREVENTIVE_INSTANT_ID);
        Instant autoInstant = cracCreationContext.getCrac().getInstant(AUTO_INSTANT_ID);
        Instant curativeInstant = cracCreationContext.getCrac().getInstant(CURATIVE_INSTANT_ID);

        List<FlowCnec> importedFlowCnecs = cracCreationContext.getCrac().getFlowCnecs()
                .stream().sorted(Comparator.comparing(FlowCnec::getId)).toList();
        assertEquals(7, importedFlowCnecs.size());

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
                importedFlowCnecs.get(0),
                "RTE_AE1 (183829bd-5c60-4c04-ad57-c72a15a75047) - RTE_CO1 - auto",
                "RTE_AE1 (183829bd-5c60-4c04-ad57-c72a15a75047) - RTE_CO1 - auto",
                "b58bf21a-096a-4dae-9a01-3f03b60c24c7",
                autoInstant,
                "bbda9fe0-77e0-4f8e-b9d9-4402a539f2b7",
                +500d,
                -500d,
                +500d,
                -500d,
                Set.of(Side.RIGHT, Side.LEFT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
                importedFlowCnecs.get(1),
                "RTE_AE1 (183829bd-5c60-4c04-ad57-c72a15a75047) - RTE_CO1 - curative",
                "RTE_AE1 (183829bd-5c60-4c04-ad57-c72a15a75047) - RTE_CO1 - curative",
                "b58bf21a-096a-4dae-9a01-3f03b60c24c7",
                curativeInstant,
                "bbda9fe0-77e0-4f8e-b9d9-4402a539f2b7",
                +1574d,
                -1574d,
                +1574d,
                -1574d,
                Set.of(Side.RIGHT, Side.LEFT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
                importedFlowCnecs.get(2),
                "RTE_AE1 (183829bd-5c60-4c04-ad57-c72a15a75047) - preventive",
                "RTE_AE1 (183829bd-5c60-4c04-ad57-c72a15a75047) - preventive",
                "b58bf21a-096a-4dae-9a01-3f03b60c24c7",
                preventiveInstant,
                null,
                +1574d,
                -1574d,
                +1574d,
                -1574d,
                Set.of(Side.RIGHT, Side.LEFT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
                importedFlowCnecs.get(3),
                "RTE_AE2 (de5b6775-b8d7-4ac4-9619-ecc30d98f3db) - RTE_CO1 - curative",
                "RTE_AE2 (de5b6775-b8d7-4ac4-9619-ecc30d98f3db) - RTE_CO1 - curative",
                "3852b69e-22d2-41cb-b370-fb372ddcde19",
                curativeInstant,
                "bbda9fe0-77e0-4f8e-b9d9-4402a539f2b7",
                +1283d,
                -1283d,
                +10188.5,
                -10188.5,
                Set.of(Side.RIGHT, Side.LEFT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
                importedFlowCnecs.get(4),
                "RTE_AE2 (de5b6775-b8d7-4ac4-9619-ecc30d98f3db) - preventive",
                "RTE_AE2 (de5b6775-b8d7-4ac4-9619-ecc30d98f3db) - preventive",
                "3852b69e-22d2-41cb-b370-fb372ddcde19",
                preventiveInstant,
                null,
                +1283d,
                -1283d,
                +10188.5,
                -10188.5,
                Set.of(Side.RIGHT, Side.LEFT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
                importedFlowCnecs.get(5),
                "RTE_AE3 (08d0622e-02c7-4c20-8dbd-992ef4e6be0a) - RTE_CO1 - curative",
                "RTE_AE3 (08d0622e-02c7-4c20-8dbd-992ef4e6be0a) - RTE_CO1 - curative",
                "4c390dab-3499-4a9f-9cee-5877081479ab + edf1af97-de49-45bb-a7ef-31f96d486712",
                curativeInstant,
                "bbda9fe0-77e0-4f8e-b9d9-4402a539f2b7",
                +1233.9,
                -1233.9,
                +1000d,
                -1000d,
                Set.of(Side.RIGHT, Side.LEFT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
                importedFlowCnecs.get(6),
                "RTE_AE3 (08d0622e-02c7-4c20-8dbd-992ef4e6be0a) - preventive",
                "RTE_AE3 (08d0622e-02c7-4c20-8dbd-992ef4e6be0a) - preventive",
                "4c390dab-3499-4a9f-9cee-5877081479ab + edf1af97-de49-45bb-a7ef-31f96d486712",
                preventiveInstant,
                null,
                +1233.9,
                -1233.9,
                +1000d,
                -1000d,
                Set.of(Side.RIGHT, Side.LEFT)
        );
    }
}
