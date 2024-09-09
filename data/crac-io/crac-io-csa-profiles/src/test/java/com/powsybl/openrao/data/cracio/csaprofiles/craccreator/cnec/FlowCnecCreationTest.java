package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.cnec;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.AUTO_INSTANT_ID;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.CURATIVE_2_INSTANT_ID;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.NETWORK;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.OUTAGE_INSTANT_ID;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.PREVENTIVE_INSTANT_ID;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.assertCnecNotImported;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static org.junit.jupiter.api.Assertions.*;

class FlowCnecCreationTest {

    @Test
    void importFlowCnecs() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/profiles/cnecs/FlowCNECs.zip", NETWORK);

        List<FlowCnec> importedFlowCnecs = cracCreationContext.getCrac().getFlowCnecs().stream().sorted(Comparator.comparing(FlowCnec::getId)).toList();
        assertEquals(55, importedFlowCnecs.size());

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(0),
            "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 2 - TWO",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_2_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(1),
            "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 3 - TWO",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(2),
            "RTE_AE1 (assessed-element-1) - preventive - TWO",
            "FFR2AA1  FFR3AA1  1",
            PREVENTIVE_INSTANT_ID,
            null,
            null,
            null,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(3),
            "RTE_AE11 (assessed-element-11) - RTE_CO1 - curative 2 - TWO",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_2_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(4),
            "RTE_AE11 (assessed-element-11) - RTE_CO1 - curative 3 - TWO",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(5),
            "RTE_AE2 (assessed-element-2) - RTE_CO1 - outage - TWO - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +5000d,
            -5000d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(6),
            "RTE_AE2 (assessed-element-2) - RTE_CO2 - outage - TWO - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-2",
            null,
            null,
            +5000d,
            -5000d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(7),
            "RTE_AE3 (assessed-element-3) - RTE_CO2 - auto - TWO - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            AUTO_INSTANT_ID,
            "contingency-2",
            null,
            null,
            +4000d,
            -4000d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(8),
            "RTE_AE3 (assessed-element-3) - RTE_CO2 - curative 1 - TWO - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_1_INSTANT_ID,
            "contingency-2",
            null,
            null,
            +4000d,
            -4000d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(9),
            "RTE_AE4 (assessed-element-4) - RTE_CO1 - curative 2 - TWO",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_2_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(10),
            "RTE_AE4 (assessed-element-4) - RTE_CO1 - curative 3 - TWO",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(11),
            "RTE_AE4 (assessed-element-4) - RTE_CO2 - curative 2 - TWO",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_2_INSTANT_ID,
            "contingency-2",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(12),
            "RTE_AE4 (assessed-element-4) - RTE_CO2 - curative 3 - TWO",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-2",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(13),
            "RTE_AE5 (assessed-element-5) - RTE_CO1 - auto - ONE - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            AUTO_INSTANT_ID,
            "contingency-1",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(14),
            "RTE_AE5 (assessed-element-5) - RTE_CO1 - auto - TWO - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            AUTO_INSTANT_ID,
            "contingency-1",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(15),
            "RTE_AE5 (assessed-element-5) - RTE_CO1 - curative 1 - ONE - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_1_INSTANT_ID,
            "contingency-1",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(16),
            "RTE_AE5 (assessed-element-5) - RTE_CO1 - curative 1 - TWO - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_1_INSTANT_ID,
            "contingency-1",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(17),
            "RTE_AE5 (assessed-element-5) - RTE_CO1 - curative 2 - ONE",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_2_INSTANT_ID,
            "contingency-1",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(18),
            "RTE_AE5 (assessed-element-5) - RTE_CO1 - curative 2 - TWO",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_2_INSTANT_ID,
            "contingency-1",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(19),
            "RTE_AE5 (assessed-element-5) - RTE_CO1 - curative 3 - ONE",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-1",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(20),
            "RTE_AE5 (assessed-element-5) - RTE_CO1 - curative 3 - TWO",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-1",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(21),
            "RTE_AE5 (assessed-element-5) - RTE_CO1 - outage - ONE - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-1",
            +5000d,
            -5000d,
            +5000d,
            -5000d,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(22),
            "RTE_AE5 (assessed-element-5) - RTE_CO1 - outage - TWO - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-1",
            +5000d,
            -5000d,
            +5000d,
            -5000d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(23),
            "RTE_AE5 (assessed-element-5) - RTE_CO2 - auto - ONE - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            AUTO_INSTANT_ID,
            "contingency-2",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(24),
            "RTE_AE5 (assessed-element-5) - RTE_CO2 - auto - TWO - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            AUTO_INSTANT_ID,
            "contingency-2",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(25),
            "RTE_AE5 (assessed-element-5) - RTE_CO2 - curative 1 - ONE - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_1_INSTANT_ID,
            "contingency-2",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(26),
            "RTE_AE5 (assessed-element-5) - RTE_CO2 - curative 1 - TWO - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_1_INSTANT_ID,
            "contingency-2",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(27),
            "RTE_AE5 (assessed-element-5) - RTE_CO2 - curative 2 - ONE",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_2_INSTANT_ID,
            "contingency-2",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(28),
            "RTE_AE5 (assessed-element-5) - RTE_CO2 - curative 2 - TWO",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_2_INSTANT_ID,
            "contingency-2",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(29),
            "RTE_AE5 (assessed-element-5) - RTE_CO2 - curative 3 - ONE",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-2",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(30),
            "RTE_AE5 (assessed-element-5) - RTE_CO2 - curative 3 - TWO",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-2",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(31),
            "RTE_AE5 (assessed-element-5) - RTE_CO2 - outage - ONE - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-2",
            +5000d,
            -5000d,
            +5000d,
            -5000d,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(32),
            "RTE_AE5 (assessed-element-5) - RTE_CO2 - outage - TWO - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-2",
            +5000d,
            -5000d,
            +5000d,
            -5000d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(33),
            "RTE_AE5 (assessed-element-5) - preventive - ONE",
            "FFR2AA1  FFR3AA1  1",
            PREVENTIVE_INSTANT_ID,
            null,
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(34),
            "RTE_AE5 (assessed-element-5) - preventive - TWO",
            "FFR2AA1  FFR3AA1  1",
            PREVENTIVE_INSTANT_ID,
            null,
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(35),
            "RTE_AE6 (assessed-element-6) - RTE_CO1 - auto - ONE - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            AUTO_INSTANT_ID,
            "contingency-1",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(36),
            "RTE_AE6 (assessed-element-6) - RTE_CO1 - auto - TWO - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            AUTO_INSTANT_ID,
            "contingency-1",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(37),
            "RTE_AE6 (assessed-element-6) - RTE_CO1 - curative 1 - ONE - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_1_INSTANT_ID,
            "contingency-1",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(38),
            "RTE_AE6 (assessed-element-6) - RTE_CO1 - curative 1 - TWO - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_1_INSTANT_ID,
            "contingency-1",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(39),
            "RTE_AE6 (assessed-element-6) - RTE_CO1 - curative 2 - ONE",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_2_INSTANT_ID,
            "contingency-1",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(40),
            "RTE_AE6 (assessed-element-6) - RTE_CO1 - curative 2 - TWO",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_2_INSTANT_ID,
            "contingency-1",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(41),
            "RTE_AE6 (assessed-element-6) - RTE_CO1 - curative 3 - ONE",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-1",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(42),
            "RTE_AE6 (assessed-element-6) - RTE_CO1 - curative 3 - TWO",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-1",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(43),
            "RTE_AE6 (assessed-element-6) - RTE_CO1 - outage - ONE - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-1",
            +5000d,
            -5000d,
            +5000d,
            -5000d,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(44),
            "RTE_AE6 (assessed-element-6) - RTE_CO1 - outage - TWO - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-1",
            +5000d,
            -5000d,
            +5000d,
            -5000d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(45),
            "RTE_AE7 (assessed-element-7) - RTE_CO1 - auto - ONE",
            "NNL2AA1  BBE3AA1  1",
            AUTO_INSTANT_ID,
            "contingency-1",
            +5000d,
            null,
            null,
            null,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(46),
            "RTE_AE7 (assessed-element-7) - RTE_CO1 - curative 1 - ONE",
            "NNL2AA1  BBE3AA1  1",
            CURATIVE_1_INSTANT_ID,
            "contingency-1",
            +5000d,
            null,
            null,
            null,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(47),
            "RTE_AE7 (assessed-element-7) - RTE_CO1 - curative 2 - ONE",
            "NNL2AA1  BBE3AA1  1",
            CURATIVE_2_INSTANT_ID,
            "contingency-1",
            +5000d,
            null,
            null,
            null,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(48),
            "RTE_AE7 (assessed-element-7) - RTE_CO1 - curative 3 - ONE",
            "NNL2AA1  BBE3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-1",
            +5000d,
            null,
            null,
            null,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(49),
            "RTE_AE7 (assessed-element-7) - RTE_CO1 - outage - ONE",
            "NNL2AA1  BBE3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-1",
            +5000d,
            null,
            null,
            null,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(50),
            "RTE_AE7 (assessed-element-7) - RTE_CO2 - auto - ONE",
            "NNL2AA1  BBE3AA1  1",
            AUTO_INSTANT_ID,
            "contingency-2",
            +5000d,
            null,
            null,
            null,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(51),
            "RTE_AE7 (assessed-element-7) - RTE_CO2 - curative 1 - ONE",
            "NNL2AA1  BBE3AA1  1",
            CURATIVE_1_INSTANT_ID,
            "contingency-2",
            +5000d,
            null,
            null,
            null,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(52),
            "RTE_AE7 (assessed-element-7) - RTE_CO2 - curative 2 - ONE",
            "NNL2AA1  BBE3AA1  1",
            CURATIVE_2_INSTANT_ID,
            "contingency-2",
            +5000d,
            null,
            null,
            null,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(53),
            "RTE_AE7 (assessed-element-7) - RTE_CO2 - curative 3 - ONE",
            "NNL2AA1  BBE3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-2",
            +5000d,
            null,
            null,
            null,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(54),
            "RTE_AE7 (assessed-element-7) - RTE_CO2 - outage - ONE",
            "NNL2AA1  BBE3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-2",
            +5000d,
            null,
            null,
            null,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        assertEquals(8, cracCreationContext.getCnecCreationContexts().stream().filter(context -> !context.isImported()).toList().size());

