/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation.interceptors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
public class RaoComputationInterceptorMock extends DefaultRaoComputationInterceptor {

    private boolean onPrecontingencyResultCount = false;

    private boolean onPostContingencyResultCount = false;

    private boolean onRaoComputationResultCount = false;

    private static void assertRunningContext(RunningContext context) {
        assertNotNull(context);
        assertNotNull(context.getNetwork());
        assertEquals("sim1", context.getNetwork().getId());
        assertEquals("test", context.getNetwork().getSourceFormat());
    }
}
