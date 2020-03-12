/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl.json;

import com.farao_community.farao.data.crac_api.ActionType;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_impl.remedial_action.AbstractRemedialAction;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.*;
import org.junit.Test;

import static com.farao_community.farao.data.crac_impl.json.RoundTripUtil.roundTrip;
import static org.junit.Assert.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class NetworkActionImportExportTest {
    @Test
    public void topologyJsonCreator() {
        Topology topology = new Topology(
            "topologyId",
            "topologyName",
            "RTE",
            new NetworkElement("neId"),
            ActionType.CLOSE
        );

        Topology transformedTopology = roundTrip(topology, Topology.class);
        assertEquals(transformedTopology, topology);
    }

    @Test
    public void topologyAlternativeConstructor() {
        Topology topology = new Topology(
            "topologyId",
            new NetworkElement("neId"),
            ActionType.CLOSE
        );

        Topology transformedTopology = roundTrip(topology, Topology.class);
        assertEquals(transformedTopology, topology);
    }

    @Test
    public void hvdcSetpointJsonCreator() {
        HvdcSetpoint hvdcSetpoint = new HvdcSetpoint(
            "hvdcId",
            "hvdcName",
            "RTE",
            new NetworkElement("neId"),
            200
        );

        HvdcSetpoint transformedHvdcSetpoint = roundTrip(hvdcSetpoint, HvdcSetpoint.class);
        assertEquals(transformedHvdcSetpoint, hvdcSetpoint);
    }

    @Test
    public void injectionSetpointJsonCreator() {
        InjectionSetpoint injectionSetpoint = new InjectionSetpoint(
            "injectionId",
            "injectionName",
            "RTE",
            new NetworkElement("neId"),
            500
        );

        InjectionSetpoint transformedInjectionSetpoint = roundTrip(injectionSetpoint, InjectionSetpoint.class);
        assertEquals(transformedInjectionSetpoint, injectionSetpoint);
    }

    @Test
    public void abstractSetpointWithHvdcConstructor() {
        AbstractSetpointElementaryNetworkAction abstractSetpoint = new HvdcSetpoint(
            "hvdcId",
            "hvdcName",
            "RTE",
            new NetworkElement("neId"),
            200
        );

        AbstractSetpointElementaryNetworkAction transformedAbstractSetpoint = roundTrip(abstractSetpoint, AbstractSetpointElementaryNetworkAction.class);
        assertEquals(transformedAbstractSetpoint, abstractSetpoint);
    }

    @Test
    public void abstractSetpointWithInjectionConstructor() {
        AbstractSetpointElementaryNetworkAction abstractSetpoint = new InjectionSetpoint(
            "injectionId",
            "injectionName",
            "RTE",
            new NetworkElement("neId"),
            500
        );

        AbstractSetpointElementaryNetworkAction transformedAbstractSetpoint = roundTrip(abstractSetpoint, AbstractSetpointElementaryNetworkAction.class);
        assertEquals(transformedAbstractSetpoint, abstractSetpoint);
    }

    @Test
    public void abstractElementaryWithHvdcConstructor() {
        AbstractElementaryNetworkAction abstractElementary = new HvdcSetpoint(
            "hvdcId",
            "hvdcName",
            "RTE",
            new NetworkElement("neId"),
            200
        );

        AbstractElementaryNetworkAction transformedAbstractElementary = roundTrip(abstractElementary, AbstractElementaryNetworkAction.class);
        assertEquals(transformedAbstractElementary, abstractElementary);
    }

    @Test
    public void abstractElementaryWithInjectionConstructor() {
        AbstractElementaryNetworkAction abstractElementary = new InjectionSetpoint(
            "injectionId",
            "injectionName",
            "RTE",
            new NetworkElement("neId"),
            500
        );

        AbstractElementaryNetworkAction transformedAbstractElementary = roundTrip(abstractElementary, AbstractElementaryNetworkAction.class);
        assertEquals(transformedAbstractElementary, abstractElementary);
    }

    @Test
    public void abstractElementaryWithTopologyConstructor() {
        AbstractElementaryNetworkAction abstractElementary = new Topology(
            "topologyId",
            "topologyName",
            "RTE",
            new NetworkElement("neId"),
            ActionType.CLOSE
        );

        AbstractElementaryNetworkAction transformedAbstractElementary = roundTrip(abstractElementary, AbstractElementaryNetworkAction.class);
        assertEquals(transformedAbstractElementary, abstractElementary);
    }

    @Test
    public void abstractRemedialActionWithHvdc() {
        AbstractRemedialAction abstractRemedialAction = new HvdcSetpoint(
            "hvdcId",
            "hvdcName",
            "RTE",
            new NetworkElement("neId"),
            200
        );

        AbstractRemedialAction transformedAbstractRemedialAction = roundTrip(abstractRemedialAction, AbstractRemedialAction.class);
        assertEquals(transformedAbstractRemedialAction, abstractRemedialAction);
    }

    @Test
    public void abstractRemedialActionWithInjection() {
        AbstractRemedialAction abstractRemedialAction = new InjectionSetpoint(
            "injectionId",
            "injectionName",
            "RTE",
            new NetworkElement("neId"),
            500
        );

        AbstractRemedialAction transformedAbstractRemedialAction = roundTrip(abstractRemedialAction, AbstractRemedialAction.class);
        assertEquals(transformedAbstractRemedialAction, abstractRemedialAction);
    }

    @Test
    public void abstractRemedialActionWithTopology() {
        AbstractRemedialAction abstractRemedialAction = new Topology(
            "topologyId",
            "topologyName",
            "RTE",
            new NetworkElement("neId"),
            ActionType.CLOSE
        );

        AbstractRemedialAction transformedAbstractRemedialAction = roundTrip(abstractRemedialAction, AbstractRemedialAction.class);
        assertEquals(transformedAbstractRemedialAction, abstractRemedialAction);
    }
}
