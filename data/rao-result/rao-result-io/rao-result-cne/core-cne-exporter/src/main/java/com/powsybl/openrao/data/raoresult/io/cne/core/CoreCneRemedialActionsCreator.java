/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.cne.core;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TsoEICode;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.io.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.stdcreationcontext.PstRangeActionCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.stdcreationcontext.UcteCracCreationContext;
import com.powsybl.openrao.data.raoresult.io.cne.commons.CneHelper;
import com.powsybl.openrao.data.raoresult.io.cne.core.xsd.ConstraintSeries;
import com.powsybl.openrao.data.raoresult.io.cne.core.xsd.ContingencySeries;
import com.powsybl.openrao.data.raoresult.io.cne.core.xsd.RemedialActionRegisteredResource;
import com.powsybl.openrao.data.raoresult.io.cne.core.xsd.RemedialActionSeries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.ABSOLUTE_MARKET_OBJECT_STATUS;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.B54_BUSINESS_TYPE;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.B56_BUSINESS_TYPE;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.B57_BUSINESS_TYPE;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.CURATIVE_MARKET_OBJECT_STATUS;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.DC_LINK_PSR_TYPE;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.MEGAWATTS_UNIT_SYMBOL;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.PREVENTIVE_MARKET_OBJECT_STATUS;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.PST_RANGE_PSR_TYPE;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.WITHOUT_UNIT_SYMBOL;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneUtil.cutString;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneUtil.randomizeString;
import static com.powsybl.openrao.data.raoresult.io.cne.core.CoreCneClassCreator.newConstraintSeries;
import static com.powsybl.openrao.data.raoresult.io.cne.core.CoreCneClassCreator.newContingencySeries;
import static com.powsybl.openrao.data.raoresult.io.cne.core.CoreCneClassCreator.newPartyMarketParticipant;
import static com.powsybl.openrao.data.raoresult.io.cne.core.CoreCneClassCreator.newRemedialActionRegisteredResource;
import static com.powsybl.openrao.data.raoresult.io.cne.core.CoreCneClassCreator.newRemedialActionSeries;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class CoreCneRemedialActionsCreator {

    private static final String SEPARATOR = " + ";
    private static final String RA_SERIES = "RAseries";

    private CneHelper cneHelper;
    private UcteCracCreationContext cracCreationContext;
    private List<ConstraintSeries> cnecsConstraintSeries;

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
        final List<ConstraintSeries> constraintSeries = new ArrayList<>();

        final Crac crac = cneHelper.getCrac();
        final List<InjectionRangeAction> sortedInjectionRangeActions = new ArrayList<>();
        final List<PstRangeAction> sortedPstRangeActions = new ArrayList<>();
        final List<NetworkAction> sortedNetworkActions = new ArrayList<>();
        getSortedNetworkAndRangeActions(crac, sortedInjectionRangeActions, sortedPstRangeActions, sortedNetworkActions);
        logMissingRangeActions();

        // PRE-OPTIM
        final List<PstRangeAction> usedPstRangeActions = sortedPstRangeActions.stream()
            .filter(this::isRangeActionUsedInRao)
            .toList();
        final List<InjectionRangeAction> usedInjectionRangeActions = sortedInjectionRangeActions.stream()
            .filter(this::isRangeActionUsedInRao)
            .toList();
        if (!usedPstRangeActions.isEmpty() || !usedInjectionRangeActions.isEmpty()) {
            constraintSeries.add(createPreOptimRaConstraintSeries(usedPstRangeActions, usedInjectionRangeActions));
        }

        // POST-PRA
        // TODO Ajouter les InjectionRangeAction dans le traitement de la méthode
        // TODO Il semble nécessaire d'avoir un objet Network à disposition pour récupérer le setpoint, en passant par injectionRangeAction.getCurrentSetpoint() ou par le targetP du générateur fictif
        //  injectionRangeAction.getCurrentSetpoint(network) * injectionRangeAction.getInjectionDistributionKeys().get(networkElement)
        final ConstraintSeries postPraB56 = createPostPraRaConstraintSeries(sortedPstRangeActions, sortedNetworkActions);
        if (!postPraB56.getRemedialActionSeries().isEmpty()) {
            constraintSeries.add(postPraB56);
        }

        // POST-CRA
        // TODO Ajouter les InjectionRangeAction dans le traitement de la méthode
        constraintSeries.addAll(createPostCraRaConstraintSeries(sortedPstRangeActions, sortedNetworkActions));

        return constraintSeries;
    }

    private void getSortedNetworkAndRangeActions(final Crac crac,
                                                 final List<InjectionRangeAction> sortedInjectionRangeActions,
                                                 final List<PstRangeAction> sortedPstRangeActions,
                                                 final List<NetworkAction> sortedNetworkActions) {
        cracCreationContext.getRemedialActionCreationContexts().stream()
            .sorted(Comparator.comparing(ElementaryCreationContext::getNativeObjectId))
            .forEach(raCreationContext -> {
                final InjectionRangeAction injectionRangeAction = crac.getInjectionRangeAction(raCreationContext.getCreatedObjectId());
                if (injectionRangeAction != null) {
                    sortedInjectionRangeActions.add(injectionRangeAction);
                }
                final PstRangeAction pstRangeAction = crac.getPstRangeAction(raCreationContext.getCreatedObjectId());
                if (pstRangeAction != null) {
                    sortedPstRangeActions.add(pstRangeAction);
                }
                final NetworkAction networkAction = crac.getNetworkAction(raCreationContext.getCreatedObjectId());
                if (networkAction != null) {
                    sortedNetworkActions.add(networkAction);
                }
            });
    }

    private void logMissingRangeActions() {
        cracCreationContext.getRemedialActionCreationContexts().forEach(remedialActionCreationContext -> {
            if (!remedialActionCreationContext.isImported()) {
                OpenRaoLoggerProvider.TECHNICAL_LOGS.warn("Remedial action {} was not imported into the RAO, it will be absent from the CNE file", remedialActionCreationContext.getNativeObjectId());
            }
        });
    }

    private boolean isRangeActionUsedInRao(RangeAction<?> rangeAction) {
        return cneHelper.getCrac().getStates().stream()
            .anyMatch(state -> cneHelper.getRaoResult().isActivatedDuringState(state, rangeAction));
    }

    private ConstraintSeries createPreOptimRaConstraintSeries(final List<PstRangeAction> pstRangeActions,
                                                              final List<InjectionRangeAction> injectionRangeActions) {
        final ConstraintSeries preOptimB56 = newConstraintSeries(randomizeString(RA_SERIES, 20), B56_BUSINESS_TYPE);
        final List<RemedialActionSeries> remedialActionSeriesList = preOptimB56.getRemedialActionSeries();

        pstRangeActions.stream()
            .map(this::createPreOptimRangeRemedialActionSeries)
            .forEach(remedialActionSeriesList::add);

        // For injectionRangeAction representing HVDC lines, we must separate the data from both "from" and "to" complex variants
        // so the createPreOptimRangeRemedialActionSeries() method returns a list of two elements
        injectionRangeActions.stream()
            .map(this::createPreOptimRangeRemedialActionSeries)
            .forEach(remedialActionSeriesList::addAll);

        return preOptimB56;
    }

    private RemedialActionSeries createPreOptimRangeRemedialActionSeries(PstRangeAction pstRangeAction) {
        final PstRangeActionCreationContext context = (PstRangeActionCreationContext) cracCreationContext.getRemedialActionCreationContexts().stream()
            .filter(raContext -> pstRangeAction.getId().equals(raContext.getCreatedObjectId()))
            .findFirst().orElseThrow();
        final int initialTap = (context.isInverted() ? -1 : 1) * pstRangeAction.getInitialTap();

        final RemedialActionSeries remedialActionSeries = createB56RemedialActionSeries(pstRangeAction.getId(), pstRangeAction.getName(), pstRangeAction.getOperator(), null);
        pstRangeAction.getNetworkElements().forEach(networkElement -> {
            final RemedialActionRegisteredResource registeredResource = newRemedialActionRegisteredResource(
                context.getNativeObjectId(), context.getNativeNetworkElementId(),
                PST_RANGE_PSR_TYPE, initialTap, WITHOUT_UNIT_SYMBOL, ABSOLUTE_MARKET_OBJECT_STATUS
            );
            remedialActionSeries.getRegisteredResource().add(registeredResource);
            remedialActionSeries.setMRID(createRangeActionId(remedialActionSeries.getMRID()));
        });
        return remedialActionSeries;
    }

    private List<RemedialActionSeries> createPreOptimRangeRemedialActionSeries(final InjectionRangeAction injectionRangeAction) {
        if (!injectionRangeAction.getId().contains(SEPARATOR)
            || !injectionRangeAction.getName().contains(SEPARATOR)
            || !injectionRangeAction.getOperator().contains(SEPARATOR)
            || injectionRangeAction.getNetworkElements().size() != 2) {
            // In Core CC, the only elements that are currently modeled with injectionRangeAction objects are HVDC lines.
            // An injectionRangeAction that does not match the expected format for HVDC lines is not supposed to exist,
            // so if we find one then we should not add it to the CNE
            return List.of();
        }

        // First part of id/name/operator is "from", the second part is "to"
        final String[] raIds = injectionRangeAction.getId().split(SEPARATOR);
        final String[] raNames = injectionRangeAction.getName().split(SEPARATOR);
        final String[] raOperators = injectionRangeAction.getOperator().split(SEPARATOR);
        // NetworkElements are sorted by distribution key : -1 is "from" element, 1 is "to" element
        final String[] networkElementNames = injectionRangeAction.getInjectionDistributionKeys().entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .map(Identifiable::getName)
            .toArray(String[]::new);

        final RemedialActionSeries remedialActionSeriesFrom = getRemedialActionSeries(raIds[0], raNames[0], raOperators[0], null, networkElementNames[0], injectionRangeAction.getInitialSetpoint(), -1);
        final RemedialActionSeries remedialActionSeriesTo = getRemedialActionSeries(raIds[1], raNames[1], raOperators[1], null, networkElementNames[1], injectionRangeAction.getInitialSetpoint(), 1);

        return List.of(remedialActionSeriesFrom, remedialActionSeriesTo);
    }

    private RemedialActionSeries getRemedialActionSeries(final String raId,
                                                         final String raName,
                                                         final String raOperator,
                                                         final InstantKind instantKind,
                                                         final String networkElementName,
                                                         final Double setpoint,
                                                         final double distributionKey) {
        final RemedialActionSeries remedialActionSeries = createB56RemedialActionSeries(raId, raName, raOperator, instantKind);
        final RemedialActionRegisteredResource registeredResource = newRemedialActionRegisteredResource(
            raId,
            networkElementName,
            DC_LINK_PSR_TYPE,
            setpoint * distributionKey,
            MEGAWATTS_UNIT_SYMBOL,
            ABSOLUTE_MARKET_OBJECT_STATUS
        );
        remedialActionSeries.getRegisteredResource().add(registeredResource);
        remedialActionSeries.setMRID(createRangeActionId(remedialActionSeries.getMRID()));
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
                constraintSeriesList.add(curativeB56); // Add B56 to document
            }
        });
        return constraintSeriesList;
    }

    public void createPostOptimPstRangeActionSeries(PstRangeAction rangeAction, InstantKind optimizedInstantKind, State state, ConstraintSeries constraintSeriesB56) {
        if (rangeAction.getUsageRules().stream().noneMatch(usageRule -> usageRule.isDefinedForState(state))) {
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
            marketObjectStatus = switch (optimizedInstantKind) {
                case PREVENTIVE -> PREVENTIVE_MARKET_OBJECT_STATUS;
                case CURATIVE -> CURATIVE_MARKET_OBJECT_STATUS;
                default -> throw new OpenRaoException("Unknown CNE state");
            };
        }

        final RemedialActionSeries remedialActionSeries = newRemedialActionSeries(remedialActionId, remedialActionName, marketObjectStatus);

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
        PstRangeActionCreationContext context = (PstRangeActionCreationContext) cracCreationContext.getRemedialActionCreationContexts().stream()
            .filter(raContext -> pstRangeAction.getId().equals(raContext.getCreatedObjectId()))
            .findFirst().orElseThrow();
        int tap = (context.isInverted() ? -1 : 1) * cneHelper.getRaoResult().getOptimizedTapOnState(state, pstRangeAction);
        RemedialActionRegisteredResource registeredResource = newRemedialActionRegisteredResource(
            context.getNativeObjectId(),
            context.getNativeNetworkElementId(),
            PST_RANGE_PSR_TYPE,
            tap,
            WITHOUT_UNIT_SYMBOL,
            ABSOLUTE_MARKET_OBJECT_STATUS
        );
        remedialActionSeries.getRegisteredResource().add(registeredResource);
        remedialActionSeries.setMRID(createRangeActionId(remedialActionSeries.getMRID()));
    }

    private String createRangeActionId(String mRid) {
        return cutString(mRid, 55);
    }

    public void createPostOptimNetworkRemedialActionSeries(NetworkAction networkAction, InstantKind optimizedInstantKind, State state, ConstraintSeries constraintSeriesB56) {
        if (networkAction.getUsageRules().stream().noneMatch(usageRule -> usageRule.isDefinedForState(state))) {
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
            RemedialActionSeries shortPostOptimRemedialActionSeries = newRemedialActionSeries(
                remedialActionSeries.getMRID(),
                remedialActionSeries.getName(),
                remedialActionSeries.getApplicationModeMarketObjectStatusStatus()
            );
            constraintSeriesList.forEach(constraintSeries -> constraintSeries.getRemedialActionSeries().add(shortPostOptimRemedialActionSeries));
        });
    }
}
