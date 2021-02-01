package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.Range;
import com.farao_community.farao.data.crac_api.RangeDefinition;
import com.farao_community.farao.data.crac_api.RangeType;
import com.farao_community.farao.data.crac_impl.AbstractRemedialActionTest;
import com.farao_community.farao.data.crac_impl.range_domain.PstRange;
import com.farao_community.farao.data.crac_impl.range_domain.RangeImpl;
import com.powsybl.iidm.network.Network;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
abstract public class AbstractRangeActionTest extends AbstractRemedialActionTest {

    protected List<Range> createRanges() {
        RangeImpl range = Mockito.mock(RangeImpl.class);
        List<Range> ranges = new ArrayList<>();
        ranges.add(range);
        return ranges;
    }

    @Test
    public void getMinAndMaxValueWithMultipleRanges() {
        HvdcRange mockedHvdcRange = Mockito.mock(HvdcRange.class);

        Range range1 = Mockito.mock(RangeImpl.class);
        Range range2 = Mockito.mock(RangeImpl.class);
        mockedHvdcRange.addRange(range1);
        mockedHvdcRange.addRange(range2);

        Network mockedNetwork = Mockito.mock(Network.class);

        double expectedMinRange1 = -5;
        Mockito.when(mockedHvdcRange.getMinValueWithRange(mockedNetwork, range1)).thenReturn(expectedMinRange1);
        assertEquals(mockedHvdcRange.getMinValueWithRange(mockedNetwork, range1), expectedMinRange1, 0);

        double expectedMinRange2 = -10;
        Mockito.when(mockedHvdcRange.getMinValueWithRange(mockedNetwork, range2)).thenReturn(expectedMinRange2);

        // doesnt work for the moment!!! don't know how to test it...
        // assertEquals(Math.max(expectedMinRange1, expectedMinRange2), mockedHvdcRange.getMinValue(mockedNetwork), 0);
    }

    @Test
    public void abstractElementaryEquals() {
        PstRange range = new PstRange(1, 10, RangeType.ABSOLUTE, RangeDefinition.STARTS_AT_ONE);
        AbstractRangeAction pst = new PstWithRange("pst_range_id", new NetworkElement("neID"));
        pst.addRange(range);
        AbstractRangeAction pstRange1 = new PstWithRange("pst_range_id", new NetworkElement("neID"));
        pstRange1.addRange(range);
        assertEquals(pst.hashCode(), pstRange1.hashCode());
        assertEquals(pst, pstRange1);
        AbstractRangeAction pstDifferent = new PstWithRange("pst_range_id_2", new NetworkElement("neOther"));
        pstDifferent.addRange(new PstRange(1, 10, RangeType.RELATIVE_TO_INITIAL_NETWORK, RangeDefinition.STARTS_AT_ONE));
        assertNotEquals(pst.hashCode(), pstDifferent.hashCode());
        assertNotEquals(pst, pstDifferent);
    }

    @Test
    public void testGroupId() {
        AbstractRangeAction pst = new PstWithRange("pst_range_id", new NetworkElement("neID"));
        pst.setGroupId("groupId");
        assertTrue(pst.getGroupId().isPresent());
        assertEquals("groupId", pst.getGroupId().get());
    }
}
