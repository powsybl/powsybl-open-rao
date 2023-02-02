/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cse.remedial_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialActionAdder;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeActionAdder;
import com.farao_community.farao.data.crac_api.range_action.PstRangeActionAdder;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.cse.*;
import com.farao_community.farao.data.crac_creation.creator.cse.parameters.BusBarChangeSwitches;
import com.farao_community.farao.data.crac_creation.creator.cse.parameters.CseCracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.RangeActionGroup;
import com.farao_community.farao.data.crac_creation.creator.cse.xsd.*;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzer;
import com.farao_community.farao.data.crac_creation.util.ucte.UctePstHelper;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteTopologicalElementHelper;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class TRemedialActionAdder {
    private final TCRACSeries tcracSeries;
    private final Crac crac;
    private final Network network;
    private final UcteNetworkAnalyzer ucteNetworkAnalyzer;
    private final CseCracCreationContext cseCracCreationContext;
    private final Map<String, Set<String>> remedialActionsForCnecsMap;
    private final CseCracCreationParameters cseCracCreationParameters;

    private static final String ABSOLUTE_VARIATION_TYPE = "ABSOLUTE";

    public TRemedialActionAdder(TCRACSeries tcracSeries, Crac crac, Network network, UcteNetworkAnalyzer ucteNetworkAnalyzer, Map<String, Set<String>> remedialActionsForCnecsMap, CseCracCreationContext cseCracCreationContext, CseCracCreationParameters cseCracCreationParameters) {
        this.tcracSeries = tcracSeries;
        this.crac = crac;
        this.network = network;
        this.ucteNetworkAnalyzer = ucteNetworkAnalyzer;
        this.cseCracCreationContext = cseCracCreationContext;
        this.remedialActionsForCnecsMap = remedialActionsForCnecsMap;
        this.cseCracCreationParameters = cseCracCreationParameters;
    }

    public void add() {
        List<TRemedialActions> tRemedialActionsList = tcracSeries.getRemedialActions();
        for (TRemedialActions tRemedialActions : tRemedialActionsList) {
            if (tRemedialActions != null) {
                tRemedialActions.getRemedialAction().forEach(tRemedialAction -> {
                    try {
                        importRemedialAction(tRemedialAction);
                    } catch (FaraoException e) {
                        // unsupported remedial action type
                        cseCracCreationContext.addRemedialActionCreationContext(
                            CseRemedialActionCreationContext.notImported(tRemedialAction, ImportStatus.NOT_YET_HANDLED_BY_FARAO, e.getMessage())
                        );
                    }
                });
            }
        }
    }

    private void importRemedialAction(TRemedialAction tRemedialAction) {
        if (tRemedialAction.getStatus() != null) {
            importTopologicalAction(tRemedialAction);
        } else if (tRemedialAction.getGeneration() != null) {
            importInjectionAction(tRemedialAction);
        } else if (tRemedialAction.getPstRange() != null) {
            importPstRangeAction(tRemedialAction);
        } else if (tRemedialAction.getHVDCRange() != null) {
            importHvdcRangeAction(tRemedialAction);
        } else if (tRemedialAction.getBusBar() != null) {
            importBusBarChangeAction(tRemedialAction);
        } else {
            throw new FaraoException("unknown remedial action type");
        }
    }

    private void importTopologicalAction(TRemedialAction tRemedialAction) {
        String createdRAId = tRemedialAction.getName().getV();
        NetworkActionAdder networkActionAdder = crac.newNetworkAction()
            .withId(createdRAId)
            .withName(tRemedialAction.getName().getV())
            .withOperator(tRemedialAction.getOperator().getV());

        if (tRemedialAction.getStatus().getBranch().isEmpty()) {
            cseCracCreationContext.addRemedialActionCreationContext(CseRemedialActionCreationContext.notImported(tRemedialAction, ImportStatus.INCOMPLETE_DATA, "field 'Status' of a topological remedial action cannot be empty"));
            return;
        }

        for (TBranch tBranch : tRemedialAction.getStatus().getBranch()) {
            UcteTopologicalElementHelper branchHelper = new UcteTopologicalElementHelper(tBranch.getFromNode().getV(), tBranch.getToNode().getV(), String.valueOf(tBranch.getOrder().getV()), createdRAId, ucteNetworkAnalyzer);
            if (!branchHelper.isValid()) {
                cseCracCreationContext.addRemedialActionCreationContext(CseRemedialActionCreationContext.notImported(tRemedialAction, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, branchHelper.getInvalidReason()));
                return;
            }
            networkActionAdder.newTopologicalAction()
                .withNetworkElement(branchHelper.getIdInNetwork())
                .withActionType(convertActionType(tBranch.getStatus()))
                .add();
        }

        addUsageRules(networkActionAdder, tRemedialAction);
        networkActionAdder.add();
        cseCracCreationContext.addRemedialActionCreationContext(CseRemedialActionCreationContext.imported(tRemedialAction, createdRAId, false, null));
    }

    private void importInjectionAction(TRemedialAction tRemedialAction) {
        String createdRAId = tRemedialAction.getName().getV();

        NetworkActionAdder networkActionAdder = crac.newNetworkAction()
            .withId(createdRAId)
            .withName(tRemedialAction.getName().getV())
            .withOperator(tRemedialAction.getOperator().getV());

        boolean isAltered = false;
        String alteringDetail = null;
        for (TNode tNode : tRemedialAction.getGeneration().getNode()) {
            if (!tNode.getVariationType().getV().equals(ABSOLUTE_VARIATION_TYPE)) {
                cseCracCreationContext.addRemedialActionCreationContext(
                    CseRemedialActionCreationContext.notImported(tRemedialAction, ImportStatus.NOT_YET_HANDLED_BY_FARAO, String.format("node %s is not defined as an ABSOLUTE injectionSetpoint (only ABSOLUTE is implemented).", tNode.getName().getV()))
                );
                return;
            }

            GeneratorHelper generatorHelper = new GeneratorHelper(tNode.getName().getV(), ucteNetworkAnalyzer);
            if (!generatorHelper.isValid()) {
                cseCracCreationContext.addRemedialActionCreationContext(CseRemedialActionCreationContext.notImported(tRemedialAction, generatorHelper.getImportStatus(), generatorHelper.getDetail()));
                return;
            } else if (generatorHelper.isAltered()) {
                isAltered = true;
                if (alteringDetail == null) {
                    alteringDetail = generatorHelper.getDetail();
                } else {
                    alteringDetail += ", " + generatorHelper.getDetail();
                }
            }
            try {
                networkActionAdder.newInjectionSetPoint()
                    .withNetworkElement(generatorHelper.getGeneratorId())
                    .withSetpoint(tNode.getValue().getV())
                    .add();
            } catch (FaraoException e) {
                cseCracCreationContext.addRemedialActionCreationContext(CseRemedialActionCreationContext.notImported(tRemedialAction, ImportStatus.OTHER, e.getMessage()));
                return;
            }

        }
        // After looping on all nodes
        addUsageRules(networkActionAdder, tRemedialAction);
        networkActionAdder.add();
        cseCracCreationContext.addRemedialActionCreationContext(CseRemedialActionCreationContext.imported(tRemedialAction, createdRAId, isAltered, alteringDetail));
    }

    private void importPstRangeAction(TRemedialAction tRemedialAction) {
        String raId = tRemedialAction.getName().getV();
        tRemedialAction.getPstRange().getBranch().forEach(tBranch -> {
            UctePstHelper pstHelper = new UctePstHelper(tBranch.getFromNode().getV(), tBranch.getToNode().getV(), String.valueOf(tBranch.getOrder().getV()), raId, ucteNetworkAnalyzer);
            if (!pstHelper.isValid()) {
                cseCracCreationContext.addRemedialActionCreationContext(CsePstCreationContext.notImported(tRemedialAction, pstHelper.getUcteId(), ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, pstHelper.getInvalidReason()));
                return;
            }
            String id = "PST_" + raId + "_" + pstHelper.getIdInNetwork();
            int pstInitialTap = pstHelper.getInitialTap();
            Map<Integer, Double> conversionMap = pstHelper.getTapToAngleConversionMap();

            PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction()
                .withId(id)
                .withName(tRemedialAction.getName().getV())
                .withOperator(tRemedialAction.getOperator().getV())
                .withNetworkElement(pstHelper.getIdInNetwork())
                .withInitialTap(pstInitialTap)
                .withTapToAngleConversionMap(conversionMap)
                .newTapRange()
                .withMinTap(tRemedialAction.getPstRange().getMin().getV())
                .withMaxTap(tRemedialAction.getPstRange().getMax().getV())
                .withRangeType(convertRangeType(tRemedialAction.getPstRange().getVariationType()))
                .add();

            addUsageRules(pstRangeActionAdder, tRemedialAction);
            pstRangeActionAdder.add();
            String nativeNetworkElementId = String.format("%1$-8s %2$-8s %3$s", pstHelper.getOriginalFrom(), pstHelper.getOriginalTo(), pstHelper.getSuffix());
            cseCracCreationContext.addRemedialActionCreationContext(CsePstCreationContext.imported(tRemedialAction, nativeNetworkElementId, id, false, null));
        });
    }

    private void importHvdcRangeAction(TRemedialAction tRemedialAction) {
        String raId = tRemedialAction.getName().getV();

        // ----  HVDC Nodes
        THVDCNode hvdcNodes = tRemedialAction.getHVDCRange().getHVDCNode().get(0);
        GeneratorHelper generatorFromHelper = new GeneratorHelper(hvdcNodes.getFromNode().getV(), ucteNetworkAnalyzer);
        GeneratorHelper generatorToHelper = new GeneratorHelper(hvdcNodes.getToNode().getV(), ucteNetworkAnalyzer);

        // ---- Only handle ABSOLUTE variation type
        if (!tRemedialAction.getHVDCRange().getVariationType().getV().equals(ABSOLUTE_VARIATION_TYPE)) {
            cseCracCreationContext.addRemedialActionCreationContext(
                CseHvdcCreationContext.notImported(tRemedialAction,
                    ImportStatus.NOT_YET_HANDLED_BY_FARAO,
                    String.format("HVDC %s is not defined with an ABSOLUTE variation type (only ABSOLUTE is handled)", raId),
                    hvdcNodes.getFromNode().getV(),
                    hvdcNodes.getToNode().getV()));
            return;
        }

        // ---- Only handle one HVDC Node
        if (tRemedialAction.getHVDCRange().getHVDCNode().size() > 1) {
            cseCracCreationContext.addRemedialActionCreationContext(
                CseHvdcCreationContext.notImported(tRemedialAction,
                    ImportStatus.INCONSISTENCY_IN_DATA,
                    String.format("HVDC %s has %s (>1) HVDC nodes", raId, tRemedialAction.getHVDCRange().getHVDCNode().size()),
                    hvdcNodes.getFromNode().getV(),
                    hvdcNodes.getToNode().getV()));
            return;
        }

        // ---- check if generators are present
        if (!generatorFromHelper.isValid() || !generatorToHelper.isValid()) {

            String importStatusDetails;
            if (generatorToHelper.isValid()) {
                importStatusDetails = generatorFromHelper.getDetail();
            } else if (generatorFromHelper.isValid()) {
                importStatusDetails = generatorToHelper.getDetail();
            } else {
                importStatusDetails = generatorFromHelper.getDetail() + " & " + generatorToHelper.getDetail();
            }

            cseCracCreationContext.addRemedialActionCreationContext(
                CseHvdcCreationContext.notImported(tRemedialAction,
                    ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK,
                    importStatusDetails,
                    hvdcNodes.getFromNode().getV(),
                    hvdcNodes.getToNode().getV()));
            return;
        }

        // ---- check if generator have inverted signs
        if (Math.abs(generatorFromHelper.getCurrentP() + generatorToHelper.getCurrentP()) > 0.1) {
            cseCracCreationContext.addRemedialActionCreationContext(
                CseHvdcCreationContext.notImported(tRemedialAction,
                    ImportStatus.INCONSISTENCY_IN_DATA,
                    "the two generators of the HVDC must have opposite power output values",
                    hvdcNodes.getFromNode().getV(),
                    hvdcNodes.getToNode().getV()));
            return;
        }

        // ---- create range action
        InjectionRangeActionAdder injectionRangeActionAdder = crac.newInjectionRangeAction()
            .withId(raId)
            .withName(tRemedialAction.getName().getV())
            .withOperator(tRemedialAction.getOperator().getV())
            .withNetworkElementAndKey(-1., generatorFromHelper.getGeneratorId())
            .withNetworkElementAndKey(1., generatorToHelper.getGeneratorId())
            .withInitialSetpoint(generatorToHelper.getCurrentP())
            .newRange()
            .withMin(tRemedialAction.getHVDCRange().getMin().getV())
            .withMax(tRemedialAction.getHVDCRange().getMax().getV())
            .add()
            .newRange()
            .withMin(Math.max(-generatorFromHelper.getPmax(), generatorToHelper.getPmin()))
            .withMax(Math.min(-generatorFromHelper.getPmin(), generatorToHelper.getPmax()))
            .add();

        // ---- add groupId if present
        if (cseCracCreationParameters != null && cseCracCreationParameters.getRangeActionGroups() != null) {
            List<RangeActionGroup> groups = cseCracCreationParameters.getRangeActionGroups().stream()
                .filter(rangeActionGroup -> rangeActionGroup.getRangeActionsIds().contains(raId))
                .collect(Collectors.toList());
            if (groups.size() == 1) {
                injectionRangeActionAdder.withGroupId(groups.get(0).toString());
            } else if (groups.size() > 1) {
                injectionRangeActionAdder.withGroupId(groups.get(0).toString());
                cseCracCreationContext.getCreationReport().warn(String.format("GroupId defined multiple times for HVDC %s, only group %s is used.", raId, groups.get(0)));
            }
        }

        addUsageRules(injectionRangeActionAdder, tRemedialAction);
        injectionRangeActionAdder.add();
        cseCracCreationContext.addRemedialActionCreationContext(CseHvdcCreationContext.imported(tRemedialAction,
            raId,
            hvdcNodes.getFromNode().getV(),
            generatorFromHelper.getGeneratorId(),
            hvdcNodes.getToNode().getV(),
            generatorToHelper.getGeneratorId()));
    }

    private static ActionType convertActionType(TStatusType tStatusType) {
        switch (tStatusType.getV()) {
            case "CLOSE":
                return ActionType.CLOSE;
            case "OPEN":
            default:
                return ActionType.OPEN;
        }
    }

    private static RangeType convertRangeType(TVariationType tVariationType) {
        if (tVariationType.getV().equals(ABSOLUTE_VARIATION_TYPE)) {
            return RangeType.ABSOLUTE;
        } else {
            throw new IllegalArgumentException(String.format("%s type is not handled by the importer", tVariationType.getV()));
        }
    }

    private static Instant getInstant(TApplication tApplication) {
        switch (tApplication.getV()) {
            case "PREVENTIVE":
                return Instant.PREVENTIVE;
            case "SPS":
                return Instant.AUTO;
            case "CURATIVE":
                return Instant.CURATIVE;
            default:
                throw new IllegalArgumentException(String.format("%s is not a recognized application type for remedial action", tApplication.getV()));
        }
    }

    void addUsageRules(RemedialActionAdder<?> remedialActionAdder, TRemedialAction tRemedialAction) {
        Instant raApplicationInstant = getInstant(tRemedialAction.getApplication());
        addOnFlowConstraintUsageRules(remedialActionAdder, tRemedialAction, raApplicationInstant);

        // According to <SharedWith> tag :
        String sharedWithId = tRemedialAction.getSharedWith().getV();
        if (sharedWithId.equals("CSE")) {
            if (raApplicationInstant.equals(Instant.AUTO)) {
                throw new FaraoException("Cannot import automatons from CSE CRAC yet");
            } else {
                addFreeToUseUsageRules(remedialActionAdder, raApplicationInstant);
            }
        } else {
            addOnFlowConstraintUsageRulesAfterSpecificCountry(remedialActionAdder, tRemedialAction, raApplicationInstant, sharedWithId);
        }
    }

    private void addOnFlowConstraintUsageRulesAfterSpecificCountry(RemedialActionAdder<?> remedialActionAdder, TRemedialAction tRemedialAction, Instant raApplicationInstant, String sharedWithId) {
        // Check that sharedWithID is a UCTE country
        if (sharedWithId.equals("None")) {
            return;
        }

        Country country;
        try {
            country = Country.valueOf(sharedWithId);
        } catch (IllegalArgumentException e) {
            cseCracCreationContext.getCreationReport().removed(String.format("RA %s has a non-UCTE sharedWith country : %s. The usage rule was not created.", tRemedialAction.getName().getV(), sharedWithId));
            return;
        }

        // RA is available for specific UCTE country
        remedialActionAdder.newOnFlowConstraintInCountryUsageRule()
            .withInstant(raApplicationInstant)
            .withCountry(country)
            .add();
    }

    private void addFreeToUseUsageRules(RemedialActionAdder<?> remedialActionAdder, Instant raApplicationInstant) {
        // RA is available for all countries
        remedialActionAdder.newFreeToUseUsageRule()
            .withInstant(raApplicationInstant)
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add();
    }

    private void addOnFlowConstraintUsageRules(RemedialActionAdder<?> remedialActionAdder, TRemedialAction tRemedialAction, Instant raApplicationInstant) {
        if (remedialActionsForCnecsMap.containsKey(tRemedialAction.getName().getV())) {
            for (String flowCnecId : remedialActionsForCnecsMap.get(tRemedialAction.getName().getV())) {
                // Only add the usage rule if the RemedialAction can be applied before or during CNEC instant
                if (raApplicationInstant.compareTo(crac.getFlowCnec(flowCnecId).getState().getInstant()) <= 0) {
                    remedialActionAdder.newOnFlowConstraintUsageRule()
                        .withInstant(raApplicationInstant)
                        .withFlowCnec(flowCnecId)
                        .add();
                }
            }
        }
    }

    void importBusBarChangeAction(TRemedialAction tRemedialAction) {
        String raId = tRemedialAction.getName().getV();
        if (cseCracCreationParameters == null || cseCracCreationParameters.getBusBarChangeSwitches(raId) == null) {
            cseCracCreationContext.addRemedialActionCreationContext(CseRemedialActionCreationContext.notImported(tRemedialAction, ImportStatus.INCOMPLETE_DATA, "CSE CRAC creation parameters is missing or does not contain information for the switches to open/close"));
            return;
        }

        BusBarChangeSwitches busBarChangeSwitches = cseCracCreationParameters.getBusBarChangeSwitches(raId);

        NetworkActionAdder networkActionAdder = crac.newNetworkAction()
            .withId(raId)
            .withOperator(tRemedialAction.getOperator().getV());
        try {
            busBarChangeSwitches.getSwitchPairs().forEach(switchPairId -> {
                assertIsSwitch(switchPairId.getSwitchToOpenId());
                assertIsSwitch(switchPairId.getSwitchToCloseId());
                networkActionAdder.newSwitchPair()
                    .withSwitchToOpen(switchPairId.getSwitchToOpenId())
                    .withSwitchToClose(switchPairId.getSwitchToCloseId())
                    .add();
            });
        } catch (FaraoException e) {
            cseCracCreationContext.addRemedialActionCreationContext(CseRemedialActionCreationContext.notImported(tRemedialAction, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, e.getMessage()));
            return;
        }
        addUsageRules(networkActionAdder, tRemedialAction);
        networkActionAdder.add();
        cseCracCreationContext.addRemedialActionCreationContext(CseRemedialActionCreationContext.imported(tRemedialAction, raId, false, null));
    }

    private void assertIsSwitch(String switchId) {
        UcteTopologicalElementHelper topoHelper = new UcteTopologicalElementHelper(switchId, ucteNetworkAnalyzer);
        if (!topoHelper.isValid()) {
            throw new FaraoException(topoHelper.getInvalidReason());
        }
        if (network.getSwitch(topoHelper.getIdInNetwork()) == null) {
            throw new FaraoException(String.format("%s is not a switch", switchId));
        }
    }
}
