/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.anglemonitoring;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class RedispatchActionWithAutoGlskTest {
    private static final double SHIFT_TOLERANCE = 0.01;

    private Network network;

    @BeforeEach
    void setUp() {
        String networkFileName = "TestCase_with_swe_countries.xiidm";
        network = Network.read(networkFileName, getClass().getResourceAsStream("/" + networkFileName));
        // Initial situation in ES (4 generators, total production of 9500 MW):
        // - EES1AA1_generator produces 1500 MW (15.79 %)
        // - EES2AA1_generator produces 3000 MW (31.58 %)
        // - EES3AA1_generator produces 2500 MW (26.32 %)
        // - EES4AA1_generator produces 2500 MW (26.32 %)
    }

    @Test
    void testNoShift() {
        new RedispatchActionWithAutoGlsk(Set.of(), Country.ES).apply(network, 0.0);
        assertEquals(1500., network.getGenerator("EES1AA11_generator").getTargetP(), SHIFT_TOLERANCE);
        assertEquals(3000., network.getGenerator("EES2AA11_generator").getTargetP(), SHIFT_TOLERANCE);
        assertEquals(2500., network.getGenerator("EES3AA11_generator").getTargetP(), SHIFT_TOLERANCE);
        assertEquals(2500., network.getGenerator("EES4AA11_generator").getTargetP(), SHIFT_TOLERANCE);
    }

    @Test
    void testShiftAllUp() {
        new RedispatchActionWithAutoGlsk(Set.of(), Country.ES).apply(network, 100.0);
        assertEquals(1500. + 15.79, network.getGenerator("EES1AA11_generator").getTargetP(), SHIFT_TOLERANCE);
        assertEquals(3000. + 31.58, network.getGenerator("EES2AA11_generator").getTargetP(), SHIFT_TOLERANCE);
        assertEquals(2500. + 26.32, network.getGenerator("EES3AA11_generator").getTargetP(), SHIFT_TOLERANCE);
        assertEquals(2500. + 26.32, network.getGenerator("EES4AA11_generator").getTargetP(), SHIFT_TOLERANCE);
    }

    @Test
    void testShiftAllDown() {
        new RedispatchActionWithAutoGlsk(Set.of(), Country.ES).apply(network, -200.0);
        assertEquals(1500. - 2 * 15.79, network.getGenerator("EES1AA11_generator").getTargetP(), SHIFT_TOLERANCE);
        assertEquals(3000. - 2 * 31.58, network.getGenerator("EES2AA11_generator").getTargetP(), SHIFT_TOLERANCE);
        assertEquals(2500. - 2 * 26.32, network.getGenerator("EES3AA11_generator").getTargetP(), SHIFT_TOLERANCE);
        assertEquals(2500. - 2 * 26.32, network.getGenerator("EES4AA11_generator").getTargetP(), SHIFT_TOLERANCE);
    }

    @Test
    void testExcludeOneAndShiftUp() {
        // If we exclude EES2AA11_generator, total production is down to 6500 MW. New coefficients are:
        // EES1AA11_generator: 1500 / 6500 = 23.08 %
        // EES3AA11_generator & EES4AA11_generator: 2500 / 6500 = 38.46 %
        new RedispatchActionWithAutoGlsk(Set.of("EES2AA11_generator"), Country.ES).apply(network, 100.0);
        assertEquals(1500. + 23.08, network.getGenerator("EES1AA11_generator").getTargetP(), SHIFT_TOLERANCE);
        assertEquals(3000., network.getGenerator("EES2AA11_generator").getTargetP(), SHIFT_TOLERANCE);
        assertEquals(2500. + 38.46, network.getGenerator("EES3AA11_generator").getTargetP(), SHIFT_TOLERANCE);
        assertEquals(2500. + 38.46, network.getGenerator("EES4AA11_generator").getTargetP(), SHIFT_TOLERANCE);
    }

    @Test
    void testExcludeTwoAndShiftDown() {
        // If we exclude EES1AA11_generator & EES3AA11_generator, total production is down to 5500 MW. New coefficients are:
        // EES2AA11_generator: 3000 / 5500 = 54.55 %
        // EES4AA11_generator: 2500 / 5500 = 45.45 %
        new RedispatchActionWithAutoGlsk(Set.of("EES3AA11_generator", "EES1AA11_generator"), Country.ES).apply(network, -200.0);
        assertEquals(1500., network.getGenerator("EES1AA11_generator").getTargetP(), SHIFT_TOLERANCE);
        assertEquals(3000. - 2 * 54.55, network.getGenerator("EES2AA11_generator").getTargetP(), SHIFT_TOLERANCE);
        assertEquals(2500., network.getGenerator("EES3AA11_generator").getTargetP(), SHIFT_TOLERANCE);
        assertEquals(2500. - 2 * 45.45, network.getGenerator("EES4AA11_generator").getTargetP(), SHIFT_TOLERANCE);
    }
}
