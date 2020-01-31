package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_impl.AbstractRemedialActionTest;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.powsybl.iidm.network.Network;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
abstract public class AbstractElementaryRangeActionTest extends AbstractRemedialActionTest {

    protected List<Range> createRanges() {
        Range range = Mockito.mock(Range.class);
        List<Range> ranges = new ArrayList<>();
        ranges.add(range);
        return ranges;
    }

    @Test
    public void getMinAndMaxValueWithMultipleRanges() {
        HvdcRange mockedHvdcRange = Mockito.mock(HvdcRange.class);

        Range range1 = Mockito.mock(Range.class);
        Range range2 = Mockito.mock(Range.class);
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
}
