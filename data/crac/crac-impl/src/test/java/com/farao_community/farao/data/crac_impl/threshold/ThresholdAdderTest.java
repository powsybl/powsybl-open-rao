package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ThresholdAdderTest {
    private SimpleCrac crac;
    private State state;
    private static final double DOUBLE_TOLERANCE = 1e-6;

    @Before
    public void setUp() {
        crac = new SimpleCrac("test-crac");
        Contingency contingency = crac.newContingency().setId("conId").add();
        Instant instant = crac.newInstant().setId("instId").setSeconds(10).add();
        state = crac.newState().setContingency(contingency).setInstant(instant).add();
    }

    @Test
    public void testAddThresholdInMW() {
        Cnec cnec = crac.newCnec()
                .setId("test-cnec").setState(state)
                .newNetworkElement().setId("neID").add()
                .newThreshold()
                .setUnit(Unit.MEGAWATT)
                .setMaxValue(1000.0)
                .setSide(Side.LEFT)
                .setDirection(Direction.DIRECT)
                .add()
                .newThreshold()
                .setUnit(Unit.MEGAWATT)
                .setMaxValue(250.0)
                .setSide(Side.LEFT)
                .setDirection(Direction.OPPOSITE)
                .add()
                .add();
        assertEquals(1000.0, (double) cnec.getMaxThreshold(Unit.MEGAWATT).get(), DOUBLE_TOLERANCE);
        assertEquals(-250.0, (double) cnec.getMinThreshold(Unit.MEGAWATT).get(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testAddThresholdInA() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Cnec cnec = crac.newCnec()
                .setId("test-cnec").setState(state)
                .newNetworkElement().setId("BBE1AA1  BBE2AA1  1").add()
                .newThreshold()
                .setUnit(Unit.AMPERE)
                .setMaxValue(1000.0)
                .setSide(Side.LEFT)
                .setDirection(Direction.BOTH)
                .add()
                .add();
        cnec.synchronize(network);
        assertEquals(1000.0, (double) cnec.getMaxThreshold(Unit.AMPERE).get(), DOUBLE_TOLERANCE);
        assertEquals(-1000.0, (double) cnec.getMinThreshold(Unit.AMPERE).get(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testAddThresholdInPercent() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        String lineId = "BBE1AA1  BBE2AA1  1";
        double lineLimit = network.getLine(lineId).getCurrentLimits1().getPermanentLimit();
        Cnec cnec = crac.newCnec()
                .setId("test-cnec").setState(state)
                .newNetworkElement().setId(lineId).add()
                .newThreshold()
                .setUnit(Unit.PERCENT)
                .setMaxValue(50.0)
                .setSide(Side.LEFT)
                .setDirection(Direction.DIRECT)
                .add()
                .newThreshold()
                .setUnit(Unit.PERCENT)
                .setMaxValue(80.0)
                .setSide(Side.LEFT)
                .setDirection(Direction.OPPOSITE)
                .add()
                .add();
        cnec.synchronize(network);
        assertEquals(0.5 * lineLimit, (double) cnec.getMaxThreshold(Unit.AMPERE).get(), DOUBLE_TOLERANCE);
        assertEquals(-0.8 * lineLimit, (double) cnec.getMinThreshold(Unit.AMPERE).get(), DOUBLE_TOLERANCE);
    }

    @Test(expected = NullPointerException.class)
    public void testNullParentFail() {
        ThresholdAdderImpl tmp = new ThresholdAdderImpl(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullUnitFail() {
        crac.newCnec().newThreshold().setUnit(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullMaxValueFail() {
        crac.newCnec().newThreshold().setMaxValue(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullSideFail() {
        crac.newCnec().newThreshold().setSide(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullDirectionFail() {
        crac.newCnec().newThreshold().setDirection(null);
    }

    @Test(expected = FaraoException.class)
    public void testUnsupportedUnitFail() {
        crac.newCnec().newThreshold().setUnit(Unit.KILOVOLT);
    }

    @Test(expected = FaraoException.class)
    public void testNoUnitFail() {
        crac.newCnec().newThreshold()
                .setMaxValue(1000.0)
                .setSide(Side.LEFT)
                .setDirection(Direction.BOTH)
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoValueFail() {
        crac.newCnec().newThreshold()
                .setUnit(Unit.AMPERE)
                .setSide(Side.LEFT)
                .setDirection(Direction.BOTH)
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoSideFail() {
        crac.newCnec().newThreshold()
                .setUnit(Unit.AMPERE)
                .setMaxValue(1000.0)
                .setDirection(Direction.BOTH)
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoDirectionFail() {
        crac.newCnec().newThreshold()
                .setUnit(Unit.AMPERE)
                .setMaxValue(1000.0)
                .setSide(Side.LEFT)
                .add();
    }
}
