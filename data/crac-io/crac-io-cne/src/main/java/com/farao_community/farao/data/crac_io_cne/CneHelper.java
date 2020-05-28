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
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_io_cne.CneClassCreator.*;
import static com.farao_community.farao.data.crac_io_cne.CneConstants.*;
import static com.farao_community.farao.data.crac_io_cne.CneConstants.B88_BUSINESS_TYPE;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CneHelper {
    private List<Instant> instants;
    private Map<Pair<Optional<Contingency>, String>, ConstraintSeries> constraintSeriesMapB54;
    private Map<Pair<Optional<Contingency>, String>, ConstraintSeries> constraintSeriesMapB56;
    private Map<Pair<Optional<Contingency>, String>, ConstraintSeries> constraintSeriesMapB57;
    private Map<Pair<Optional<Contingency>, String>, ConstraintSeries> constraintSeriesMapB88;
    private Map<Pair<NetworkElement, Optional<Contingency>>, List<Analog>> measurementListEachNetworkElement;
    private String preOptimVariantId;
    private String postOptimVariantId;

    private static final Logger LOGGER = LoggerFactory.getLogger(CneHelper.class);

    public CneHelper() {

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

        crac.getCnecs().forEach(this::fillThresholdsMap);
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

    public void setConstraintSeriesMapB54(Map<Pair<Optional<Contingency>, String>, ConstraintSeries> constraintSeriesMapB54) {
        this.constraintSeriesMapB54 = constraintSeriesMapB54;
    }

    public Map<Pair<Optional<Contingency>, String>, ConstraintSeries> getConstraintSeriesMapB56() {
        return constraintSeriesMapB56;
    }

    public void setConstraintSeriesMapB56(Map<Pair<Optional<Contingency>, String>, ConstraintSeries> constraintSeriesMapB56) {
        this.constraintSeriesMapB56 = constraintSeriesMapB56;
    }

    public Map<Pair<Optional<Contingency>, String>, ConstraintSeries> getConstraintSeriesMapB57() {
        return constraintSeriesMapB57;
    }

    public void setConstraintSeriesMapB57(Map<Pair<Optional<Contingency>, String>, ConstraintSeries> constraintSeriesMapB57) {
        this.constraintSeriesMapB57 = constraintSeriesMapB57;
    }

    public Map<Pair<Optional<Contingency>, String>, ConstraintSeries> getConstraintSeriesMapB88() {
        return constraintSeriesMapB88;
    }

    public void setConstraintSeriesMapB88(Map<Pair<Optional<Contingency>, String>, ConstraintSeries> constraintSeriesMapB88) {
        this.constraintSeriesMapB88 = constraintSeriesMapB88;
    }

    private String instantToCodeConverter(Instant instant) {
        if (instant.equals(instants.get(0))) { // Before contingency
            return PATL_MEASUREMENT_TYPE;
        } else { // After contingency, before any post-contingency RA
            return TATL_MEASUREMENT_TYPE;
        }
    }

    // Creates the Measurement Lists of thresholds for each network element
    private void fillThresholdsMap(Cnec cnec) {

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
                // TODO : add to this other codes (A03, Z01, Z02, Z03, Z04, Z05, Z06, Z07)
            }
        }

        Pair<NetworkElement, Optional<Contingency>> pair = Pair.create(cnec.getNetworkElement(), cnec.getState().getContingency());
        if (measurementListEachNetworkElement.get(pair) == null) {
            measurementListEachNetworkElement.put(pair, measurementsList);
        } else {
            measurementListEachNetworkElement.get(pair).addAll(measurementsList);
        }
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
}
