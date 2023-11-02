package com.farao_community.farao.data.crac_loopflow_extension;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoopFlowThresholdAdderImplTest {
    private FlowCnec flowCnec;

    @BeforeEach
    public void setUp() {

        Crac crac = CracFactory.findDefault().create("cracId", "cracName");
        crac.newInstant("preventive", InstantKind.PREVENTIVE, null);
        flowCnec = crac.newFlowCnec()
            .withId("flowCnecId")
            .withName("flowCnecName")
            .withNetworkElement("networkElementId")
            .withInstant("preventive")
            .withOperator("operator")
            .withOptimized(true)
            .newThreshold()
            .withSide(Side.LEFT)
            .withUnit(Unit.MEGAWATT)
            .withMax(1000.0)
            .withMin(-1000.0)
            .add()
            .add();
    }

    @Test
    void addLoopFlowThreshold() {

        flowCnec.newExtension(LoopFlowThresholdAdder.class)
            .withUnit(Unit.MEGAWATT)
            .withValue(100.0)
            .add();

        LoopFlowThreshold loopFlowThreshold = flowCnec.getExtension(LoopFlowThreshold.class);
        assertNotNull(loopFlowThreshold);
        assertEquals(Unit.MEGAWATT, loopFlowThreshold.getUnit());
        assertEquals(100.0, loopFlowThreshold.getValue(), 1e-3);
    }

    @Test
    void addLoopFlowThresholdNoValue() {
        LoopFlowThresholdAdder loopFlowThresholdAdder = flowCnec.newExtension(LoopFlowThresholdAdder.class)
            .withUnit(Unit.MEGAWATT);
        FaraoException exception = assertThrows(FaraoException.class, loopFlowThresholdAdder::add);
        assertEquals("Cannot add LoopFlowThreshold without a threshold value. Please use withValue() with a non null value", exception.getMessage());
    }

    @Test
    void addLoopFlowThresholdNoUnit() {
        LoopFlowThresholdAdder loopFlowThresholdAdder = flowCnec.newExtension(LoopFlowThresholdAdder.class)
            .withValue(100.0);
        FaraoException exception = assertThrows(FaraoException.class, loopFlowThresholdAdder::add);
        assertEquals("Cannot add LoopFlowThreshold without a threshold unit. Please use withUnit() with a non null value", exception.getMessage());
    }

    @Test
    void addLoopFlowThresholdNegativeThreshold() {
        LoopFlowThresholdAdder loopFlowThresholdAdder = flowCnec.newExtension(LoopFlowThresholdAdder.class)
            .withUnit(Unit.MEGAWATT)
            .withValue(-100.0);
        FaraoException exception = assertThrows(FaraoException.class, loopFlowThresholdAdder::add);
        assertEquals("LoopFlowThresholds must have a positive threshold.", exception.getMessage());
    }

    @Test
    void addLoopFlowThresholdPercentGreaterThanOne() {
        LoopFlowThresholdAdder loopFlowThresholdAdder = flowCnec.newExtension(LoopFlowThresholdAdder.class)
            .withUnit(Unit.PERCENT_IMAX)
            .withValue(25);
        FaraoException exception = assertThrows(FaraoException.class, loopFlowThresholdAdder::add);
        assertEquals("LoopFlowThresholds in Unit.PERCENT_IMAX must be defined between 0 and 1, where 1 = 100%.", exception.getMessage());
    }
}
