/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.virtual_hubs.network_extension;

import com.google.auto.service.AutoService;
import com.powsybl.commons.extensions.AbstractExtensionXmlSerializer;
import com.powsybl.commons.extensions.ExtensionXmlSerializer;
import com.powsybl.commons.xml.XmlReaderContext;
import com.powsybl.commons.xml.XmlWriterContext;
import com.powsybl.iidm.network.Injection;

import javax.xml.stream.XMLStreamException;
import java.util.Objects;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot@rte-france.com>}
 */
@AutoService(ExtensionXmlSerializer.class)
public class AssignedVirtualHubXmlSerializer<T extends Injection<T>> extends AbstractExtensionXmlSerializer<T, AssignedVirtualHub<T>> {

    public AssignedVirtualHubXmlSerializer() {
        super("assignedVirtualHub", "network", AssignedVirtualHub.class, false,
            "assignedVirtualHub.xsd",
            "https://farao-community.github.io/schema/iidm/ext/virtual_hub_extension/1_0", "farao");
    }

    @Override
    public void write(AssignedVirtualHub<T> assignedVirtualHub, XmlWriterContext context) throws XMLStreamException {
        if (!Objects.isNull(assignedVirtualHub.getCode())) {
            context.getExtensionsWriter().writeAttribute("code", assignedVirtualHub.getCode());
        }
        context.getExtensionsWriter().writeAttribute("eic", assignedVirtualHub.getEic());
        context.getExtensionsWriter().writeAttribute("isMcParticipant", Boolean.toString(assignedVirtualHub.isMcParticipant()));

        if (!Objects.isNull(assignedVirtualHub.getNodeName())) {
            context.getExtensionsWriter().writeAttribute("nodeName", assignedVirtualHub.getNodeName());
        }
        if (!Objects.isNull(assignedVirtualHub.getRelatedMa())) {
            context.getExtensionsWriter().writeAttribute("relatedMa", assignedVirtualHub.getRelatedMa());
        }
    }

    @Override
    public AssignedVirtualHub<T> read(T extendable, XmlReaderContext context) {
        String code = context.getReader().getAttributeValue(null, "code");
        String eic = context.getReader().getAttributeValue(null, "eic");
        String isMcParticipantAsString = context.getReader().getAttributeValue(null, "isMcParticipant");
        String nodeName = context.getReader().getAttributeValue(null, "nodeName");
        String relatedMa = context.getReader().getAttributeValue(null, "relatedMa");

        boolean isMcParticipant = false;
        if (isMcParticipantAsString.equals(Boolean.toString(true))) {
            isMcParticipant = true;
        }

        extendable.newExtension(AssignedVirtualHubAdder.class).withCode(code).withEic(eic).withMcParticipant(isMcParticipant).withNodeName(nodeName).withRelatedMa(relatedMa).add();
        return extendable.getExtension(AssignedVirtualHub.class);
    }
}
