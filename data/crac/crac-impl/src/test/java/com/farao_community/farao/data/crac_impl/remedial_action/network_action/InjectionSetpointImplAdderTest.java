package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCracFactory;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class InjectionSetpointImplAdderTest {

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
        NetworkAction networkAction = networkActionAdder.newInjectionSetPoint()
            .withNetworkElement("groupNetworkElementId")
            .withSetpoint(100.)
            .add()
            .add();

        InjectionSetpoint injectionSetpoint = (InjectionSetpoint) networkAction.getElementaryActions().iterator().next();
        assertEquals("groupNetworkElementId", injectionSetpoint.getNetworkElement().getId());
        assertEquals(100., injectionSetpoint.getSetpoint(), 1e-3);
    }

    @Test (expected = FaraoException.class)
    public void testNoNetworkElement() {
        networkActionAdder.newInjectionSetPoint()
            .withSetpoint(100.)
            .add()
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testNoSetpoint() {
        networkActionAdder.newInjectionSetPoint()
            .withNetworkElement("groupNetworkElementId")
            .add()
            .add();
    }
}
