/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.corecneexporter;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TsoEICode;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.cneexportercommons.CneHelper;
import com.powsybl.openrao.data.corecneexporter.xsd.ConstraintSeries;
import com.powsybl.openrao.data.corecneexporter.xsd.ContingencySeries;
import com.powsybl.openrao.data.corecneexporter.xsd.RemedialActionRegisteredResource;
import com.powsybl.openrao.data.corecneexporter.xsd.RemedialActionSeries;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracio.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.PstRangeActionCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.UcteCracCreationContext;

import java.util.*;

import static com.powsybl.openrao.data.cneexportercommons.CneConstants.*;
import static com.powsybl.openrao.data.cneexportercommons.CneUtil.cutString;
import static com.powsybl.openrao.data.cneexportercommons.CneUtil.randomizeString;
import static com.powsybl.openrao.data.corecneexporter.CoreCneClassCreator.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class CoreCneRemedialActionsCreator {

    private CneHelper cneHelper;
    private UcteCracCreationContext cracCreationContext;
    private List<ConstraintSeries> cnecsConstraintSeries;

    private static final String RA_SERIES = "RAseries";

    public CoreCneRemedialActionsCreator(CneHelper cneHelper, UcteCracCreationContext cracCreationContext, List<ConstraintSeries> cnecsConstraintSeries) {
        this.cneHelper = cneHelper;
        this.cnecsConstraintSeries = new ArrayList<>(cnecsConstraintSeries);
        this.cracCreationContext = cracCreationContext;
    }

    private CoreCneRemedialActionsCreator() {

    }

    /**
     * Creates RA ConstraintSeries for all RAs (B56) and adds them to the list
     * PS: This also adds the RemedialActionSeries to the CNECs' ConstraintSeries in the list,
     * so it should be done after adding the CNECs' ConstraintSeries to the list
     *
     * @return a List of ConstraintSeries
     */
    public List<ConstraintSeries> generate() {
        List<ConstraintSeries> constraintSeries = new ArrayList<>();

        List<PstRangeAction> sortedRangeActions = cracCreationContext.getRemedialActionCreationContexts().stream()
                .sorted(Comparator.comparing(ElementaryCreationContext::getNativeObjectId))
                .map(raCreationContext -> cneHelper.getCrac().getPstRangeAction(raCreationContext.getCreatedObjectId()))
                .filter(ra -> !Objects.isNull(ra))
                .toList();
        List<NetworkAction> sortedNetworkActions = cracCreationContext.getRemedialActionCreationContexts().stream()
                .sorted(Comparator.comparing(ElementaryCreationContext::getNativeObjectId))
                .map(raCreationContext -> cneHelper.getCrac().getNetworkAction(raCreationContext.getCreatedObjectId()))
                .filter(ra -> !Objects.isNull(ra))
                .toList();

        logMissingRangeActions();
        List<PstRangeAction> usedRangeActions = sortedRangeActions.stream().filter(this::isRangeActionUsedInRao).toList();
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
        cracCreationContext.getRemedialActionCreationContexts().forEach(remedialActionCreationContext -> {
            if (!remedialActionCreationContext.isImported()) {
                OpenRaoLoggerProvider.TECHNICAL_LOGS.warn("Remedial action {} was not imported into the RAO, it will be absent from the CNE file", remedialActionCreationContext.getNativeObjectId());
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
        PstRangeActionCreationContext context = (PstRangeActionCreationContext) cracCreationContext.getRemedialActionCreationContexts().stream().filter(raContext -> pstRangeAction.getId().equals(raContext.getCreatedObjectId())).findFirst().orElseThrow();
        int initialTap = (context.isInverted() ? -1 : 1) * pstRangeAction.getInitialTap();
        RemedialActionSeries remedialActionSeries = createB56RemedialActionSeries(pstRangeAction.getId(), pstRangeAction.getName(), pstRangeAction.getOperator(), null);
        pstRangeAction.getNetworkElements().forEach(networkElement -> {
            RemedialActionRegisteredResource registeredResource = newRemedialActionRegisteredResource(context.getNativeObjectId(), context.getNativeNetworkElementId(),
                    PST_RANGE_PSR_TYPE, initialTap, WITHOUT_UNIT_SYMBOL, ABSOLUTE_MARKET_OBJECT_STATUS);
            remedialActionSeries.getRegisteredResource().add(registeredResource);
            remedialActionSeries.setMRID(createRangeActionId(remedialActionSeries.getMRID()));
        });
        return remedialActionSeries;
    }

    private ConstraintSeries createPostPraRaConstraintSeries(List<PstRangeAction> sortedRangeActions, List<NetworkAction> sortedNetworkActions) {
        ConstraintSeries preventiveB56 = newConstraintSeries(randomizeString(RA_SERIES, 20), B56_BUSINESS_TYPE);
        sortedRangeActions.forEach(rangeAction -> createPostOptimPstRangeActionSeries(rangeAction, InstantKind.PREVENTIVE, cneHelper.getCrac().getPreventiveState(), preventiveB56));
        sortedNetworkActions.forEach(networkAction -> createPostOptimNetworkRemedialActionSeries(networkAction, InstantKind.PREVENTIVE, cneHelper.getCrac().getPreventiveState(), preventiveB56));

        // Add the remedial action series to B54 and B57
        List<ConstraintSeries> basecaseConstraintSeriesList = cnecsConstraintSeries.stream()
                .filter(constraintSeries -> constraintSeries.getBusinessType().equals(B54_BUSINESS_TYPE) || constraintSeries.getBusinessType().equals(B57_BUSINESS_TYPE))
                .toList();
        addRemedialActionsToOtherConstraintSeries(preventiveB56.getRemedialActionSeries(), basecaseConstraintSeriesList);
        return preventiveB56;
    }

    private List<ConstraintSeries> createPostCraRaConstraintSeries(List<PstRangeAction> sortedRangeActions, List<NetworkAction> sortedNetworkActions) {
        List<ConstraintSeries> constraintSeriesList = new ArrayList<>();
        cneHelper.getCrac().getContingencies().stream().sorted(Comparator.comparing(Contingency::getId)).forEach(contingency -> {
            State curativeState = cneHelper.getCrac().getState(contingency.getId(), cneHelper.getCrac().getInstant(InstantKind.CURATIVE));
            if (curativeState == null) {
                return;
            }
            ConstraintSeries curativeB56 = newConstraintSeries(randomizeString(RA_SERIES, 20), B56_BUSINESS_TYPE);
            ContingencySeries contingencySeries = newContingencySeries(contingency.getId(), contingency.getName().orElse(contingency.getId()));
            curativeB56.getContingencySeries().add(contingencySeries);
            sortedRangeActions.forEach(rangeAction -> createPostOptimPstRangeActionSeries(rangeAction, InstantKind.CURATIVE, curativeState, curativeB56));
            sortedNetworkActions.forEach(networkAction -> createPostOptimNetworkRemedialActionSeries(networkAction, InstantKind.CURATIVE, curativeState, curativeB56));
            if (!curativeB56.getRemedialActionSeries().isEmpty()) {
                // Add remedial actions to corresponding CNECs' B54
                List<ConstraintSeries> contingencyConstraintSeriesList = cnecsConstraintSeries.stream()
                        .filter(constraintSeries -> constraintSeries.getBusinessType().equals(B54_BUSINESS_TYPE)
                                && constraintSeries.getContingencySeries().stream().anyMatch(series -> series.getName().equals(contingency.getName().orElse(contingency.getId()))))
                        .toList();
                addRemedialActionsToOtherConstraintSeries(curativeB56.getRemedialActionSeries(), contingencyConstraintSeriesList);
                // Add B56 to document
                constraintSeriesList.add(curativeB56);
            }
        });
        return constraintSeriesList;
    }

    public void createPostOptimPstRangeActionSeries(PstRangeAction rangeAction, InstantKind optimizedInstantKind, State state, ConstraintSeries constraintSeriesB56) {
        if (rangeAction.getUsageRules().stream().noneMatch(usageRule ->
                usageRule.getUsageMethod(state).equals(UsageMethod.AVAILABLE) || usageRule.getUsageMethod(state).equals(UsageMethod.FORCED))) {
            return;
        }
        // using RaoResult.isActivatedDuringState may throw an exception
        // if the state was not optimized or if the Range action was filtered out
        // that's why we use getActivatedRangeActionsDuringState instead
        boolean isActivated = cneHelper.getRaoResult().getActivatedRangeActionsDuringState(state).contains(rangeAction);
        if (isActivated && !rangeAction.getNetworkElements().isEmpty()) {
            RemedialActionSeries remedialActionSeries = createB56RemedialActionSeries(rangeAction.getId(), rangeAction.getName(), rangeAction.getOperator(), optimizedInstantKind);
            createPstRangeActionRegisteredResource(rangeAction, state, remedialActionSeries);
            constraintSeriesB56.getRemedialActionSeries().add(remedialActionSeries);
        }
    }

    private RemedialActionSeries createB56RemedialActionSeries(String remedialActionId, String remedialActionName, String operator, InstantKind optimizedInstantKind) {
        String marketObjectStatus = null;
        if (optimizedInstantKind != null) {
            switch (optimizedInstantKind) {
                case PREVENTIVE:
                    marketObjectStatus = PREVENTIVE_MARKET_OBJECT_STATUS;
                    break;
                case CURATIVE:
                    marketObjectStatus = CURATIVE_MARKET_OBJECT_STATUS;
                    break;
                default:
                    throw new OpenRaoException("Unknown CNE state");
            }
        }

        RemedialActionSeries remedialActionSeries = newRemedialActionSeries(remedialActionId, remedialActionName, marketObjectStatus);

        try {
            if (!Objects.isNull(operator)) {
                remedialActionSeries.getPartyMarketParticipant().add(newPartyMarketParticipant(TsoEICode.fromShortId(operator).getEICode()));
            }
        } catch (IllegalArgumentException e) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.warn(String.format("Operator %s is not a country id.", operator));
        }

        return remedialActionSeries;
    }

    private void fillB56ConstraintSeries(String remedialActionId, String remedialActionName, String operator, InstantKind optimizedInstantKind, ConstraintSeries constraintSeries) {
        RemedialActionSeries remedialActionSeries = createB56RemedialActionSeries(remedialActionId, remedialActionName, operator, optimizedInstantKind);
        constraintSeries.getRemedialActionSeries().add(remedialActionSeries);
    }

    private void createPstRangeActionRegisteredResource(PstRangeAction pstRangeAction, State state, RemedialActionSeries remedialActionSeries) {
        PstRangeActionCreationContext context = (PstRangeActionCreationContext) cracCreationContext.getRemedialActionCreationContexts().stream().filter(raContext -> pstRangeAction.getId().equals(raContext.getCreatedObjectId())).findFirst().orElseThrow();
        int tap = (context.isInverted() ? -1 : 1) * cneHelper.getRaoResult().getOptimizedTapOnState(state, pstRangeAction);
        RemedialActionRegisteredResource registeredResource = newRemedialActionRegisteredResource(context.getNativeObjectId(), context.getNativeNetworkElementId(), PST_RANGE_PSR_TYPE, tap, WITHOUT_UNIT_SYMBOL, ABSOLUTE_MARKET_OBJECT_STATUS);
        remedialActionSeries.getRegisteredResource().add(registeredResource);
        remedialActionSeries.setMRID(createRangeActionId(remedialActionSeries.getMRID()));
    }

    private String createRangeActionId(String mRid) {
        return cutString(mRid, 55);
    }

    public void createPostOptimNetworkRemedialActionSeries(NetworkAction networkAction, InstantKind optimizedInstantKind, State state, ConstraintSeries constraintSeriesB56) {
        if (networkAction.getUsageRules().stream().noneMatch(usageRule ->
                usageRule.getUsageMethod(state).equals(UsageMethod.AVAILABLE) || usageRule.getUsageMethod(state).equals(UsageMethod.FORCED))) {
            return;
        }
        // using RaoResult.isActivatedDuringState may throw an exception
        // if the state was not optimized
        // that's why we use getActivatedNetworkActionsDuringState instead
        boolean isActivated = cneHelper.getRaoResult().getActivatedNetworkActionsDuringState(state).contains(networkAction);
        if (isActivated && !networkAction.getNetworkElements().isEmpty()) {
            fillB56ConstraintSeries(networkAction.getId(), networkAction.getName(), networkAction.getOperator(), optimizedInstantKind, constraintSeriesB56);
        }
    }

    public void addRemedialActionsToOtherConstraintSeries(List<RemedialActionSeries> remedialActionSeriesList, List<ConstraintSeries> constraintSeriesList) {
        remedialActionSeriesList.forEach(remedialActionSeries -> {
            RemedialActionSeries shortPostOptimRemedialActionSeries = newRemedialActionSeries(remedialActionSeries.getMRID(), remedialActionSeries.getName(), remedialActionSeries.getApplicationModeMarketObjectStatusStatus());
            constraintSeriesList.stream().forEach(constraintSeries -> constraintSeries.getRemedialActionSeries().add(shortPostOptimRemedialActionSeries));
        });
    }
}
