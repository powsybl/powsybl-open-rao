/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator;

import com.powsybl.openrao.data.cracio.csaprofiles.nc.AssessedElementWithRemedialAction;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet <thomas.bouquet at rte-france.com>
 */
class NcAggregatorTest {
    @Test
    void testNcAggregator() {
        AssessedElementWithRemedialAction aeWithRa1 = new AssessedElementWithRemedialAction("aeWithRa1", "assessed-element-1", "remedial-action-1", null, true);
        AssessedElementWithRemedialAction aeWithRa2 = new AssessedElementWithRemedialAction("aeWithRa2", "assessed-element-2", "remedial-action-1", null, true);
        AssessedElementWithRemedialAction aeWithRa3 = new AssessedElementWithRemedialAction("aeWithRa3", "assessed-element-3", "remedial-action-1", null, true);
        AssessedElementWithRemedialAction aeWithRa4 = new AssessedElementWithRemedialAction("aeWithRa4", "assessed-element-4", "remedial-action-2", null, true);
        AssessedElementWithRemedialAction aeWithRa5 = new AssessedElementWithRemedialAction("aeWithRa5", "assessed-element-5", "remedial-action-2", null, true);
        AssessedElementWithRemedialAction aeWithRa6 = new AssessedElementWithRemedialAction("aeWithRa6", "assessed-element-6", "remedial-action-2", null, true);
        assertEquals(
            Map.of("remedial-action-1", Set.of(aeWithRa1, aeWithRa2, aeWithRa3), "remedial-action-2", Set.of(aeWithRa4, aeWithRa5, aeWithRa6)),
            new NcAggregator<>(AssessedElementWithRemedialAction::remedialAction).aggregate(Set.of(aeWithRa1, aeWithRa2, aeWithRa3, aeWithRa4, aeWithRa5, aeWithRa6))
        );
    }
}
