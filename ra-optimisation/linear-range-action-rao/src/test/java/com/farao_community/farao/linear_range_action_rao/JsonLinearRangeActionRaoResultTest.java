/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_range_action_rao;

import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.ra_optimisation.json.JsonRaoComputationResult;
import com.powsybl.commons.AbstractConverterTest;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class JsonLinearRangeActionRaoResultTest extends AbstractConverterTest {

    @Test
    public void roundTripTest() {
        // read
        RaoComputationResult result = JsonRaoComputationResult.read(getClass().getResourceAsStream("/LinearRangeActionRaoResults.json"));
        assertNotNull(result.getExtension(LinearRangeActionRaoResult.class));

        // write
        OutputStream baos = new ByteArrayOutputStream();
        JsonRaoComputationResult.write(result, baos);

        //compare
        compareTxt(getClass().getResourceAsStream("/LinearRangeActionRaoResults.json"), baos.toString());
    }
}
