/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.cim.parameters;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class CimCracCreationParametersTest {

    @Test
    void testDefaultParameters() {
        CimCracCreationParameters parameters = new CimCracCreationParameters();

        assertEquals(0, parameters.getRangeActionGroupsAsString().size());
        assertEquals(0, parameters.getRangeActionGroups().size());
    }

    @Test
    void testParallelRaConf() {

        CimCracCreationParameters parameters = new CimCracCreationParameters();
        List<String> parallelRaAsConcatenatedString = new ArrayList<>();
        parallelRaAsConcatenatedString.add("rangeAction1 + rangeAction3 + rangeAction7");
        parallelRaAsConcatenatedString.add("errorInThisOne");

        parameters.setRangeActionGroupsAsString(parallelRaAsConcatenatedString);

        assertEquals(1, parameters.getRangeActionGroupsAsString().size());
        assertEquals(1, parameters.getRangeActionGroups().size());
        assertEquals("rangeAction1 + rangeAction3 + rangeAction7", parameters.getRangeActionGroups().get(0).toString());
    }

    @Test
    void testAlignedRaWithSameSpeed() {
        CimCracCreationParameters parameters = new CimCracCreationParameters();
        List<String> parallelRaAsConcatenatedString = new ArrayList<>();
        parallelRaAsConcatenatedString.add("rangeAction1 + rangeAction2 + rangeAction7");
        Set<RangeActionSpeed> speedSet = Set.of(new RangeActionSpeed("rangeAction1", 1), new RangeActionSpeed("rangeAction2", 1));

        parameters.setRangeActionGroupsAsString(parallelRaAsConcatenatedString);
        parameters.setRemedialActionSpeed(speedSet);

        assertEquals(2, parameters.getRangeActionSpeedSet().size());

    }

    @Test
    void testAlignedRaWithDifferentSpeed() {
        CimCracCreationParameters parameters = new CimCracCreationParameters();
        List<String> parallelRaAsConcatenatedString = new ArrayList<>();
        parallelRaAsConcatenatedString.add("rangeAction1 + rangeAction2 + rangeAction7");
        Set<RangeActionSpeed> speedSet = Set.of(new RangeActionSpeed("rangeAction1", 1), new RangeActionSpeed("rangeAction2", 2));

        parameters.setRangeActionGroupsAsString(parallelRaAsConcatenatedString);
        assertThrows(OpenRaoException.class, () -> parameters.setRemedialActionSpeed(speedSet));
    }

    @Test
    void testUnalignedRaWithSameSpeed() {
        CimCracCreationParameters parameters = new CimCracCreationParameters();
        List<String> parallelRaAsConcatenatedString = new ArrayList<>();
        parallelRaAsConcatenatedString.add("rangeAction1 + rangeAction2 + rangeAction7");
        Set<RangeActionSpeed> speedSet = Set.of(new RangeActionSpeed("rangeAction1", 1), new RangeActionSpeed("rangeAction3", 1));

        parameters.setRangeActionGroupsAsString(parallelRaAsConcatenatedString);
        assertThrows(OpenRaoException.class, () -> parameters.setRemedialActionSpeed(speedSet));
    }

    @Test
    void testParametersWithinExtendable() {
        CracCreationParameters parameters = new CracCreationParameters();
        assertNull(parameters.getExtension(CimCracCreationParameters.class));

        parameters.addExtension(CimCracCreationParameters.class, new CimCracCreationParameters());
        assertNotNull(parameters.getExtension(CimCracCreationParameters.class));
    }

}
