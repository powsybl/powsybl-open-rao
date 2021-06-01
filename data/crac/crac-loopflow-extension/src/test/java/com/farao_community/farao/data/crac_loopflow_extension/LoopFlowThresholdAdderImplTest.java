package com.farao_community.farao.data.crac_loopflow_extension;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public class LoopFlowThresholdAdderImplTest {

    private FlowCnec flowCnec;

    @Before
    public void setUp() {

        Crac crac = CracFactory.findDefault().create("cracId", "cracName");
        flowCnec = crac.newFlowCnec()
            .withId("flowCnecId")
            .withName("flowCnecName")
            .withNetworkElement("networkElementId")
            .withInstant(Instant.PREVENTIVE)
            .withOperator("operator")
            .withOptimized(true)
            .newThreshold()
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withUnit(Unit.MEGAWATT)
                .withMax(1000.0)
                .withMin(-1000.0)
                .add()
            .add();
    }

    @Test
    public void addLoopFlowThreshold() {

        flowCnec.newExtension(LoopFlowThresholdAdder.class)
            .withUnit(Unit.MEGAWATT)
            .withValue(100.0)
            .add();

        LoopFlowThreshold loopFlowThreshold = flowCnec.getExtension(LoopFlowThreshold.class);
        assertNotNull(loopFlowThreshold);
        assertEquals(Unit.MEGAWATT, loopFlowThreshold.getUnit());
        assertEquals(100.0, loopFlowThreshold.getValue(), 1e-3);
    }

    @Test (expected = FaraoException.class)
    public void addLoopFlowThresholdNoValue() {
        flowCnec.newExtension(LoopFlowThresholdAdder.class)
            .withUnit(Unit.MEGAWATT)
            .add();
    }

    @Test (expected = FaraoException.class)
    public void addLoopFlowThresholdNoUnit() {
        flowCnec.newExtension(LoopFlowThresholdAdder.class)
            .withValue(100.0)
            .add();
    }

    @Test (expected = FaraoException.class)
    public void addLoopFlowThresholdNegativeThreshold() {
        flowCnec.newExtension(LoopFlowThresholdAdder.class)
            .withUnit(Unit.MEGAWATT)
            .withValue(-100.0)
            .add();
    }

    @Test (expected = FaraoException.class)
    public void addLoopFlowThresholdPercentGreaterThanOne() {
        flowCnec.newExtension(LoopFlowThresholdAdder.class)
            .withUnit(Unit.PERCENT_IMAX)
            .withValue(25)
            .add();
    }
}
