package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec;

import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.AUTO_INSTANT_ID;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.CURATIVE_INSTANT_ID;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.NETWORK;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.OUTAGE_INSTANT_ID;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.PREVENTIVE_INSTANT_ID;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertCnecNotImported;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertHasOnFlowConstraintUsageRule;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static org.junit.jupiter.api.Assertions.*;

class FlowCnecCreationTest {

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

    // TODO: add cnec pointing to 3 co (1 ok / 1 not ok / 1 disabled)

    @Test
    void importFlowCnecs() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/profiles/cnecs/FlowCNECs.zip", NETWORK);

        List<FlowCnec> importedFlowCnecs = cracCreationContext.getCrac().getFlowCnecs().stream().sorted(Comparator.comparing(FlowCnec::getId)).toList();
        assertEquals(19, importedFlowCnecs.size());

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(0),
            "RTE_AE1 (ae1) - RTE_CO1 - curative",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(Side.RIGHT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(1),
            "RTE_AE1 (ae1) - preventive",
            "FFR2AA1  FFR3AA1  1",
            PREVENTIVE_INSTANT_ID,
            null,
            null,
            null,
            +2500d,
            -2500d,
            Set.of(Side.RIGHT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(2),
            "RTE_AE2 (ae2) - RTE_CO1 - outage - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +5000d,
            -5000d,
            Set.of(Side.RIGHT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(3),
            "RTE_AE2 (ae2) - RTE_CO2 - outage - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-2",
            null,
            null,
            +5000d,
            -5000d,
            Set.of(Side.RIGHT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(4),
            "RTE_AE3 (ae3) - RTE_CO2 - auto - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            AUTO_INSTANT_ID,
            "contingency-2",
            null,
            null,
            +4000d,
            -4000d,
            Set.of(Side.RIGHT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(5),
            "RTE_AE4 (ae4) - RTE_CO1 - curative",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_INSTANT_ID,
            "contingency-1",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(Side.RIGHT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(6),
            "RTE_AE4 (ae4) - RTE_CO2 - curative",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_INSTANT_ID,
            "contingency-2",
            null,
            null,
            +2500d,
            -2500d,
            Set.of(Side.RIGHT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(7),
            "RTE_AE5 (ae5) - RTE_CO1 - auto - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            AUTO_INSTANT_ID,
            "contingency-1",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(Side.LEFT, Side.RIGHT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(8),
            "RTE_AE5 (ae5) - RTE_CO1 - curative",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_INSTANT_ID,
            "contingency-1",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(Side.LEFT, Side.RIGHT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(9),
            "RTE_AE5 (ae5) - RTE_CO1 - outage - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-1",
            +5000d,
            -5000d,
            +5000d,
            -5000d,
            Set.of(Side.LEFT, Side.RIGHT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(10),
            "RTE_AE5 (ae5) - RTE_CO2 - auto - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            AUTO_INSTANT_ID,
            "contingency-2",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(Side.LEFT, Side.RIGHT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(11),
            "RTE_AE5 (ae5) - RTE_CO2 - curative",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_INSTANT_ID,
            "contingency-2",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(Side.LEFT, Side.RIGHT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(12),
            "RTE_AE5 (ae5) - RTE_CO2 - outage - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-2",
            +5000d,
            -5000d,
            +5000d,
            -5000d,
            Set.of(Side.LEFT, Side.RIGHT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(13),
            "RTE_AE5 (ae5) - preventive",
            "FFR2AA1  FFR3AA1  1",
            PREVENTIVE_INSTANT_ID,
            null,
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(Side.LEFT, Side.RIGHT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(14),
            "RTE_AE6 (ae6) - RTE_CO1 - auto - TATL 900",
            "FFR2AA1  FFR3AA1  1",
            AUTO_INSTANT_ID,
            "contingency-1",
            +4000d,
            -4000d,
            +4000d,
            -4000d,
            Set.of(Side.LEFT, Side.RIGHT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(15),
            "RTE_AE6 (ae6) - RTE_CO1 - curative",
            "FFR2AA1  FFR3AA1  1",
            CURATIVE_INSTANT_ID,
            "contingency-1",
            +2500d,
            -2500d,
            +2500d,
            -2500d,
            Set.of(Side.LEFT, Side.RIGHT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(16),
            "RTE_AE6 (ae6) - RTE_CO1 - outage - TATL 60",
            "FFR2AA1  FFR3AA1  1",
            OUTAGE_INSTANT_ID,
            "contingency-1",
            +5000d,
            -5000d,
            +5000d,
            -5000d,
            Set.of(Side.LEFT, Side.RIGHT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(17),
            "RTE_AE7 (ae7) - RTE_CO1 - curative",
            "NNL2AA1  BBE3AA1  1",
            CURATIVE_INSTANT_ID,
            "contingency-1",
            +5000d,
            null,
            null,
            null,
            Set.of(Side.LEFT)
        );

        CsaProfileCracCreationTestUtil.assertFlowCnecEquality(
            importedFlowCnecs.get(18),
            "RTE_AE7 (ae7) - RTE_CO2 - curative",
            "NNL2AA1  BBE3AA1  1",
            CURATIVE_INSTANT_ID,
            "contingency-2",
            +5000d,
            null,
            null,
            null,
            Set.of(Side.LEFT)
        );

        assertEquals(6, cracCreationContext.getCnecCreationContexts().stream().filter(context -> !context.isImported()).toList().size());

        assertCnecNotImported(cracCreationContext, "ae8", ImportStatus.NOT_FOR_RAO, "AssessedElement.normalEnabled is false");
        assertCnecNotImported(cracCreationContext, "ae9", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "AssessedElement ae9 ignored because the following element is missing from the network: unknown-equipment");
        assertCnecNotImported(cracCreationContext, "ae10", ImportStatus.INCOMPLETE_DATA, "AssessedElement ae10 ignored because no ConductingEquipment or OperationalLimit was provided");
        // TODO: should not happen
        assertCnecNotImported(cracCreationContext, "ae11", ImportStatus.INCONSISTENCY_IN_DATA, "the contingency unknown-contingency linked to the assessed element doesn't exist in the CRAC");
        assertCnecNotImported(cracCreationContext, "ae12", ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement ae12 ignored because the assessed element is not in base case and not combinable with contingencies, but no explicit link to a contingency was found");
        assertCnecNotImported(cracCreationContext, "ae13", ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement ae13 ignored because the assessed element is not in base case and not combinable with contingencies, but no explicit link to a contingency was found");
        // TODO: add pointing to generator

        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-1", "RTE_AE1 (ae1) - RTE_CO1 - curative", cracCreationContext.getCrac().getInstant(CURATIVE_INSTANT_ID), UsageMethod.AVAILABLE);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-2", "RTE_AE4 (ae4) - RTE_CO1 - curative", cracCreationContext.getCrac().getInstant(CURATIVE_INSTANT_ID), UsageMethod.AVAILABLE);
        assertHasOnFlowConstraintUsageRule(cracCreationContext, "remedial-action-2", "RTE_AE4 (ae4) - RTE_CO2 - curative", cracCreationContext.getCrac().getInstant(CURATIVE_INSTANT_ID), UsageMethod.AVAILABLE);
    }
}
