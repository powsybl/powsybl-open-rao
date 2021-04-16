/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_io_json.serializers;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class CracSerializer extends AbstractJsonSerializer<Crac> {

    @Override
    public void serialize(Crac crac, JsonGenerator gen, SerializerProvider serializers) throws IOException {

        // todo: suggestion, ajouter une petite en-tête pour dire que le Crac a été généré par farao avec un lien vers le site web ?

        gen.writeStringField(ID, crac.getId());
        gen.writeStringField(NAME, crac.getName());

        writeNetworkElements(crac, gen);
        writeContingencies(crac, gen);
        writeFlowCnecs(crac, gen);
        /*
        gen.writeArrayFieldStart(CNECS);
        for (BranchCnec cnec : value.getFlowCnecs()) {
            gen.writeObject(cnec);
        }
        gen.writeEndArray();
        gen.writeArrayFieldStart(RANGE_ACTIONS);
        for (RangeAction rangeAction: value.getRangeActions()) {
            gen.writeObject(rangeAction);
        }
        gen.writeEndArray();
        gen.writeArrayFieldStart(NETWORK_ACTIONS);
        for (NetworkAction networkAction : value.getNetworkActions()) {
            gen.writeObject(networkAction);
        }
        gen.writeEndArray();

         */
        JsonUtil.writeExtensions(crac, gen, serializers, ExtensionsHandler.getExtensionsSerializers());
    }

    private void writeNetworkElements(Crac crac, JsonGenerator gen) throws IOException {
        Set<NetworkElement> networkElements = new HashSet<>();

        // Get network elements from Cnecs
        Set<NetworkElement> cnecNetworkElements = crac.getCnecs().stream().map(Cnec::getNetworkElement).collect(Collectors.toSet());
        networkElements.addAll(cnecNetworkElements);

        // Get network elements from Contingencies
        Set<NetworkElement> contingencyNetworkElements = crac.getContingencies().stream().map(Contingency::getNetworkElements).flatMap(Set::stream).collect(Collectors.toSet());
        networkElements.addAll(contingencyNetworkElements);

        // Get network elements from RemedialActions
        Set<NetworkElement> remedialActionNetworkElements = new HashSet<>();
        crac.getRemedialActions().forEach(remedialAction -> remedialActionNetworkElements.addAll(remedialAction.getNetworkElements()));
        networkElements.addAll(remedialActionNetworkElements);

        // Write all
        gen.writeArrayFieldStart(NETWORK_ELEMENTS);

        List<NetworkElement> sortedListOfNetworkElements = networkElements.stream()
                .sorted(Comparator.comparing(NetworkElement::getId))
                .collect(Collectors.toList());

        for (NetworkElement networkElement : sortedListOfNetworkElements) {
            gen.writeObject(networkElement);
        }

        gen.writeEndArray();
    }

    private void writeContingencies(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(CONTINGENCIES);

        List<Contingency> sortedListOfContingencies = crac.getContingencies().stream()
                .sorted(Comparator.comparing(Contingency::getId))
                .collect(Collectors.toList());

        for (Contingency contingency : sortedListOfContingencies) {
            gen.writeObject(contingency);
        }

        gen.writeEndArray();
    }

    private void writeFlowCnecs(Crac crac, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(FLOW_CNECS);

        List<FlowCnec> sortedListOfCnecs = crac.getFlowCnecs().stream()
                .sorted(Comparator.comparing(FlowCnec::getId))
                .collect(Collectors.toList());

        for (FlowCnec flowCnec : sortedListOfCnecs) {
            gen.writeObject(flowCnec);
        }

        gen.writeEndArray();
    }
}
