/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.threshold.BranchThreshold;
import com.powsybl.openrao.data.crac.api.usagerule.OnInstant;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import com.powsybl.openrao.data.crac.io.commons.iidm.IidmPstHelper;
import com.powsybl.openrao.data.crac.io.network.parameters.CriticalElements;
import com.powsybl.openrao.data.crac.io.network.parameters.NetworkCracCreationParameters;
import com.powsybl.openrao.data.crac.io.network.parameters.PstRangeActions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class NetworkCracCreatorTest {

    private CracCreationParameters cracCreationParameters;
    private NetworkCracCreationParameters parameters;
    private CracCreationContext creationContext;
    private Crac crac;
    private Network network;

    @BeforeEach
    void setUp() {
        createBasicParameters();
    }

    private void importCracFrom(String networkName) {
        network = Network.read(networkName, getClass().getResourceAsStream("/" + networkName));
        creationContext = new NetworkCracCreator().createCrac(network, cracCreationParameters);
        creationContext.getCreationReport().printCreationReport();
        crac = creationContext.getCrac();
    }

    private void createBasicParameters() {
        cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES);
        parameters = new NetworkCracCreationParameters(null, List.of("curative"));
        cracCreationParameters.addExtension(NetworkCracCreationParameters.class, parameters);

        parameters.getCriticalElements().setThresholdDefinition(CriticalElements.ThresholdDefinition.PERM_LIMIT_MULTIPLIER);
        parameters.getCriticalElements().setLimitMultiplierPerInstant(
            Map.of("preventive", 0.95, "outage", 1.3, "curative", 1.1)
        );
        parameters.getCriticalElements().setApplicableLimitDurationPerInstant(Map.of("outage", 60., "curative", Double.POSITIVE_INFINITY));

        parameters.getPstRangeActions()
            .setAvailableTapRangesAtInstants(
                Map.of("preventive", new PstRangeActions.TapRange(-8, 8, RangeType.RELATIVE_TO_PREVIOUS_INSTANT),
                    "curative", new PstRangeActions.TapRange(-2, 2, RangeType.RELATIVE_TO_PREVIOUS_INSTANT))
            );
    }

    @Test
    void testImportUcteFull() {
        importCracFrom("TestCase12Nodes.uct");
        assertTrue(creationContext.isCreationSuccessful());
        assertNotNull(crac);
        assertEquals(496, crac.getFlowCnecs().size());
        assertEquals(2, crac.getPstRangeActions().size());
        assertEquals(24, crac.getInjectionRangeActions().size());
    }

    private void checkCnec(String id, String neId, InstantKind instantKind, Unit thresholdUnit, double thresholdValue) {
        FlowCnec cnec = crac.getFlowCnec(id);
        assertNotNull(cnec);
        assertEquals(neId, cnec.getNetworkElement().getId());
        assertEquals(instantKind, cnec.getState().getInstant().getKind());
        for (BranchThreshold t : cnec.getThresholds()) {
            assertEquals(thresholdUnit, t.getUnit());
            assertEquals(-thresholdValue, t.min().orElseThrow());
            assertEquals(thresholdValue, t.max().orElseThrow());
        }
    }

    @Test
    void testUcteCnecsFiltered() {
        parameters.getCriticalElements().setCountryFilter(Set.of(Country.BE));
        parameters.getContingencies().setCountryFilter(Set.of(Country.NL));
        parameters.getCriticalElements().setOptimizedMonitoredProvider((b, c) -> new CriticalElements.OptimizedMonitored(!Utils.branchIsInCountries(b, Set.of(Country.FR)), false));
        importCracFrom("TestCase12Nodes.uct");
        assertTrue(creationContext.isCreationSuccessful());
        assertNotNull(crac);
        assertEquals(5, crac.getContingencies().size());
        assertEquals(42, crac.getFlowCnecs().size());

        checkCnec("BBE2AA1  BBE3AA1  1_preventive", "BBE2AA1  BBE3AA1  1", InstantKind.PREVENTIVE, Unit.AMPERE, 0.95 * 5000);
        checkCnec("BBE2AA1  BBE3AA1  1_CO_NNL1AA1  NNL2AA1  1_outage", "BBE2AA1  BBE3AA1  1", InstantKind.OUTAGE, Unit.AMPERE, 1.3 * 5000);
        checkCnec("BBE2AA1  BBE3AA1  1_CO_NNL1AA1  NNL2AA1  1_curative", "BBE2AA1  BBE3AA1  1", InstantKind.CURATIVE, Unit.AMPERE, 1.1 * 5000);

        checkCnec("NNL2AA1  BBE3AA1  1_preventive", "NNL2AA1  BBE3AA1  1", InstantKind.PREVENTIVE, Unit.AMPERE, 0.95 * 5000);
        checkCnec("NNL2AA1  BBE3AA1  1_CO_DDE2AA1  NNL3AA1  1_outage", "NNL2AA1  BBE3AA1  1", InstantKind.OUTAGE, Unit.AMPERE, 1.3 * 5000);
        checkCnec("NNL2AA1  BBE3AA1  1_CO_DDE2AA1  NNL3AA1  1_curative", "NNL2AA1  BBE3AA1  1", InstantKind.CURATIVE, Unit.AMPERE, 1.1 * 5000);

        // Filtered out by predicate
        assertNull(crac.getFlowCnec("BBE2AA1  FFR3AA1  1_CO_NNL1AA1  NNL2AA1  1_outage"));
    }

    @Test
    void testUctePstsFiltered1() {
        parameters.getPstRangeActions().setCountryFilter(Set.of(Country.FR));
        importCracFrom("TestCase12Nodes.uct");
        assertTrue(creationContext.isCreationSuccessful());
        assertNotNull(crac);
        assertTrue(crac.getPstRangeActions().isEmpty());
    }

    @Test
    void testUctePstsFiltered2() {
        parameters.getPstRangeActions().setCountryFilter(Set.of(Country.BE));
        parameters.getPstRangeActions().setAvailableTapRangesAtInstants(Map.of("curative", new PstRangeActions.TapRange(-1, 5, RangeType.RELATIVE_TO_PREVIOUS_INSTANT)));
        importCracFrom("TestCase12Nodes.uct");
        assertTrue(creationContext.isCreationSuccessful());
        assertNotNull(crac);
        assertEquals(1, crac.getPstRangeActions().size());
        PstRangeAction pst = crac.getPstRangeAction("PST_RA_BBE2AA1  BBE3AA1  1_curative");
        assertNotNull(pst);
        assertEquals("BBE2AA1  BBE3AA1  1", pst.getNetworkElement().getId());
        assertEquals(0, pst.getInitialTap());
        assertEquals(new IidmPstHelper("BBE2AA1  BBE3AA1  1", network).getTapToAngleConversionMap(), pst.getTapToAngleConversionMap());
        assertEquals(2, pst.getRanges().size());
        assertEquals(RangeType.ABSOLUTE, pst.getRanges().get(0).getRangeType());
        assertEquals(-16, pst.getRanges().get(0).getMinTap());
        assertEquals(16, pst.getRanges().get(0).getMaxTap());
        assertEquals(RangeType.RELATIVE_TO_PREVIOUS_INSTANT, pst.getRanges().get(1).getRangeType());
        assertEquals(-1, pst.getRanges().get(1).getMinTap());
        assertEquals(5, pst.getRanges().get(1).getMaxTap());
        assertEquals(1, pst.getUsageRules().size());
        UsageRule ur = pst.getUsageRules().iterator().next();
        assertInstanceOf(OnInstant.class, ur);
        assertEquals(crac.getInstant(InstantKind.CURATIVE), ur.getInstant());
    }

    @Test
    void testUctePstsFiltered3() {
        parameters.getPstRangeActions().setCountryFilter(Set.of(Country.BE));
        parameters.getPstRangeActions().setAvailableTapRangesAtInstants(Map.of("curative", new PstRangeActions.TapRange(-1, 5, RangeType.RELATIVE_TO_PREVIOUS_INSTANT)));
        parameters.getPstRangeActions().setPstRaPredicate((twt, state) -> state.getContingency().isPresent() && state.getContingency().get().getId().contains("NNL3AA1"));
        importCracFrom("TestCase12Nodes.uct");
        assertTrue(creationContext.isCreationSuccessful());
        assertNotNull(crac);
        assertEquals(1, crac.getPstRangeActions().size());
        PstRangeAction pst = crac.getPstRangeAction("PST_RA_BBE2AA1  BBE3AA1  1_curative");
        assertNotNull(pst);
        assertEquals(3, pst.getUsageRules().size());
        crac.getStates(crac.getInstant(InstantKind.CURATIVE)).stream().filter(
                state -> state.getContingency().isPresent() && state.getContingency().get().getId().contains("NNL3AA1")
            )
            .forEach(state -> assertTrue(pst.isAvailableForState(state)));
    }

    @Test
    void testImport1() {
        importCracFrom("testNetwork3BusbarSections.xiidm");
        assertTrue(creationContext.isCreationSuccessful());
        assertNotNull(crac);
    }

    @Test
    void testImport2() {
        parameters.getCriticalElements().setThresholdDefinition(CriticalElements.ThresholdDefinition.FROM_OPERATIONAL_LIMITS);

        importCracFrom("network_one_voltage_level.xiidm");

        assertTrue(creationContext.isCreationSuccessful());
        assertNotNull(crac);
    }

    @Test
    void testImport3() {
        importCracFrom("TestCase16NodesWith2Hvdc.xiidm");
        assertTrue(creationContext.isCreationSuccessful());
        assertNotNull(crac);
    }
}
