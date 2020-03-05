package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PstRangeResultTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void getTap() {
    }

    @Test
    public void setTap() {
    }

    @Test
    public void setSetPoint() {
    }

    @Test
    public void addExtension() {
        PstRange pstRange = new PstWithRange("pst", new NetworkElement("ne"));
        State state = new SimpleState(Optional.empty(), new Instant("initial", 0));
        PstRangeResult pstRangeResult = new PstRangeResult(Collections.singleton(state));
        pstRange.addExtension(PstRangeResult.class, pstRangeResult);

        pstRange.getExtension(PstRangeResult.class).setTap(state, 15);
        assertEquals(15, pstRange.getExtension(PstRangeResult.class).getTap(state));
    }
}
