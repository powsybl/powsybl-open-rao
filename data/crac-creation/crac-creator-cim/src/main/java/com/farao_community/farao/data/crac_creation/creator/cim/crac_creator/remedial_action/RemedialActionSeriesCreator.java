/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.powsybl.iidm.network.*;
import com.farao_community.farao.data.crac_creation.creator.cim.xsd.*;
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
    private final CimCracCreationContext cracCreationContext;
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
        addRemedialAction(cimSerieId, remedialActionSeries);
    }

    private void addRemedialAction(String cimSerieId, RemedialActionSeries remedialActionSeries) {
        String createdRemedialActionId = remedialActionSeries.getMRID();
        String createdRemedialActionName = remedialActionSeries.getName();
        List<RemedialActionRegisteredResource> remedialActionRegisteredResources = remedialActionSeries.getRegisteredResource();
        String applicationModeMarketObjectStatus = remedialActionSeries.getApplicationModeMarketObjectStatusStatus();

        // 1) Remedial Action is a Pst Range Action :
        if (identifyAndAddRemedialActionPstRangeAction(createdRemedialActionId, createdRemedialActionName, remedialActionRegisteredResources, applicationModeMarketObjectStatus)) {
            return;
        }
        // 2) Remedial Action is part of a HVDC Range Action :
        if (identifyHvdcRangeAction(cimSerieId, remedialActionSeries)) {
            return;
        }

        // 3) Remedial Action is a Network Action :
        Set<RemedialActionSeriesCreationContext> networkActionSeriesCreationContexts = new NetworkActionCreator(
                crac, network,
                createdRemedialActionId, createdRemedialActionName, applicationModeMarketObjectStatus,
                remedialActionRegisteredResources,
                contingencies, invalidContingencies).addRemedialActionNetworkAction();
        remedialActionSeriesCreationContexts.addAll(networkActionSeriesCreationContexts);
    }

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
                    Set<RemedialActionSeriesCreationContext> pstRemedialActionSeriesCreationContexts = new PstRangeActionCreator(crac, network,
                            createdRemedialActionId, createdRemedialActionName, applicationModeMarketObjectStatus, remedialActionRegisteredResource,
                            contingencies, invalidContingencies).addPstRangeAction();
                    remedialActionSeriesCreationContexts.addAll(pstRemedialActionSeriesCreationContexts);
                    return true;
                }
            }
        }
        return false;
    }

    // Indicates whether an hvdc range action is present amidst the registered resources
    private boolean identifyHvdcRangeAction(String cimSerieId, RemedialActionSeries remedialActionSeries) {
        List<RemedialActionRegisteredResource> remedialActionRegisteredResources = remedialActionSeries.getRegisteredResource();
        String applicationModeMarketObjectStatus = remedialActionSeries.getApplicationModeMarketObjectStatusStatus();
        for (RemedialActionRegisteredResource remedialActionRegisteredResource : remedialActionRegisteredResources) {
            if (remedialActionRegisteredResource.getPSRTypePsrType().equals(PsrType.HVDC.getStatus())) {
                if (!applicationModeMarketObjectStatus.equals(ApplicationModeMarketObjectStatus.AUTO.getStatus())) {
                    remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(cimSerieId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("HVDC cannot be imported at instant %s", applicationModeMarketObjectStatus)));
                    return true;
                }
                storedHvdcRangeActions.add(remedialActionSeries);
                return true;
            }
        }
        return false;
    }

    /*---------------- STATIC METHODS ----------------------*/
    public static void importWithContingencies(String createdRemedialActionId, List<String> invalidContingencies, Set<RemedialActionSeriesCreationContext> remedialActionSeriesCreationContexts) {
        if (invalidContingencies.isEmpty()) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.imported(createdRemedialActionId, false, ""));
        } else {
            String contingencyList = StringUtils.join(invalidContingencies, ", ");
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.imported(createdRemedialActionId, true, String.format("Contingencies %s not defined in B55s", contingencyList)));
        }
    }

    public static boolean checkPstUnit(String createdRemedialActionId, String unitSymbol, Set<RemedialActionSeriesCreationContext> remedialActionSeriesCreationContexts) {
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

    public static boolean addUsageRules(String createdRemedialActionId, String applicationModeMarketObjectStatus, RemedialActionAdder<?> remedialActionAdder, List<Contingency> contingencies, List<String> invalidContingencies, Set<RemedialActionSeriesCreationContext> remedialActionSeriesCreationContexts) {
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
                    RemedialActionSeriesCreator.addFreeToUseUsageRules(remedialActionAdder, Instant.CURATIVE);
                }
            } else {
                RemedialActionSeriesCreator.addOnStateUsageRules(remedialActionAdder, Instant.CURATIVE, UsageMethod.AVAILABLE, contingencies);
            }
        }
        if (applicationModeMarketObjectStatus.equals(ApplicationModeMarketObjectStatus.PRA_AND_CRA.getStatus())) {
            addFreeToUseUsageRules(remedialActionAdder, Instant.PREVENTIVE);
            if (invalidContingencies.isEmpty() && contingencies.isEmpty()) {
                addFreeToUseUsageRules(remedialActionAdder, Instant.CURATIVE);
            }
            if (!contingencies.isEmpty()) {
                RemedialActionSeriesCreator.addOnStateUsageRules(remedialActionAdder, Instant.CURATIVE, UsageMethod.AVAILABLE, contingencies);
            }
        }
        if (applicationModeMarketObjectStatus.equals(ApplicationModeMarketObjectStatus.AUTO.getStatus())) {
            if (contingencies.isEmpty() && invalidContingencies.isEmpty()) {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "Cannot create a free-to-use remedial action at instant AUTO"));
                return true;
            } else if (contingencies.isEmpty()) {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "Contingencies are all invalid, and usage rule is on AUTO instant"));
                return true;
            } else {
                RemedialActionSeriesCreator.addOnStateUsageRules(remedialActionAdder, Instant.AUTO, UsageMethod.FORCED, contingencies);
            }
        }
        return false;
    }

    private static void addFreeToUseUsageRules(RemedialActionAdder<?> adder, Instant raApplicationInstant) {
        adder.newFreeToUseUsageRule()
                .withInstant(raApplicationInstant)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add();
    }

    private static void addOnStateUsageRules(RemedialActionAdder<?> adder, Instant raApplicationInstant, UsageMethod usageMethod,  List<Contingency> contingencies) {
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
