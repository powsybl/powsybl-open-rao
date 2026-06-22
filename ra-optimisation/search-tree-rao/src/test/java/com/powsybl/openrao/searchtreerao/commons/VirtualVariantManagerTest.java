/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions.AppliedRemedialActionsPerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class VirtualVariantManagerTest {

    private Network network;
    private VirtualVariantManager manager;

    @BeforeEach
    void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        manager = new VirtualVariantManager();
    }

    private AppliedRemedialActionsPerState computeAndCapture() {
        SensitivityComputer sensitivityComputer = Mockito.mock(SensitivityComputer.class);
        manager.compute(sensitivityComputer, network);
        ArgumentCaptor<AppliedRemedialActionsPerState> captor = ArgumentCaptor.forClass(AppliedRemedialActionsPerState.class);
        Mockito.verify(sensitivityComputer).compute(Mockito.eq(network), captor.capture());
        return captor.getValue();
    }

    @Test
    void testSetWorkingVariantFromRealVariant() {
        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        manager.setWorkingVariant(network, initialVariantId, "v1");
        // no exception expected, working variant is set
        RangeAction<?> ra = Mockito.mock(RangeAction.class);
        assertDoesNotThrow(() -> manager.applyRangeAction(ra, 1.0));
    }

    @Test
    void testSetWorkingVariantFromVirtualVariant() {
        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        manager.setWorkingVariant(network, initialVariantId, "v1");
        // "v1" is virtual — set "v2" from it
        manager.setWorkingVariant(network, "v1", "v2");
        RangeAction<?> ra = Mockito.mock(RangeAction.class);
        assertDoesNotThrow(() -> manager.applyRangeAction(ra, 2.0));
    }

    @Test
    void testSetWorkingVariantReusesExistingVariant() {
        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        manager.setWorkingVariant(network, initialVariantId, "v1");
        RangeAction<?> ra1 = Mockito.mock(RangeAction.class);
        manager.applyRangeAction(ra1, 5.0);

        // set again to same id — should reuse without resetting content
        manager.setWorkingVariant(network, initialVariantId, "v1");
        AppliedRemedialActionsPerState result = computeAndCapture();
        assertTrue(result.getRangeActions().containsKey(ra1));
        assertEquals(5.0, result.getRangeActions().get(ra1));
    }

    @Test
    void testSetWorkingVariantUnknownFromVariantThrows() {
        OpenRaoException ex = assertThrows(OpenRaoException.class,
            () -> manager.setWorkingVariant(network, "unknown-variant", "v1"));
        assertTrue(ex.getMessage().contains("unknown-variant"));
    }

    @Test
    void testApplyRangeActionWithoutWorkingVariantThrows() {
        RangeAction<?> ra = Mockito.mock(RangeAction.class);
        OpenRaoException ex = assertThrows(OpenRaoException.class, () -> manager.applyRangeAction(ra, 1.0));
        assertEquals("Working variant not set", ex.getMessage());
    }

    @Test
    void testApplyNetworkActionWithoutWorkingVariantThrows() {
        NetworkAction na = Mockito.mock(NetworkAction.class);
        OpenRaoException ex = assertThrows(OpenRaoException.class, () -> manager.applyNetworkAction(na));
        assertEquals("Working variant not set", ex.getMessage());
    }

    @Test
    void testApplyNullRangeActionThrows() {
        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        manager.setWorkingVariant(network, initialVariantId, "v1");
        assertThrows(NullPointerException.class, () -> manager.applyRangeAction(null, 1.0));
    }

    @Test
    void testApplyNullNetworkActionThrows() {
        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        manager.setWorkingVariant(network, initialVariantId, "v1");
        assertThrows(NullPointerException.class, () -> manager.applyNetworkAction(null));
    }

    @Test
    void testApplyRangeActionStoredInWorkingVariant() {
        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        manager.setWorkingVariant(network, initialVariantId, "v1");
        RangeAction<?> ra = Mockito.mock(RangeAction.class);
        manager.applyRangeAction(ra, 3.5);

        AppliedRemedialActionsPerState result = computeAndCapture();
        assertEquals(1, result.getRangeActions().size());
        assertEquals(3.5, result.getRangeActions().get(ra));
        assertTrue(result.getNetworkActions().isEmpty());
    }

    @Test
    void testApplyNetworkActionStoredInWorkingVariant() {
        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        manager.setWorkingVariant(network, initialVariantId, "v1");
        NetworkAction na = Mockito.mock(NetworkAction.class);
        manager.applyNetworkAction(na);

        AppliedRemedialActionsPerState result = computeAndCapture();
        assertEquals(1, result.getNetworkActions().size());
        assertTrue(result.getNetworkActions().contains(na));
        assertTrue(result.getRangeActions().isEmpty());
    }

    @Test
    void testGetFullAppliedRemedialActionsAccumulatesParentChain() {
        String initialVariantId = network.getVariantManager().getWorkingVariantId();

        // root virtual variant: apply ra1
        manager.setWorkingVariant(network, initialVariantId, "v1");
        RangeAction<?> ra1 = Mockito.mock(RangeAction.class);
        manager.applyRangeAction(ra1, 1.0);

        // child virtual variant: apply na1
        manager.setWorkingVariant(network, "v1", "v2");
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        manager.applyNetworkAction(na1);

        // grandchild virtual variant: apply ra2
        manager.setWorkingVariant(network, "v2", "v3");
        RangeAction<?> ra2 = Mockito.mock(RangeAction.class);
        manager.applyRangeAction(ra2, 7.0);

        AppliedRemedialActionsPerState full = computeAndCapture();
        assertEquals(2, full.getRangeActions().size());
        assertEquals(1.0, full.getRangeActions().get(ra1));
        assertEquals(7.0, full.getRangeActions().get(ra2));
        assertEquals(1, full.getNetworkActions().size());
        assertTrue(full.getNetworkActions().contains(na1));
    }

    @Test
    void testComputeDelegatesToSensitivityComputer() {
        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        manager.setWorkingVariant(network, initialVariantId, "v1");

        SensitivityComputer sensitivityComputer = Mockito.mock(SensitivityComputer.class);
        manager.compute(sensitivityComputer, network);

        Mockito.verify(sensitivityComputer).compute(Mockito.eq(network), Mockito.any(AppliedRemedialActionsPerState.class));
    }

    @Test
    void testRemoveWorkingVariantsClearsState() {
        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        manager.setWorkingVariant(network, initialVariantId, "v1");
        manager.removeWorkingVariants();

        // any operation requiring working variant should now throw
        RangeAction<?> ra = Mockito.mock(RangeAction.class);
        assertThrows(OpenRaoException.class, () -> manager.applyRangeAction(ra, 1.0));
    }

    @Test
    void testRemoveWorkingVariantsPreventsReuseOfOldVariants() {
        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        manager.setWorkingVariant(network, initialVariantId, "v1");
        manager.removeWorkingVariants();

        // after removal, "v1" is no longer known as a virtual parent
        OpenRaoException ex = assertThrows(OpenRaoException.class,
            () -> manager.setWorkingVariant(network, "v1", "v2"));
        assertTrue(ex.getMessage().contains("v1"));
    }
}
