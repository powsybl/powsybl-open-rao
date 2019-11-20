package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkElement;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class SimpleStateTest {
    private SimpleState state;

    @Before
    public void create() {
        NetworkElement networkElement = new NetworkElement("basicElemId", "basicElemName");
        state =  new SimpleState(
                Optional.of(new ComplexContingency("contingencyId", "contingencyName", new ArrayList<>(Arrays.asList(networkElement)))),
                new Instant(12)
        );
    }

    @Test
    public void getInstant() {
        assertEquals(12, state.getInstant().getDuration(), 0.1);
    }

    @Test
    public void setInstant() {
        state.setInstant(new Instant(5));
        assertEquals(5, state.getInstant().getDuration(), 0.1);
    }

    @Test
    public void getContingency() {
    }

    @Test
    public void setContingency() {
    }
}
