/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.json;

import com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoResult;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.ra_optimisation.json.JsonRaoComputationResult;
import com.powsybl.commons.AbstractConverterTest;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class JsonClosedOptimisationRaoResultTest extends AbstractConverterTest {

    private static final double EPSILON = 1e-4;

    @Test
    public void roundTrip() throws IOException {
        RaoComputationResult result = JsonRaoComputationResult.read(getClass().getResourceAsStream("/closed_optimisation_rao_result.json"));
        roundTripTest(result, JsonRaoComputationResult::write, JsonRaoComputationResult::read, "/closed_optimisation_rao_result.json");
    }

    @Test
    public void testImport() {
        RaoComputationResult result = JsonRaoComputationResult.read(getClass().getResourceAsStream("/closed_optimisation_rao_result.json"));
        ClosedOptimisationRaoResult resultExtension = result.getExtension(ClosedOptimisationRaoResult.class);
        assertNotNull(resultExtension);

        // Check solver info
        assertNotNull(resultExtension.getSolverInfo());
        assertEquals(2, resultExtension.getSolverInfo().getNumVariables());
        assertEquals(1, resultExtension.getSolverInfo().getNumConstraints());
        assertEquals(10, resultExtension.getSolverInfo().getNumIterations());
        assertEquals(10000, resultExtension.getSolverInfo().getWallTime());
        assertEquals("SUCCESS", resultExtension.getSolverInfo().getStatus());

        // Check variables infos
        assertNotNull(resultExtension.getVariableInfos());
        assertEquals(2, resultExtension.getVariableInfos().size());
        assertTrue(resultExtension.getVariableInfos().containsKey("var1"));
        ClosedOptimisationRaoResult.VariableInfo variableInfo1 = resultExtension.getVariableInfos().get("var1");
        assertEquals("var1", variableInfo1.getName());
        assertEquals(10., variableInfo1.getSolutionValue(), EPSILON);
        assertEquals(5., variableInfo1.getLb(), EPSILON);
        assertEquals(7., variableInfo1.getUb(), EPSILON);
        assertTrue(resultExtension.getVariableInfos().containsKey("var2"));
        ClosedOptimisationRaoResult.VariableInfo variableInfo2 = resultExtension.getVariableInfos().get("var2");
        assertEquals("var2", variableInfo2.getName());
        assertEquals(310., variableInfo2.getSolutionValue(), EPSILON);
        assertEquals(Double.NEGATIVE_INFINITY, variableInfo2.getLb(), EPSILON);
        assertEquals(Double.POSITIVE_INFINITY, variableInfo2.getUb(), EPSILON);

        // Check constraints infos
        assertNotNull(resultExtension.getConstraintInfos());
        assertEquals(1, resultExtension.getConstraintInfos().size());
        assertTrue(resultExtension.getConstraintInfos().containsKey("constraint"));
        ClosedOptimisationRaoResult.ConstraintInfo constraintInfo = resultExtension.getConstraintInfos().get("constraint");
        assertEquals("constraint", constraintInfo.getName());
        assertEquals(3.14, constraintInfo.getDualValue(), EPSILON);
        assertEquals(-5., constraintInfo.getLb(), EPSILON);
        assertEquals(5, constraintInfo.getUb(), EPSILON);
        assertEquals("STATUS", constraintInfo.getBasisStatus());
        assertFalse(constraintInfo.isLazy());

        // Check objective info
        assertNotNull(resultExtension.getObjectiveInfo());
        assertTrue(resultExtension.getObjectiveInfo().isMaximization());
        assertEquals(13., resultExtension.getObjectiveInfo().getValue(), EPSILON);
    }

    @Test
    public void roundTripInfeasible() throws IOException {
        RaoComputationResult result = new RaoComputationResult(RaoComputationResult.Status.FAILURE);
        ClosedOptimisationRaoResult resultExtension = new ClosedOptimisationRaoResult();
        resultExtension.setSolverInfo(2, 1, 10, 10000, "INFEASIBLE");
        result.addExtension(ClosedOptimisationRaoResult.class, resultExtension);
        roundTripTest(result, JsonRaoComputationResult::write, JsonRaoComputationResult::read, "/closed_optimisation_rao_result_infeasible.json");
    }
}
