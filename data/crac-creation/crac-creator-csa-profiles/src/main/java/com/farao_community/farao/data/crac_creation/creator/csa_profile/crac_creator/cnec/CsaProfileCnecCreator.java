/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.cnec;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnecAdder;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracUtils;
import com.farao_community.farao.data.crac_creation.util.cgmes.CgmesBranchHelper;
import com.powsybl.iidm.network.*;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileCnecCreator {
    private final Crac crac;
    private final Network network;
    private final PropertyBags assessedElementsPropertyBags;
    private final Map<String, Set<PropertyBag>> assessedElementsWithContingenciesPropertyBags;
    private final Map<String, Set<PropertyBag>> currentLimitsPropertyBags;
    private Set<CsaProfileCnecCreationContext> csaProfileCnecCreationContexts;
    private CsaProfileCracCreationContext cracCreationContext;
    private Instant currentFlowCnecInstant;

    public CsaProfileCnecCreator(Crac crac, Network network, PropertyBags assessedElementsPropertyBags, PropertyBags assessedElementsWithContingenciesPropertyBags, PropertyBags currentLimitsPropertyBags, CsaProfileCracCreationContext cracCreationContext) {
        this.crac = crac;
        this.network = network;
        this.assessedElementsPropertyBags = assessedElementsPropertyBags;
        this.assessedElementsWithContingenciesPropertyBags = CsaProfileCracUtils.getMappedPropertyBagsSet(assessedElementsWithContingenciesPropertyBags, CsaProfileConstants.REQUEST_ASSESSED_ELEMENT);
        this.currentLimitsPropertyBags = CsaProfileCracUtils.getMappedPropertyBagsSet(currentLimitsPropertyBags, CsaProfileConstants.REQUEST_CURRENT_LIMIT);
        this.cracCreationContext = cracCreationContext;
        this.createAndAddCnecs();
    }

    private void createAndAddCnecs() {
        this.csaProfileCnecCreationContexts = new HashSet<>();

        for (PropertyBag assessedElementPropertyBag : assessedElementsPropertyBags) {
            this.addFlowCnec(assessedElementPropertyBag);
        }
        this.cracCreationContext.setFlowCnecCreationContexts(this.csaProfileCnecCreationContexts);
    }

    private void addFlowCnec(PropertyBag assessedElementPropertyBag) {
        String assessedElementId = assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT);
        boolean isDataCheckOk = this.dataCheck(assessedElementId, assessedElementPropertyBag);

        if (!isDataCheckOk) {
            return;
        }

        String inBaseCaseStr = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IN_BASE_CASE);
        boolean inBaseCase = Boolean.parseBoolean(inBaseCaseStr);

        Set<PropertyBag> assessedElementsWithContingencies = getAssessedElementsWithContingencies(assessedElementId, assessedElementPropertyBag, inBaseCase);
        if (!inBaseCase && assessedElementsWithContingencies == null) {
            return;
        }

        PropertyBag currentLimit = getCurrentLimit(assessedElementId, assessedElementPropertyBag);
        if (currentLimit == null) {
            return;
        }

        String isCombinableWithContingencyStr = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IS_COMBINABLE_WITH_CONTINGENCY);
        boolean isCombinableWithContingency = isCombinableWithContingencyStr != null && Boolean.parseBoolean(isCombinableWithContingencyStr);
        Set<Contingency> combinableContingencies;
        if (isCombinableWithContingency) {
            combinableContingencies = cracCreationContext.getCrac().getContingencies();
        } else {
            combinableContingencies = new HashSet<>();
        }

        String nativeAssessedElementName = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_NAME);
        String assessedSystemOperator = assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_OPERATOR);
        String assessedElementName = CsaProfileCracUtils.getUniqueName(assessedSystemOperator, nativeAssessedElementName);

        FlowCnecAdder flowCnecAdder = crac.newFlowCnec();
        flowCnecAdder
            .withMonitored(false)
            .withOptimized(true)
            .withReliabilityMargin(0);

        if (!this.addCurrentLimit(assessedElementId, flowCnecAdder, currentLimit, isCombinableWithContingency)) {
            return;
        }

        if (assessedElementsWithContingencies != null) {
            for (PropertyBag assessedElementWithContingencies : assessedElementsWithContingencies) {
                combinableContingencies = this.checkLinkAssessedElementContingency(assessedElementId, assessedElementWithContingencies, combinableContingencies, isCombinableWithContingency);
                if (combinableContingencies == null) {
                    return;
                }
            }
        }

        for (Contingency contingency : combinableContingencies) {
            String flowCnecName = assessedElementName + " - " + contingency.getName() + " - " + currentFlowCnecInstant.toString();
            flowCnecAdder.withContingency(contingency.getId())
                .withId(assessedElementId + "-" + contingency.getId())
                .withName(flowCnecName)
                .add();
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.imported(assessedElementId, assessedElementId, flowCnecName, "", false));
        }
        if (inBaseCase) {
            String flowCnecName = assessedElementName + " - preventive";
            flowCnecAdder.withContingency(null)
                .withId(assessedElementId)
                .withName(flowCnecName)
                .withInstant(crac.getInstant(Instant.Kind.PREVENTIVE))
                .add();
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.imported(assessedElementId, assessedElementId, flowCnecName, "", false));
        }
    }

    private boolean dataCheck(String assessedElementId, PropertyBag assessedElementPropertyBag) {
        String keyword = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_HEADER_KEYWORD);

        if (!CsaProfileConstants.ASSESSED_ELEMENT_FILE_KEYWORD.equals(keyword)) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "Model.keyword must be AE, but it is " + keyword));
            return false;
        }

        String startTime = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_HEADER_START_DATE);
        String endTime = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_HEADER_END_DATE);

        if (!CsaProfileCracUtils.isValidInterval(cracCreationContext.getTimeStamp(), startTime, endTime)) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.NOT_FOR_REQUESTED_TIMESTAMP, "Required timestamp does not fall between Model.startDate and Model.endDate"));
            return false;
        }

        String isCritical = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IS_CRITICAL);

        if (isCritical != null && !Boolean.parseBoolean(isCritical)) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.NOT_FOR_RAO, "AssessedElement.isCritical is false"));
            return false;
        }

        String normalEnabled = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_NORMAL_ENABLED);

        if (normalEnabled != null && !Boolean.parseBoolean(normalEnabled)) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.NOT_FOR_RAO, "AssessedElement.normalEnabled is false"));
            return false;
        }
        return true;
    }

    private Set<PropertyBag> getAssessedElementsWithContingencies(String assessedElementId, PropertyBag assessedElementPropertyBag, boolean inBaseCase) {
        Set<PropertyBag> assessedElementsWithContingencies = this.assessedElementsWithContingenciesPropertyBags.get(assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT));

        if (!inBaseCase && assessedElementsWithContingencies == null) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCOMPLETE_DATA, "no link between the assessed element and a contingency"));
        }
        return assessedElementsWithContingencies;
    }

    private PropertyBag getCurrentLimit(String assessedElementId, PropertyBag assessedElementPropertyBag) {
        Set<PropertyBag> currentLimits = this.currentLimitsPropertyBags.get(assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_OPERATIONAL_LIMIT));

        if (currentLimits == null) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCOMPLETE_DATA, "no current limit linked with the assessed element"));
            return null;
        } else if (currentLimits.size() > 1) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "more than one current limit linked with the assessed element"));
            return null;
        }
        return currentLimits.iterator().next();
    }

    private Set<Contingency> checkLinkAssessedElementContingency(String assessedElementId, PropertyBag assessedElementWithContingencies, Set<Contingency> combinableContingenciesSet, boolean isCombinableWithContingency) {
        Set<Contingency> combinableContingencies = combinableContingenciesSet.stream().collect(Collectors.toSet());
        String normalEnabledWithContingencies = assessedElementWithContingencies.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY_NORMAL_ENABLED);

        if (normalEnabledWithContingencies != null && !Boolean.parseBoolean(normalEnabledWithContingencies)) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.NOT_FOR_RAO, "AssessedElementWithContingency.normalEnabled is false"));
            return null;
        }

        String contingencyId = assessedElementWithContingencies.getId(CsaProfileConstants.REQUEST_CONTINGENCY);
        String combinationConstraintKind = assessedElementWithContingencies.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY_COMBINATION_CONSTRAINT_KIND);
        if (CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString().equals(combinationConstraintKind)) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElementWithContingency.combinationConstraintKind is considered"));
            return null;
        }
        if (CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString().equals(combinationConstraintKind) && !isCombinableWithContingency) {
            Contingency contingencyToLink = crac.getContingency(contingencyId);
            if (contingencyToLink == null) {
                csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "the contingency "
                    + contingencyId + " linked to the assessed element doesn't exist in the CRAC"));
                return null;
            }
            combinableContingencies.add(contingencyToLink);
        } else if (CsaProfileConstants.ElementCombinationConstraintKind.EXCLUDED.toString().equals(combinationConstraintKind) && isCombinableWithContingency) {
            Contingency contingencyToRemove = crac.getContingency(contingencyId);
            if (contingencyToRemove == null) {
                csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "the contingency "
                    + contingencyId + " excluded from the contingencies linked to the assessed element doesn't exist in the CRAC"));
                return null;
            }
            combinableContingencies.remove(contingencyToRemove);
        } else {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElementWithContingency.combinationConstraintKind = "
                + combinationConstraintKind + " and AssessedElement.isCombinableWithContingency = " + isCombinableWithContingency + " have inconsistent values"));
            return null;
        }
        return combinableContingencies;
    }

    private boolean addCurrentLimit(String assessedElementId, FlowCnecAdder flowCnecAdder, PropertyBag currentLimit, boolean inBaseCase) {
        String terminalId = currentLimit.getId(CsaProfileConstants.REQUEST_CURRENT_LIMIT_OPERATIONAL_LIMIT_TERMINAL);
        Identifiable<?> networkElement = this.getNetworkElementInNetwork(terminalId);
        if (networkElement == null) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "current limit equipment is missing in network : " + terminalId));
            return false;
        }

        if (!(networkElement instanceof Branch)) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "network element " + networkElement.getId() + " is not a branch"));
            return false;
        }

        boolean isNominalVoltageOk = setNominalVoltage(assessedElementId, flowCnecAdder, (Branch<?>) networkElement);

        if (!isNominalVoltageOk) {
            return false;
        }

        boolean isCurrentsLimitOk = setCurrentLimits(assessedElementId, flowCnecAdder, (Branch<?>) networkElement);

        if (!isCurrentsLimitOk) {
            return false;
        }

        String networkElementId = networkElement.getId();
        flowCnecAdder.withNetworkElement(networkElementId);

        boolean isInstantOk = this.addInstant(assessedElementId, flowCnecAdder, currentLimit, inBaseCase);
        if (!isInstantOk) {
            return false;
        }

        return this.addThreshold(assessedElementId, flowCnecAdder, currentLimit, networkElement, getSideFromNetworkElement(networkElement, terminalId));

    }

    private boolean setNominalVoltage(String assessedElementId, FlowCnecAdder flowCnecAdder, Branch<?> branch) {
        double voltageLevelLeft = branch.getTerminal1().getVoltageLevel().getNominalV();
        double voltageLevelRight = branch.getTerminal2().getVoltageLevel().getNominalV();
        if (voltageLevelLeft > 1e-6 && voltageLevelRight > 1e-6) {
            flowCnecAdder.withNominalVoltage(voltageLevelLeft, Side.LEFT);
            flowCnecAdder.withNominalVoltage(voltageLevelRight, Side.RIGHT);
            return true;
        } else {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "Voltage level for branch " + branch.getId() + " is 0 in network"));
            return false;
        }
    }

    private boolean setCurrentLimits(String assessedElementId, FlowCnecAdder flowCnecAdder, Branch<?> branch) {
        Double currentLimitLeft = getCurrentLimit(branch, Branch.Side.ONE);
        Double currentLimitRight = getCurrentLimit(branch, Branch.Side.TWO);
        if (Objects.nonNull(currentLimitLeft) && Objects.nonNull(currentLimitRight)) {
            flowCnecAdder.withIMax(currentLimitLeft, Side.LEFT);
            flowCnecAdder.withIMax(currentLimitRight, Side.RIGHT);
            return true;
        } else {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "Unable to get branch current limits from network for branch " + branch.getId()));
            return false;
        }
    }

    private Double getCurrentLimit(Branch<?> branch, Branch.Side side) {

        if (branch.getCurrentLimits(side).isPresent()) {
            return branch.getCurrentLimits(side).orElseThrow().getPermanentLimit();
        }

        if (side == Branch.Side.ONE && branch.getCurrentLimits(Branch.Side.TWO).isPresent()) {
            return branch.getCurrentLimits(Branch.Side.TWO).orElseThrow().getPermanentLimit() * branch.getTerminal1().getVoltageLevel().getNominalV() / branch.getTerminal2().getVoltageLevel().getNominalV();
        }

        if (side == Branch.Side.TWO && branch.getCurrentLimits(Branch.Side.ONE).isPresent()) {
            return branch.getCurrentLimits(Branch.Side.ONE).orElseThrow().getPermanentLimit() * branch.getTerminal2().getVoltageLevel().getNominalV() / branch.getTerminal1().getVoltageLevel().getNominalV();
        }

        return null;
    }

    private Identifiable<?> getNetworkElementInNetwork(String networkElementId) {
        Identifiable<?> networkElement = network.getIdentifiable(networkElementId);
        if (networkElement == null) {
            CgmesBranchHelper cgmesBranchHelper = new CgmesBranchHelper(networkElementId, network);
            if (cgmesBranchHelper.isValid()) {
                networkElement = cgmesBranchHelper.getBranch();
            }
        }

        if (networkElement instanceof DanglingLine) {
            Optional<TieLine> optionalTieLine = ((DanglingLine) networkElement).getTieLine();
            if (optionalTieLine.isPresent()) {
                networkElement = optionalTieLine.get();
            }
        }
        return networkElement;
    }

    private boolean addInstant(String assessedElementId, FlowCnecAdder flowCnecAdder, PropertyBag currentLimit, boolean inBaseCase) {
        this.currentFlowCnecInstant = null;
        String kind = currentLimit.get(CsaProfileConstants.REQUEST_CURRENT_LIMIT_OPERATIONAL_LIMIT_KIND);
        Instant.Kind instantKind = null;
        // TODO : handle multiple curative instants here

        if (CsaProfileConstants.LimitKind.TATL.toString().equals(kind)) {
            String acceptableDurationStr = currentLimit.get(CsaProfileConstants.REQUEST_CURRENT_LIMIT_OPERATIONAL_LIMIT_ACCEPTABLE_DURATION);
            Double acceptableDuration = Double.valueOf(acceptableDurationStr);
            if (acceptableDuration == null || acceptableDuration < 0) {
                csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "OperationalLimitType.acceptableDuration is incorrect : " + acceptableDurationStr));
                return false;
            } else if (acceptableDuration == 0) {
                instantKind = inBaseCase ? Instant.Kind.PREVENTIVE : Instant.Kind.CURATIVE;
            } else if (acceptableDuration <= CracCreationParameters.DurationThresholdsLimits.DURATION_THRESHOLDS_LIMITS_MAX_OUTAGE_INSTANT.getLimit()) {
                instantKind = Instant.Kind.OUTAGE;
            } else if (acceptableDuration <= CracCreationParameters.DurationThresholdsLimits.DURATION_THRESHOLDS_LIMITS_MAX_AUTO_INSTANT.getLimit()) {
                instantKind = Instant.Kind.AUTO;
            } else {
                instantKind = Instant.Kind.CURATIVE;
            }
            flowCnecAdder.withInstant(crac.getInstant(instantKind));
        } else if (CsaProfileConstants.LimitKind.PATL.toString().equals(kind)) {
            instantKind = inBaseCase ? Instant.Kind.PREVENTIVE : Instant.Kind.CURATIVE;
            flowCnecAdder.withInstant(crac.getInstant(instantKind));
        } else {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "OperationalLimitType.kind is incorrect : " + kind));
            return false;
        }
        this.currentFlowCnecInstant = crac.getInstant(instantKind);
        return true;
    }

    private boolean addThreshold(String assessedElementId, FlowCnecAdder flowCnecAdder, PropertyBag currentLimit, Identifiable<?> networkElement, Side side) {
        if (side == null) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "could not find side of threshold with network element : " + networkElement.getId()));
            return false;
        }
        String normalValueStr = currentLimit.get(CsaProfileConstants.REQUEST_CURRENT_LIMIT_NORMAL_VALUE);
        Double normalValue = Double.valueOf(normalValueStr);
        String direction = currentLimit.get(CsaProfileConstants.REQUEST_CURRENT_LIMIT_OPERATIONAL_LIMIT_DIRECTION);
        if (CsaProfileConstants.LimitDirectionKind.ABSOLUTE.toString().equals(direction)) {
            flowCnecAdder.newThreshold().withSide(side)
                .withUnit(Unit.AMPERE)
                .withMax(normalValue)
                .withMin(-normalValue).add();
        } else if (CsaProfileConstants.LimitDirectionKind.HIGH.toString().equals(direction)) {
            flowCnecAdder.newThreshold().withSide(side)
                .withUnit(Unit.AMPERE)
                .withMax(normalValue).add();
        } else if (CsaProfileConstants.LimitDirectionKind.LOW.toString().equals(direction)) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.NOT_FOR_RAO, "OperationalLimitType.direction is low"));
            return false;
        }
        return true;
    }

    private Side getSideFromNetworkElement(Identifiable<?> networkElement, String terminalId) {
        for (String key : CsaProfileConstants.CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_LEFT) {
            Optional<String> oAlias = networkElement.getAliasFromType(key);
            if (oAlias.isPresent() && oAlias.get().equals(terminalId)) {
                return Side.LEFT;
            }
        }
        for (String key : CsaProfileConstants.CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_RIGHT) {
            Optional<String> oAlias = networkElement.getAliasFromType(key);
            if (oAlias.isPresent() && oAlias.get().equals(terminalId)) {
                return Side.RIGHT;
            }
        }
        return null;
    }
}
