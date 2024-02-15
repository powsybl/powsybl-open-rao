package com.powsybl.openrao.data.cracapi;

import com.powsybl.openrao.commons.OpenRaoException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RaUsageLimitsTest {
    private final RaUsageLimits raUsageLimits = new RaUsageLimits();

    @Test
    void testNominalBehavior() {
        // check default values
        assertEquals(Integer.MAX_VALUE, raUsageLimits.getMaxRa());
        assertEquals(Integer.MAX_VALUE, raUsageLimits.getMaxTso());
        assertTrue(raUsageLimits.getMaxRaPerTso().isEmpty());
        assertTrue(raUsageLimits.getMaxPstPerTso().isEmpty());
        assertTrue(raUsageLimits.getMaxTopoPerTso().isEmpty());
        // set regular values
        raUsageLimits.setMaxRa(4);
        assertEquals(4, raUsageLimits.getMaxRa());
        raUsageLimits.setMaxTso(4);
        assertEquals(4, raUsageLimits.getMaxTso());
        Map<String, Integer> pstMap = Map.of("FR", 4, "DE", 5);
        raUsageLimits.setMaxPstPerTso(pstMap);
        assertEquals(pstMap, raUsageLimits.getMaxPstPerTso());
        Map<String, Integer> topoMap = Map.of("FR", 2, "DE", 0);
        raUsageLimits.setMaxTopoPerTso(topoMap);
        assertEquals(topoMap, raUsageLimits.getMaxTopoPerTso());
        Map<String, Integer> raMap = Map.of("FR", 7, "DE", 10);
        raUsageLimits.setMaxRaPerTso(raMap);
        assertEquals(raMap, raUsageLimits.getMaxRaPerTso());
    }

    @Test
    void testIllegalValues() {
        // negative values
        raUsageLimits.setMaxTso(-2);
        assertEquals(0, raUsageLimits.getMaxTso());
        raUsageLimits.setMaxRa(-2);
        assertEquals(0, raUsageLimits.getMaxRa());
        // incoherent parameters for topo
        raUsageLimits.setMaxTopoPerTso(Map.of("FR", 5));
        Map<String, Integer> illegalMap = Map.of("FR", 3);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> raUsageLimits.setMaxRaPerTso(illegalMap));
        assertEquals("TSO FR has a maximum number of allowed CRAs smaller than the number of allowed topological CRAs. This is not supported.", exception.getMessage());
        raUsageLimits.setMaxTopoPerTso(Map.of("FR", 1));
        // incoherent parameters for pst
        raUsageLimits.setMaxPstPerTso(Map.of("FR", 5));
        exception = assertThrows(OpenRaoException.class, () -> raUsageLimits.setMaxRaPerTso(illegalMap));
        assertEquals("TSO FR has a maximum number of allowed CRAs smaller than the number of allowed PST CRAs. This is not supported.", exception.getMessage());
        // fill values with null maps
        raUsageLimits.setMaxPstPerTso(null);
        assertTrue(raUsageLimits.getMaxPstPerTso().isEmpty());
        raUsageLimits.setMaxTopoPerTso(null);
        assertTrue(raUsageLimits.getMaxTopoPerTso().isEmpty());
        raUsageLimits.setMaxRaPerTso(null);
        assertTrue(raUsageLimits.getMaxRaPerTso().isEmpty());
    }
}
