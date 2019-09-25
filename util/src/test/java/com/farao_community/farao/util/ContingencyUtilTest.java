package com.farao_community.farao.util;

import com.farao_community.farao.data.crac_file.Contingency;
import com.farao_community.farao.data.crac_file.ContingencyElement;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class ContingencyUtilTest {
    private Network network;
    private ComputationManager computationManager;

    @Before
    public void setUp() {
        network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        computationManager = LocalComputationManager.getDefault();
    }

    @Test
    public void testApplyBranchContingency() {
        String branchId = "FFR1AA1  FFR3AA1  1";
        Contingency branchContingency = Contingency.builder()
                .id("Branch contingency")
                .name("Branch contingency")
                .contingencyElements(Collections.singletonList(
                        ContingencyElement.builder()
                                .name("Branch contingency element")
                                .elementId(branchId)
                                .build()
                ))
                .build();
        assertTrue(network.getBranch(branchId).getTerminal1().isConnected());
        assertTrue(network.getBranch(branchId).getTerminal2().isConnected());

        ContingencyUtil.applyContingency(network, computationManager, branchContingency);

        assertFalse(network.getBranch(branchId).getTerminal1().isConnected());
        assertFalse(network.getBranch(branchId).getTerminal2().isConnected());
    }

    @Test
    public void testApplyMultipleContingency() {
        String branchId1 = "FFR1AA1  FFR3AA1  1";
        String branchId2 = "FFR2AA1  FFR3AA1  1";
        Contingency branchContingency = Contingency.builder()
                .id("Branch double contingency")
                .name("Branch double contingency")
                .contingencyElements(Arrays.asList(
                        ContingencyElement.builder()
                                .name("Branch contingency element 1")
                                .elementId(branchId1)
                                .build(),
                        ContingencyElement.builder()
                                .name("Branch contingency element 2")
                                .elementId(branchId2)
                                .build()
                ))
                .build();
        assertTrue(network.getBranch(branchId1).getTerminal1().isConnected());
        assertTrue(network.getBranch(branchId1).getTerminal2().isConnected());
        assertTrue(network.getBranch(branchId2).getTerminal1().isConnected());
        assertTrue(network.getBranch(branchId2).getTerminal2().isConnected());

        ContingencyUtil.applyContingency(network, computationManager, branchContingency);

        assertFalse(network.getBranch(branchId1).getTerminal1().isConnected());
        assertFalse(network.getBranch(branchId1).getTerminal2().isConnected());
        assertFalse(network.getBranch(branchId2).getTerminal1().isConnected());
        assertFalse(network.getBranch(branchId2).getTerminal2().isConnected());
    }
}
