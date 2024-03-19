/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.networkaction.InjectionSetpoint;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil;
import com.powsybl.openrao.data.cracimpl.OnContingencyStateImpl;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ShuntCompensator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.stream.Stream;

import static com.powsybl.openrao.data.cracapi.InstantKind.PREVENTIVE;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InjectionSetPointActionCreationTest {

    // TODO: clean tests

    @Test
    void testImportInjectionSetPointActions() {
        Network network = Mockito.mock(Network.class);
        Branch networkElementMock = Mockito.mock(Branch.class);
        Mockito.when(networkElementMock.getId()).thenReturn("equipment-with-contingency");
        Mockito.when(network.getIdentifiable("equipment-with-contingency")).thenReturn(networkElementMock);

        Load loadMock = Mockito.mock(Load.class);
        Mockito.when(loadMock.getId()).thenReturn("rotating-machine");
        Mockito.when(network.getLoadStream()).thenAnswer(invocation -> {
            Stream<Load> loadStream = Stream.of(loadMock);
            return loadStream.filter(load ->
                load.getId().equals("rotating-machine")
            );
        });

        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/csa-23/CSA_23_1_ValidProfiles.zip", network);
        Instant preventiveInstant = cracCreationContext.getCrac().getInstant("preventive");
        Instant curativeInstant = cracCreationContext.getCrac().getInstant("curative");

        assertEquals(7, cracCreationContext.getCrac().getRemedialActions().size());
        // RA1 (on instant)
        NetworkAction ra1 = cracCreationContext.getCrac().getNetworkAction("on-instant-preventive-remedial-action");
        assertEquals("RA1", ra1.getName());
        assertEquals(preventiveInstant, ra1.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra1.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("rotating-machine", ((InjectionSetpoint) ra1.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(75., ((InjectionSetpoint) ra1.getElementaryActions().iterator().next()).getSetpoint());

        // RA2 (on instant)
        NetworkAction ra2 = cracCreationContext.getCrac().getNetworkAction("on-instant-curative-remedial-action");
        assertEquals("RA2", ra2.getName());
        assertEquals(curativeInstant, ra2.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra2.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("rotating-machine", ((InjectionSetpoint) ra2.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(17.3, ((InjectionSetpoint) ra2.getElementaryActions().iterator().next()).getSetpoint(), 0.1);

        // on-instant-preventive-nameless-remedial-action-with-speed (on instant)
        NetworkAction namelessRa = cracCreationContext.getCrac().getNetworkAction("on-instant-preventive-nameless-remedial-action-with-speed");
        assertEquals("on-instant-preventive-nameless-remedial-action-with-speed", namelessRa.getName());
        assertEquals(preventiveInstant, namelessRa.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, namelessRa.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("rotating-machine", ((InjectionSetpoint) namelessRa.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(22.4, ((InjectionSetpoint) namelessRa.getElementaryActions().iterator().next()).getSetpoint(), 0.1);
        assertEquals(137, namelessRa.getSpeed().get());

        // RTE_RA7 (on instant)
        NetworkAction ra7 = cracCreationContext.getCrac().getNetworkAction("on-instant-preventive-remedial-with-tso-name");
        assertEquals("RTE_RA7", ra7.getName());
        assertEquals(preventiveInstant, ra7.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra7.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("rotating-machine", ((InjectionSetpoint) ra7.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(100., ((InjectionSetpoint) ra7.getElementaryActions().iterator().next()).getSetpoint(), 0.1);
        assertEquals("RTE", ra7.getOperator());

        // on-instant-nameless-preventive-remedial-with-tso-name (on instant)
        NetworkAction namelessRa2 = cracCreationContext.getCrac().getNetworkAction("on-instant-nameless-preventive-remedial-with-tso-name");
        assertEquals("on-instant-nameless-preventive-remedial-with-tso-name", namelessRa2.getName());
        assertEquals(preventiveInstant, namelessRa2.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, namelessRa2.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("rotating-machine", ((InjectionSetpoint) namelessRa2.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(98., ((InjectionSetpoint) namelessRa2.getElementaryActions().iterator().next()).getSetpoint(), 0.1);
        assertEquals("RTE", ra7.getOperator());

        // on-state-included-curative-remedial-action (on state)
        NetworkAction ra3 = cracCreationContext.getCrac().getNetworkAction("on-state-included-curative-remedial-action");
        assertEquals("RA3", ra3.getName());
        assertEquals(UsageMethod.AVAILABLE, ra3.getUsageRules().iterator().next().getUsageMethod());
        assertEquals(curativeInstant, ra3.getUsageRules().iterator().next().getInstant());
        assertEquals("contingency", ((OnContingencyStateImpl) ra3.getUsageRules().iterator().next()).getContingency().getId());
        assertEquals("rotating-machine", ra3.getNetworkElements().iterator().next().getId());
        assertEquals(2.8, ((InjectionSetpoint) ra3.getElementaryActions().iterator().next()).getSetpoint(), 0.1);

        // on-state-considered-curative-remedial-action (on state)
        NetworkAction ra4 = cracCreationContext.getCrac().getNetworkAction("on-state-considered-curative-remedial-action");
        assertEquals("RA4", ra4.getName());
        assertEquals(UsageMethod.AVAILABLE, ra4.getUsageRules().iterator().next().getUsageMethod());
        assertEquals(curativeInstant, ra4.getUsageRules().iterator().next().getInstant());
        assertEquals("contingency", ((OnContingencyStateImpl) ra4.getUsageRules().iterator().next()).getContingency().getId());
        assertEquals("rotating-machine", ra4.getNetworkElements().iterator().next().getId());
        assertEquals(15.6, ((InjectionSetpoint) ra4.getElementaryActions().iterator().next()).getSetpoint(), 0.1);

    }

    @Test
    void testIgnoreInvalidInjectionSetpointProfile() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/csa-23/CSA_23_2_InvalidProfiles.zip");

        assertEquals(0, cracCreationContext.getCrac().getRemedialActions().size());

        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-1", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action parent-remedial-action-1 will not be imported because Network model does not contain a generator, neither a load with id of RotatingMachine: unknown-rotating-machine");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-2", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-2 will not be imported because there is no elementary action for that RA");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-3", ImportStatus.NOT_FOR_RAO, "Remedial action parent-remedial-action-3 will not be imported because it has no elementary action");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-4", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action 'parent-remedial-action-4' will not be imported because 'RotatingMachineAction' must have a property reference with 'http://energy.referencedata.eu/PropertyReference/RotatingMachine.p' value, but it was: 'http://energy.referencedata.eu/PropertyReference/RotatingMachine.q'");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-5", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-5 will not be imported because there is no elementary action for that RA");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-6", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-6 will not be imported because there is no StaticPropertyRange linked to elementary action rotating-machine-action-with-missing-static-property-range");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-7", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-7 will not be imported because StaticPropertyRange has a non float-castable normalValue so no set-point value was retrieved");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-8", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-8 will not be imported because there is no StaticPropertyRange linked to elementary action rotating-machine-action-with-static-property-range-with-missing-castable-normal-value-field");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-9", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action 'parent-remedial-action-9' will not be imported because 'StaticPropertyRange' must have a property reference with 'http://energy.referencedata.eu/PropertyReference/RotatingMachine.p' value, but it was: 'http://energy.referencedata.eu/PropertyReference/RotatingMachine.q'");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-10", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-10 will not be imported because there is no StaticPropertyRange linked to elementary action rotating-machine-action-with-static-property-range-with-missing-property-reference-field");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-11", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-11 will not be imported because StaticPropertyRange has wrong values of valueKind and direction");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-12", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-12 will not be imported because StaticPropertyRange has wrong values of valueKind and direction");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-13", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-13 will not be imported because StaticPropertyRange has wrong values of valueKind and direction");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-14", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-14 will not be imported because StaticPropertyRange has wrong values of valueKind and direction");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-15", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-15 will not be imported because there is no StaticPropertyRange linked to elementary action rotating-machine-action-with-static-property-range-with-missing-direction-field");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-16", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-16 will not be imported because StaticPropertyRange has wrong values of valueKind and direction");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-17", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-17 will not be imported because there is no StaticPropertyRange linked to elementary action rotating-machine-action-with-static-property-range-with-missing-value-kind-field");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-18", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-18 will not be imported because several conflictual StaticPropertyRanges are linked to elementary action rotating-machine-action-with-two-static-property-ranges");
    }

    @Test
    void testImportShuntCompensatorModifications() {
        ShuntCompensator loadMock = Mockito.mock(ShuntCompensator.class);
        Mockito.when(loadMock.getId()).thenReturn("726c5cfa-d197-4e98-95a1-7dd357dd9353");

        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getShuntCompensator("726c5cfa-d197-4e98-95a1-7dd357dd9353")).thenReturn(loadMock);

        Mockito.when(network.getShuntCompensatorStream()).thenAnswer(invocation -> {
            Stream<ShuntCompensator> loadStream = Stream.of(loadMock);
            return loadStream.filter(load ->
                load.getId().equals("726c5cfa-d197-4e98-95a1-7dd357dd9353")
            );
        });

        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_30_CustomProfiles.zip", network);

        assertNotNull(cracCreationContext);

        assertEquals(5, cracCreationContext.getCreationReport().getReport().size());
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "dabf1e87-a0d7-4046-a237-9a25b5bbb0d8", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action 'dabf1e87-a0d7-4046-a237-9a25b5bbb0d8' will not be imported because 'ShuntCompensatorModification' must have a property reference with 'http://energy.referencedata.eu/PropertyReference/ShuntCompensator.sections' value, but it was: 'http://energy.referencedata.eu/PropertyReference/RotatingMachine.p'");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "4a0b07a9-0a33-4926-a0ef-b3ebf7c9eb17", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action 4a0b07a9-0a33-4926-a0ef-b3ebf7c9eb17 will not be imported because StaticPropertyRange has a negative normalValue so no set-point value was retrieved");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "e3eb8875-79a7-42d6-8bc2-9ae81e9265c9", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action e3eb8875-79a7-42d6-8bc2-9ae81e9265c9 will not be imported because StaticPropertyRange has a non integer-castable normalValue so no set-point value was retrieved");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "6206f03a-9db7-4c46-86aa-03f8aec9d0f2", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action 6206f03a-9db7-4c46-86aa-03f8aec9d0f2 will not be imported because there is no StaticPropertyRange linked to elementary action ce11ada7-fe05-4398-a967-a85bea0d3b22");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "c5c666d1-cc87-4652-ae81-1694a3849a07", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action c5c666d1-cc87-4652-ae81-1694a3849a07 will not be imported because Network model does not contain a shunt compensator with id of ShuntCompensator: f8cf2bf7-c100-40e6-8c7c-c2bfc7099606");

        assertEquals(3, cracCreationContext.getCrac().getRemedialActions().size());
        NetworkAction ra1 = cracCreationContext.getCrac().getNetworkAction("d6247efe-3317-4c75-a752-c2a3a9f03aed");
        assertEquals("d6247efe-3317-4c75-a752-c2a3a9f03aed", ra1.getName());
        assertEquals(PREVENTIVE, ra1.getUsageRules().iterator().next().getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ra1.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("726c5cfa-d197-4e98-95a1-7dd357dd9353", ((InjectionSetpoint) ra1.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(5, ((InjectionSetpoint) ra1.getElementaryActions().iterator().next()).getSetpoint());

        NetworkAction ra2 = cracCreationContext.getCrac().getNetworkAction("c1ac819a-4f03-48ee-826e-6f7c19dfba0a");
        assertEquals("c1ac819a-4f03-48ee-826e-6f7c19dfba0a", ra2.getName());
        assertEquals(PREVENTIVE, ra2.getUsageRules().iterator().next().getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ra2.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("726c5cfa-d197-4e98-95a1-7dd357dd9353", ((InjectionSetpoint) ra2.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(3, ((InjectionSetpoint) ra2.getElementaryActions().iterator().next()).getSetpoint(), 0.1);

        NetworkAction ra3 = cracCreationContext.getCrac().getNetworkAction("43f38f8b-b81e-4f23-aa0a-44cdd508642e");
        assertEquals("43f38f8b-b81e-4f23-aa0a-44cdd508642e", ra3.getName());
        assertEquals(PREVENTIVE, ra3.getUsageRules().iterator().next().getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ra3.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("726c5cfa-d197-4e98-95a1-7dd357dd9353", ((InjectionSetpoint) ra3.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(2, ((InjectionSetpoint) ra3.getElementaryActions().iterator().next()).getSetpoint(), 0.1);
    }

    @Test
    void testImportSetPointIncrementalValues() {
        Network network = Mockito.mock(Network.class);
        Load loadMock = Mockito.mock(Load.class);
        Mockito.when(loadMock.getId()).thenReturn("606a1624-2be7-4c5b-8957-62126b8f38ad");
        Mockito.when(loadMock.getP0()).thenReturn(150.0);
        Mockito.when(network.getLoadStream()).thenAnswer(invocation -> {
            Stream<Load> loadStream = Stream.of(loadMock);
            return loadStream.filter(load ->
                load.getId().equals("606a1624-2be7-4c5b-8957-62126b8f38ad")
            );
        });

        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_33_TestExample.zip", network);

        assertNotNull(cracCreationContext);
        assertEquals(5, cracCreationContext.getCrac().getRemedialActions().size());

        NetworkAction ra1 = cracCreationContext.getCrac().getNetworkAction("9cfbe895-d7f3-4396-9405-d28a37d8a6bf");
        assertEquals("RTE_Relatively decreasing set-point RA in percent", ra1.getName());
        assertEquals(PREVENTIVE, ra1.getUsageRules().iterator().next().getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ra1.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("606a1624-2be7-4c5b-8957-62126b8f38ad", ((InjectionSetpoint) ra1.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(90, ((InjectionSetpoint) ra1.getElementaryActions().iterator().next()).getSetpoint());

        NetworkAction ra2 = cracCreationContext.getCrac().getNetworkAction("a5ebfb06-3e6b-4397-85d4-3158a5952372");
        assertEquals("RTE_Absolute set-point RA", ra2.getName());
        assertEquals(PREVENTIVE, ra2.getUsageRules().iterator().next().getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ra2.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("606a1624-2be7-4c5b-8957-62126b8f38ad", ((InjectionSetpoint) ra2.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(100, ((InjectionSetpoint) ra2.getElementaryActions().iterator().next()).getSetpoint());

        NetworkAction ra3 = cracCreationContext.getCrac().getNetworkAction("9346a870-47b0-4a70-8ab5-bac72ed83280");
        assertEquals("RTE_Relatively increasing set-point RA in percent", ra3.getName());
        assertEquals(PREVENTIVE, ra3.getUsageRules().iterator().next().getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ra3.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("606a1624-2be7-4c5b-8957-62126b8f38ad", ((InjectionSetpoint) ra3.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(195, ((InjectionSetpoint) ra3.getElementaryActions().iterator().next()).getSetpoint());

        NetworkAction ra4 = cracCreationContext.getCrac().getNetworkAction("3bf50d11-f507-46c9-ae98-cd3e66020dde");
        assertEquals("RTE_Relatively increasing set-point RA", ra4.getName());
        assertEquals(PREVENTIVE, ra4.getUsageRules().iterator().next().getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ra4.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("606a1624-2be7-4c5b-8957-62126b8f38ad", ((InjectionSetpoint) ra4.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(170, ((InjectionSetpoint) ra4.getElementaryActions().iterator().next()).getSetpoint());

        NetworkAction ra5 = cracCreationContext.getCrac().getNetworkAction("c96947f6-0c3d-4cf1-9331-937529fef6e9");
        assertEquals("RTE_Relatively decreasing set-point RA", ra5.getName());
        assertEquals(PREVENTIVE, ra5.getUsageRules().iterator().next().getInstant().getKind());
        assertEquals(UsageMethod.AVAILABLE, ra5.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("606a1624-2be7-4c5b-8957-62126b8f38ad", ((InjectionSetpoint) ra5.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(135, ((InjectionSetpoint) ra5.getElementaryActions().iterator().next()).getSetpoint());
    }
}
