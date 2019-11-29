/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx.service;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.RedispatchRemedialActionElement;
import com.farao_community.farao.data.crac_file.RemedialAction;
import com.farao_community.farao.data.crac_file.RemedialActionElement;
import com.farao_community.farao.data.crac_file.UsageRule;
import com.farao_community.farao.data.crac_file.xlsx.model.Activation;
import com.farao_community.farao.data.crac_file.xlsx.model.RaTimeSeries;
import com.farao_community.farao.data.crac_file.xlsx.model.RedispatchingRemedialActionXlsx;
import com.farao_community.farao.data.crac_file.xlsx.model.TimesSeries;
import io.vavr.control.Validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class RdRemedialActionValidation {

    private static final int MAX_NODEID_LENGTH = 8;

    private RdRemedialActionValidation() {
        throw new AssertionError("Utility class should not have constructor");
    }

    private static HashMap<String, List<RemedialActionElement>> remedialActionElementValidations(Validation<FaraoException, List<RedispatchingRemedialActionXlsx>> redispatchingRemedialActionXlsx) {
        HashMap<String, List<RemedialActionElement>> remedialActionElementHashMap = new HashMap<>();

        redispatchingRemedialActionXlsx.forEach(remedialActionList -> remedialActionList.forEach(remedialActionXlsx -> {
            List<RemedialActionElement> remedialActionElementsTempList = new ArrayList<>();
            if (remedialActionElementHashMap.get(remedialActionXlsx.getRaRdId()) != null) {
                // finding good remedial action element
                List<RemedialActionElement> remedialActionElements = remedialActionElementHashMap.get(remedialActionXlsx.getRaRdId());
                // adding the actual remedial action element
                remedialActionElements.add(buildRemedialActionElements(remedialActionXlsx));
                // put in the hashmap, the last list is crushed
                remedialActionElementHashMap.put(remedialActionXlsx.getRaRdId(), remedialActionElements);
            } else {
                // if the object is null i put a new list
                remedialActionElementsTempList.add(buildRemedialActionElements(remedialActionXlsx));
                remedialActionElementHashMap.put(remedialActionXlsx.getRaRdId(), remedialActionElementsTempList);
            }
        }));
        return remedialActionElementHashMap;
    }

    private static HashMap<String, List<UsageRule>> usageRulesRedispatchingValidation(Validation<FaraoException, List<RedispatchingRemedialActionXlsx>> redispatchingRemedialActionXlsx) {
        HashMap<String, List<UsageRule>> usageRuleHashMap = new HashMap<>();

        redispatchingRemedialActionXlsx.forEach(remedialActionList -> remedialActionList.forEach(ra -> {
            List<UsageRule> usageRules = new ArrayList<>();

            if (usageRuleHashMap.get(ra.getRaRdId()) != null) {
                usageRules = usageRuleHashMap.get(ra.getRaRdId());
            } else {
                if (null != ra.getPreventive() && ra.getPreventive().equals(Activation.YES)) {
                    usageRules.add(preventiveUsage(ra.getRaRdId()));
                }
                if (null != ra.getCurative() && ra.getCurative().equals(Activation.YES)) {
                    usageRules.add(curativeUsage(ra.getRaRdId()));
                }
            }
            usageRuleHashMap.put(ra.getRaRdId(), usageRules);
        }));
        return usageRuleHashMap;
    }

    public static List<RemedialAction> rdRemedialActionValidation(Validation<FaraoException, List<RedispatchingRemedialActionXlsx>> redispatchingRemedialActionXlsx, Validation<FaraoException, List<RaTimeSeries>> radTimeSeries, TimesSeries timesSeries) {
        List<RemedialAction> remedialActionList = new ArrayList<>();
        HashMap<String, List<UsageRule>> usageRuleHashMap = usageRulesRedispatchingValidation(redispatchingRemedialActionXlsx);
        redispatchingRemedialActionXlsx.forEach(ras -> {
            List<Float> valueTs = new ArrayList<Float>();
            ras = filterRedispatchingRemedialActionXlsx(ras);
            ras.stream().forEach(ra -> {
                radTimeSeries.filter(Objects::nonNull).forEach(timeSerie -> {
                    timeSerie.forEach(ts -> {
                        ts.stream().forEach(raTimeSeries -> {
                            if (null != raTimeSeries.getRaId() && raTimeSeries.getRaId().equals(ra.getRaRdId())) {
                                valueTs.add(raTimeSeries.getCurentLimit1(timesSeries));
                            }
                        });
                    });
                });
                if (null == ra.getRaRdId()) {

                } else {
                    remedialActionList.add(buildRemedialAction(ra, usageRuleHashMap.get(ra.getRaRdId()), remedialActionElementValidations(redispatchingRemedialActionXlsx).get(ra.getRaRdId())));
                }
            });
        });
        return remedialActionList;
    }

    private static List<RedispatchingRemedialActionXlsx> filterRedispatchingRemedialActionXlsx(List<RedispatchingRemedialActionXlsx> rrad) {
        return rrad.stream()
                .filter(ra -> null != ra.getActivation())
                .filter(ra -> ra.getActivation().equals(Activation.YES))
                .filter(ra -> null != ra.getCurative() && null != ra.getPreventive())
                .filter(ra -> ra.getPreventive().equals(Activation.YES) || ra.getCurative().equals(Activation.YES))
                .collect(Collectors.toList());
    }

    private static RemedialActionElement buildRemedialActionElements(RedispatchingRemedialActionXlsx ra) {
        String spaceNode = "";
        int nodeSpace = 0;
        if (ra.getUctNodeOrGsk() != null && ra.getUctNodeOrGsk().length() != 0) {
            nodeSpace = MAX_NODEID_LENGTH - ra.getUctNodeOrGsk().length();
        }
        for (int i = 0; i < nodeSpace; i++) {
            spaceNode = spaceNode + " ";
        }
        String id = ra.getUctNodeOrGsk() + spaceNode + "_generator";
        return RedispatchRemedialActionElement.builder()
                .id(id)
                .startupCost(ra.getStartupCosts())
                .minimumPower(ra.getMinimumPower())
                .maximumPower(ra.getMaximumPower())
                .marginalCost(ra.getMarginalCosts())
                .build();
    }

    private static RemedialAction buildRemedialAction(RedispatchingRemedialActionXlsx ra, List<UsageRule> urs, List<RemedialActionElement> rae) {
        return RemedialAction.builder()
                .name(ra.getGeneratorName())
                .id(ra.getRaRdId())
                .usageRules(urs)
                .remedialActionElements(rae)
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
