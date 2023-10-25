package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_api.network_action.InjectionSetpoint;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.CsaProfileCrac;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationTestUtil;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreator;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreatorTest;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.importer.CsaProfileCracImporter;
import com.farao_community.farao.data.crac_impl.OnContingencyStateImpl;
import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Properties;
import java.util.stream.Stream;

import static com.farao_community.farao.data.crac_api.Instant.CURATIVE;
import static com.farao_community.farao.data.crac_api.Instant.PREVENTIVE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class InjectionSetPointActionCreationTest {

    @Test
    public void testImportInjectionSetPointActions() {
        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-23/CSA_23_1_ValidProfiles.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();

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

        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertEquals(8, cracCreationContext.getCrac().getRemedialActions().size());
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
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/csa-23/CSA_23_2_InvalidProfiles.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);
        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-23/CSA_23_2_InvalidProfiles.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);
        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());
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
        CsaProfileCracCreationTestUtil.assertRaNotImported(cracCreationContext, "parent-remedial-action-18", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action parent-remedial-action-18 will not be imported because several conflictual StaticPropertyRanges are linked to that RA's RotatingMachineAction");
    }
}
