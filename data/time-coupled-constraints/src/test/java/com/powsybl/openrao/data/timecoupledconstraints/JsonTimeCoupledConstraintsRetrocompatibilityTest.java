/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.timecoupledconstraints;

import com.powsybl.openrao.data.timecoupledconstraints.io.JsonTimeCoupledConstraints;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Atena Amnache {@literal <atena.amnache at rte-france.com>}
 */
class JsonTimeCoupledConstraintsRetrocompatibilityTest {

    @Test
    void testV1Point0() throws IOException {
        TimeCoupledConstraints timeCoupledConstraints = JsonTimeCoupledConstraints.read(getClass().getResourceAsStream("/retrocompatibility/v1.0/time-coupled-constraints-v1.0.json"));

        testBaseContentOfV1Point0TimeCoupledConstraints(timeCoupledConstraints);
        assertTrue(timeCoupledConstraints.getPstConstraints().isEmpty());
    }

    @Test
    void testV1Point1() throws IOException {
        // PST constraints were added for v1.1 + minOffTime for generator constraints
        TimeCoupledConstraints timeCoupledConstraints = JsonTimeCoupledConstraints.read(getClass().getResourceAsStream("/retrocompatibility/v1.1/time-coupled-constraints-v1.1.json"));

        testBaseContentOfV1Point1TimeCoupledConstraints(timeCoupledConstraints);
    }

    private void testBaseContentOfV1Point0TimeCoupledConstraints(TimeCoupledConstraints timeCoupledConstraints) {
        assertEquals(1, timeCoupledConstraints.getGeneratorConstraints().size());
        GeneratorConstraints generatorConstraints = timeCoupledConstraints.getGeneratorConstraints().iterator().next();
        assertEquals("generator-1", generatorConstraints.getGeneratorId());
        assertEquals(Optional.of(1.15), generatorConstraints.getLeadTime());
        assertEquals(Optional.of(2.0), generatorConstraints.getLagTime());
        assertEquals(Optional.of(100.0), generatorConstraints.getUpwardPowerGradient());
        assertTrue(generatorConstraints.isShutDownAllowed());
        assertTrue(generatorConstraints.isStartUpAllowed());
    }

    private void testBaseContentOfV1Point1TimeCoupledConstraints(TimeCoupledConstraints timeCoupledConstraints) {
        testBaseContentOfV1Point0TimeCoupledConstraints(timeCoupledConstraints);
        GeneratorConstraints generatorConstraints = timeCoupledConstraints.getGeneratorConstraints().iterator().next();
        assertEquals(Optional.of(1.), generatorConstraints.getMinOffTime());

        assertEquals(1, timeCoupledConstraints.getPstConstraints().size());
        PstConstraints pstConstraints = timeCoupledConstraints.getPstConstraints().iterator().next();
        assertEquals("pst-1", pstConstraints.getPstId());
        assertEquals(Optional.of(1), pstConstraints.getUpwardTapGradient());
        assertEquals(Optional.of(-1), pstConstraints.getDownwardTapGradient());
    }
}
