/*
 *
 *  * Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.linear_rao.json;

//import com.farao_community.farao.linear_rao.LinearRaoResult;
import com.farao_community.farao.rao_api.RaoResult;
//import com.farao_community.farao.rao_api.json.JsonRaoResult;
import com.powsybl.commons.AbstractConverterTest;
import org.junit.Before;
//import org.junit.Test;

import java.io.IOException;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class JsonLinearRaoResultTest extends AbstractConverterTest {

    RaoResult raoResult;

    @Before
    public void setUp() throws IOException {
        raoResult = new RaoResult(RaoResult.Status.SUCCESS);
        raoResult.setPreOptimVariantId("variant1");
        raoResult.setPostOptimVariantId("variant2");
        super.setUp();
    }

    /*@Test
    public void roundTripDefaultSensiParams() throws IOException {
        LinearRaoResult resultExtension = new LinearRaoResult();
        resultExtension.setSystematicSensitivityAnalysisParameters(false);
        raoResult.addExtension(LinearRaoResult.class, resultExtension);
        roundTripTest(raoResult, JsonRaoResult::write, JsonRaoResult::read, "/RaoResultDefaultSensi.json");
    }

    @Test
    public void roundTripFallbackSensiParams() throws IOException {
        LinearRaoResult resultExtension = new LinearRaoResult();
        resultExtension.setSystematicSensitivityAnalysisParameters(true);
        raoResult.addExtension(LinearRaoResult.class, resultExtension);
        roundTripTest(raoResult, JsonRaoResult::write, JsonRaoResult::read, "/RaoResultFallbackSensi.json");
    }*/
}
