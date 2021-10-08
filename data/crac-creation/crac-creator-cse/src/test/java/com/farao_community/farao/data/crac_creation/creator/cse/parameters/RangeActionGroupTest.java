/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cse.parameters;

import com.farao_community.farao.commons.FaraoException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RangeActionGroupTest {

    @Test
    public void constructorTest() {
        List<String> idList = new ArrayList<>();
        idList.add("rangeAction1");
        idList.add("rangeAction2");

        RangeActionGroup parallelRangeActions = new RangeActionGroup(idList);
        assertEquals(2, parallelRangeActions.getRangeActionsIds().size());
        assertEquals("rangeAction1", parallelRangeActions.getRangeActionsIds().get(0));
        assertEquals("rangeAction2", parallelRangeActions.getRangeActionsIds().get(1));
    }

    @Test
    public void parseAndToStringTest() {
        String concatenatedId = "rangeAction1 + rangeAction2 + rangeAction3";

        List<String> seperatedIds = RangeActionGroup.parse(concatenatedId);
        assertEquals(3, seperatedIds.size());
        assertEquals("rangeAction1", seperatedIds.get(0));
        assertEquals("rangeAction2", seperatedIds.get(1));
        assertEquals("rangeAction3", seperatedIds.get(2));

        assertEquals(concatenatedId, new RangeActionGroup(seperatedIds).toString());
    }

    @Test
    public void isValidTest() {
        assertEquals(List.of("rangeAction1", "rangeAction2"), RangeActionGroup.parse("rangeAction1 + rangeAction2"));
        assertEquals(List.of("range action 1", "range action 2"), RangeActionGroup.parse("range action 1 + range action 2"));
        assertThrows(FaraoException.class, () -> RangeActionGroup.parse("rangeAction1 and rangeAction2"));
        assertThrows(FaraoException.class, () -> RangeActionGroup.parse("rangeAction1"));
        assertThrows(FaraoException.class, () -> RangeActionGroup.parse("rangeAction1+rangeAction2"));
    }
}
