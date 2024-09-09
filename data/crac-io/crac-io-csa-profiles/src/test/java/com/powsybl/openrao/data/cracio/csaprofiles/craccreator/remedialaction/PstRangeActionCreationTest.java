/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.remedialaction;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;
import com.powsybl.openrao.data.cracapi.parameters.JsonCracCreationParameters;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationContext;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.AUTO_INSTANT_ID;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.CURATIVE_2_INSTANT_ID;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.NETWORK;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.PREVENTIVE_INSTANT_ID;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.assertContingencyEquality;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.assertFlowCnecEquality;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.assertHasOnConstraintUsageRule;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.assertHasOnContingencyStateUsageRule;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.assertPstRangeActionImported;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.assertRaNotImported;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PstRangeActionCreationTest {

    @Test
    void importPstRangeActions() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/profiles/remedialactions/PSTRangeActions.zip", NETWORK);

        List<PstRangeAction> importedPstRangeActions = cracCreationContext.getCrac().getPstRangeActions().stream().sorted(Comparator.comparing(PstRangeAction::getId)).toList();
        assertEquals(6, importedPstRangeActions.size());

        assertPstRangeActionImported(importedPstRangeActions.get(0), "remedial-action-1", "RTE_RA1", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-1", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertPstRangeActionImported(importedPstRangeActions.get(1), "remedial-action-2", "RTE_RA2", "FFR2AA1  FFR4AA1  1", 3, null, "RTE");
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);

        assertPstRangeActionImported(importedPstRangeActions.get(2), "remedial-action-3", "RTE_RA3", "FFR2AA1  FFR4AA1  1", null, 17, "RTE");
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-3", CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-3", CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-3", CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);

        assertPstRangeActionImported(importedPstRangeActions.get(3), "remedial-action-4", "RTE_RA4", "BBE2AA1  BBE3AA1  1", 5, 15, "RTE");
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-4", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertPstRangeActionImported(importedPstRangeActions.get(4), "tap-position-action-8-1", "tap-position-action-8-1", "BBE2AA1  BBE3AA1  1", null, null, null);
        assertPstRangeActionImported(importedPstRangeActions.get(5), "tap-position-action-8-2", "tap-position-action-8-2", "FFR2AA1  FFR4AA1  1", null, null, null);

        assertEquals(7, cracCreationContext.getRemedialActionCreationContexts().stream().filter(context -> !context.isImported()).toList().size());

        assertRaNotImported(cracCreationContext, "remedial-action-5", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action remedial-action-5 will not be imported because transformer with id unknown-pst was not found in network");
        assertRaNotImported(cracCreationContext, "remedial-action-6", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-6 will not be imported because TapPositionAction must have a property reference with http://energy.referencedata.eu/PropertyReference/TapChanger.step value, but it was: http://energy.referencedata.eu/PropertyReference/Switch.open");
        assertRaNotImported(cracCreationContext, "remedial-action-7", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-7 will not be imported because StaticPropertyRange must have a property reference with http://energy.referencedata.eu/PropertyReference/TapChanger.step value, but it was: http://energy.referencedata.eu/PropertyReference/Switch.open");
        assertRaNotImported(cracCreationContext, "remedial-action-9", ImportStatus.NOT_FOR_RAO, "Remedial action remedial-action-9 will not be imported because the field normalEnabled in TapPositionAction is set to false");
        assertRaNotImported(cracCreationContext, "remedial-action-10", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-10 will not be imported because there is more than ONE StaticPropertyRange with direction RelativeDirectionKind.up");
        assertRaNotImported(cracCreationContext, "remedial-action-11", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-11 will not be imported because there is more than ONE StaticPropertyRange with direction RelativeDirectionKind.down");
        assertRaNotImported(cracCreationContext, "remedial-action-12", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-12 will not be imported because StaticPropertyRange has wrong value of valueKind, the only allowed value is absolute");
    }

    @Test
    void importPstSps0second() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/csaCracCreationParameters_SPS_0_sec.json"));
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/profiles/remedialactions/CSA-92_PST-SPS_with_0_sec_threshold.zip", NETWORK, importedParameters);

        List<Contingency> importedContingencies = cracCreationContext.getCrac().getContingencies().stream().sorted(Comparator.comparing(Contingency::getId)).toList();
        assertEquals(1, importedContingencies.size());

        assertContingencyEquality(importedContingencies.get(0), "contingency", "RTE_CO", Set.of("FFR1AA1  FFR2AA1  1"));

        List<FlowCnec> importedFlowCnecs = cracCreationContext.getCrac().getFlowCnecs().stream().sorted(Comparator.comparing(FlowCnec::getId)).toList();
        assertEquals(5, importedFlowCnecs.size());

        assertFlowCnecEquality(importedFlowCnecs.get(0), "RTE_AE1 (ae-1) - RTE_CO - auto - TWO - TATL 900", "FFR2AA1  FFR3AA1  1",
            AUTO_INSTANT_ID, "contingency", null, null, 4000.0, -4000.0, Set.of(TwoSides.TWO), "RTE", "ES-FR");
        assertFlowCnecEquality(importedFlowCnecs.get(1), "RTE_AE1 (ae-1) - RTE_CO - curative 1 - TWO - TATL 900", "FFR2AA1  FFR3AA1  1",
            CURATIVE_1_INSTANT_ID, "contingency", null, null, 4000.0, -4000.0, Set.of(TwoSides.TWO), "RTE", "ES-FR");
        assertFlowCnecEquality(importedFlowCnecs.get(2), "RTE_AE2 (ae-2) - RTE_CO - curative 2 - TWO", "FFR2AA1  FFR3AA1  1",
            CURATIVE_2_INSTANT_ID, "contingency", null, null, 2500.0, -2500.0, Set.of(TwoSides.TWO), "RTE", "ES-FR");
        assertFlowCnecEquality(importedFlowCnecs.get(3), "RTE_AE2 (ae-2) - RTE_CO - curative 3 - TWO", "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID, "contingency", null, null, 2500.0, -2500.0, Set.of(TwoSides.TWO), "RTE", "ES-FR");
        assertFlowCnecEquality(importedFlowCnecs.get(4), "RTE_AE2 (ae-2) - preventive - TWO", "FFR2AA1  FFR3AA1  1",
            PREVENTIVE_INSTANT_ID, null, null, null, 2500.0, -2500.0, Set.of(TwoSides.TWO), "RTE", "ES-FR");

        List<PstRangeAction> importedPstRangeActions = cracCreationContext.getCrac().getPstRangeActions().stream().sorted(Comparator.comparing(PstRangeAction::getId)).toList();
        assertEquals(3, importedPstRangeActions.size());

        assertPstRangeActionImported(cracCreationContext, "auto-pst-be2-be3", "BBE2AA1  BBE3AA1  1", false, 1, null);
        assertHasOnConstraintUsageRule(cracCreationContext, "auto-pst-be2-be3", "RTE_AE1 (ae-1) - RTE_CO - auto - TWO - TATL 900", cracCreationContext.getCrac().getInstant(AUTO_INSTANT_ID), UsageMethod.FORCED, FlowCnec.class);
        assertPstRangeActionImported(cracCreationContext, "cra-pst-be2-be3", "BBE2AA1  BBE3AA1  1", false, 5, null);
        assertHasOnConstraintUsageRule(cracCreationContext, "cra-pst-be2-be3", "RTE_AE2 (ae-2) - RTE_CO - curative 2 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_1_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "cra-pst-be2-be3", "RTE_AE2 (ae-2) - RTE_CO - curative 2 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_2_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "cra-pst-be2-be3", "RTE_AE2 (ae-2) - RTE_CO - curative 3 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_1_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "cra-pst-be2-be3", "RTE_AE2 (ae-2) - RTE_CO - curative 3 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_2_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        assertHasOnConstraintUsageRule(cracCreationContext, "cra-pst-be2-be3", "RTE_AE2 (ae-2) - RTE_CO - curative 3 - TWO", cracCreationContext.getCrac().getInstant(CURATIVE_3_INSTANT_ID), UsageMethod.AVAILABLE, FlowCnec.class);
        assertPstRangeActionImported(cracCreationContext, "cra-pst-fr2-fr4", "FFR2AA1  FFR4AA1  1", false, 3, null);
        assertHasOnInstantUsageRule(cracCreationContext, "cra-pst-fr2-fr4", CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnInstantUsageRule(cracCreationContext, "cra-pst-fr2-fr4", CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnInstantUsageRule(cracCreationContext, "cra-pst-fr2-fr4", CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);
    }

    @Test
    void importPstSps60seconds() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/csaCracCreationParameters_SPS_60_sec.json"));
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/profiles/remedialactions/CSA-92_PST-SPS_with_60_sec_threshold.zip", NETWORK, importedParameters);

        List<Contingency> importedContingencies = cracCreationContext.getCrac().getContingencies().stream().sorted(Comparator.comparing(Contingency::getId)).toList();
        assertEquals(1, importedContingencies.size());

        assertContingencyEquality(importedContingencies.get(0), "contingency", "RTE_CO", Set.of("FFR1AA1  FFR2AA1  1"));

        List<FlowCnec> importedFlowCnecs = cracCreationContext.getCrac().getFlowCnecs().stream().sorted(Comparator.comparing(FlowCnec::getId)).toList();
        assertEquals(5, importedFlowCnecs.size());

        assertFlowCnecEquality(importedFlowCnecs.get(0), "RTE_AE1 (ae-1) - RTE_CO - auto - TWO - TATL 900", "FFR2AA1  FFR3AA1  1",
            AUTO_INSTANT_ID, "contingency", null, null, 4000.0, -4000.0, Set.of(TwoSides.TWO), "RTE", "ES-FR");
        assertFlowCnecEquality(importedFlowCnecs.get(1), "RTE_AE1 (ae-1) - RTE_CO - curative 1 - TWO - TATL 900", "FFR2AA1  FFR3AA1  1",
            CURATIVE_1_INSTANT_ID, "contingency", null, null, 4000.0, -4000.0, Set.of(TwoSides.TWO), "RTE", "ES-FR");
        assertFlowCnecEquality(importedFlowCnecs.get(2), "RTE_AE2 (ae-2) - RTE_CO - curative 2 - TWO", "FFR2AA1  FFR3AA1  1",
            CURATIVE_2_INSTANT_ID, "contingency", null, null, 2500.0, -2500.0, Set.of(TwoSides.TWO), "RTE", "ES-FR");
        assertFlowCnecEquality(importedFlowCnecs.get(3), "RTE_AE2 (ae-2) - RTE_CO - curative 3 - TWO", "FFR2AA1  FFR3AA1  1",
            CURATIVE_3_INSTANT_ID, "contingency", null, null, 2500.0, -2500.0, Set.of(TwoSides.TWO), "RTE", "ES-FR");
        assertFlowCnecEquality(importedFlowCnecs.get(4), "RTE_AE2 (ae-2) - preventive - TWO", "FFR2AA1  FFR3AA1  1",
            PREVENTIVE_INSTANT_ID, null, null, null, 2500.0, -2500.0, Set.of(TwoSides.TWO), "RTE", "ES-FR");

        List<PstRangeAction> importedPstRangeActions = cracCreationContext.getCrac().getPstRangeActions().stream().sorted(Comparator.comparing(PstRangeAction::getId)).toList();
        assertEquals(3, importedPstRangeActions.size());

        assertPstRangeActionImported(cracCreationContext, "auto-pst-be2-be3", "BBE2AA1  BBE3AA1  1", false, 1, null);
        assertHasOnContingencyStateUsageRule(cracCreationContext, "auto-pst-be2-be3", "contingency", AUTO_INSTANT_ID, UsageMethod.FORCED);
        assertPstRangeActionImported(cracCreationContext, "cra-pst-be2-be3", "BBE2AA1  BBE3AA1  1", false, 3, null);
        assertHasOnContingencyStateUsageRule(cracCreationContext, "cra-pst-be2-be3", "contingency", CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnContingencyStateUsageRule(cracCreationContext, "cra-pst-be2-be3", "contingency", CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnContingencyStateUsageRule(cracCreationContext, "cra-pst-be2-be3", "contingency", CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);
        assertPstRangeActionImported(cracCreationContext, "cra-pst-fr2-fr4", "FFR2AA1  FFR4AA1  1", false, 3, null);
        assertHasOnInstantUsageRule(cracCreationContext, "cra-pst-fr2-fr4", CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnInstantUsageRule(cracCreationContext, "cra-pst-fr2-fr4", CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnInstantUsageRule(cracCreationContext, "cra-pst-fr2-fr4", CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);
    }

    @Test
    void checkAlignedPstsImport() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/profiles/remedialactions/AlignedPSTs.zip", NETWORK);
        List<PstRangeAction> importedPstRangeActions = cracCreationContext.getCrac().getPstRangeActions().stream().sorted(Comparator.comparing(PstRangeAction::getId)).toList();
        assertEquals(4, importedPstRangeActions.size());

        assertPstRangeActionImported(importedPstRangeActions.get(0), "pst-range-action-4", "PST 4", "FFR2AA1  FFR4AA1  1", null, null, null);
        assertEquals(Optional.empty(), importedPstRangeActions.get(0).getGroupId());
        assertEquals(Optional.empty(), importedPstRangeActions.get(0).getSpeed());
        assertHasOnInstantUsageRule(cracCreationContext, "pst-range-action-4", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        assertPstRangeActionImported(importedPstRangeActions.get(1), "tap-position-action-1-1", "RTE-tap-position-action-1-1", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals("pst-group-1", importedPstRangeActions.get(1).getGroupId().get());
        assertEquals(2, importedPstRangeActions.get(1).getSpeed().get());
        assertHasOnInstantUsageRule(cracCreationContext, "tap-position-action-1-1", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        assertPstRangeActionImported(importedPstRangeActions.get(2), "tap-position-action-1-2", "RTE-tap-position-action-1-2", "FFR2AA1  FFR4AA1  1", null, null, "RTE");
        assertEquals("pst-group-1", importedPstRangeActions.get(2).getGroupId().get());
        assertEquals(2, importedPstRangeActions.get(2).getSpeed().get());
        assertHasOnInstantUsageRule(cracCreationContext, "tap-position-action-1-2", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        assertPstRangeActionImported(importedPstRangeActions.get(3), "tap-position-action-2-1", "tap-position-action-2-1", "BBE2AA1  BBE3AA1  1", null, null, null);
        assertEquals("pst-group-2", importedPstRangeActions.get(3).getGroupId().get());
        assertEquals(3, importedPstRangeActions.get(3).getSpeed().get());
        assertHasOnInstantUsageRule(cracCreationContext, "tap-position-action-2-1", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        assertEquals(2, cracCreationContext.getRemedialActionCreationContexts().stream().filter(context -> !context.isImported()).toList().size());
        assertRaNotImported(cracCreationContext, "tap-position-action-2-2", ImportStatus.NOT_FOR_RAO, "Remedial action tap-position-action-2-2 will not be imported because the field normalEnabled in TapPositionAction is set to false");
        assertRaNotImported(cracCreationContext, "pst-group-3", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action tap-position-action-3-2 will not be imported because there is more than ONE StaticPropertyRange with direction RelativeDirectionKind.up");
    }

}
