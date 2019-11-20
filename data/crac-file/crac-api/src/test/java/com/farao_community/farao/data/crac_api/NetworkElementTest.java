package com.farao_community.farao.data.crac_api;

import org.junit.Test;

import static org.junit.Assert.*;

public class NetworkElementTest {

    @Test
    public void testConstructoElementTest() {
        NetworkElement networkElement = new NetworkElement("basicElemId", "basicElemName");
        assertEquals("basicElemId", networkElement.getId());
        assertEquals("basicElemName", networkElement.getName());
        assertEquals("basicElemId", networkElement.toString());
    }

}
