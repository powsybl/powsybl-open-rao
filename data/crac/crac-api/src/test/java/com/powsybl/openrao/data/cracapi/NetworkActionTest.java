package com.powsybl.openrao.data.cracapi;

import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.InjectionSetpoint;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.networkaction.PstSetpoint;
import com.powsybl.openrao.data.cracapi.networkaction.SwitchPair;
import com.powsybl.openrao.data.cracapi.networkaction.TopologicalAction;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkActionTest {
    @Test
    void compatibility() {
        NetworkAction hvdcFrEs200Mw = mockHvdcAction(-200d);
        NetworkAction hvdcEsFr200Mw = mockHvdcAction(200d);
        NetworkAction alignedPsts = Mockito.mock(NetworkAction.class);
        PstSetpoint pstSetpoint1 = mockPstSetpoint("pst-fr-1", 4);
        PstSetpoint pstSetpoint2 = mockPstSetpoint("pst-fr-2", 4);
        PstSetpoint pstSetpoint3 = mockPstSetpoint("pst-fr-3", 4);
        Mockito.when(alignedPsts.getElementaryActions()).thenReturn(Set.of(pstSetpoint1, pstSetpoint2, pstSetpoint3));
        NetworkAction switchPairAndPst = Mockito.mock(NetworkAction.class);
        PstSetpoint pstSetpoint4 = mockPstSetpoint("pst-fr-2", -2);
        SwitchPair switchPair = mockSwitchPair();
        Mockito.when(switchPairAndPst.getElementaryActions()).thenReturn(Set.of(pstSetpoint4, switchPair));

        assertTrue(hvdcFrEs200Mw.isCompatibleWith(hvdcFrEs200Mw));
        assertFalse(hvdcFrEs200Mw.isCompatibleWith(hvdcEsFr200Mw));
        assertTrue(hvdcFrEs200Mw.isCompatibleWith(alignedPsts));
        assertTrue(hvdcFrEs200Mw.isCompatibleWith(switchPairAndPst));
        assertTrue(hvdcEsFr200Mw.isCompatibleWith(hvdcEsFr200Mw));
        assertTrue(hvdcEsFr200Mw.isCompatibleWith(alignedPsts));
        assertTrue(hvdcEsFr200Mw.isCompatibleWith(switchPairAndPst));
        assertTrue(alignedPsts.isCompatibleWith(alignedPsts));
        assertFalse(alignedPsts.isCompatibleWith(switchPairAndPst));
        assertFalse(switchPairAndPst.isCompatibleWith(switchPairAndPst));
    }

    private NetworkAction mockHvdcAction(double setpoint) {
        NetworkAction hvdcAction = Mockito.mock(NetworkAction.class);
        TopologicalAction topologicalAction1 = mockTopologicalAction("switch-fr");
        TopologicalAction topologicalAction2 = mockTopologicalAction("switch-es");
        InjectionSetpoint generatorAction11 = mockInjectionSetpoint("generator-fr-1", setpoint / 2d);
        InjectionSetpoint generatorAction12 = mockInjectionSetpoint("generator-fr-2", setpoint / 2d);
        InjectionSetpoint generatorAction21 = mockInjectionSetpoint("generator-es-1", -setpoint / 2d);
        InjectionSetpoint generatorAction22 = mockInjectionSetpoint("generator-es-2", -setpoint / 2d);
        Mockito.when(hvdcAction.getElementaryActions()).thenReturn(Set.of(topologicalAction1, topologicalAction2, generatorAction11, generatorAction12, generatorAction21, generatorAction22));
        Mockito.when(hvdcAction.isCompatibleWith(Mockito.any())).thenCallRealMethod();
        return hvdcAction;
    }

    private TopologicalAction mockTopologicalAction(String switchId) {
        TopologicalAction topologicalAction = Mockito.mock(TopologicalAction.class);
        NetworkElement networkElement = mockNetworkElement(switchId);
        Mockito.when(topologicalAction.getNetworkElement()).thenReturn(networkElement);
        Mockito.when(topologicalAction.getActionType()).thenReturn(ActionType.OPEN);
        return topologicalAction;
    }

    private InjectionSetpoint mockInjectionSetpoint(String networkElementId, double setpoint) {
        InjectionSetpoint injectionSetpoint = Mockito.mock(InjectionSetpoint.class);
        NetworkElement networkElement = mockNetworkElement(networkElementId);
        Mockito.when(injectionSetpoint.getSetpoint()).thenReturn(setpoint);
        Mockito.when(injectionSetpoint.getNetworkElement()).thenReturn(networkElement);
        return injectionSetpoint;
    }

    private PstSetpoint mockPstSetpoint(String pstId, int setpoint) {
        PstSetpoint pstSetpoint = Mockito.mock(PstSetpoint.class);
        NetworkElement networkElement = mockNetworkElement(pstId);
        Mockito.when(pstSetpoint.getSetpoint()).thenReturn(setpoint);
        Mockito.when(pstSetpoint.getNetworkElement()).thenReturn(networkElement);
        return pstSetpoint;
    }

    private SwitchPair mockSwitchPair() {
        SwitchPair switchPair = Mockito.mock(SwitchPair.class);
        NetworkElement switchToOpen = mockNetworkElement("switch-fr");
        NetworkElement switchToClose = mockNetworkElement("switch-es");
        Mockito.when(switchPair.getSwitchToOpen()).thenReturn(switchToOpen);
        Mockito.when(switchPair.getSwitchToClose()).thenReturn(switchToClose);
        return switchPair;
    }

    private NetworkElement mockNetworkElement(String networkElementId) {
        NetworkElement networkElement = Mockito.mock(NetworkElement.class);
        Mockito.when(networkElement.getId()).thenReturn(networkElementId);
        return networkElement;
    }
}
