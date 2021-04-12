package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.range_action.Range;
import com.farao_community.farao.data.crac_api.TapConvention;
import com.farao_community.farao.data.crac_api.range_action.RangeType;
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
        // todo : review this test

        /*
        HvdcRange mockedHvdcRange = Mockito.mock(TapRangeImpl.class);

        Range range1 = Mockito.mock(RangeImpl.class);
        Range range2 = Mockito.mock(RangeImpl.class);
        mockedHvdcRange.addRange(range1);
        mockedHvdcRange.addRange(range2);

        Network mockedNetwork = Mockito.mock(Network.class);
        double randomPrePerimeterValue = 5;

        double expectedMinRange1 = -5;
        Mockito.when(mockedHvdcRange.getMinValueWithRange(mockedNetwork, range1, randomPrePerimeterValue)).thenReturn(expectedMinRange1);
        assertEquals(mockedHvdcRange.getMinValueWithRange(mockedNetwork, range1, randomPrePerimeterValue), expectedMinRange1, 0);

        double expectedMinRange2 = -10;
        Mockito.when(mockedHvdcRange.getMinValueWithRange(mockedNetwork, range2, randomPrePerimeterValue)).thenReturn(expectedMinRange2);

        // doesnt work for the moment!!! don't know how to test it...
        // assertEquals(Math.max(expectedMinRange1, expectedMinRange2), mockedHvdcRange.getMinValue(mockedNetwork), 0);

         */
    }

    @Test
    public void abstractElementaryEquals() {
        TapRangeImpl range = new TapRangeImpl(1, 10, RangeType.ABSOLUTE, TapConvention.STARTS_AT_ONE);
        AbstractRangeAction pst = new PstRangeActionImpl("pst_range_id", new NetworkElement("neID"));
        pst.addRange(range);
        AbstractRangeAction pstRange1 = new PstRangeActionImpl("pst_range_id", new NetworkElement("neID"));
        pstRange1.addRange(range);
        assertEquals(pst.hashCode(), pstRange1.hashCode());
        assertEquals(pst, pstRange1);
        AbstractRangeAction pstDifferent = new PstRangeActionImpl("pst_range_id_2", new NetworkElement("neOther"));
        pstDifferent.addRange(new TapRangeImpl(1, 10, RangeType.RELATIVE_TO_INITIAL_NETWORK, TapConvention.STARTS_AT_ONE));
        assertNotEquals(pst.hashCode(), pstDifferent.hashCode());
        assertNotEquals(pst, pstDifferent);
    }

    @Test
    public void testGroupId() {
        AbstractRangeAction pst = new PstRangeActionImpl("pst_range_id", new NetworkElement("neID"));
        pst.setGroupId("groupId");
        assertTrue(pst.getGroupId().isPresent());
        assertEquals("groupId", pst.getGroupId().get());
    }
}
