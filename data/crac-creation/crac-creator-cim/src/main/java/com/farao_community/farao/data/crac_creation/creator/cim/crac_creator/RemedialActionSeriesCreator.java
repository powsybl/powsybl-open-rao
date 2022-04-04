/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.range_action.PstRangeActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.util.PstHelper;
import com.farao_community.farao.data.crac_creation.util.iidm.IidmPstHelper;
import com.powsybl.iidm.network.Network;
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
    private CimCracCreationContext cracCreationContext;

    RemedialActionSeriesCreator(List<TimeSeries> cimTimeSeries, Crac crac, Network network, CimCracCreationContext cracCreationContext) {
        this.cimTimeSeries = cimTimeSeries;
        this.crac = crac;
        this.network = network;
        this.cracCreationContext = cracCreationContext;
    }

    void createAndAddRemedialActionSeries() {
        this.remedialActionSeriesCreationContexts = new HashSet<>();

        for (TimeSeries cimTimeSerie : cimTimeSeries) {
            for (SeriesPeriod cimPeriodInTimeSerie : cimTimeSerie.getPeriod()) {
                for (Point cimPointInPeriodInTimeSerie : cimPeriodInTimeSerie.getPoint()) {
                    for (Series cimSerie : cimPointInPeriodInTimeSerie.getSeries().stream().filter(this::describesRemedialActionsToImport).collect(Collectors.toList())) {
                        if (checkRemedialActionSeries(cimSerie)) {                                                                            // Read and store contingencies
                            List<Contingency> contingencies = new ArrayList<>();
                            List<String> invalidContingencies = new ArrayList<>();
                            for (ContingencySeries cimContingency : cimSerie.getContingencySeries()) {
                                Optional<Contingency> contingency = getContingencyFromCrac(cimContingency, crac);
                                if (contingency.isPresent()) {
                                    contingencies.add(contingency.get());
                                } else {
                                    invalidContingencies.add(cimContingency.getMRID());
                                }
                            }
                            for (RemedialActionSeries remedialActionSeries : cimSerie.getRemedialActionSeries()) {
                                readAndAddRemedialAction(remedialActionSeries, contingencies, invalidContingencies);
                            }
                        }
                    }
                }
            }
        }
        this.cracCreationContext.setRemedialActionSeriesCreationContexts(remedialActionSeriesCreationContexts);
    }

    // For now, only free-to-use remedial actions are handled.
    private void readAndAddRemedialAction(RemedialActionSeries remedialActionSeries, List<Contingency> contingencies, List<String> invalidContingencies) {
        String createdRemedialActionId =  remedialActionSeries.getMRID();
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
            } else {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.NOT_YET_HANDLED_BY_FARAO, String.format("Only free to use remedial actions series are handled. Shared domain is: %s", remedialActionSeries.getSharedDomain().get(0).getMRID().getValue())));
                return;
            }
        }

        // --- Registered Resources
        List<RemedialActionRegisteredResource> remedialActionRegisteredResources = remedialActionSeries.getRegisteredResource();
        if (remedialActionRegisteredResources.isEmpty()) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, "Missing registered resource"));
            return;
        }
        if (remedialActionRegisteredResources.size() > 1) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("> 1 registered resources (%s)", remedialActionRegisteredResources.size())));
            return;
        }
        RemedialActionRegisteredResource remedialActionRegisteredResource = remedialActionRegisteredResources.get(0);
        if (!checkRemedialActionRegisteredResource(createdRemedialActionId, remedialActionRegisteredResource)) {
            return;
        }

        addRemedialAction(createdRemedialActionId, createdRemedialActionName, remedialActionRegisteredResource, applicationModeMarketObjectStatus, contingencies, invalidContingencies);
    }

    private void addRemedialAction(String createdRemedialActionId, String createdRemedialActionName, RemedialActionRegisteredResource remedialActionRegisteredResource, String applicationModeMarketObjectStatus, List<Contingency> contingencies, List<String> invalidContingencies) {
        // --- Market Object status: define RangeType
        String marketObjectStatusStatus = remedialActionRegisteredResource.getMarketObjectStatusStatus();
        RangeType rangeType;
        if (marketObjectStatusStatus.equals(PstRangeType.ABSOLUTE.getStatus())) {
            rangeType = RangeType.ABSOLUTE;
        } else if (marketObjectStatusStatus.equals(PstRangeType.RELATIVE_TO_INITIAL_NETWORK.getStatus())) {
            rangeType = RangeType.RELATIVE_TO_INITIAL_NETWORK;
        } else if (marketObjectStatusStatus.equals(PstRangeType.RELATIVE_TO_PREVIOUS_INSTANT1.getStatus()) || marketObjectStatusStatus.equals(PstRangeType.RELATIVE_TO_PREVIOUS_INSTANT2.getStatus())) {
            rangeType = RangeType.RELATIVE_TO_PREVIOUS_INSTANT;
        } else {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s", applicationModeMarketObjectStatus)));
            return;
        }

        // Add remedial action
        // --- For now, do not set operator, do not set group id.
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
            if (!checkUnit(createdRemedialActionId, unitSymbol)) {
                return;
            }
            addTapRangeWithMinAndMaxTap(pstRangeActionAdder, pstHelper, minCapacity, maxCapacity, rangeType);
        } else if (Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMaximumCapacity())) {
            int maxCapacity = remedialActionRegisteredResource.getResourceCapacityMaximumCapacity().intValue();
            if (!checkUnit(createdRemedialActionId, unitSymbol)) {
                return;
            }
            addTapRangeWithMaxTap(pstRangeActionAdder, pstHelper, maxCapacity, rangeType);
        } else if (Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMinimumCapacity())) {
            int minCapacity = remedialActionRegisteredResource.getResourceCapacityMinimumCapacity().intValue();
            if (!checkUnit(createdRemedialActionId, unitSymbol)) {
                return;
            }
            addTapRangeWithMinTap(pstRangeActionAdder, pstHelper, minCapacity, rangeType);
        }

        if (!addUsageRules(createdRemedialActionId, applicationModeMarketObjectStatus, pstRangeActionAdder, contingencies, invalidContingencies)) {
            return;
        }

        if (!invalidContingencies.isEmpty()) {
            String invalidContingencyList = StringUtils.join(invalidContingencies, ", ");
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.imported(createdRemedialActionId, true, String.format("Contingencies %s not defined in B55s", invalidContingencyList)));
        } else {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.imported(createdRemedialActionId, false, ""));
        }
        pstRangeActionAdder.add();
    }

    private boolean addUsageRules(String createdRemedialActionId, String applicationModeMarketObjectStatus, PstRangeActionAdder pstRangeActionAdder, List<Contingency> contingencies, List<String> invalidContingencies) {
        if (applicationModeMarketObjectStatus.equals(AuthorizedPstApplicationModeMarketObjectStatus.PRA.getStatus())) {
            if (contingencies.isEmpty() && invalidContingencies.isEmpty()) {
                addFreeToUseUsageRules(pstRangeActionAdder, Instant.PREVENTIVE);
            } else  {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "Cannot create a preventive remedial action associated to a contingency"));
                return false;
            }
        }
        if (applicationModeMarketObjectStatus.equals(AuthorizedPstApplicationModeMarketObjectStatus.CRA.getStatus())) {
            if (contingencies.isEmpty()) {
                if (!invalidContingencies.isEmpty()) {
                    remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "Contingencies are all invalid, and usage rule is on curative instant"));
                    return false;
                } else {
                    addFreeToUseUsageRules(pstRangeActionAdder, Instant.CURATIVE);
                }
            } else {
                addOnStateUsageRules(pstRangeActionAdder, Instant.CURATIVE, UsageMethod.AVAILABLE, contingencies);
            }
        }
        if (applicationModeMarketObjectStatus.equals(AuthorizedPstApplicationModeMarketObjectStatus.PRA_AND_CRA.getStatus())) {
            addFreeToUseUsageRules(pstRangeActionAdder, Instant.PREVENTIVE);
            if (invalidContingencies.isEmpty() && contingencies.isEmpty()) {
                addFreeToUseUsageRules(pstRangeActionAdder, Instant.CURATIVE);
            }
            if (!contingencies.isEmpty()) {
                addOnStateUsageRules(pstRangeActionAdder, Instant.CURATIVE, UsageMethod.AVAILABLE, contingencies);
            }
        }
        if (applicationModeMarketObjectStatus.equals(AuthorizedPstApplicationModeMarketObjectStatus.AUTO.getStatus())) {
            if (contingencies.isEmpty() && invalidContingencies.isEmpty()) {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "Cannot create a free-to-use remedial action at instant AUTO"));
                return false;
            } else if (contingencies.isEmpty()) {
                remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "Contingencies are all invalid, and usage rule is on AUTO instant"));
                return false;
            } else {
                addOnStateUsageRules(pstRangeActionAdder, Instant.AUTO, UsageMethod.FORCED, contingencies);
            }
        }
        return true;
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

    private boolean checkRemedialActionRegisteredResource(String createdRemedialActionId, RemedialActionRegisteredResource remedialActionRegisteredResource) {
        // Check PSR Type
        if (!checkPsrType(createdRemedialActionId, remedialActionRegisteredResource.getPSRTypePsrType())) {
            return false;
        }

        // Check that there isn't a resourceCapacity.defaultCapacity
        if (Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityDefaultCapacity())) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "ResourceCapacity.defaultCapacity shouldn't be defined"));
            return false;
        }

        if (Objects.isNull(remedialActionRegisteredResource.getMarketObjectStatusStatus())) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, "Missing marketObjectStatus"));
            return false;
        }
        return true;
    }

    private boolean checkUnit(String createdRemedialActionId, String unitSymbol) {
        if (Objects.isNull(unitSymbol)) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, "Missing unit symbol"));
            return false;
        }
        if (!unitSymbol.equals(PST_CAPACITY_UNIT_SYMBOL)) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong unit symbol in its registered resource: %s", unitSymbol)));
            return false;
        }
        return true;
    }

    private boolean checkBusinessType(String createdRemedialActionId, String businessType) {
        if (Objects.isNull(businessType) || !businessType.equals(PST_BUSINESS_TYPE)) {
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
        for (AuthorizedPstApplicationModeMarketObjectStatus value : AuthorizedPstApplicationModeMarketObjectStatus.values()) {
            if (applicationModeMarketObjectStatus.equals(value.getStatus())) {
                return true;
            }
        }
        remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong applicationMode_MarketObjectStatus: %s", applicationModeMarketObjectStatus)));
        return false;
    }

    private boolean checkAvailabilityMarketObjectStatus(String createdRemedialActionId, String availabilityMarketObjectStatus) {
        if (Objects.isNull(availabilityMarketObjectStatus)) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, "Missing availabilityMarketObjectStatus."));
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

    private boolean checkPsrType(String createdRemedialActionId, String psrType) {
        if (Objects.isNull(psrType)) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, "Missing psrType"));
            return false;
        }
        if (!psrType.equals(PST_PSR_TYPE)) {
            remedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong psrType: %s", psrType)));
            return false;
        }
        return true;
    }

    private void addFreeToUseUsageRules(PstRangeActionAdder adder, Instant raApplicationInstant) {
        adder.newFreeToUseUsageRule()
                .withInstant(raApplicationInstant)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add();
    }

    private void addOnStateUsageRules(PstRangeActionAdder adder, Instant raApplicationInstant, UsageMethod usageMethod, List<Contingency> contingencies) {
        contingencies.forEach(contingency ->
            adder.newOnStateUsageRule()
                .withInstant(raApplicationInstant)
                .withUsageMethod(usageMethod)
                .withContingency(contingency.getId())
                .add());
    }

    private boolean checkRemedialActionSeries(Series cimSerie) {
        // --- Check optimizationStatus
        String optimizationStatus = cimSerie.getOptimizationMarketObjectStatusStatus();
        if (Objects.nonNull(optimizationStatus)) {
            boolean statusOk = false;
            for (String status : PST_AUTHORIZED_OPTIMIZATION_STATUS) {
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

    private boolean describesRemedialActionsToImport(Series series) {
        return series.getBusinessType().equals(REMEDIAL_ACTIONS_SERIES_BUSINESS_TYPE);
    }
}
