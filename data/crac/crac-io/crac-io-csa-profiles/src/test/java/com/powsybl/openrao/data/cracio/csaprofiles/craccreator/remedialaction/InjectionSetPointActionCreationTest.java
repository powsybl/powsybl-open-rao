/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.remedialaction;

import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationContext;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.CURATIVE_1_INSTANT_ID;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.CURATIVE_2_INSTANT_ID;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.CURATIVE_3_INSTANT_ID;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.NETWORK;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.PREVENTIVE_INSTANT_ID;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.assertRaNotImported;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InjectionSetPointActionCreationTest {

    @Test
    void importRotatingMachineActions() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/profiles/remedialactions/RotatingMachineActions.zip", NETWORK);

        List<NetworkAction> importedInjectionSetpointActions = cracCreationContext.getCrac().getNetworkActions().stream().sorted(Comparator.comparing(NetworkAction::getId)).toList();
        assertEquals(5, importedInjectionSetpointActions.size());

        assertSimpleGeneratorActionImported(importedInjectionSetpointActions.get(0), "remedial-action-1", "RTE_RA1", "FFR1AA1 _generator", 1500d, "RTE");
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-1", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertSimpleGeneratorActionImported(importedInjectionSetpointActions.get(1), "remedial-action-2", "RTE_RA2", "FFR2AA1 _generator", 2350d, "RTE");
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);

        assertSimpleGeneratorActionImported(importedInjectionSetpointActions.get(2), "remedial-action-3", "RTE_RA3", "FFR2AA1 _generator", 1790d, "RTE");
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-3", CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-3", CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-3", CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);

        assertSimpleLoadActionImported(importedInjectionSetpointActions.get(3), "remedial-action-4", "RTE_RA4", "FFR1AA1 _load", 1150d, "RTE");
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-4", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertSimpleLoadActionImported(importedInjectionSetpointActions.get(4), "remedial-action-5", "RTE_RA5", "FFR1AA1 _load", 900d, "RTE");
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-5", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertEquals(10, cracCreationContext.getRemedialActionCreationContexts().stream().filter(context -> !context.isImported()).toList().size());

        assertRaNotImported(cracCreationContext, "remedial-action-6", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-6 will not be imported because RotatingMachineAction must have a property reference with http://energy.referencedata.eu/PropertyReference/RotatingMachine.p value, but it was: http://energy.referencedata.eu/PropertyReference/Switch.open");
        assertRaNotImported(cracCreationContext, "remedial-action-7", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-7 will not be imported because StaticPropertyRange must have a property reference with http://energy.referencedata.eu/PropertyReference/RotatingMachine.p value, but it was: http://energy.referencedata.eu/PropertyReference/Switch.open");
        assertRaNotImported(cracCreationContext, "remedial-action-8", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-8 will not be imported because its StaticPropertyRange uses an illegal combination of ValueOffsetKind and RelativeDirectionKind");
        assertRaNotImported(cracCreationContext, "remedial-action-9", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-9 will not be imported because its StaticPropertyRange uses an illegal combination of ValueOffsetKind and RelativeDirectionKind");
        assertRaNotImported(cracCreationContext, "remedial-action-10", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-10 will not be imported because its StaticPropertyRange uses an illegal combination of ValueOffsetKind and RelativeDirectionKind");
        assertRaNotImported(cracCreationContext, "remedial-action-11", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-11 will not be imported because its StaticPropertyRange uses an illegal combination of ValueOffsetKind and RelativeDirectionKind");
        assertRaNotImported(cracCreationContext, "remedial-action-12", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action remedial-action-12 will not be imported because the network does not contain a generator, neither a load with id: unknown-rotating-machine");
        assertRaNotImported(cracCreationContext, "remedial-action-13", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-13 will not be imported because there is no StaticPropertyRange linked to elementary action rotating-machine-action-13");
        assertRaNotImported(cracCreationContext, "remedial-action-14", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-14 will not be imported because several conflictual StaticPropertyRanges are linked to elementary action rotating-machine-action-14");
        assertRaNotImported(cracCreationContext, "remedial-action-15", ImportStatus.NOT_FOR_RAO, "Remedial action remedial-action-15 will not be imported because it has no elementary action");
    }

    @Test
    void importShuntCompensatorModifications() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/profiles/remedialactions/ShuntCompensatorModifications.zip", NETWORK);

        List<NetworkAction> importedInjectionSetpointActions = cracCreationContext.getCrac().getNetworkActions().stream().sorted(Comparator.comparing(NetworkAction::getId)).toList();
        assertEquals(5, importedInjectionSetpointActions.size());

        assertSimpleShuntCompensatorActionImported(importedInjectionSetpointActions.get(0), "remedial-action-1", "RTE_RA1", "shunt-compensator", 3d, "RTE");
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-1", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertSimpleShuntCompensatorActionImported(importedInjectionSetpointActions.get(1), "remedial-action-2", "RTE_RA2", "shunt-compensator", 3d, "RTE");
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);

        assertSimpleShuntCompensatorActionImported(importedInjectionSetpointActions.get(2), "remedial-action-3", "RTE_RA3", "shunt-compensator", 0d, "RTE");
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-3", CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-3", CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-3", CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);

        assertSimpleShuntCompensatorActionImported(importedInjectionSetpointActions.get(3), "remedial-action-4", "RTE_RA4", "shunt-compensator", 2d, "RTE");
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-4", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertSimpleShuntCompensatorActionImported(importedInjectionSetpointActions.get(4), "remedial-action-5", "RTE_RA5", "shunt-compensator", 0d, "RTE");
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-4", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertEquals(17, cracCreationContext.getRemedialActionCreationContexts().stream().filter(context -> !context.isImported()).toList().size());

        assertRaNotImported(cracCreationContext, "remedial-action-6", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-6 will not be imported because ShuntCompensatorModification must have a property reference with http://energy.referencedata.eu/PropertyReference/ShuntCompensator.sections value, but it was: http://energy.referencedata.eu/PropertyReference/Switch.open");
        assertRaNotImported(cracCreationContext, "remedial-action-7", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-7 will not be imported because StaticPropertyRange must have a property reference with http://energy.referencedata.eu/PropertyReference/ShuntCompensator.sections value, but it was: http://energy.referencedata.eu/PropertyReference/Switch.open");
        assertRaNotImported(cracCreationContext, "remedial-action-8", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-8 will not be imported because its StaticPropertyRange uses an illegal combination of ValueOffsetKind and RelativeDirectionKind");
        assertRaNotImported(cracCreationContext, "remedial-action-9", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-9 will not be imported because its StaticPropertyRange uses an illegal combination of ValueOffsetKind and RelativeDirectionKind");
        assertRaNotImported(cracCreationContext, "remedial-action-10", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-10 will not be imported because its StaticPropertyRange uses an illegal combination of ValueOffsetKind and RelativeDirectionKind");
        assertRaNotImported(cracCreationContext, "remedial-action-11", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-11 will not be imported because its StaticPropertyRange uses an illegal combination of ValueOffsetKind and RelativeDirectionKind");
        assertRaNotImported(cracCreationContext, "remedial-action-12", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action remedial-action-12 will not be imported because the network does not contain a shunt compensator with id: unknown-shunt-compensator");
        assertRaNotImported(cracCreationContext, "remedial-action-13", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-13 will not be imported because there is no StaticPropertyRange linked to elementary action shunt-compensator-modification-13");
        assertRaNotImported(cracCreationContext, "remedial-action-14", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-14 will not be imported because several conflictual StaticPropertyRanges are linked to elementary action shunt-compensator-modification-14");
        assertRaNotImported(cracCreationContext, "remedial-action-15", ImportStatus.NOT_FOR_RAO, "Remedial action remedial-action-15 will not be imported because it has no elementary action");
        assertRaNotImported(cracCreationContext, "remedial-action-16", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-16 will not be imported because of a non integer-castable number of sections");
        assertRaNotImported(cracCreationContext, "remedial-action-17", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-17 will not be imported because of a non integer-castable number of sections");
        assertRaNotImported(cracCreationContext, "remedial-action-18", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-18 will not be imported because of a non integer-castable number of sections");
        assertRaNotImported(cracCreationContext, "remedial-action-19", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-19 will not be imported because of a non integer-castable number of sections");
        assertRaNotImported(cracCreationContext, "remedial-action-20", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-20 will not be imported because of a non integer-castable number of sections");
        assertRaNotImported(cracCreationContext, "remedial-action-21", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-21 will not be imported because of an incoherent negative number of sections");
        assertRaNotImported(cracCreationContext, "remedial-action-22", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-22 will not be imported because of an incoherent negative number of sections");
    }
}
