/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class LinearRaoSkeletonTest {
    @Test
    public void test() {
        LinearRaoProblem linearRaoProblem = new LinearRaoProblem(1, true, new HashMap<>(), new HashMap<>(), new HashMap<>());
        assertNull(linearRaoProblem.flowVariable(""));
        assertNotNull(linearRaoProblem.flowVariables());
        assertNull(linearRaoProblem.positivePstShiftVariable(""));
        assertNotNull(linearRaoProblem.positivePstShiftVariables());
        assertNull(linearRaoProblem.negativePstShiftVariable(""));
        assertNotNull(linearRaoProblem.negativePstShiftVariables());

        LinearRaoModeller linearRaoModeller = new LinearRaoModeller(null, null, null, null);
        linearRaoModeller.buildProblem();
        linearRaoModeller.updateProblem(null);
        linearRaoModeller.solve();

        LinearRaoData linearRaoData = new LinearRaoData(null, null, null);
        linearRaoData.getCrac();
        linearRaoData.getNetwork();
        assertEquals(0.0, linearRaoData.getReferenceFlow(null), 1e-10);
        assertEquals(0.0, linearRaoData.getSensitivity(null, null), 1e-10);
        assertEquals(0.0, linearRaoData.getReferenceFlow(null), 1e-10);
    }
}
