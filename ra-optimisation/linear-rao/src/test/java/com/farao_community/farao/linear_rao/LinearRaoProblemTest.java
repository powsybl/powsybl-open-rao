package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearRaoProblemTest {

    private LinearRaoProblem linearRaoProblem;

    @Before
    public void setUp() {
        linearRaoProblem = new LinearRaoProblem();
    }

    @Test
    public void addCnec() {
        assertNull(linearRaoProblem.lookupVariableOrNull("cnec-test-variable"));
        assertNull(linearRaoProblem.lookupConstraintOrNull("cnec-test-constraint"));
        linearRaoProblem.addCnec("cnec-test", 500, -800, 800);
        assertNotNull(linearRaoProblem.lookupVariableOrNull("cnec-test-variable"));
        assertEquals(800, linearRaoProblem.lookupVariableOrNull("cnec-test-variable").ub(), 1);
        assertNotNull(linearRaoProblem.lookupConstraintOrNull("cnec-test-constraint"));
        assertEquals(-800, linearRaoProblem.lookupVariableOrNull("cnec-test-variable").lb(), 1);
        assertEquals(1, linearRaoProblem.numVariables());
        assertEquals(1, linearRaoProblem.numConstraints());
    }

    @Test
    public void addRangeActionVariable() {
        assertNull(linearRaoProblem.lookupVariableOrNull("positive-range-action-test-network-element-test-variable"));
        assertNull(linearRaoProblem.lookupVariableOrNull("negative-range-action-test-network-element-test-variable"));
        linearRaoProblem.addRangeActionVariable("range-action-test", "network-element-test", 12, 15);
        assertNotNull(linearRaoProblem.lookupVariableOrNull("positive-range-action-test-network-element-test-variable"));
        assertNotNull(linearRaoProblem.lookupVariableOrNull("negative-range-action-test-network-element-test-variable"));
        assertEquals(0, linearRaoProblem.lookupVariableOrNull("positive-range-action-test-network-element-test-variable").lb(), 1);
        assertEquals(15, linearRaoProblem.lookupVariableOrNull("positive-range-action-test-network-element-test-variable").ub(), 1);
        assertEquals(0, linearRaoProblem.lookupVariableOrNull("negative-range-action-test-network-element-test-variable").lb(), 1);
        assertEquals(12, linearRaoProblem.lookupVariableOrNull("negative-range-action-test-network-element-test-variable").ub(), 1);
        assertEquals(2, linearRaoProblem.numVariables());
        assertEquals(0, linearRaoProblem.numConstraints());
    }

    @Test
    public void addRangeActionFlowOnBranch() {
        linearRaoProblem.addCnec("cnec-test", 500, -800, 800);
        linearRaoProblem.addRangeActionVariable("range-action-test", "network-element-test", 12, 15);
        linearRaoProblem.addRangeActionFlowOnBranch("cnec-test", "range-action-test", "network-element-test", 0.2);

        assertEquals(
            -0.2,
            linearRaoProblem.lookupConstraintOrNull("cnec-test-constraint")
                .getCoefficient(linearRaoProblem.lookupVariableOrNull("positive-range-action-test-network-element-test-variable")),
            0.01);

        assertEquals(
            0.2,
            linearRaoProblem.lookupConstraintOrNull("cnec-test-constraint")
                .getCoefficient(linearRaoProblem.lookupVariableOrNull("negative-range-action-test-network-element-test-variable")),
            0.01);
    }

    @Test
    public void addRangeActionFlowOnBranchWithCnecFailure() {
        linearRaoProblem.addRangeActionVariable("range-action-test", "network-element-test", 12, 15);
        try {
            linearRaoProblem.addRangeActionFlowOnBranch("cnec-test", "range-action-test", "network-element-test", 0.2);
        } catch (FaraoException e) {
            assertEquals("Flow variable on cnec-test has not been defined yet.", e.getMessage());
        }
    }

    @Test
    public void addRangeActionFlowOnBranchWithRangeActionFailure() {
        linearRaoProblem.addCnec("cnec-test", 500, -800, 800);
        try {
            linearRaoProblem.addRangeActionFlowOnBranch("cnec-test", "range-action-test", "network-element-test", 0.2);
        } catch (FaraoException e) {
            assertEquals("Range action variable for range-action-test on network-element-test has not been defined yet.", e.getMessage());
        }
    }
}