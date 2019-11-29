/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
package com.farao_community.farao.data.crac_file.xlsx.service;

import com.farao_community.farao.data.crac_file.ContingencyElement;
import com.farao_community.farao.data.crac_file.xlsx.model.Activation;
import com.farao_community.farao.data.crac_file.xlsx.model.ContingencyElementXlsx;

import java.util.List;
import java.util.stream.Collectors;

public class ContingencyValidation {
    /**
     * Method for specify how information if you want set in contingencyElement object
     * @param name
     * @param elementId
     */
    public ContingencyElement buildContingencyElement(String name, String elementId) {
        return ContingencyElement.builder()
                .name(name)
                .elementId(elementId)
                .build();
    }

    /** filtering before add contingency
     * @param postContingency
     */
    public List<ContingencyElementXlsx> filterPostContingencyActivation(List <ContingencyElementXlsx> postContingency) {
        return postContingency.stream()
                .filter(postContingencyXlsx -> postContingencyXlsx.getActivation().equals(Activation.YES))
                .collect(Collectors.toList());
    }
}