        assertCnecNotImported(cracCreationContext, "assessed-element-8", ImportStatus.NOT_FOR_RAO, "AssessedElement assessed-element-8 ignored because it is not enabled");
        assertCnecNotImported(cracCreationContext, "assessed-element-9", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "AssessedElement assessed-element-9 ignored because the following network element is missing from the network: unknown-equipment");
        assertCnecNotImported(cracCreationContext, "assessed-element-10", ImportStatus.INCOMPLETE_DATA, "AssessedElement assessed-element-10 ignored because no ConductingEquipment or OperationalLimit was provided");
        assertCnecNotImported(cracCreationContext, "assessed-element-11", ImportStatus.INCONSISTENCY_IN_DATA, "The contingency unknown-contingency linked to the assessed element does not exist in the CRAC");
        assertCnecNotImported(cracCreationContext, "assessed-element-11", ImportStatus.NOT_FOR_RAO, "The link between contingency contingency-2 and the assessed element is disabled");
        assertCnecNotImported(cracCreationContext, "assessed-element-12", ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement assessed-element-12 ignored because the assessed element is not in base case and not combinable with contingencies, but no explicit link to a contingency was found");
        assertCnecNotImported(cracCreationContext, "assessed-element-13", ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement assessed-element-13 ignored because the assessed element is not in base case and not combinable with contingencies, but no explicit link to a contingency was found");
        assertCnecNotImported(cracCreationContext, "assessed-element-14", ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement assessed-element-14 ignored because the network element FFR1AA1 _generator is not a branch");

        assertEquals(15, cracCreationContext.getCrac().getRemedialAction("remedial-action-1").getUsageRules().size());

        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 2 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_1_INSTANT_ID), UsageMethod.FORCED, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 3 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_1_INSTANT_ID), UsageMethod.FORCED, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 2 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_2_INSTANT_ID), UsageMethod.FORCED, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 3 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_2_INSTANT_ID), UsageMethod.FORCED, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 3 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_3_INSTANT_ID), UsageMethod.FORCED, FlowCnec.class);

        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE4 (assessed-element-4) - RTE_CO1 - curative 2 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_1_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE4 (assessed-element-4) - RTE_CO1 - curative 3 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_1_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE4 (assessed-element-4) - RTE_CO1 - curative 2 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_2_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE4 (assessed-element-4) - RTE_CO1 - curative 3 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_2_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE4 (assessed-element-4) - RTE_CO1 - curative 3 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_3_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);

        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE4 (assessed-element-4) - RTE_CO2 - curative 2 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_1_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE4 (assessed-element-4) - RTE_CO2 - curative 3 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_1_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE4 (assessed-element-4) - RTE_CO2 - curative 2 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_2_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE4 (assessed-element-4) - RTE_CO2 - curative 3 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_2_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE4 (assessed-element-4) - RTE_CO2 - curative 3 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_3_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);

        assertEquals(13, cracCreationContext.getCrac().getRemedialAction("remedial-action-2").getUsageRules().size());

        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);

        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-2", "RTE_AE4 (assessed-element-4) - RTE_CO1 - curative 2 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_1_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-2", "RTE_AE4 (assessed-element-4) - RTE_CO1 - curative 3 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_1_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-2", "RTE_AE4 (assessed-element-4) - RTE_CO1 - curative 2 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_2_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-2", "RTE_AE4 (assessed-element-4) - RTE_CO1 - curative 3 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_2_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-2", "RTE_AE4 (assessed-element-4) - RTE_CO1 - curative 3 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_3_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);

        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-2", "RTE_AE4 (assessed-element-4) - RTE_CO2 - curative 2 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_1_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-2", "RTE_AE4 (assessed-element-4) - RTE_CO2 - curative 3 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_1_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-2", "RTE_AE4 (assessed-element-4) - RTE_CO2 - curative 2 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_2_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-2", "RTE_AE4 (assessed-element-4) - RTE_CO2 - curative 3 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_2_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-2", "RTE_AE4 (assessed-element-4) - RTE_CO2 - curative 3 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_3_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
    }

    @Test
    void importFlowCnecsWithFlowReliabilityMargin() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/profiles/cnecs/FlowCNECsWithFlowReliabilityMargin.zip", NETWORK);

        List<FlowCnec> importedFlowCnecs = cracCreationContext.getCrac().getFlowCnecs().stream().sorted(Comparator.comparing(FlowCnec::getId)).toList();
        assertEquals(17, importedFlowCnecs.size());

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(0),
            "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 2 - TWO",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_2_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +2250d,
            -2250d,
            Set.of(TwoSides.TWO),
            "RTE",
            "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(1),
            "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 3 - TWO",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +2250d,
            -2250d,
            Set.of(TwoSides.TWO),
            "RTE",
            "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(2),
            "RTE_AE1 (assessed-element-1) - preventive - TWO",
            "FFR2AA1  FFR3AA1  1",
            PREVENTIVE_INSTANT_ID,
            null,
            null,
            null,
            +2250d,
            -2250d,
            Set.of(TwoSides.TWO),
            "RTE",
            "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(3),
            "RTE_AE2 (assessed-element-2) - RTE_CO2 - auto - TWO - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            AUTO_INSTANT_ID,
            "contingency-2",
            null,
            null,
            +3400d,
            -3400d,
            Set.of(TwoSides.TWO),
            "RTE",
            "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(4),
            "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 1 - TWO - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_1_INSTANT_ID,
            "contingency-2",
            null,
            null,
            +3400d,
            -3400d,
            Set.of(TwoSides.TWO),
            "RTE",
            "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(5),
            "RTE_AE3 (assessed-element-3) - RTE_CO3 - auto - ONE - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            AUTO_INSTANT_ID,
            "contingency-3",
            +3200d,
            -3200d,
            +3200d,
            -3200d,
            Set.of(TwoSides.ONE),
            "RTE",
            "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(6),
            "RTE_AE3 (assessed-element-3) - RTE_CO3 - auto - TWO - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            AUTO_INSTANT_ID,
            "contingency-3",
            +3200d,
            -3200d,
            +3200d,
            -3200d,
            Set.of(TwoSides.TWO),
            "RTE",
            "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(7),
            "RTE_AE3 (assessed-element-3) - RTE_CO3 - curative 1 - ONE - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_1_INSTANT_ID,
            "contingency-3",
            +3200d,
            -3200d,
            +3200d,
            -3200d,
            Set.of(TwoSides.ONE),
            "RTE",
            "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(8),
            "RTE_AE3 (assessed-element-3) - RTE_CO3 - curative 1 - TWO - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_1_INSTANT_ID,
            "contingency-3",
            +3200d,
            -3200d,
            +3200d,
            -3200d,
            Set.of(TwoSides.TWO),
            "RTE",
            "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(9),
            "RTE_AE3 (assessed-element-3) - RTE_CO3 - curative 2 - ONE",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_2_INSTANT_ID,
            "contingency-3",
            +2000d,
            -2000d,
            +2000d,
            -2000d,
            Set.of(TwoSides.ONE),
            "RTE",
            "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(10),
            "RTE_AE3 (assessed-element-3) - RTE_CO3 - curative 2 - TWO",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_2_INSTANT_ID,
            "contingency-3",
            +2000d,
            -2000d,
            +2000d,
            -2000d,
            Set.of(TwoSides.TWO),
            "RTE",
            "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(11),
            "RTE_AE3 (assessed-element-3) - RTE_CO3 - curative 3 - ONE",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-3",
            +2000d,
            -2000d,
            +2000d,
            -2000d,
            Set.of(TwoSides.ONE),
            "RTE",
            "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(12),
            "RTE_AE3 (assessed-element-3) - RTE_CO3 - curative 3 - TWO",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-3",
            +2000d,
            -2000d,
            +2000d,
            -2000d,
            Set.of(TwoSides.TWO),
            "RTE",
            "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(13),
            "RTE_AE3 (assessed-element-3) - RTE_CO3 - outage - ONE - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-3",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(TwoSides.ONE),
            "RTE",
            "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(14),
            "RTE_AE3 (assessed-element-3) - RTE_CO3 - outage - TWO - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-3",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(TwoSides.TWO),
            "RTE",
            "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(15),
            "RTE_AE3 (assessed-element-3) - preventive - ONE",
            "FFR2AA1  FFR3AA1  1",
            PREVENTIVE_INSTANT_ID,
            null,
            +2000d,
            -2000d,
            +2000d,
            -2000d,
            Set.of(TwoSides.ONE),
            "RTE",
            "ES-FR");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(16),
            "RTE_AE3 (assessed-element-3) - preventive - TWO",
            "FFR2AA1  FFR3AA1  1",
            PREVENTIVE_INSTANT_ID,
            null,
            +2000d,
            -2000d,
            +2000d,
            -2000d,
            Set.of(TwoSides.TWO),
            "RTE",
            "ES-FR");
    }

    @Test
    void testCnecsWithBorders() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/profiles/cnecs/AssessedElementBorders.zip", NETWORK);
        assertEquals("ES-FR", cracCreationContext.getCrac().getFlowCnec("RTE_AE1 (assessed-element-1) - preventive - ONE").getBorder());
        assertEquals("ES-PT", cracCreationContext.getCrac().getFlowCnec("REN_AE2 (assessed-element-2) - preventive - ONE").getBorder());
        assertNull(cracCreationContext.getCrac().getFlowCnec("REE_AE3 (assessed-element-3) - preventive - ONE").getBorder());
        assertEquals("ES-FR", cracCreationContext.getCrac().getFlowCnec("REE_AE4 (assessed-element-4) - preventive - ONE").getBorder());
        assertEquals("ES-PT", cracCreationContext.getCrac().getFlowCnec("REE_AE5 (assessed-element-5) - preventive - ONE").getBorder());
    }
}
