/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class LinearRaoResultTest {

    @Test
    public void testErrorMessage() {
        LinearRaoResult linearRaoResult = new LinearRaoResult();

        linearRaoResult.setErrorMessage("Sensitivity computation failed");
        assertEquals("Sensitivity computation failed", linearRaoResult.getErrorMessage());
    }
}
