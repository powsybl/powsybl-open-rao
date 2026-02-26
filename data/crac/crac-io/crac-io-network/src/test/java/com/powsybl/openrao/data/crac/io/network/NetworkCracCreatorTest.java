/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;

import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.api.threshold.BranchThreshold;
import com.powsybl.openrao.data.crac.api.usagerule.OnInstant;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import com.powsybl.openrao.data.crac.io.commons.iidm.IidmPstHelper;
import com.powsybl.openrao.data.crac.io.network.parameters.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.*;

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
        try {
            creationContext = Crac.readWithContext(networkName, getClass().getResourceAsStream("/" + networkName), network, cracCreationParameters);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

        parameters.getPstRangeActions()
            .setAvailableTapRangesAtInstants(
                Map.of("preventive", new PstRangeActions.TapRange(-8, 8, RangeType.RELATIVE_TO_PREVIOUS_INSTANT),
                    "curative", new PstRangeActions.TapRange(-2, 2, RangeType.RELATIVE_TO_PREVIOUS_INSTANT))
            );
    }

    Map<String, Double> getKeys(String raId) {
        InjectionRangeAction ra = crac.getInjectionRangeAction(raId);
        assertNotNull(ra);
        Map<String, Double> distKeys = new HashMap<>();
        ra.getInjectionDistributionKeys().forEach((ne, k) -> distKeys.put(ne.getId(), k));
        return distKeys;
    }

    @Test
    void testImportUcteFull() {
        importCracFrom("TestCase12Nodes.uct");
        assertTrue(creationContext.isCreationSuccessful());
        assertNotNull(crac);
        assertEquals(16, crac.getContingencies().size());
        assertEquals(496, crac.getFlowCnecs().size());
        assertEquals(2, crac.getPstRangeActions().size());
        assertEquals(24, crac.getInjectionRangeActions().size());
    }

    @Test
    void testImportUcteFullOneSide() {
        cracCreationParameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_TWO);
        importCracFrom("TestCase12Nodes.uct");
        assertTrue(creationContext.isCreationSuccessful());
        assertNotNull(crac);
        assertEquals(16, crac.getContingencies().size());
        assertEquals(496, crac.getFlowCnecs().size());
        assertEquals(2, crac.getPstRangeActions().size());
        assertEquals(24, crac.getInjectionRangeActions().size());
    }

    private void checkCnec(String id, String neId, InstantKind instantKind, Unit thresholdUnit, double thresholdValue) {
        checkCnec(id, neId, instantKind, thresholdUnit, thresholdValue, thresholdValue);
    }

    private void checkCnec(String id, String neId, InstantKind instantKind, Unit thresholdUnit, double thresholdValue1, double thresholdValue2) {
        FlowCnec cnec = crac.getFlowCnec(id);
        assertNotNull(cnec);
        assertEquals(neId, cnec.getNetworkElement().getId());
        assertEquals(instantKind, cnec.getState().getInstant().getKind());
        for (BranchThreshold t : cnec.getThresholds()) {
            double expectedValue = t.getSide().equals(TwoSides.ONE) ? thresholdValue1 : thresholdValue2;
            assertEquals(thresholdUnit, t.getUnit());
            assertEquals(-expectedValue, t.min().orElseThrow(), 1e-3);
            assertEquals(expectedValue, t.max().orElseThrow(), 1e-3);
        }
    }

    @Test
    void testUcteCnecsFiltered() {
        parameters.getCriticalElements().setCountryFilter(Set.of(Country.BE));
        parameters.getContingencies().setCountryFilter(Set.of(Country.NL));
        parameters.getCriticalElements().setOptimizedMonitoredProvider((b, c, cc) -> new CriticalElements.OptimizedMonitored(!Utils.branchIsInCountries(b, Set.of(Country.FR)), false));
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
        assertEquals(2, crac.getPstRangeActions().size());
        PstRangeAction pst = crac.getPstRangeAction("PST_RA_BBE2AA1  BBE3AA1  1_curative");
        assertNotNull(pst);
        assertEquals("BBE2AA1  BBE3AA1  1", pst.getNetworkElement().getId());
        assertEquals(0, pst.getInitialTap());
        assertEquals(new IidmPstHelper("BBE2AA1  BBE3AA1  1", network).getTapToAngleConversionMap(), pst.getTapToAngleConversionMap());
        assertEquals(2, pst.getRanges().size());
        assertEquals(RangeType.ABSOLUTE, pst.getRanges().getFirst().getRangeType());
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
    void testUcteWithFilteredRedispatching1() {
        parameters.getRedispatchingRangeActions().setCountryFilter(Set.of(Country.BE));
        parameters.getRedispatchingRangeActions().setRdRaPredicate((injection, instant) -> injection.getType() == IdentifiableType.GENERATOR && instant.isPreventive());
        importCracFrom("TestCase12Nodes.uct");
        assertTrue(creationContext.isCreationSuccessful());
        assertEquals(0, creationContext.getCreationReport().getReport().size());
        assertEquals(3, crac.getInjectionRangeActions().size());
        assertTrue(crac.getInjectionRangeActions().stream().allMatch(ra -> ra.getId().contains("_preventive")));
    }

    @Test
    void testUcteWithFilteredRedispatching2() {
        parameters.getRedispatchingRangeActions().setCountryFilter(Set.of(Country.BE));
        parameters.getRedispatchingRangeActions().setRdRaPredicate((injection, instant) -> instant.isPreventive());
        parameters.getRedispatchingRangeActions().setRaCostsProvider((injection, instant) -> new InjectionRangeActionCosts(1., 2., 3.));
        importCracFrom("TestCase12Nodes.uct");
        assertTrue(creationContext.isCreationSuccessful());
        // loads should be skipped
        assertEquals(3, creationContext.getCreationReport().getReport().size());
        assertEquals(3, crac.getInjectionRangeActions().size());
        assertTrue(crac.getInjectionRangeActions().stream().allMatch(ra -> ra.getId().contains("_preventive")));

        InjectionRangeAction ra = crac.getInjectionRangeAction("RD_GEN_BBE1AA1 _generator_preventive");
        assertNotNull(ra);
        assertEquals(1, ra.getInjectionDistributionKeys().size());
        assertEquals(1.0, ra.getInjectionDistributionKeys().values().iterator().next());
        assertEquals("BBE1AA1 _generator", ra.getInjectionDistributionKeys().keySet().iterator().next().getId());
        assertEquals(-9000., ra.getRanges().getFirst().getMin());
        assertEquals(9000., ra.getRanges().getFirst().getMax());
        assertEquals(1500., ra.getInitialSetpoint());
        assertEquals(Optional.of(1.), ra.getActivationCost());
        assertEquals(Optional.of(2.), ra.getVariationCost(VariationDirection.UP));
        assertEquals(Optional.of(3.), ra.getVariationCost(VariationDirection.DOWN));
    }

    @Test
    void testUcteRdWithCombis() {
        parameters.getRedispatchingRangeActions().setIncludeAllInjections(false);
        parameters.getRedispatchingRangeActions().setGeneratorCombinations(
            Map.of(
                "combi1", Set.of("DDE1AA1 _generator", "FFR1AA1 _generator"),
                "combi2", Set.of("NNL2AA1 _generator")
            )
        );
        parameters.getRedispatchingRangeActions().setRdRaPredicate((injection, instant) -> instant.isPreventive());
        parameters.getRedispatchingRangeActions().setCombinationRangeProvider((combi, instant) -> new MinAndMax<>(1000., 1500.));
        parameters.getRedispatchingRangeActions().setCombinationCostsProvider((combi, instant) -> new InjectionRangeActionCosts(1., 2., 3.));
        importCracFrom("TestCase12Nodes.uct");
        assertTrue(creationContext.isCreationSuccessful());
        assertEquals(2, crac.getInjectionRangeActions().size());
        assertTrue(crac.getInjectionRangeActions().stream().allMatch(ra -> ra.getId().contains("_preventive")));

        InjectionRangeAction ra = crac.getInjectionRangeAction("RD_COMBI_combi1_preventive");
        assertNotNull(ra);
        assertEquals(2, ra.getInjectionDistributionKeys().size());
        assertEquals(Map.of("FFR1AA1 _generator", 3000. / 5500., "DDE1AA1 _generator", 2500. / 5500.), getKeys("RD_COMBI_combi1_preventive"));
        assertEquals(1000., ra.getRanges().getFirst().getMin());
        assertEquals(5500., ra.getRanges().getFirst().getMax());
        assertEquals(5500., ra.getInitialSetpoint());
        assertEquals(Optional.of(1.), ra.getActivationCost());
        assertEquals(Optional.of(2.), ra.getVariationCost(VariationDirection.UP));
        assertEquals(Optional.of(3.), ra.getVariationCost(VariationDirection.DOWN));
    }

    @Test
    void testUcteWithRedispatchingOnLoad() {
        parameters.getRedispatchingRangeActions().setCountryFilter(Set.of(Country.NL));
        parameters.getRedispatchingRangeActions().setRdRaPredicate((injection, instant) -> instant.isPreventive() && injection.getType().equals(IdentifiableType.LOAD));
        parameters.getRedispatchingRangeActions().setRaRangeProvider((injection, instant) -> new MinAndMax<>(-100., 100.));
        parameters.getRedispatchingRangeActions().setRaCostsProvider((injection, instant) -> new InjectionRangeActionCosts(1., 2., 3.));
        importCracFrom("TestCase12Nodes.uct");
        assertTrue(creationContext.isCreationSuccessful());
        assertEquals(3, crac.getInjectionRangeActions().size());
        assertTrue(crac.getInjectionRangeActions().stream().allMatch(ra -> ra.getId().contains("_preventive")));

        InjectionRangeAction ra = crac.getInjectionRangeAction("RD_LOAD_NNL3AA1 _load_preventive");
        assertNotNull(ra);
        assertEquals(1, ra.getInjectionDistributionKeys().size());
        assertEquals(1.0, ra.getInjectionDistributionKeys().values().iterator().next());
        assertEquals("NNL3AA1 _load", ra.getInjectionDistributionKeys().keySet().iterator().next().getId());
        assertEquals(-100., ra.getRanges().getFirst().getMin());
        assertEquals(2500., ra.getRanges().getFirst().getMax());
        assertEquals(2500., ra.getInitialSetpoint());
        assertEquals(Optional.of(1.), ra.getActivationCost());
        assertEquals(Optional.of(2.), ra.getVariationCost(VariationDirection.UP));
        assertEquals(Optional.of(3.), ra.getVariationCost(VariationDirection.DOWN));
    }

    @Test
    void testUcteRdWithWrongCombis() {
        parameters.getRedispatchingRangeActions().setIncludeAllInjections(false);
        parameters.getRedispatchingRangeActions().setGeneratorCombinations(
            Map.of(
                "combi1", Set.of("DDE1AA1 _generator", "wrong")
            )
        );
        parameters.getRedispatchingRangeActions().setRdRaPredicate((injection, instant) -> instant.isPreventive());
        parameters.getRedispatchingRangeActions().setCombinationRangeProvider((combi, instant) -> new MinAndMax<>(1000., 1500.));
        parameters.getRedispatchingRangeActions().setCombinationCostsProvider((combi, instant) -> new InjectionRangeActionCosts(1., 2., 3.));
        importCracFrom("TestCase12Nodes.uct");
        assertTrue(creationContext.isCreationSuccessful());
        assertEquals(1, creationContext.getCreationReport().getReport().size());
        assertEquals("[REMOVED] Combination 'combi1' could not be considered because at least one generator could not be found in the network.", creationContext.getCreationReport().getReport().get(0));
        assertTrue(crac.getInjectionRangeActions().isEmpty());
    }

    @Test
    void testUcteCt() {
        parameters.getRedispatchingRangeActions().setIncludeAllInjections(false);
        parameters.getCountertradingRangeActions().setCountryFilter(Set.of(Country.NL));
        parameters.getCountertradingRangeActions().setRaRangeProvider((country, instant) ->
            instant.isPreventive() ? new MinAndMax<>(-1000., 1000.) : new MinAndMax<>(0., 0.));
        parameters.getCountertradingRangeActions().setRaCostsProvider((country, instant) -> new InjectionRangeActionCosts(10., 20., 30.));
        parameters.getCountertradingRangeActions().setGlsks(Mockito.mock(ZonalDataImpl.class));
        importCracFrom("TestCase12Nodes.uct");
        assertTrue(creationContext.isCreationSuccessful());
        assertEquals(1, crac.getInjectionRangeActions().size());
        InjectionRangeAction ra = crac.getInjectionRangeAction("CT_NETHERLANDS_preventive");
        assertNotNull(ra);
        assertEquals(Map.of("NNL1AA1 _generator", 0.375, "NNL3AA1 _generator", 0.5, "NNL2AA1 _generator", 0.125), getKeys("CT_NETHERLANDS_preventive"));
        assertEquals(3000., ra.getRanges().getFirst().getMin());
        assertEquals(5000., ra.getRanges().getFirst().getMax());
        assertEquals(4000., ra.getInitialSetpoint());
        assertEquals(Optional.of(10.), ra.getActivationCost());
        assertEquals(Optional.of(20.), ra.getVariationCost(VariationDirection.UP));
        assertEquals(Optional.of(30.), ra.getVariationCost(VariationDirection.DOWN));
        assertEquals(1, creationContext.getCreationReport().getReport().size());
        assertEquals("[ALTERED] Network CRAC importer does not yet support custom GLSKs for counter-trading actions. Proportional GLSK will be considered.", creationContext.getCreationReport().getReport().get(0));
    }

    @Test
    void testUcteWrongCt() {
        parameters.getRedispatchingRangeActions().setIncludeAllInjections(false);
        parameters.getCountertradingRangeActions().setCountryFilter(null);
        importCracFrom("TestCase12Nodes.uct");
        assertTrue(creationContext.isCreationSuccessful());
        assertTrue(crac.getInjectionRangeActions().isEmpty());
        assertEquals(1, creationContext.getCreationReport().getReport().size());
        assertEquals("[REMOVED] Cannot create counter-trading remedial actions without an explicit list of countries.", creationContext.getCreationReport().getReport().get(0));
    }

    @Test
    void testUcteWrongCt2() {
        parameters.getRedispatchingRangeActions().setIncludeAllInjections(false);
        parameters.getCountertradingRangeActions().setCountryFilter(Set.of(Country.NL));
        parameters.getCountertradingRangeActions().setRaRangeProvider((country, instant) ->
            instant.isPreventive() ? new MinAndMax<>(-1000., null) : new MinAndMax<>(null, 0.));
        importCracFrom("TestCase12Nodes.uct");
        assertTrue(creationContext.isCreationSuccessful());
        assertTrue(crac.getInjectionRangeActions().isEmpty());
        assertEquals(2, creationContext.getCreationReport().getReport().size());
        assertTrue(creationContext.getCreationReport().getReport().contains("[REMOVED] Cannot create a counter-trading action for NL at instant preventive without a defined min/max range."));
        assertTrue(creationContext.getCreationReport().getReport().contains("[REMOVED] Cannot create a counter-trading action for NL at instant curative without a defined min/max range."));
        assertTrue(crac.getInjectionRangeActions().isEmpty());
    }

    @Test
    void testUcteBalancing() {
        parameters.getRedispatchingRangeActions().setIncludeAllInjections(false);
        parameters.getBalancingRangeAction().setInjectionPredicate((injection, instant) -> instant.isPreventive() && injection.getId().contains("FFR"));
        parameters.getBalancingRangeAction().setRaRangeProvider(instant -> new MinAndMax<>(-1000., 2000.));
        parameters.getBalancingRangeAction().setRaCostsProvider(instant -> new InjectionRangeActionCosts(20., 300, 500));
        importCracFrom("TestCase12Nodes.uct");
        assertTrue(creationContext.isCreationSuccessful());
        assertEquals(1, crac.getInjectionRangeActions().size());
        InjectionRangeAction ra = crac.getInjectionRangeAction("BALANCING_preventive");
        assertNotNull(ra);
        assertEquals(Map.of("FFR2AA1 _generator", 0.25, "FFR1AA1 _generator", 0.375, "FFR3AA1 _generator", 0.375), getKeys("BALANCING_preventive"));
        assertEquals(7000., ra.getRanges().getFirst().getMin());
        assertEquals(10000., ra.getRanges().getFirst().getMax());
        assertEquals(8000., ra.getInitialSetpoint());
        assertEquals(Optional.of(20.), ra.getActivationCost());
        assertEquals(Optional.of(300.), ra.getVariationCost(VariationDirection.UP));
        assertEquals(Optional.of(500.), ra.getVariationCost(VariationDirection.DOWN));
    }

    @Test
    void testUcteWrongBalancing() {
        parameters.getRedispatchingRangeActions().setIncludeAllInjections(false);
        parameters.getBalancingRangeAction().setInjectionPredicate((injection, instant) -> instant.isPreventive() && injection.getId().contains("FFR"));
        parameters.getBalancingRangeAction().setRaRangeProvider(instant -> instant.isPreventive() ? new MinAndMax<>(null, 2000.) : new MinAndMax<>(-1000., null));
        importCracFrom("TestCase12Nodes.uct");
        assertTrue(creationContext.isCreationSuccessful());
        assertEquals(0, crac.getInjectionRangeActions().size());
        assertEquals(2, creationContext.getCreationReport().getReport().size());
        assertTrue(creationContext.getCreationReport().getReport().contains("[REMOVED] Cannot create a balancing action at instant preventive without a defined min/max range."));
        assertTrue(creationContext.getCreationReport().getReport().contains("[REMOVED] Cannot create a balancing action at instant curative without a defined min/max range."));
    }

    @Test
    void testImportWithOperationalLimits() {
        parameters.getCriticalElements().setThresholdDefinition(CriticalElements.ThresholdDefinition.FROM_OPERATIONAL_LIMITS);
        parameters.getCriticalElements().setApplicableLimitDurationPerInstant(Map.of("outage", 60., "curative", Double.POSITIVE_INFINITY));
        importCracFrom("network_one_voltage_level.xiidm");
        assertTrue(creationContext.isCreationSuccessful());
        assertNotNull(crac);

        checkCnec("CJ_preventive", "CJ", InstantKind.PREVENTIVE, Unit.AMPERE, 884.45);
        checkCnec("CJ_CO_CI_outage", "CJ", InstantKind.OUTAGE, Unit.AMPERE, 1210.3, 2600.);
        checkCnec("CJ_CO_CI_curative", "CJ", InstantKind.CURATIVE, Unit.AMPERE, 1024.1);

        checkCnec("CI_preventive", "CI", InstantKind.PREVENTIVE, Unit.AMPERE, 884.45);
        checkCnec("CI_CO_CJ_outage", "CI", InstantKind.OUTAGE, Unit.AMPERE, 1210.3);
        checkCnec("CI_CO_CJ_curative", "CI", InstantKind.CURATIVE, Unit.AMPERE, 1024.1);
    }
}
