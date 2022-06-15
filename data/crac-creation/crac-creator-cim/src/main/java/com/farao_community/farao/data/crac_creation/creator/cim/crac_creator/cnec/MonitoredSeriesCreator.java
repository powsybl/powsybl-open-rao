/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnecAdder;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdAdder;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.xsd.*;
import com.farao_community.farao.data.crac_creation.util.cgmes.CgmesBranchHelper;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimConstants.*;
import static com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracUtils.getContingencyFromCrac;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MonitoredSeriesCreator {
    private final Crac crac;
    private final Network network;
    private final List<TimeSeries> cimTimeSeries;
    private Map<String, MonitoredSeriesCreationContext> monitoredSeriesCreationContexts;
    private CimCracCreationContext cracCreationContext;

    public MonitoredSeriesCreator(List<TimeSeries> cimTimeSeries, Network network, CimCracCreationContext cracCreationContext) {
        this.cimTimeSeries = cimTimeSeries;
        this.crac = cracCreationContext.getCrac();
        this.network = network;
        this.cracCreationContext = cracCreationContext;
    }

    public void createAndAddMonitoredSeries() {
        this.monitoredSeriesCreationContexts = new HashMap<>();

        for (Series cimSerie : getCnecSeries()) {
            List<Contingency> contingencies = new ArrayList<>();
            List<String> invalidContingencies = new ArrayList<>();
            String optimizationStatus = cimSerie.getOptimizationMarketObjectStatusStatus();
            for (ContingencySeries cimContingency : cimSerie.getContingencySeries()) {
                Contingency contingency = getContingencyFromCrac(cimContingency, cracCreationContext);
                if (Objects.nonNull(contingency)) {
                    contingencies.add(contingency);
                } else {
                    invalidContingencies.add(cimContingency.getMRID());
                }
            }
            if (cimSerie.getContingencySeries().isEmpty()) {
                contingencies = new ArrayList<>(crac.getContingencies());
            }
            for (MonitoredSeries monitoredSeries : cimSerie.getMonitoredSeries()) {
                readAndAddCnec(monitoredSeries, contingencies, optimizationStatus, invalidContingencies);
            }
        }

        this.cracCreationContext.setMonitoredSeriesCreationContexts(monitoredSeriesCreationContexts);
    }

    private Set<Series> getCnecSeries() {
        Set<Series> cnecSeries = new HashSet<>();
        cimTimeSeries.forEach(
            timeSerie -> timeSerie.getPeriod().forEach(
                period -> period.getPoint().forEach(
                    point -> point.getSeries().stream().filter(this::describesCnecsToImport).forEach(cnecSeries::add)
                )));
        return cnecSeries;
    }

    private boolean describesCnecsToImport(Series series) {
        return series.getBusinessType().equals(CNECS_SERIES_BUSINESS_TYPE);
    }

    private void readAndAddCnec(MonitoredSeries monitoredSeries, List<Contingency> contingencies, String optimizationStatus, List<String> invalidContingencies) {
        String nativeId = monitoredSeries.getMRID();
        String nativeName = monitoredSeries.getName();
        List<MonitoredRegisteredResource> monitoredRegisteredResources = monitoredSeries.getRegisteredResource();
        if (monitoredRegisteredResources.isEmpty()) {
            monitoredSeriesCreationContexts.put(nativeId, MonitoredSeriesCreationContext.notImported(nativeId, nativeName, null, null, ImportStatus.INCOMPLETE_DATA, "No registered resources"));
            return;
        }
        if (monitoredRegisteredResources.size() > 1) {
            monitoredSeriesCreationContexts.put(nativeId, MonitoredSeriesCreationContext.notImported(nativeId, nativeName, null, null, ImportStatus.INCONSISTENCY_IN_DATA, "More than one registered resources"));
            return;
        }

        MonitoredRegisteredResource monitoredRegisteredResource = monitoredRegisteredResources.get(0);
        String cnecId = monitoredRegisteredResource.getName();
        String resourceId = monitoredRegisteredResource.getMRID().getValue();
        String resourceName = monitoredRegisteredResource.getName();

        //Get network element
        CgmesBranchHelper branchHelper = new CgmesBranchHelper(monitoredRegisteredResource.getMRID().getValue(), network);
        if (branchHelper.getBranch() == null) {
            monitoredSeriesCreationContexts.put(nativeId, MonitoredSeriesCreationContext.notImported(nativeId, nativeName, resourceId, resourceName,
                ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("Network element was not found in network: %s", monitoredRegisteredResource.getMRID().getValue())));
            return;
        }

        // Check if pure MNEC
        boolean isMnec;
        try {
            isMnec = isMnec(optimizationStatus);
        } catch (FaraoException e) {
            monitoredSeriesCreationContexts.put(nativeId,
                MonitoredSeriesCreationContext.notImported(nativeId, nativeName, resourceId, resourceName,
                    ImportStatus.INCONSISTENCY_IN_DATA, e.getMessage())
            );
            return;
        }

        MonitoredSeriesCreationContext monitoredSeriesCreationContext;
        if (invalidContingencies.isEmpty()) {
            monitoredSeriesCreationContext = MonitoredSeriesCreationContext.imported(nativeId, nativeName, resourceId, resourceName, false, "");
        } else {
            String contingencyList = StringUtils.join(invalidContingencies, ", ");
            monitoredSeriesCreationContext = MonitoredSeriesCreationContext.imported(nativeId, nativeName, resourceId, resourceName,
                true, String.format("Contingencies %s not defined in B55s", contingencyList));
        }
        monitoredSeriesCreationContexts.put(nativeId, monitoredSeriesCreationContext);

        // Read measurements
        monitoredRegisteredResource.getMeasurements().forEach(
            measurement -> monitoredSeriesCreationContext.addMeasurementCreationContext(
                createCnecFromMeasurement(measurement, cnecId, isMnec, branchHelper, contingencies)
            )
        );
    }

    private boolean isMnec(String optimizationStatus) {
        if (Objects.nonNull(optimizationStatus) && optimizationStatus.equals(CNECS_MNEC_MARKET_OBJECT_STATUS)) {
            return true;
        } else if (Objects.isNull(optimizationStatus) || optimizationStatus.equals(CNECS_OPTIMIZED_MARKET_OBJECT_STATUS)) {
            return false;
        } else {
            throw new FaraoException(String.format("Unrecognized optimization_MarketObjectStatus.status: %s", optimizationStatus));
        }
    }

    private MeasurementCreationContext createCnecFromMeasurement(Analog measurement, String cnecId, boolean isMnec, CgmesBranchHelper branchHelper, List<Contingency> contingencies) {
        Instant instant;
        Unit unit;
        String direction;
        try {
            instant = getMeasurementInstant(measurement);
            unit = getMeasurementUnit(measurement);
            direction = getMeasurementDirection(measurement);
        } catch (FaraoException e) {
            return MeasurementCreationContext.notImported(ImportStatus.INCONSISTENCY_IN_DATA, e.getMessage());
        }

        return addCnecs(cnecId, branchHelper, isMnec, direction, unit, measurement.getAnalogValuesValue(), contingencies, instant);
    }

    private Instant getMeasurementInstant(Analog measurement) {
        switch (measurement.getMeasurementType()) {
            case CNECS_N_STATE_MEASUREMENT_TYPE:
                return Instant.PREVENTIVE;
            case CNECS_OUTAGE_STATE_MEASUREMENT_TYPE:
                return Instant.OUTAGE;
            case CNECS_AUTO_STATE_MEASUREMENT_TYPE:
                return Instant.AUTO;
            case CNECS_CURATIVE_STATE_MEASUREMENT_TYPE:
                return Instant.CURATIVE;
            default:
                throw new FaraoException(String.format("Unrecognized measurementType: %s", measurement.getMeasurementType()));
        }
    }

    private Unit getMeasurementUnit(Analog measurement) {
        switch (measurement.getUnitSymbol()) {
            case CNECS_PATL_UNIT_SYMBOL:
                return Unit.PERCENT_IMAX;
            case MEGAWATT_UNIT_SYMBOL:
                return Unit.MEGAWATT;
            case AMPERES_UNIT_SYMBOL:
                return Unit.AMPERE;
            default:
                throw new FaraoException(String.format("Unrecognized unitSymbol: %s", measurement.getUnitSymbol()));
        }
    }

    private String getMeasurementDirection(Analog measurement) {
        if (Objects.isNull(measurement.getPositiveFlowIn())) {
            return "both";
        }
        if (measurement.getPositiveFlowIn().equals(CNECS_DIRECT_DIRECTION_FLOW)
            || measurement.getPositiveFlowIn().equals(CNECS_OPPOSITE_DIRECTION_FLOW)) {
            return measurement.getPositiveFlowIn();
        }
        throw new FaraoException(String.format("Unrecognized positiveFlowIn: %s", measurement.getPositiveFlowIn()));
    }

    private MeasurementCreationContext addCnecs(String cnecNativeId, CgmesBranchHelper branchHelper,
                                                boolean isMnec, String direction, Unit unit, float threshold,
                                                List<Contingency> contingencies, Instant instant) {
        MeasurementCreationContext measurementCreationContext = MeasurementCreationContext.imported();
        if (instant == Instant.PREVENTIVE) {
            addCnecsOnContingency(cnecNativeId, branchHelper, isMnec, direction, unit, threshold, null, instant, measurementCreationContext);
        } else {
            contingencies.forEach(contingency ->
                addCnecsOnContingency(cnecNativeId, branchHelper, isMnec, direction, unit, threshold, contingency, instant, measurementCreationContext)
            );
        }
        return measurementCreationContext;
    }

    private void addCnecsOnContingency(String cnecNativeId, CgmesBranchHelper branchHelper,
                                       boolean isMnec, String direction, Unit unit, float threshold,
                                       Contingency contingency, Instant instant, MeasurementCreationContext measurementCreationContext) {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec();
        String contingencyId = Objects.isNull(contingency) ? "" : contingency.getId();

        flowCnecAdder.withNetworkElement(branchHelper.getBranch().getId());

        String cnecId = addThreshold(flowCnecAdder, unit, branchHelper, cnecNativeId, direction, threshold);

        try {
            setNominalVoltage(flowCnecAdder, branchHelper);
            setCurrentsLimit(flowCnecAdder, branchHelper);
        } catch (FaraoException e) {
            measurementCreationContext.addCnecCreationContext(contingencyId, instant,
                CnecCreationContext.notImported(ImportStatus.OTHER, e.getMessage())
            );
            return;
        }

        if (isMnec) {
            flowCnecAdder.withMonitored();
            cnecId += " - MONITORED";
        } else {
            flowCnecAdder.withOptimized();
        }

        if (instant != Instant.PREVENTIVE) {
            flowCnecAdder.withContingency(contingencyId);
            cnecId += " - " + contingencyId;
        }
        flowCnecAdder.withInstant(instant);
        cnecId += " - " + instant.toString();

        if (Objects.nonNull(crac.getFlowCnec(cnecId))) {
            measurementCreationContext.addCnecCreationContext(contingencyId, instant, CnecCreationContext.notImported(
                ImportStatus.OTHER, String.format("A flow cnec with id %s already exists.", cnecId)
            ));
        } else {
            measurementCreationContext.addCnecCreationContext(contingencyId, instant, CnecCreationContext.imported(cnecId));
            flowCnecAdder.withId(cnecId);
            flowCnecAdder.withName(cnecId).add();
        }
    }

    private String addThreshold(FlowCnecAdder flowCnecAdder, Unit unit, CgmesBranchHelper branchHelper, String cnecId, String direction, float threshold) {
        BranchThresholdAdder branchThresholdAdder = flowCnecAdder.newThreshold();
        branchThresholdAdder.withUnit(unit);
        String modifiedCnecId = cnecId;
        if (branchHelper.isTieLine()) {
            if (branchHelper.getTieLineSide() == Branch.Side.ONE) {
                branchThresholdAdder.withRule(BranchThresholdRule.ON_LEFT_SIDE);
                modifiedCnecId += " - LEFT";
            } else {
                branchThresholdAdder.withRule(BranchThresholdRule.ON_RIGHT_SIDE);
                modifiedCnecId += " - RIGHT";
            }
        } else {
            branchThresholdAdder.withRule(BranchThresholdRule.ON_REGULATED_SIDE);
        }

        if (direction.equals(CNECS_DIRECT_DIRECTION_FLOW)) {
            branchThresholdAdder.withMax((double) threshold);
            modifiedCnecId += " - DIRECT";
        } else if (direction.equals(CNECS_OPPOSITE_DIRECTION_FLOW)) {
            branchThresholdAdder.withMin((double) -threshold);
            modifiedCnecId += " - OPPOSITE";
        } else {
            branchThresholdAdder.withMax((double) threshold);
            branchThresholdAdder.withMin((double) -threshold);
        }
        branchThresholdAdder.add();
        return modifiedCnecId;
    }

    private void setNominalVoltage(FlowCnecAdder flowCnecAdder, CgmesBranchHelper branchHelper) {
        double voltageLevelLeft = branchHelper.getBranch().getTerminal1().getVoltageLevel().getNominalV();
        double voltageLevelRight = branchHelper.getBranch().getTerminal2().getVoltageLevel().getNominalV();
        if (voltageLevelLeft > 1e-6 && voltageLevelRight > 1e-6) {
            flowCnecAdder.withNominalVoltage(voltageLevelLeft, Side.LEFT);
            flowCnecAdder.withNominalVoltage(voltageLevelRight, Side.RIGHT);
        } else {
            throw new FaraoException(String.format("Voltage level for branch %s is 0 in network.", branchHelper.getBranch().getId()));
        }
    }

    private void setCurrentsLimit(FlowCnecAdder flowCnecAdder, CgmesBranchHelper branchHelper) {
        Double currentLimitLeft = getCurrentLimit(branchHelper.getBranch(), Branch.Side.ONE);
        Double currentLimitRight = getCurrentLimit(branchHelper.getBranch(), Branch.Side.TWO);
        if (Objects.nonNull(currentLimitLeft) && Objects.nonNull(currentLimitRight)) {
            flowCnecAdder.withIMax(currentLimitLeft, Side.LEFT);
            flowCnecAdder.withIMax(currentLimitRight, Side.RIGHT);
        } else {
            throw new FaraoException(String.format("Unable to get branch current limits from network for branch %s", branchHelper.getBranch().getId()));
        }
    }

    // This uses the same logic as the UcteCnecElementHelper which is used for CBCO cnec import for instance
    private Double getCurrentLimit(Branch<?> branch, Branch.Side side) {

        if (!Objects.isNull(branch.getCurrentLimits(side))) {
            return branch.getCurrentLimits(side).getPermanentLimit();
        }

        if (side == Branch.Side.ONE && Objects.nonNull(branch.getCurrentLimits(Branch.Side.TWO))) {
            return branch.getCurrentLimits(Branch.Side.TWO).getPermanentLimit() * branch.getTerminal1().getVoltageLevel().getNominalV() / branch.getTerminal2().getVoltageLevel().getNominalV();
        }

        if (side == Branch.Side.TWO && Objects.nonNull(branch.getCurrentLimits(Branch.Side.ONE))) {
            return branch.getCurrentLimits(Branch.Side.ONE).getPermanentLimit() * branch.getTerminal2().getVoltageLevel().getNominalV() / branch.getTerminal1().getVoltageLevel().getNominalV();
        }

        return null;
    }
}
