package com.farao_community.farao.data.crac_result_extensions;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class CnecResultTest {

    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    public void testConstructorWithTwoArguments() {
        CnecResult cnecResult = new CnecResult(50.0, 75.0);
        assertEquals(50.0, cnecResult.getFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(75.0, cnecResult.getFlowInA(), DOUBLE_TOLERANCE);

        cnecResult.setFlowInMW(150.0);
        cnecResult.setFlowInA(175.0);

        assertEquals(150.0, cnecResult.getFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(175.0, cnecResult.getFlowInA(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testConstructorWithOneArgument() {
        CnecResult cnecResult = new CnecResult(-45.0);
        assertEquals(-45.0, cnecResult.getFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(Double.NaN, cnecResult.getFlowInA(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testDefaultConstructor() {
        CnecResult cnecResult = new CnecResult();
        assertEquals(Double.NaN, cnecResult.getFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(Double.NaN, cnecResult.getFlowInA(), DOUBLE_TOLERANCE);
    }
}
