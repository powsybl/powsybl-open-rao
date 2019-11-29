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

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.Contingency;
import com.farao_community.farao.data.crac_file.ContingencyElement;
import com.farao_community.farao.data.crac_file.MonitoredBranch;
import com.farao_community.farao.data.crac_file.xlsx.model.Activation;
import com.farao_community.farao.data.crac_file.xlsx.model.ContingencyElementXlsx;
import io.vavr.control.Validation;

import java.util.*;
import java.util.stream.Collectors;

public class PostContingencyValidation {
    ContingencyValidation contingencyValidation;

    public PostContingencyValidation() {
        this.contingencyValidation = new ContingencyValidation();
    }

    /**
     * Validation of postContingency
     * @param contingencyElementsValidation
     * @param monitoredBranchesHashMap
     */
    public List<Contingency> postContingencyElementValidation(Validation<FaraoException, List<ContingencyElementXlsx>> contingencyElementsValidation, Map<String, List<MonitoredBranch>> monitoredBranchesHashMap) {
        List<Contingency> postContingency = new ArrayList<>();

        contingencyElementsValidation.forEach(contingenciesElementXlsx -> {
            // if the activation is NO the contingency is not imported
            contingenciesElementXlsx = filterPostContingencyActivation(contingenciesElementXlsx);

            contingenciesElementXlsx.stream().forEach(contingencyElementXlsx -> {
                List<ContingencyElement> contingencyElementList = new ArrayList<>();

                if (null != contingencyElementXlsx.getDescriptionModeTimeseries1() && contingencyElementList.isEmpty()) {
                    String id = CracTools.getOrderCodeElementName(contingencyElementXlsx.getDescriptionModeTimeseries1(), contingencyElementXlsx.getUctNodeFromTimeseries1(), contingencyElementXlsx.getUctNodeToTimeseries1(), contingencyElementXlsx.getOrderCodeOrElementNameTimeseries1());
                    contingencyElementList.add(contingencyValidation.buildContingencyElement(contingencyElementXlsx.getUniqueCOName(), id));
                }

                if (null != contingencyElementXlsx.getDescriptionModeTimeseries2() && contingencyElementList.size() == 1) {
                    String id = CracTools.getOrderCodeElementName(contingencyElementXlsx.getDescriptionModeTimeseries2(), contingencyElementXlsx.getUctNodeFromTimeseries2(), contingencyElementXlsx.getUctNodeToTimeseries2(), contingencyElementXlsx.getOrderCodeOrElementNameTimeseries2());
                    contingencyElementList.add(contingencyValidation.buildContingencyElement(contingencyElementXlsx.getUniqueCOName(), id));
                }

                if (null != contingencyElementXlsx.getDescriptionModeTimeseries3() && contingencyElementList.size() == 2) {
                    String id = CracTools.getOrderCodeElementName(contingencyElementXlsx.getDescriptionModeTimeseries3(), contingencyElementXlsx.getUctNodeFromTimeseries3(), contingencyElementXlsx.getUctNodeToTimeseries3(), contingencyElementXlsx.getOrderCodeOrElementNameTimeseries3());
                    contingencyElementList.add(contingencyValidation.buildContingencyElement(contingencyElementXlsx.getUniqueCOName(), id));
                }

                postContingency.add(
                        Contingency.builder()
                                .name(contingencyElementXlsx.getUniqueCOName())
                                .id(contingencyElementXlsx.getUniqueCOName())
                                .contingencyElements(contingencyElementList)
                                .monitoredBranches(monitoredBranchesHashMap.getOrDefault(contingencyElementXlsx.getUniqueCOName(), Collections.emptyList()))
                                .build());
            });
        });
        return postContingency;
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
