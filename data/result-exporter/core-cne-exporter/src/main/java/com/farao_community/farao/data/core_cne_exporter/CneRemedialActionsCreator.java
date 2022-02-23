/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.core_cne_exporter;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.farao_community.farao.data.core_cne_exporter.xsd.ConstraintSeries;
import com.farao_community.farao.data.core_cne_exporter.xsd.ContingencySeries;
import com.farao_community.farao.data.core_cne_exporter.xsd.RemedialActionRegisteredResource;
import com.farao_community.farao.data.core_cne_exporter.xsd.RemedialActionSeries;
import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.PstRangeActionCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.RemedialActionCreationContext;
import com.farao_community.farao.data.rao_result_api.OptimizationState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.core_cne_exporter.CneClassCreator.*;
import static com.farao_community.farao.data.core_cne_exporter.CneConstants.*;
import static com.farao_community.farao.data.core_cne_exporter.CneUtil.cutString;
import static com.farao_community.farao.data.core_cne_exporter.CneUtil.randomizeString;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class CneRemedialActionsCreator {

    private CneHelper cneHelper;
    private List<ConstraintSeries> cnecsConstraintSeries;

    private static final String RA_SERIES = "RAseries";

    public CneRemedialActionsCreator(CneHelper cneHelper, List<ConstraintSeries> cnecsConstraintSeries) {
        this.cneHelper = cneHelper;
        this.cnecsConstraintSeries = new ArrayList<>(cnecsConstraintSeries);
    }

    private CneRemedialActionsCreator() {

    }

    /**
     * Creates RA ConstraintSeries for all RAs (B56) and adds them to the list
     * PS: This also adds the RemedialActionSeries to the CNECs' ConstraintSeries in the list,
     * so it should be done after adding the CNECs' ConstraintSeries to the list
     * @return a List of ConstraintSeries
     */
    public List<ConstraintSeries> generate() {
        List<ConstraintSeries> constraintSeries = new ArrayList<>();

        List<PstRangeAction> sortedRangeActions = cneHelper.getCracCreationContext().getRemedialActionCreationContexts().stream()
            .sorted(Comparator.comparing(RemedialActionCreationContext::getNativeId))
            .map(raCreationContext -> cneHelper.getCrac().getPstRangeAction(raCreationContext.getCreatedRAId()))
            .filter(ra -> !Objects.isNull(ra))
            .collect(Collectors.toList());
        List<NetworkAction> sortedNetworkActions = cneHelper.getCracCreationContext().getRemedialActionCreationContexts().stream()
            .sorted(Comparator.comparing(RemedialActionCreationContext::getNativeId))
            .map(raCreationContext -> cneHelper.getCrac().getNetworkAction(raCreationContext.getCreatedRAId()))
            .filter(ra -> !Objects.isNull(ra))
            .collect(Collectors.toList());

        logMissingRangeActions();
        List<PstRangeAction> usedRangeActions = sortedRangeActions.stream().filter(this::isRangeActionUsedInRao).collect(Collectors.toList());
        if (!usedRangeActions.isEmpty()) {
            constraintSeries.add(createPreOptimRaConstraintSeries(usedRangeActions));
        }
        ConstraintSeries postPraB56 = createPostPraRaConstraintSeries(sortedRangeActions, sortedNetworkActions);
        if (!postPraB56.getRemedialActionSeries().isEmpty()) {
            constraintSeries.add(postPraB56);
        }
        constraintSeries.addAll(createPostCraRaConstraintSeries(sortedRangeActions, sortedNetworkActions));

        return constraintSeries;
    }

    private void logMissingRangeActions() {
        cneHelper.getCracCreationContext().getRemedialActionCreationContexts().forEach(remedialActionCreationContext -> {
            if (!remedialActionCreationContext.isImported()) {
                FaraoLoggerProvider.TECHNICAL_LOGS.warn("Remedial action {} was not imported into the RAO, it will be absent from the CNE file", remedialActionCreationContext.getNativeId());
            }
        });
    }

    private ConstraintSeries createPreOptimRaConstraintSeries(List<PstRangeAction> sortedRangeActions) {
        ConstraintSeries preOptimB56 = newConstraintSeries(randomizeString(RA_SERIES, 20), B56_BUSINESS_TYPE);
        sortedRangeActions.forEach(rangeAction -> preOptimB56.getRemedialActionSeries().add(createPreOptimRangeRemedialActionSeries(rangeAction)));
        return preOptimB56;
    }

    private boolean isRangeActionUsedInRao(PstRangeAction pstRangeAction) {
        return cneHelper.getCrac().getStates().stream().anyMatch(state -> cneHelper.getRaoResult().isActivatedDuringState(state, pstRangeAction));
    }

    private RemedialActionSeries createPreOptimRangeRemedialActionSeries(PstRangeAction pstRangeAction) {
        PstRangeActionCreationContext context = (PstRangeActionCreationContext) cneHelper.getCracCreationContext().getRemedialActionCreationContexts().stream().filter(raContext -> pstRangeAction.getId().equals(raContext.getCreatedRAId())).findFirst().orElseThrow();
        int initialTap = (context.isInverted() ? -1 : 1) * pstRangeAction.getInitialTap();
        RemedialActionSeries remedialActionSeries = createB56RemedialActionSeries(pstRangeAction.getId(), pstRangeAction.getName(), pstRangeAction.getOperator(), OptimizationState.INITIAL);
        pstRangeAction.getNetworkElements().forEach(networkElement -> {
            RemedialActionRegisteredResource registeredResource = newRemedialActionRegisteredResource(context.getNativeId(), context.getNativeNetworkElementId(),
                    PST_RANGE_PSR_TYPE, initialTap, WITHOUT_UNIT_SYMBOL, ABSOLUTE_MARKET_OBJECT_STATUS);
            remedialActionSeries.getRegisteredResource().add(registeredResource);
            remedialActionSeries.setMRID(createRangeActionId(remedialActionSeries.getMRID()));
        });
        return remedialActionSeries;
    }

    private ConstraintSeries createPostPraRaConstraintSeries(List<PstRangeAction> sortedRangeActions, List<NetworkAction> sortedNetworkActions) {
        ConstraintSeries preventiveB56 = newConstraintSeries(randomizeString(RA_SERIES, 20), B56_BUSINESS_TYPE);
        sortedRangeActions.forEach(rangeAction -> createPostOptimPstRangeActionSeries(rangeAction, OptimizationState.AFTER_PRA, cneHelper.getCrac().getPreventiveState(), preventiveB56));
        sortedNetworkActions.forEach(networkAction -> createPostOptimNetworkRemedialActionSeries(networkAction, OptimizationState.AFTER_PRA, cneHelper.getCrac().getPreventiveState(), preventiveB56));

        // Add the remedial action series to B54 and B57
        List<ConstraintSeries> basecaseConstraintSeriesList = cnecsConstraintSeries.stream()
                .filter(constraintSeries -> constraintSeries.getBusinessType().equals(B54_BUSINESS_TYPE) || constraintSeries.getBusinessType().equals(B57_BUSINESS_TYPE))
                .collect(Collectors.toList());
        addRemedialActionsToOtherConstraintSeries(preventiveB56.getRemedialActionSeries(), basecaseConstraintSeriesList);
        return preventiveB56;
    }

    private List<ConstraintSeries> createPostCraRaConstraintSeries(List<PstRangeAction> sortedRangeActions, List<NetworkAction> sortedNetworkActions) {
        List<ConstraintSeries> constraintSeriesList = new ArrayList<>();
        cneHelper.getCrac().getContingencies().stream().sorted(Comparator.comparing(Identifiable::getId)).forEach(contingency -> {
            State curativeState = cneHelper.getCrac().getState(contingency.getId(), Instant.CURATIVE);
            if (curativeState == null) {
                return;
            }
            ConstraintSeries curativeB56 = newConstraintSeries(randomizeString(RA_SERIES, 20), B56_BUSINESS_TYPE);
            ContingencySeries contingencySeries = newContingencySeries(contingency.getId(), contingency.getName());
            curativeB56.getContingencySeries().add(contingencySeries);
            sortedRangeActions.forEach(rangeAction -> createPostOptimPstRangeActionSeries(rangeAction, OptimizationState.AFTER_CRA, curativeState, curativeB56));
            sortedNetworkActions.forEach(networkAction -> createPostOptimNetworkRemedialActionSeries(networkAction, OptimizationState.AFTER_CRA, curativeState, curativeB56));
            if (!curativeB56.getRemedialActionSeries().isEmpty()) {
                // Add remedial actions to corresponding CNECs' B54
                List<ConstraintSeries> contingencyConstraintSeriesList = cnecsConstraintSeries.stream()
                        .filter(constraintSeries -> constraintSeries.getBusinessType().equals(B54_BUSINESS_TYPE)
                                && constraintSeries.getContingencySeries().stream().anyMatch(series -> series.getName().equals(contingency.getName())))
                        .collect(Collectors.toList());
                addRemedialActionsToOtherConstraintSeries(curativeB56.getRemedialActionSeries(), contingencyConstraintSeriesList);
                // Add B56 to document
                constraintSeriesList.add(curativeB56);
            }
        });
        return constraintSeriesList;
    }

    public void createPostOptimPstRangeActionSeries(PstRangeAction rangeAction, OptimizationState optimizationState, State state, ConstraintSeries constraintSeriesB56) {
        if (rangeAction.getUsageRules().stream().noneMatch(usageRule ->
                usageRule.getUsageMethod(state).equals(UsageMethod.AVAILABLE) || usageRule.getUsageMethod(state).equals(UsageMethod.FORCED)))  {
            return;
        }
        // using RaoResult.isActivatedDuringState may throw an exception
        // if the state was not optimized or if the Range action was filtered out
        // that's why we use getActivatedRangeActionsDuringState instead
        boolean isActivated = cneHelper.getRaoResult().getActivatedRangeActionsDuringState(state).contains(rangeAction);
        if (isActivated && !rangeAction.getNetworkElements().isEmpty()) {
            RemedialActionSeries remedialActionSeries = createB56RemedialActionSeries(rangeAction.getId(), rangeAction.getName(), rangeAction.getOperator(), optimizationState);
            createPstRangeActionRegisteredResource(rangeAction, state, remedialActionSeries);
            constraintSeriesB56.getRemedialActionSeries().add(remedialActionSeries);
        }
    }

    private RemedialActionSeries createB56RemedialActionSeries(String remedialActionId, String remedialActionName, String operator, OptimizationState optimizationState) {
        String marketObjectStatus = null;
        switch (optimizationState) {
            case INITIAL:
                break;
            case AFTER_PRA:
                marketObjectStatus = PREVENTIVE_MARKET_OBJECT_STATUS;
                break;
            case AFTER_CRA:
                marketObjectStatus = CURATIVE_MARKET_OBJECT_STATUS;
                break;
            default:
                throw new FaraoException("Unknown CNE state");
        }

        RemedialActionSeries remedialActionSeries = newRemedialActionSeries(remedialActionId, remedialActionName, marketObjectStatus);

        try {
            if (!Objects.isNull(operator)) {
                remedialActionSeries.getPartyMarketParticipant().add(newPartyMarketParticipant(TsoEICode.fromShortId(operator).getEICode()));
            }
        } catch (IllegalArgumentException e) {
            FaraoLoggerProvider.TECHNICAL_LOGS.warn(String.format("Operator %s is not a country id.", operator));
        }

        return remedialActionSeries;
    }

    private void fillB56ConstraintSeries(String remedialActionId, String remedialActionName, String operator, OptimizationState optimizationState, ConstraintSeries constraintSeries) {
        RemedialActionSeries remedialActionSeries = createB56RemedialActionSeries(remedialActionId, remedialActionName, operator, optimizationState);
        constraintSeries.getRemedialActionSeries().add(remedialActionSeries);
    }

    private void createPstRangeActionRegisteredResource(PstRangeAction pstRangeAction, State state, RemedialActionSeries remedialActionSeries) {
        PstRangeActionCreationContext context = (PstRangeActionCreationContext) cneHelper.getCracCreationContext().getRemedialActionCreationContexts().stream().filter(raContext -> pstRangeAction.getId().equals(raContext.getCreatedRAId())).findFirst().orElseThrow();
        int tap = (context.isInverted() ? -1 : 1) * cneHelper.getRaoResult().getOptimizedTapOnState(state, pstRangeAction);
        RemedialActionRegisteredResource registeredResource = newRemedialActionRegisteredResource(context.getNativeId(), context.getNativeNetworkElementId(), PST_RANGE_PSR_TYPE, tap, WITHOUT_UNIT_SYMBOL, ABSOLUTE_MARKET_OBJECT_STATUS);
        remedialActionSeries.getRegisteredResource().add(registeredResource);
        remedialActionSeries.setMRID(createRangeActionId(remedialActionSeries.getMRID()));
    }

    private String createRangeActionId(String mRid) {
        return cutString(mRid, 55);
    }

    public void createPostOptimNetworkRemedialActionSeries(NetworkAction networkAction, OptimizationState optimizationState, State state, ConstraintSeries constraintSeriesB56) {
        if (networkAction.getUsageRules().stream().noneMatch(usageRule ->
                usageRule.getUsageMethod(state).equals(UsageMethod.AVAILABLE) || usageRule.getUsageMethod(state).equals(UsageMethod.FORCED)))  {
            return;
        }
        // using RaoResult.isActivatedDuringState may throw an exception
        // if the state was not optimized
        // that's why we use getActivatedNetworkActionsDuringState instead
        boolean isActivated = cneHelper.getRaoResult().getActivatedNetworkActionsDuringState(state).contains(networkAction);
        if (isActivated && !networkAction.getNetworkElements().isEmpty()) {
            fillB56ConstraintSeries(networkAction.getId(), networkAction.getName(), networkAction.getOperator(), optimizationState, constraintSeriesB56);
        }
    }

    public void addRemedialActionsToOtherConstraintSeries(List<RemedialActionSeries> remedialActionSeriesList, List<ConstraintSeries> constraintSeriesList) {
        remedialActionSeriesList.forEach(remedialActionSeries -> {
            RemedialActionSeries shortPostOptimRemedialActionSeries = newRemedialActionSeries(remedialActionSeries.getMRID(), remedialActionSeries.getName(), remedialActionSeries.getApplicationModeMarketObjectStatusStatus());
            constraintSeriesList.stream().forEach(constraintSeries -> constraintSeries.getRemedialActionSeries().add(shortPostOptimRemedialActionSeries));
        });
    }
}
