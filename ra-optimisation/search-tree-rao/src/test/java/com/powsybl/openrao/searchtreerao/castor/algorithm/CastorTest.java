/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class CastorTest {
    @Test
    void runTimeCoupled() {
        TimeCoupledRaoInput timeCoupledRaoInput = Mockito.mock(TimeCoupledRaoInput.class);
        OpenRaoException exception = assertThrows(
            OpenRaoException.class,
            () -> new Castor().run(timeCoupledRaoInput, new RaoParameters(ReportNode.NO_OP), ReportNode.NO_OP).get()
        );
        assertEquals("Time-coupled optimization is not supported by CASTOR", exception.getMessage());
    }
}
