package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.RaUsageLimits;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RaUsageLimitsTest {

    @Test
    void testIllegalValues() {
        RaUsageLimits raUsageLimits = new RaUsageLimits();
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
