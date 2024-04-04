package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec;

import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.AUTO_INSTANT_ID;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.NETWORK;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.OUTAGE_INSTANT_ID;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.PREVENTIVE_INSTANT_ID;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertCnecNotImported;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertHasOnFlowConstraintUsageRule;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static org.junit.jupiter.api.Assertions.*;

class FlowCnecCreationTest {

    @Test
    void importFlowCnecs() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/profiles/cnecs/FlowCNECs.zip", NETWORK);

        List<FlowCnec> importedFlowCnecs = cracCreationContext.getCrac().getFlowCnecs().stream().sorted(Comparator.comparing(FlowCnec::getId)).toList();
        assertEquals(20, importedFlowCnecs.size());

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(0),
            "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 3",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(Side.RIGHT),
            "RTE");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(1),
            "RTE_AE1 (assessed-element-1) - preventive",
            "FFR2AA1  FFR3AA1  1",
            PREVENTIVE_INSTANT_ID,
            null,
            null,
            null,
            +2500d,
            -2500d,
            Set.of(Side.RIGHT),
            "RTE");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(3),
            "RTE_AE2 (assessed-element-2) - RTE_CO1 - outage - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +5000d,
            -5000d,
            Set.of(Side.RIGHT),
            "RTE");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(4),
            "RTE_AE2 (assessed-element-2) - RTE_CO2 - outage - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-2",
            null,
            null,
            +5000d,
            -5000d,
            Set.of(Side.RIGHT),
            "RTE");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(5),
            "RTE_AE3 (assessed-element-3) - RTE_CO2 - auto - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            AUTO_INSTANT_ID,
            "contingency-2",
            null,
            null,
            +4000d,
            -4000d,
            Set.of(Side.RIGHT),
            "RTE");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(6),
            "RTE_AE4 (assessed-element-4) - RTE_CO1 - curative 3",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(Side.RIGHT),
            "RTE");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(7),
            "RTE_AE4 (assessed-element-4) - RTE_CO2 - curative 3",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-2",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(Side.RIGHT),
            "RTE");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(8),
            "RTE_AE5 (assessed-element-5) - RTE_CO1 - auto - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            AUTO_INSTANT_ID,
            "contingency-1",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(Side.LEFT, Side.RIGHT),
            "RTE");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(9),
            "RTE_AE5 (assessed-element-5) - RTE_CO1 - curative 3",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-1",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(Side.LEFT, Side.RIGHT),
            "RTE");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(10),
            "RTE_AE5 (assessed-element-5) - RTE_CO1 - outage - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-1",
            +5000d,
            -5000d,
            +5000d,
            -5000d,
            Set.of(Side.LEFT, Side.RIGHT),
            "RTE");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(11),
            "RTE_AE5 (assessed-element-5) - RTE_CO2 - auto - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            AUTO_INSTANT_ID,
            "contingency-2",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(Side.LEFT, Side.RIGHT),
            "RTE");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(12),
            "RTE_AE5 (assessed-element-5) - RTE_CO2 - curative 3",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-2",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(Side.LEFT, Side.RIGHT),
            "RTE");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(13),
            "RTE_AE5 (assessed-element-5) - RTE_CO2 - outage - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-2",
            +5000d,
            -5000d,
            +5000d,
            -5000d,
            Set.of(Side.LEFT, Side.RIGHT),
            "RTE");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(14),
            "RTE_AE5 (assessed-element-5) - preventive",
            "FFR2AA1  FFR3AA1  1",
            PREVENTIVE_INSTANT_ID,
            null,
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(Side.LEFT, Side.RIGHT),
            "RTE");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(15),
            "RTE_AE6 (assessed-element-6) - RTE_CO1 - auto - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            AUTO_INSTANT_ID,
            "contingency-1",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(Side.LEFT, Side.RIGHT),
            "RTE");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(16),
            "RTE_AE6 (assessed-element-6) - RTE_CO1 - curative 3",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-1",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(Side.LEFT, Side.RIGHT),
            "RTE");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(17),
            "RTE_AE6 (assessed-element-6) - RTE_CO1 - outage - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-1",
            +5000d,
            -5000d,
            +5000d,
            -5000d,
            Set.of(Side.LEFT, Side.RIGHT),
            "RTE");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(18),
            "RTE_AE7 (assessed-element-7) - RTE_CO1 - curative 3",
            "NNL2AA1  BBE3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-1",
            +5000d,
            null,
            null,
            null,
            Set.of(Side.LEFT),
            "RTE");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(19),
            "RTE_AE7 (assessed-element-7) - RTE_CO2 - curative 3",
            "NNL2AA1  BBE3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-2",
            +5000d,
            null,
            null,
            null,
            Set.of(Side.LEFT),
            "RTE");

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(2),
            "RTE_AE11 (assessed-element-11) - RTE_CO1 - curative 3",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(Side.RIGHT),
            "RTE");

        assertEquals(8, cracCreationContext.getCnecCreationContexts().stream().filter(context -> !context.isImported()).toList().size());

        assertCnecNotImported(cracCreationContext, "assessed-element-8", ImportStatus.NOT_FOR_RAO, "AssessedElement.normalEnabled is false");
        assertCnecNotImported(cracCreationContext, "assessed-element-9", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "AssessedElement assessed-element-9 ignored because the following element is missing from the network: unknown-equipment");
        assertCnecNotImported(cracCreationContext, "assessed-element-10", ImportStatus.INCOMPLETE_DATA, "AssessedElement assessed-element-10 ignored because no ConductingEquipment or OperationalLimit was provided");
        assertCnecNotImported(cracCreationContext, "assessed-element-11", ImportStatus.INCONSISTENCY_IN_DATA, "the contingency unknown-contingency linked to the assessed element doesn't exist in the CRAC");
        assertCnecNotImported(cracCreationContext, "assessed-element-11", ImportStatus.NOT_FOR_RAO, "AssessedElementWithContingency.normalEnabled is false for contingency contingency-2");
        assertCnecNotImported(cracCreationContext, "assessed-element-12", ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement assessed-element-12 ignored because the assessed element is not in base case and not combinable with contingencies, but no explicit link to a contingency was found");
        assertCnecNotImported(cracCreationContext, "assessed-element-13", ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement assessed-element-13 ignored because the assessed element is not in base case and not combinable with contingencies, but no explicit link to a contingency was found");
        assertCnecNotImported(cracCreationContext, "assessed-element-14", ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement assessed-element-14 ignored because the network element FFR1AA1 _generator is not a branch");

        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 3", cracCreationContext.getCrac().getInstant(CURATIVE_3_INSTANT_ID), UsageMethod.AVAILABLE);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-2", "RTE_AE4 (assessed-element-4) - RTE_CO1 - curative 3", cracCreationContext.getCrac().getInstant(CURATIVE_3_INSTANT_ID), UsageMethod.AVAILABLE);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-2", "RTE_AE4 (assessed-element-4) - RTE_CO2 - curative 3", cracCreationContext.getCrac().getInstant(CURATIVE_3_INSTANT_ID), UsageMethod.AVAILABLE);
    }
}
