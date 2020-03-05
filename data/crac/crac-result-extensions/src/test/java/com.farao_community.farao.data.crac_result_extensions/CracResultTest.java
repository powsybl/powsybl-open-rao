/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_result_extensions;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class CracResultTest {

    private double epsilon = 0.0;

    @Test
    public void getName() {
        CracResult cracResult = new CracResult();
        assertEquals("CracResult", cracResult.getName());
    }

    @Test
    public void securedCracResult() {
        double cost = -5;
        CracResult securedCracResult = new CracResult(cost);
        assertEquals(CracResult.NetworkSecurityStatus.SECURED, securedCracResult.getNetworkSecurityStatus());
        assertEquals(cost, securedCracResult.getCost(), epsilon);
    }

    @Test
    public void unsecuredCracResult() {
        double cost = 5;
        CracResult unsecuredCracResult = new CracResult(cost);
        assertEquals(CracResult.NetworkSecurityStatus.UNSECURED, unsecuredCracResult.getNetworkSecurityStatus());
        assertEquals(cost, unsecuredCracResult.getCost(), epsilon);
    }

    @Test
    public void unknownCracResult() {
        CracResult unknownCracResult = new CracResult();
        assertEquals(CracResult.NetworkSecurityStatus.UNSECURED, unknownCracResult.getNetworkSecurityStatus());
        assertEquals(Double.NaN, unknownCracResult.getCost(), epsilon);
        double cost = -5;
        unknownCracResult.setCost(cost);
        assertEquals(CracResult.NetworkSecurityStatus.SECURED, unknownCracResult.getNetworkSecurityStatus());
        assertEquals(cost, unknownCracResult.getCost(), epsilon);
    }
}
