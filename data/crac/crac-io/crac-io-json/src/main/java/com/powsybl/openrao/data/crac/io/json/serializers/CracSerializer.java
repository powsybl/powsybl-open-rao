/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.openrao.data.crac.io.json.serializers;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.io.json.ExtensionsHandler;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParametersConstants;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class CracSerializer extends AbstractJsonSerializer<Crac> {

    @Override
    public void serialize(Crac crac, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        gen.writeStringField(JsonSerializationConstants.TYPE, JsonSerializationConstants.CRAC_TYPE);
        gen.writeStringField(JsonSerializationConstants.VERSION, JsonSerializationConstants.CRAC_IO_VERSION);
        gen.writeStringField(JsonSerializationConstants.INFO, JsonSerializationConstants.CRAC_INFO);
        gen.writeStringField(JsonSerializationConstants.ID, crac.getId());
        gen.writeStringField(JsonSerializationConstants.NAME, crac.getName());
        serializeTimestamp(crac, gen);

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

    private static void serializeTimestamp(Crac crac, JsonGenerator gen) throws IOException {
        Optional<OffsetDateTime> timestamp = crac.getTimestamp();
        if (timestamp.isPresent()) {
            gen.writeStringField(JsonSerializationConstants.TIMESTAMP, timestamp.get().format(DateTimeFormatter.ISO_DATE_TIME));
        }
    }

    private void serializeRaUsageLimits(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(JsonSerializationConstants.RA_USAGE_LIMITS_PER_INSTANT);
        for (Map.Entry<Instant, RaUsageLimits> entry : crac.getRaUsageLimitsPerInstant().entrySet()) {
            JsonCracCreationParametersConstants.serializeRaUsageLimitForOneInstant(gen, Map.entry(entry.getKey().getId(), entry.getValue()));
        }
        gen.writeEndArray();
    }

    private void serializeInstants(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(JsonSerializationConstants.INSTANTS);
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
        gen.writeObjectField(JsonSerializationConstants.NETWORK_ELEMENTS_NAME_PER_ID, networkElementsNamesPerId);
    }

    private void serializeContingencies(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(JsonSerializationConstants.CONTINGENCIES);
        List<Contingency> sortedListOfContingencies = crac.getContingencies().stream()
                .sorted(Comparator.comparing(Contingency::getId))
                .toList();
        for (Contingency contingency : sortedListOfContingencies) {
            gen.writeObject(contingency);
        }
        gen.writeEndArray();
    }

    private void serializeFlowCnecs(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(JsonSerializationConstants.FLOW_CNECS);
        List<FlowCnec> sortedListOfCnecs = crac.getFlowCnecs().stream()
                .sorted(Comparator.comparing(FlowCnec::getId))
                .toList();
        for (FlowCnec flowCnec : sortedListOfCnecs) {
            gen.writeObject(flowCnec);
        }
        gen.writeEndArray();
    }

    private void serializeAngleCnecs(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(JsonSerializationConstants.ANGLE_CNECS);
        List<AngleCnec> sortedListOfCnecs = crac.getAngleCnecs().stream()
            .sorted(Comparator.comparing(AngleCnec::getId))
            .toList();
        for (AngleCnec angleCnec : sortedListOfCnecs) {
            gen.writeObject(angleCnec);
        }
        gen.writeEndArray();
    }

    private void serializeVoltageCnecs(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(JsonSerializationConstants.VOLTAGE_CNECS);
        List<VoltageCnec> sortedListOfCnecs = crac.getVoltageCnecs().stream()
            .sorted(Comparator.comparing(VoltageCnec::getId))
            .toList();
        for (VoltageCnec voltageCnec : sortedListOfCnecs) {
            gen.writeObject(voltageCnec);
        }
        gen.writeEndArray();
    }

    private void serializePstRangeActions(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(JsonSerializationConstants.PST_RANGE_ACTIONS);
        List<PstRangeAction> sortedListPsts = crac.getPstRangeActions().stream()
                .sorted(Comparator.comparing(PstRangeAction::getId))
                .toList();
        for (PstRangeAction pstRangeAction : sortedListPsts) {
            gen.writeObject(pstRangeAction);
        }
        gen.writeEndArray();
    }

    private void serializeHvdcRangeActions(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(JsonSerializationConstants.HVDC_RANGE_ACTIONS);
        List<HvdcRangeAction> sortedListHvdcs = crac.getHvdcRangeActions().stream()
                .sorted(Comparator.comparing(HvdcRangeAction::getId))
                .toList();
        for (HvdcRangeAction hvdcRangeAction : sortedListHvdcs) {
            gen.writeObject(hvdcRangeAction);
        }
        gen.writeEndArray();
    }

    private void serializeInjectionRangeActions(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(JsonSerializationConstants.INJECTION_RANGE_ACTIONS);
        List<InjectionRangeAction> sortedInjectionRangeActionList = crac.getInjectionRangeActions().stream()
                .sorted(Comparator.comparing(InjectionRangeAction::getId))
                .toList();
        for (InjectionRangeAction injectionRangeAction : sortedInjectionRangeActionList) {
            gen.writeObject(injectionRangeAction);
        }
        gen.writeEndArray();
    }

    private void serializeCounterTradeRangeActions(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(JsonSerializationConstants.COUNTER_TRADE_RANGE_ACTIONS);
        List<CounterTradeRangeAction> sortedCounterTradeRangeActionList = crac.getCounterTradeRangeActions().stream()
                .sorted(Comparator.comparing(CounterTradeRangeAction::getId))
                .toList();
        for (CounterTradeRangeAction counterTradeRangeAction : sortedCounterTradeRangeActionList) {
            gen.writeObject(counterTradeRangeAction);
        }
        gen.writeEndArray();
    }

    private void serializeNetworkActions(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(JsonSerializationConstants.NETWORK_ACTIONS);
        List<NetworkAction> sortedList = crac.getNetworkActions().stream()
                .sorted(Comparator.comparing(NetworkAction::getId))
                .toList();
        for (NetworkAction networkAction : sortedList) {
            gen.writeObject(networkAction);
        }
        gen.writeEndArray();
    }
}
