/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.openrao.data.cracio.json.serializers;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracio.json.ExtensionsHandler;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.*;
import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.INSTANT;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class CracSerializer extends AbstractJsonSerializer<Crac> {

    @Override
    public void serialize(Crac crac, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        gen.writeStringField(TYPE, CRAC_TYPE);
        gen.writeStringField(VERSION, CRAC_IO_VERSION);
        gen.writeStringField(INFO, CRAC_INFO);
        gen.writeStringField(ID, crac.getId());
        gen.writeStringField(NAME, crac.getName());

        serializeInstants(crac, gen);
        serializeRaUsageLimits(crac, gen);
        serializeNetworkElements(crac, gen);
        serializeContingencies(crac, gen);
        serializeFlowCnecs(crac, gen);
        serializeAngleCnecs(crac, gen);
        serializeVoltageCnecs(crac, gen);
        serializePstRangeActions(crac, gen);
        serializeHvdcRangeActions(crac, gen);
        serializeInjectionRangeActions(crac, gen);
        serializeCounterTradeRangeActions(crac, gen);
        serializeNetworkActions(crac, gen);

        JsonUtil.writeExtensions(crac, gen, serializers, ExtensionsHandler.getExtensionsSerializers());

        gen.writeEndObject();
    }

    private void serializeRaUsageLimits(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(RA_USAGE_LIMITS_PER_INSTANT);
        for (Map.Entry<Instant, RaUsageLimits> entry : crac.getRaUsageLimitsPerInstant().entrySet()) {
            RaUsageLimits raUsageLimits = entry.getValue();
            gen.writeStartObject();
            gen.writeStringField(INSTANT, entry.getKey().getId());
            gen.writeNumberField(MAX_RA, raUsageLimits.getMaxRa());
            gen.writeNumberField(MAX_TSO, raUsageLimits.getMaxTso());
            gen.writeObjectField(MAX_TOPO_PER_TSO, new TreeMap<>(raUsageLimits.getMaxTopoPerTso()));
            gen.writeObjectField(MAX_PST_PER_TSO, new TreeMap<>(raUsageLimits.getMaxPstPerTso()));
            gen.writeObjectField(MAX_RA_PER_TSO, new TreeMap<>(raUsageLimits.getMaxRaPerTso()));
            gen.writeObjectField(MAX_ELEMENTARY_ACTIONS_PER_TSO, new TreeMap<>(raUsageLimits.getMaxElementaryActionsPerTso()));
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }

    private void serializeInstants(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(INSTANTS);
        List<Instant> instants = crac.getSortedInstants();
        for (Instant instant : instants) {
            gen.writeObject(instant);
        }
        gen.writeEndArray();
    }

    private void serializeNetworkElements(Crac crac, JsonGenerator gen) throws IOException {
        Map<String, String> networkElementsNamesPerId = new HashMap<>();

        // Get network elements from Cnecs
        for (Set<NetworkElement> networkElements : crac.getCnecs().stream().map(Cnec::getNetworkElements).collect(Collectors.toSet())) {
            networkElements.stream().filter(networkElement -> !networkElement.getId().equals(networkElement.getName()))
                .forEach(networkElement -> networkElementsNamesPerId.put(networkElement.getId(), networkElement.getName()));
        }

        // Get network elements from RemedialActions
        for (RemedialAction<?> remedialAction : crac.getRemedialActions()) {
            remedialAction.getNetworkElements().stream().filter(networkElement -> !networkElement.getId().equals(networkElement.getName()))
                    .forEach(networkElement -> networkElementsNamesPerId.put(networkElement.getId(), networkElement.getName()));
        }

        // Write all
        gen.writeObjectField(NETWORK_ELEMENTS_NAME_PER_ID, networkElementsNamesPerId);
    }

    private void serializeContingencies(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(CONTINGENCIES);
        List<Contingency> sortedListOfContingencies = crac.getContingencies().stream()
                .sorted(Comparator.comparing(Contingency::getId))
                .toList();
        for (Contingency contingency : sortedListOfContingencies) {
            gen.writeObject(contingency);
        }
        gen.writeEndArray();
    }

    private void serializeFlowCnecs(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(FLOW_CNECS);
        List<FlowCnec> sortedListOfCnecs = crac.getFlowCnecs().stream()
                .sorted(Comparator.comparing(FlowCnec::getId))
                .toList();
        for (FlowCnec flowCnec : sortedListOfCnecs) {
            gen.writeObject(flowCnec);
        }
        gen.writeEndArray();
    }

    private void serializeAngleCnecs(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(ANGLE_CNECS);
        List<AngleCnec> sortedListOfCnecs = crac.getAngleCnecs().stream()
            .sorted(Comparator.comparing(AngleCnec::getId))
            .toList();
        for (AngleCnec angleCnec : sortedListOfCnecs) {
            gen.writeObject(angleCnec);
        }
        gen.writeEndArray();
    }

    private void serializeVoltageCnecs(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(VOLTAGE_CNECS);
        List<VoltageCnec> sortedListOfCnecs = crac.getVoltageCnecs().stream()
            .sorted(Comparator.comparing(VoltageCnec::getId))
            .toList();
        for (VoltageCnec voltageCnec : sortedListOfCnecs) {
            gen.writeObject(voltageCnec);
        }
        gen.writeEndArray();
    }

    private void serializePstRangeActions(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(PST_RANGE_ACTIONS);
        List<PstRangeAction> sortedListPsts = crac.getPstRangeActions().stream()
                .sorted(Comparator.comparing(PstRangeAction::getId))
                .toList();
        for (PstRangeAction pstRangeAction : sortedListPsts) {
            gen.writeObject(pstRangeAction);
        }
        gen.writeEndArray();
    }

    private void serializeHvdcRangeActions(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(HVDC_RANGE_ACTIONS);
        List<HvdcRangeAction> sortedListHvdcs = crac.getHvdcRangeActions().stream()
                .sorted(Comparator.comparing(HvdcRangeAction::getId))
                .toList();
        for (HvdcRangeAction hvdcRangeAction : sortedListHvdcs) {
            gen.writeObject(hvdcRangeAction);
        }
        gen.writeEndArray();
    }

    private void serializeInjectionRangeActions(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(INJECTION_RANGE_ACTIONS);
        List<InjectionRangeAction> sortedInjectionRangeActionList = crac.getInjectionRangeActions().stream()
                .sorted(Comparator.comparing(InjectionRangeAction::getId))
                .toList();
        for (InjectionRangeAction injectionRangeAction : sortedInjectionRangeActionList) {
            gen.writeObject(injectionRangeAction);
        }
        gen.writeEndArray();
    }

    private void serializeCounterTradeRangeActions(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(COUNTER_TRADE_RANGE_ACTIONS);
        List<CounterTradeRangeAction> sortedCounterTradeRangeActionList = crac.getCounterTradeRangeActions().stream()
                .sorted(Comparator.comparing(CounterTradeRangeAction::getId))
                .toList();
        for (CounterTradeRangeAction counterTradeRangeAction : sortedCounterTradeRangeActionList) {
            gen.writeObject(counterTradeRangeAction);
        }
        gen.writeEndArray();
    }

    private void serializeNetworkActions(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(NETWORK_ACTIONS);
        List<NetworkAction> sortedList = crac.getNetworkActions().stream()
                .sorted(Comparator.comparing(NetworkAction::getId))
                .toList();
        for (NetworkAction networkAction : sortedList) {
            gen.writeObject(networkAction);
        }
        gen.writeEndArray();
    }
}
