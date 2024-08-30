/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.swecneexporter;

import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracio.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.cracio.cim.craccreator.CnecCreationContext;
import com.powsybl.openrao.data.cracio.cim.craccreator.MeasurementCreationContext;
import com.powsybl.openrao.data.cracio.cim.craccreator.MonitoredSeriesCreationContext;
import com.powsybl.openrao.data.swecneexporter.xsd.Analog;
import com.powsybl.openrao.data.swecneexporter.xsd.MonitoredRegisteredResource;
import com.powsybl.openrao.data.swecneexporter.xsd.MonitoredSeries;

import java.util.*;

import static com.powsybl.openrao.data.cneexportercommons.CneConstants.*;

/**
 * Generates MonitoredSeries for SWE CNE format
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class SweMonitoredSeriesCreator {
    private final SweCneHelper sweCneHelper;
    private final CimCracCreationContext cracCreationContext;
    private Map<Contingency, Map<MonitoredSeriesCreationContext, Set<CnecCreationContext>>> cnecCreationContextsMap;

    public SweMonitoredSeriesCreator(SweCneHelper sweCneHelper, CimCracCreationContext cracCreationContext) {
        this.sweCneHelper = sweCneHelper;
        this.cracCreationContext = cracCreationContext;
        prepareMap();
    }

    private void prepareMap() {
        cnecCreationContextsMap = new TreeMap<>((c1, c2) -> {
            if (Objects.isNull(c1) && Objects.isNull(c2)) {
                return 0;
            } else if (Objects.isNull(c1)) {
                return -1;
            } else if (Objects.isNull(c2)) {
                return 1;
            } else {
                return c1.getId().compareTo(c2.getId());
            }
        });
        Crac crac = sweCneHelper.getCrac();
        cracCreationContext.getMonitoredSeriesCreationContexts().values().stream()
            .filter(MonitoredSeriesCreationContext::isImported).forEach(
                monitoredSeriesCreationContext -> monitoredSeriesCreationContext.getMeasurementCreationContexts().stream()
                    .filter(MeasurementCreationContext::isImported).forEach(
                        measurementCreationContext -> measurementCreationContext.getCnecCreationContexts().values().stream()
                            .filter(CnecCreationContext::isImported).forEach(
                                cnecCreationContext -> {
                                    FlowCnec cnec = crac.getFlowCnec(cnecCreationContext.getCreatedCnecId());
                                    Contingency contingency = cnec.getState().getContingency().orElse(null);
                                    cnecCreationContextsMap.computeIfAbsent(contingency, c -> new TreeMap<>(Comparator.comparing(MonitoredSeriesCreationContext::getNativeId)));
                                    cnecCreationContextsMap.get(contingency).computeIfAbsent(monitoredSeriesCreationContext, cc -> new TreeSet<>(Comparator.comparing(CnecCreationContext::getCreatedCnecId)));
                                    cnecCreationContextsMap.get(contingency).get(monitoredSeriesCreationContext).add(cnecCreationContext);
                                })));
    }

    public List<MonitoredSeries> generateMonitoredSeries(Contingency contingency) {
        if (!cnecCreationContextsMap.containsKey(contingency)) {
            return Collections.emptyList();
        }
        List<MonitoredSeries> monitoredSeriesList = new ArrayList<>();
        boolean includeMeasurements = !sweCneHelper.isContingencyDivergent(contingency);
        cnecCreationContextsMap.get(contingency).forEach(
            (monitoredSeriesCC, cnecCCSet) -> monitoredSeriesList.addAll(generateMonitoredSeries(monitoredSeriesCC, cnecCCSet, includeMeasurements))
        );
        return monitoredSeriesList;
    }

    private double getCnecFlowClosestToThreshold(Instant optimizedInstant, FlowCnec cnec) {
        double flow = 0.0;
        double margin = Double.POSITIVE_INFINITY;
        for (TwoSides side : cnec.getMonitoredSides()) {
            double flowOnSide = sweCneHelper.getRaoResult().getFlow(optimizedInstant, cnec, side, Unit.AMPERE);
            if (Double.isNaN(flowOnSide)) {
                continue;
            }
            double marginOnSide = cnec.computeMargin(flowOnSide, side, Unit.AMPERE);
            if (marginOnSide < margin) {
                margin = marginOnSide;
                flow = flowOnSide;
            }
        }
        return flow;
    }

    private List<MonitoredSeries> generateMonitoredSeries(MonitoredSeriesCreationContext monitoredSeriesCreationContext, Set<CnecCreationContext> cnecCreationContexts, boolean includeMeasurements) {
        Crac crac = sweCneHelper.getCrac();
        Map<Integer, MonitoredSeries> monitoredSeriesPerFlowValue = new LinkedHashMap<>();
        cnecCreationContexts.forEach(cnecCreationContext -> {
            FlowCnec cnec = crac.getFlowCnec(cnecCreationContext.getCreatedCnecId());
            int roundedFlow = (int) Math.round(getCnecFlowClosestToThreshold(cnec.getState().getInstant(), cnec));
            if (monitoredSeriesPerFlowValue.containsKey(roundedFlow) && includeMeasurements) {
                mergeSeries(monitoredSeriesPerFlowValue.get(roundedFlow), cnec);
            } else if (!monitoredSeriesPerFlowValue.containsKey(roundedFlow)) {
                monitoredSeriesPerFlowValue.put(roundedFlow, generateMonitoredSeriesFromScratch(monitoredSeriesCreationContext, cnec, includeMeasurements));
            }
        });
        return new ArrayList<>(monitoredSeriesPerFlowValue.values());
    }

    private void mergeSeries(MonitoredSeries monitoredSeries, FlowCnec cnec) {
        Analog threshold = new Analog();
        threshold.setMeasurementType(getThresholdMeasurementType(cnec));
        threshold.setUnitSymbol(AMP_UNIT_SYMBOL);
        TwoSides side = cnec.getMonitoredSides().contains(TwoSides.ONE) ? TwoSides.ONE : cnec.getMonitoredSides().iterator().next();
        float roundedThreshold = Math.round(Math.min(
            Math.abs(cnec.getLowerBound(side, Unit.AMPERE).orElse(Double.POSITIVE_INFINITY)),
            Math.abs(cnec.getUpperBound(side, Unit.AMPERE).orElse(Double.NEGATIVE_INFINITY))));
        threshold.setPositiveFlowIn(getCnecFlowClosestToThreshold(cnec.getState().getInstant(), cnec) >= 0 ?
            DIRECT_POSITIVE_FLOW_IN : OPPOSITE_POSITIVE_FLOW_IN);
        threshold.setAnalogValuesValue(Math.abs(roundedThreshold));

        monitoredSeries.getRegisteredResource().get(0).getMeasurements().add(threshold);
    }

    private MonitoredSeries generateMonitoredSeriesFromScratch(MonitoredSeriesCreationContext monitoredSeriesCreationContext, FlowCnec cnec, boolean includeMeasurements) {
        MonitoredSeries monitoredSeries = new MonitoredSeries();
        monitoredSeries.setMRID(monitoredSeriesCreationContext.getNativeId());
        monitoredSeries.setName(monitoredSeriesCreationContext.getNativeName());
        MonitoredRegisteredResource registeredResource = new MonitoredRegisteredResource();
        registeredResource.setMRID(SweCneUtil.createResourceIDString(A02_CODING_SCHEME, monitoredSeriesCreationContext.getNativeResourceId()));
        registeredResource.setName(monitoredSeriesCreationContext.getNativeResourceName());
        setInOutAggregateNodes(cnec.getNetworkElement().getId(), monitoredSeriesCreationContext.getNativeId(), registeredResource);

        if (includeMeasurements) {
            Analog flow = new Analog();
            flow.setMeasurementType(FLOW_MEASUREMENT_TYPE);
            flow.setUnitSymbol(AMP_UNIT_SYMBOL);
            float roundedFlow = Math.round(getCnecFlowClosestToThreshold(cnec.getState().getInstant(), cnec));
            flow.setPositiveFlowIn(roundedFlow >= 0 ? DIRECT_POSITIVE_FLOW_IN : OPPOSITE_POSITIVE_FLOW_IN);
            flow.setAnalogValuesValue(Math.abs(roundedFlow));
            registeredResource.getMeasurements().add(flow);

            Analog threshold = new Analog();
            threshold.setMeasurementType(getThresholdMeasurementType(cnec));
            threshold.setUnitSymbol(AMP_UNIT_SYMBOL);
            TwoSides side = cnec.getMonitoredSides().contains(TwoSides.ONE) ? TwoSides.ONE : cnec.getMonitoredSides().iterator().next();
            float roundedThreshold = Math.round(Math.min(
                Math.abs(cnec.getLowerBound(side, Unit.AMPERE).orElse(Double.POSITIVE_INFINITY)),
                Math.abs(cnec.getUpperBound(side, Unit.AMPERE).orElse(Double.NEGATIVE_INFINITY))));
            threshold.setPositiveFlowIn(roundedFlow >= 0 ? DIRECT_POSITIVE_FLOW_IN : OPPOSITE_POSITIVE_FLOW_IN);
            threshold.setAnalogValuesValue(Math.abs(roundedThreshold));
            registeredResource.getMeasurements().add(threshold);
        }

        monitoredSeries.getRegisteredResource().add(registeredResource);
        return monitoredSeries;
    }

    void setInOutAggregateNodes(String networkElementId, String monitoredSeriesId, MonitoredRegisteredResource registeredResource) {
        Branch<?> branch = sweCneHelper.getNetwork().getBranch(networkElementId);
        if (branch instanceof TieLine tieLine && tieLine.getDanglingLine1().hasProperty("CGMES.TopologicalNode_Boundary")) {
            Country cnecOperatorCountry = SweCneUtil.getOperatorCountry(monitoredSeriesId.substring(0, 3));
            String xNodeMRId = tieLine.getDanglingLine1().getProperty("CGMES.TopologicalNode_Boundary");
            if (SweCneUtil.getBranchCountry(branch, TwoSides.ONE).equals(cnecOperatorCountry)) {
                registeredResource.setInAggregateNodeMRID(SweCneUtil.createResourceIDString(A02_CODING_SCHEME, branch.getTerminal1().getVoltageLevel().getId()));
                registeredResource.setOutAggregateNodeMRID(SweCneUtil.createResourceIDString(A02_CODING_SCHEME, xNodeMRId));
            } else {
                registeredResource.setInAggregateNodeMRID(SweCneUtil.createResourceIDString(A02_CODING_SCHEME, xNodeMRId));
                registeredResource.setOutAggregateNodeMRID(SweCneUtil.createResourceIDString(A02_CODING_SCHEME, branch.getTerminal2().getVoltageLevel().getId()));
            }
        } else {
            registeredResource.setInAggregateNodeMRID(SweCneUtil.createResourceIDString(A02_CODING_SCHEME, branch.getTerminal1().getVoltageLevel().getId()));
            registeredResource.setOutAggregateNodeMRID(SweCneUtil.createResourceIDString(A02_CODING_SCHEME, branch.getTerminal2().getVoltageLevel().getId()));
        }
    }

    private String getThresholdMeasurementType(FlowCnec cnec) {
        switch (cnec.getState().getInstant().getKind()) {
            case PREVENTIVE:
                return PATL_MEASUREMENT_TYPE;
            case OUTAGE:
                return TATL_MEASUREMENT_TYPE;
            case AUTO:
                return AUTO_MEASUREMENT_TYPE;
            case CURATIVE:
                return CURATIVE_MEASUREMENT_TYPE;
            default:
                throw new OpenRaoException(String.format("Unexpected instant: %s", cnec.getState().getInstant().toString()));
        }
    }
}
