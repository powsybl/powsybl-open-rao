/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.range_action.PstRangeActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.util.PstHelper;
import com.farao_community.farao.data.crac_creation.util.cgmes.CgmesBranchHelper;
import com.farao_community.farao.data.crac_creation.util.iidm.IidmPstHelper;
import com.powsybl.iidm.network.*;
import com.farao_community.farao.data.crac_creation.creator.cim.xsd.*;
import com.powsybl.iidm.network.Identifiable;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimConstants.*;
import static com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracUtils.*;

/**
 * @author Godelaine de Montmorillon <godelaine.demontmorillon at rte-france.com>
 */
public class RemedialActionSeriesCreator {
    private final Crac crac;
    private final Network network;
    private final List<TimeSeries> cimTimeSeries;
    private Set<RemedialActionSeriesCreationContext> remedialActionSeriesCreationContexts;
    private CimCracCreationContext cracCreationContext;
    private List<RemedialActionSeries> storedHvdcRangeActions;
    private List<Contingency> contingencies;
    private List<String> invalidContingencies;

    public RemedialActionSeriesCreator(List<TimeSeries> cimTimeSeries, Crac crac, Network network, CimCracCreationContext cracCreationContext) {
        this.cimTimeSeries = cimTimeSeries;
        this.crac = crac;
        this.network = network;
        this.cracCreationContext = cracCreationContext;
    }

    public void createAndAddRemedialActionSeries() {
        this.remedialActionSeriesCreationContexts = new HashSet<>();
        this.storedHvdcRangeActions = new ArrayList<>();
        this.contingencies = new ArrayList<>();
        this.invalidContingencies = new ArrayList<>();

        for (TimeSeries cimTimeSerie : cimTimeSeries) {
            for (SeriesPeriod cimPeriodInTimeSerie : cimTimeSerie.getPeriod()) {
                for (Point cimPointInPeriodInTimeSerie : cimPeriodInTimeSerie.getPoint()) {
                    for (Series cimSerie : cimPointInPeriodInTimeSerie.getSeries().stream().filter(this::describesRemedialActionsToImport).collect(Collectors.toList())) {
                        if (checkRemedialActionSeries(cimSerie)) {
                            // Read and store contingencies
                            for (ContingencySeries cimContingency : cimSerie.getContingencySeries()) {
                                Optional<Contingency> contingency = getContingencyFromCrac(cimContingency, crac);
                                if (contingency.isPresent()) {
                                    contingencies.add(contingency.get());
                                } else {
                                    invalidContingencies.add(cimContingency.getMRID());
                                }
                            }
                            for (RemedialActionSeries remedialActionSeries : cimSerie.getRemedialActionSeries()) {

                                readAndAddRemedialAction(cimSerie.getMRID(), remedialActionSeries);
                            }
                            // Hvdc remedial action series are defined in the same Series
                            // Add HVDC range actions.
                            if (storedHvdcRangeActions.size() != 0) {
                                Set<RemedialActionSeriesCreationContext> hvdcRemedialActionSeriesCreationContexts = new HvdcRangeActionCreator(
                                        cimSerie.getMRID(),
                                        crac, network, storedHvdcRangeActions,
                                        contingencies, invalidContingencies).createAndAddHvdcRemedialActionSeries();
                                remedialActionSeriesCreationContexts.addAll(hvdcRemedialActionSeriesCreationContexts);
                            }
                        }
                        storedHvdcRangeActions.clear();
                        contingencies.clear();
                        invalidContingencies.clear();
                    }
                }
            }
        }
        this.cracCreationContext.setRemedialActionSeriesCreationContexts(remedialActionSeriesCreationContexts);
    }

