package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnecAdder;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.threshold.BranchThresholdAdder;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.iidm.network.*;
import com.powsybl.triplestore.api.PropertyBag;

import java.util.*;
import java.util.stream.Collectors;

public class FlowCnecCreator extends AbstractCnecCreator {

    private enum FlowCnecDefinitionMode {
        CONDUCTING_EQUIPMENT,
        OPERATIONAL_LIMIT,
        WRONG_DEFINITION
    }

    private final String conductingEquipment;
    private final Set<Side> defaultMonitoredSides;
    private final FlowCnecInstantHelper instantHelper;

    public FlowCnecCreator(Crac crac, Network network, String assessedElementId, String nativeAssessedElementName, String assessedElementOperator, boolean inBaseCase, PropertyBag currentLimitPropertyBag, String conductingEquipment, List<Contingency> linkedContingencies, Set<CsaProfileElementaryCreationContext> csaProfileCnecCreationContexts, CsaProfileCracCreationContext cracCreationContext, String rejectedLinksAssessedElementContingency, boolean aeSecuredForRegion, boolean aeScannedForRegion, CracCreationParameters cracCreationParameters) {
        super(crac, network, assessedElementId, nativeAssessedElementName, assessedElementOperator, inBaseCase, currentLimitPropertyBag, linkedContingencies, csaProfileCnecCreationContexts, cracCreationContext, rejectedLinksAssessedElementContingency, aeSecuredForRegion, aeScannedForRegion);
        this.conductingEquipment = conductingEquipment;
        this.defaultMonitoredSides = cracCreationParameters.getDefaultMonitoredSides();
        this.instantHelper = new FlowCnecInstantHelper(cracCreationParameters);
    }

