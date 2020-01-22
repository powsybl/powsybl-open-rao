package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_impl.AbstractRemedialActionTest;
import com.farao_community.farao.data.crac_impl.range_domain.AbstractRange;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
abstract public class AbstractNetworkElementRangeActionTest extends AbstractRemedialActionTest {

    protected ArrayList<AbstractRange> createRanges() {
        AbstractRange range = Mockito.mock(AbstractRange.class);
        ArrayList<AbstractRange>ranges = new ArrayList<>();
        ranges.add(range);
        return ranges;
    }

    @Test
    public void getMinValue() {
    }

    @Test
    public void getMaxValue() {
    }

    @Test
    public void getNetworkElement() {
    }

    @Test
    public void getNetworkElements() {
    }

    @Test
    public void setNetworkElement() {
    }
}
