/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnecAdder;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class VoltageCnecImplTest {
    private final static double DOUBLE_TOLERANCE = 1e-3;
    private static final Instant INSTANT_PREV = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private Crac crac;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId");
        crac.addInstant(INSTANT_PREV);
    }

    private VoltageCnecAdder initPreventiveCnecAdder() {
        return crac.newVoltageCnec()
            .withId("voltage-cnec")
            .withName("voltage-cnec-name")
            .withNetworkElement("networkElement")
            .withOperator("FR")
            .withInstantId(INSTANT_PREV.getId())
            .withOptimized(false);
    }

    @Test
    void testGetLocation1() {

        Network network = NetworkImportsUtil.import12NodesNetwork();

        VoltageCnec cnec1 = crac.newVoltageCnec()
            .withId("cnec-1-id")
            .withNetworkElement("BBE1AA1")
            .withInstantId(INSTANT_PREV.getId())
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(1000.).add()
            .add();

        VoltageCnec cnec2 = crac.newVoltageCnec()
            .withId("cnec-2-id")
            .withNetworkElement("DDE2AA1")
            .withInstantId(INSTANT_PREV.getId())
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(1000.).add()
            .add();

        Set<Optional<Country>> countries = cnec1.getLocation(network);
        assertEquals(1, countries.size());
        assertTrue(countries.contains(Optional.of(Country.BE)));

        countries = cnec2.getLocation(network);
        assertEquals(1, countries.size());
        assertTrue(countries.contains(Optional.of(Country.DE)));
    }

    @Test
    void testVoltageCnecWithOneMaxThreshold() {

        VoltageCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(500.).add()
            .add();

        // bounds
        assertEquals(500., cnec.getUpperBound(Unit.KILOVOLT).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(cnec.getLowerBound(Unit.KILOVOLT).isPresent());

        // margin
        assertEquals(200., cnec.computeMargin(300, Unit.KILOVOLT), DOUBLE_TOLERANCE); // bound: 500 MW
        assertEquals(800., cnec.computeMargin(-300, Unit.KILOVOLT), DOUBLE_TOLERANCE); // bound: 760 A
    }

    @Test
    void testVoltageCnecWithSeveralThresholds() {
        VoltageCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.).add()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-200.).add()
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(500.).add()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-300.).add()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-50.).withMax(150.).add()
            .add();

        assertEquals(100., cnec.getUpperBound(Unit.KILOVOLT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-50., cnec.getLowerBound(Unit.KILOVOLT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-200., cnec.computeMargin(300, Unit.KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(-150., cnec.computeMargin(-200, Unit.KILOVOLT), DOUBLE_TOLERANCE);
    }

    @Test
    void marginsWithNegativeAndPositiveLimits() {

        VoltageCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-200.).withMax(500.).add()
            .add();

        assertEquals(-100, cnec.computeMargin(-300, Unit.KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(200, cnec.computeMargin(0, Unit.KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(100, cnec.computeMargin(400, Unit.KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(-300, cnec.computeMargin(800, Unit.KILOVOLT), DOUBLE_TOLERANCE);
    }

    // other

    @Test
    void testEqualsAndHashCode() {
        VoltageCnec cnec1 = initPreventiveCnecAdder().newThreshold().withUnit(Unit.KILOVOLT).withMax(1000.).add().add();
        VoltageCnec cnec2 = initPreventiveCnecAdder().withId("anotherId").newThreshold().withUnit(Unit.KILOVOLT).withMin(-1000.).add().add();

        assertEquals(cnec1, cnec1);
        assertNotEquals(cnec1, cnec2);
        assertNotNull(cnec1);
        assertNotEquals(1, cnec1);

        assertEquals(cnec1.hashCode(), cnec1.hashCode());
        assertNotEquals(cnec1.hashCode(), cnec2.hashCode());
    }
}
