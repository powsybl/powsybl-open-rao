/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialActionAdder;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.FaraoImportException;
import com.farao_community.farao.data.crac_creation.creator.cim.parameters.CimCracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cim.xsd.*;
import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimConstants.*;
import static com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracUtils.getContingencyFromCrac;
import static com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracUtils.getFlowCnecsFromCrac;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class RemedialActionSeriesCreator {
    private final Crac crac;
    private final Network network;
    private final List<TimeSeries> cimTimeSeries;
    private Set<RemedialActionSeriesCreationContext> remedialActionSeriesCreationContexts;
    private final CimCracCreationContext cracCreationContext;
    private List<Contingency> contingencies;
    private List<String> invalidContingencies;
    private HvdcRangeActionCreator hvdcRangeActionCreator = null;
    private Set<FlowCnec> flowCnecs;
    private final CimCracCreationParameters cimCracCreationParameters;
    private Map<String, PstRangeActionCreator> pstRangeActionCreators = new HashMap<>();
    private Map<String, NetworkActionCreator> networkActionCreators = new HashMap<>();
    private Set<HvdcRangeActionCreator> hvdcRangeActionCreators = new HashSet<>();
    private Country sharedDomain;

    public RemedialActionSeriesCreator(List<TimeSeries> cimTimeSeries, Crac crac, Network network, CimCracCreationContext cracCreationContext, CimCracCreationParameters cimCracCreationParameters) {
        this.cimTimeSeries = cimTimeSeries;
        this.crac = crac;
        this.network = network;
        this.cracCreationContext = cracCreationContext;
        this.cimCracCreationParameters = cimCracCreationParameters;
    }

    private Set<Series> getRaSeries() {
        Set<Series> raSeries = new HashSet<>();
        cimTimeSeries.forEach(
            timeSerie -> timeSerie.getPeriod().forEach(
                period -> period.getPoint().forEach(
                    point -> point.getSeries().stream()
                        .filter(this::describesRemedialActionsToImport)
                        .filter(this::checkRemedialActionSeries)
                        .forEach(raSeries::add)
                )));
        return raSeries;
    }

    public void createAndAddRemedialActionSeries() {
        this.remedialActionSeriesCreationContexts = new HashSet<>();
        this.contingencies = new ArrayList<>();
        this.invalidContingencies = new ArrayList<>();

        for (Series cimSerie : getRaSeries()) {
            // Read and store contingencies
            cimSerie.getContingencySeries().forEach(cimContingency -> {
                Contingency contingency = getContingencyFromCrac(cimContingency, cracCreationContext);
                if (Objects.nonNull(contingency)) {
                    contingencies.add(contingency);
                } else {
                    invalidContingencies.add(cimContingency.getMRID());
                }
            });
            // Read and store Monitored Series
            this.flowCnecs = getFlowCnecsFromMonitoredAndContingencySeries(cimSerie);
            // Read and create / modify RA creators
            boolean shouldReadSharedDomain = cimSerie.getContingencySeries().isEmpty() && cimSerie.getMonitoredSeries().isEmpty();
            for (RemedialActionSeries remedialActionSeries : cimSerie.getRemedialActionSeries()) {
                readRemedialAction(remedialActionSeries, shouldReadSharedDomain);
            }

            if (hvdcRangeActionCreator != null) {
                this.hvdcRangeActionCreators.add(hvdcRangeActionCreator);
                hvdcRangeActionCreator = null;
            }

            contingencies.clear();
            invalidContingencies.clear();
        }

        // Add all RAs from creators to CRAC
        addAllRemedialActionsToCrac();
        this.cracCreationContext.setRemedialActionSeriesCreationContexts(remedialActionSeriesCreationContexts);
    }

    /**
     * Reads Monitored_Series of a Series and maps them to flow cnecs
     * If Contingency_Series exist in the Series, then the flow cnecs that do not correspond to these contingencies are filtered out
     */
    private Set<FlowCnec> getFlowCnecsFromMonitoredAndContingencySeries(Series cimSerie) {
        Set<FlowCnec> flowCnecsFromMsAndCs = new HashSet<>();
        for (MonitoredSeries monitoredSeries : cimSerie.getMonitoredSeries()) {
            Set<FlowCnec> flowCnecsForMs = getFlowCnecsFromCrac(monitoredSeries, cracCreationContext);
            if (!cimSerie.getContingencySeries().isEmpty()) {
                flowCnecsForMs = flowCnecsForMs.stream().filter(
                    flowCnec -> flowCnec.getState().getContingency().isPresent()
                        && contingencies.contains(flowCnec.getState().getContingency().get())
                ).collect(Collectors.toSet());
            }
            flowCnecsFromMsAndCs.addAll(flowCnecsForMs);
        }
        return flowCnecsFromMsAndCs;
    }

    private void addAllRemedialActionsToCrac() {
        pstRangeActionCreators.values().forEach(
            creator -> {
                creator.addPstRangeAction();
                this.remedialActionSeriesCreationContexts.add(creator.getPstRangeActionCreationContext());
            }
        );
        networkActionCreators.values().forEach(
            creator -> {
                creator.addNetworkAction();
                this.remedialActionSeriesCreationContexts.add(creator.getNetworkActionCreationContext());
            }
        );
        hvdcRangeActionCreators.forEach(
            creator -> this.remedialActionSeriesCreationContexts.addAll(creator.add())
        );
    }

    private void readRemedialAction(RemedialActionSeries remedialActionSeries, boolean shouldReadSharedDomain) {
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

        // --- Shared domain
        sharedDomain = null;
        if (shouldReadSharedDomain && !remedialActionSeries.getSharedDomain().isEmpty()) {
            if (remedialActionSeries.getSharedDomain().size() > 1) {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.NOT_YET_HANDLED_BY_FARAO, "RemedialActionSeries with multiple SharedDomain are not supported"));
                return;
            }
            sharedDomain = new CountryEICode(remedialActionSeries.getSharedDomain().get(0).getMRID().getValue()).getCountry();
        }

        // --- Registered Resources
        List<RemedialActionRegisteredResource> remedialActionRegisteredResources = remedialActionSeries.getRegisteredResource();
        if (remedialActionRegisteredResources.isEmpty()) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, "Missing registered resource"));
            return;
        }

        // -- Read remedial action, and store or modify its creator
        readRemedialAction(remedialActionSeries);
    }

    private void readRemedialAction(RemedialActionSeries remedialActionSeries) {
        String createdRemedialActionId = remedialActionSeries.getMRID();
        String createdRemedialActionName = remedialActionSeries.getName();
        List<RemedialActionRegisteredResource> remedialActionRegisteredResources = remedialActionSeries.getRegisteredResource();
        String applicationModeMarketObjectStatus = remedialActionSeries.getApplicationModeMarketObjectStatusStatus();

        // 1) Check if Remedial Action is a Pst Range Action :
        if (identifyAndReadPstRangeAction(createdRemedialActionId, createdRemedialActionName, remedialActionRegisteredResources, applicationModeMarketObjectStatus)) {
            return;
        }
        // 2) Check if Remedial Action is part of a HVDC Range Action :
        if (identifyAndReadHvdcRangeAction(remedialActionSeries)) {
            return;
        }

        // 3) Suppose that Remedial Action is a Network Action :
        readNetworkAction(createdRemedialActionId, createdRemedialActionName, remedialActionRegisteredResources, applicationModeMarketObjectStatus);
    }

    // Return true if PST range action has been read.
    private boolean identifyAndReadPstRangeAction(String createdRemedialActionId, String createdRemedialActionName, List<RemedialActionRegisteredResource> remedialActionRegisteredResources, String applicationModeMarketObjectStatus) {
        for (RemedialActionRegisteredResource remedialActionRegisteredResource : remedialActionRegisteredResources) {
            String psrType = remedialActionRegisteredResource.getPSRTypePsrType();
            if (Objects.isNull(psrType)) {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, "Missing psrType"));
                return true;
            }
            // ------ PST Range Action
            if (psrType.equals(PsrType.PST.getStatus()) && Objects.isNull(remedialActionRegisteredResource.getResourceCapacityDefaultCapacity())) {
                if (remedialActionRegisteredResources.size() > 1) {
                    remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("> 1 registered resources (%s) with at least one PST Range Action defined", remedialActionRegisteredResources.size())));
                    return true;
                }
                if (!pstRangeActionCreators.containsKey(createdRemedialActionId)
                    || !pstRangeActionCreators.get(createdRemedialActionId).getPstRangeActionCreationContext().isImported()) {
                    PstRangeActionCreator pstRangeActionCreator = new PstRangeActionCreator(crac, network,
                        createdRemedialActionId, createdRemedialActionName, applicationModeMarketObjectStatus, remedialActionRegisteredResource,
                        contingencies, invalidContingencies, flowCnecs, sharedDomain);
                    pstRangeActionCreator.createPstRangeActionAdder(cimCracCreationParameters);
                    pstRangeActionCreators.put(createdRemedialActionId, pstRangeActionCreator);
                } else {
                    // Some remedial actions can be defined in multiple Series in order to define multiple usage rules (eg on flow constraint on different CNECs)
                    // In this case, only import extra usage rules
                    addExtraUsageRules(applicationModeMarketObjectStatus, createdRemedialActionId, pstRangeActionCreators.get(createdRemedialActionId).getPstRangeActionAdder());
                }
                return true;

            }
        }
        return false;
    }

    private void addExtraUsageRules(String applicationModeMarketObjectStatus, String remedialActionId, RemedialActionAdder<?> adder) {
        try {
            RemedialActionSeriesCreator.addUsageRules(
                applicationModeMarketObjectStatus, adder, contingencies, invalidContingencies, flowCnecs, sharedDomain
            );
        } catch (FaraoImportException e) {
            cracCreationContext.getCreationReport().warn(String.format("Extra usage rules for RA %s could not be imported: %s", remedialActionId, e.getMessage()));
        }
    }

    // Indicates whether an hvdc range action is present amidst the registered resources
    private boolean identifyAndReadHvdcRangeAction(RemedialActionSeries remedialActionSeries) {
        List<RemedialActionRegisteredResource> remedialActionRegisteredResources = remedialActionSeries.getRegisteredResource();
        String applicationModeMarketObjectStatus = remedialActionSeries.getApplicationModeMarketObjectStatusStatus();
        for (RemedialActionRegisteredResource remedialActionRegisteredResource : remedialActionRegisteredResources) {
            if (remedialActionRegisteredResource.getPSRTypePsrType().equals(PsrType.HVDC.getStatus())) {
                if (!applicationModeMarketObjectStatus.equals(ApplicationModeMarketObjectStatus.AUTO.getStatus())) {
                    remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(remedialActionSeries.getMRID(), ImportStatus.INCONSISTENCY_IN_DATA, String.format("HVDC cannot be imported at instant %s", applicationModeMarketObjectStatus)));
                    return true;
                }
                if (Objects.isNull(hvdcRangeActionCreator)) {
                    hvdcRangeActionCreator = new HvdcRangeActionCreator(
                        crac, network,
                        contingencies, invalidContingencies, flowCnecs, sharedDomain);
                }
                hvdcRangeActionCreator.addDirection(remedialActionSeries);
                return true;
            }
        }
        return false;
    }

    private void readNetworkAction(String createdRemedialActionId, String createdRemedialActionName, List<RemedialActionRegisteredResource> remedialActionRegisteredResources, String applicationModeMarketObjectStatus) {
        if (!networkActionCreators.containsKey(createdRemedialActionId)
            || !networkActionCreators.get(createdRemedialActionId).getNetworkActionCreationContext().isImported()) {
            NetworkActionCreator networkActionCreator = new NetworkActionCreator(
                crac, network,
                createdRemedialActionId, createdRemedialActionName, applicationModeMarketObjectStatus,
                remedialActionRegisteredResources,
                contingencies, invalidContingencies, flowCnecs, sharedDomain);
            networkActionCreator.createNetworkActionAdder();
            networkActionCreators.put(createdRemedialActionId, networkActionCreator);
        } else {
            // Some remedial actions can be defined in multiple Series in order to define multiple usage rules (eg on flow constraint on different CNECs)
            // In this case, only import extra usage rules
            addExtraUsageRules(applicationModeMarketObjectStatus, createdRemedialActionId, networkActionCreators.get(createdRemedialActionId).getNetworkActionAdder());
        }
    }

    /*---------------- STATIC METHODS ----------------------*/
    public static RemedialActionSeriesCreationContext importWithContingencies(String createdRemedialActionId, List<String> invalidContingencies) {
        if (invalidContingencies.isEmpty()) {
            return RemedialActionSeriesCreationContext.imported(createdRemedialActionId, false, "");
        } else {
            String contingencyList = StringUtils.join(invalidContingencies, ", ");
            return RemedialActionSeriesCreationContext.imported(createdRemedialActionId, true, String.format("Contingencies %s not defined in B55s", contingencyList));
        }
    }

    public static void checkPstUnit(String unitSymbol) {
        if (Objects.isNull(unitSymbol)) {
            throw new FaraoImportException(ImportStatus.INCOMPLETE_DATA, "Missing unit symbol");
        }
        if (!unitSymbol.equals(PST_CAPACITY_UNIT_SYMBOL)) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong unit symbol in its registered resource: %s", unitSymbol));
        }
    }

    public static void addUsageRules(String applicationModeMarketObjectStatus,
                                     RemedialActionAdder<?> remedialActionAdder,
                                     List<Contingency> contingencies,
                                     List<String> invalidContingencies,
                                     Set<FlowCnec> flowCnecs,
                                     Country sharedDomain) {
        if (applicationModeMarketObjectStatus.equals(ApplicationModeMarketObjectStatus.PRA.getStatus())) {
            addUsageRulesAtInstant(remedialActionAdder, contingencies, invalidContingencies, flowCnecs, sharedDomain, Instant.PREVENTIVE);
        }
        if (applicationModeMarketObjectStatus.equals(ApplicationModeMarketObjectStatus.CRA.getStatus())) {
            addUsageRulesAtInstant(remedialActionAdder, contingencies, invalidContingencies, flowCnecs, sharedDomain, Instant.CURATIVE);
        }
        if (applicationModeMarketObjectStatus.equals(ApplicationModeMarketObjectStatus.PRA_AND_CRA.getStatus())) {
            addUsageRulesAtInstant(remedialActionAdder, null, null, flowCnecs, sharedDomain, Instant.PREVENTIVE);
            addUsageRulesAtInstant(remedialActionAdder, contingencies, invalidContingencies, flowCnecs, sharedDomain, Instant.CURATIVE);
        }
        if (applicationModeMarketObjectStatus.equals(ApplicationModeMarketObjectStatus.AUTO.getStatus())) {
            addUsageRulesAtInstant(remedialActionAdder, contingencies, invalidContingencies, flowCnecs, sharedDomain, Instant.AUTO);
        }
    }

    private static void addUsageRulesAtInstant(RemedialActionAdder<?> remedialActionAdder,
                                               List<Contingency> contingencies,
                                               List<String> invalidContingencies,
                                               Set<FlowCnec> flowCnecs,
                                               Country sharedDomain,
                                               Instant instant) {
        if (!flowCnecs.isEmpty()) {
            flowCnecs.forEach(flowCnec -> addOnFlowConstraintUsageRule(remedialActionAdder, flowCnec, instant));
            return;
        }
        if (!Objects.isNull(sharedDomain)) {
            remedialActionAdder.newOnFlowConstraintInCountryUsageRule().withInstant(instant).withCountry(sharedDomain).add();
            return;
        }

        checkUsageRulesContingencies(instant, contingencies, invalidContingencies);

        if (instant.equals(Instant.PREVENTIVE) || (instant.equals(Instant.CURATIVE) && contingencies.isEmpty())) {
            addFreeToUseUsageRules(remedialActionAdder, instant);
        } else {
            UsageMethod usageMethod = instant.equals(Instant.CURATIVE) ? UsageMethod.AVAILABLE : UsageMethod.FORCED;
            RemedialActionSeriesCreator.addOnStateUsageRules(remedialActionAdder, instant, usageMethod, contingencies);
        }
    }

    private static void checkUsageRulesContingencies(Instant instant, List<Contingency> contingencies, List<String> invalidContingencies) {
        switch (instant) {
            case PREVENTIVE:
                if ((Objects.nonNull(contingencies) && !contingencies.isEmpty()) || (Objects.nonNull(invalidContingencies) && !invalidContingencies.isEmpty())) {
                    throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Cannot create a preventive remedial action associated to a contingency");
                }
                break;
            case AUTO:
                if (contingencies.isEmpty() && invalidContingencies.isEmpty()) {
                    throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Cannot create a free-to-use remedial action at instant AUTO");
                }
                if (contingencies.isEmpty()) {
                    throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Contingencies are all invalid, and usage rule is on AUTO instant");
                }
                break;
            case CURATIVE:
                if (contingencies.isEmpty() && !invalidContingencies.isEmpty()) {
                    throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Contingencies are all invalid, and usage rule is on curative instant");
                }
                break;
            default:
                throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Cannot add usage rule on instant %s", instant));
        }
    }

    private static void addFreeToUseUsageRules(RemedialActionAdder<?> adder, Instant raApplicationInstant) {
        adder.newFreeToUseUsageRule()
            .withInstant(raApplicationInstant)
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add();
    }

    private static void addOnStateUsageRules(RemedialActionAdder<?> adder, Instant raApplicationInstant, UsageMethod usageMethod, List<Contingency> contingencies) {
        contingencies.forEach(contingency ->
            adder.newOnStateUsageRule()
                .withInstant(raApplicationInstant)
                .withUsageMethod(usageMethod)
                .withContingency(contingency.getId())
                .add());
    }

    private static void addOnFlowConstraintUsageRule(RemedialActionAdder<?> adder, FlowCnec flowCnec, Instant instant) {
        if (flowCnec.getState().getInstant().comesBefore(instant)) {
            return;
        }
        adder.newOnFlowConstraintUsageRule()
            .withFlowCnec(flowCnec.getId())
            .withInstant(instant)
            .add();
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
        if (!cimSerie.getAdditionalConstraintSeries().isEmpty()) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(cimSerie.getMRID(), ImportStatus.NOT_YET_HANDLED_BY_FARAO, String.format("Series %s: remedial actions specific to angle monitoring are not handled yet", cimSerie.getMRID())));
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
        // A38 not handled by FARAO.
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
