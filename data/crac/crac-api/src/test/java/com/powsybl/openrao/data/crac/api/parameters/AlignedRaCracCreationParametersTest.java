/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.parameters;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class AlignedRaCracCreationParametersTest {

    @Test
    void setRangeActionGroupsAsStringTest() {
        CracCreationParametersMock cracCreationParameters = new CracCreationParametersMock();
        cracCreationParameters.setRangeActionGroupsAsString(List.of("rangeAction3 + rangeAction4", "hvdc1 + hvdc2"));
        assertEquals(2, cracCreationParameters.getRangeActionGroups().size());

        RangeActionGroup raGroup1 = cracCreationParameters.getRangeActionGroups().get(0);
        List<String> raIds1 = raGroup1.getRangeActionsIds();
        assertEquals("rangeAction3", raIds1.get(0));
        assertEquals("rangeAction4", raIds1.get(1));

        RangeActionGroup raGroup2 = cracCreationParameters.getRangeActionGroups().get(1);
        List<String> raIds2 = raGroup2.getRangeActionsIds();
        assertEquals("hvdc1", raIds2.get(0));
        assertEquals("hvdc2", raIds2.get(1));
    }

    private class CracCreationParametersMock extends AbstractAlignedRaCracCreationParameters {

        @Override
        public String getName() {
            return "crac-creation-parameters-mock";
        }
    }

}
