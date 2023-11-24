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
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileElementaryCreationContext;
import com.powsybl.iidm.network.*;
import com.powsybl.triplestore.api.PropertyBag;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class FlowCnecCreator extends CnecCreator {

    private enum FlowCnecDefinitionMode {
        CONDUCTING_EQUIPMENT,
        OPERATIONAL_LIMIT,
        WRONG_DEFINITION;
    }

    private final String conductingEquipement;
    private final Set<Side> defaultMonitoredSides;

    public FlowCnecCreator(Crac crac, Network network, String assessedElementId, String nativeAssessedElementName, String assessedElementOperator, boolean inBaseCase, PropertyBag currentLimitPropertyBag, String conductingEquipement, List<Contingency> linkedContingencies, Set<CsaProfileElementaryCreationContext> csaProfileCnecCreationContexts, CsaProfileCracCreationContext cracCreationContext, Set<Side> defaultMonitoredSides) {
        super(crac, network, assessedElementId, nativeAssessedElementName, assessedElementOperator, inBaseCase, currentLimitPropertyBag, linkedContingencies, csaProfileCnecCreationContexts, cracCreationContext);
        this.conductingEquipement = conductingEquipement;
        this.defaultMonitoredSides = defaultMonitoredSides;
    }

    private FlowCnecAdder initFlowCnec() {
        return crac.newFlowCnec()
                .withMonitored(false)
                .withOptimized(true)
                .withReliabilityMargin(0);
    }

    private boolean addCurrentLimit(FlowCnecAdder flowCnecAdder) {
        String terminalId = operationalLimitPropertyBag.getId(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_TERMINAL);
        Identifiable<?> networkElement = this.getNetworkElementInNetwork(terminalId);
        if (networkElement == null) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "current limit equipment is missing in network : " + terminalId));
            return false;
        }

        if (!(networkElement instanceof Branch)) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "network element " + networkElement.getId() + " is not a branch"));
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

        boolean isInstantOk = this.addCurrentLimitInstant(assessedElementId, flowCnecAdder, operationalLimitPropertyBag);
        if (!isInstantOk) {
            return false;
        }

        return this.addFlowCnecThreshold(assessedElementId, flowCnecAdder, operationalLimitPropertyBag, networkElement, this.getSideFromNetworkElement(networkElement, terminalId));
    }

    private boolean addCurrentLimitInstant(String assessedElementId, FlowCnecAdder flowCnecAdder, PropertyBag currentLimit) {
        this.cnecInstant = null;
        String kind = currentLimit.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_KIND);
        Instant instant;

        if (CsaProfileConstants.LimitKind.TATL.toString().equals(kind)) {
            String acceptableDurationStr = currentLimit.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_ACCEPTABLE_DURATION);
            double acceptableDuration = Double.parseDouble(acceptableDurationStr);
            if (acceptableDuration < 0) {
                csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "OperationalLimitType.acceptableDuration is incorrect : " + acceptableDurationStr));
                return false;
            } else if (acceptableDuration <= CracCreationParameters.DurationThresholdsLimits.DURATION_THRESHOLDS_LIMITS_MAX_OUTAGE_INSTANT.getLimit()) {
                instant = Instant.OUTAGE;
            } else if (acceptableDuration <= CracCreationParameters.DurationThresholdsLimits.DURATION_THRESHOLDS_LIMITS_MAX_AUTO_INSTANT.getLimit()) {
                instant = Instant.AUTO;
            } else {
                instant = Instant.CURATIVE;
            }
            flowCnecAdder.withInstant(instant);
        } else if (CsaProfileConstants.LimitKind.PATL.toString().equals(kind)) {
            instant = Instant.CURATIVE;
            flowCnecAdder.withInstant(instant);
        } else {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "OperationalLimitType.kind is incorrect : " + kind));
            return false;
        }
        this.cnecInstant = instant;
        return true;
    }

    private Side getSideFromNetworkElement(Identifiable<?> networkElement, String terminalId) {
        if (networkElement instanceof TieLine) {
            for (String key : CsaProfileConstants.CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_TIE_LINE) {
                TieLine tieLine = (TieLine) networkElement;
                Optional<String> oAlias = tieLine.getDanglingLine1().getAliasFromType(key);
                if (oAlias.isPresent() && oAlias.get().equals(terminalId)) {
                    return Side.LEFT;
                }
                oAlias = tieLine.getDanglingLine2().getAliasFromType(key);
                if (oAlias.isPresent() && oAlias.get().equals(terminalId)) {
                    return Side.RIGHT;
                }

            }
        } else {
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
        }
        return null;
    }

    private boolean addFlowCnecThreshold(String assessedElementId, FlowCnecAdder flowCnecAdder, PropertyBag currentLimit, Identifiable<?> networkElement, Side side) {
        if (side == null) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "could not find side of threshold with network element : " + networkElement.getId()));
            return false;
        }
        String normalValueStr = currentLimit.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_NORMAL_VALUE);
        Double normalValue = Double.valueOf(normalValueStr);
        String direction = currentLimit.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_DIRECTION);
        if (CsaProfileConstants.OperationalLimitDirectionKind.ABSOLUTE.toString().equals(direction)) {
            flowCnecAdder.newThreshold().withSide(side)
                    .withUnit(Unit.AMPERE)
                    .withMax(normalValue)
                    .withMin(-normalValue).add();
        } else if (CsaProfileConstants.OperationalLimitDirectionKind.HIGH.toString().equals(direction)) {
            flowCnecAdder.newThreshold().withSide(side)
                    .withUnit(Unit.AMPERE)
                    .withMax(normalValue).add();
        } else if (CsaProfileConstants.OperationalLimitDirectionKind.LOW.toString().equals(direction)) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.NOT_FOR_RAO, "OperationalLimitType.direction is low"));
            return false;
        }
        return true;
    }

    private void addFlowCnecThreshold(FlowCnecAdder flowCnecAdder, Side side, double threshold) {
        flowCnecAdder.newThreshold().withSide(side)
                .withUnit(Unit.AMPERE)
                .withMax(threshold)
                .withMin(-threshold)
                .add();
    }

    private boolean setNominalVoltage(String assessedElementId, FlowCnecAdder flowCnecAdder, Branch<?> branch) {
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

    private boolean setCurrentLimitsFromBranch(String assessedElementId, FlowCnecAdder flowCnecAdder, Branch<?> branch) {
        Double currentLimitLeft = getCurrentLimitFromBranch(branch, Branch.Side.ONE);
        Double currentLimitRight = getCurrentLimitFromBranch(branch, Branch.Side.TWO);
        if (Objects.nonNull(currentLimitLeft) && Objects.nonNull(currentLimitRight)) {
            flowCnecAdder.withIMax(currentLimitLeft, Side.LEFT);
            flowCnecAdder.withIMax(currentLimitRight, Side.RIGHT);
            return true;
        } else {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "Unable to get branch current limits from network for branch " + branch.getId()));
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

    private String writeAssessedElementIgnoredReasonMessage(String assessedElementId, String reason) {
        return "Assessed Element " + assessedElementId + " ignored because " + reason + ".";
    }

    private Instant getCnecInstant(int acceptableDuration) {
        if (0 < acceptableDuration && acceptableDuration <= 60) {
            return Instant.OUTAGE;
        }
        if (60 < acceptableDuration && acceptableDuration <= 900) {
            return Instant.AUTO;
        }
        return Instant.CURATIVE;
    }

    private void addFlowCnec(String assessedElementId, String assessedElementName, Branch<?> networkElement, Contingency contingency, Instant instant, EnumMap<Branch.Side, Double> thresholds, String rejectedLinksAssessedElementContingency) {
        if (thresholds.isEmpty()) {
            return;
        }
        FlowCnecAdder cnecAdder = initFlowCnec();
        String cnecName = assessedElementName + " (" + assessedElementId + ")" + (contingency != null ? " - " + contingency.getName() : "") + " - " + instant;
        for (Branch.Side side : thresholds.keySet()) {
            double threshold = thresholds.get(side);
            addFlowCnecThreshold(cnecAdder, side == Branch.Side.ONE ? Side.LEFT : Side.RIGHT, threshold);
        }
        addFlowCnecData(cnecAdder, networkElement, contingency, instant);
        setNominalVoltage(assessedElementId, cnecAdder, networkElement);
        cnecAdder.add();
        handleRejectedLinksAssessedElementContingency(assessedElementId, cnecName, rejectedLinksAssessedElementContingency);
    }
    private void addFlowCnecData(FlowCnecAdder cnecAdder, Branch<?> networkElement, Contingency contingency, Instant instant) {
        cnecAdder.withNetworkElement(networkElement.getId());
        addCnecData(cnecAdder, contingency, instant);
    }

    private void addAllFlowCnecsFromConductingEquipment(String assessedElementId, String assessedElementName, Branch<?> networkElement, boolean inBaseCase, EnumMap<Branch.Side, Double> patlThresholds, Map<Integer, EnumMap<Branch.Side, Double>> tatlThresholds, Set<Contingency> combinableContingencies, String rejectedLinksAssessedElementContingency) {
        if (inBaseCase) {
            addFlowCnec(assessedElementId, assessedElementName, networkElement, null, Instant.PREVENTIVE, patlThresholds, rejectedLinksAssessedElementContingency);
        }

        for (Contingency contingency : combinableContingencies) {
            // Add PATL
            addFlowCnec(assessedElementId, assessedElementName, networkElement, contingency, Instant.CURATIVE, patlThresholds, rejectedLinksAssessedElementContingency);
            // Add TATLs
            for (int acceptableDuration : tatlThresholds.keySet()) {
                Instant instant = getCnecInstant(acceptableDuration);
                addFlowCnec(assessedElementId, assessedElementName, networkElement, contingency, instant, tatlThresholds.get(acceptableDuration), rejectedLinksAssessedElementContingency);
            }
        }
    }

    private Pair<EnumMap<Branch.Side, Double>, Map<Integer, EnumMap<Branch.Side, Double>>> getPermanentAndTemporaryLimitsOfBranch(Branch<?> branch) {
        Set<Side> sidesToCheck = getSidesToCheck(branch);

        EnumMap<Branch.Side, Double> patlThresholds = new EnumMap<>(Branch.Side.class);
        Map<Integer, EnumMap<Branch.Side, Double>> tatlThresholds = new HashMap<>();

        for (Side side : sidesToCheck) {
            Optional<CurrentLimits> currentLimits = branch.getCurrentLimits(side.iidmSide());
            if (currentLimits.isPresent()) {
                // Retrieve PATL
                Double threshold = currentLimits.get().getPermanentLimit();
                patlThresholds.put(side.iidmSide(), threshold);
                // Retrieve TATLs
                List<LoadingLimits.TemporaryLimit> temporaryLimits = currentLimits.get().getTemporaryLimits().stream().toList();
                for (LoadingLimits.TemporaryLimit temporaryLimit : temporaryLimits) {
                    int acceptableDuration = temporaryLimit.getAcceptableDuration();
                    double temporaryThreshold = temporaryLimit.getValue();
                    if (tatlThresholds.containsKey(acceptableDuration)) {
                        tatlThresholds.get(acceptableDuration).put(side.iidmSide(), temporaryThreshold);
                    } else {
                        tatlThresholds.put(acceptableDuration, new EnumMap<>(Map.of(side.iidmSide(), temporaryThreshold)));
                    }
                }
            }
        }
        return Pair.of(patlThresholds, tatlThresholds);
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
