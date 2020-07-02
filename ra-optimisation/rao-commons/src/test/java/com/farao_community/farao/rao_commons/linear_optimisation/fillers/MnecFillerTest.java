package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Direction;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class MnecFillerTest extends AbstractFillerTest {

    private MnecFiller mnecFiller;
    private Cnec mnec1;
    private Cnec mnec2;
    private double mnec1MaxFlow = 0; // TO DO //mnec1_max_flow = max(1000, f0 + 50) - 0;
    private double mnec1MinFlow = 0; // TO DO //mnec1_min_flow = min(-1000, f0 - 50) + 0;
    private double mnec2MaxFlow = 0; // TO DO
    private double mnec2MinFlow = 0; // TO DO


    @Before
    public void setUp() {
        init();
        coreProblemFiller = new CoreProblemFiller();
        mnecFiller = new MnecFiller();
        crac.newCnec().setId("MNEC1 - N - preventive")
                .newNetworkElement().setId("DDE2AA1  NNL3AA1  1").add()
                .newThreshold().setDirection(Direction.BOTH).setSide(Side.BOTH).setMaxValue(1000.0).setUnit(Unit.MEGAWATT).add()
                .setOptimized(false).setMonitored(true)
                .setInstant(crac.getInstant("N"))
                .add();
        mnec1 = crac.getCnec("MNEC1 - N - preventive");
        crac.newCnec().setId("MNEC2 - N - preventive")
                .newNetworkElement().setId("NNL2AA1  BBE3AA1  1").add()
                .newThreshold().setDirection(Direction.BOTH).setSide(Side.BOTH).setMaxValue(100.0).setUnit(Unit.MEGAWATT).add()
                .setOptimized(false).setMonitored(true)
                .setInstant(crac.getInstant("N"))
                .add();
        mnec2 = crac.getCnec("MNEC2 - N - preventive");
    }

    private void fillProblemWithFiller() {
        // fill the problem : the core filler is required
        coreProblemFiller.fill(raoData, linearProblem);
        mnecFiller.fill(raoData, linearProblem);
    }

    @Test
    public void testAddMnecViolationVariables() {
        fillProblemWithFiller();
        crac.getCnecs().forEach(cnec -> {
            MPVariable variable = linearProblem.getMnecViolationVariable(cnec);
            if (cnec.isMonitored()) {
                assertNotNull(variable);
                assertEquals(0, variable.lb(), DOUBLE_TOLERANCE);
                assertEquals(Double.POSITIVE_INFINITY, variable.ub(), DOUBLE_TOLERANCE);
            } else {
                assertNull(variable);
            }
        });
    }

    @Test
    public void testAddMnecMinFlowConstraints() {
        fillProblemWithFiller();
        crac.getCnecs().stream().filter(cnec -> !cnec.isMonitored()).forEach(cnec -> {
            assertNull(linearProblem.getMnecFlowConstraint(cnec, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        });
        /*
        MPConstraint ct1_max = linearProblem.getMnecFlowConstraint(mnec1, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(ct1_max);
        assertEquals(Double.NEGATIVE_INFINITY, ct1_max.lb(), DOUBLE_TOLERANCE);
        assertEquals(mnec1_max_flow, ct1_max.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, ct1_max.getCoefficient(linearProblem.getFlowVariable(mnec1)), DOUBLE_TOLERANCE);
        assertEquals(-1, ct1_max.getCoefficient(linearProblem.getMnecViolationVariable(mnec1)), DOUBLE_TOLERANCE);
        MPConstraint ct1_min = linearProblem.getMnecFlowConstraint(mnec1, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(ct1_min);
        assertEquals(mnec1_min_flow, ct1_min.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, ct1_min.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, ct1_min.getCoefficient(linearProblem.getFlowVariable(mnec1)), DOUBLE_TOLERANCE);
        assertEquals(1, ct1_min.getCoefficient(linearProblem.getMnecViolationVariable(mnec1)), DOUBLE_TOLERANCE);

        MPConstraint ct2_max = linearProblem.getMnecFlowConstraint(mnec2, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(ct2_max);
        assertEquals(Double.NEGATIVE_INFINITY, ct2_max.lb(), DOUBLE_TOLERANCE);
        assertEquals(mnec2_max_flow, ct2_max.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, ct2_max.getCoefficient(linearProblem.getFlowVariable(mnec2)), DOUBLE_TOLERANCE);
        assertEquals(-1, ct2_max.getCoefficient(linearProblem.getMnecViolationVariable(mnec2)), DOUBLE_TOLERANCE);
        MPConstraint ct2_min = linearProblem.getMnecFlowConstraint(mnec2, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(ct2_min);
        assertEquals(mnec2_min_flow, ct2_min.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, ct2_min.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, ct2_min.getCoefficient(linearProblem.getFlowVariable(mnec2)), DOUBLE_TOLERANCE);
        assertEquals(1, ct2_min.getCoefficient(linearProblem.getMnecViolationVariable(mnec2)), DOUBLE_TOLERANCE);
        */
    }

}
