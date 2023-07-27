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

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileFlowCnecCreator {
    private final Crac crac;
    private final Network network;
    private final PropertyBags assessedElementsPropertyBags;
    private final PropertyBags assessedElementsWithContingenciesPropertyBags;
    private final PropertyBags currentLimitsPropertyBags;
    private Set<CsaProfileFlowCnecCreationContext> csaProfileFlowCnecCreationContexts;
    private CsaProfileCracCreationContext cracCreationContext;

    public CsaProfileFlowCnecCreator(Crac crac, Network network, PropertyBags assessedElementsPropertyBags, PropertyBags assessedElementsWithContingenciesPropertyBags, PropertyBags currentLimitsPropertyBags, CsaProfileCracCreationContext cracCreationContext) {
        this.crac = crac;
        this.network = network;
        this.assessedElementsPropertyBags = assessedElementsPropertyBags;
        this.assessedElementsWithContingenciesPropertyBags = assessedElementsWithContingenciesPropertyBags;
        this.currentLimitsPropertyBags = currentLimitsPropertyBags;
        this.cracCreationContext = cracCreationContext;
        this.createAndAddCnecs();
    }

    private void createAndAddCnecs() {
        this.csaProfileFlowCnecCreationContexts = new HashSet<>();

        for (PropertyBag assessedElementPropertyBag : assessedElementsPropertyBags) {
            this.addFlowCnec(assessedElementPropertyBag);
        }
        this.cracCreationContext.setFlowCnecCreationContexts(this.csaProfileFlowCnecCreationContexts);
    }

    private void addFlowCnec(PropertyBag assessedElementPropertyBag) {
        String assessedElementId = assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT);
        boolean isDataCheckOk = this.dataCheck(assessedElementId, assessedElementPropertyBag);

        if (!isDataCheckOk) {
            return;
        }

        PropertyBags assessedElementsWithContingencies = getAssessedElementsWithContingencies(assessedElementId, assessedElementPropertyBag);
        if (assessedElementsWithContingencies.isEmpty()) {
            return;
        }

        PropertyBags currentLimits = getCurrentLimits(assessedElementId, assessedElementPropertyBag);
        if (currentLimits.isEmpty()) {
            return;
        }

        String isCombinableWithContingencyStr = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IS_COMBINABLE_WITH_CONTINGENCY);
        Boolean isCombinableWithContingency = isCombinableWithContingencyStr != null && Boolean.parseBoolean(isCombinableWithContingencyStr);
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

        for (PropertyBag currentLimit : currentLimits) {
            if (!this.addCurrentLimit(assessedElementId, flowCnecAdder, currentLimit, isCombinableWithContingency)) {
                return;
            }
        }

        for (PropertyBag assessedElementWithContingencies : assessedElementsWithContingencies) {
            combinableContingencies = this.checkLinkAssessedElementContingency(assessedElementId, assessedElementWithContingencies, combinableContingencies, isCombinableWithContingency);
            if (combinableContingencies == null) {
                return;
            }
        }

        String inBaseCaseStr = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IN_BASE_CASE);
        Boolean inBaseCase = Boolean.parseBoolean(inBaseCaseStr);

        for (Contingency contingency : combinableContingencies) {
            String flowCnecName = assessedElementName + " - " + contingency.getName() + " - curative";
            flowCnecAdder.withContingency(contingency.getId())
                    .withId(assessedElementId + "-" + contingency.getId())
                    .withName(flowCnecName)
                    .add();
            csaProfileFlowCnecCreationContexts.add(CsaProfileFlowCnecCreationContext.imported(assessedElementId, assessedElementId, flowCnecName, "", false));
        }
        if (inBaseCase) {
            String flowCnecName = assessedElementName + " - preventive";
            flowCnecAdder.withContingency(null)
                    .withId(assessedElementId)
                    .withName(flowCnecName)
                    .withInstant(Instant.PREVENTIVE)
                    .add();
            csaProfileFlowCnecCreationContexts.add(CsaProfileFlowCnecCreationContext.imported(assessedElementId, assessedElementId, flowCnecName, "", false));
        }
    }

    private boolean dataCheck(String assessedElementId, PropertyBag assessedElementPropertyBag) {
        String keyword = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_HEADER_KEYWORD);

        if (!CsaProfileConstants.ASSESSED_ELEMENT_FILE_KEYWORD.equals(keyword)) {
            csaProfileFlowCnecCreationContexts.add(CsaProfileFlowCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "Model.keyword must be AE, but it is " + keyword));
            return false;
        }

        String startTime = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_HEADER_START_DATE);
        String endTime = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_HEADER_END_DATE);

        if (!CsaProfileCracUtils.isValidInterval(cracCreationContext.getTimeStamp(), startTime, endTime)) {
            csaProfileFlowCnecCreationContexts.add(CsaProfileFlowCnecCreationContext.notImported(assessedElementId, ImportStatus.NOT_FOR_REQUESTED_TIMESTAMP, "Required timestamp does not fall between Model.startDate and Model.endDate"));
            return false;
        }

        String isCritical = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IS_CRITICAL);

        if (isCritical != null && !Boolean.parseBoolean(isCritical)) {
            csaProfileFlowCnecCreationContexts.add(CsaProfileFlowCnecCreationContext.notImported(assessedElementId, ImportStatus.NOT_FOR_RAO, "AssessedElement.isCritical is false"));
            return false;
        }

        String normalEnabled = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_NORMAL_ENABLED);

        if (normalEnabled != null && !Boolean.parseBoolean(normalEnabled)) {
            csaProfileFlowCnecCreationContexts.add(CsaProfileFlowCnecCreationContext.notImported(assessedElementId, ImportStatus.NOT_FOR_RAO, "AssessedElement.normalEnabled is false"));
            return false;
        }
        return true;
    }

    private PropertyBags getAssessedElementsWithContingencies(String assessedElementId, PropertyBag assessedElementPropertyBag) {
        PropertyBags assessedElementsWithContingencies = CsaProfileCracUtils.getLinkedPropertyBags(assessedElementsWithContingenciesPropertyBags, assessedElementPropertyBag, CsaProfileConstants.REQUEST_ASSESSED_ELEMENT, CsaProfileConstants.REQUEST_ASSESSED_ELEMENT);

        if (assessedElementsWithContingencies.isEmpty()) {
            csaProfileFlowCnecCreationContexts.add(CsaProfileFlowCnecCreationContext.notImported(assessedElementId, ImportStatus.INCOMPLETE_DATA, "no link between the assessed element and a contingency"));
        }
        return assessedElementsWithContingencies;
    }

    private PropertyBags getCurrentLimits(String assessedElementId, PropertyBag assessedElementPropertyBag) {
        PropertyBags currentLimits = CsaProfileCracUtils.getLinkedPropertyBags(currentLimitsPropertyBags, assessedElementPropertyBag, CsaProfileConstants.REQUEST_CURRENT_LIMIT, CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_OPERATIONAL_LIMIT);

        if (currentLimits.isEmpty()) {
            csaProfileFlowCnecCreationContexts.add(CsaProfileFlowCnecCreationContext.notImported(assessedElementId, ImportStatus.INCOMPLETE_DATA, "no current limit linked with the assessed element"));
        }
        return currentLimits;
    }

    private Set<Contingency> checkLinkAssessedElementContingency(String assessedElementId, PropertyBag assessedElementWithContingencies, Set<Contingency> combinableContingenciesSet, Boolean isCombinableWithContingency) {
        Set<Contingency> combinableContingencies = combinableContingenciesSet.stream().collect(Collectors.toSet());
        String normalEnabledWithContingencies = assessedElementWithContingencies.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY_NORMAL_ENABLED);

        if (normalEnabledWithContingencies != null && !Boolean.parseBoolean(normalEnabledWithContingencies)) {
            csaProfileFlowCnecCreationContexts.add(CsaProfileFlowCnecCreationContext.notImported(assessedElementId, ImportStatus.NOT_FOR_RAO, "AssessedElementWithContingency.normalEnabled is false"));
            return null;
        }

        String contingencyId = assessedElementWithContingencies.getId(CsaProfileConstants.REQUEST_CONTINGENCY);
        String combinationConstraintKind = assessedElementWithContingencies.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY_COMBINATION_CONSTRAINT_KIND);
        if (CsaProfileConstants.ASSESSED_ELEMENT_WITH_CONTINGENCIES_LINK_CONSIDERED.equals(combinationConstraintKind)) {
            csaProfileFlowCnecCreationContexts.add(CsaProfileFlowCnecCreationContext.notImported(assessedElementId, ImportStatus.NOT_FOR_RAO, "AssessedElementWithContingency.combinationConstraintKind is considered"));
            return null;
        }
        if (CsaProfileConstants.ASSESSED_ELEMENT_WITH_CONTINGENCIES_LINK_INCLUDED.equals(combinationConstraintKind) && !isCombinableWithContingency) {
            Contingency contingencyToLink = crac.getContingencies().stream().filter(contingency -> contingency.getId().equals(contingencyId)).findFirst().orElse(null);
            if (contingencyToLink == null) {
                csaProfileFlowCnecCreationContexts.add(CsaProfileFlowCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "the contingency " + contingencyId + " linked to the assessed element doesn't exist in the CRAC"));
                return null;
            }
            combinableContingencies.add(contingencyToLink);
        } else if (CsaProfileConstants.ASSESSED_ELEMENT_WITH_CONTINGENCIES_LINK_EXCLUDED.equals(combinationConstraintKind) && isCombinableWithContingency) {
            Set<Contingency> combinableContingenciesAfterFilter =
                    combinableContingencies.stream().filter(contingency -> !contingency.getId().equals(contingencyId)).collect(Collectors.toSet());
            if (combinableContingenciesAfterFilter.size() == combinableContingencies.size()) {
                csaProfileFlowCnecCreationContexts.add(CsaProfileFlowCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "the contingency " + contingencyId + " excluded from the contingencies linked to the assessed element doesn't exist in the CRAC"));
                return null;
            }
            combinableContingencies = combinableContingenciesAfterFilter;
        } else {
            csaProfileFlowCnecCreationContexts.add(CsaProfileFlowCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElementWithContingency.combinationConstraintKind and AssessedElement.isCombinableWithContingency have inconsistent values"));
            return null;
        }
        return combinableContingencies;
    }

    private boolean addCurrentLimit(String assessedElementId, FlowCnecAdder flowCnecAdder, PropertyBag currentLimit, Boolean isCombinableWithContingency) {
        String currentLimitId = currentLimit.getId(CsaProfileConstants.REQUEST_CURRENT_LIMIT_OPERATIONAL_LIMIT_TERMINAL);
        Identifiable<?> networkElement = this.getNetworkElementInNetwork(currentLimitId);
        if (networkElement == null) {
            csaProfileFlowCnecCreationContexts.add(CsaProfileFlowCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "current limit equipment is missing in network : " + currentLimitId));
            return false;
        }

        if (!(networkElement instanceof Branch)) {
            csaProfileFlowCnecCreationContexts.add(CsaProfileFlowCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "network element " + networkElement.getId() + " is not a branch"));
            return false;
        }

        boolean isNominalVoltageOk = setNominalVoltage(assessedElementId, flowCnecAdder, (Branch<?>) networkElement);

        if (!isNominalVoltageOk) {
            return false;
        }

        boolean isCurrentsLimitOk = setCurrentsLimit(assessedElementId, flowCnecAdder, (Branch<?>) networkElement);

        if (!isCurrentsLimitOk) {
            return false;
        }

        String networkElementId = networkElement.getId();
        flowCnecAdder.withNetworkElement(networkElementId);

        boolean isInstantOk = this.addInstant(assessedElementId, flowCnecAdder, currentLimit, isCombinableWithContingency);
        if (!isInstantOk) {
            return false;
        }

        return this.addThreshold(assessedElementId, flowCnecAdder, currentLimit, networkElement);

    }

    private boolean setNominalVoltage(String assessedElementId, FlowCnecAdder flowCnecAdder, Branch<?> branch) {
        double voltageLevelLeft = branch.getTerminal1().getVoltageLevel().getNominalV();
        double voltageLevelRight = branch.getTerminal2().getVoltageLevel().getNominalV();
        if (voltageLevelLeft > 1e-6 && voltageLevelRight > 1e-6) {
            flowCnecAdder.withNominalVoltage(voltageLevelLeft, Side.LEFT);
            flowCnecAdder.withNominalVoltage(voltageLevelRight, Side.RIGHT);
            return true;
        } else {
            csaProfileFlowCnecCreationContexts.add(CsaProfileFlowCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "Voltage level for branch " + branch.getId() + " is 0 in network"));
            return false;
        }
    }

    private boolean setCurrentsLimit(String assessedElementId, FlowCnecAdder flowCnecAdder, Branch<?> branch) {
        Double currentLimitLeft = getCurrentLimit(branch, Branch.Side.ONE);
        Double currentLimitRight = getCurrentLimit(branch, Branch.Side.TWO);
        if (Objects.nonNull(currentLimitLeft) && Objects.nonNull(currentLimitRight)) {
            flowCnecAdder.withIMax(currentLimitLeft, Side.LEFT);
            flowCnecAdder.withIMax(currentLimitRight, Side.RIGHT);
            return true;
        } else {
            csaProfileFlowCnecCreationContexts.add(CsaProfileFlowCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "Unable to get branch current limits from network for branch " + branch.getId()));
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

    private boolean addInstant(String assessedElementId, FlowCnecAdder flowCnecAdder, PropertyBag currentLimit, Boolean isCombinableWithContingency) {
        String kind = currentLimit.get(CsaProfileConstants.REQUEST_CURRENT_LIMIT_OPERATIONAL_LIMIT_KIND);
        Instant instant = null;

        if (CsaProfileConstants.OPERATIONAL_LIMIT_TYPE_TATL.equals(kind)) {
            String acceptableDurationStr = currentLimit.get(CsaProfileConstants.REQUEST_CURRENT_LIMIT_OPERATIONAL_LIMIT_ACCEPTABLE_DURATION);
            Double acceptableDuration = Double.valueOf(acceptableDurationStr);
            if (acceptableDuration == null || acceptableDuration < 0) {
                csaProfileFlowCnecCreationContexts.add(CsaProfileFlowCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "OperationalLimitType.acceptableDuration is incorrect : " + acceptableDurationStr));
                return false;
            } else if (acceptableDuration == 0) {
                instant = isCombinableWithContingency ? Instant.PREVENTIVE : Instant.CURATIVE;
            } else if (acceptableDuration <= CracCreationParameters.DurationThresholdsLimits.DURATION_THRESHOLDS_LIMITS_MAX_OUTAGE_INSTANT.getLimit()) {
                instant = Instant.OUTAGE;
            } else if (acceptableDuration <= CracCreationParameters.DurationThresholdsLimits.DURATION_THRESHOLDS_LIMITS_MAX_AUTO_INSTANT.getLimit()) {
                instant = Instant.AUTO;
            } else {
                instant = Instant.CURATIVE;
            }
            flowCnecAdder.withInstant(instant);
        } else if (CsaProfileConstants.OPERATIONAL_LIMIT_TYPE_PATL.equals(kind)) {
            instant = isCombinableWithContingency ? Instant.PREVENTIVE : Instant.CURATIVE;
            flowCnecAdder.withInstant(instant);
        } else {
            csaProfileFlowCnecCreationContexts.add(CsaProfileFlowCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "OperationalLimitType.kind is incorrect : " + kind));
            return false;
        }
        return true;
    }

    private boolean addThreshold(String assessedElementId, FlowCnecAdder flowCnecAdder, PropertyBag currentLimit, Identifiable<?> networkElement) {
        Side side = this.getSideFromNetworkElement(networkElement);
        if (side == null) {
            csaProfileFlowCnecCreationContexts.add(CsaProfileFlowCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "could not find side of threshold with network element : " + networkElement.getId()));
            return false;
        }
        String normalValueStr = currentLimit.get(CsaProfileConstants.REQUEST_CURRENT_LIMIT_NORMAL_VALUE);
        Double normalValue = Double.valueOf(normalValueStr);
        String direction = currentLimit.get(CsaProfileConstants.REQUEST_CURRENT_LIMIT_OPERATIONAL_LIMIT_DIRECTION);
        if (CsaProfileConstants.OPERATIONAL_LIMIT_TYPE_DIRECTION_ABSOLUTE.equals(direction)) {
            flowCnecAdder.newThreshold().withSide(side)
                    .withUnit(Unit.AMPERE)
                    .withMax(normalValue)
                    .withMin(-normalValue).add();
        } else if (CsaProfileConstants.OPERATIONAL_LIMIT_TYPE_DIRECTION_HIGH.equals(direction)) {
            flowCnecAdder.newThreshold().withSide(side)
                    .withUnit(Unit.AMPERE)
                    .withMax(normalValue).add();
        } else if (CsaProfileConstants.OPERATIONAL_LIMIT_TYPE_DIRECTION_LOW.equals(direction)) {
            csaProfileFlowCnecCreationContexts.add(CsaProfileFlowCnecCreationContext.notImported(assessedElementId, ImportStatus.NOT_FOR_RAO, "OperationalLimitType.direction is low"));
            return false;
        }
        return true;
    }

    private Side getSideFromNetworkElement(Identifiable<?> networkElement) {
        for (String key : CsaProfileConstants.CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_LEFT) {
            Optional<String> oAlias = networkElement.getAliasFromType(key);
            if (oAlias.isPresent()) {
                return Side.LEFT;
            }
        }
        for (String key : CsaProfileConstants.CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_RIGHT) {
            Optional<String> oAlias = networkElement.getAliasFromType(key);
            if (oAlias.isPresent()) {
                return Side.RIGHT;
            }
        }
        return null;
    }
}
