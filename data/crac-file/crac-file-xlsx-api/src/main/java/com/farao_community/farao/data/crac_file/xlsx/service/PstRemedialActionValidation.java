/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx.service;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.*;
import com.farao_community.farao.data.crac_file.xlsx.model.Activation;
import com.farao_community.farao.data.crac_file.xlsx.model.RaPstTap;
import io.vavr.control.Validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_file.xlsx.service.CracTools.getOrderCodeElementName;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class PstRemedialActionValidation {

    private PstRemedialActionValidation() {
        throw new AssertionError("Utility class should not have constructor");
    }

    public static List<RemedialAction> pstRemedialActionValidation(Validation<FaraoException, List<RaPstTap>> raPstTaps) {
        return raPstTaps.toStream().flatMap(Function.identity())
                .filter(PstRemedialActionValidation::keepPstRemedialActionFilter)
                .map(raPstTap -> {
                    List<UsageRule> usageRuleList = usageRulesPstValidation(raPstTap);
                    List<RemedialActionElement> rae = pstElementsValidation(raPstTap);

                    return RemedialAction.builder()
                            .name(raPstTap.getUniqueRaPstTab())
                            .usageRules(usageRuleList)
                            .remedialActionElements(rae)
                            .id(raPstTap.getUniqueRaPstTab())
                            .build();
                }).collect(Collectors.toList());
    }

    private static List<RemedialActionElement> pstElementsValidation(RaPstTap raPstTap) {
        return Collections.singletonList(pstElementBuilder(raPstTap));
    }

    private static boolean keepPstRemedialActionFilter(RaPstTap raPstTap) {
        return Objects.nonNull(raPstTap) &&
                Objects.nonNull(raPstTap.getActivation()) &&
                raPstTap.getActivation().equals(Activation.YES);
    }

    private static List<UsageRule> usageRulesPstValidation(RaPstTap raPstTap) {
        List<UsageRule> usageRules = new ArrayList<>();

        if (null != raPstTap.getPreventive() && raPstTap.getPreventive().equals(Activation.YES)) {
            usageRules.add(preventiveUsage(raPstTap.getUniqueRaPstTab()));
        }
        if (null != raPstTap.getCurative() && raPstTap.getCurative().equals(Activation.YES)) {
            usageRules.add(curativeUsage(raPstTap.getUniqueRaPstTab()));
        }
        return usageRules;
    }

    private static PstElement pstElementBuilder(RaPstTap pstExcel) {
        String[] ranges;
        if (null != pstExcel.getRange()) {
            ranges = pstExcel.getRange().split(";");
        } else {
            return null;
        }
        String id = getOrderCodeElementName(pstExcel.getElementDescriptionMode(),
                pstExcel.getUctNodeFrom(),
                pstExcel.getUctNodeTo(),
                pstExcel.getOrdercodeElementName());

        return PstElement.builder()
                .id(id)
                .typeOfLimit(TypeOfLimit.ABSOLUTE)
                .minStepRange(Integer.parseInt(ranges[0].trim()))
                .maxStepRange(Integer.parseInt(ranges[1].trim()))
                .build();
    }

    private static UsageRule preventiveUsage(String id) {
        return UsageRule.builder()
                .id(id)
                .instants(UsageRule.Instant.N)
                .usage(UsageRule.Usage.FREE_TO_USE)
                .build();
    }

    private static UsageRule curativeUsage(String id) {
        return UsageRule.builder()
                .id(id)
                .instants(UsageRule.Instant.CURATIVE)
                .usage(UsageRule.Usage.FREE_TO_USE)
                .build();
    }
}
