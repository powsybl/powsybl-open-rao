/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.CracResultExtension;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_io_cne.CneClassCreator.*;
import static com.farao_community.farao.data.crac_io_cne.CneConstants.*;
import static com.farao_community.farao.data.crac_io_cne.CneUtil.findNodeInNetwork;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CneHelper {
    private Crac crac;
    private Network network;
    private List<Instant> instants;
    private Map<Pair<Optional<Contingency>, String>, ConstraintSeries> constraintSeriesMapB54;
    private Map<Pair<Optional<Contingency>, String>, ConstraintSeries> constraintSeriesMapB56;
    private Map<Pair<Optional<Contingency>, String>, ConstraintSeries> constraintSeriesMapB57;
    private Map<Pair<Optional<Contingency>, String>, ConstraintSeries> constraintSeriesMapB88;
    private Map<Pair<NetworkElement, Optional<Contingency>>, List<Analog>> measurementListEachNetworkElement;
    private String preOptimVariantId;
    private String postOptimVariantId;

    private static final Logger LOGGER = LoggerFactory.getLogger(CneHelper.class);

    public CneHelper(Crac crac, Network network) {

        this.crac = crac;
        this.network = network;
        instants = new ArrayList<>();
        preOptimVariantId = "";
        postOptimVariantId = "";
        constraintSeriesMapB54 = new HashMap<>();
        constraintSeriesMapB56 = new HashMap<>();
        constraintSeriesMapB57 = new HashMap<>();
        constraintSeriesMapB88 = new HashMap<>();
        measurementListEachNetworkElement = new HashMap<>();
    }

    public void initializeAttributes(Crac crac, CracResultExtension cracExtension, List<String> variants) {
        // sort the instants in order to determine which one is preventive, after outage, after auto RA and after CRA
        instants = crac.getInstants().stream().sorted(Comparator.comparing(Instant::getSeconds)).collect(Collectors.toList());

        // TODO: store the information on preOptim/postOptim Variant in the ResultVariantManager
        preOptimVariantId = variants.get(0);
        postOptimVariantId = variants.get(0);

        double minCost = cracExtension.getVariant(variants.get(0)).getCost();
        double maxCost = cracExtension.getVariant(variants.get(0)).getCost();
        for (String variant : variants) {
            if (cracExtension.getVariant(variant).getCost() < minCost) {
                minCost = cracExtension.getVariant(variant).getCost();
                postOptimVariantId = variant;
            } else if (cracExtension.getVariant(variant).getCost() > maxCost) {
                maxCost = cracExtension.getVariant(variant).getCost();
                preOptimVariantId = variant;
            }
        }

        //crac.getCnecs().forEach(this::fillThresholdsMap);
    }

    public List<Instant> getInstants() {
        return instants;
    }

    public String getPreOptimVariantId() {
        return preOptimVariantId;
    }

    public String getPostOptimVariantId() {
        return postOptimVariantId;
    }

    public Map<Pair<Optional<Contingency>, String>, ConstraintSeries> getConstraintSeriesMapB54() {
        return constraintSeriesMapB54;
    }

    public Crac getCrac() {
        return crac;
    }

    public Map<Pair<Optional<Contingency>, String>, ConstraintSeries> getConstraintSeriesMapB56() {
        return constraintSeriesMapB56;
    }

    public Map<Pair<Optional<Contingency>, String>, ConstraintSeries> getConstraintSeriesMapB57() {
        return constraintSeriesMapB57;
    }

    public Map<Pair<Optional<Contingency>, String>, ConstraintSeries> getConstraintSeriesMapB88() {
        return constraintSeriesMapB88;
    }

    private String instantToCodeConverter(Instant instant) {
        if (instant.equals(instants.get(0))) { // Before contingency
            return PATL_MEASUREMENT_TYPE;
        } else { // After contingency, before any post-contingency RA
            return TATL_MEASUREMENT_TYPE;
        }
    }

    // Creates the Measurement Lists of thresholds for each network element
    /*private void fillThresholdsMap(Cnec cnec) {

        List<Analog> measurementsList = new ArrayList<>();
        CnecResultExtension cnecResultExtension = cnec.getExtension(CnecResultExtension.class);
        if (cnecResultExtension != null) {
            CnecResult cnecResult = cnecResultExtension.getVariant(postOptimVariantId); // can be any of the optimVariantIds
            if (cnecResult != null) {
                boolean exportA = false;
                boolean exportMW = false;
                double thresholdA = Double.NaN;
                double thresholdMW = Double.NaN;
                if (!Double.isNaN(cnecResult.getFlowInA())) {
                    if (cnecResult.getFlowInA() >= 0 && !Double.isNaN(cnecResult.getMaxThresholdInA())) {
                        exportA = true;
                        thresholdA = cnecResult.getMaxThresholdInA();
                    } else if (cnecResult.getFlowInA() < 0 && !Double.isNaN(cnecResult.getMinThresholdInA())) {
                        exportA = true;
                        thresholdA = cnecResult.getMinThresholdInA();
                    } else {
                        LOGGER.warn(String.format("No threshold available in A for %s.", cnec.getName()));
                    }
                }

                if (!Double.isNaN(cnecResult.getFlowInMW())) {
                    if (cnecResult.getFlowInMW() >= 0 && !Double.isNaN(cnecResult.getMaxThresholdInMW())) {
                        exportMW = true;
                        thresholdMW = cnecResult.getMaxThresholdInMW();
                    } else if (cnecResult.getFlowInMW() < 0 && !Double.isNaN(cnecResult.getMinThresholdInMW())) {
                        exportMW = true;
                        thresholdMW = cnecResult.getMinThresholdInMW();
                    } else {
                        LOGGER.warn(String.format("No threshold available in MW for %s.", cnec.getName()));
                    }
                }

                if (exportA) {
                    measurementsList.add(newMeasurement(instantToCodeConverter(cnec.getState().getInstant()), Unit.AMPERE, thresholdA));
                }
                if (exportMW) {
                    measurementsList.add(newMeasurement(instantToCodeConverter(cnec.getState().getInstant()), Unit.MEGAWATT, thresholdMW));
                }
                if (cnec instanceof SimpleCnec) {
                    SimpleCnec simpleCnec = (SimpleCnec) cnec;
                    if (!Double.isNaN(simpleCnec.getFrm())) {
                        measurementsList.add(newMeasurement(FRM_MEASUREMENT_TYPE, Unit.MEGAWATT, simpleCnec.getFrm()));
                    }
                }
                if (!Double.isNaN(cnecResult.getLoopflowInMW()) && !Double.isNaN(cnecResult.getLoopflowThresholdInMW())) {
                    measurementsList.add(newMeasurement(LOOPFLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, cnecResult.getLoopflowInMW()));
                    measurementsList.add(newMeasurement(MAX_LOOPFLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, cnecResult.getLoopflowThresholdInMW()));
                }
            }
        }

        Pair<NetworkElement, Optional<Contingency>> pair = Pair.create(cnec.getNetworkElement(), cnec.getState().getContingency());
        if (measurementListEachNetworkElement.get(pair) == null) {
            measurementListEachNetworkElement.put(pair, measurementsList);
        } else {
            measurementListEachNetworkElement.get(pair).addAll(measurementsList);
        }
    }

    public void addB88MonitoredSeriesToConstraintSeries(Cnec cnec, Set<Pair<String, String>> recordedCbco) {
        constraintSeriesMapB88.forEach((contingencyStateid, constraintSeries) -> {
            if (contingencyStateid.getFirst().equals(cnec.getState().getContingency())) {
                if (constraintSeries.monitoredSeries == null) {
                    constraintSeries.monitoredSeries = new ArrayList<>();
                }
                Pair<String, String> pair = Pair.create(cnec.getNetworkElement().getId(), getContingencyId(cnec.getState().getContingency()));
                if (!recordedCbco.contains(pair)) {
                    MonitoredSeries monitoredSeries = createB88MonitoredSeries(cnec);
                    if (monitoredSeries.registeredResource.size() == 1) {
                        monitoredSeries.registeredResource.get(0).measurements.addAll(createFlow(cnec));
                    }  else {
                        throw new FaraoException(String.format("Wrong number of registered resources %s for monitored series %s.", monitoredSeries.registeredResource.size(), monitoredSeries.getMRID()));
                    }
                    recordedCbco.add(pair);
                    constraintSeries.monitoredSeries.add(monitoredSeries);
                }
            }
        });
    }*/

    private String getContingencyId(Optional<Contingency> contingency) {
        if (contingency.isPresent()) {
            return contingency.get().getId();
        } else {
            return "BASECASE";
        }
    }

    private List<Analog> createFlow(Cnec cnec) {
        List<Analog> measurements = new ArrayList<>();
        CnecResultExtension cnecResultExtension = cnec.getExtension(CnecResultExtension.class);
        if (cnecResultExtension != null) {
            CnecResult cnecResult = cnecResultExtension.getVariant(preOptimVariantId);
            if (cnecResult != null) {
                if (!Double.isNaN(cnecResult.getFlowInA())) {
                    measurements.add(newMeasurement(FLOW_MEASUREMENT_TYPE, Unit.AMPERE, cnecResult.getFlowInA()));
                }
                if (Double.isNaN(cnecResult.getFlowInMW())) {
                    measurements.add(newMeasurement(FLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, cnecResult.getFlowInMW()));
                }
            }
        }
        return measurements;
    }

    private MonitoredSeries createB88MonitoredSeries(Cnec cnec) {

        Pair<NetworkElement, Optional<Contingency>> myPair = Pair.create(cnec.getNetworkElement(), cnec.getState().getContingency());
        MonitoredRegisteredResource monitoredRegisteredResource = newMonitoredRegisteredResource(cnec.getNetworkElement().getId(),
            cnec.getNetworkElement().getName(),
            findNodeInNetwork(cnec.getNetworkElement().getId(), network, Branch.Side.ONE),
            findNodeInNetwork(cnec.getNetworkElement().getId(), network, Branch.Side.TWO),
            measurementListEachNetworkElement.get(myPair));
        return newMonitoredSeries(cnec.getId(), cnec.getName(), monitoredRegisteredResource);
    }

    // Helper: fills maps (B56/B57) containing the constraint series corresponding to a state
    public void addConstraintsToMap(State state) {
        // add to map
        Optional<Contingency> optionalContingency = state.getContingency();
        if (optionalContingency.isPresent()) {
            Contingency contingency = optionalContingency.get();
            constraintSeriesMapB54.put(Pair.create(Optional.of(contingency), state.getId()), newConstraintSeries(B54_BUSINESS_TYPE, OPTIMIZED_MARKET_STATUS, newContingencySeries(contingency.getId(), contingency.getName())));
            constraintSeriesMapB56.put(Pair.create(Optional.of(contingency), state.getId()), newConstraintSeries(B56_BUSINESS_TYPE, OPTIMIZED_MARKET_STATUS, newContingencySeries(contingency.getId(), contingency.getName())));
            constraintSeriesMapB57.put(Pair.create(Optional.of(contingency), state.getId()), newConstraintSeries(B57_BUSINESS_TYPE, OPTIMIZED_MARKET_STATUS, newContingencySeries(contingency.getId(), contingency.getName())));
            constraintSeriesMapB88.put(Pair.create(Optional.of(contingency), state.getId()), newConstraintSeries(B88_BUSINESS_TYPE, OPTIMIZED_MARKET_STATUS, newContingencySeries(contingency.getId(), contingency.getName())));
        } else {
            constraintSeriesMapB54.put(Pair.create(Optional.empty(), state.getId()), newConstraintSeries(B54_BUSINESS_TYPE, OPTIMIZED_MARKET_STATUS));
            constraintSeriesMapB56.put(Pair.create(Optional.empty(), state.getId()), newConstraintSeries(B56_BUSINESS_TYPE, OPTIMIZED_MARKET_STATUS));
            constraintSeriesMapB57.put(Pair.create(Optional.empty(), state.getId()), newConstraintSeries(B57_BUSINESS_TYPE, OPTIMIZED_MARKET_STATUS));
            constraintSeriesMapB88.put(Pair.create(Optional.empty(), state.getId()), newConstraintSeries(B88_BUSINESS_TYPE, OPTIMIZED_MARKET_STATUS));
        }
    }

    public void checkSynchronize() {
        if (!crac.isSynchronized()) {
            crac.synchronize(network);
        }
    }
}
