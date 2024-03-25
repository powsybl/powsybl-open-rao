package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec;

import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.CURATIVE_INSTANT_ID;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.NETWORK;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.PREVENTIVE_INSTANT_ID;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertCnecNotImported;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertHasOnVoltageConstraintUsageRule;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VoltageCnecCreationTest {

    @Test
    void importVoltageCnecs() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/profiles/cnecs/VoltageCNECs.zip", NETWORK);

        List<VoltageCnec> importedVoltageCnecs = cracCreationContext.getCrac().getVoltageCnecs().stream().sorted(Comparator.comparing(VoltageCnec::getId)).toList();
        assertEquals(4, importedVoltageCnecs.size());

        CsaProfileCracCreationTestUtil.assertVoltageCnecEquality(
            importedVoltageCnecs.get(0),
            "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative",
            "BBE1AA1 ",
            CURATIVE_INSTANT_ID,
            "contingency-1",
            135d,
            null
        );

        CsaProfileCracCreationTestUtil.assertVoltageCnecEquality(
            importedVoltageCnecs.get(1),
            "RTE_AE2 (assessed-element-2) - RTE_CO1 - curative",
            "BBE1AA1 ",
            CURATIVE_INSTANT_ID,
            "contingency-1",
            null,
            -72d
        );

        CsaProfileCracCreationTestUtil.assertVoltageCnecEquality(
            importedVoltageCnecs.get(2),
            "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative",
            "BBE1AA1 ",
            CURATIVE_INSTANT_ID,
            "contingency-2",
            null,
            -72d
        );

        CsaProfileCracCreationTestUtil.assertVoltageCnecEquality(
            importedVoltageCnecs.get(3),
            "RTE_AE2 (assessed-element-2) - preventive",
            "BBE1AA1 ",
            PREVENTIVE_INSTANT_ID,
            null,
            null,
            -72d
        );

        assertEquals(6, cracCreationContext.getCnecCreationContexts().stream().filter(context -> !context.isImported()).toList().size());

        assertCnecNotImported(cracCreationContext, "assessed-element-3", ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement assessed-element-3 ignored because a voltage limit can only be of kind highVoltage or lowVoltage");
        assertCnecNotImported(cracCreationContext, "assessed-element-4", ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO, "AssessedElement assessed-element-4 ignored because only permanent voltage limits (with infinite duration) are currently handled");
        assertCnecNotImported(cracCreationContext, "assessed-element-5", ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement assessed-element-5 ignored because the network element FFR1AA1 _generator is not a bus bar section");
        assertCnecNotImported(cracCreationContext, "assessed-element-6", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "AssessedElement assessed-element-6 ignored because the voltage limit equipment unknown-equipment is missing in network");
        assertCnecNotImported(cracCreationContext, "assessed-element-7", ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO, "AssessedElement assessed-element-7 ignored because only permanent voltage limits (with infinite duration) are currently handled");
        assertCnecNotImported(cracCreationContext, "assessed-element-8", ImportStatus.INCOMPLETE_DATA, "AssessedElement assessed-element-8 ignored because no ConductingEquipment or OperationalLimit was provided");

        assertHasOnVoltageConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative", cracCreationContext.getCrac().getInstant(CURATIVE_INSTANT_ID), UsageMethod.AVAILABLE);
        assertHasOnVoltageConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE2 (assessed-element-2) - RTE_CO1 - curative", cracCreationContext.getCrac().getInstant(CURATIVE_INSTANT_ID), UsageMethod.AVAILABLE);
        assertHasOnVoltageConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative", cracCreationContext.getCrac().getInstant(CURATIVE_INSTANT_ID), UsageMethod.AVAILABLE);
        assertHasOnVoltageConstraintUsageRule(cracCreationContext, "remedial-action-2", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative", cracCreationContext.getCrac().getInstant(CURATIVE_INSTANT_ID), UsageMethod.AVAILABLE);
    }
}
