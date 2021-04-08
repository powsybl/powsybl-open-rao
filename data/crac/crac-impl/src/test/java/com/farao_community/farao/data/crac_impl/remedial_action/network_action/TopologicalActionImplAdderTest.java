package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCracFactory;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class TopologicalActionImplAdderTest {

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
        NetworkAction networkAction = networkActionAdder.newTopologicalAction()
            .withNetworkElement("branchNetworkElementId")
            .withActionType(ActionType.OPEN)
            .add()
            .add();

        TopologicalAction topologicalAction = (TopologicalAction) networkAction.getElementaryActions().iterator().next();
        assertEquals("branchNetworkElementId", topologicalAction.getNetworkElement().getId());
        assertEquals(ActionType.OPEN, topologicalAction.getActionType());
    }

    @Test (expected = FaraoException.class)
    public void testNoNetworkElement() {
        networkActionAdder.newTopologicalAction()
            .withActionType(ActionType.OPEN)
            .add()
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testNoActionType() {
        networkActionAdder.newTopologicalAction()
            .withNetworkElement("branchNetworkElementId")
            .add()
            .add();
    }
}
