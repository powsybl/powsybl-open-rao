/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class UnitTest {

    @Test
    void checkPhysicalParameterTestOk() {
        Unit.AMPERE.checkPhysicalParameter(PhysicalParameter.FLOW);
        Unit.DEGREE.checkPhysicalParameter(PhysicalParameter.ANGLE);
        Unit.KILOVOLT.checkPhysicalParameter(PhysicalParameter.VOLTAGE);
        Unit.MEGAWATT.checkPhysicalParameter(PhysicalParameter.FLOW);
    }

    @Test
    void checkPhysicalParameterTestNok() {

        try {
            Unit.AMPERE.checkPhysicalParameter(PhysicalParameter.ANGLE);
            fail();
        } catch (OpenRaoException e) {
            // should throw
        }

        try {
            Unit.DEGREE.checkPhysicalParameter(PhysicalParameter.VOLTAGE);
            fail();
        } catch (OpenRaoException e) {
            // should throw
        }

        try {
            Unit.KILOVOLT.checkPhysicalParameter(PhysicalParameter.FLOW);
            fail();
        } catch (OpenRaoException e) {
            // should throw
        }

        try {
            Unit.MEGAWATT.checkPhysicalParameter(PhysicalParameter.VOLTAGE);
            fail();
        } catch (OpenRaoException e) {
            // should throw
        }
    }
}
