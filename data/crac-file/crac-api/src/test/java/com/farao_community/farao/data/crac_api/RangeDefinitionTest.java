/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_api;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class RangeDefinitionTest {

    @Test
    public void basicTest() {
        assertEquals(2, RangeDefinition.values().length);
        RangeDefinition rangeDefinition1 = RangeDefinition.CENTERED_ON_ZERO;
        RangeDefinition rangeDefinition2 = RangeDefinition.STARTS_AT_ONE;
    }

}
