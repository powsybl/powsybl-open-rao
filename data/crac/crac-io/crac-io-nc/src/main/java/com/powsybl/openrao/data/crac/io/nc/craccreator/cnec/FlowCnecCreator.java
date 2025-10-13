/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.cnec;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracUtils;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.LimitTypeKind;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.OperationalLimitDirectionKind;
import com.powsybl.openrao.data.crac.io.nc.objects.AssessedElement;
import com.powsybl.openrao.data.crac.io.nc.objects.CurrentLimit;
import com.powsybl.openrao.data.crac.io.nc.parameters.NcCracCreationParameters;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.threshold.BranchThresholdAdder;
import com.powsybl.openrao.data.crac.io.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.commons.api.StandardElementaryCreationContext;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcConstants.CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_LEFT;
import static com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcConstants.CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_RIGHT;
import static com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcConstants.CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_TIE_LINE;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class FlowCnecCreator extends AbstractCnecCreator {
    private final NcCracCreationParameters ncCracCreationParameters;
    private final Set<TwoSides> defaultMonitoredSides;
    private final FlowCnecInstantHelper instantHelper;
    private final CurrentLimit nativeCurrentLimit;

    public FlowCnecCreator(Crac crac, Network network, AssessedElement nativeAssessedElement, CurrentLimit nativeCurrentLimit, Set<Contingency> linkedContingencies, Set<ElementaryCreationContext> ncCnecCreationContexts, String rejectedLinksAssessedElementContingency, CracCreationParameters cracCreationParameters, Map<String, String> borderPerTso, Map<String, String> borderPerEic) {
        super(crac, network, nativeAssessedElement, linkedContingencies, ncCnecCreationContexts, rejectedLinksAssessedElementContingency, cracCreationParameters, borderPerTso, borderPerEic);
        this.defaultMonitoredSides = cracCreationParameters.getDefaultMonitoredSides();
        this.nativeCurrentLimit = nativeCurrentLimit;
        ncCracCreationParameters = cracCreationParameters.getExtension(NcCracCreationParameters.class);
        if (ncCracCreationParameters == null) {
            throw new OpenRaoException("No NcCracCreatorParameters extension provided.");
        }
        this.instantHelper = new FlowCnecInstantHelper(ncCracCreationParameters, crac);
        checkCnecDefinitionMode();
    }

    private void checkCnecDefinitionMode() {
        if (nativeAssessedElement.conductingEquipment() == null && nativeCurrentLimit == null) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, writeAssessedElementIgnoredReasonMessage("no ConductingEquipment or OperationalLimit was provided"));
        }
        if (nativeAssessedElement.conductingEquipment() != null && nativeCurrentLimit != null) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("an assessed element must be defined using either a ConductingEquipment or an OperationalLimit, not both"));
        }
    }

    public void addFlowCnecs() {
        String networkElementId = nativeAssessedElement.conductingEquipment() != null ? nativeAssessedElement.conductingEquipment() : nativeCurrentLimit.terminal();
        Identifiable<?> branch = getFlowCnecBranch(networkElementId);

        // The thresholds are a map of acceptable durations to thresholds (per branch side)
        // Integer.MAX_VALUE is used for the PATL's acceptable duration
        Map<Integer, Map<TwoSides, Double>> thresholds = nativeAssessedElement.conductingEquipment() != null ? getPermanentAndTemporaryLimitsOfBranch((Branch<?>) branch) : getPermanentAndTemporaryLimitsOfOperationalLimit(branch, networkElementId);
        if (thresholds.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, writeAssessedElementIgnoredReasonMessage("no PATL or TATLs could be retrieved for the branch " + branch.getId()));
        }

        // If the AssessedElement is defined with a conducting equipment, we use both max and min thresholds.
        boolean useMaxAndMinThresholds = true;
        if (nativeAssessedElement.conductingEquipment() == null) {
            if (OperationalLimitDirectionKind.HIGH.toString().equals(nativeCurrentLimit.direction())) {
                useMaxAndMinThresholds = false;
            } else if (!OperationalLimitDirectionKind.ABSOLUTE.toString().equals(nativeCurrentLimit.direction())) {
                throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, writeAssessedElementIgnoredReasonMessage("OperationalLimitType.direction is neither 'absoluteValue' nor 'high'"));
            }
        }

        addAllFlowCnecsFromBranchAndOperationalLimits((Branch<?>) branch, thresholds, useMaxAndMinThresholds);
    }

    private FlowCnecAdder initFlowCnec() {
        return crac.newFlowCnec().withReliabilityMargin(0);
    }

    private Identifiable<?> getFlowCnecBranch(String networkElementId) {
        Identifiable<?> networkElement = getNetworkElementInNetwork(networkElementId);
        if (networkElement == null) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, writeAssessedElementIgnoredReasonMessage("the following network element is missing from the network: " + networkElementId));
        }
        if (!(networkElement instanceof Branch)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("the network element " + networkElement.getId() + " is not a branch"));
        }
        return networkElement;
    }

    private TwoSides getSideFromNetworkElement(Identifiable<?> networkElement, String terminalId) {
        if (networkElement instanceof TieLine tieLine) {
            return getSideFromTieLine(tieLine, terminalId);
        } else {
            return getSideFromNonTieLine(networkElement, terminalId);
        }
    }

    private TwoSides getSideFromTieLine(TieLine tieLine, String terminalId) {
        for (String key : CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_TIE_LINE) {
            Optional<String> oAlias = tieLine.getDanglingLine1().getAliasFromType(key);
            if (oAlias.isPresent() && oAlias.get().equals(terminalId)) {
                return TwoSides.ONE;
            }
            oAlias = tieLine.getDanglingLine2().getAliasFromType(key);
            if (oAlias.isPresent() && oAlias.get().equals(terminalId)) {
                return TwoSides.TWO;
            }
        }
        return null;
    }

    private TwoSides getSideFromNonTieLine(Identifiable<?> networkElement, String terminalId) {
        for (String key : CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_LEFT) {
            Optional<String> oAlias = networkElement.getAliasFromType(key);
            if (oAlias.isPresent() && oAlias.get().equals(terminalId)) {
                return TwoSides.ONE;
            }
        }

        for (String key : CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_RIGHT) {
            Optional<String> oAlias = networkElement.getAliasFromType(key);
            if (oAlias.isPresent() && oAlias.get().equals(terminalId)) {
                return TwoSides.TWO;
            }
        }

        return null;
    }

    private void addFlowCnecThreshold(FlowCnecAdder flowCnecAdder, TwoSides side, double threshold, boolean useMaxAndMinThresholds) {
        if (nativeAssessedElement.flowReliabilityMargin() < 0 || nativeAssessedElement.flowReliabilityMargin() > 100) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("of an invalid flow reliability margin (expected a value between 0 and 100)"));
        }
        double thresholdWithReliabilityMargin = threshold * (1d - nativeAssessedElement.flowReliabilityMargin() / 100d);
        BranchThresholdAdder adder = flowCnecAdder.newThreshold().withSide(side)
            .withUnit(Unit.AMPERE)
            .withMax(thresholdWithReliabilityMargin);
        if (useMaxAndMinThresholds) {
            adder.withMin(-thresholdWithReliabilityMargin);
        }
        adder.add();
    }

    private void setNominalVoltage(FlowCnecAdder flowCnecAdder, Branch<?> branch) {
        double voltageLevelLeft = branch.getTerminal1().getVoltageLevel().getNominalV();
        double voltageLevelRight = branch.getTerminal2().getVoltageLevel().getNominalV();
        if (voltageLevelLeft > 1e-6 && voltageLevelRight > 1e-6) {
            flowCnecAdder.withNominalVoltage(voltageLevelLeft, TwoSides.ONE);
            flowCnecAdder.withNominalVoltage(voltageLevelRight, TwoSides.TWO);
        } else {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Voltage level for branch " + branch.getId() + " is 0 in network");
        }
    }

    private void addFlowCnec(Branch<?> networkElement, Contingency contingency, String instantId, Map<TwoSides, Double> thresholdPerSide, int limitDuration, boolean useMaxAndMinThresholds) {
        if (thresholdPerSide.isEmpty()) {
            return;
        }
        FlowCnecAdder cnecAdder = initFlowCnec();
        addCnecBaseInformation(cnecAdder, contingency, instantId, limitDuration);
        thresholdPerSide.forEach((twoSides, threshold) -> addFlowCnecThreshold(cnecAdder, twoSides, threshold, useMaxAndMinThresholds));
        cnecAdder.withNetworkElement(networkElement.getId());
        setNominalVoltage(cnecAdder, networkElement);
        cnecAdder.add();
    }

    private void addAllFlowCnecsFromBranchAndOperationalLimits(Branch<?> networkElement, Map<Integer, Map<TwoSides, Double>> thresholds, boolean useMaxAndMinThresholds) {
        // Preventive CNEC
        if (nativeAssessedElement.inBaseCase()) {
            Map<TwoSides, Double> thresholdPerSide = thresholds.getOrDefault(Integer.MAX_VALUE, Map.of());
            String cnecName = getCnecName(crac.getPreventiveInstant().getId(), null, Integer.MAX_VALUE);
            addFlowCnec(networkElement, null, crac.getPreventiveInstant().getId(), thresholdPerSide, Integer.MAX_VALUE, useMaxAndMinThresholds);
            ncCnecCreationContexts.add(StandardElementaryCreationContext.imported(nativeAssessedElement.mrid(), cnecName, cnecName, false, ""));
        }

        // Curative CNECs
        if (!linkedContingencies.isEmpty()) {
            String operatorName = NcCracUtils.getTsoNameFromUrl(nativeAssessedElement.operator());
            Map<TwoSides, Map<String, Integer>> instantToDurationMaps = Arrays.stream(TwoSides.values()).collect(Collectors.toMap(twoSides -> twoSides, twoSides -> instantHelper.mapPostContingencyInstantsAndLimitDurations(networkElement, twoSides, operatorName)));
            boolean operatorDoesNotUsePatlInFinalState = ncCracCreationParameters.getTsosWhichDoNotUsePatlInFinalState().contains(operatorName);

            // If an operator does not use the PATL for the final state but has no TATL defined, the use of PATL if forced
            Map<TwoSides, Boolean> forceUseOfPatl = Arrays.stream(TwoSides.values()).collect(Collectors.toMap(
                twoSides -> twoSides,
                twoSides -> operatorDoesNotUsePatlInFinalState
                    && (networkElement.getCurrentLimits(twoSides).isEmpty() || networkElement.getCurrentLimits(twoSides).isPresent() && networkElement.getCurrentLimits(twoSides).get().getTemporaryLimits().isEmpty())));

            linkedContingencies.forEach(
                contingency -> thresholds.forEach(
                    (acceptableDuration, thresholdPerSide) -> addCurativeFlowCnec(networkElement, useMaxAndMinThresholds, instantToDurationMaps, forceUseOfPatl, contingency, acceptableDuration, thresholdPerSide)));
        }
    }

    private void addCurativeFlowCnec(Branch<?> networkElement, boolean useMaxAndMinThresholds, Map<TwoSides, Map<String, Integer>> instantToDurationMaps, Map<TwoSides, Boolean> forceUseOfPatl, Contingency contingency, Integer acceptableDuration, Map<TwoSides, Double> thresholdPerSide) {
        Map<String, Map<TwoSides, Double>> thresholdPerSidePerInstant = new HashMap<>();
        for (TwoSides twoSides : thresholdPerSide.keySet()) {
            double threshold = thresholdPerSide.get(twoSides);
            Set<String> instantsForSide = instantHelper.getPostContingencyInstantsAssociatedToLimitDuration(instantToDurationMaps.get(twoSides), acceptableDuration);
            for (String instant : instantsForSide) {
                thresholdPerSidePerInstant.computeIfAbsent(instant, k -> new HashMap<>()).putIfAbsent(twoSides, threshold);
            }
        }
        thresholdPerSidePerInstant.forEach((instant, thresholdsOfInstant) -> {
            String cnecName = getCnecName(instant, contingency, acceptableDuration);
            addFlowCnec(networkElement, contingency, instant, thresholdsOfInstant, acceptableDuration, useMaxAndMinThresholds);
            if (acceptableDuration == Integer.MAX_VALUE && thresholdPerSide.keySet().stream().anyMatch(twoSides -> Boolean.TRUE.equals(forceUseOfPatl.get(twoSides)))) {
                ncCnecCreationContexts.add(StandardElementaryCreationContext.imported(nativeAssessedElement.mrid(), cnecName, cnecName, true, "TSO %s does not use PATL in final state but has no TATL defined for branch %s on at least one of its sides, PATL will be used".formatted(NcCracUtils.getTsoNameFromUrl(nativeAssessedElement.operator()), networkElement.getId())));
            } else {
                ncCnecCreationContexts.add(StandardElementaryCreationContext.imported(nativeAssessedElement.mrid(), cnecName, cnecName, false, ""));
            }
        });
    }

    private Map<Integer, Map<TwoSides, Double>> getPermanentAndTemporaryLimitsOfOperationalLimit(Identifiable<?> branch, String terminalId) {
        Map<Integer, Map<TwoSides, Double>> thresholds = new HashMap<>();

        TwoSides side = getSideFromNetworkElement(branch, terminalId);

        if (side != null) {
            int acceptableDuration;
            if (LimitTypeKind.PATL.toString().equals(nativeCurrentLimit.limitType())) {
                acceptableDuration = Integer.MAX_VALUE;
            } else if (LimitTypeKind.TATL.toString().equals(nativeCurrentLimit.limitType())) {
                acceptableDuration = Integer.parseInt(nativeCurrentLimit.acceptableDuration());
            } else {
                return thresholds;
            }
            thresholds.put(acceptableDuration, Map.of(side, nativeCurrentLimit.value()));
        }

        return thresholds;
    }

    private Map<Integer, Map<TwoSides, Double>> getPermanentAndTemporaryLimitsOfBranch(Branch<?> branch) {
        Set<TwoSides> sidesToCheck = getSidesToCheck(branch);

        Map<Integer, Map<TwoSides, Double>> thresholds = new HashMap<>();

        for (TwoSides side : sidesToCheck) {
            Optional<CurrentLimits> currentLimits = branch.getCurrentLimits(side);
            if (currentLimits.isPresent()) {
                // Retrieve PATL
                double permanentThreshold = currentLimits.get().getPermanentLimit();
                addLimitThreshold(thresholds, Integer.MAX_VALUE, permanentThreshold, side);
                // Retrieve TATLs
                List<LoadingLimits.TemporaryLimit> temporaryLimits = currentLimits.get().getTemporaryLimits().stream().toList();
                for (LoadingLimits.TemporaryLimit temporaryLimit : temporaryLimits) {
                    int acceptableDuration = temporaryLimit.getAcceptableDuration();
                    double temporaryThreshold = temporaryLimit.getValue();
                    addLimitThreshold(thresholds, acceptableDuration, temporaryThreshold, side);
                }
            }
        }
        return thresholds;
    }

    private static void addLimitThreshold(Map<Integer, Map<TwoSides, Double>> thresholds, int acceptableDuration, double threshold, TwoSides side) {
        if (thresholds.containsKey(acceptableDuration)) {
            thresholds.get(acceptableDuration).put(side, threshold);
        } else {
            thresholds.put(acceptableDuration, new EnumMap<>(Map.of(side, threshold)));
        }
    }

    private Set<TwoSides> getSidesToCheck(Branch<?> branch) {
        Set<TwoSides> sidesToCheck = new HashSet<>();
        if (defaultMonitoredSides.size() == 2) {
            // TODO: if TieLine, only put relevant side? ask TSOs what is the expected behavior
            sidesToCheck.add(TwoSides.ONE);
            sidesToCheck.add(TwoSides.TWO);
        } else {
            // Only one side in the set -> check the default side.
            // If no limit for the default side, check the other side.
            TwoSides defaultSide = defaultMonitoredSides.stream().toList().get(0);
            TwoSides otherSide = defaultSide == TwoSides.ONE ? TwoSides.TWO : TwoSides.ONE;
            sidesToCheck.add(branch.getCurrentLimits(defaultSide).isPresent() ? defaultSide : otherSide);
        }
        return sidesToCheck;
    }
}
