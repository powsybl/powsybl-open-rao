package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_impl.range_domain.AbstractRange;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class RedispatchingTest extends AbstractNetworkElementRangeActionTest {

    private Redispatching redispatching;
    private NetworkElement generator;
    private double minPower = 0;
    private double maxPower = 200;
    private double targetPower = 100;
    private double startupCost = 1;
    private double marginalCost = 2;

    @Before
    public void setUp() throws Exception {
        generator = Mockito.mock(NetworkElement.class);
        redispatching = new Redispatching(
                "rd_id",
                "rd_name",
                "rd_operator",
                createUsageRules(),
                createRanges(),
                minPower,
                maxPower,
                targetPower,
                startupCost,
                marginalCost,
                generator
        );
    }

    @Test
    public void basicTests() {
        redispatching.setMinimumPower(redispatching.getMinimumPower() + 1);
        redispatching.setMaximumPower(redispatching.getMaximumPower() + 1);
        redispatching.setTargetPower(redispatching.getTargetPower() + 1);
        redispatching.setStartupCost(redispatching.getStartupCost() + 1);
        redispatching.setMarginalCost(redispatching.getMarginalCost() + 1);
    }

    @Test
    public void apply() {
    }

    @Test
    public void getNetworkElements() {
    }

    @Test
    public void getMinAndMaxValueWithRange() {
        AbstractRange mockedAbstractRange = Mockito.mock(AbstractRange.class);
        Network mockedNetwork = Mockito.mock(Network.class);
        assertEquals(0, redispatching.getMinValueWithRange(mockedNetwork, mockedAbstractRange), 0);
        assertEquals(0, redispatching.getMaxValueWithRange(mockedNetwork, mockedAbstractRange), 0);
    }

    @Test
    public void testGetMinValue() {
    }

    @Test
    public void testApply() {
    }

    @Test
    public void testGetNetworkElements() {
    }
}
