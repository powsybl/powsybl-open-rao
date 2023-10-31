package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_api.network_action.InjectionSetpoint;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationTestUtil;
import com.farao_community.farao.data.crac_impl.OnContingencyStateImpl;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.stream.Stream;

import static com.farao_community.farao.data.crac_api.Instant.CURATIVE;
import static com.farao_community.farao.data.crac_api.Instant.PREVENTIVE;
import static com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InjectionSetPointActionCreationTest {

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

        assertEquals(7, cracCreationContext.getCrac().getRemedialActions().size());
        // RA1 (on instant)
        NetworkAction ra1 = cracCreationContext.getCrac().getNetworkAction("on-instant-preventive-remedial-action");
        assertEquals("RA1", ra1.getName());
        assertEquals(PREVENTIVE, ra1.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra1.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("rotating-machine", ((InjectionSetpoint) ra1.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(75., ((InjectionSetpoint) ra1.getElementaryActions().iterator().next()).getSetpoint());

        // RA2 (on instant)
        NetworkAction ra2 = cracCreationContext.getCrac().getNetworkAction("on-instant-curative-remedial-action");
        assertEquals("RA2", ra2.getName());
        assertEquals(CURATIVE, ra2.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra2.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("rotating-machine", ((InjectionSetpoint) ra2.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(17.3, ((InjectionSetpoint) ra2.getElementaryActions().iterator().next()).getSetpoint(), 0.1);

        // on-instant-preventive-nameless-remedial-action-with-speed (on instant)
        NetworkAction namelessRa = cracCreationContext.getCrac().getNetworkAction("on-instant-preventive-nameless-remedial-action-with-speed");
        assertEquals("on-instant-preventive-nameless-remedial-action-with-speed", namelessRa.getName());
        assertEquals(PREVENTIVE, namelessRa.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, namelessRa.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("rotating-machine", ((InjectionSetpoint) namelessRa.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(22.4, ((InjectionSetpoint) namelessRa.getElementaryActions().iterator().next()).getSetpoint(), 0.1);
        assertEquals(137, namelessRa.getSpeed().get());

        // RTE_RA7 (on instant)
        NetworkAction ra7 = cracCreationContext.getCrac().getNetworkAction("on-instant-preventive-remedial-with-tso-name");
        assertEquals("RTE_RA7", ra7.getName());
        assertEquals(PREVENTIVE, ra7.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra7.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("rotating-machine", ((InjectionSetpoint) ra7.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(100., ((InjectionSetpoint) ra7.getElementaryActions().iterator().next()).getSetpoint(), 0.1);
        assertEquals("RTE", ra7.getOperator());

        // on-instant-nameless-preventive-remedial-with-tso-name (on instant)
        NetworkAction namelessRa2 = cracCreationContext.getCrac().getNetworkAction("on-instant-nameless-preventive-remedial-with-tso-name");
        assertEquals("on-instant-nameless-preventive-remedial-with-tso-name", namelessRa2.getName());
        assertEquals(PREVENTIVE, namelessRa2.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, namelessRa2.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("rotating-machine", ((InjectionSetpoint) namelessRa2.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(98., ((InjectionSetpoint) namelessRa2.getElementaryActions().iterator().next()).getSetpoint(), 0.1);
        assertEquals("RTE", ra7.getOperator());

        // on-state-included-curative-remedial-action (on state)
        NetworkAction ra3 = cracCreationContext.getCrac().getNetworkAction("on-state-included-curative-remedial-action");
        assertEquals("RA3", ra3.getName());
        assertEquals(UsageMethod.FORCED, ra3.getUsageRules().iterator().next().getUsageMethod());
        assertEquals(CURATIVE, ra3.getUsageRules().iterator().next().getInstant());
        assertEquals("contingency", ((OnContingencyStateImpl) ra3.getUsageRules().iterator().next()).getContingency().getId());
        assertEquals("rotating-machine", ra3.getNetworkElements().iterator().next().getId());
        assertEquals(2.8, ((InjectionSetpoint) ra3.getElementaryActions().iterator().next()).getSetpoint(), 0.1);

        // on-state-considered-curative-remedial-action (on state)
        NetworkAction ra4 = cracCreationContext.getCrac().getNetworkAction("on-state-considered-curative-remedial-action");
        assertEquals("RA4", ra4.getName());
        assertEquals(UsageMethod.AVAILABLE, ra4.getUsageRules().iterator().next().getUsageMethod());
        assertEquals(CURATIVE, ra4.getUsageRules().iterator().next().getInstant());
        assertEquals("contingency", ((OnContingencyStateImpl) ra4.getUsageRules().iterator().next()).getContingency().getId());
        assertEquals("rotating-machine", ra4.getNetworkElements().iterator().next().getId());
        assertEquals(15.6, ((InjectionSetpoint) ra4.getElementaryActions().iterator().next()).getSetpoint(), 0.1);

    }

    @Test
    void testIgnoreInvalidInjectionSetpointProfile() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/csa-23/CSA_23_2_InvalidProfiles.zip");

        assertEquals(0, cracCreationContext.getCrac().getRemedialActions().size());

        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-1", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action parent-remedial-action-1 will not be imported because Network model does not contain a generator, neither a load with id of RotatingMachine: unknown-rotating-machine");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-2", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-2 will not be imported because there is no topology actions, no Set point actions, nor tap position action linked to that RA");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-3", ImportStatus.NOT_FOR_RAO, "Remedial action 'parent-remedial-action-3' will not be imported because field 'normalEnabled' in 'RotatingMachineAction' must be true or empty");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-4", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action 'parent-remedial-action-4' will not be imported because 'RotatingMachineAction' must have a property reference with 'http://energy.referencedata.eu/PropertyReference/RotatingMachine.p' value, but it was: 'http://energy.referencedata.eu/PropertyReference/RotatingMachine.q'");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-5", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-5 will not be imported because there is no topology actions, no Set point actions, nor tap position action linked to that RA");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-6", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-6 will not be imported because there is no StaticPropertyRange linked to that RA");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-7", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-7 will not be imported because StaticPropertyRange has a non float-castable normalValue so no set-point value was retrieved");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-8", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-8 will not be imported because there is no StaticPropertyRange linked to that RA");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-9", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action 'parent-remedial-action-9' will not be imported because 'StaticPropertyRange' must have a property reference with 'http://energy.referencedata.eu/PropertyReference/RotatingMachine.p' value, but it was: 'http://energy.referencedata.eu/PropertyReference/RotatingMachine.q'");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-10", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-10 will not be imported because there is no StaticPropertyRange linked to that RA");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-11", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-11 will not be imported because StaticPropertyRange has wrong values of valueKind and direction, the only allowed combination is absolute + none");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-12", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-12 will not be imported because StaticPropertyRange has wrong values of valueKind and direction, the only allowed combination is absolute + none");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-13", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-13 will not be imported because StaticPropertyRange has wrong values of valueKind and direction, the only allowed combination is absolute + none");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-14", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-14 will not be imported because StaticPropertyRange has wrong values of valueKind and direction, the only allowed combination is absolute + none");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-15", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-15 will not be imported because there is no StaticPropertyRange linked to that RA");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-16", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-16 will not be imported because StaticPropertyRange has wrong values of valueKind and direction, the only allowed combination is absolute + none");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-17", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-17 will not be imported because there is no StaticPropertyRange linked to that RA");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-18", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-18 will not be imported because several conflictual StaticPropertyRanges are linked to that RA's injection set point action");
    }

    @Test
    void testImportShuntCompensatorModifications() {
        Network network = Mockito.mock(Network.class);
        Branch networkElementMock = Mockito.mock(Branch.class);
        Mockito.when(networkElementMock.getId()).thenReturn("726c5cfa-d197-4e98-95a1-7dd357dd9353");
        Mockito.when(network.getIdentifiable("726c5cfa-d197-4e98-95a1-7dd357dd9353")).thenReturn(networkElementMock);

        Load loadMock = Mockito.mock(Load.class);
        Mockito.when(loadMock.getId()).thenReturn("726c5cfa-d197-4e98-95a1-7dd357dd9353");
        Mockito.when(network.getLoadStream()).thenAnswer(invocation -> {
            Stream<Load> loadStream = Stream.of(loadMock);
            return loadStream.filter(load ->
                load.getId().equals("726c5cfa-d197-4e98-95a1-7dd357dd9353")
            );
        });

        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_30_CustomProfiles.zip", network);

        assertNotNull(cracCreationContext);

        assertEquals(6, cracCreationContext.getCreationReport().getReport().size());
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "dabf1e87-a0d7-4046-a237-9a25b5bbb0d8", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action 'dabf1e87-a0d7-4046-a237-9a25b5bbb0d8' will not be imported because 'ShuntCompensatorModification' must have a property reference with 'http://energy.referencedata.eu/PropertyReference/ShuntCompensator.sections' value, but it was: 'http://energy.referencedata.eu/PropertyReference/RotatingMachine.p'");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "4a0b07a9-0a33-4926-a0ef-b3ebf7c9eb17", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action 4a0b07a9-0a33-4926-a0ef-b3ebf7c9eb17 will not be imported because StaticPropertyRange has a negative integer normalValue so no set-point value was retrieved");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "e3eb8875-79a7-42d6-8bc2-9ae81e9265c9", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action e3eb8875-79a7-42d6-8bc2-9ae81e9265c9 will not be imported because StaticPropertyRange has a non integer-castable normalValue so no set-point value was retrieved");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "6206f03a-9db7-4c46-86aa-03f8aec9d0f2", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action 6206f03a-9db7-4c46-86aa-03f8aec9d0f2 will not be imported because there is no StaticPropertyRange linked to that RA");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "43f38f8b-b81e-4f23-aa0a-44cdd508642e", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action 43f38f8b-b81e-4f23-aa0a-44cdd508642e will not be imported because StaticPropertyRange has wrong values of valueKind and direction, the only allowed combination is absolute + none");
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "c5c666d1-cc87-4652-ae81-1694a3849a07", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action c5c666d1-cc87-4652-ae81-1694a3849a07 will not be imported because Network model does not contain a generator, neither a load with id of ShuntCompensator: f8cf2bf7-c100-40e6-8c7c-c2bfc7099606");

        assertEquals(2, cracCreationContext.getCrac().getRemedialActions().size());
        NetworkAction ra1 = cracCreationContext.getCrac().getNetworkAction("d6247efe-3317-4c75-a752-c2a3a9f03aed");
        assertEquals("d6247efe-3317-4c75-a752-c2a3a9f03aed", ra1.getName());
        assertEquals(PREVENTIVE, ra1.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra1.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("726c5cfa-d197-4e98-95a1-7dd357dd9353", ((InjectionSetpoint) ra1.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(5, ((InjectionSetpoint) ra1.getElementaryActions().iterator().next()).getSetpoint());

        NetworkAction ra2 = cracCreationContext.getCrac().getNetworkAction("c1ac819a-4f03-48ee-826e-6f7c19dfba0a");
        assertEquals("c1ac819a-4f03-48ee-826e-6f7c19dfba0a", ra2.getName());
        assertEquals(PREVENTIVE, ra2.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra2.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("726c5cfa-d197-4e98-95a1-7dd357dd9353", ((InjectionSetpoint) ra2.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(3, ((InjectionSetpoint) ra2.getElementaryActions().iterator().next()).getSetpoint(), 0.1);
    }
}
