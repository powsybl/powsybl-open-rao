package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnecAdder;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.threshold.BranchThresholdAdder;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.iidm.network.*;
import com.powsybl.triplestore.api.PropertyBag;

import java.util.*;

public class FlowCnecCreator extends AbstractCnecCreator {

    private enum FlowCnecDefinitionMode {
        CONDUCTING_EQUIPMENT,
        OPERATIONAL_LIMIT,
        WRONG_DEFINITION;
    }

    private final String conductingEquipment;
    private final Set<Side> defaultMonitoredSides;

    public FlowCnecCreator(Crac crac, Network network, String assessedElementId, String nativeAssessedElementName, String assessedElementOperator, boolean inBaseCase, PropertyBag currentLimitPropertyBag, String conductingEquipment, List<Contingency> linkedContingencies, Set<CsaProfileElementaryCreationContext> csaProfileCnecCreationContexts, CsaProfileCracCreationContext cracCreationContext, Set<Side> defaultMonitoredSides, String rejectedLinksAssessedElementContingency, boolean aeSecuredForRegion, boolean aeScannedForRegion) {
        super(crac, network, assessedElementId, nativeAssessedElementName, assessedElementOperator, inBaseCase, currentLimitPropertyBag, linkedContingencies, csaProfileCnecCreationContexts, cracCreationContext, rejectedLinksAssessedElementContingency, aeSecuredForRegion, aeScannedForRegion);
        this.conductingEquipment = conductingEquipment;
        this.defaultMonitoredSides = defaultMonitoredSides;
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
        Map<Integer, EnumMap<TwoSides, Double>> thresholds = definitionMode == FlowCnecDefinitionMode.CONDUCTING_EQUIPMENT ? getPermanentAndTemporaryLimitsOfBranch((Branch<?>) branch) : getPermanentAndTemporaryLimitsOfOperationalLimit(branch, networkElementId);
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

        addAllFlowCnecsFromBranchAndOperationalLimits((Branch<?>) branch, thresholds, useMaxAndMinThresholds, definitionMode == FlowCnecDefinitionMode.CONDUCTING_EQUIPMENT);
    }

    private FlowCnecAdder initFlowCnec() {
        return crac.newFlowCnec()
            .withReliabilityMargin(0);
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

    private Instant getCnecInstant(int acceptableDuration) {
        if (acceptableDuration < 0) {
            return null;
        }
        if (0 < acceptableDuration && acceptableDuration <= 60) {
            return crac.getOutageInstant();
        }
        if (60 < acceptableDuration && acceptableDuration <= 900) {
            return crac.getInstant(InstantKind.AUTO);
        }
        return crac.getInstant(InstantKind.CURATIVE);
    }

    private void addFlowCnec(Branch<?> networkElement, Contingency contingency, String instantId, EnumMap<TwoSides, Double> thresholds, boolean useMaxAndMinThresholds, boolean hasNoPatl) {
        if (thresholds.isEmpty()) {
            return;
        }
        FlowCnecAdder cnecAdder = initFlowCnec();
        if (!addCnecBaseInformation(cnecAdder, contingency, instantId)) {
            return;
        }
        for (Map.Entry<TwoSides, Double> thresholdEntry : thresholds.entrySet()) {
            TwoSides side = thresholdEntry.getKey();
            double threshold = thresholdEntry.getValue();
            addFlowCnecThreshold(cnecAdder, side == TwoSides.ONE ? Side.LEFT : Side.RIGHT, threshold, useMaxAndMinThresholds);
        }
        cnecAdder.withNetworkElement(networkElement.getId());
        if (!setNominalVoltage(cnecAdder, networkElement) || !setCurrentLimitsFromBranch(cnecAdder, networkElement)) {
            return;
        }
        cnecAdder.add();
        if (hasNoPatl) {
            String cnecName = getCnecName(instantId, null);
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.imported(assessedElementId, cnecName, cnecName, "the AssessedElement was pointing to a TATL and used inBaseCase. For the preventive instant, this TATL was also used as a PATL to create the CNEC", true));
            return;
        }
        markCnecAsImportedAndHandleRejectedContingencies(getCnecName(instantId, contingency));
    }

