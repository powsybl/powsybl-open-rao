/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static com.farao_community.farao.data.crac_io_cne.CneConstants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CneGenerationTest {

    @Test
    public void testExport1() {

        Crac crac = CracImporters.importCrac("US2-3-crac1-standard.json", getClass().getResourceAsStream("/US2-3-crac1-standard.json"));
        Network network = Importers.loadNetwork("US2-3-case1-standard.uct", getClass().getResourceAsStream("/US2-3-case1-standard.uct"));

        // build object
        Cne cne = new Cne(crac, network);
        cne.generate();
        CriticalNetworkElementMarketDocument marketDocument = cne.getMarketDocument();
        Point point = marketDocument.getTimeSeries().get(0).getPeriod().get(0).getPoint().get(0);

        assertEquals(3, point.getConstraintSeries().size());

        Optional<ConstraintSeries> constraintSeriesB54 = point.getConstraintSeries().stream().filter(constraintSeries ->
            constraintSeries.getMRID().equals("FFR1AA1  FFR2AA1  1 - N - preventive") && constraintSeries.getBusinessType().equals(B54_BUSINESS_TYPE)).findFirst();
        Optional<ConstraintSeries> constraintSeriesB57 = point.getConstraintSeries().stream().filter(constraintSeries ->
            constraintSeries.getMRID().equals("FFR1AA1  FFR2AA1  1 - N - preventive") && constraintSeries.getBusinessType().equals(B57_BUSINESS_TYPE)).findFirst();
        Optional<ConstraintSeries> constraintSeriesB88 = point.getConstraintSeries().stream().filter(constraintSeries ->
            constraintSeries.getMRID().equals("FFR1AA1  FFR2AA1  1 - N - preventive") && constraintSeries.getBusinessType().equals(B88_BUSINESS_TYPE)).findFirst();

        if (constraintSeriesB54.isPresent() && constraintSeriesB57.isPresent() && constraintSeriesB88.isPresent()) {
            // Constraint series B54
            assertEquals(8, constraintSeriesB54.get().getMonitoredSeries().get(0).getRegisteredResource().get(0).getMeasurements().size());
            // Constraint series B57
            assertEquals(4, constraintSeriesB57.get().getMonitoredSeries().get(0).getRegisteredResource().get(0).getMeasurements().size());
            // Constraint series B88
            assertEquals("10YFR-RTE------C", constraintSeriesB88.get().getPartyMarketParticipant().get(0).getMRID().getValue());
            assertEquals(0, constraintSeriesB88.get().getContingencySeries().size());
            MonitoredRegisteredResource monitoredRegisteredResource = constraintSeriesB88.get().getMonitoredSeries().get(0).getRegisteredResource().get(0);
            assertEquals(11, monitoredRegisteredResource.getMeasurements().size());
            assertEquals("FFR1AA1  FFR2AA1  1 - N - preventive", monitoredRegisteredResource.getMRID().getValue());
            assertEquals("Threshold12", monitoredRegisteredResource.getName());
            assertEquals("FFR1AA1_0", monitoredRegisteredResource.getInAggregateNodeMRID().getValue());
            assertEquals("FFR2AA1_0", monitoredRegisteredResource.getOutAggregateNodeMRID().getValue());

            List<Analog> measurements = monitoredRegisteredResource.getMeasurements();
            Optional<Analog> measurement1 = measurements.stream().filter(measurement -> measurement.getMeasurementType().equals(FLOW_MEASUREMENT_TYPE) && measurement.getUnitSymbol().equals(AMP_UNIT_SYMBOL)).findFirst();
            if (measurement1.isPresent()) {
                assertEquals(DIRECT_POSITIVE_FLOW_IN, measurement1.get().getPositiveFlowIn());
                assertEquals(13186, measurement1.get().getAnalogValuesValue(), 0);
            } else {
                fail();
            }
            Optional<Analog> measurement2 = measurements.stream().filter(measurement -> measurement.getMeasurementType().equals(PATL_MEASUREMENT_TYPE) && measurement.getUnitSymbol().equals(AMP_UNIT_SYMBOL)).findFirst();
            if (measurement2.isPresent()) {
                assertEquals(DIRECT_POSITIVE_FLOW_IN, measurement2.get().getPositiveFlowIn());
                assertEquals(3325, measurement2.get().getAnalogValuesValue(), 0);
            } else {
                fail();
            }
            Optional<Analog> measurement3 = measurements.stream().filter(measurement -> measurement.getMeasurementType().equals(ABS_MARG_PATL_MEASUREMENT_TYPE) && measurement.getUnitSymbol().equals(AMP_UNIT_SYMBOL)).findFirst();
            if (measurement3.isPresent()) {
                assertEquals(OPPOSITE_POSITIVE_FLOW_IN, measurement3.get().getPositiveFlowIn());
                assertEquals(9861, measurement3.get().getAnalogValuesValue(), 0);
            } else {
                fail();
            }
            Optional<Analog> measurement4 = measurements.stream().filter(measurement -> measurement.getMeasurementType().equals(OBJ_FUNC_PATL_MEASUREMENT_TYPE) && measurement.getUnitSymbol().equals(MAW_UNIT_SYMBOL)).findFirst();
            if (measurement4.isPresent()) {
                assertEquals(OPPOSITE_POSITIVE_FLOW_IN, measurement4.get().getPositiveFlowIn());
                assertEquals(26, measurement4.get().getAnalogValuesValue(), 0);
            } else {
                fail();
            }
            Optional<Analog> measurement5 = measurements.stream().filter(measurement -> measurement.getMeasurementType().equals(FRM_MEASUREMENT_TYPE) && measurement.getUnitSymbol().equals(MAW_UNIT_SYMBOL)).findFirst();
            if (measurement5.isPresent()) {
                assertEquals(DIRECT_POSITIVE_FLOW_IN, measurement5.get().getPositiveFlowIn());
                assertEquals(0, measurement5.get().getAnalogValuesValue(), 0);
            } else {
                fail();
            }
            Optional<Analog> measurement6 = measurements.stream().filter(measurement -> measurement.getMeasurementType().equals(LOOPFLOW_MEASUREMENT_TYPE) && measurement.getUnitSymbol().equals(MAW_UNIT_SYMBOL)).findFirst();
            if (measurement6.isPresent()) {
                assertEquals(OPPOSITE_POSITIVE_FLOW_IN, measurement6.get().getPositiveFlowIn());
                assertEquals(13, measurement6.get().getAnalogValuesValue(), 0);
            } else {
                fail();
            }
            Optional<Analog> measurement7 = measurements.stream().filter(measurement -> measurement.getMeasurementType().equals(MAX_LOOPFLOW_MEASUREMENT_TYPE) && measurement.getUnitSymbol().equals(MAW_UNIT_SYMBOL)).findFirst();
            if (measurement7.isPresent()) {
                assertEquals(OPPOSITE_POSITIVE_FLOW_IN, measurement7.get().getPositiveFlowIn());
                assertEquals(19, measurement7.get().getAnalogValuesValue(), 0);
            } else {
                fail();
            }
        } else {
            fail();
        }
    }

    @Test
    public void testExport2() {

        Crac crac = CracImporters.importCrac("US3-2-pst-direct.json", getClass().getResourceAsStream("/US3-2-pst-direct.json"));
        Network network = NetworkImportsUtil.import12NodesNetwork();

        // build object
        Cne cne = new Cne(crac, network);
        cne.generate();
        CriticalNetworkElementMarketDocument marketDocument = cne.getMarketDocument();
        Point point = marketDocument.getTimeSeries().get(0).getPeriod().get(0).getPoint().get(0);

        assertEquals(18, point.getConstraintSeries().size());
    }
}
