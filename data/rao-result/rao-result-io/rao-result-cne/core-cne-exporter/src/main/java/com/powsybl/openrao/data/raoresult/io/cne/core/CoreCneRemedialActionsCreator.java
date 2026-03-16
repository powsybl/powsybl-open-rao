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
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.RemedialAction;
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
import java.util.Set;
import java.util.stream.Collectors;

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
    private static final String SEPARATOR_REGEX = " \\+ ";
    private static final String RA_SERIES = "RAseries";

    private final CneHelper cneHelper;
    private final UcteCracCreationContext cracCreationContext;
    private final List<ConstraintSeries> cnecsConstraintSeries;

    public CoreCneRemedialActionsCreator(CneHelper cneHelper, UcteCracCreationContext cracCreationContext, List<ConstraintSeries> cnecsConstraintSeries) {
        this.cneHelper = cneHelper;
        this.cnecsConstraintSeries = new ArrayList<>(cnecsConstraintSeries);
        this.cracCreationContext = cracCreationContext;
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
        final List<PstRangeAction> sortedPstRangeActions = new ArrayList<>();
        final List<InjectionRangeAction> sortedInjectionRangeActions = new ArrayList<>();
        final List<NetworkAction> sortedNetworkActions = new ArrayList<>();
        getSortedPstRangeAndNetworkActions(crac, sortedPstRangeActions, sortedNetworkActions);
        getSortedInjectionRangeActions(crac, sortedInjectionRangeActions);
        logMissingRangeActions();

        // PRE-OPTIM: only one ConstraintSeries
        final ConstraintSeries preOptimB56 = createPreOptimRaConstraintSeries(sortedPstRangeActions, sortedInjectionRangeActions);
        if (!preOptimB56.getRemedialActionSeries().isEmpty()) {
            constraintSeries.add(preOptimB56);
        }

        // POST-PRA: only one ConstraintSeries
        final ConstraintSeries postPraB56 = createPostPraRaConstraintSeries(sortedPstRangeActions, sortedInjectionRangeActions, sortedNetworkActions);
        if (!postPraB56.getRemedialActionSeries().isEmpty()) {
            constraintSeries.add(postPraB56);
        }

        // POST-CRA: one ConstraintSeries for each contingency
        constraintSeries.addAll(createPostCraRaConstraintSeries(sortedPstRangeActions, sortedInjectionRangeActions, sortedNetworkActions));

        return constraintSeries;
    }

    private void getSortedPstRangeAndNetworkActions(final Crac crac,
                                                    final List<PstRangeAction> sortedPstRangeActions,
                                                    final List<NetworkAction> sortedNetworkActions) {
        cracCreationContext.getRemedialActionCreationContexts().stream()
            .sorted(Comparator.comparing(ElementaryCreationContext::getNativeObjectId))
            .forEach(raCreationContext -> {
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

    private void getSortedInjectionRangeActions(final Crac crac,
                                                final List<InjectionRangeAction> sortedInjectionRangeActions) {
        final Set<String> raCreationContextIds = cracCreationContext.getRemedialActionCreationContexts().stream()
            .map(ElementaryCreationContext::getCreatedObjectId)
            .collect(Collectors.toSet());

        // InjectionRangeActions in Core are composed of two elements
        // So we must ensure that the two elements composing the injection range action exist in the creation context
        // before adding the remedial action to the list
        crac.getInjectionRangeActions().stream()
            .filter(ira -> isInjectionRangeActionValid(ira, raCreationContextIds))
            .sorted(Comparator.comparing(InjectionRangeAction::getId))
            .forEach(sortedInjectionRangeActions::add);
    }

    private static boolean isInjectionRangeActionValid(final InjectionRangeAction ira,
                                                       final Set<String> raCreationContextIds) {
        if (ira.getId() == null) {
            return false;
        }

        final String[] splitRaId = ira.getId().split(SEPARATOR_REGEX);
        if (splitRaId.length != 2) {
            return false;
        }

        return raCreationContextIds.contains(splitRaId[0]) && raCreationContextIds.contains(splitRaId[1]);
    }

    private void logMissingRangeActions() {
        cracCreationContext.getRemedialActionCreationContexts().stream()
            .filter(remedialActionCreationContext -> !remedialActionCreationContext.isImported())
            .forEach(remedialActionCreationContext ->
                OpenRaoLoggerProvider.TECHNICAL_LOGS.warn(
                    "Remedial action {} was not imported into the RAO, it will be absent from the CNE file",
                    remedialActionCreationContext.getNativeObjectId()
                )
        );
    }

    // PRE-OPTIM

    private ConstraintSeries createPreOptimRaConstraintSeries(final List<PstRangeAction> pstRangeActions,
                                                              final List<InjectionRangeAction> injectionRangeActions) {
        final ConstraintSeries preOptimB56 = getNewB56RaConstraintSeries();
        final List<RemedialActionSeries> remedialActionSeriesList = preOptimB56.getRemedialActionSeries();

        pstRangeActions.stream()
            .filter(this::isRangeActionUsedInRao)
            .map(this::createPreOptimPstRangeActionSeries)
            .forEach(remedialActionSeriesList::add);

        // For injectionRangeAction representing HVDC lines, we must separate the data from both "from" and "to" complex variants
        // so the createPreOptimRangeRemedialActionSeries() method returns a list of two elements
        injectionRangeActions.stream()
            .filter(this::isRangeActionUsedInRao)
            .map(this::createPreOptimInjectionRangeActionSeries)
            .forEach(remedialActionSeriesList::addAll);

        return preOptimB56;
    }

    private boolean isRangeActionUsedInRao(final RangeAction<?> rangeAction) {
        return cneHelper.getCrac().getStates().stream()
            .anyMatch(state -> cneHelper.getRaoResult().isActivatedDuringState(state, rangeAction));
    }

    private RemedialActionSeries createPreOptimPstRangeActionSeries(final PstRangeAction rangeAction) {
        final int initialTap = rangeAction.getInitialTap();
        return createPstRangeActionSeries(rangeAction, null, initialTap);
    }

    private List<RemedialActionSeries> createPreOptimInjectionRangeActionSeries(final InjectionRangeAction rangeAction) {
        final Double setpoint = rangeAction.getInitialSetpoint();
        return createInjectionRangeActionSeries(rangeAction, null, setpoint);
    }

    // POST-PRA

    private ConstraintSeries createPostPraRaConstraintSeries(final List<PstRangeAction> pstRangeActions,
                                                             final List<InjectionRangeAction> injectionRangeActions,
                                                             final List<NetworkAction> sortedNetworkActions) {
        final State preventiveState = cneHelper.getCrac().getPreventiveState();
        final ConstraintSeries preventiveB56 = getNewB56RaConstraintSeries();
        final List<RemedialActionSeries> remedialActionSeriesList = preventiveB56.getRemedialActionSeries();

        createPostOptimPstRangeActionSeries(pstRangeActions, preventiveState, InstantKind.PREVENTIVE, remedialActionSeriesList);
        createPostOptimInjectionRangeActionSeries(injectionRangeActions, preventiveState, InstantKind.PREVENTIVE, remedialActionSeriesList);
        createPostOptimNetworkActionSeries(sortedNetworkActions, preventiveState, InstantKind.PREVENTIVE, remedialActionSeriesList);

        // Add the remedial action series to B54 and B57
        List<ConstraintSeries> basecaseConstraintSeriesList = cnecsConstraintSeries.stream()
            .filter(constraintSeries -> constraintSeries.getBusinessType().equals(B54_BUSINESS_TYPE) || constraintSeries.getBusinessType().equals(B57_BUSINESS_TYPE))
            .toList();
        addRemedialActionsToOtherConstraintSeries(remedialActionSeriesList, basecaseConstraintSeriesList);

        return preventiveB56;
    }

    // POST-CRA

    private List<ConstraintSeries> createPostCraRaConstraintSeries(final List<PstRangeAction> pstRangeActions,
                                                                   final List<InjectionRangeAction> injectionRangeActions,
                                                                   final List<NetworkAction> sortedNetworkActions) {
        List<ConstraintSeries> constraintSeriesList = new ArrayList<>();
        cneHelper.getCrac().getContingencies().stream()
            .sorted(Comparator.comparing(Contingency::getId))
            .forEach(contingency ->
                createPostCraRaConstraintSeriesForContingency(pstRangeActions, injectionRangeActions, sortedNetworkActions, contingency, constraintSeriesList)
        );
        return constraintSeriesList;
    }

    private void createPostCraRaConstraintSeriesForContingency(final List<PstRangeAction> pstRangeActions,
                                                               final List<InjectionRangeAction> injectionRangeActions,
                                                               final List<NetworkAction> sortedNetworkActions,
                                                               final Contingency contingency,
                                                               final List<ConstraintSeries> constraintSeriesList) {
        final State curativeState = cneHelper.getCrac().getState(contingency.getId(), cneHelper.getCrac().getInstant(InstantKind.CURATIVE));
        if (curativeState == null) {
            return;
        }
        final ConstraintSeries curativeB56 = getNewB56RaConstraintSeries();

        final List<RemedialActionSeries> remedialActionSeriesList = curativeB56.getRemedialActionSeries();
        createPostOptimPstRangeActionSeries(pstRangeActions, curativeState, InstantKind.CURATIVE, remedialActionSeriesList);
        createPostOptimInjectionRangeActionSeries(injectionRangeActions, curativeState, InstantKind.CURATIVE, remedialActionSeriesList);
        createPostOptimNetworkActionSeries(sortedNetworkActions, curativeState, InstantKind.CURATIVE, remedialActionSeriesList);

        final ContingencySeries contingencySeries = newContingencySeries(contingency.getId(), contingency.getName().orElse(contingency.getId()));
        curativeB56.getContingencySeries().add(contingencySeries);

        if (!remedialActionSeriesList.isEmpty()) {
            // Add remedial actions to corresponding CNECs' B54
            List<ConstraintSeries> contingencyConstraintSeriesList = cnecsConstraintSeries.stream()
                .filter(constraintSeries -> constraintSeries.getBusinessType().equals(B54_BUSINESS_TYPE)
                    && constraintSeries.getContingencySeries().stream().anyMatch(series -> series.getName().equals(contingency.getName().orElse(contingency.getId()))))
                .toList();
            addRemedialActionsToOtherConstraintSeries(remedialActionSeriesList, contingencyConstraintSeriesList);
            // Add B56 to document
            constraintSeriesList.add(curativeB56);
        }
    }

    // POST-OPTIM

    private void createPostOptimPstRangeActionSeries(final List<PstRangeAction> rangeActions,
                                                     final State state,
                                                     final InstantKind instant,
                                                     final List<RemedialActionSeries> remedialActionSeriesList) {
        rangeActions.stream()
            .filter(action -> isRemedialActionDefinedForState(action, state))
            .filter(action -> isRangeActionActivatedDuringState(action, state))
            .filter(action -> !action.getNetworkElements().isEmpty())
            .map(action -> {
                final int optimizedTap = cneHelper.getRaoResult().getOptimizedTapOnState(state, action);
                return createPstRangeActionSeries(action, instant, optimizedTap);
            })
            .forEach(remedialActionSeriesList::add);
    }

    private void createPostOptimInjectionRangeActionSeries(final List<InjectionRangeAction> rangeActions,
                                                           final State state,
                                                           final InstantKind instant,
                                                           final List<RemedialActionSeries> remedialActionSeriesList) {
        rangeActions.stream()
            .filter(action -> isRemedialActionDefinedForState(action, state))
            .filter(action -> isRangeActionActivatedDuringState(action, state))
            .filter(action -> !action.getNetworkElements().isEmpty())
            .map(action -> {
                final Double optimizedSetpoint = cneHelper.getRaoResult().getOptimizedSetPointOnState(state, action);
                return createInjectionRangeActionSeries(action, instant, optimizedSetpoint);
            })
            .forEach(remedialActionSeriesList::addAll);
    }

    private void createPostOptimNetworkActionSeries(final List<NetworkAction> networkActions,
                                                    final State state,
                                                    final InstantKind instant,
                                                    final List<RemedialActionSeries> remedialActionSeriesList) {
        networkActions.stream()
            .filter(action -> isRemedialActionDefinedForState(action, state))
            .filter(action -> isNetworkActionActivatedDuringState(action, state))
            .map(action -> createB56RemedialActionSeries(action.getId(), action.getName(), action.getOperator(), instant))
            .forEach(remedialActionSeriesList::add);
    }

    private static boolean isRemedialActionDefinedForState(final RemedialAction<?> remedialAction, final State state) {
        return remedialAction.getUsageRules().stream().anyMatch(usageRule -> usageRule.isDefinedForState(state));
    }

    private boolean isRangeActionActivatedDuringState(final RangeAction<?> rangeAction, final State state) {
        // using RaoResult.isActivatedDuringState may throw an exception
        // if the state was not optimized or if the Range action was filtered out
        // that's why we use getActivatedRangeActionsDuringState instead
        // TODO Vérifier si le commentaire ci-dessus est toujours valable
        return cneHelper.getRaoResult().getActivatedRangeActionsDuringState(state).contains(rangeAction);
    }

    private boolean isNetworkActionActivatedDuringState(final NetworkAction networkAction, final State state) {
        // using RaoResult.isActivatedDuringState may throw an exception
        // if the state was not optimized
        // that's why we use getActivatedNetworkActionsDuringState instead
        return cneHelper.getRaoResult().getActivatedNetworkActionsDuringState(state).contains(networkAction);
    }

    private void addRemedialActionsToOtherConstraintSeries(final List<RemedialActionSeries> remedialActionSeriesList,
                                                           final List<ConstraintSeries> constraintSeriesList) {
        final List<RemedialActionSeries> shortPostOptimRemedialActionSeriesList = remedialActionSeriesList.stream()
            .map(remedialActionSeries -> newRemedialActionSeries(
                remedialActionSeries.getMRID(), remedialActionSeries.getName(), remedialActionSeries.getApplicationModeMarketObjectStatusStatus()
            ))
            .toList();
        constraintSeriesList.forEach(constraintSeries -> constraintSeries.getRemedialActionSeries().addAll(shortPostOptimRemedialActionSeriesList));
    }

    // COMMON

    private static ConstraintSeries getNewB56RaConstraintSeries() {
        return newConstraintSeries(randomizeString(RA_SERIES, 20), B56_BUSINESS_TYPE);
    }

    private RemedialActionSeries createPstRangeActionSeries(final PstRangeAction rangeAction,
                                                            final InstantKind instant,
                                                            final int tap) {
        final RemedialActionSeries remedialActionSeries = createB56RemedialActionSeries(
            rangeAction.getId(), rangeAction.getName(), rangeAction.getOperator(), instant
        );

        final PstRangeActionCreationContext context = (PstRangeActionCreationContext) cracCreationContext.getRemedialActionCreationContexts().stream()
            .filter(raContext -> rangeAction.getId().equals(raContext.getCreatedObjectId()))
            .findFirst().orElseThrow();
        final int invertedTap = (context.isInverted() ? -1 : 1) * tap;

        final RemedialActionRegisteredResource registeredResource = newRemedialActionRegisteredResource(
            context.getNativeObjectId(), context.getNativeNetworkElementId(),
            PST_RANGE_PSR_TYPE, invertedTap, WITHOUT_UNIT_SYMBOL, ABSOLUTE_MARKET_OBJECT_STATUS
        );
        remedialActionSeries.getRegisteredResource().add(registeredResource);
        remedialActionSeries.setMRID(createRangeActionId(remedialActionSeries.getMRID()));

        return remedialActionSeries;
    }

    private static boolean isInjectionRangeActionHvdcCompliant(final InjectionRangeAction rangeAction) {
        return rangeAction.getId().contains(SEPARATOR)
            && rangeAction.getName().contains(SEPARATOR)
            && rangeAction.getOperator().contains(SEPARATOR)
            && rangeAction.getNetworkElements().size() == 2;
    }

    private List<RemedialActionSeries> createInjectionRangeActionSeries(final InjectionRangeAction rangeAction,
                                                                        final InstantKind instant,
                                                                        final Double setpoint) {
        if (!isInjectionRangeActionHvdcCompliant(rangeAction)) {
            // In Core CC, the only elements that are currently modeled with injectionRangeAction objects are HVDC lines.
            // An injectionRangeAction that does not match the expected format for HVDC lines is not supposed to exist,
            // so if we find one then we should not add it to the CNE
            return List.of();
        }

        // First part of id/name/operator is "from", the second part is "to"
        final String[] raIds = rangeAction.getId().split(SEPARATOR_REGEX);
        final String[] raNames = rangeAction.getName().split(SEPARATOR_REGEX);
        final String[] raOperators = rangeAction.getOperator().split(SEPARATOR_REGEX);
        // NetworkElements are sorted by distribution key : -1 is "from" element, 1 is "to" element
        final String[] networkElementNames = rangeAction.getInjectionDistributionKeys().entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .map(Identifiable::getName)
            .toArray(String[]::new);

        final RemedialActionSeries remedialActionSeriesFrom = createInjectionRangeActionSeries(
            raIds[0], raNames[0], raOperators[0], instant, networkElementNames[0], setpoint, -1
        );
        final RemedialActionSeries remedialActionSeriesTo = createInjectionRangeActionSeries(
            raIds[1], raNames[1], raOperators[1], instant, networkElementNames[1], setpoint, 1
        );

        return List.of(remedialActionSeriesFrom, remedialActionSeriesTo);
    }

    private RemedialActionSeries createInjectionRangeActionSeries(final String raId,
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

    private RemedialActionSeries createB56RemedialActionSeries(final String remedialActionId,
                                                               final String remedialActionName,
                                                               final String operator,
                                                               final InstantKind optimizedInstantKind) {
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

    private String createRangeActionId(final String mRid) {
        return cutString(mRid, 55);
    }
}
