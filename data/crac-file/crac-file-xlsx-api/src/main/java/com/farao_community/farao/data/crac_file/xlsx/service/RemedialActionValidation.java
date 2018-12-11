/**
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
import com.farao_community.farao.data.crac_file.*;
import com.farao_community.farao.data.crac_file.xlsx.model.*;
import io.vavr.control.Validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RemedialActionValidation {
    private String pstId;

    public RemedialAction remedialActionPstValidation(Validation<FaraoException, List<RaPstTap>> raPstTap) {
        List<UsageRule> usageRuleList = usageRulesPstValidation(raPstTap);
        List<RemedialActionElement> rae = pstRemedialActionValidation(raPstTap);

        return RemedialAction.builder()
                .name(pstId)
                .usageRules(usageRuleList)
                .remedialActionElements(rae)
                .id(pstId)
                .build();
    }

    private List<RemedialActionElement> pstRemedialActionValidation(Validation<FaraoException, List<RaPstTap>> raPstTap) {

        List<RemedialActionElement> pstRemedialActionElement = new ArrayList<>();
        raPstTap.forEach(pstElements -> {
            if (!pstElements.isEmpty() || null != pstElements) {
                pstElements = pstRemedialActionFilter(pstElements);
            }
            pstElements.stream()
                    .forEach(pstElement -> {
                        usageRulesPstValidation(raPstTap);
                        pstId = pstElement.getUniqueRaPstTab();
                        pstRemedialActionElement.add(pstElementBuilder(pstElement));
                    });
        });
        return pstRemedialActionElement;
    }

    private List<RaPstTap> pstRemedialActionFilter(List<RaPstTap> raPstTaps) {
        return raPstTaps.stream()
                .filter(Objects::nonNull)
                .filter(raPstTap -> null != raPstTap.getActivation())
                .filter(raPst -> raPst.getActivation().equals(Activation.YES))
                .filter(raPstTap -> null != raPstTap.getAngleRegulation())
                .filter(raPstTap -> raPstTap.getAngleRegulation().equals(Activation.YES))
                .filter(raPstTap -> null != raPstTap.getCurative() && raPstTap.getCurative().equals(Activation.YES) ||
                        null != raPstTap.getPreventive() && raPstTap.getPreventive().equals(Activation.YES))
                .collect(Collectors.toList());
    }

    private List<UsageRule> usageRulesPstValidation(Validation<FaraoException, List<RaPstTap>> raPstTap) {
        List<UsageRule> usageRules = new ArrayList<>();

        raPstTap.forEach(remedialActionList -> remedialActionList.forEach(ra -> {
            if (null != ra.getPreventive() && ra.getPreventive().equals(Activation.YES)) {
                usageRules.add(preventiveUsage(ra.getUniqueRaPstTab()));
            }
        }
        ));
        return usageRules;
    }

    private PstElement pstElementBuilder(RaPstTap pstExcel) {
        String[] ranges;
        if (null != pstExcel.getRange()) {
            ranges = pstExcel.getRange().split(";");
        } else {
            return null;
        }
        return PstElement.builder()
                .id(pstExcel.getUniqueRaPstTab())
                .typeOfLimit(TypeOfLimit.ABSOLUTE)
                .minStepRange(Integer.parseInt(ranges[0]))
                .maxStepRange(Integer.parseInt(ranges[1]))
                .build();
    }

    private HashMap<String, List<RemedialActionElement>> remedialActionElementValitions(Validation<FaraoException, List<RedispatchingRemedialActionXlsx>> redispatchingRemedialActionXlsx) {
        HashMap<String, List<RemedialActionElement>> remedialActionElementHashMap = new HashMap<>();

        redispatchingRemedialActionXlsx.forEach(remedialActionList -> remedialActionList
                .forEach(remedialActionXlsx -> {
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

    private HashMap<String, List<UsageRule>> usageRulesRedispatchingValidation(Validation<FaraoException, List<RedispatchingRemedialActionXlsx>> redispatchingRemedialActionXlsx) {
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


    public List<RemedialAction> remdialActionValidation(Validation<FaraoException, List<RedispatchingRemedialActionXlsx>> redispatchingRemedialActionXlsx, Validation<FaraoException, List<RaTimeSeries>> radTimeSeries, TimesSeries timesSeries, Validation<FaraoException, List<RaPstTap>> raPstTap) {
        List<RemedialAction>  remedialActionList = new ArrayList<>();
        HashMap<String, List<UsageRule>> usageRuleHashMap = usageRulesRedispatchingValidation(redispatchingRemedialActionXlsx);
        redispatchingRemedialActionXlsx.forEach(ras -> {
            List<Float> valueTs = new ArrayList<Float>();
            ras = filterRedispatchingRemedialActionXlsx(ras);
            ras.stream().forEach(ra -> {
                radTimeSeries.filter(Objects::nonNull)
                        .forEach(timeSerie -> timeSerie
                                .forEach(ts -> ts
                                        .stream().forEach(raTimeSeries -> {
                                            if (null != raTimeSeries.getRaId() && raTimeSeries.getRaId().equals(ra.getRaRdId())) {
                                                valueTs.add(raTimeSeries.getCurentLimit1(timesSeries));
                                            }
                                        })));
                if (null == ra.getRaRdId()) {

                } else {
                    remedialActionList.add(buildRemedialAction(ra, usageRuleHashMap.get(ra.getRaRdId()), remedialActionElementValitions(redispatchingRemedialActionXlsx).get(ra.getRaRdId())));
                }
            });
        }
        );
        return remedialActionList;
    }

    private List<RedispatchingRemedialActionXlsx> filterRedispatchingRemedialActionXlsx(List<RedispatchingRemedialActionXlsx> rrad) {
        return rrad.stream()
                .filter(ra -> null != ra.getActivation())
                .filter(ra -> ra.getActivation().equals(Activation.YES))
                .filter(ra -> null != ra.getCurative() && null != ra.getPreventive())
                .filter(ra -> ra.getPreventive().equals(Activation.YES) || ra.getCurative().equals(Activation.YES))
                .collect(Collectors.toList());
    }

    private RemedialActionElement buildRemedialActionElements(RedispatchingRemedialActionXlsx ra) {
        //TODO make the verification of GSK element later
        String id = ra.getUctNodeOrGsk() + "_generator";
        return RedispatchRemedialActionElement.builder()
                .id(id)
                .startupCost(ra.getStartupCosts())
                .minimumPower(ra.getMinimumPower())
                .maximumPower(ra.getMaximumPower())
                .marginalCost(ra.getMarginalCosts())
                .build();
    }

    private RemedialAction buildRemedialAction(RedispatchingRemedialActionXlsx ra, List<UsageRule> urs, List<RemedialActionElement> rae) {
        return RemedialAction.builder()
                .name(ra.getGeneratorName())
                .id(ra.getRaRdId())
                .usageRules(urs)
                .remedialActionElements(rae)
                .build();

    }

    private UsageRule preventiveUsage(String id) {
        return UsageRule.builder()
                .id(id)
                .instants(UsageRule.Instant.N)
                .usage(UsageRule.Usage.FREE_TO_USE)
                .build();
    }

    private UsageRule curativeUsage(String id) {
        return UsageRule.builder()
                .id(id)
                .instants(UsageRule.Instant.CURATIVE)
                .usage(UsageRule.Usage.FREE_TO_USE)
                .build();
    }
}
