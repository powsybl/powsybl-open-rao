package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.cnec;

import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.NETWORK;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.PREVENTIVE_INSTANT_ID;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.assertCnecNotImported;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VoltageCnecCreationTest {

    @Test
    void importVoltageCnecs() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/profiles/cnecs/VoltageCNECs.zip", NETWORK);

        List<VoltageCnec> importedVoltageCnecs = cracCreationContext.getCrac().getVoltageCnecs().stream().sorted(Comparator.comparing(VoltageCnec::getId)).toList();
        assertEquals(5, importedVoltageCnecs.size());

        CsaProfileCracCreationTestUtil.assertVoltageCnecEquality(
            importedVoltageCnecs.get(0),
            "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 3",
            "BBE1AA1 ",
            CURATIVE_3_INSTANT_ID,
            "contingency-1",
            135d,
            null,
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertVoltageCnecEquality(
            importedVoltageCnecs.get(1),
            "RTE_AE2 (assessed-element-2) - RTE_CO1 - curative 3",
            "BBE1AA1 ",
            CURATIVE_3_INSTANT_ID,
            "contingency-1",
            null,
            -72d,
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertVoltageCnecEquality(
            importedVoltageCnecs.get(2),
            "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 3",
            "BBE1AA1 ",
            CURATIVE_3_INSTANT_ID,
            "contingency-2",
            null,
            -72d,
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertVoltageCnecEquality(
            importedVoltageCnecs.get(3),
            "RTE_AE2 (assessed-element-2) - preventive",
            "BBE1AA1 ",
            PREVENTIVE_INSTANT_ID,
            null,
            null,
            -72d,
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertVoltageCnecEquality(
            importedVoltageCnecs.get(4),
            "RTE_AE7 (assessed-element-7) - preventive",
            "BBE1AA1 ",
            PREVENTIVE_INSTANT_ID,
            null,
            100d,
            null,
            "RTE", "ES-FR");

        assertEquals(5, cracCreationContext.getCnecCreationContexts().stream().filter(context -> !context.isImported()).toList().size());

        assertCnecNotImported(cracCreationContext, "assessed-element-3", ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement assessed-element-3 ignored because a voltage limit can only be of kind highVoltage or lowVoltage");
        assertCnecNotImported(cracCreationContext, "assessed-element-4", ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO, "AssessedElement assessed-element-4 ignored because only permanent voltage limits (with infinite duration) are currently handled");
        assertCnecNotImported(cracCreationContext, "assessed-element-5", ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement assessed-element-5 ignored because the network element FFR1AA1 _generator is not a bus bar section");
        assertCnecNotImported(cracCreationContext, "assessed-element-6", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "AssessedElement assessed-element-6 ignored because the voltage limit equipment unknown-equipment is missing in network");
        assertCnecNotImported(cracCreationContext, "assessed-element-8", ImportStatus.INCOMPLETE_DATA, "AssessedElement assessed-element-8 ignored because no ConductingEquipment or OperationalLimit was provided");

        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 3", cracCreationContext.getCrac().getInstant(CURATIVE_3_INSTANT_ID), UsageMethod.AVAILABLE, VoltageCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE2 (assessed-element-2) - RTE_CO1 - curative 3", cracCreationContext.getCrac().getInstant(CURATIVE_3_INSTANT_ID), UsageMethod.FORCED, VoltageCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 3", cracCreationContext.getCrac().getInstant(CURATIVE_3_INSTANT_ID), UsageMethod.FORCED, VoltageCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-2", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 3", cracCreationContext.getCrac().getInstant(CURATIVE_3_INSTANT_ID), UsageMethod.AVAILABLE, VoltageCnec.class);
    }
}
