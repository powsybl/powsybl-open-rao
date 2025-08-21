/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.cnec;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationContext;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationTestUtil;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FlowCnecCreationTest {

    @Test
    void importFlowCnecs() {
        NcCracCreationContext cracCreationContext = NcCracCreationTestUtil.getNcCracCreationContext("/profiles/cnecs/FlowCNECs.zip", NcCracCreationTestUtil.NETWORK);

        List<FlowCnec> importedFlowCnecs = cracCreationContext.getCrac().getFlowCnecs().stream().sorted(Comparator.comparing(FlowCnec::getId)).toList();
        assertEquals(39, importedFlowCnecs.size());

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(0),
            "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 2 - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(1),
            "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 3 - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(2),
            "RTE_AE1 (assessed-element-1) - preventive - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.PREVENTIVE_INSTANT_ID,
            null,
            null,
            null,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(3),
            "RTE_AE11 (assessed-element-11) - RTE_CO1 - curative 2 - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(4),
            "RTE_AE11 (assessed-element-11) - RTE_CO1 - curative 3 - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(5),
            "RTE_AE2 (assessed-element-2) - RTE_CO1 - outage - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.OUTAGE_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +5000d,
            -5000d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(6),
            "RTE_AE2 (assessed-element-2) - RTE_CO2 - outage - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.OUTAGE_INSTANT_ID,
            "contingency-2",
            null,
            null,
            +5000d,
            -5000d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(7),
            "RTE_AE3 (assessed-element-3) - RTE_CO2 - auto - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.AUTO_INSTANT_ID,
            "contingency-2",
            null,
            null,
            +4000d,
            -4000d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(8),
            "RTE_AE3 (assessed-element-3) - RTE_CO2 - curative 1 - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_1_INSTANT_ID,
            "contingency-2",
            null,
            null,
            +4000d,
            -4000d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(9),
            "RTE_AE4 (assessed-element-4) - RTE_CO1 - curative 2 - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(10),
            "RTE_AE4 (assessed-element-4) - RTE_CO1 - curative 3 - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(11),
            "RTE_AE4 (assessed-element-4) - RTE_CO2 - curative 2 - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID,
            "contingency-2",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(12),
            "RTE_AE4 (assessed-element-4) - RTE_CO2 - curative 3 - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID,
            "contingency-2",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(13),
            "RTE_AE5 (assessed-element-5) - RTE_CO1 - auto - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.AUTO_INSTANT_ID,
            "contingency-1",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(14),
            "RTE_AE5 (assessed-element-5) - RTE_CO1 - curative 1 - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_1_INSTANT_ID,
            "contingency-1",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(15),
            "RTE_AE5 (assessed-element-5) - RTE_CO1 - curative 2 - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID,
            "contingency-1",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(16),
            "RTE_AE5 (assessed-element-5) - RTE_CO1 - curative 3 - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID,
            "contingency-1",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(17),
            "RTE_AE5 (assessed-element-5) - RTE_CO1 - outage - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.OUTAGE_INSTANT_ID,
            "contingency-1",
            +5000d,
            -5000d,
            +5000d,
            -5000d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(18),
            "RTE_AE5 (assessed-element-5) - RTE_CO2 - auto - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.AUTO_INSTANT_ID,
            "contingency-2",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(19),
            "RTE_AE5 (assessed-element-5) - RTE_CO2 - curative 1 - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_1_INSTANT_ID,
            "contingency-2",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(20),
            "RTE_AE5 (assessed-element-5) - RTE_CO2 - curative 2 - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID,
            "contingency-2",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(21),
            "RTE_AE5 (assessed-element-5) - RTE_CO2 - curative 3 - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID,
            "contingency-2",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(22),
            "RTE_AE5 (assessed-element-5) - RTE_CO2 - outage - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.OUTAGE_INSTANT_ID,
            "contingency-2",
            +5000d,
            -5000d,
            +5000d,
            -5000d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(23),
            "RTE_AE5 (assessed-element-5) - preventive - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.PREVENTIVE_INSTANT_ID,
            null,
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(24),
            "RTE_AE6 (assessed-element-6) - RTE_CO1 - auto - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.AUTO_INSTANT_ID,
            "contingency-1",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(25),
            "RTE_AE6 (assessed-element-6) - RTE_CO1 - curative 1 - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_1_INSTANT_ID,
            "contingency-1",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(26),
            "RTE_AE6 (assessed-element-6) - RTE_CO1 - curative 2 - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID,
            "contingency-1",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(27),
            "RTE_AE6 (assessed-element-6) - RTE_CO1 - curative 3 - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID,
            "contingency-1",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(28),
            "RTE_AE6 (assessed-element-6) - RTE_CO1 - outage - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.OUTAGE_INSTANT_ID,
            "contingency-1",
            +5000d,
            -5000d,
            +5000d,
            -5000d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(29),
            "RTE_AE7 (assessed-element-7) - RTE_CO1 - auto - PATL",
            "NNL2AA1  BBE3AA1  1",
            NcCracCreationTestUtil.AUTO_INSTANT_ID,
            "contingency-1",
            +5000d,
            null,
            null,
            null,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(30),
            "RTE_AE7 (assessed-element-7) - RTE_CO1 - curative 1 - PATL",
            "NNL2AA1  BBE3AA1  1",
            NcCracCreationTestUtil.CURATIVE_1_INSTANT_ID,
            "contingency-1",
            +5000d,
            null,
            null,
            null,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(31),
            "RTE_AE7 (assessed-element-7) - RTE_CO1 - curative 2 - PATL",
            "NNL2AA1  BBE3AA1  1",
            NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID,
            "contingency-1",
            +5000d,
            null,
            null,
            null,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(32),
            "RTE_AE7 (assessed-element-7) - RTE_CO1 - curative 3 - PATL",
            "NNL2AA1  BBE3AA1  1",
            NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID,
            "contingency-1",
            +5000d,
            null,
            null,
            null,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(33),
            "RTE_AE7 (assessed-element-7) - RTE_CO1 - outage - PATL",
            "NNL2AA1  BBE3AA1  1",
            NcCracCreationTestUtil.OUTAGE_INSTANT_ID,
            "contingency-1",
            +5000d,
            null,
            null,
            null,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(34),
            "RTE_AE7 (assessed-element-7) - RTE_CO2 - auto - PATL",
            "NNL2AA1  BBE3AA1  1",
            NcCracCreationTestUtil.AUTO_INSTANT_ID,
            "contingency-2",
            +5000d,
            null,
            null,
            null,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(35),
            "RTE_AE7 (assessed-element-7) - RTE_CO2 - curative 1 - PATL",
            "NNL2AA1  BBE3AA1  1",
            NcCracCreationTestUtil.CURATIVE_1_INSTANT_ID,
            "contingency-2",
            +5000d,
            null,
            null,
            null,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(36),
            "RTE_AE7 (assessed-element-7) - RTE_CO2 - curative 2 - PATL",
            "NNL2AA1  BBE3AA1  1",
            NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID,
            "contingency-2",
            +5000d,
            null,
            null,
            null,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(37),
            "RTE_AE7 (assessed-element-7) - RTE_CO2 - curative 3 - PATL",
            "NNL2AA1  BBE3AA1  1",
            NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID,
            "contingency-2",
            +5000d,
            null,
            null,
            null,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(38),
            "RTE_AE7 (assessed-element-7) - RTE_CO2 - outage - PATL",
            "NNL2AA1  BBE3AA1  1",
            NcCracCreationTestUtil.OUTAGE_INSTANT_ID,
            "contingency-2",
            +5000d,
            null,
            null,
            null,
            Set.of(TwoSides.ONE),
            "RTE", "ES-FR");

        assertEquals(8, cracCreationContext.getCnecCreationContexts().stream().filter(context -> !context.isImported()).toList().size());

        NcCracCreationTestUtil.assertCnecNotImported(cracCreationContext, "assessed-element-8", ImportStatus.NOT_FOR_RAO, "AssessedElement assessed-element-8 ignored because it is not enabled");
        NcCracCreationTestUtil.assertCnecNotImported(cracCreationContext, "assessed-element-9", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "AssessedElement assessed-element-9 ignored because the following network element is missing from the network: unknown-equipment");
        NcCracCreationTestUtil.assertCnecNotImported(cracCreationContext, "assessed-element-10", ImportStatus.INCOMPLETE_DATA, "AssessedElement assessed-element-10 ignored because no ConductingEquipment or OperationalLimit was provided");
        NcCracCreationTestUtil.assertCnecNotImported(cracCreationContext, "assessed-element-11", ImportStatus.INCONSISTENCY_IN_DATA, "The contingency unknown-contingency linked to the assessed element does not exist in the CRAC");
        NcCracCreationTestUtil.assertCnecNotImported(cracCreationContext, "assessed-element-11", ImportStatus.NOT_FOR_RAO, "The link between contingency contingency-2 and the assessed element is disabled");
        NcCracCreationTestUtil.assertCnecNotImported(cracCreationContext, "assessed-element-12", ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement assessed-element-12 ignored because the assessed element is not in base case and not combinable with contingencies, but no explicit link to a contingency was found");
        NcCracCreationTestUtil.assertCnecNotImported(cracCreationContext, "assessed-element-13", ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement assessed-element-13 ignored because the assessed element is not in base case and not combinable with contingencies, but no explicit link to a contingency was found");
        NcCracCreationTestUtil.assertCnecNotImported(cracCreationContext, "assessed-element-14", ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement assessed-element-14 ignored because the network element FFR1AA1 _generator is not a branch");

        assertEquals(5, cracCreationContext.getCrac().getRemedialAction("remedial-action-1").getUsageRules().size());

        NcCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 2 - PATL", cracCreationContext.getCrac().getInstant(NcCracCreationTestUtil.CURATIVE_1_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        NcCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 3 - PATL", cracCreationContext.getCrac().getInstant(NcCracCreationTestUtil.CURATIVE_1_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        NcCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 2 - PATL", cracCreationContext.getCrac().getInstant(NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        NcCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 3 - PATL", cracCreationContext.getCrac().getInstant(NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        NcCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 3 - PATL", cracCreationContext.getCrac().getInstant(NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);

        assertEquals(3, cracCreationContext.getCrac().getRemedialAction("remedial-action-2").getUsageRules().size());

        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", NcCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);
    }

    @Test
    void importFlowCnecsWithFlowReliabilityMargin() {
        NcCracCreationContext cracCreationContext = NcCracCreationTestUtil.getNcCracCreationContext("/profiles/cnecs/FlowCNECsWithFlowReliabilityMargin.zip", NcCracCreationTestUtil.NETWORK);

        List<FlowCnec> importedFlowCnecs = cracCreationContext.getCrac().getFlowCnecs().stream().sorted(Comparator.comparing(FlowCnec::getId)).toList();
        assertEquals(11, importedFlowCnecs.size());

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(0),
            "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 2 - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +2250d,
            -2250d,
            Set.of(TwoSides.TWO),
            "RTE",
            "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(1),
            "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 3 - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +2250d,
            -2250d,
            Set.of(TwoSides.TWO),
            "RTE",
            "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(2),
            "RTE_AE1 (assessed-element-1) - preventive - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.PREVENTIVE_INSTANT_ID,
            null,
            null,
            null,
            +2250d,
            -2250d,
            Set.of(TwoSides.TWO),
            "RTE",
            "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(3),
            "RTE_AE2 (assessed-element-2) - RTE_CO2 - auto - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.AUTO_INSTANT_ID,
            "contingency-2",
            null,
            null,
            +3400d,
            -3400d,
            Set.of(TwoSides.TWO),
            "RTE",
            "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(4),
            "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 1 - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_1_INSTANT_ID,
            "contingency-2",
            null,
            null,
            +3400d,
            -3400d,
            Set.of(TwoSides.TWO),
            "RTE",
            "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(5),
            "RTE_AE3 (assessed-element-3) - RTE_CO3 - auto - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.AUTO_INSTANT_ID,
            "contingency-3",
            +3200d,
            -3200d,
            +3200d,
            -3200d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE",
            "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(6),
            "RTE_AE3 (assessed-element-3) - RTE_CO3 - curative 1 - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_1_INSTANT_ID,
            "contingency-3",
            +3200d,
            -3200d,
            +3200d,
            -3200d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE",
            "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(7),
            "RTE_AE3 (assessed-element-3) - RTE_CO3 - curative 2 - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID,
            "contingency-3",
            +2000d,
            -2000d,
            +2000d,
            -2000d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE",
            "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(8),
            "RTE_AE3 (assessed-element-3) - RTE_CO3 - curative 3 - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID,
            "contingency-3",
            +2000d,
            -2000d,
            +2000d,
            -2000d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE",
            "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(9),
            "RTE_AE3 (assessed-element-3) - RTE_CO3 - outage - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.OUTAGE_INSTANT_ID,
            "contingency-3",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE",
            "ES-FR");

        NcCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(10),
            "RTE_AE3 (assessed-element-3) - preventive - PATL",
            "FFR2AA1  FFR3AA1  1",
            NcCracCreationTestUtil.PREVENTIVE_INSTANT_ID,
            null,
            +2000d,
            -2000d,
            +2000d,
            -2000d,
            Set.of(TwoSides.ONE, TwoSides.TWO),
            "RTE",
            "ES-FR");
    }

    @Test
    void testCnecsWithBorders() {
        NcCracCreationContext cracCreationContext = NcCracCreationTestUtil.getNcCracCreationContext("/profiles/cnecs/AssessedElementBorders.zip", NcCracCreationTestUtil.NETWORK);
        assertEquals("ES-FR", cracCreationContext.getCrac().getFlowCnec("RTE_AE1 (assessed-element-1) - preventive - PATL").getBorder());
        assertEquals("ES-PT", cracCreationContext.getCrac().getFlowCnec("REN_AE2 (assessed-element-2) - preventive - PATL").getBorder());
        assertNull(cracCreationContext.getCrac().getFlowCnec("REE_AE3 (assessed-element-3) - preventive - PATL").getBorder());
        assertEquals("ES-FR", cracCreationContext.getCrac().getFlowCnec("REE_AE4 (assessed-element-4) - preventive - PATL").getBorder());
        assertEquals("ES-PT", cracCreationContext.getCrac().getFlowCnec("REE_AE5 (assessed-element-5) - preventive - PATL").getBorder());
    }
}
