package com.powsybl.openrao.data.raoresult.nc;

import com.powsybl.action.GeneratorAction;
import com.powsybl.action.LoadAction;
import com.powsybl.action.ShuntCompensatorPositionAction;
import com.powsybl.action.SwitchAction;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

class NcExporterTest {
    private Crac crac;
    private State preventiveState;
    private State curativeContingency1State;
    private State curativeContingency2State;
    private NetworkAction networkAction1;
    private NetworkAction networkAction2;
    private NetworkAction networkAction3;
    private PstRangeAction pstRangeAction;
    private RangeAction<?> generatorRangeAction;
    private RaoResult raoResult;

    @BeforeEach
    private void setUp() {
        initCrac();
        initRaoResult();
    }

    private void initCrac() {
        crac = Mockito.mock(Crac.class);
        Contingency contingency1 = Mockito.mock(Contingency.class);
        Mockito.when(contingency1.getId()).thenReturn("contingency-1");
        Contingency contingency2 = Mockito.mock(Contingency.class);
        Mockito.when(contingency2.getId()).thenReturn("contingency-2");
        preventiveState = Mockito.mock(State.class);
        Mockito.when(preventiveState.getContingency()).thenReturn(Optional.empty());
        curativeContingency1State = Mockito.mock(State.class);
        Mockito.when(curativeContingency1State.getContingency()).thenReturn(Optional.of(contingency1));
        curativeContingency2State = Mockito.mock(State.class);
        Mockito.when(curativeContingency2State.getContingency()).thenReturn(Optional.of(contingency2));
        Mockito.when(crac.getStates()).thenReturn(Set.of(preventiveState, curativeContingency1State, curativeContingency2State));

        SwitchAction switchAction = Mockito.mock(SwitchAction.class);
        Mockito.when(switchAction.getId()).thenReturn("switch-action");
        Mockito.when(switchAction.isOpen()).thenReturn(true);
        ShuntCompensatorPositionAction shuntCompensatorPositionAction = Mockito.mock(ShuntCompensatorPositionAction.class);
        Mockito.when(shuntCompensatorPositionAction.getId()).thenReturn("shunt-compensator-position-action");
        Mockito.when(shuntCompensatorPositionAction.getSectionCount()).thenReturn(2);
        GeneratorAction generatorAction = Mockito.mock(GeneratorAction.class);
        Mockito.when(generatorAction.getId()).thenReturn("generator-action");
        Mockito.when(generatorAction.getActivePowerValue()).thenReturn(OptionalDouble.of(200));
        LoadAction loadAction = Mockito.mock(LoadAction.class);
        Mockito.when(loadAction.getId()).thenReturn("load-action");
        Mockito.when(loadAction.getActivePowerValue()).thenReturn(OptionalDouble.of(150));

        networkAction1 = Mockito.mock(NetworkAction.class);
        Mockito.when(networkAction1.getId()).thenReturn("network-action-1");
        Mockito.when(networkAction1.getElementaryActions()).thenReturn(Set.of(switchAction, shuntCompensatorPositionAction));
        networkAction2 = Mockito.mock(NetworkAction.class);
        Mockito.when(networkAction2.getId()).thenReturn("network-action-2");
        Mockito.when(networkAction2.getElementaryActions()).thenReturn(Set.of(generatorAction));
        networkAction3 = Mockito.mock(NetworkAction.class);
        Mockito.when(networkAction3.getId()).thenReturn("network-action-3");
        Mockito.when(networkAction3.getElementaryActions()).thenReturn(Set.of(loadAction));

        pstRangeAction = Mockito.mock(PstRangeAction.class);
        Mockito.when(pstRangeAction.getId()).thenReturn("pst-range-action");
        generatorRangeAction = Mockito.mock(RangeAction.class);
        Mockito.when(generatorRangeAction.getId()).thenReturn("generator-range-action");
    }

    private void initRaoResult() {
        raoResult = Mockito.mock(RaoResult.class);
        Mockito.when(raoResult.getActivatedRangeActionsDuringState(preventiveState)).thenReturn(Set.of(pstRangeAction));
        Mockito.when(raoResult.getOptimizedTapOnState(preventiveState, pstRangeAction)).thenReturn(5);
        Mockito.when(raoResult.getActivatedRangeActionsDuringState(curativeContingency1State)).thenReturn(Set.of(generatorRangeAction));
        Mockito.when(raoResult.getOptimizedSetPointOnState(curativeContingency1State, generatorRangeAction)).thenReturn(35d);
        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(preventiveState)).thenReturn(Set.of(networkAction1));
        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(curativeContingency1State)).thenReturn(Set.of(networkAction2, networkAction3));
        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(curativeContingency2State)).thenReturn(Set.of());
    }

    @Test
    void exportRasProfile() throws FileNotFoundException {
        CsaProfileCracCreationContext ncContext = Mockito.mock(CsaProfileCracCreationContext.class);
        Mockito.when(ncContext.getTimeStamp()).thenReturn(OffsetDateTime.of(2024, 10, 10, 14, 42, 0, 0, ZoneOffset.UTC));
        Mockito.when(ncContext.getCrac()).thenReturn(crac);
        OutputStream outputStream = null; // new FileOutputStream("RAS.xml");
        new NcExporter().exportData(raoResult, ncContext, null, outputStream);
    }
}