    public void addFlowCnecs() {
        FlowCnecDefinitionMode definitionMode = getCnecDefinitionMode();
        if (definitionMode == FlowCnecDefinitionMode.WRONG_DEFINITION) {
            return;
        }

        String networkElementId = definitionMode == FlowCnecDefinitionMode.CONDUCTING_EQUIPMENT ? conductingEquipment : operationalLimitPropertyBag.getId(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_TERMINAL);
        Identifiable<?> branch = getFlowCnecBranch(networkElementId);
        if (branch == null) {
            return;
        }

        // The thresholds are a map of acceptable durations to thresholds (per branch side)
        // Integer.MAX_VALUE is used for the PATL's acceptable duration
        Map<Integer, Map<TwoSides, Double>> thresholds = definitionMode == FlowCnecDefinitionMode.CONDUCTING_EQUIPMENT ? getPermanentAndTemporaryLimitsOfBranch((Branch<?>) branch) : getPermanentAndTemporaryLimitsOfOperationalLimit(branch, networkElementId);
        if (thresholds.isEmpty()) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCOMPLETE_DATA, writeAssessedElementIgnoredReasonMessage("no PATL or TATLs could be retrieved for the branch " + branch.getId())));
            return;
        }

        // If the AssessedElement is defined with a conducting equipment, we use both max and min thresholds.
        boolean useMaxAndMinThresholds = true;
        if (definitionMode == FlowCnecDefinitionMode.OPERATIONAL_LIMIT) {
            String direction = operationalLimitPropertyBag.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_DIRECTION);
            if (CsaProfileConstants.OperationalLimitDirectionKind.HIGH.toString().equals(direction)) {
                useMaxAndMinThresholds = false;
            } else if (!CsaProfileConstants.OperationalLimitDirectionKind.ABSOLUTE.toString().equals(direction)) {
                csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.NOT_FOR_RAO, writeAssessedElementIgnoredReasonMessage("OperationalLimitType.direction is neither 'absoluteValue' nor 'high'")));
                return;
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
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, writeAssessedElementIgnoredReasonMessage("the following element is missing from the network: " + networkElementId)));
            return null;
        }
        if (!(networkElement instanceof Branch)) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("the network element " + networkElement.getId() + " is not a branch")));
            return null;
        }
        return networkElement;
    }

    private FlowCnecDefinitionMode getCnecDefinitionMode() {
        if (conductingEquipment == null && operationalLimitPropertyBag == null) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCOMPLETE_DATA, writeAssessedElementIgnoredReasonMessage("no ConductingEquipment or OperationalLimit was provided")));
            return FlowCnecDefinitionMode.WRONG_DEFINITION;
        }
        if (conductingEquipment != null && operationalLimitPropertyBag != null) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("an assessed element must be defined using either a ConductingEquipment or an OperationalLimit, not both")));
            return FlowCnecDefinitionMode.WRONG_DEFINITION;
        }
        return conductingEquipment != null ? FlowCnecDefinitionMode.CONDUCTING_EQUIPMENT : FlowCnecDefinitionMode.OPERATIONAL_LIMIT;
    }

    private Side getSideFromNetworkElement(Identifiable<?> networkElement, String terminalId) {
        if (networkElement instanceof TieLine tieLine) {
            return getSideFromTieLine(tieLine, terminalId);
        } else {
            return getSideFromNonTieLine(networkElement, terminalId);
        }
    }

    private Side getSideFromTieLine(TieLine tieLine, String terminalId) {
        for (String key : CsaProfileConstants.CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_TIE_LINE) {
            Optional<String> oAlias = tieLine.getDanglingLine1().getAliasFromType(key);
            if (oAlias.isPresent() && oAlias.get().equals(terminalId)) {
                return Side.LEFT;
            }
            oAlias = tieLine.getDanglingLine2().getAliasFromType(key);
            if (oAlias.isPresent() && oAlias.get().equals(terminalId)) {
                return Side.RIGHT;
            }
        }
        return null;
    }

    private Side getSideFromNonTieLine(Identifiable<?> networkElement, String terminalId) {
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

    private void addFlowCnecThreshold(FlowCnecAdder flowCnecAdder, Side side, double threshold, boolean useMaxAndMinThresholds) {
        BranchThresholdAdder adder = flowCnecAdder.newThreshold().withSide(side)
            .withUnit(Unit.AMPERE)
            .withMax(threshold);
        if (useMaxAndMinThresholds) {
            adder.withMin(-threshold);
        }
        adder.add();
    }

    private boolean setNominalVoltage(FlowCnecAdder flowCnecAdder, Branch<?> branch) {
        double voltageLevelLeft = branch.getTerminal1().getVoltageLevel().getNominalV();
        double voltageLevelRight = branch.getTerminal2().getVoltageLevel().getNominalV();
        if (voltageLevelLeft > 1e-6 && voltageLevelRight > 1e-6) {
            flowCnecAdder.withNominalVoltage(voltageLevelLeft, Side.LEFT);
            flowCnecAdder.withNominalVoltage(voltageLevelRight, Side.RIGHT);
            return true;
        } else {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "Voltage level for branch " + branch.getId() + " is 0 in network"));
            return false;
        }
    }

    private boolean setCurrentLimitsFromBranch(FlowCnecAdder flowCnecAdder, Branch<?> branch) {
        Double currentLimitLeft = getCurrentLimitFromBranch(branch, TwoSides.ONE);
        Double currentLimitRight = getCurrentLimitFromBranch(branch, TwoSides.TWO);
        if (Objects.nonNull(currentLimitLeft) && Objects.nonNull(currentLimitRight)) {
            flowCnecAdder.withIMax(currentLimitLeft, Side.LEFT);
            flowCnecAdder.withIMax(currentLimitRight, Side.RIGHT);
            return true;
        } else {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "Unable to get branch current limits from network for branch " + branch.getId()));
            return false;
        }
    }

    private Double getCurrentLimitFromBranch(Branch<?> branch, TwoSides side) {

        if (branch.getCurrentLimits(side).isPresent()) {
            return branch.getCurrentLimits(side).orElseThrow().getPermanentLimit();
        }

        if (side == TwoSides.ONE && branch.getCurrentLimits(TwoSides.TWO).isPresent()) {
            return branch.getCurrentLimits(TwoSides.TWO).orElseThrow().getPermanentLimit() * branch.getTerminal1().getVoltageLevel().getNominalV() / branch.getTerminal2().getVoltageLevel().getNominalV();
        }

        if (side == TwoSides.TWO && branch.getCurrentLimits(TwoSides.ONE).isPresent()) {
            return branch.getCurrentLimits(TwoSides.ONE).orElseThrow().getPermanentLimit() * branch.getTerminal2().getVoltageLevel().getNominalV() / branch.getTerminal1().getVoltageLevel().getNominalV();
        }

        return null;
    }

    private void addFlowCnec(Branch<?> networkElement, Contingency contingency, String instantId, Side side, double threshold, int limitDuration, boolean useMaxAndMinThresholds) {
        FlowCnecAdder cnecAdder = initFlowCnec();
        addCnecBaseInformation(cnecAdder, contingency, instantId, side, limitDuration);
        addFlowCnecThreshold(cnecAdder, side, threshold, useMaxAndMinThresholds);
        cnecAdder.withNetworkElement(networkElement.getId());
        if (!setNominalVoltage(cnecAdder, networkElement) || !setCurrentLimitsFromBranch(cnecAdder, networkElement)) {
            return;
        }
        cnecAdder.add();
    }

    private void addAllFlowCnecsFromBranchAndOperationalLimits(Branch<?> networkElement, Map<Integer, Map<TwoSides, Double>> thresholds, boolean useMaxAndMinThresholds) {
        // Preventive CNEC
        if (inBaseCase) {
            thresholds.getOrDefault(Integer.MAX_VALUE, Map.of()).forEach((twoSides, threshold) -> {
                String cnecName = getCnecName(crac.getPreventiveInstant().getId(), null, Side.fromIidmSide(twoSides), Integer.MAX_VALUE);
                addFlowCnec(networkElement, null, crac.getPreventiveInstant().getId(), Side.fromIidmSide(twoSides), threshold, Integer.MAX_VALUE, useMaxAndMinThresholds);
                csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.imported(assessedElementId, cnecName, cnecName, "", false));
            });
        }

        // Curative CNECs
        if (!linkedContingencies.isEmpty()) {
            String operatorName = CsaProfileCracUtils.getTsoNameFromUrl(assessedElementOperator);
            Map<TwoSides, Map<String, Integer>> instantToDurationMaps = Arrays.stream(TwoSides.values()).collect(Collectors.toMap(twoSides -> twoSides, twoSides -> instantHelper.mapPostContingencyInstantsAndLimitDurations(networkElement, twoSides, operatorName)));
            boolean operatorDoesNotUsePatlInFinalState = instantHelper.getTsosWhichDoNotUsePatlInFinalState().contains(CsaProfileCracUtils.getTsoNameFromUrl(assessedElementOperator));

            // If an operator does not use the PATL for the final state but has no TATL defined, the use of PATL if forced
            Map<TwoSides, Boolean> forceUseOfPatl = Arrays.stream(TwoSides.values()).collect(Collectors.toMap(
                twoSides -> twoSides,
                twoSides -> operatorDoesNotUsePatlInFinalState
                    && (networkElement.getCurrentLimits(twoSides).isEmpty() || networkElement.getCurrentLimits(twoSides).isPresent() && networkElement.getCurrentLimits(twoSides).get().getTemporaryLimits().isEmpty())));

            for (Contingency contingency : linkedContingencies) {
                thresholds.forEach((acceptableDuration, limitThresholds) ->
                    limitThresholds.forEach((twoSides, threshold) -> addCurativeFlowCnec(networkElement, useMaxAndMinThresholds, instantToDurationMaps, forceUseOfPatl, contingency, acceptableDuration, twoSides, threshold)));
            }
        }
    }

    private void addCurativeFlowCnec(Branch<?> networkElement, boolean useMaxAndMinThresholds, Map<TwoSides, Map<String, Integer>> instantToDurationMaps, Map<TwoSides, Boolean> forceUseOfPatl, Contingency contingency, Integer acceptableDuration, TwoSides twoSides, Double threshold) {
        instantHelper.getPostContingencyInstantsAssociatedToLimitDuration(instantToDurationMaps.get(twoSides), acceptableDuration).forEach(
            instant -> {
                String cnecName = getCnecName(instant, contingency, Side.fromIidmSide(twoSides), acceptableDuration);
                addFlowCnec(networkElement, contingency, instant, Side.fromIidmSide(twoSides), threshold, acceptableDuration, useMaxAndMinThresholds);
                if (acceptableDuration == Integer.MAX_VALUE && Boolean.TRUE.equals(forceUseOfPatl.get(twoSides))) {
                    csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.imported(assessedElementId, cnecName, cnecName, "TSO %s does not use PATL in final state but has no TATL defined for branch %s on side %s, PATL will be used".formatted(CsaProfileCracUtils.getTsoNameFromUrl(assessedElementOperator), networkElement.getId(), Side.fromIidmSide(twoSides)), true));
                } else {
                    csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.imported(assessedElementId, cnecName, cnecName, "", false));
                }
            }
        );
    }

    private Map<Integer, Map<TwoSides, Double>> getPermanentAndTemporaryLimitsOfOperationalLimit(Identifiable<?> branch, String terminalId) {
        Map<Integer, Map<TwoSides, Double>> thresholds = new HashMap<>();

        String limitType = operationalLimitPropertyBag.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_TYPE);
        Side side = getSideFromNetworkElement(branch, terminalId);
        String valueStr = operationalLimitPropertyBag.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_VALUE);
        Double value = Double.valueOf(valueStr);

        if (side != null) {
            int acceptableDuration;
            if (CsaProfileConstants.LimitTypeKind.PATL.toString().equals(limitType)) {
                acceptableDuration = Integer.MAX_VALUE;
            } else if (CsaProfileConstants.LimitTypeKind.TATL.toString().equals(limitType)) {
                String acceptableDurationStr = operationalLimitPropertyBag.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_ACCEPTABLE_DURATION);
                acceptableDuration = Integer.parseInt(acceptableDurationStr);
            } else {
                return thresholds;
            }
            thresholds.put(acceptableDuration, Map.of(side.iidmSide(), value));
        }

        return thresholds;
    }

    private Map<Integer, Map<TwoSides, Double>> getPermanentAndTemporaryLimitsOfBranch(Branch<?> branch) {
        Set<Side> sidesToCheck = getSidesToCheck(branch);

        Map<Integer, Map<TwoSides, Double>> thresholds = new HashMap<>();

        for (Side side : sidesToCheck) {
            Optional<CurrentLimits> currentLimits = branch.getCurrentLimits(side.iidmSide());
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

    private static void addLimitThreshold(Map<Integer, Map<TwoSides, Double>> thresholds, int acceptableDuration, double threshold, Side side) {
        if (thresholds.containsKey(acceptableDuration)) {
            thresholds.get(acceptableDuration).put(side.iidmSide(), threshold);
        } else {
            thresholds.put(acceptableDuration, new EnumMap<>(Map.of(side.iidmSide(), threshold)));
        }
    }

    private Set<Side> getSidesToCheck(Branch<?> branch) {
        Set<Side> sidesToCheck = new HashSet<>();
        if (defaultMonitoredSides.size() == 2) {
            // TODO: if TieLine, only put relevant side? ask TSOs what is the expected behavior
            sidesToCheck.add(Side.LEFT);
            sidesToCheck.add(Side.RIGHT);
        } else {
            // Only one side in the set -> check the default side.
            // If no limit for the default side, check the other side.
            Side defaultSide = defaultMonitoredSides.stream().toList().get(0);
            Side otherSide = defaultSide == Side.LEFT ? Side.RIGHT : Side.LEFT;
            sidesToCheck.add(branch.getCurrentLimits(defaultSide.iidmSide()).isPresent() ? defaultSide : otherSide);
        }
        return sidesToCheck;
    }
}
