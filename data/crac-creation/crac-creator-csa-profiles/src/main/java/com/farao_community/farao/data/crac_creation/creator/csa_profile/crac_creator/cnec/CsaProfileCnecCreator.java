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
import com.farao_community.farao.data.crac_api.cnec.*;
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
    private final Map<String, Set<PropertyBag>> voltageLimitsPropertyBags;
    private final Map<String, Set<PropertyBag>> angleLimitsPropertyBags;
    private Set<CsaProfileCnecCreationContext> csaProfileCnecCreationContexts;
    private CsaProfileCracCreationContext cracCreationContext;
    private Instant cnecInstant;
    private PropertyBag cnecLimit;

    public CsaProfileCnecCreator(Crac crac, Network network, PropertyBags assessedElementsPropertyBags, PropertyBags assessedElementsWithContingenciesPropertyBags, PropertyBags currentLimitsPropertyBags, PropertyBags voltageLimitsPropertyBags, PropertyBags angleLimitsPropertyBags, CsaProfileCracCreationContext cracCreationContext) {
        this.crac = crac;
        this.network = network;
        this.assessedElementsPropertyBags = assessedElementsPropertyBags;
        this.assessedElementsWithContingenciesPropertyBags = CsaProfileCracUtils.getMappedPropertyBagsSet(assessedElementsWithContingenciesPropertyBags, CsaProfileConstants.REQUEST_ASSESSED_ELEMENT);
        this.currentLimitsPropertyBags = CsaProfileCracUtils.getMappedPropertyBagsSet(currentLimitsPropertyBags, CsaProfileConstants.REQUEST_CURRENT_LIMIT);
        this.voltageLimitsPropertyBags = CsaProfileCracUtils.getMappedPropertyBagsSet(voltageLimitsPropertyBags, CsaProfileConstants.REQUEST_VOLTAGE_LIMIT);
        this.angleLimitsPropertyBags = CsaProfileCracUtils.getMappedPropertyBagsSet(voltageLimitsPropertyBags, CsaProfileConstants.REQUEST_ANGLE_LIMIT);
        this.cracCreationContext = cracCreationContext;
        this.createAndAddCnecs();
    }

    private void createAndAddCnecs() {
        this.csaProfileCnecCreationContexts = new HashSet<>();

        for (PropertyBag assessedElementPropertyBag : assessedElementsPropertyBags) {
            this.addCnec(assessedElementPropertyBag);
        }
        this.cracCreationContext.setCnecCreationContexts(this.csaProfileCnecCreationContexts);
    }

    private void addCnec(PropertyBag assessedElementPropertyBag) {
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

        CsaProfileConstants.LimitType limitType = getLimit(assessedElementId, assessedElementPropertyBag);
        if (limitType == null) {
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

        CnecAdder cnecAdder;
        if (CsaProfileConstants.LimitType.CURRENT.equals(limitType)) {
            cnecAdder = crac.newFlowCnec()
                .withMonitored(false)
                .withOptimized(true)
                .withReliabilityMargin(0);

            if (!this.addCurrentLimit(assessedElementId, (FlowCnecAdder) cnecAdder, isCombinableWithContingency)) {
                return;
            }
        } else if (CsaProfileConstants.LimitType.VOLTAGE.equals(limitType)) {
            cnecAdder = crac.newVoltageCnec()
                .withMonitored(false)
                .withOptimized(false)
                .withReliabilityMargin(0);

            if (!this.addVoltageLimit(assessedElementId, (VoltageCnecAdder) cnecAdder, isCombinableWithContingency)) {
                return;
            }
        } else if (CsaProfileConstants.LimitType.ANGLE.equals(limitType)) {

            String isFlowToRefTerminalStr = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_IS_FLOW_TO_REF_TERMINAL);
            boolean isFlowToRefTerminalIsNull = isFlowToRefTerminalStr == null;
            boolean isFlowToRefTerminal = isFlowToRefTerminalIsNull || Boolean.parseBoolean(isFlowToRefTerminalStr);

            cnecAdder = crac.newAngleCnec()
                    .withMonitored(false)
                    .withOptimized(false)
                    .withReliabilityMargin(0);

            if (!this.addAngleLimit(assessedElementId, (AngleCnecAdder) cnecAdder, isFlowToRefTerminal, isFlowToRefTerminalIsNull, isCombinableWithContingency)) {
                return;
            }
        } else {
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
            String cnecName = assessedElementName + " - " + contingency.getName() + " - " + cnecInstant.toString();
            this.addCnec(cnecAdder, limitType, contingency.getId(), assessedElementId, cnecName, cnecInstant);
        }
        if (inBaseCase) {
            String cnecName = assessedElementName + " - preventive";
            this.addCnec(cnecAdder, limitType, null, assessedElementId, cnecName, Instant.PREVENTIVE);
        }
    }

    private void addCnec(CnecAdder cnecAdder, CsaProfileConstants.LimitType limitType, String contingencyId, String assessedElementId, String cnecName, Instant instant) {
        if (CsaProfileConstants.LimitType.CURRENT.equals(limitType)) {
            ((FlowCnecAdder) cnecAdder).withContingency(contingencyId)
                .withId(cnecName)
                .withName(cnecName)
                .withInstant(instant)
                .add();
        } else if (CsaProfileConstants.LimitType.VOLTAGE.equals(limitType)) {
            ((VoltageCnecAdder) cnecAdder).withContingency(contingencyId)
                .withId(cnecName)
                .withName(cnecName)
                .withInstant(instant)
                .add();
        } else {
            ((AngleCnecAdder) cnecAdder).withContingency(contingencyId)
                .withId(cnecName)
                .withName(cnecName)
                .withInstant(instant)
                .add();
        }
        csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.imported(assessedElementId, assessedElementId, cnecName, "", false));
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

    private CsaProfileConstants.LimitType getLimit(String assessedElementId, PropertyBag assessedElementPropertyBag) {
        this.cnecLimit = null;
        Set<PropertyBag> currentLimits = this.currentLimitsPropertyBags.get(assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_OPERATIONAL_LIMIT));
        if (currentLimits == null) {
            Set<PropertyBag> voltageLimits = this.voltageLimitsPropertyBags.get(assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_OPERATIONAL_LIMIT));
            if (voltageLimits == null) {
                Set<PropertyBag> angleLimits = this.angleLimitsPropertyBags.get(assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_OPERATIONAL_LIMIT));
                if (angleLimits == null) {
                    csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCOMPLETE_DATA, "no current, voltage nor angle limit linked with the assessed element"));
                    return null;
                } else if (angleLimits.size() != 1) {
                    csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "more than one angle limit linked with the assessed element"));
                    return null;
                }
                this.cnecLimit = angleLimits.stream().findAny().orElse(null);
                return CsaProfileConstants.LimitType.ANGLE;
            } else if (voltageLimits.size() != 1) {
                csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "more than one voltage limit linked with the assessed element"));
                return null;
            }
            this.cnecLimit = voltageLimits.stream().findAny().orElse(null);
            return CsaProfileConstants.LimitType.VOLTAGE;
        } else if (currentLimits.size() != 1) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "more than one current limit linked with the assessed element"));
            return null;
        }
        this.cnecLimit = currentLimits.stream().findAny().orElse(null);
        return CsaProfileConstants.LimitType.CURRENT;
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

    private boolean addCurrentLimit(String assessedElementId, FlowCnecAdder flowCnecAdder, boolean inBaseCase) {
        String terminalId = cnecLimit.getId(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_TERMINAL);
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

        boolean isCurrentsLimitOk = setCurrentLimitsFromBranch(assessedElementId, flowCnecAdder, (Branch<?>) networkElement);

        if (!isCurrentsLimitOk) {
            return false;
        }

        String networkElementId = networkElement.getId();
        flowCnecAdder.withNetworkElement(networkElementId);

        boolean isInstantOk = this.addCurrentLimitInstant(assessedElementId, flowCnecAdder, cnecLimit, inBaseCase);
        if (!isInstantOk) {
            return false;
        }

        return this.addCurrentLimitThreshold(assessedElementId, flowCnecAdder, cnecLimit, networkElement, this.getSideFromNetworkElement(networkElement, terminalId));
    }

    private boolean addVoltageLimit(String assessedElementId, VoltageCnecAdder voltageCnecAdder, boolean inBaseCase) {
        String terminalId = cnecLimit.getId(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_TERMINAL);
        Identifiable<?> networkElement = this.getNetworkElementInNetwork(terminalId);
        if (networkElement == null) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "current limit equipment is missing in network : " + terminalId));
            return false;
        }

        if (!(networkElement instanceof BusbarSection)) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "network element " + networkElement.getId() + " is not a bus bar section"));
            return false;
        }

        String networkElementId = networkElement.getId();
        voltageCnecAdder.withNetworkElement(networkElementId);

        boolean isInstantOk = this.addVoltageLimitInstant(assessedElementId, voltageCnecAdder, cnecLimit, inBaseCase);
        if (!isInstantOk) {
            return false;
        }

        return this.addVoltageLimitThreshold(assessedElementId, voltageCnecAdder, cnecLimit);
    }

    private boolean addAngleLimit(String assessedElementId, AngleCnecAdder angleCnecAdder, boolean isFlowToRefTerminal, boolean isFlowToRefTerminalIsNull, boolean inBaseCase) {
        String terminal1Id = cnecLimit.getId(CsaProfileConstants.REQUEST_ANGLE_REFERENCE_TERMINAL);
        String terminal2Id = cnecLimit.getId(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_TERMINAL);

        String networkElement1Id = checkNetworkElementAndGetId(assessedElementId, terminal1Id);
        if (networkElement1Id == null) {
            return false;
        }
        String networkElement2Id = checkNetworkElementAndGetId(assessedElementId, terminal2Id);
        if (networkElement2Id == null) {
            return false;
        }

        boolean areNetworkElementsOk = this.addAngleCnecElements(assessedElementId, angleCnecAdder, networkElement1Id, networkElement2Id, isFlowToRefTerminal);
        if (!areNetworkElementsOk) {
            return false;
        }
        this.addAngleLimitInstant(angleCnecAdder, inBaseCase);
        return this.addAngleLimitThreshold(assessedElementId, angleCnecAdder, cnecLimit, isFlowToRefTerminalIsNull);
    }

    private String checkNetworkElementAndGetId(String assessedElementId, String terminalId) {
        Identifiable<?> networkElement = this.getNetworkElementInNetwork(terminalId);
        if (networkElement == null) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "angle limit equipment is missing in network : " + terminalId));
            return null;
        }
        if (!(networkElement instanceof BusbarSection)) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "network element " + networkElement.getId() + " is not a bus bar section"));
            return null;
        }
        return networkElement.getId();
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

    private boolean setCurrentLimitsFromBranch(String assessedElementId, FlowCnecAdder flowCnecAdder, Branch<?> branch) {
        Double currentLimitLeft = getCurrentLimitFromBranch(branch, Branch.Side.ONE);
        Double currentLimitRight = getCurrentLimitFromBranch(branch, Branch.Side.TWO);
        if (Objects.nonNull(currentLimitLeft) && Objects.nonNull(currentLimitRight)) {
            flowCnecAdder.withIMax(currentLimitLeft, Side.LEFT);
            flowCnecAdder.withIMax(currentLimitRight, Side.RIGHT);
            return true;
        } else {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "Unable to get branch current limits from network for branch " + branch.getId()));
            return false;
        }
    }

    private Double getCurrentLimitFromBranch(Branch<?> branch, Branch.Side side) {

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

    private boolean addCurrentLimitInstant(String assessedElementId, FlowCnecAdder flowCnecAdder, PropertyBag currentLimit, boolean inBaseCase) {
        this.cnecInstant = null;
        String kind = currentLimit.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_KIND);
        Instant instant = null;

        if (CsaProfileConstants.LimitKind.TATL.toString().equals(kind)) {
            String acceptableDurationStr = currentLimit.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_ACCEPTABLE_DURATION);
            Double acceptableDuration = Double.valueOf(acceptableDurationStr);
            if (acceptableDuration == null || acceptableDuration < 0) {
                csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "OperationalLimitType.acceptableDuration is incorrect : " + acceptableDurationStr));
                return false;
            } else if (acceptableDuration == 0) {
                instant = inBaseCase ? Instant.PREVENTIVE : Instant.CURATIVE;
            } else if (acceptableDuration <= CracCreationParameters.DurationThresholdsLimits.DURATION_THRESHOLDS_LIMITS_MAX_OUTAGE_INSTANT.getLimit()) {
                instant = Instant.OUTAGE;
            } else if (acceptableDuration <= CracCreationParameters.DurationThresholdsLimits.DURATION_THRESHOLDS_LIMITS_MAX_AUTO_INSTANT.getLimit()) {
                instant = Instant.AUTO;
            } else {
                instant = Instant.CURATIVE;
            }
            flowCnecAdder.withInstant(instant);
        } else if (CsaProfileConstants.LimitKind.PATL.toString().equals(kind)) {
            instant = inBaseCase ? Instant.PREVENTIVE : Instant.CURATIVE;
            flowCnecAdder.withInstant(instant);
        } else {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "OperationalLimitType.kind is incorrect : " + kind));
            return false;
        }
        this.cnecInstant = instant;
        return true;
    }

    private boolean addCurrentLimitThreshold(String assessedElementId, FlowCnecAdder flowCnecAdder, PropertyBag currentLimit, Identifiable<?> networkElement, Side side) {
        if (side == null) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "could not find side of threshold with network element : " + networkElement.getId()));
            return false;
        }
        String normalValueStr = currentLimit.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_NORMAL_VALUE);
        Double normalValue = Double.valueOf(normalValueStr);
        String direction = currentLimit.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_DIRECTION);
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

    private boolean addVoltageLimitInstant(String assessedElementId, VoltageCnecAdder voltageCnecAdder, PropertyBag voltageLimit, boolean inBaseCase) {
        this.cnecInstant = null;

        String isInfiniteDurationStr = voltageLimit.get(CsaProfileConstants.REQUEST_VOLTAGE_LIMIT_IS_INFINITE_DURATION);
        boolean isInfiniteDuration = isInfiniteDurationStr != null && Boolean.parseBoolean(isInfiniteDurationStr);
        if (!isInfiniteDuration) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "cim:OperationalLimitType.isInfiniteDuration is not true"));
            return false;
        }

        this.cnecInstant = inBaseCase ? Instant.PREVENTIVE : Instant.CURATIVE;
        voltageCnecAdder.withInstant(this.cnecInstant);
        return true;
    }

    private boolean addVoltageLimitThreshold(String assessedElementId, VoltageCnecAdder voltageCnecAdder, PropertyBag voltageLimit) {

        String normalValueStr = voltageLimit.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_NORMAL_VALUE);
        Double normalValue = Double.valueOf(normalValueStr);
        String direction = voltageLimit.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_DIRECTION);
        if (CsaProfileConstants.LimitDirectionKind.HIGH.toString().equals(direction)) {
            voltageCnecAdder.newThreshold()
                .withUnit(Unit.KILOVOLT)
                .withMax(normalValue).add();
        } else if (CsaProfileConstants.LimitDirectionKind.LOW.toString().equals(direction)) {
            voltageCnecAdder.newThreshold()
                .withUnit(Unit.KILOVOLT)
                .withMin(normalValue).add();
        } else if (CsaProfileConstants.LimitDirectionKind.ABSOLUTE.toString().equals(direction)) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.NOT_FOR_RAO, "OperationalLimitType.direction is absolute"));
            return false;
        }
        return true;
    }

    private void addAngleLimitInstant(AngleCnecAdder angleCnecAdder, boolean inBaseCase) {
        this.cnecInstant = inBaseCase ? Instant.PREVENTIVE : Instant.CURATIVE;
        angleCnecAdder.withInstant(this.cnecInstant);
    }

    private boolean addAngleLimitThreshold(String assessedElementId, AngleCnecAdder angleCnecAdder, PropertyBag angleLimit, boolean isFlowToRefTerminalIsNull) {
        String normalValueStr = angleLimit.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_NORMAL_VALUE);
        Double normalValue = Double.valueOf(normalValueStr);
        if (normalValue < 0) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "angle limit normal value is negative"));
            return false;
        }
        String direction = angleLimit.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_DIRECTION);
        if (CsaProfileConstants.LimitDirectionKind.HIGH.toString().equals(direction)) {
            if (handleMissingIsFlowToRefTerminalForNotAbsoluteDirection(assessedElementId, isFlowToRefTerminalIsNull, CsaProfileConstants.LimitDirectionKind.HIGH)) {
                return false;
            }
            angleCnecAdder.newThreshold()
                    .withUnit(Unit.DEGREE)
                    .withMax(normalValue).add();
        } else if (CsaProfileConstants.LimitDirectionKind.LOW.toString().equals(direction)) {
            if (handleMissingIsFlowToRefTerminalForNotAbsoluteDirection(assessedElementId, isFlowToRefTerminalIsNull, CsaProfileConstants.LimitDirectionKind.LOW)) {
                return false;
            }
            angleCnecAdder.newThreshold()
                    .withUnit(Unit.DEGREE)
                    .withMin(-normalValue).add();
        } else if (CsaProfileConstants.LimitDirectionKind.ABSOLUTE.toString().equals(direction)) {
            angleCnecAdder.newThreshold()
                    .withUnit(Unit.DEGREE)
                    .withMin(-normalValue)
                    .withMax(normalValue).add();
        }
        return true;
    }

    private boolean handleMissingIsFlowToRefTerminalForNotAbsoluteDirection(String assessedElementId, boolean isFlowToRefTerminalIsNull, CsaProfileConstants.LimitDirectionKind direction) {
        if (isFlowToRefTerminalIsNull) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "ambiguous angle limit direction definition from an undefined VoltageAngleLimit.isFlowToRefTerminal and an OperationalLimit.OperationalLimitType " + direction));
            return true;
        }
        return false;
    }

    private boolean addAngleCnecElements(String assessedElementId, AngleCnecAdder angleCnecAdder, String networkElement1Id, String networkElement2Id, boolean isFlowToRefTerminal) {
        if (Objects.equals(networkElement1Id, networkElement2Id)) {
            csaProfileCnecCreationContexts.add(CsaProfileCnecCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "AngleCNEC's importing and exporting equipments are the same : " + networkElement1Id));
            return false;
        }
        String importingElement = isFlowToRefTerminal ? networkElement1Id : networkElement2Id;
        String exportingElement = isFlowToRefTerminal ? networkElement2Id : networkElement1Id;
        angleCnecAdder.withImportingNetworkElement(importingElement).withExportingNetworkElement(exportingElement).add();
        return true;
    }
}
