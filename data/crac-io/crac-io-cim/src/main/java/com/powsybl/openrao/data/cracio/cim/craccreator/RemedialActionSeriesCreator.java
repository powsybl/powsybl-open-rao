/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.cim.craccreator;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.usagerule.OnFlowConstraintInCountryAdder;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.cim.parameters.CimCracCreationParameters;
import com.powsybl.openrao.data.cracio.commons.OpenRaoImportException;
import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracio.cim.xsd.MonitoredSeries;
import com.powsybl.openrao.data.cracio.cim.xsd.RemedialActionRegisteredResource;
import com.powsybl.openrao.data.cracio.cim.xsd.RemedialActionSeries;
import com.powsybl.openrao.data.cracio.cim.xsd.Series;
import com.powsybl.openrao.data.cracio.cim.xsd.TimeSeries;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.cracio.cim.craccreator.CimConstants.*;
import static com.powsybl.openrao.data.cracio.cim.craccreator.CimCracUtils.getContingencyFromCrac;
import static com.powsybl.openrao.data.cracio.cim.craccreator.CimCracUtils.getFlowCnecsFromCrac;

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
    private AngleCnec angleCnec;
    private final CimCracCreationParameters cimCracCreationParameters;
    private final Map<String, PstRangeActionCreator> pstRangeActionCreators = new HashMap<>();
    private final Map<String, NetworkActionCreator> networkActionCreators = new HashMap<>();
    private final Set<HvdcRangeActionCreator> hvdcRangeActionCreators = new HashSet<>();
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

        CimCracUtils.applyActionToEveryPoint(
            cimTimeSeries,
            cracCreationContext.getTimeStamp().toInstant(),
            point -> point.getSeries().stream()
                    .filter(this::describesRemedialActionsToImport)
                    .filter(this::checkRemedialActionSeries)
                    .forEach(raSeries::add)
        );

        return raSeries;
    }

    public void createAndAddRemedialActionSeries() {
        this.remedialActionSeriesCreationContexts = new HashSet<>();
        this.contingencies = new ArrayList<>();
        this.invalidContingencies = new ArrayList<>();

        for (Series cimSerie : getRaSeries()) {
            this.angleCnec = null;
            // Read and store contingencies
            cimSerie.getContingencySeries().forEach(cimContingency -> {
                Contingency contingency = getContingencyFromCrac(cimContingency, cracCreationContext);
                if (Objects.nonNull(contingency)) {
                    contingencies.add(contingency);
                } else {
                    invalidContingencies.add(cimContingency.getMRID());
                }
            });

            // Read and store AdditionalConstraint Series
            if (!readAdditionalConstraintSeries(cimSerie)) {
                continue;
            }
            // Read and store Monitored Series
            this.flowCnecs = getFlowCnecsFromMonitoredAndContingencySeries(cimSerie);
            // Read and create / modify RA creators
            boolean shouldReadSharedDomain = cimSerie.getMonitoredSeries().isEmpty();
            for (RemedialActionSeries remedialActionSeries : cimSerie.getRemedialActionSeries()) {
                readRemedialAction(remedialActionSeries, shouldReadSharedDomain);
            }

            if (hvdcRangeActionCreator != null) {
                this.hvdcRangeActionCreators.add(hvdcRangeActionCreator);
                hvdcRangeActionCreator = null;
            }
            resetSeriesContingencies();
        }

        // Add all RAs from creators to CRAC
        addAllRemedialActionsToCrac();
        this.cracCreationContext.setRemedialActionSeriesCreationContexts(remedialActionSeriesCreationContexts);
    }

    /**
     * Reads AdditionalConstraint_Series of a Series and potentially creates associated angleCnec
     * If an angle cnec has been correctly defined, return true to import cimSerie's remedial actions.
     */
    private boolean readAdditionalConstraintSeries(Series cimSerie) {
        if (!cimSerie.getAdditionalConstraintSeries().isEmpty()) {
            if (isAValidAngleCnecSeries(cimSerie)) {
                AdditionalConstraintSeriesCreator additionalConstraintSeriesCreator = new AdditionalConstraintSeriesCreator(crac, network, cimSerie.getAdditionalConstraintSeries().get(0), contingencies.get(0).getId(), cimSerie.getMRID(), cracCreationContext);
                this.angleCnec = additionalConstraintSeriesCreator.createAndAddAdditionalConstraintSeries();
                // If angle cnec import has failed, create failed RemedialActionSeriesCreationContexts for associated remedial actions.
                if (cracCreationContext.getAngleCnecCreationContexts().stream().anyMatch(context -> context.getSerieId().equals(cimSerie.getMRID()) && !context.isImported())) {
                    for (RemedialActionSeries remedialActionSeries : cimSerie.getRemedialActionSeries()) {
                        remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(remedialActionSeries.getMRID(), ImportStatus.INCONSISTENCY_IN_DATA, "Associated angle cnec could not be imported"));
                    }
                    resetSeriesContingencies();
                    return false;
                }
            } else {
                resetSeriesContingencies();
                return false;
            }
        }
        return true;
    }

    /**
     * A valid angle cnec series contains :
     * -- exactly 1 additional constraint series
     * -- exactly 1 valid contingency
     */
    private boolean isAValidAngleCnecSeries(Series cimSerie) {
        if (cimSerie.getAdditionalConstraintSeries().size() == 1
            && contingencies.size() == 1 && invalidContingencies.isEmpty()) {
            return true;
        } else if (cimSerie.getAdditionalConstraintSeries().size() > 1) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(cimSerie.getMRID(), ImportStatus.INCONSISTENCY_IN_DATA, String.format("Angle cnec series has too many (%s) additional constraint series", cimSerie.getAdditionalConstraintSeries().size())));
            return false;
        } else {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(cimSerie.getMRID(), ImportStatus.INCONSISTENCY_IN_DATA, "Angle cnec series has an ill defined contingency series"));
            return false;
        }
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
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO, "RemedialActionSeries with multiple SharedDomain are not supported"));
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
                remedialActionSeriesCreationContexts.add(PstRangeActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, "Missing psrType"));
                return true;
            }
            // ------ PST Range Action
            if (psrType.equals(PsrType.PST.getStatus()) && Objects.isNull(remedialActionRegisteredResource.getResourceCapacityDefaultCapacity())) {
                if (remedialActionRegisteredResources.size() > 1) {
                    remedialActionSeriesCreationContexts.add(PstRangeActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("> 1 registered resources (%s) with at least one PST Range Action defined", remedialActionRegisteredResources.size())));
                    return true;
                }
                if (!pstRangeActionCreators.containsKey(createdRemedialActionId)
                    || !pstRangeActionCreators.get(createdRemedialActionId).getPstRangeActionCreationContext().isImported()) {
                    PstRangeActionCreator pstRangeActionCreator = new PstRangeActionCreator(crac, network,
                        createdRemedialActionId, createdRemedialActionName, applicationModeMarketObjectStatus, remedialActionRegisteredResource,
                        contingencies, invalidContingencies, flowCnecs, angleCnec, sharedDomain);
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
                crac, applicationModeMarketObjectStatus, adder, contingencies, invalidContingencies, flowCnecs, angleCnec, sharedDomain
            );
        } catch (OpenRaoImportException e) {
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
                        contingencies, invalidContingencies, flowCnecs, angleCnec, sharedDomain, cimCracCreationParameters);
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
                contingencies, invalidContingencies, flowCnecs, angleCnec, sharedDomain);
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
            return RemedialActionSeriesCreationContext.imported(createdRemedialActionId, true, String.format("Contingencies %s were not imported", contingencyList));
        }
    }

    public static RemedialActionSeriesCreationContext importPstRaWithContingencies(String createdRemedialActionId, String networkElementNativeMrid, String networkElementNativeName, List<String> invalidContingencies) {
        if (invalidContingencies.isEmpty()) {
            return PstRangeActionSeriesCreationContext.imported(createdRemedialActionId, networkElementNativeMrid, networkElementNativeName, false, "");
        } else {
            String contingencyList = StringUtils.join(invalidContingencies, ", ");
            return PstRangeActionSeriesCreationContext.imported(createdRemedialActionId, networkElementNativeMrid, networkElementNativeName, true, String.format("Contingencies %s were not imported", contingencyList));
        }
    }

    public static void checkPstUnit(String unitSymbol) {
        if (Objects.isNull(unitSymbol)) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, "Missing unit symbol");
        }
        if (!unitSymbol.equals(PST_CAPACITY_UNIT_SYMBOL)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong unit symbol in its registered resource: %s", unitSymbol));
        }
    }

    public static void addUsageRules(Crac crac, String applicationModeMarketObjectStatus,
                                     RemedialActionAdder<?> remedialActionAdder,
                                     List<Contingency> contingencies,
                                     List<String> invalidContingencies,
                                     Set<FlowCnec> flowCnecs,
                                     AngleCnec angleCnec,
                                     Country sharedDomain) {
        Instant curativeInstant = crac.getInstant(InstantKind.CURATIVE);
        if (applicationModeMarketObjectStatus.equals(ApplicationModeMarketObjectStatus.PRA.getStatus())) {
            addUsageRulesAtInstant(remedialActionAdder, contingencies, invalidContingencies, flowCnecs, angleCnec, sharedDomain, curativeInstant, crac.getPreventiveInstant());
        }
        if (applicationModeMarketObjectStatus.equals(ApplicationModeMarketObjectStatus.CRA.getStatus())) {
            addUsageRulesAtInstant(remedialActionAdder, contingencies, invalidContingencies, flowCnecs, angleCnec, sharedDomain, curativeInstant, curativeInstant);
        }
        if (applicationModeMarketObjectStatus.equals(ApplicationModeMarketObjectStatus.PRA_AND_CRA.getStatus())) {
            addUsageRulesAtInstant(remedialActionAdder, null, null, flowCnecs, angleCnec, sharedDomain, curativeInstant, crac.getPreventiveInstant());
            addUsageRulesAtInstant(remedialActionAdder, contingencies, invalidContingencies, flowCnecs, angleCnec, sharedDomain, curativeInstant, curativeInstant);
        }
        if (applicationModeMarketObjectStatus.equals(ApplicationModeMarketObjectStatus.AUTO.getStatus())) {
            addUsageRulesAtInstant(remedialActionAdder, contingencies, invalidContingencies, flowCnecs, angleCnec, sharedDomain, curativeInstant, crac.getInstant(InstantKind.AUTO));
        }
    }

    private static void addUsageRulesAtInstant(RemedialActionAdder<?> remedialActionAdder,
                                               List<Contingency> contingencies,
                                               List<String> invalidContingencies,
                                               Set<FlowCnec> flowCnecs,
                                               AngleCnec angleCnec,
                                               Country sharedDomain,
                                               Instant curativeInstant, Instant instant) {
        if (!flowCnecs.isEmpty()) {
            flowCnecs.forEach(flowCnec -> addOnFlowConstraintUsageRule(remedialActionAdder, flowCnec, instant));
            return;
        }
        if (Objects.nonNull(angleCnec)) {
            addOnAngleConstraintUsageRule(remedialActionAdder, angleCnec, curativeInstant);
            return;
        }
        UsageMethod usageMethod = instant.isAuto() ? UsageMethod.FORCED : UsageMethod.AVAILABLE;
        if (!Objects.isNull(sharedDomain)) {
            addOnFlowConstraintInCountryUsageRule(remedialActionAdder, contingencies, sharedDomain, instant, usageMethod);
            return;
        }

        checkUsageRulesContingencies(instant, contingencies, invalidContingencies);

        if (instant.isPreventive() ||
            instant.isCurative() && (contingencies == null || contingencies.isEmpty())) {
            addOnInstantUsageRules(remedialActionAdder, instant);
        } else {
            RemedialActionSeriesCreator.addOnStateUsageRules(remedialActionAdder, instant, usageMethod, contingencies);
        }
    }

    private static void checkUsageRulesContingencies(Instant instant, List<Contingency> contingencies, List<String> invalidContingencies) {
        switch (instant.getKind()) {
            case PREVENTIVE:
                if (Objects.nonNull(contingencies) && !contingencies.isEmpty()
                    || Objects.nonNull(invalidContingencies) && !invalidContingencies.isEmpty()) {
                    throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Cannot create a preventive remedial action associated to a contingency");
                }
                break;
            case AUTO:
                if (contingencies.isEmpty() && invalidContingencies.isEmpty()) {
                    throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Cannot create a free-to-use remedial action at instant '%s'", instant));
                }
                if (contingencies.isEmpty()) {
                    throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Contingencies are all invalid, and usage rule is on instant '%s'", instant));
                }
                break;
            case CURATIVE:
                if (contingencies.isEmpty() && !invalidContingencies.isEmpty()) {
                    throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Contingencies are all invalid, and usage rule is on instant '%s'", instant));
                }
                break;
            default:
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Cannot add usage rule on instant '%s'", instant));
        }
    }

    private static void addOnInstantUsageRules(RemedialActionAdder<?> adder, Instant raApplicationInstant) {
        adder.newOnInstantUsageRule()
            .withInstant(raApplicationInstant.getId())
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add();
    }

    private static void addOnStateUsageRules(RemedialActionAdder<?> adder, Instant raApplicationInstant, UsageMethod usageMethod, List<Contingency> contingencies) {
        contingencies.forEach(contingency ->
            adder.newOnContingencyStateUsageRule()
                .withInstant(raApplicationInstant.getId())
                .withUsageMethod(usageMethod)
                .withContingency(contingency.getId())
                .add());
    }

    private static void addOnFlowConstraintUsageRule(RemedialActionAdder<?> adder, FlowCnec flowCnec, Instant instant) {
        // Only allow PRAs with usage method OnFlowConstraint/OnAngleConstraint, for CNECs of instants PREVENTIVE & OUTAGE & CURATIVE
        // Only allow ARAs with usage method OnFlowConstraint/OnAngleConstraint, for CNECs of instant AUTO
        // Only allow CRAs with usage method OnFlowConstraint/OnAngleConstraint, for CNECs of instant CURATIVE

        if (flowCnec.getState().getInstant().comesBefore(instant)) {
            return;
        }
        adder.newOnConstraintUsageRule()
            .withCnec(flowCnec.getId())
            .withUsageMethod(instant.isAuto() ? UsageMethod.FORCED : UsageMethod.AVAILABLE)
            .withInstant(instant.getId())
            .add();
    }

    private static void addOnAngleConstraintUsageRule(RemedialActionAdder<?> adder, AngleCnec angleCnec, Instant instant) {
        adder.newOnConstraintUsageRule()
            .withCnec(angleCnec.getId())
            .withUsageMethod(instant.isAuto() ? UsageMethod.FORCED : UsageMethod.AVAILABLE)
            .withInstant(instant.getId())
            .add();
    }

    private static void addOnFlowConstraintInCountryUsageRule(RemedialActionAdder<?> remedialActionAdder, List<Contingency> contingencies, Country sharedDomain, Instant instant, UsageMethod usageMethod) {
        OnFlowConstraintInCountryAdder<?> onFlowConstraintInCountryAdder = remedialActionAdder
            .newOnFlowConstraintInCountryUsageRule()
            .withInstant(instant.getId())
            .withCountry(sharedDomain)
            .withUsageMethod(usageMethod);
        if (!Objects.isNull(contingencies) && !contingencies.isEmpty()) {
            contingencies.forEach(
                contingency -> onFlowConstraintInCountryAdder.withContingency(contingency.getId()).add());
        } else {
            onFlowConstraintInCountryAdder.add();
        }
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
        // A38 not handled by Open RAO.
        if (availabilityMarketObjectStatus.equals(AvailabilityMarketObjectStatus.SHALL_BE_USED.getStatus())) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO, String.format("Wrong availabilityMarketObjectStatus: %s", availabilityMarketObjectStatus)));
            return false;
        }
        if (!availabilityMarketObjectStatus.equals(AvailabilityMarketObjectStatus.MIGHT_BE_USED.getStatus())) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong availabilityMarketObjectStatus: %s", availabilityMarketObjectStatus)));
            return false;
        }
        return true;
    }

    private void resetSeriesContingencies() {
        contingencies.clear();
        invalidContingencies.clear();
    }

    private boolean describesRemedialActionsToImport(Series series) {
        return series.getBusinessType().equals(REMEDIAL_ACTIONS_SERIES_BUSINESS_TYPE);
    }
}
