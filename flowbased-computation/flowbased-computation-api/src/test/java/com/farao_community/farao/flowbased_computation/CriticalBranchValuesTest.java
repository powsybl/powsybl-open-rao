/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

/**
 * @author Di Gallo Luc  {@literal <luc.di-gallo at rte-france.com>}
 */
public class CriticalBranchValuesTest {
    private static final double EPSILON_COMPARISON = 1e-6;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testNullVariables() {
        new CriticalBranchValues("", "", 1.f, 2.f, "");
    }

    @Test
    public void getId() {
        String idValueRef = "idValue";
        CriticalBranchValues criticalBranchValues = new CriticalBranchValues(idValueRef, "", 1.f, 2.f, "");
        assertEquals(idValueRef, criticalBranchValues.getId());
    }

    @Test
    public void getName() {
        String nameValueRef = "nameValue";
        CriticalBranchValues criticalBranchValues = new CriticalBranchValues("", nameValueRef, 1.f, 2.f, "");
        assertEquals(nameValueRef, criticalBranchValues.getName());
    }

    @Test
    public void getuNominal() {
        double uNominalValueRef = 1.1;
        CriticalBranchValues criticalBranchValues = new CriticalBranchValues("", "", uNominalValueRef, 2.f, "");
        assertEquals(uNominalValueRef, criticalBranchValues.getuNominal(), EPSILON_COMPARISON);
    }

    @Test
    public void getiMax() {
        double iMaxValueRef = 1.2;
        CriticalBranchValues criticalBranchValues = new CriticalBranchValues("", "", 1.f, iMaxValueRef, "");
        assertEquals(iMaxValueRef, criticalBranchValues.getiMax(), EPSILON_COMPARISON);
    }

    @Test
    public void getPowerFlow() {
        double uNominalValueRef = 1.1e6;
        double iMaxValueRef = 1.2;
        double powerFlowValueRef = uNominalValueRef * iMaxValueRef * Math.sqrt(3.0) / 1000.0;
        CriticalBranchValues criticalBranchValues = new CriticalBranchValues("", "", uNominalValueRef, iMaxValueRef, "");
        assertEquals(powerFlowValueRef, criticalBranchValues.getPowerFlow(), EPSILON_COMPARISON);
    }

    @Test
    public void getDirection() {
        String directionValueRef = "directionValue";
        CriticalBranchValues criticalBranchValues = new CriticalBranchValues("", "", 1.f, 2.f, directionValueRef);
        assertEquals(directionValueRef, criticalBranchValues.getDirection());
    }
}
