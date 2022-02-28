/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.fb_constraint.crac_creator;

import com.farao_community.farao.data.crac_api.ContingencyAdder;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.xsd.OutageType;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteContingencyElementHelper;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzer;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
class OutageReader {

    private final OutageType outage;

    private boolean isOutageValid = true;
    private String invalidOutageReason = "";

    private List<String> outageElementIds;

    OutageType getOutage() {
        return outage;
    }

    boolean isOutageValid() {
        return isOutageValid;
    }

    String getInvalidOutageReason() {
        return invalidOutageReason;
    }

    void addContingency(Crac crac) {
        ContingencyAdder contingencyAdder = crac.newContingency()
            .withId(outage.getId())
            .withName(outage.getName());
        outageElementIds.forEach(contingencyAdder::withNetworkElement);
        contingencyAdder.add();
    }

    OutageReader(OutageType outage, UcteNetworkAnalyzer ucteNetworkHelper) {
        this.outage = outage;
        interpretWithNetwork(ucteNetworkHelper);
    }

    private void interpretWithNetwork(UcteNetworkAnalyzer ucteNetworkHelper) {

        if (outage.getHvdcVH().isEmpty() && outage.getBranch().isEmpty()) {
            this.isOutageValid = false;
            this.invalidOutageReason = String.format("outage %s is not valid as it contains neither 'Branch' nor 'HvdcVH' fields", outage.getId());
            return;
        }

        outageElementIds = new ArrayList<>();

        if (!outage.getBranch().isEmpty()) {

            List<UcteContingencyElementHelper> outageElementsReader = outage.getBranch().stream()
                .map(branch -> new UcteContingencyElementHelper(branch.getFrom(), branch.getTo(), branch.getOrder(), branch.getElementName(), ucteNetworkHelper))
                .collect(Collectors.toList());

            Optional<UcteContingencyElementHelper> invalidBranch = outageElementsReader.stream().filter(br -> !br.isValid()).findAny();

            if (invalidBranch.isPresent()) {
                this.isOutageValid = false;
                this.invalidOutageReason = String.format("outage %s is not valid: %s", outage.getId(), invalidBranch.get().getInvalidReason());
                return;
            } else {
                outageElementsReader.forEach(br -> outageElementIds.add(br.getIdInNetwork()));
            }
        }

        if (!outage.getHvdcVH().isEmpty()) {
            outage.getHvdcVH().forEach(hvdcVH -> {
                DanglingLine dl1 = findDanglingLineWithXnode(hvdcVH.getFrom(), ucteNetworkHelper.getNetwork());
                DanglingLine dl2 = findDanglingLineWithXnode(hvdcVH.getTo(), ucteNetworkHelper.getNetwork());

                if (Objects.isNull(dl1) || Objects.isNull(dl2)) {
                    this.isOutageValid = false;
                    this.invalidOutageReason = String.format("one of the two Xnodes of outage %s was not found in the network: %s, %s", outage.getId(), hvdcVH.getFrom(), hvdcVH.getTo());
                } else {
                    outageElementIds.add(dl1.getId());
                    outageElementIds.add(dl2.getId());
                }
            });
        }
    }

    private DanglingLine findDanglingLineWithXnode(String xNodeId, Network network) {
        return network.getDanglingLineStream().filter(danglingLine -> danglingLine.getUcteXnodeCode().equals(xNodeId)).findFirst().orElse(null);
    }
}
