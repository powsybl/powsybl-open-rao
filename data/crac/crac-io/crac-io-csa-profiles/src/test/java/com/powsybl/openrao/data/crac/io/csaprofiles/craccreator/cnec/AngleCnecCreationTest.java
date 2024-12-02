package com.powsybl.openrao.data.crac.io.csaprofiles.craccreator.cnec;

import com.powsybl.openrao.data.crac.io.csaprofiles.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.crac.io.csaprofiles.craccreator.CsaProfileCracCreationTestUtil;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AngleCnecCreationTest {

    @Test
    void importAngleCnecs() {
        CsaProfileCracCreationContext cracCreationContext = CsaProfileCracCreationTestUtil.getCsaCracCreationContext("/profiles/cnecs/AngleCNECs.zip", CsaProfileCracCreationTestUtil.NETWORK);

        List<AngleCnec> importedFlowCnecs = cracCreationContext.getCrac().getAngleCnecs().stream().sorted(Comparator.comparing(AngleCnec::getId)).toList();
        assertEquals(10, importedFlowCnecs.size());

        CsaProfileCracCreationTestUtil.assertAngleCnecEquality(
            importedFlowCnecs.get(0),
            "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 3",
            "BBE1AA1 ",
            "BBE4AA1 ",
            CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID,
            "contingency-1",
            60d,
            -60d,
            "RTE",
            "ES-FR");

        CsaProfileCracCreationTestUtil.assertAngleCnecEquality(
            importedFlowCnecs.get(1),
            "RTE_AE1 (assessed-element-1) - preventive",
            "BBE1AA1 ",
            "BBE4AA1 ",
            CsaProfileCracCreationTestUtil.PREVENTIVE_INSTANT_ID,
            null,
            60d,
            -60d,
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertAngleCnecEquality(
            importedFlowCnecs.get(2),
            "RTE_AE2 (assessed-element-2) - RTE_CO1 - curative 3",
            "BBE1AA1 ",
            "BBE4AA1 ",
            CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID,
            "contingency-1",
            45d,
            -45d,
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertAngleCnecEquality(
            importedFlowCnecs.get(3),
            "RTE_AE2 (assessed-element-2) - RTE_CO2 - curative 3",
            "BBE1AA1 ",
            "BBE4AA1 ",
            CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID,
            "contingency-2",
            45d,
            -45d,
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertAngleCnecEquality(
            importedFlowCnecs.get(4),
            "RTE_AE2 (assessed-element-2) - preventive",
            "BBE1AA1 ",
            "BBE4AA1 ",
            CsaProfileCracCreationTestUtil.PREVENTIVE_INSTANT_ID,
            null,
            45d,
            -45d,
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertAngleCnecEquality(
            importedFlowCnecs.get(5),
            "RTE_AE3 (assessed-element-3) - RTE_CO1 - curative 3",
            "BBE4AA1 ",
            "BBE1AA1 ",
            CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID,
            "contingency-1",
            45d,
            -45d,
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertAngleCnecEquality(
            importedFlowCnecs.get(6),
            "RTE_AE3 (assessed-element-3) - RTE_CO2 - curative 3",
            "BBE4AA1 ",
            "BBE1AA1 ",
            CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID,
            "contingency-2",
            45d,
            -45d,
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertAngleCnecEquality(
            importedFlowCnecs.get(7),
            "RTE_AE3 (assessed-element-3) - preventive",
            "BBE4AA1 ",
            "BBE1AA1 ",
            CsaProfileCracCreationTestUtil.PREVENTIVE_INSTANT_ID,
            null,
            45d,
            -45d,
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertAngleCnecEquality(
            importedFlowCnecs.get(8),
            "RTE_AE4 (assessed-element-4) - RTE_CO2 - curative 3",
            "BBE1AA1 ",
            "BBE4AA1 ",
            CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID,
            "contingency-2",
            60d,
            null,
            "RTE", "ES-FR");

        CsaProfileCracCreationTestUtil.assertAngleCnecEquality(
            importedFlowCnecs.get(9),
            "RTE_AE5 (assessed-element-5) - preventive",
            "BBE1AA1 ",
            "BBE4AA1 ",
            CsaProfileCracCreationTestUtil.PREVENTIVE_INSTANT_ID,
            null,
            null,
            -30d,
            "RTE", "ES-FR");

        assertEquals(8, cracCreationContext.getCnecCreationContexts().stream().filter(context -> !context.isImported()).toList().size());

        CsaProfileCracCreationTestUtil.assertCnecNotImported(cracCreationContext, "assessed-element-6", ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement assessed-element-6 ignored because AngleCNEC's importing and exporting equipments are the same: BBE4AA1 ");
        CsaProfileCracCreationTestUtil.assertCnecNotImported(cracCreationContext, "assessed-element-7", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "AssessedElement assessed-element-7 ignored because the angle limit equipment unknown-terminal is missing in network");
        CsaProfileCracCreationTestUtil.assertCnecNotImported(cracCreationContext, "assessed-element-8", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "AssessedElement assessed-element-8 ignored because the angle limit equipment unknown-terminal is missing in network");
        CsaProfileCracCreationTestUtil.assertCnecNotImported(cracCreationContext, "assessed-element-9", ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement assessed-element-9 ignored because the angle limit's normal value is negative");
        CsaProfileCracCreationTestUtil.assertCnecNotImported(cracCreationContext, "assessed-element-10", ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement assessed-element-10 ignored because of an ambiguous angle limit direction definition from an undefined VoltageAngleLimit.isFlowToRefTerminal and an OperationalLimit.OperationalLimitType: http://iec.ch/TC57/CIM100#OperationalLimitDirectionKind.low");
        CsaProfileCracCreationTestUtil.assertCnecNotImported(cracCreationContext, "assessed-element-11", ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement assessed-element-11 ignored because of an ambiguous angle limit direction definition from an undefined VoltageAngleLimit.isFlowToRefTerminal and an OperationalLimit.OperationalLimitType: http://iec.ch/TC57/CIM100#OperationalLimitDirectionKind.high");
        CsaProfileCracCreationTestUtil.assertCnecNotImported(cracCreationContext, "assessed-element-12", ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement assessed-element-12 ignored because the network element FFR1AA1 _generator is not a bus bar section");
        CsaProfileCracCreationTestUtil.assertCnecNotImported(cracCreationContext, "assessed-element-13", ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement assessed-element-13 ignored because the network element FFR2AA1 _generator is not a bus bar section");

        CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative 3", cracCreationContext.getCrac().getInstant(CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID), UsageMethod.FORCED, AngleCnec.class);
    }
}
