/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.cim.craccreator;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.io.cim.xsd.Analog;
import com.powsybl.openrao.data.crac.io.cim.xsd.ContingencySeries;
import com.powsybl.openrao.data.crac.io.cim.xsd.MonitoredRegisteredResource;
import com.powsybl.openrao.data.crac.io.cim.xsd.MonitoredSeries;
import com.powsybl.openrao.data.crac.io.cim.xsd.Series;
import com.powsybl.openrao.data.crac.io.cim.xsd.TimeSeries;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.commons.cgmes.CgmesBranchHelper;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.io.commons.iidm.IidmCnecElementHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.crac.io.cim.craccreator.CimConstants.*;
import static com.powsybl.openrao.data.crac.io.cim.craccreator.CimCracUtils.getContingencyFromCrac;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MonitoredSeriesCreator {
    private final Crac crac;
    private final Network network;
    private final List<TimeSeries> cimTimeSeries;
    private Map<String, MonitoredSeriesCreationContext> monitoredSeriesCreationContexts;
    private final CimCracCreationContext cracCreationContext;
    private final Set<TwoSides> defaultMonitoredSides;

    public MonitoredSeriesCreator(List<TimeSeries> cimTimeSeries, Network network, CimCracCreationContext cracCreationContext, Set<TwoSides> defaultMonitoredSides) {
        this.cimTimeSeries = cimTimeSeries;
        this.crac = cracCreationContext.getCrac();
        this.network = network;
        this.cracCreationContext = cracCreationContext;
        this.defaultMonitoredSides = defaultMonitoredSides;
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
        CimCracUtils.applyActionToEveryPoint(
            cimTimeSeries,
            cracCreationContext.getTimeStamp().toInstant(),
            point -> point.getSeries().stream().filter(this::describesCnecsToImport).forEach(cnecSeries::add)
        );
        return cnecSeries;
    }

    private boolean describesCnecsToImport(Series series) {
        // Read CNECs from B57 or B56 series
        // WARNING: if the same CNEC is defined in multiple places, but with different information (e.g. different
        // thresholds), the imported CNEC will be unpredictable
        return series.getBusinessType().equals(CNECS_SERIES_BUSINESS_TYPE) || series.getBusinessType().equals(REMEDIAL_ACTIONS_SERIES_BUSINESS_TYPE);
    }

    private void readAndAddCnec(MonitoredSeries monitoredSeries, List<Contingency> contingencies, String optimizationStatus, List<String> invalidContingencies) {
        String nativeId = monitoredSeries.getMRID();
        String nativeName = monitoredSeries.getName();
        List<MonitoredRegisteredResource> monitoredRegisteredResources = monitoredSeries.getRegisteredResource();
        if (monitoredRegisteredResources.isEmpty()) {
            saveMonitoredSeriesCreationContexts(nativeId, MonitoredSeriesCreationContext.notImported(nativeId, nativeName, null, null, ImportStatus.INCOMPLETE_DATA, "No registered resources"));
            return;
        }
        if (monitoredRegisteredResources.size() > 1) {
            saveMonitoredSeriesCreationContexts(nativeId, MonitoredSeriesCreationContext.notImported(nativeId, nativeName, null, null, ImportStatus.INCONSISTENCY_IN_DATA, "More than one registered resources"));
            return;
        }

        MonitoredRegisteredResource monitoredRegisteredResource = monitoredRegisteredResources.get(0);
        String cnecId = monitoredRegisteredResource.getName();
        String resourceId = monitoredRegisteredResource.getMRID().getValue();
        String resourceName = monitoredRegisteredResource.getName();

        //Get network element
        CgmesBranchHelper branchHelper = new CgmesBranchHelper(monitoredRegisteredResource.getMRID().getValue(), network);
        if (!branchHelper.isValid()) {
            saveMonitoredSeriesCreationContexts(nativeId, MonitoredSeriesCreationContext.notImported(nativeId, nativeName, resourceId, resourceName,
                ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("Network element was not found in network: %s", monitoredRegisteredResource.getMRID().getValue())));
            return;
        }

        // Check if pure MNEC
        boolean isMnec;
        try {
            isMnec = isMnec(optimizationStatus);
        } catch (OpenRaoException e) {
            saveMonitoredSeriesCreationContexts(nativeId,
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
                true, String.format("Contingencies %s were not imported", contingencyList));
        }

        // Read measurements
        monitoredRegisteredResource.getMeasurements().forEach(
            measurement -> monitoredSeriesCreationContext.addMeasurementCreationContext(
                createCnecFromMeasurement(measurement, cnecId, isMnec, branchHelper, contingencies)
            )
        );

        saveMonitoredSeriesCreationContexts(nativeId, monitoredSeriesCreationContext);
    }

    private void saveMonitoredSeriesCreationContexts(String nativeId, MonitoredSeriesCreationContext mscc) {
        MonitoredSeriesCreationContext newMscc = mscc;
        if (monitoredSeriesCreationContexts.containsKey(nativeId)) {
            cracCreationContext.getCreationReport().warn(
                String.format("Multiple Monitored_Series with same mRID \"%s\" detected; they will be merged.", nativeId)
            );
            // TSO can define multiple Monitored_Series with same mRID. Add information from new one to old one
            MonitoredSeriesCreationContext mscc2 = monitoredSeriesCreationContexts.get(nativeId);
            ImportStatus importStatus = null;
            boolean isAltered = false;
            if (mscc.isImported() == mscc2.isImported()) {
                importStatus = mscc.getImportStatus();
            } else {
                importStatus = ImportStatus.IMPORTED;
                isAltered = true;
            }
            String importStatusDetail =
                mscc.getImportStatusDetail()
                    + (!mscc.getImportStatusDetail().isEmpty() && !mscc2.getImportStatusDetail().isEmpty() ? " - " : "")
                    + mscc2.getImportStatusDetail();
            newMscc = new MonitoredSeriesCreationContext(
                mscc.getNativeId(),
                mscc.getNativeName(),
                mscc.getNativeResourceId(),
                mscc.getNativeResourceName(),
                importStatus,
                mscc.isAltered() || mscc2.isAltered() || isAltered,
                importStatusDetail);
            newMscc.addMeasurementCreationContexts(mscc.getMeasurementCreationContexts());
            newMscc.addMeasurementCreationContexts(mscc2.getMeasurementCreationContexts());
        }
        monitoredSeriesCreationContexts.put(nativeId, newMscc);
    }

    private boolean isMnec(String optimizationStatus) {
        return Objects.nonNull(optimizationStatus) && optimizationStatus.equals(CNECS_MNEC_MARKET_OBJECT_STATUS);
    }

    private InstantKind getMeasurementInstant(Analog measurement) {
        return switch (measurement.getMeasurementType()) {
            case CNECS_N_STATE_MEASUREMENT_TYPE -> InstantKind.PREVENTIVE;
            case CNECS_OUTAGE_STATE_MEASUREMENT_TYPE -> InstantKind.OUTAGE;
            case CNECS_AUTO_STATE_MEASUREMENT_TYPE -> InstantKind.AUTO;
            case CNECS_CURATIVE_STATE_MEASUREMENT_TYPE -> InstantKind.CURATIVE;
            default ->
                throw new OpenRaoException(String.format("Unrecognized measurementType: %s", measurement.getMeasurementType()));
        };
    }

    private Unit getMeasurementUnit(Analog measurement) {
        return switch (measurement.getUnitSymbol()) {
            case CNECS_PATL_UNIT_SYMBOL -> Unit.PERCENT_IMAX;
            case MEGAWATT_UNIT_SYMBOL -> Unit.MEGAWATT;
            case AMPERES_UNIT_SYMBOL -> Unit.AMPERE;
            default ->
                throw new OpenRaoException(String.format("Unrecognized unitSymbol: %s", measurement.getUnitSymbol()));
        };
    }

    private String getMeasurementDirection(Analog measurement) {
        if (Objects.isNull(measurement.getPositiveFlowIn())) {
            return "both";
        }
        if (measurement.getPositiveFlowIn().equals(CNECS_DIRECT_DIRECTION_FLOW)
            || measurement.getPositiveFlowIn().equals(CNECS_OPPOSITE_DIRECTION_FLOW)) {
            return measurement.getPositiveFlowIn();
        }
        throw new OpenRaoException(String.format("Unrecognized positiveFlowIn: %s", measurement.getPositiveFlowIn()));
    }

    private MeasurementCreationContext createCnecFromMeasurement(Analog measurement, String cnecNativeId, boolean isMnec, CgmesBranchHelper branchHelper, List<Contingency> contingencies) {
        Instant instant;
        Unit unit;
        String direction;
        double threshold;
        try {
            instant = crac.getInstant(getMeasurementInstant(measurement));
            unit = getMeasurementUnit(measurement);
            direction = getMeasurementDirection(measurement);
            threshold = (unit.equals(Unit.PERCENT_IMAX) ? 0.01 : 1) * measurement.getAnalogValuesValue(); // Open RAO uses relative convention for %Imax (0 <= threshold <= 1)
        } catch (OpenRaoException e) {
            return MeasurementCreationContext.notImported(ImportStatus.INCONSISTENCY_IN_DATA, e.getMessage());
        }

        MeasurementCreationContext measurementCreationContext = MeasurementCreationContext.imported();

        FlowCnecAdder flowCnecAdder = crac.newFlowCnec()
            .withInstant(instant.getId())
            .withNetworkElement(branchHelper.getBranch().getId());

        String cnecId;
        try {
            cnecId = addThreshold(flowCnecAdder, unit, branchHelper, cnecNativeId, direction, threshold);
            setNominalVoltage(flowCnecAdder, branchHelper);
            IidmCnecElementHelper cnecElementHelper = new IidmCnecElementHelper(branchHelper.getBranch().getId(), network);
            flowCnecAdder.withIMax(cnecElementHelper.getCurrentLimit(TwoSides.ONE), TwoSides.ONE);
            flowCnecAdder.withIMax(cnecElementHelper.getCurrentLimit(TwoSides.TWO), TwoSides.TWO);
        } catch (OpenRaoException e) {
            if (instant.isPreventive()) {
                measurementCreationContext.addCnecCreationContext(null, instant, CnecCreationContext.notImported(ImportStatus.OTHER, e.getMessage()));
            } else {
                contingencies.forEach(contingency ->
                    measurementCreationContext.addCnecCreationContext(contingency.getId(), instant, CnecCreationContext.notImported(ImportStatus.OTHER, e.getMessage()))
                );
            }
            return measurementCreationContext;
        }

        if (isMnec) {
            flowCnecAdder.withMonitored();
            cnecId += " - MONITORED";
        } else {
            flowCnecAdder.withOptimized();
        }

        if (instant.isPreventive()) {
            addCnecsOnState(flowCnecAdder, cnecId, null, instant, measurementCreationContext, branchHelper.getIdInNetwork());
        } else {
            String finalCnecId = cnecId;
            contingencies.forEach(contingency -> {
                String contingencyId = contingency.getId();
                flowCnecAdder.withContingency(contingencyId);
                String cnecIdWithContingency = finalCnecId + " - " + contingencyId;
                addCnecsOnState(flowCnecAdder, cnecIdWithContingency, contingency, instant, measurementCreationContext, branchHelper.getIdInNetwork());
            });
        }

        return measurementCreationContext;
    }

    private void addCnecsOnState(FlowCnecAdder flowCnecAdder, String cnecIdWithContingency, Contingency contingency, Instant instant, MeasurementCreationContext measurementCreationContext, String networkElementId) {
        String contingencyId = Objects.isNull(contingency) ? "" : contingency.getId();
        String fullCnecId = cnecIdWithContingency + " - " + instant.getId();

        if (Objects.isNull(crac.getFlowCnec(fullCnecId))) {
            flowCnecAdder.withId(fullCnecId);
            flowCnecAdder.withName(fullCnecId).add();
        } else {
            // If a CNEC with the same ID has already been created, we assume that the 2 CNECs are the same
            // (we know network element and state are the same, we assume that thresholds are the same.
            // This is true if the TSO is consistent in the definition of its CNECs; and two different TSOs can only
            // share tielines, but those are distinguished by the TWO/ONE label)
            cracCreationContext.getCreationReport().warn(
                String.format("Multiple CNECs on same network element (%s) and same state (%s%s%s) have been detected. Only one CNEC will be created.", networkElementId, contingencyId, Objects.isNull(contingency) ? "" : " - ", instant)
            );
        }
        measurementCreationContext.addCnecCreationContext(contingencyId, instant, CnecCreationContext.imported(fullCnecId));
    }

    private String addThreshold(FlowCnecAdder flowCnecAdder, Unit unit, CgmesBranchHelper branchHelper, String cnecId, String direction, double threshold) {
        String modifiedCnecId = cnecId;

        Set<TwoSides> monitoredSides = defaultMonitoredSides;
        if (branchHelper.isHalfLine()) {
            modifiedCnecId += " - " + (branchHelper.getTieLineSide() == TwoSides.ONE ? "ONE" : "TWO");
            monitoredSides = Set.of(branchHelper.getTieLineSide());
        } else if (unit.equals(Unit.AMPERE) &&
            Math.abs(branchHelper.getBranch().getTerminal1().getVoltageLevel().getNominalV() - branchHelper.getBranch().getTerminal2().getVoltageLevel().getNominalV()) > 1.) {
            // If unit is absolute amperes, monitor low voltage side
            monitoredSides = branchHelper.getBranch().getTerminal1().getVoltageLevel().getNominalV() <= branchHelper.getBranch().getTerminal2().getVoltageLevel().getNominalV() ?
                Set.of(TwoSides.ONE) : Set.of(TwoSides.TWO);
        } else if (unit.equals(Unit.PERCENT_IMAX)) {
            // If unit is %Imax, check that Imax exists
            monitoredSides = monitoredSides.stream().filter(side -> hasCurrentLimit(branchHelper.getBranch(), side)).collect(Collectors.toSet());
            if (monitoredSides.isEmpty()) {
                throw new OpenRaoException(String.format("Cannot create any PERCENT_IMAX threshold on branch %s, as it holds no current limit at the wanted side", branchHelper.getIdInNetwork()));
            }
        }

        Double min = -threshold;
        Double max = threshold;
        if (direction.equals(CNECS_DIRECT_DIRECTION_FLOW)) {
            modifiedCnecId += " - DIRECT";
            min = null;
        } else if (direction.equals(CNECS_OPPOSITE_DIRECTION_FLOW)) {
            modifiedCnecId += " - OPPOSITE";
            max = null;
        }

        addThreshold(flowCnecAdder, unit, min, max, monitoredSides);
        return modifiedCnecId;
    }

    private void addThreshold(FlowCnecAdder flowCnecAdder, Unit unit, Double min, Double max, Set<TwoSides> sides) {
        sides.forEach(side ->
            flowCnecAdder.newThreshold()
                .withUnit(unit)
                .withSide(side)
                .withMax(max)
                .withMin(min)
                .add()
        );
    }

    private void setNominalVoltage(FlowCnecAdder flowCnecAdder, CgmesBranchHelper branchHelper) {
        double voltageLevelLeft = branchHelper.getBranch().getTerminal1().getVoltageLevel().getNominalV();
        double voltageLevelRight = branchHelper.getBranch().getTerminal2().getVoltageLevel().getNominalV();
        if (voltageLevelLeft > 1e-6 && voltageLevelRight > 1e-6) {
            flowCnecAdder.withNominalVoltage(voltageLevelLeft, TwoSides.ONE);
            flowCnecAdder.withNominalVoltage(voltageLevelRight, TwoSides.TWO);
        } else {
            throw new OpenRaoException(String.format("Voltage level for branch %s is 0 in network.", branchHelper.getBranch().getId()));
        }
    }

    private boolean hasCurrentLimit(Branch<?> branch, TwoSides side) {
        return branch.getCurrentLimits(side).isPresent();
    }
}
