/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class NetworkIndexMapperUtilTest {
    private Network testNetwork;

    @Before
    public void setUp() {
        testNetwork = Importers.loadNetwork("testCase.xiidm", NetworkUtilTest.class.getResourceAsStream("/testCase.xiidm"));
    }

    @Test
    public void generateBusMapping() {
        Map<Bus, Integer> busMapper = NetworkIndexMapperUtil.generateBusMapping(testNetwork);
        assertEquals(testNetwork.getBusView().getBusStream().count(), busMapper.size());
        assertTrue(busMapper.values().stream().allMatch(index -> index < busMapper.size()));
        // Check no duplicate in indexes
        assertEquals(busMapper.values().size(), busMapper.values().stream().distinct().count());
    }

    @Test
    public void generateBranchMapping() {
        Map<Branch, Integer> branchMapper = NetworkIndexMapperUtil.generateBranchMapping(testNetwork);
        assertEquals(testNetwork.getBranchCount(), branchMapper.size());
        assertTrue(branchMapper.values().stream().allMatch(index -> index < branchMapper.size()));
        // Check no duplicate in indexes
        assertEquals(branchMapper.values().size(), branchMapper.values().stream().distinct().count());
    }
}
