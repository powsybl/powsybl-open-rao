/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation.interceptors;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
public class RaoComputationInterceptorTest {

    @Test
    public void test() {
        assertEquals(Collections.singleton("RaoComputationInterceptorMock"), RaoComputationInterceptors.getExtensionNames());

        RaoComputationInterceptor interceptor = RaoComputationInterceptors.createInterceptor("RaoComputationInterceptorMock");
        assertNotNull(interceptor);
        assertEquals(RaoComputationInterceptorMock.class, interceptor.getClass());

        try {
            interceptor = RaoComputationInterceptors.createInterceptor(null);
            fail();
        } catch (NullPointerException e) {
            // Nothing to do
        }

        try {
            interceptor = RaoComputationInterceptors.createInterceptor("unknown-ra_optimisation-computation-interceptor");
            fail();
        } catch (IllegalArgumentException e) {
            // Nothing to do
        }
    }

}
