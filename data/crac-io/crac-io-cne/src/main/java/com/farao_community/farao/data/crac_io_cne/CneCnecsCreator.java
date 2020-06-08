/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Unit;
import com.farao_community.farao.data.crac_impl.SimpleCnec;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;

import java.util.List;

import static com.farao_community.farao.data.crac_io_cne.CneClassCreator.newMeasurement;
import static com.farao_community.farao.data.crac_io_cne.CneConstants.*;
import static com.farao_community.farao.data.crac_io_cne.CneConstants.ABS_MARG_TATL_MEASUREMENT_TYPE;

/**
 * Creates the measurements, monitored registered resources and monitored series
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CneCnecsCreator {

    private CneCnecsCreator() {

    }

    // B54 & B57
    public static void createB54B57Measurements(Cnec cnec, String measurementType, String postOptimVariantId, List<Analog> measurementsB54, List<Analog> measurementsB57) {
        // The check of the existence of the CnecResultExtension was done in another method
        CnecResultExtension cnecResultExtension = cnec.getExtension(CnecResultExtension.class);
        assert cnecResultExtension != null;

        CnecResult cnecResultPost = cnecResultExtension.getVariant(postOptimVariantId);
        if (cnecResultPost != null) {
            // Flow and threshold in A
            addFlowThreshold(cnecResultPost, Unit.AMPERE, measurementType, measurementsB54, measurementsB57);
            // Flow and threshold in MW
            addFlowThreshold(cnecResultPost, Unit.MEGAWATT, measurementType, measurementsB54, measurementsB57);

            // sumPTDF
            addSumPtdf();

            // loopflow
            addLoopflow(cnecResultPost, measurementsB54);
            addLoopflow(cnecResultPost, measurementsB57);
        }
    }

    // B88
    public static void createB88Measurements(Cnec cnec, String measurementType, String preOptimVariantId, List<Analog> measurementsB88) {
        CnecResultExtension cnecResultExtension = cnec.getExtension(CnecResultExtension.class);
        // The check of the existence of the CnecResultExtension was done in another method
        assert cnecResultExtension != null;

        CnecResult cnecResultPre = cnecResultExtension.getVariant(preOptimVariantId);
        if (cnecResultPre != null) {
            // Flow and threshold in A
            addFlowThreshold(cnecResultPre, Unit.AMPERE, measurementType, measurementsB88);
            // Flow and threshold in MW
            addFlowThreshold(cnecResultPre, Unit.MEGAWATT, measurementType, measurementsB88);
            // FRM
            addFrm(cnec, measurementsB88);

            // sumPTDF
            addSumPtdf();

            // loopflow
            addLoopflow(cnecResultPre, measurementsB88);
        }
    }

    // Flow and threshold
    private static void addFlowThreshold(CnecResult cnecResult, Unit unit, String measurementType, List<Analog> measurements) {
        addFlowThreshold(cnecResult, unit, measurementType, measurements, measurements, true);
    }

    private static void addFlowThreshold(CnecResult cnecResult, Unit unit, String measurementType, List<Analog> measurementsPatl, List<Analog> measurementsTatl) {
        addFlowThreshold(cnecResult, unit, measurementType, measurementsPatl, measurementsTatl, false);
    }

    private static void addFlowThreshold(CnecResult cnecResult, Unit unit, String measurementType, List<Analog> measurementsPatl, List<Analog> measurementsTatl, boolean b88) {

        double flow;
        double threshold;
        // cnecResult is not null, checked before
        assert cnecResult != null;
        if (unit.equals(Unit.AMPERE)) {
            flow = cnecResult.getFlowInA();
            if (flow < 0) {
                threshold = cnecResult.getMinThresholdInA();
            } else {
                threshold = cnecResult.getMaxThresholdInA();
            }
        } else if (unit.equals(Unit.MEGAWATT)) {
            flow = cnecResult.getFlowInMW();
            if (flow < 0) {
                threshold = cnecResult.getMinThresholdInMW();
            } else {
                threshold = cnecResult.getMaxThresholdInMW();
            }
        } else {
            throw new FaraoException(String.format(UNHANDLED_UNIT, unit.toString()));
        }

        String absMarginMeasType = computeAbsMarginMeasType(measurementType);
        if (!Double.isNaN(flow) && !Double.isNaN(threshold)) {
            measurementsPatl.add(newMeasurement(FLOW_MEASUREMENT_TYPE, unit, flow));
            if (b88) {
                measurementsTatl.add(newMeasurement(measurementType, unit, threshold));
            } else {
                measurementsTatl.add(newMeasurement(FLOW_MEASUREMENT_TYPE, unit, flow));
            }

            double value = Math.abs(threshold) - Math.abs(flow);
            if (absMarginMeasType.equals(ABS_MARG_PATL_MEASUREMENT_TYPE)) {
                measurementsPatl.add(newMeasurement(absMarginMeasType, unit, value));
                measurementsPatl.add(newMeasurement(OBJ_FUNC_PATL_MEASUREMENT_TYPE, unit, -value));
            } else if (absMarginMeasType.equals(ABS_MARG_TATL_MEASUREMENT_TYPE)) {
                measurementsTatl.add(newMeasurement(absMarginMeasType, unit, value));
                measurementsTatl.add(newMeasurement(OBJ_FUNC_TATL_MEASUREMENT_TYPE, unit, -value));
            }
        }
    }

    private static void addFrm(Cnec cnec, List<Analog> measurements) {
        if (cnec instanceof SimpleCnec && !Double.isNaN(((SimpleCnec) cnec).getFrm())) {
            measurements.add(newMeasurement(FRM_MEASUREMENT_TYPE, Unit.MEGAWATT, ((SimpleCnec) cnec).getFrm()));
        }
    }

    private static void addSumPtdf() {
        // TODO: develop this
    }

    private static void addLoopflow(CnecResult cnecResult, List<Analog> measurements) {
        if (!Double.isNaN(cnecResult.getLoopflowInMW()) && !Double.isNaN(cnecResult.getLoopflowThresholdInMW())) {
            measurements.add(newMeasurement(LOOPFLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, cnecResult.getLoopflowInMW()));
            double threshold = Math.signum(cnecResult.getLoopflowInMW()) * cnecResult.getLoopflowThresholdInMW();
            measurements.add(newMeasurement(MAX_LOOPFLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, threshold));
        }
    }

    public static String computeAbsMarginMeasType(String measurementType) {
        String absMarginMeasType = "";
        if (measurementType.equals(PATL_MEASUREMENT_TYPE)) {
            absMarginMeasType = ABS_MARG_PATL_MEASUREMENT_TYPE;
        } else if (measurementType.equals(TATL_MEASUREMENT_TYPE)) {
            absMarginMeasType = ABS_MARG_TATL_MEASUREMENT_TYPE;
        }
        return absMarginMeasType;
    }
}
