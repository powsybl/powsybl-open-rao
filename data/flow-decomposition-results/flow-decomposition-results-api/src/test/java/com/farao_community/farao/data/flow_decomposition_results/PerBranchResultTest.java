/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.flow_decomposition_results;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PerBranchResultTest {
    private PerBranchResult perBranchResult;

    @Before
    public void setUp() {
        Table<String, String, Double> countryExchangeFlows = HashBasedTable.create();
        countryExchangeFlows.put("Branch Country", "Branch Country", 135.);
        countryExchangeFlows.put("Other Country", "Other Country", -28.);
        countryExchangeFlows.put("Third Country", "Third Country", -5.);
        countryExchangeFlows.put("Branch Country", "Other Country", 12.);
        countryExchangeFlows.put("Branch Country", "Third Country", 23.);
        countryExchangeFlows.put("Other Country", "Branch Country", 10.);
        countryExchangeFlows.put("Other Country", "Third Country", 0.);
        countryExchangeFlows.put("Third Country", "Branch Country", 18.);
        countryExchangeFlows.put("Third Country", "Other Country", 5.);

        Map<String, Double> countryPstFlows = new HashMap<>();
        countryPstFlows.put("Branch Country", 42.);
        countryPstFlows.put("Other Country", 18.);
        countryPstFlows.put("Third Country", 10.);

        perBranchResult = PerBranchResult.builder()
                .branchId("Branch ID")
                .branchCountry1("Branch Country")
                .branchCountry2("Other Country")
                .referenceFlows(414)
                .maximumFlows(450)
                .countryExchangeFlows(countryExchangeFlows)
                .countryPstFlows(countryPstFlows)
                .build();
    }

    @Test
    public void testBuildingAndGetting() {
        assertEquals("Branch ID", perBranchResult.getBranchId());
        assertEquals("Branch Country", perBranchResult.getBranchCountry1());
        assertEquals("Other Country", perBranchResult.getBranchCountry2());
        assertEquals(414, perBranchResult.getReferenceFlows(), 0.);
        assertEquals(450, perBranchResult.getMaximumFlows(), 0.);
        assertEquals(135., perBranchResult.getCountryExchangeFlows().get("Branch Country", "Branch Country"), 0.);
        assertEquals(-28., perBranchResult.getCountryExchangeFlows().get("Other Country", "Other Country"), 0.);
        assertEquals(12., perBranchResult.getCountryExchangeFlows().get("Branch Country", "Other Country"), 0.);
        assertEquals(10., perBranchResult.getCountryExchangeFlows().get("Other Country", "Branch Country"), 0.);
        assertEquals(42., perBranchResult.getCountryPstFlows().get("Branch Country"), 0.);
        assertEquals(18., perBranchResult.getCountryPstFlows().get("Other Country"), 0.);
    }

    @Test
    public void testConvenientGetters() {
        assertEquals(135, perBranchResult.getTotalInternalFlows(), 0.);
        assertEquals(-33, perBranchResult.getTotalLoopFlows(), 0.);
        assertEquals(28, perBranchResult.getTotalImportFlows(), 0.);
        assertEquals(35, perBranchResult.getTotalExportFlows(), 0.);
        assertEquals(5, perBranchResult.getTotalTransitFlows(), 0.);
        assertEquals(70, perBranchResult.getTotalPstFlows(), 0.);
    }
}
