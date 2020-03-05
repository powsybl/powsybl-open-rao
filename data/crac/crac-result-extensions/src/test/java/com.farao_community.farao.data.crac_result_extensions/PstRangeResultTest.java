package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PstRangeResultTest {
    private static final double EPSILON = 0.1;
    private PstRange pstRange;
    private State state;
    private PstRangeResult pstRangeResult;

    @Before
    public void setUp() {
        pstRange = new PstWithRange("id", new NetworkElement("ne"));
        state = new SimpleState(Optional.empty(), new Instant("initial", 0));
        pstRangeResult = new PstRangeResult(Collections.singleton(state));
    }

    @Test
    public void setSetPoint() {
        PstRange pstRangeMock = Mockito.mock(PstRange.class);
        Mockito.when(pstRangeMock.computeTapPosition(3.2)).thenReturn(5);
        Mockito.when(pstRangeMock.isSynchronized()).thenReturn(true);
        pstRangeResult.setExtendable(pstRangeMock);
        pstRangeResult.setSetPoint(state, 3.2);
        assertEquals(3.2, pstRangeResult.getSetPoint(state), EPSILON);
        assertEquals(5, pstRangeResult.getTap(state));
    }

    @Test
    public void addExtension() {
        pstRange.addExtension(PstRangeResult.class, pstRangeResult);
        pstRangeResult.setTap(state, 15);
        assertEquals(15, pstRange.getExtension(PstRangeResult.class).getTap(state));
    }
}
