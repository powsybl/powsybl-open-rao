/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import com.powsybl.iidm.network.BoundaryLineFilter;
import com.powsybl.iidm.network.ContainerType;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.DcLine;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.NetworkEventRecorder;
import com.powsybl.iidm.network.NetworkListener;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.ValidationLevel;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class LazyNetworkTest {
    @Test
    void testNetworkContent() {
        Network network = LazyNetwork.of("src/test/resources/12Nodes.uct");

        // metadata

        ZonedDateTime zonedDateTime = ZonedDateTime.of(LocalDate.of(2026, 3, 30).atStartOfDay(), ZoneId.of("UTC"));
        network.setCaseDate(zonedDateTime);
        assertEquals(zonedDateTime, network.getCaseDate());

        network.setForecastDistance(5);
        assertEquals(5, network.getForecastDistance());

        assertEquals("UCTE", network.getSourceFormat());

        assertNotNull(network.getVariantManager());

        assertEquals("12Nodes", network.getId());

        // network content

        assertEquals(Set.of(Country.BE, Country.DE, Country.FR, Country.NL), network.getCountries());
        assertEquals(4, network.getCountryCount());

        assertEquals(1, getIterableAsList(network.getAreaTypes()).size());
        assertEquals(1, network.getAreaTypeStream().toList().size());
        assertEquals(1, network.getAreaTypeCount());

        assertEquals(4, getIterableAsList(network.getAreas()).size());
        assertEquals(4, network.getAreaStream().toList().size());
        assertEquals(4, network.getAreaCount());
        assertNotNull(network.getArea("FR"));

        assertTrue(network.getSubnetworks().isEmpty());
        assertNull(network.getSubnetwork("unknown"));

        assertTrue(network.getDcComponents().isEmpty());

        assertEquals(11, getIterableAsList(network.getSubstations()).size());
        assertEquals(3, getIterableAsList(network.getSubstations(Country.FR, null)).size());
        assertEquals(3, getIterableAsList(network.getSubstations("FRANCE", null)).size());
        assertEquals(11, network.getSubstationStream().toList().size());
        assertEquals(11, network.getSubstationCount());
        assertNotNull(network.getSubstation("FFR1AA"));

        assertEquals(11, getIterableAsList(network.getVoltageLevels()).size());
        assertEquals(11, network.getVoltageLevelStream().toList().size());
        assertEquals(11, network.getVoltageLevelCount());
        assertNotNull(network.getVoltageLevel("FFR1AA1"));

        assertEquals(12, getIterableAsList(network.getGenerators()).size());
        assertEquals(12, network.getGeneratorStream().toList().size());
        assertEquals(12, network.getGeneratorCount());
        assertNotNull(network.getGenerator("FFR1AA1 _generator"));

        assertEquals(12, getIterableAsList(network.getLoads()).size());
        assertEquals(12, network.getLoadStream().toList().size());
        assertEquals(12, network.getLoadCount());
        assertNotNull(network.getLoad("FFR1AA1 _load"));

        assertEquals(15, getIterableAsList(network.getLines()).size());
        assertEquals(15, network.getLineStream().toList().size());
        assertEquals(15, network.getLineCount());
        assertNotNull(network.getLine("FFR1AA1  FFR2AA1  1"));

        assertEquals(0, getIterableAsList(network.getTieLines()).size());
        assertEquals(0, network.getTieLineStream().toList().size());
        assertEquals(0, network.getTieLineCount());
        assertNull(network.getTieLine("unknown"));

        assertEquals(16, getIterableAsList(network.getBranches()).size());
        assertEquals(16, network.getBranchStream().toList().size());
        assertEquals(16, network.getBranchCount());
        assertNotNull(network.getBranch("FFR1AA1  FFR2AA1  1"));

        assertEquals(1, getIterableAsList(network.getTwoWindingsTransformers()).size());
        assertEquals(1, network.getTwoWindingsTransformerStream().toList().size());
        assertEquals(1, network.getTwoWindingsTransformerCount());
        assertNotNull(network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1"));

        assertEquals(0, getIterableAsList(network.getThreeWindingsTransformers()).size());
        assertEquals(0, network.getThreeWindingsTransformerStream().toList().size());
        assertEquals(0, network.getThreeWindingsTransformerCount());
        assertNull(network.getThreeWindingsTransformer("unknown"));

        assertEquals(0, getIterableAsList(network.getShuntCompensators()).size());
        assertEquals(0, network.getShuntCompensatorStream().toList().size());
        assertEquals(0, network.getShuntCompensatorCount());
        assertNull(network.getShuntCompensator("unknown"));

        assertEquals(0, getIterableAsList(network.getHvdcLines()).size());
        assertEquals(0, network.getHvdcLineStream().toList().size());
        assertEquals(0, network.getHvdcLineCount());
        assertNull(network.getHvdcLine("unknown"));

        assertEquals(0, getIterableAsList(network.getOverloadManagementSystems()).size());
        assertEquals(0, network.getOverloadManagementSystemStream().toList().size());
        assertEquals(0, network.getOverloadManagementSystemCount());
        assertNull(network.getOverloadManagementSystem("unknown"));

        assertEquals(0, getIterableAsList(network.getBatteries()).size());
        assertEquals(0, network.getBatteryStream().toList().size());
        assertEquals(0, network.getBatteryCount());
        assertNull(network.getBattery("unknown"));

        assertEquals(0, getIterableAsList(network.getBoundaryLines()).size());
        assertEquals(0, getIterableAsList(network.getBoundaryLines(BoundaryLineFilter.ALL)).size());
        assertEquals(0, network.getBoundaryLineStream().toList().size());
        assertEquals(0, network.getBoundaryLineStream(BoundaryLineFilter.ALL).toList().size());
        assertEquals(0, network.getBoundaryLineCount());
        assertNull(network.getBoundaryLine("unknown"));

        assertEquals(0, getIterableAsList(network.getStaticVarCompensators()).size());
        assertEquals(0, network.getStaticVarCompensatorStream().toList().size());
        assertEquals(0, network.getStaticVarCompensatorCount());
        assertNull(network.getStaticVarCompensator("unknown"));

        assertEquals(0, getIterableAsList(network.getSwitches()).size());
        assertEquals(0, network.getSwitchStream().toList().size());
        assertEquals(0, network.getSwitchCount());
        assertNull(network.getSwitch("unknown"));

        assertEquals(0, getIterableAsList(network.getBusbarSections()).size());
        assertEquals(0, network.getBusbarSectionStream().toList().size());
        assertEquals(0, network.getBusbarSectionCount());
        assertNull(network.getBusbarSection("unknown"));

        assertEquals(0, getIterableAsList(network.getHvdcConverterStations()).size());
        assertEquals(0, network.getHvdcConverterStationStream().toList().size());
        assertEquals(0, network.getHvdcConverterStationCount());
        assertNull(network.getHvdcConverterStation("unknown"));

        assertEquals(0, getIterableAsList(network.getLccConverterStations()).size());
        assertEquals(0, network.getLccConverterStationStream().toList().size());
        assertEquals(0, network.getLccConverterStationCount());
        assertNull(network.getLccConverterStation("unknown"));

        assertEquals(0, getIterableAsList(network.getVscConverterStations()).size());
        assertEquals(0, network.getVscConverterStationStream().toList().size());
        assertEquals(0, network.getVscConverterStationCount());
        assertNull(network.getVscConverterStation("unknown"));

        assertEquals(0, getIterableAsList(network.getGrounds()).size());
        assertEquals(0, network.getGroundStream().toList().size());
        assertEquals(0, network.getGroundCount());
        assertNull(network.getGround("unknown"));

        assertEquals(0, getIterableAsList(network.getDcNodes()).size());
        assertEquals(0, network.getDcNodeStream().toList().size());
        assertEquals(0, network.getDcNodeCount());
        assertNull(network.getDcNode("unknown"));

        assertEquals(0, getIterableAsList(network.getDcLines()).size());
        assertEquals(0, network.getDcLineStream().toList().size());
        assertEquals(0, network.getDcLineCount());
        assertNull(network.getDcLine("unknown"));

        assertEquals(0, getIterableAsList(network.getDcSwitches()).size());
        assertEquals(0, network.getDcSwitchStream().toList().size());
        assertEquals(0, network.getDcSwitchCount());
        assertNull(network.getDcSwitch("unknown"));

        assertEquals(0, getIterableAsList(network.getDcGrounds()).size());
        assertEquals(0, network.getDcGroundStream().toList().size());
        assertEquals(0, network.getDcGroundCount());
        assertNull(network.getDcGround("unknown"));

        assertEquals(0, getIterableAsList(network.getLineCommutatedConverters()).size());
        assertEquals(0, network.getLineCommutatedConverterStream().toList().size());
        assertEquals(0, network.getLineCommutatedConverterCount());
        assertNull(network.getLineCommutatedConverter("unknown"));

        assertEquals(0, getIterableAsList(network.getVoltageSourceConverters()).size());
        assertEquals(0, network.getVoltageSourceConverterStream().toList().size());
        assertEquals(0, network.getVoltageSourceConverterCount());
        assertNull(network.getVoltageSourceConverter("unknown"));

        assertEquals(0, network.getDcBusStream().toList().size());
        assertEquals(0, network.getDcBusCount());
        assertNull(network.getDcBus("unknown"));

        assertEquals(15, network.getIdentifiableStream(IdentifiableType.LINE).toList().size());
        assertEquals(79, network.getIdentifiables().size());
        assertNotNull(network.getIdentifiable("FFR1AA1  FFR2AA1  1"));

        assertEquals(40, getIterableAsList(network.getConnectables()).size());
        assertEquals(40, network.getConnectableStream().toList().size());
        assertEquals(40, network.getConnectableCount());
        assertNotNull(network.getConnectable("FFR1AA1  FFR2AA1  1"));

        assertEquals(15, getIterableAsList(network.getConnectables(Line.class)).size());
        assertEquals(15, network.getConnectableStream(Line.class).toList().size());
        assertEquals(15, network.getConnectableCount(Line.class));

        assertEquals(0, getIterableAsList(network.getDcConnectables(DcLine.class)).size());
        assertEquals(0, network.getDcConnectableStream(DcLine.class).toList().size());
        assertEquals(0, network.getDcConnectableCount(DcLine.class));
        assertEquals(0, getIterableAsList(network.getDcConnectables()).size());
        assertEquals(0, network.getDcConnectableStream().toList().size());
        assertEquals(0, network.getDcConnectableCount());
        assertNull(network.getDcConnectable("unknown"));

        assertNotNull(network.getBusBreakerView());
        assertNotNull(network.getBusView());

        assertEquals(0, getIterableAsList(network.getVoltageAngleLimits()).size());
        assertEquals(0, network.getVoltageAngleLimitsStream().toList().size());
        assertNull(network.getVoltageAngleLimit("unknown"));

        assertFalse(network.isDetachable());

        assertEquals(0, network.getBoundaryElements().size());
        assertFalse(network.isBoundaryElement(network.getIdentifiable("FFR1AA1  FFR2AA1  1")));

        assertEquals(IdentifiableType.NETWORK, network.getType());

        assertEquals(ValidationLevel.STEADY_STATE_HYPOTHESIS, network.getValidationLevel());

        // adders

        network.newArea().setId("fictitiousArea").setAreaType("fictitious").add();
        assertEquals(5, network.getAreaCount());

        network.newSubstation().setCountry(Country.FR).setId("FFR4AA").add();
        assertEquals(4, getIterableAsList(network.getSubstations(Country.FR, null)).size());

        network.newVoltageLevel().setId("FFR4AA1").setNominalV(400.0).setTopologyKind(TopologyKind.BUS_BREAKER).add();
        assertEquals(12, network.getVoltageLevelCount());

        // other

        NetworkListener networkListener = new NetworkEventRecorder();
        network.addListener(networkListener);
        network.removeListener(networkListener);

        network.allowReportNodeContextMultiThreadAccess(true);
        network.flatten();
        assertThrows(IllegalStateException.class, network::detach);

        network.setMinimumAcceptableValidationLevel(ValidationLevel.STEADY_STATE_HYPOTHESIS);

        assertEquals(ValidationLevel.STEADY_STATE_HYPOTHESIS, network.runValidationChecks());
        assertEquals(ValidationLevel.STEADY_STATE_HYPOTHESIS, network.runValidationChecks(true));

        assertEquals(ContainerType.NETWORK, network.getContainerType());

        assertEquals(network, network.getNetwork());
        assertEquals(network, network.getParentNetwork());

        network.setName("UCTE Lazy Network");
        assertEquals(Optional.of("UCTE Lazy Network"), network.getOptionalName());

        network.setProperty("property", "Hello world!");
        assertTrue(network.hasProperty("property"));
        assertEquals("Hello world!", network.getProperty("property"));
        assertEquals(Set.of("property"), network.getPropertyNames());
        network.removeProperty("property");

        network.addAlias("alias-1");
        network.addAlias("alias-2", true);
        assertTrue(network.hasAliases());
        network.removeAlias("alias-1");
        network.removeAlias("alias-2");
        assertFalse(network.hasAliases());
    }

    private static <T> List<T> getIterableAsList(Iterable<T> iterable) {
        List<T> list = new ArrayList<>();
        iterable.forEach(list::add);
        return list;
    }
}
