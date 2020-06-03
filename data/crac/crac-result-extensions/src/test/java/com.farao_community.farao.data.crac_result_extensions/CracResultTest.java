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

    private double epsilon = 0.01;

    @Test
    public void securedCracResult() {
        CracResult securedCracResult = new CracResult(CracResult.NetworkSecurityStatus.SECURED, -5, 0);

        assertEquals(CracResult.NetworkSecurityStatus.SECURED, securedCracResult.getNetworkSecurityStatus());
        assertEquals(-5, securedCracResult.getCost(), epsilon);
        assertEquals(-5, securedCracResult.getFunctionalCost(), epsilon);
        assertEquals(0., securedCracResult.getVirtualCost(), epsilon);

    }

    @Test
    public void unsecuredCracResult() {
        CracResult unsecuredCracResult = new CracResult(5);
        unsecuredCracResult.setNetworkSecurityStatus(CracResult.NetworkSecurityStatus.UNSECURED);

        assertEquals(CracResult.NetworkSecurityStatus.UNSECURED, unsecuredCracResult.getNetworkSecurityStatus());
        assertEquals(5, unsecuredCracResult.getCost(), epsilon);
        assertEquals(5, unsecuredCracResult.getFunctionalCost(), epsilon);
        assertEquals(0, unsecuredCracResult.getVirtualCost(), epsilon);
    }

    @Test
    public void securedCracResultWithPositiveCost() {
        CracResult securedCracResult = new CracResult(CracResult.NetworkSecurityStatus.SECURED, -5, 10);

        assertEquals(CracResult.NetworkSecurityStatus.SECURED, securedCracResult.getNetworkSecurityStatus());
        assertEquals(5, securedCracResult.getCost(), epsilon);
        assertEquals(-5, securedCracResult.getFunctionalCost(), epsilon);
        assertEquals(10, securedCracResult.getVirtualCost(), epsilon);
    }

    @Test
    public void updateCosts() {
        CracResult cracResult = new CracResult();
        cracResult.setFunctionalCost(5);
        assertEquals(5, cracResult.getCost(), epsilon);
        cracResult.setVirtualCost(15);
        assertEquals(20, cracResult.getCost(), epsilon);
    }
}
