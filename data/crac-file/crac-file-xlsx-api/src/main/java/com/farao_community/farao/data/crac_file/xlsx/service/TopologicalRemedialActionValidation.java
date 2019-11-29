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
import com.farao_community.farao.data.crac_file.xlsx.model.RaTopologyXlsx;
import com.farao_community.farao.data.crac_file.xlsx.model.Status;
import io.vavr.control.Validation;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class TopologicalRemedialActionValidation {

    private TopologicalRemedialActionValidation() {
        throw new AssertionError("Utility class should not have constructor");
    }

    public static List<RemedialAction> topologicalRemedialActionValidation(Validation<FaraoException, List<RaTopologyXlsx>> raTopology) {
        return raTopology.toStream().flatMap(Function.identity())
                .filter(TopologicalRemedialActionValidation::keepTopologicalRemedialActionFilter)
                .map(raTopologyElement -> {
                    List<UsageRule> usageRuleList = usageRulesTopologyValidation(raTopologyElement);
                    List<RemedialActionElement> rae = topologicalElementsValidation(raTopologyElement);
                    return RemedialAction.builder()
                            .name(raTopologyElement.getName())
                            .usageRules(usageRuleList)
                            .remedialActionElements(rae)
                            .id(raTopologyElement.getName())
                            .build();
                }).collect(Collectors.toList());
    }

    private static List<RemedialActionElement> topologicalElementsValidation(RaTopologyXlsx raTopology) {
        return Collections.singletonList(topologicalElementBuilder(raTopology));
    }

    private static boolean keepTopologicalRemedialActionFilter(RaTopologyXlsx raTopology) {
        return Objects.nonNull(raTopology) &&
                Objects.nonNull(raTopology.getActivation()) &&
                raTopology.getActivation().equals(Activation.YES);
    }

    private static List<UsageRule> usageRulesTopologyValidation(RaTopologyXlsx raTopology) {
        List<UsageRule> usageRules = new ArrayList<>();

        if (null != raTopology.getPreventive() && raTopology.getPreventive().equals(Activation.YES)) {
            usageRules.add(preventiveUsage(raTopology.getName()));
        }
        return usageRules;
    }

    private static TopologicalAction.Status convertStatus(Status status) {
        switch (status) {
            case OPEN:
                return TopologicalAction.Status.OPEN;
            case CLOSE:
                return TopologicalAction.Status.CLOSE;
            default:
                throw new AssertionError("Should not be there");
        }
    }

    private static TopologicalAction topologicalElementBuilder(RaTopologyXlsx raTopology) {
        Map<String, TopologicalAction.Status> elementaryActions = new HashMap<>();

        if (null != raTopology.getElementDescriptionMode1() && null != raTopology.getUctNodeFrom1()
            && null != raTopology.getUctNodeTo1() && null != raTopology.getOrderCodeElementName1() && null != raTopology.getStatus1()) {
            String id1 = CracTools.getOrderCodeElementName(raTopology.getElementDescriptionMode1(), raTopology.getUctNodeFrom1(), raTopology.getUctNodeTo1(), raTopology.getOrderCodeElementName1());
            elementaryActions.put(id1, convertStatus(raTopology.getStatus1()));
        }

        if (null != raTopology.getElementDescriptionMode2() && null != raTopology.getUctNodeFrom2()
                && null != raTopology.getUctNodeTo2() && null != raTopology.getOrderCodeElementName2() && null != raTopology.getStatus2()) {
            String id2 = CracTools.getOrderCodeElementName(raTopology.getElementDescriptionMode1(), raTopology.getUctNodeFrom1(), raTopology.getUctNodeTo1(), raTopology.getOrderCodeElementName1());
            elementaryActions.put(id2, convertStatus(raTopology.getStatus2()));
        }

        if (null != raTopology.getElementDescriptionMode3() && null != raTopology.getUctNodeFrom3()
                && null != raTopology.getUctNodeTo3() && null != raTopology.getOrderCodeElementName3() && null != raTopology.getStatus3()) {
            String id3 = CracTools.getOrderCodeElementName(raTopology.getElementDescriptionMode3(), raTopology.getUctNodeFrom3(), raTopology.getUctNodeTo3(), raTopology.getOrderCodeElementName3());
            elementaryActions.put(id3, convertStatus(raTopology.getStatus3()));
        }

        if (null != raTopology.getElementDescriptionMode4() && null != raTopology.getUctNodeFrom4()
                && null != raTopology.getUctNodeTo4() && null != raTopology.getOrderCodeElementName4() && null != raTopology.getStatus4()) {
            String id4 = CracTools.getOrderCodeElementName(raTopology.getElementDescriptionMode4(), raTopology.getUctNodeFrom4(), raTopology.getUctNodeTo4(), raTopology.getOrderCodeElementName4());
            elementaryActions.put(id4, convertStatus(raTopology.getStatus4()));
        }

        if (null != raTopology.getElementDescriptionMode5() && null != raTopology.getUctNodeFrom5()
                && null != raTopology.getUctNodeTo5() && null != raTopology.getOrderCodeElementName5() && null != raTopology.getStatus5()) {
            String id5 = CracTools.getOrderCodeElementName(raTopology.getElementDescriptionMode5(), raTopology.getUctNodeFrom5(), raTopology.getUctNodeTo5(), raTopology.getOrderCodeElementName5());
            elementaryActions.put(id5, convertStatus(raTopology.getStatus5()));
        }

        if (null != raTopology.getElementDescriptionMode6() && null != raTopology.getUctNodeFrom6()
                && null != raTopology.getUctNodeTo6() && null != raTopology.getOrderCodeElementName6() && null != raTopology.getStatus6()) {
            String id6 = CracTools.getOrderCodeElementName(raTopology.getElementDescriptionMode6(), raTopology.getUctNodeFrom6(), raTopology.getUctNodeTo6(), raTopology.getOrderCodeElementName6());
            elementaryActions.put(id6, convertStatus(raTopology.getStatus6()));
        }

        if (null != raTopology.getElementDescriptionMode7() && null != raTopology.getUctNodeFrom7()
                && null != raTopology.getUctNodeTo7() && null != raTopology.getOrderCodeElementName7() && null != raTopology.getStatus7()) {
            String id7 = CracTools.getOrderCodeElementName(raTopology.getElementDescriptionMode7(), raTopology.getUctNodeFrom7(), raTopology.getUctNodeTo7(), raTopology.getOrderCodeElementName7());
            elementaryActions.put(id7, convertStatus(raTopology.getStatus7()));
        }

        if (null != raTopology.getElementDescriptionMode8() && null != raTopology.getUctNodeFrom8()
                && null != raTopology.getUctNodeTo8() && null != raTopology.getOrderCodeElementName8() && null != raTopology.getStatus8()) {
            String id8 = CracTools.getOrderCodeElementName(raTopology.getElementDescriptionMode8(), raTopology.getUctNodeFrom8(), raTopology.getUctNodeTo8(), raTopology.getOrderCodeElementName8());
            elementaryActions.put(id8, convertStatus(raTopology.getStatus8()));
        }

        return TopologicalAction.builder()
                .id(raTopology.getName())
                .actions(elementaryActions)
                .build();
    }

    private static UsageRule preventiveUsage(String id) {
        return UsageRule.builder()
                .id(id)
                .instants(UsageRule.Instant.N)
                .usage(UsageRule.Usage.FREE_TO_USE)
                .build();
    }
}
