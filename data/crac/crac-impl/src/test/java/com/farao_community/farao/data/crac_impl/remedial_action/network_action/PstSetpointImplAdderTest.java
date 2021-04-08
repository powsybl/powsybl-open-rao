package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCracFactory;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class PstSetpointImplAdderTest {

    private NetworkActionAdder networkActionAdder;

    @Before
    public void setUp() {
        Crac crac = new SimpleCracFactory().create("cracId");
        networkActionAdder = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator");
    }

    @Test
    public void testOk() {

        NetworkAction networkAction = networkActionAdder.newPstSetPoint()
            .withNetworkElement("pstNetworkElementId")
            .withSetpoint(0)
            .withRangeDefinition(RangeDefinition.STARTS_AT_ONE)
            .add()
            .add();

        PstSetpoint pstSetpoint = (PstSetpoint) networkAction.getElementaryActions().iterator().next();
        assertEquals("pstNetworkElementId", pstSetpoint.getNetworkElement().getId());
        assertEquals(0, pstSetpoint.getSetpoint(), 1e-3);
        assertEquals(RangeDefinition.STARTS_AT_ONE, pstSetpoint.getRangeDefinition());
    }

    @Test (expected = FaraoException.class)
    public void testNoNetworkElement() {
        networkActionAdder.newPstSetPoint()
            .withSetpoint(0)
            .withRangeDefinition(RangeDefinition.STARTS_AT_ONE)
            .add()
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testNoSetpoint() {
        networkActionAdder.newPstSetPoint()
            .withNetworkElement("pstNetworkElementId")
            .withRangeDefinition(RangeDefinition.STARTS_AT_ONE)
            .add()
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testNoRangeDefinition() {
        networkActionAdder.newPstSetPoint()
            .withNetworkElement("pstNetworkElementId")
            .withSetpoint(0)
            .add()
            .add();
    }
}