    private void addCurativeFlowCnec(Branch<?> networkElement, Contingency contingency, String instantId, EnumMap<TwoSides, Double> thresholds, boolean useMaxAndMinThresholds, int tatlDuration) {
        if (thresholds.isEmpty()) {
            return;
        }
        FlowCnecAdder cnecAdder = initFlowCnec();
        addCnecBaseInformation(cnecAdder, contingency, instantId, tatlDuration);
        for (TwoSides side : thresholds.keySet()) {
            double threshold = thresholds.get(side);
            addFlowCnecThreshold(cnecAdder, side == TwoSides.ONE ? Side.LEFT : Side.RIGHT, threshold, useMaxAndMinThresholds);
        }
        cnecAdder.withNetworkElement(networkElement.getId());
        if (!setNominalVoltage(cnecAdder, networkElement) || !setCurrentLimitsFromBranch(cnecAdder, networkElement)) {
            return;
        }
        cnecAdder.add();
        markCnecAsImportedAndHandleRejectedContingencies(getCnecName(instantId, contingency, tatlDuration));
    }

    private void addAllFlowCnecsFromBranchAndOperationalLimits(Branch<?> networkElement, Map<Integer, EnumMap<TwoSides, Double>> thresholds, boolean useMaxAndMinThresholds, boolean definedWithConductingEquipment) {
        EnumMap<TwoSides, Double> patlThresholds = thresholds.get(Integer.MAX_VALUE);
        boolean hasPatl = thresholds.get(Integer.MAX_VALUE) != null;

        if (inBaseCase) {
            // If no PATL, we use the lowest TATL instead (as in PowSyBl).
            if (hasPatl) {
                addFlowCnec(networkElement, null, crac.getPreventiveInstant().getId(), patlThresholds, useMaxAndMinThresholds, false);
            } else if (definedWithConductingEquipment) {
                // No PATL thus the longest acceptable duration is strictly lower than Integer.MAX_VALUE
                Optional<Integer> longestAcceptableDuration = thresholds.keySet().stream().max(Integer::compareTo);
                longestAcceptableDuration.ifPresent(integer -> addFlowCnec(networkElement, null, crac.getPreventiveInstant().getId(), thresholds.get(integer), useMaxAndMinThresholds, true));
            }
        }

        for (Contingency contingency : linkedContingencies) {
            // Add PATL
            if (hasPatl) {
                addFlowCnec(networkElement, contingency, crac.getInstant(InstantKind.CURATIVE).getId(), patlThresholds, useMaxAndMinThresholds, false);
            }
            // Add TATLs
            addTatls(networkElement, thresholds, useMaxAndMinThresholds, contingency);
        }
    }

    private void addTatls(Branch<?> networkElement, Map<Integer, EnumMap<TwoSides, Double>> thresholds, boolean useMaxAndMinThresholds, Contingency contingency) {
        for (Map.Entry<Integer, EnumMap<TwoSides, Double>> thresholdEntry : thresholds.entrySet()) {
            int acceptableDuration = thresholdEntry.getKey();
            if (acceptableDuration != Integer.MAX_VALUE) {
                Instant instant = getCnecInstant(acceptableDuration);
                if (instant == null) {
                    csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("TATL acceptable duration is negative: " + acceptableDuration)));
                    //TODO : this most likely needs fixing, maybe continue is enough as a fix instead of return, but then the context wont be very clear since some will have been imported (can already be the case)
                    continue;
                }
                addCurativeFlowCnec(networkElement, contingency, instant.getId(), thresholds.get(acceptableDuration), useMaxAndMinThresholds, acceptableDuration);
            }
        }
    }

    private Map<Integer, EnumMap<TwoSides, Double>> getPermanentAndTemporaryLimitsOfOperationalLimit(Identifiable<?> branch, String terminalId) {
        Map<Integer, EnumMap<TwoSides, Double>> thresholds = new HashMap<>();

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
            thresholds.put(acceptableDuration, new EnumMap<>(Map.of(side.iidmSide(), value)));
        }

        return thresholds;
    }

    private Map<Integer, EnumMap<TwoSides, Double>> getPermanentAndTemporaryLimitsOfBranch(Branch<?> branch) {
        Set<Side> sidesToCheck = getSidesToCheck(branch);

        Map<Integer, EnumMap<TwoSides, Double>> thresholds = new HashMap<>();

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

    private static void addLimitThreshold(Map<Integer, EnumMap<TwoSides, Double>> thresholds, int acceptableDuration, double threshold, Side side) {
        if (thresholds.containsKey(acceptableDuration)) {
            thresholds.get(acceptableDuration).put(side.iidmSide(), threshold);
        } else {
            thresholds.put(acceptableDuration, new EnumMap<>(Map.of(side.iidmSide(), threshold)));
        }
    }

    private Set<Side> getSidesToCheck(Branch<?> branch) {
        Set<Side> sidesToCheck = new HashSet<>();
        if (defaultMonitoredSides.size() == 2) {
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
