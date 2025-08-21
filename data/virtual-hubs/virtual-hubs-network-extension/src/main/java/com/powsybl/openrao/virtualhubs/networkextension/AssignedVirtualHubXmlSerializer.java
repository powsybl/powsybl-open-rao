/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.virtualhubs.networkextension;

import com.google.auto.service.AutoService;
import com.powsybl.commons.extensions.AbstractExtensionSerDe;
import com.powsybl.commons.extensions.ExtensionSerDe;
import com.powsybl.commons.io.DeserializerContext;
import com.powsybl.commons.io.SerializerContext;
import com.powsybl.iidm.network.Injection;

import java.util.Objects;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(ExtensionSerDe.class)
public class AssignedVirtualHubXmlSerializer<T extends Injection<T>> extends AbstractExtensionSerDe<T, AssignedVirtualHub<T>> {

    public AssignedVirtualHubXmlSerializer() {
        super("assignedVirtualHub", "network", AssignedVirtualHub.class,
            "assignedVirtualHub.xsd",
            "https://farao-community.github.io/schema/iidm/ext/virtual_hub_extension/1_0", "farao");
    }

    @Override
    public void write(AssignedVirtualHub<T> assignedVirtualHub, SerializerContext context) {
        if (!Objects.isNull(assignedVirtualHub.getCode())) {
            context.getWriter().writeStringAttribute("code", assignedVirtualHub.getCode());
        }
        context.getWriter().writeStringAttribute("eic", assignedVirtualHub.getEic());
        context.getWriter().writeStringAttribute("isMcParticipant", Boolean.toString(assignedVirtualHub.isMcParticipant()));

        if (!Objects.isNull(assignedVirtualHub.getNodeName())) {
            context.getWriter().writeStringAttribute("nodeName", assignedVirtualHub.getNodeName());
        }
        if (!Objects.isNull(assignedVirtualHub.getRelatedMa())) {
            context.getWriter().writeStringAttribute("relatedMa", assignedVirtualHub.getRelatedMa());
        }
    }

    @Override
    public AssignedVirtualHub<T> read(T extendable, DeserializerContext context) {
        String code = context.getReader().readStringAttribute("code");
        String eic = context.getReader().readStringAttribute("eic");
        String isMcParticipantAsString = context.getReader().readStringAttribute("isMcParticipant");
        String nodeName = context.getReader().readStringAttribute("nodeName");
        String relatedMa = context.getReader().readStringAttribute("relatedMa");

        boolean isMcParticipant = false;
        if (isMcParticipantAsString.equals(Boolean.toString(true))) {
            isMcParticipant = true;
        }

        extendable.newExtension(AssignedVirtualHubAdder.class).withCode(code).withEic(eic).withMcParticipant(isMcParticipant).withNodeName(nodeName).withRelatedMa(relatedMa).add();
        return extendable.getExtension(AssignedVirtualHub.class);
    }
}
