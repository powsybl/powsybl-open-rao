/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.openrao.commons.EICode;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.raoapi.parameters.LoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.RelativeMarginsParameters;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import com.powsybl.openrao.data.cracloopflowextension.LoopFlowThreshold;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.loopflowcomputation.LoopFlowComputation;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityInterface;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class ToolProviderTest {
    private Network network;
    private RaoParameters raoParameters;
    private FlowCnec cnec1;
    private FlowCnec cnec2;

    @BeforeEach
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        raoParameters = new RaoParameters();
        cnec1 = Mockito.mock(FlowCnec.class);
        cnec2 = Mockito.mock(FlowCnec.class);
        State preventiveState = Mockito.mock(State.class);
        Mockito.when(preventiveState.isPreventive()).thenReturn(true);
        Mockito.when(cnec1.getState()).thenReturn(preventiveState);
        Mockito.when(cnec2.getState()).thenReturn(preventiveState);
        Mockito.when(cnec1.getLocation(network)).thenReturn(Set.of(Optional.of(Country.FR), Optional.of(Country.BE)));
        Mockito.when(cnec2.getLocation(network)).thenReturn(Set.of(Optional.empty()));
    }

    @Test
    void testBasicConstructor() {
        ToolProvider toolProvider = ToolProvider.create()
                .withNetwork(network)
                .withRaoParameters(raoParameters)
                .build();
        assertNull(toolProvider.getLoopFlowComputation());
        assertNull(toolProvider.getAbsolutePtdfSumsComputation());
        assertTrue(toolProvider.getLoopFlowCnecs(Set.of(cnec1, cnec2)).isEmpty());

        Instant outageInstant = Mockito.mock(Instant.class);
        Mockito.when(outageInstant.isOutage()).thenReturn(true);
        SystematicSensitivityInterface sensitivityInterface = toolProvider.getSystematicSensitivityInterface(
                Set.of(cnec1, cnec2), Set.of(Mockito.mock(RangeAction.class)), false, false, outageInstant);
        assertNotNull(sensitivityInterface);
    }

    @Test
    void testCnecInCountry() {
        assertTrue(ToolProvider.cnecIsInCountryList(cnec1, network, Set.of(Country.FR, Country.DE)));
        assertTrue(ToolProvider.cnecIsInCountryList(cnec1, network, Set.of(Country.BE, Country.DE)));
        assertFalse(ToolProvider.cnecIsInCountryList(cnec1, network, Set.of(Country.NL, Country.DE)));

        // If country is empty, always return false
        assertFalse(ToolProvider.cnecIsInCountryList(cnec2, network, Set.of(Country.FR, Country.DE)));
        assertFalse(ToolProvider.cnecIsInCountryList(cnec2, network, Set.of(Country.BE, Country.DE)));
        assertFalse(ToolProvider.cnecIsInCountryList(cnec2, network, Set.of(Country.NL, Country.DE)));
    }

    @Test
    void testGetEicForObjectiveFunction() {
        RelativeMarginsParameters relativeMarginsParameters = new RelativeMarginsParameters();
        raoParameters.setRelativeMarginsParameters(relativeMarginsParameters);
        relativeMarginsParameters.setPtdfBoundariesFromString(
                List.of("{FR}-{BE}", "{ES}-{FR}")
        );
        ToolProvider toolProvider = ToolProvider.create()
                .withNetwork(network)
                .withRaoParameters(raoParameters)
                .build();
        assertEquals(Set.of("10YFR-RTE------C", "10YES-REE------0", "10YBE----------2"), toolProvider.getEicForObjectiveFunction());
    }

    @Test
    void testGetEicForLoopFlows() {
        ReferenceProgram referenceProgram = Mockito.mock(ReferenceProgram.class);
        Mockito.when(referenceProgram.getListOfAreas()).thenReturn(
                Set.of(new EICode("10YFR-RTE------C"), new EICode("10YES-REE------0"), new EICode("10YBE----------2"))
        );
        ToolProvider toolProvider = ToolProvider.create()
                .withNetwork(network)
                .withRaoParameters(raoParameters)
                .withLoopFlowComputation(referenceProgram, Mockito.mock(ZonalData.class), Mockito.mock(LoopFlowComputation.class))
                .build();
        assertEquals(Set.of("10YFR-RTE------C", "10YES-REE------0", "10YBE----------2"), toolProvider.getEicForLoopFlows());
    }

    @Test
    void testGetGlskForEic() {
        ZonalData<SensitivityVariableSet> glskProvider = Mockito.mock(ZonalData.class);

        SensitivityVariableSet linearGlsk1 = Mockito.mock(SensitivityVariableSet.class);
        Mockito.when(glskProvider.getData("10YFR-RTE------C")).thenReturn(linearGlsk1);

        SensitivityVariableSet linearGlsk2 = Mockito.mock(SensitivityVariableSet.class);
        Mockito.when(glskProvider.getData("10YES-REE------0")).thenReturn(linearGlsk2);

        ToolProvider toolProvider = ToolProvider.create()
                .withNetwork(network)
                .withRaoParameters(raoParameters)
                .withLoopFlowComputation(Mockito.mock(ReferenceProgram.class), glskProvider, Mockito.mock(LoopFlowComputation.class))
                .build();

        ZonalData<SensitivityVariableSet> result = toolProvider.getGlskForEic(Set.of("10YFR-RTE------C", "10YES-REE------0", "absent"));
        assertEquals(linearGlsk1, result.getData("10YFR-RTE------C"));
        assertEquals(linearGlsk2, result.getData("10YES-REE------0"));
        assertNull(result.getData("absent"));
    }

    @Test
    void testGetLoopFlowCnecs() {
        ToolProvider toolProvider = ToolProvider.create()
                .withNetwork(network)
                .withRaoParameters(raoParameters)
                .build();
        assertTrue(toolProvider.getLoopFlowCnecs(Set.of(cnec1, cnec2)).isEmpty());

        Mockito.when(cnec1.getExtension(LoopFlowThreshold.class)).thenReturn(Mockito.mock(LoopFlowThreshold.class));
        assertEquals(Set.of(cnec1), toolProvider.getLoopFlowCnecs(Set.of(cnec1, cnec2)));
        assertTrue(toolProvider.getLoopFlowCnecs(Set.of(cnec2)).isEmpty());

        Mockito.when(cnec2.getExtension(LoopFlowThreshold.class)).thenReturn(Mockito.mock(LoopFlowThreshold.class));
        assertEquals(Set.of(cnec1, cnec2), toolProvider.getLoopFlowCnecs(Set.of(cnec1, cnec2)));
        assertEquals(Set.of(cnec1), toolProvider.getLoopFlowCnecs(Set.of(cnec1)));
        assertEquals(Set.of(cnec2), toolProvider.getLoopFlowCnecs(Set.of(cnec2)));

        LoopFlowParameters loopFlowParameters = new LoopFlowParameters();
        raoParameters.setLoopFlowParameters(loopFlowParameters);
        loopFlowParameters.setCountries(List.of("FR"));
        assertEquals(Set.of(cnec1), toolProvider.getLoopFlowCnecs(Set.of(cnec1, cnec2)));
    }

}
