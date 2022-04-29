/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.FlowCnecAdder;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdAdder;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.util.cgmes.CgmesBranchHelper;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.farao_community.farao.data.crac_creation.creator.cim.xsd.*;
import org.apache.commons.lang3.StringUtils;
import static com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracUtils.*;
import static com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimConstants.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Philippe Edwards <philippe.edwards at rte-france.com>
 */
public class MonitoredSeriesCreator {
    private final Crac crac;
    private final Network network;
    private final List<TimeSeries> cimTimeSeries;
    private Map<String, MonitoredSeriesCreationContext> monitoredSeriesCreationContexts;
    private CimCracCreationContext cracCreationContext;

    public Map<String, MonitoredSeriesCreationContext> getMonitoredSeriesCreationContexts() {
        return monitoredSeriesCreationContexts;
    }

    public MonitoredSeriesCreator(List<TimeSeries> cimTimeSeries, Crac crac, Network network, CimCracCreationContext cracCreationContext) {
        this.cimTimeSeries = cimTimeSeries;
        this.crac = crac;
        this.network = network;
        this.cracCreationContext = cracCreationContext;
    }

    public void createAndAddMonitoredSeries() {
        this.monitoredSeriesCreationContexts = new HashMap<>();

        for (TimeSeries cimTimeSerie : cimTimeSeries) {
            for (SeriesPeriod cimPeriodInTimeSerie : cimTimeSerie.getPeriod()) {
                for (Point cimPointInPeriodInTimeSerie : cimPeriodInTimeSerie.getPoint()) {
                    for (Series cimSerie : cimPointInPeriodInTimeSerie.getSeries().stream().filter(this::describesCnecsToImport).collect(Collectors.toList())) {
                        List<Contingency> contingencies = new ArrayList<>();
                        List<String> invalidContingencies = new ArrayList<>();
                        String optimizationStatus = cimSerie.getOptimizationMarketObjectStatusStatus();
                        for (ContingencySeries cimContingency : cimSerie.getContingencySeries()) {
                            Optional<Contingency> contingency = getContingencyFromCrac(cimContingency, crac);
                            if (contingency.isPresent()) {
                                contingencies.add(contingency.get());
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
                }
            }
        }
        this.cracCreationContext.setMonitoredSeriesCreationContexts(monitoredSeriesCreationContexts);
    }

    private void readAndAddCnec(MonitoredSeries monitoredSeries, List<Contingency> contingencies, String optimizationStatus, List<String> invalidContingencies) {
        String nativeId = monitoredSeries.getMRID();
        List<MonitoredRegisteredResource> monitoredRegisteredResources = monitoredSeries.getRegisteredResource();
        if (monitoredRegisteredResources.isEmpty()) {
            monitoredSeriesCreationContexts.put(nativeId, MonitoredSeriesCreationContext.notImported(nativeId, ImportStatus.INCOMPLETE_DATA, "No registered resources"));
            return;
        }
        if (monitoredRegisteredResources.size() > 1) {
            monitoredSeriesCreationContexts.put(nativeId, MonitoredSeriesCreationContext.notImported(nativeId, ImportStatus.INCONSISTENCY_IN_DATA, "More than one registered resources"));
            return;
        }

        MonitoredRegisteredResource monitoredRegisteredResource = monitoredRegisteredResources.get(0);
        String cnecId = monitoredRegisteredResource.getName();

        //Get network element
        CgmesBranchHelper branchHelper = new CgmesBranchHelper(monitoredRegisteredResource.getMRID().getValue(), network);
        if (branchHelper.getBranch() == null) {
            monitoredSeriesCreationContexts.put(nativeId, MonitoredSeriesCreationContext.notImported(nativeId, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK,
                String.format("Network element was not found in network: %s", monitoredRegisteredResource.getMRID().getValue())));
            return;
        }

        //Check if pure MNEC
        boolean isMnec;
        if (Objects.nonNull(optimizationStatus) && optimizationStatus.equals(CNECS_MNEC_MARKET_OBJECT_STATUS)) {
            isMnec = true;
        } else if (Objects.isNull(optimizationStatus) || optimizationStatus.equals(CNECS_OPTIMIZED_MARKET_OBJECT_STATUS)) {
            isMnec = false;
        } else {
            monitoredSeriesCreationContexts.put(nativeId, MonitoredSeriesCreationContext.notImported(nativeId, ImportStatus.INCONSISTENCY_IN_DATA,
                String.format("Unrecognized optimization_MarketObjectStatus.status: %s", optimizationStatus)));
            return;
        }

        MonitoredSeriesCreationContext monitoredSeriesCreationContext;
        if (invalidContingencies.isEmpty()) {
            monitoredSeriesCreationContext = MonitoredSeriesCreationContext.imported(nativeId, false, "");
        } else {
            String contingencyList = StringUtils.join(invalidContingencies, ", ");
            monitoredSeriesCreationContext = MonitoredSeriesCreationContext.imported(nativeId, true, String.format("Contingencies %s not defined in B55s", contingencyList));
        }
        monitoredSeriesCreationContexts.put(nativeId, monitoredSeriesCreationContext);

        //Read measurements
        for (Analog measurement : monitoredRegisteredResource.getMeasurements()) {
            Instant instant;
            switch (measurement.getMeasurementType()) {
                case CNECS_N_STATE_MEASUREMENT_TYPE:
                    instant = Instant.PREVENTIVE;
                    break;
                case CNECS_OUTAGE_STATE_MEASUREMENT_TYPE:
                    instant = Instant.OUTAGE;
                    break;
                case CNECS_AUTO_STATE_MEASUREMENT_TYPE:
                    instant = Instant.AUTO;
                    break;
                case CNECS_CURATIVE_STATE_MEASUREMENT_TYPE:
                    instant = Instant.CURATIVE;
                    break;
                default:
                    monitoredSeriesCreationContext.addMeasurementCreationContext(MeasurementCreationContext.notImported(
                        ImportStatus.INCONSISTENCY_IN_DATA, String.format("Unrecognized measurementType: %s", measurement.getMeasurementType())
                    ));
                    return;
            }

            Unit unit;
            switch (measurement.getUnitSymbol()) {
                case CNECS_PATL_UNIT_SYMBOL:
                    unit = Unit.PERCENT_IMAX;
                    break;
                case MEGAWATT_UNIT_SYMBOL:
                    unit = Unit.MEGAWATT;
                    break;
                case AMPERES_UNIT_SYMBOL:
                    unit = Unit.AMPERE;
                    break;
                default:
                    monitoredSeriesCreationContext.addMeasurementCreationContext(MeasurementCreationContext.notImported(
                        ImportStatus.INCONSISTENCY_IN_DATA, String.format("Unrecognized unitSymbol: %s", measurement.getUnitSymbol())
                    ));
                    return;
            }

            String direction = "both";
            if (Objects.nonNull(measurement.getPositiveFlowIn())) {
                switch (measurement.getPositiveFlowIn()) {
                    case CNECS_DIRECT_DIRECTION_FLOW:
                    case CNECS_OPPOSITE_DIRECTION_FLOW:
                        direction = measurement.getPositiveFlowIn();
                        break;
                    default:
                        monitoredSeriesCreationContext.addMeasurementCreationContext(MeasurementCreationContext.notImported(
                            ImportStatus.INCONSISTENCY_IN_DATA, String.format("Unrecognized positiveFlowIn: %s", measurement.getPositiveFlowIn())
                        ));
                        return;
                }
            }

            MeasurementCreationContext measurementCreationContext = MeasurementCreationContext.imported("");
            monitoredSeriesCreationContext.addMeasurementCreationContext(measurementCreationContext);

            addCnecs(measurementCreationContext, cnecId, branchHelper, isMnec, direction, unit, measurement.getAnalogValuesValue(), contingencies, instant);
        }
    }

    private void addCnecs(MeasurementCreationContext measurementCreationContext, String cnecNativeId, CgmesBranchHelper branchHelper,
                          boolean isMnec, String direction, Unit unit, float value, List<Contingency> contingencies, Instant instant) {
        List<Contingency> contingenciesOrPreventive;
        if (instant == Instant.PREVENTIVE) {
            contingenciesOrPreventive = new ArrayList<>();
            contingenciesOrPreventive.add(null);
        } else {
            contingenciesOrPreventive = contingencies;
        }
        for (Contingency contingency : contingenciesOrPreventive) {
            FlowCnecAdder flowCnecAdder = crac.newFlowCnec();
            BranchThresholdAdder branchThresholdAdder = flowCnecAdder.newThreshold();
            String cnecId = cnecNativeId;
            String contingencyId = Objects.isNull(contingency) ? "" : contingency.getId();

            flowCnecAdder.withNetworkElement(branchHelper.getBranch().getId());
            if (branchHelper.isTieLine()) {
                if (branchHelper.getTieLineSide() == Branch.Side.ONE) {
                    branchThresholdAdder.withRule(BranchThresholdRule.ON_LEFT_SIDE);
                    cnecId += " - LEFT";
                } else {
                    branchThresholdAdder.withRule(BranchThresholdRule.ON_RIGHT_SIDE);
                    cnecId += " - RIGHT";
                }
            } else {
                branchThresholdAdder.withRule(BranchThresholdRule.ON_REGULATED_SIDE);
            }

            double voltageLevelLeft = branchHelper.getBranch().getTerminal1().getVoltageLevel().getNominalV();
            double voltageLevelRight = branchHelper.getBranch().getTerminal2().getVoltageLevel().getNominalV();
            if (voltageLevelLeft > 1e-6 && voltageLevelRight > 1e-6) {
                flowCnecAdder.withNominalVoltage(voltageLevelLeft, Side.LEFT);
                flowCnecAdder.withNominalVoltage(voltageLevelRight, Side.RIGHT);
            } else {
                measurementCreationContext.addCnecCreationContext(contingencyId, instant, CnecCreationContext.notImported(
                    ImportStatus.OTHER, String.format("Voltage level for branch %s is 0 in network.", branchHelper.getBranch().getId())
                ));
                return;
            }
            Double currentLimitLeft = getCurrentLimit(branchHelper.getBranch(), Branch.Side.ONE);
            Double currentLimitRight = getCurrentLimit(branchHelper.getBranch(), Branch.Side.TWO);
            if (Objects.nonNull(currentLimitLeft) && Objects.nonNull(currentLimitRight)) {
                flowCnecAdder.withIMax(currentLimitLeft, Side.LEFT);
                flowCnecAdder.withIMax(currentLimitRight, Side.RIGHT);
            } else {
                measurementCreationContext.addCnecCreationContext(contingencyId, instant, CnecCreationContext.notImported(
                    ImportStatus.OTHER, String.format("Unable to get branch current limits from network for branch %s", branchHelper.getBranch().getId())
                ));
                return;
            }

            branchThresholdAdder.withUnit(unit);
            boolean isInverted = false;
            if (direction.equals(CNECS_DIRECT_DIRECTION_FLOW)) {
                branchThresholdAdder.withMax((double) value).add();
                cnecId += " - DIRECT";
            } else if (direction.equals(CNECS_OPPOSITE_DIRECTION_FLOW)) {
                branchThresholdAdder.withMin((double) -value).add();
                cnecId += " - OPPOSITE";
                isInverted = true;
            } else {
                branchThresholdAdder.withMax((double) value);
                branchThresholdAdder.withMin((double) -value).add();
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
                measurementCreationContext.addCnecCreationContext(contingencyId, instant, CnecCreationContext.imported(
                    cnecId, isInverted, ""
                ));
                flowCnecAdder.withId(cnecId);
                flowCnecAdder.withName(cnecId).add();
            }
        }
    }

    private boolean describesCnecsToImport(Series series) {
        return series.getBusinessType().equals(CNECS_SERIES_BUSINESS_TYPE);
    }

    // This uses the same logic as the UcteCnecElementHelper which is used for CBCO cnec import for instance
    private Double getCurrentLimit(Branch branch, Branch.Side side) {

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
