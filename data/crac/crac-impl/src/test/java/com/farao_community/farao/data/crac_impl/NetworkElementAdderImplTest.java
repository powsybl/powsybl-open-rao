package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.threshold.AbstractFlowThreshold;
import com.powsybl.iidm.network.Branch;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import java.util.Collections;
import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class NetworkElementAdderImplTest {

    private Cnec cnec;
    private Crac crac;
    private Contingency contingency;

    @Before
    public void setUp() {
        crac = (new SimpleCracFactory()).create("test-crac");

        // mock threshold
        AbstractFlowThreshold threshold = Mockito.mock(AbstractFlowThreshold.class);
        Mockito.when(threshold.copy()).thenReturn(threshold);
        Mockito.when(threshold.getBranchSide()).thenReturn(Branch.Side.ONE);
        State state = Mockito.mock(State.class);

        // arrange Cnecs
        cnec = new SimpleCnec("cnec1", new NetworkElement("FRANCE_BELGIUM_1"), Collections.singleton(threshold), state);

        // init contingency
        contingency = new ComplexContingency("test-contingecy");
    }

    @Test
    public void testCnecAddNetworkElement() {
        Cnec cnec2 = cnec.newNetworkElement()
                .setId("neID")
                .setName("neName")
                .add();
        assertNotNull(cnec.getNetworkElement());
        assertEquals("neID", cnec.getNetworkElement().getId());
        assertEquals("neName", cnec.getNetworkElement().getName());
        assertSame(cnec, cnec2);
    }

    @Test
    public void testCnecAddNetworkElementNoName() {
        cnec.newNetworkElement()
                .setId("neID")
                .add();
        assertNotNull(cnec.getNetworkElement());
        assertEquals("neID", cnec.getNetworkElement().getId());
        assertEquals("neID", cnec.getNetworkElement().getName());
    }

    @Test(expected = FaraoException.class)
    public void testCnecAddNetworkElementNoIdFail() {
        cnec.newNetworkElement()
                .setName("neName")
                .add();
    }

    @Test
    public void testCracAddNetworkElement() {
        Crac crac2 = crac.newNetworkElement()
                .setId("neID")
                .setName("neName")
                .add();
        Crac crac3 = crac.newNetworkElement()
                .setId("neID2")
                .add();
        assertEquals(2, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("neID"));
        assertEquals("neName", crac.getNetworkElement("neID").getName());
        assertNotNull(crac.getNetworkElement("neID2"));
        assertEquals("neID2", crac.getNetworkElement("neID2").getName());
        assertSame(crac, crac2);
        assertSame(crac, crac3);
    }

    @Test(expected = FaraoException.class)
    public void testCracAddNetworkElementNoIdFail() {
        crac.newNetworkElement()
                .setName("neName")
                .add();
    }

    @Test(expected = NullPointerException.class)
    public void testNullParentFail() {
        NetworkElementAdder tmp = new NetworkElementAdderImpl(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullIdFail() {
        crac.newNetworkElement().setId(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullNameFail() {
        crac.newNetworkElement().setName(null);
    }
}