    // For now, only free-to-use remedial actions are handled.
    private void readAndAddRemedialAction(String cimSerieId, RemedialActionSeries remedialActionSeries) {
        String createdRemedialActionId = remedialActionSeries.getMRID();
        String createdRemedialActionName = remedialActionSeries.getName();

        // --- BusinessType
        String businessType = remedialActionSeries.getBusinessType();
        if (!checkBusinessType(createdRemedialActionId, businessType)) {
            return;
        }

        // --- ApplicationModeMarketObjectStatus
        String applicationModeMarketObjectStatus = remedialActionSeries.getApplicationModeMarketObjectStatusStatus();
        if (!checkApplicationModeMarketObjectStatus(createdRemedialActionId, applicationModeMarketObjectStatus)) {
            return;
        }

        // --- Availability_MarketObjectStatus
        String availabilityMarketObjectStus = remedialActionSeries.getAvailabilityMarketObjectStatusStatus();
        if (!checkAvailabilityMarketObjectStatus(createdRemedialActionId, availabilityMarketObjectStus)) {
            return;
        }

        // --- Only import free-to-use remedial actions: there shouldn't be a shared domain tag
        if (remedialActionSeries.getSharedDomain().size() > 0) {
            if (remedialActionSeries.getSharedDomain().size() > 1) {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.NOT_YET_HANDLED_BY_FARAO, String.format("Only free to use remedial actions series are handled. There are %s shared domains", remedialActionSeries.getSharedDomain().size())));
                return;
            }
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.NOT_YET_HANDLED_BY_FARAO, String.format("Only free to use remedial actions series are handled. Shared domain is: %s", remedialActionSeries.getSharedDomain().get(0).getMRID().getValue())));
            return;
        }

        // --- Registered Resources
        List<RemedialActionRegisteredResource> remedialActionRegisteredResources = remedialActionSeries.getRegisteredResource();
        if (remedialActionRegisteredResources.isEmpty()) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, "Missing registered resource"));
            return;
        }

        // -- Add remedial action, or store it if it's a valid HVDC range action.
        if (!addRemedialAction(cimSerieId, createdRemedialActionId, createdRemedialActionName, remedialActionRegisteredResources, applicationModeMarketObjectStatus)) {
            storedHvdcRangeActions.add(remedialActionSeries);
        }
    }

    // Returns true if remedial action has been dealt with.
    // If false, remedial action is an hvdc range action, and will be added after the whole Series has been read.
    private boolean addRemedialAction(String cimSerieId, String createdRemedialActionId, String createdRemedialActionName, List<RemedialActionRegisteredResource> remedialActionRegisteredResources, String applicationModeMarketObjectStatus) {
        // 1) Remedial Action is a Pst Range Action :
        if (identifyAndAddRemedialActionPstRangeAction(createdRemedialActionId, createdRemedialActionName, remedialActionRegisteredResources, applicationModeMarketObjectStatus)) {
            return true;
        }
        // 2) Remedial Action is part of a HVDC Range Action :
        List<Boolean> hvdcRangeActionStatus = identifyHvdcRangeAction(cimSerieId, remedialActionRegisteredResources, applicationModeMarketObjectStatus);
        // -- Remedial action is ill defined
        if (hvdcRangeActionStatus.get(0)) {
            return true;
        }
        // -- Remedial action is an HVDC Range action :
        if (hvdcRangeActionStatus.get(1)) {
            return false;
        }
        // 3) Remedial Action is a Network Action :
        addRemedialActionNetworkAction(createdRemedialActionId, createdRemedialActionName, remedialActionRegisteredResources, applicationModeMarketObjectStatus);
        return true;
    }

    /*-------------- PST RANGE ACTION ------------------------------*/
    // Return true if remedial action has been defined.
    private boolean identifyAndAddRemedialActionPstRangeAction(String createdRemedialActionId, String createdRemedialActionName, List<RemedialActionRegisteredResource> remedialActionRegisteredResources, String applicationModeMarketObjectStatus) {
        for (RemedialActionRegisteredResource remedialActionRegisteredResource : remedialActionRegisteredResources) {
            String psrType = remedialActionRegisteredResource.getPSRTypePsrType();
            if (Objects.isNull(psrType)) {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, "Missing psrType"));
                return true;
            }
            // ------ PST
            if (psrType.equals(PsrType.PST.getStatus())) {
                // --------- PST Range Action
                if (Objects.isNull(remedialActionRegisteredResource.getResourceCapacityDefaultCapacity())) {
                    if (remedialActionRegisteredResources.size() > 1) {
                        remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("> 1 registered resources (%s) with at least one PST Range Action defined", remedialActionRegisteredResources.size())));
                        return true;
                    }
                    addPstRangeAction(createdRemedialActionId, createdRemedialActionName, remedialActionRegisteredResource, applicationModeMarketObjectStatus);
                    return true;
                }
            }
        }
        return false;
    }

    private void addPstRangeAction(String createdRemedialActionId, String createdRemedialActionName, RemedialActionRegisteredResource remedialActionRegisteredResource, String applicationModeMarketObjectStatus) {
        // --- Market Object status: define RangeType
        String marketObjectStatusStatus = remedialActionRegisteredResource.getMarketObjectStatusStatus();
        if (Objects.isNull(marketObjectStatusStatus)) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, "Missing marketObjectStatus"));
            return;
        }
        RangeType rangeType;
        if (marketObjectStatusStatus.equals(MarketObjectStatus.ABSOLUTE.getStatus())) {
            rangeType = RangeType.ABSOLUTE;
        } else if (marketObjectStatusStatus.equals(MarketObjectStatus.RELATIVE_TO_INITIAL_NETWORK.getStatus())) {
            rangeType = RangeType.RELATIVE_TO_INITIAL_NETWORK;
        } else if (marketObjectStatusStatus.equals(MarketObjectStatus.RELATIVE_TO_PREVIOUS_INSTANT1.getStatus()) || marketObjectStatusStatus.equals(MarketObjectStatus.RELATIVE_TO_PREVIOUS_INSTANT2.getStatus())) {
            rangeType = RangeType.RELATIVE_TO_PREVIOUS_INSTANT;
        } else if (marketObjectStatusStatus.equals(MarketObjectStatus.OPEN.getStatus()) || marketObjectStatusStatus.equals(MarketObjectStatus.CLOSE.getStatus())) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s, PST can no longer be opened/closed (deprecated)", marketObjectStatusStatus)));
            return;
        } else {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s", marketObjectStatusStatus)));
            return;
        }

        // Add remedial action
        // --- For now, do not set operator.
        // --- For now, do not set group id.
        String networkElementId = remedialActionRegisteredResource.getMRID().getValue();

        IidmPstHelper pstHelper = new IidmPstHelper(networkElementId, network);
        if (!pstHelper.isValid()) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("%s", pstHelper.getInvalidReason())));
            return;
        }

        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction()
                .withId(createdRemedialActionId)
                .withName(createdRemedialActionName)
                .withNetworkElement(pstHelper.getIdInNetwork())
                .withInitialTap(pstHelper.getInitialTap())
                .withTapToAngleConversionMap(pstHelper.getTapToAngleConversionMap());

        // --- Resource capacity
        String unitSymbol = remedialActionRegisteredResource.getResourceCapacityUnitSymbol();
        // Min and Max defined
        if (Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMinimumCapacity()) && Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMaximumCapacity())) {
            int minCapacity = remedialActionRegisteredResource.getResourceCapacityMinimumCapacity().intValue();
            int maxCapacity = remedialActionRegisteredResource.getResourceCapacityMaximumCapacity().intValue();
            if (checkUnit(createdRemedialActionId, unitSymbol)) {
                return;
            }
            addTapRangeWithMinAndMaxTap(pstRangeActionAdder, pstHelper, minCapacity, maxCapacity, rangeType);
        } else if (Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMaximumCapacity())) {
            int maxCapacity = remedialActionRegisteredResource.getResourceCapacityMaximumCapacity().intValue();
            if (checkUnit(createdRemedialActionId, unitSymbol)) {
                return;
            }
            addTapRangeWithMaxTap(pstRangeActionAdder, pstHelper, maxCapacity, rangeType);
        } else if (Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMinimumCapacity())) {
            int minCapacity = remedialActionRegisteredResource.getResourceCapacityMinimumCapacity().intValue();
            if (checkUnit(createdRemedialActionId, unitSymbol)) {
                return;
            }
            addTapRangeWithMinTap(pstRangeActionAdder, pstHelper, minCapacity, rangeType);
        }

        if (addUsageRules(createdRemedialActionId, applicationModeMarketObjectStatus, pstRangeActionAdder)) {
            return;
        }

        if (invalidContingencies.isEmpty()) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.imported(createdRemedialActionId, false, ""));
        } else {
            String contingencyList = StringUtils.join(invalidContingencies, ", ");
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.imported(createdRemedialActionId, true, String.format("Contingencies %s not defined in B55s", contingencyList)));
        }

        pstRangeActionAdder.add();
    }

    private boolean checkUnit(String createdRemedialActionId, String unitSymbol) {
        if (Objects.isNull(unitSymbol)) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, "Missing unit symbol"));
            return true;
        }
        if (!unitSymbol.equals(PST_CAPACITY_UNIT_SYMBOL)) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong unit symbol in its registered resource: %s", unitSymbol)));
            return true;
        }
        return false;
    }

    private void addTapRangeWithMinTap(PstRangeActionAdder pstRangeActionAdder, IidmPstHelper pstHelper, int minCapacity, RangeType rangeType) {
        int minTap = minCapacity;
        if (rangeType.equals(RangeType.ABSOLUTE)) {
            minTap = pstHelper.normalizeTap(minTap, PstHelper.TapConvention.STARTS_AT_ONE);
        }
        pstRangeActionAdder.newTapRange()
                .withMinTap(minTap)
                .withRangeType(rangeType)
                .add();
    }

    private void addTapRangeWithMaxTap(PstRangeActionAdder pstRangeActionAdder, IidmPstHelper pstHelper, int maxCapacity, RangeType rangeType) {
        int maxTap = maxCapacity;
        if (rangeType.equals(RangeType.ABSOLUTE)) {
            maxTap = pstHelper.normalizeTap(maxTap, PstHelper.TapConvention.STARTS_AT_ONE);
        }
        pstRangeActionAdder.newTapRange()
                .withMaxTap(maxTap)
                .withRangeType(rangeType)
                .add();
    }

    private void addTapRangeWithMinAndMaxTap(PstRangeActionAdder pstRangeActionAdder, IidmPstHelper pstHelper, int minCapacity, int maxCapacity, RangeType rangeType) {
        int minTap = minCapacity;
        int maxTap = maxCapacity;
        if (rangeType.equals(RangeType.ABSOLUTE)) {
            minTap = pstHelper.normalizeTap(minTap, PstHelper.TapConvention.STARTS_AT_ONE);
            maxTap = pstHelper.normalizeTap(maxTap, PstHelper.TapConvention.STARTS_AT_ONE);
        }
        pstRangeActionAdder.newTapRange()
                .withMinTap(minTap)
                .withMaxTap(maxTap)
                .withRangeType(rangeType)
                .add();
    }

    /*-------------- NETWORK ACTION ------------------------------*/
    private void addRemedialActionNetworkAction(String createdRemedialActionId, String createdRemedialActionName, List<RemedialActionRegisteredResource> remedialActionRegisteredResources, String applicationModeMarketObjectStatus) {
        NetworkActionAdder networkActionAdder = crac.newNetworkAction()
                .withId(createdRemedialActionId)
                .withName(createdRemedialActionName);

        if (addUsageRules(createdRemedialActionId, applicationModeMarketObjectStatus, networkActionAdder)) {
            return;
        }

        // Elementary actions
        for (RemedialActionRegisteredResource remedialActionRegisteredResource : remedialActionRegisteredResources) {
            String psrType = remedialActionRegisteredResource.getPSRTypePsrType();
            String elementaryActionId = remedialActionRegisteredResource.getName();
            if (Objects.isNull(psrType)) {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, String.format("Missing psrType on elementary action %s", elementaryActionId)));
                return;
            }

            // ------ PST setpoint elementary action
            if (psrType.equals(PsrType.PST.getStatus())) {
                if (!addPstSetpointElementaryAction(createdRemedialActionId, elementaryActionId, remedialActionRegisteredResource, networkActionAdder)) {
                    return;
                }
                // ------ Injection elementary action
            } else if (psrType.equals(PsrType.GENERATION.getStatus()) || psrType.equals(PsrType.LOAD.getStatus())) {
                if (!addInjectionSetpointElementaryAction(createdRemedialActionId, elementaryActionId, remedialActionRegisteredResource, networkActionAdder)) {
                    return;
                }
                // ------ TODO : Missing check : default capacity in ohms
            } else if (psrType.equals(PsrType.LINE.getStatus()) && remedialActionRegisteredResource.getMarketObjectStatusStatus().equals(MarketObjectStatus.ABSOLUTE.getStatus())) {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.NOT_YET_HANDLED_BY_FARAO, String.format("Modify line impedance as remedial action on elementary action %s", elementaryActionId)));
                return;
                // ------ Topological elementary action
            } else if (psrType.equals(PsrType.TIE_LINE.getStatus()) || psrType.equals(PsrType.LINE.getStatus()) || psrType.equals(PsrType.CIRCUIT_BREAKER.getStatus()) || psrType.equals(PsrType.TRANSFORMER.getStatus())) {
                if (!addTopologicalElementaryAction(createdRemedialActionId, elementaryActionId, remedialActionRegisteredResource, networkActionAdder)) {
                    return;
                }
            } else if (psrType.equals(PsrType.DEPRECATED_LINE.getStatus())) {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong psrType: %s, deprecated LINE psrType on elementary action %s", psrType, elementaryActionId)));
                return;
            } else {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong psrType: %s on elementary action %s", psrType, elementaryActionId)));
                return;
            }
        }
        if (invalidContingencies.isEmpty()) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.imported(createdRemedialActionId, false, ""));
        } else {
            String contingencyList = StringUtils.join(invalidContingencies, ", ");
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.imported(createdRemedialActionId, true, String.format("Contingencies %s not defined in B55s", contingencyList)));
        }

        networkActionAdder.add();
    }

    private boolean addPstSetpointElementaryAction(String createdRemedialActionId, String elementaryActionId, RemedialActionRegisteredResource remedialActionRegisteredResource, NetworkActionAdder networkActionAdder) {
        if (checkUnit(createdRemedialActionId, remedialActionRegisteredResource.getResourceCapacityUnitSymbol())) {
            return false;
        }
        // Pst helper
        String networkElementId = remedialActionRegisteredResource.getMRID().getValue();
        IidmPstHelper pstHelper = new IidmPstHelper(networkElementId, network);
        if (!pstHelper.isValid()) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("%s on elementary action %s", pstHelper.getInvalidReason(), elementaryActionId)));
            return false;
        }
        // --- Market Object status: define RangeType
        String marketObjectStatusStatus = remedialActionRegisteredResource.getMarketObjectStatusStatus();
        int setpoint;
        if (Objects.isNull(marketObjectStatusStatus)) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, String.format("Missing marketObjectStatus on elementary action %s", elementaryActionId)));
            return false;
        } else if (marketObjectStatusStatus.equals(MarketObjectStatus.ABSOLUTE.getStatus())) {
            setpoint = pstHelper.normalizeTap(remedialActionRegisteredResource.getResourceCapacityDefaultCapacity().intValue(), PstHelper.TapConvention.STARTS_AT_ONE);
        } else if (marketObjectStatusStatus.equals(MarketObjectStatus.RELATIVE_TO_INITIAL_NETWORK.getStatus())) {
            setpoint = pstHelper.getInitialTap() + remedialActionRegisteredResource.getResourceCapacityDefaultCapacity().intValue();
        } else if (marketObjectStatusStatus.equals(MarketObjectStatus.OPEN.getStatus()) || marketObjectStatusStatus.equals(MarketObjectStatus.CLOSE.getStatus())) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s, PST can no longer be opened/closed (deprecated) on elementary action %s", marketObjectStatusStatus, elementaryActionId)));
            return false;
        } else {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s on elementary action %s", marketObjectStatusStatus, elementaryActionId)));
            return false;
        }
        networkActionAdder.newPstSetPoint()
                .withNetworkElement(networkElementId)
                .withSetpoint(setpoint)
                .add();

        return true;
    }

    private boolean addInjectionSetpointElementaryAction(String createdRemedialActionId, String elementaryActionId, RemedialActionRegisteredResource remedialActionRegisteredResource, NetworkActionAdder networkActionAdder) {
        // Injection range actions aren't handled
        if (Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMinimumCapacity()) || Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMaximumCapacity())) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Injection setpoint elementary action %s should not have a min or max capacity defined", elementaryActionId)));
            return false;
        }
        // Market object status
        String marketObjectStatusStatus = remedialActionRegisteredResource.getMarketObjectStatusStatus();
        int setpoint;
        if (Objects.isNull(marketObjectStatusStatus)) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, String.format("Missing marketObjectStatus on injection setpoint elementary action %s", elementaryActionId)));
            return false;
        } else if (marketObjectStatusStatus.equals(MarketObjectStatus.ABSOLUTE.getStatus())) {
            if (Objects.isNull(remedialActionRegisteredResource.getResourceCapacityDefaultCapacity())) {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, String.format("Injection setpoint elementary action %s with ABSOLUTE marketObjectStatus should have a defaultCapacity", elementaryActionId)));
                return false;
            }
            if (Objects.isNull(remedialActionRegisteredResource.getResourceCapacityUnitSymbol())) {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, String.format("Injection setpoint elementary action %s with ABSOLUTE marketObjectStatus should have a unitSymbol", elementaryActionId)));
                return false;
            }
            if (!remedialActionRegisteredResource.getResourceCapacityUnitSymbol().equals(MEGAWATT_UNIT_SYMBOL)) {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong unit symbol for injection setpoint elementary action %s with ABSOLUTE marketObjectStatus : %s", elementaryActionId, remedialActionRegisteredResource.getResourceCapacityUnitSymbol())));
                return false;
            }
            setpoint = remedialActionRegisteredResource.getResourceCapacityDefaultCapacity().intValue();
        } else if (marketObjectStatusStatus.equals(MarketObjectStatus.STOP.getStatus())) {
            if (Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityDefaultCapacity())) {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Injection setpoint elementary action %s with STOP marketObjectStatus shouldn't have a defaultCapacity", elementaryActionId)));
                return false;
            }
            if (Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityUnitSymbol())) {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Injection setpoint elementary action %s with STOP marketObjectStatus shouldn't have a unitSymbol", elementaryActionId)));
                return false;
            }
            setpoint = 0;
        } else {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s on injection setpoint elementary action %s", marketObjectStatusStatus, elementaryActionId)));
            return false;
        }

        String networkElementId = remedialActionRegisteredResource.getMRID().getValue();
        if (Objects.isNull(network.getGenerator(networkElementId)) && Objects.isNull(network.getLoad(networkElementId))) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("%s is nor a generator nor a load on injection setpoint elementary action %s", networkElementId, elementaryActionId)));
            return false;
        }

        networkActionAdder.newInjectionSetPoint()
                .withNetworkElement(networkElementId)
                .withSetpoint(setpoint)
                .add();

        return true;
    }

    private boolean addTopologicalElementaryAction(String createdRemedialActionId, String elementaryActionId, RemedialActionRegisteredResource remedialActionRegisteredResource, NetworkActionAdder networkActionAdder) {
        if (Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMinimumCapacity()) || Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMaximumCapacity()) || Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityDefaultCapacity())) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Topological elementary action %s should not have any resource capacity defined", elementaryActionId)));
            return false;
        }
        // Market object status
        String marketObjectStatusStatus = remedialActionRegisteredResource.getMarketObjectStatusStatus();
        ActionType actionType;
        if (Objects.isNull(marketObjectStatusStatus)) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, String.format("Missing marketObjectStatus on topological elementary action %s", elementaryActionId)));
            return false;
        } else if (marketObjectStatusStatus.equals(MarketObjectStatus.OPEN.getStatus())) {
            actionType = ActionType.OPEN;
        } else if (marketObjectStatusStatus.equals(MarketObjectStatus.CLOSE.getStatus())) {
            actionType = ActionType.CLOSE;
        } else {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA,  String.format("Wrong marketObjectStatusStatus: %s on topological elementary action %s", marketObjectStatusStatus, elementaryActionId)));
            return false;
        }

        String networkElementId = remedialActionRegisteredResource.getMRID().getValue();
        Identifiable element = network.getIdentifiable(networkElementId);
        if (Objects.isNull(element)) {
            // Check that network element is not half a tie line
            CgmesBranchHelper branchHelper = new CgmesBranchHelper(networkElementId, network);
            if (branchHelper.getBranch() == null) {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("%s not in network on topological elementary action %s", networkElementId, elementaryActionId)));
                return false;
            }
            networkElementId = branchHelper.getIdInNetwork();
            element = branchHelper.getBranch();
        }
        if (!(element instanceof Branch) &&  !(element instanceof Switch)) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("%s is nor a branch nor a switch on elementary action %s", networkElementId, elementaryActionId)));
            return false;
        }

        networkActionAdder.newTopologicalAction()
                .withNetworkElement(networkElementId)
                .withActionType(actionType)
                .add();

        return true;
    }

    /*-------------- HVDC RANGE ACTION ------------------------------*/
    // Return type : list of two booleans, indicating whether the hvdc range action is ill defined,
    // and whether an hvdc range action is present amidst the registered resources
    private List<Boolean> identifyHvdcRangeAction(String cimSerieId, List<RemedialActionRegisteredResource> remedialActionRegisteredResources, String applicationModeMarketObjectStatus) {
        for (RemedialActionRegisteredResource remedialActionRegisteredResource : remedialActionRegisteredResources) {
            if (remedialActionRegisteredResource.getPSRTypePsrType().equals(PsrType.HVDC.getStatus())) {
                if (!applicationModeMarketObjectStatus.equals(ApplicationModeMarketObjectStatus.AUTO.getStatus())) {
                    remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(cimSerieId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("HVDC cannot be imported at instant %s", applicationModeMarketObjectStatus)));
                    return List.of(true, true);
                }
                return List.of(false, true);
            }
        }
        return List.of(false, false);
    }

    /*-------------- USAGE RULES ------------------------------*/
    public boolean addUsageRules(String createdRemedialActionId, String applicationModeMarketObjectStatus, RemedialActionAdder remedialActionAdder) {
        if (applicationModeMarketObjectStatus.equals(ApplicationModeMarketObjectStatus.PRA.getStatus())) {
            if (contingencies.isEmpty() && invalidContingencies.isEmpty()) {
                addFreeToUseUsageRules(remedialActionAdder, Instant.PREVENTIVE);
            } else  {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "Cannot create a preventive remedial action associated to a contingency"));
                return true;
            }
        }
        if (applicationModeMarketObjectStatus.equals(ApplicationModeMarketObjectStatus.CRA.getStatus())) {
            if (contingencies.isEmpty()) {
                if (!invalidContingencies.isEmpty()) {
                    remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "Contingencies are all invalid, and usage rule is on curative instant"));
                    return true;
                } else {
                    addFreeToUseUsageRules(remedialActionAdder, Instant.CURATIVE);
                }
            } else {
                addOnStateUsageRules(remedialActionAdder, Instant.CURATIVE, UsageMethod.AVAILABLE);
            }
        }
        if (applicationModeMarketObjectStatus.equals(ApplicationModeMarketObjectStatus.PRA_AND_CRA.getStatus())) {
            addFreeToUseUsageRules(remedialActionAdder, Instant.PREVENTIVE);
            if (invalidContingencies.isEmpty() && contingencies.isEmpty()) {
                addFreeToUseUsageRules(remedialActionAdder, Instant.CURATIVE);
            }
            if (!contingencies.isEmpty()) {
                addOnStateUsageRules(remedialActionAdder, Instant.CURATIVE, UsageMethod.AVAILABLE);
            }
        }
        if (applicationModeMarketObjectStatus.equals(ApplicationModeMarketObjectStatus.AUTO.getStatus())) {
            if (contingencies.isEmpty() && invalidContingencies.isEmpty()) {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "Cannot create a free-to-use remedial action at instant AUTO"));
                return true;
            } else if (contingencies.isEmpty()) {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "Contingencies are all invalid, and usage rule is on AUTO instant %s"));
                return true;
            } else {
                addOnStateUsageRules(remedialActionAdder, Instant.AUTO, UsageMethod.FORCED);
            }
        }
        return false;
    }

    private void addFreeToUseUsageRules(RemedialActionAdder adder, Instant raApplicationInstant) {
        adder.newFreeToUseUsageRule()
                .withInstant(raApplicationInstant)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add();
    }

    private void addOnStateUsageRules(RemedialActionAdder adder, Instant raApplicationInstant, UsageMethod usageMethod) {
        contingencies.forEach(contingency ->
                adder.newOnStateUsageRule()
                        .withInstant(raApplicationInstant)
                        .withUsageMethod(usageMethod)
                        .withContingency(contingency.getId())
                        .add());
    }

    /*-------------- SERIES CHECKS ------------------------------*/
    private boolean checkRemedialActionSeries(Series cimSerie) {
        // --- Check optimizationStatus
        String optimizationStatus = cimSerie.getOptimizationMarketObjectStatusStatus();
        if (Objects.nonNull(optimizationStatus)) {
            boolean statusOk = false;
            for (String status : REMEDIAL_ACTION_OPTIMIZATION_STATUS) {
                if (optimizationStatus.equals(status)) {
                    statusOk = true;
                    break;
                }
            }
            if (!statusOk) {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(cimSerie.getMRID(), ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong optimization status: %s", optimizationStatus)));
                return false;
            }
        }
        // --- Do not import remedial actions specific to angle monitoring yet
        if (cimSerie.getAdditionalConstraintSeries().size() > 0) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(cimSerie.getMRID(), ImportStatus.NOT_YET_HANDLED_BY_FARAO, String.format("Series %s: remedial actions specific to angle monitoring are not handled yet",  cimSerie.getMRID())));
            return false;
        }
        // --- Do not import remedial actions available on a CNEC yet.
        if (cimSerie.getMonitoredSeries().size() > 0) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(cimSerie.getMRID(), ImportStatus.NOT_YET_HANDLED_BY_FARAO, "Remedial actions available on a CNEC are not handled yet"));
            return false;
        }
        return true;
    }

    private boolean checkBusinessType(String createdRemedialActionId, String businessType) {
        if (Objects.isNull(businessType) || !businessType.equals(BUSINESS_TYPE_IN_REMEDIALACTION_SERIES)) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong businessType: %s", businessType)));
            return false;
        }
        return true;
    }

    private boolean checkApplicationModeMarketObjectStatus(String createdRemedialActionId, String applicationModeMarketObjectStatus) {
        if (Objects.isNull(applicationModeMarketObjectStatus)) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, "Missing applicationMode MarketObjectStatus"));
            return false;
        }
        for (ApplicationModeMarketObjectStatus value : ApplicationModeMarketObjectStatus.values()) {
            if (applicationModeMarketObjectStatus.equals(value.getStatus())) {
                return true;
            }
        }
        remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong applicationMode_MarketObjectStatus: %s", applicationModeMarketObjectStatus)));
        return false;
    }

    private boolean checkAvailabilityMarketObjectStatus(String createdRemedialActionId, String availabilityMarketObjectStatus) {
        if (Objects.isNull(availabilityMarketObjectStatus)) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, "Missing availabilityMarketObjectStatus"));
            return false;
        }
        // Temporary: A38 not yet handled by FARAO.
        if (availabilityMarketObjectStatus.equals(AvailabilityMarketObjectStatus.SHALL_BE_USED.getStatus())) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.NOT_YET_HANDLED_BY_FARAO, String.format("Wrong availabilityMarketObjectStatus: %s", availabilityMarketObjectStatus)));
            return false;
        }
        if (!availabilityMarketObjectStatus.equals(AvailabilityMarketObjectStatus.MIGHT_BE_USED.getStatus())) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong availabilityMarketObjectStatus: %s", availabilityMarketObjectStatus)));
            return false;
        }
        return true;
    }

    private boolean describesRemedialActionsToImport(Series series) {
        return series.getBusinessType().equals(REMEDIAL_ACTIONS_SERIES_BUSINESS_TYPE);
    }
}
