/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.core_cne_exporter;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.cne_exporter_commons.CneExporterParameters;
import com.farao_community.farao.data.cne_exporter_commons.CneHelper;
import com.farao_community.farao.data.cne_exporter_commons.CneUtil;
import com.farao_community.farao.data.core_cne_exporter.xsd.*;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_loopflow_extension.LoopFlowThresholdAdder;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CoreCneCnecsCreatorTest {

    private Crac crac;
    private Network network;
    private RaoResult raoResult;
    private RaoParameters raoParameters;
    private CneExporterParameters exporterParameters;

    @Before
    public void setUp() {
        CneUtil.initUniqueIds();
        network = Network.read("TestCase12Nodes.uct", getClass().getResourceAsStream("/TestCase12Nodes.uct"));
        crac = CracFactory.findDefault().create("test-crac");
        raoResult = Mockito.mock(RaoResult.class);
        raoParameters = new RaoParameters();
        exporterParameters = new CneExporterParameters("22XCORESO------S-20211115-F299v1", 2, "10YDOM-REGION-1V", CneExporterParameters.ProcessType.DAY_AHEAD_CC,
            "22XCORESO------S", CneExporterParameters.RoleType.REGIONAL_SECURITY_COORDINATOR, "17XTSO-CS------W", CneExporterParameters.RoleType.CAPACITY_COORDINATOR, "2021-10-30T22:00Z/2021-10-31T23:00Z");
    }

    private void checkConstraintSeriesContent(ConstraintSeries cs, FlowCnec cnec, String businessType, List<String> countries, boolean asMnec,
                                              Double flowMw, Double patlA, Double patlMw, Double frm,
                                              Double tatlA, Double tatlMw, Double ptdf, Double patlMarginMw,
                                              Double patlObjMw, Double tatlMarginMw,
                                              Double tatlObjMw, Double lfMw, Double lfThresholdMw) {
        assertEquals(businessType, cs.getBusinessType());
        assertEquals(countries.size(), cs.getPartyMarketParticipant().size());
        for (PartyMarketParticipant pmp : cs.getPartyMarketParticipant()) {
            assertTrue(countries.contains(pmp.getMRID().getValue()));
        }
        assertEquals(asMnec ? "A49" : "A52", cs.getOptimizationMarketObjectStatusStatus());

        Optional<Contingency> contingency = cnec.getState().getContingency();
        if (contingency.isPresent()) {
            assertEquals(1, cs.getContingencySeries().size());
            assertTrue(cs.getContingencySeries().get(0).getMRID().contains(contingency.get().getId()));
            assertEquals(contingency.get().getId(), cs.getContingencySeries().get(0).getName());
        } else {
            assertTrue(cs.getContingencySeries().isEmpty());
        }

        assertEquals(1, cs.getMonitoredSeries().size());
        MonitoredSeries ms = cs.getMonitoredSeries().get(0);

        assertEquals(1, ms.getRegisteredResource().size());
        MonitoredRegisteredResource rr = ms.getRegisteredResource().get(0);

        // check measurements
        List<Analog> measurements = rr.getMeasurements();
        int iMeasure = 0;
        iMeasure += checkMeasurement(measurements, iMeasure, "A01", "MAW", flowMw);
        iMeasure += checkMeasurement(measurements, iMeasure, "A02", "AMP", patlA);
        iMeasure += checkMeasurement(measurements, iMeasure, "A02", "MAW", patlMw);
        iMeasure += checkMeasurement(measurements, iMeasure, "A03", "MAW", frm);
        iMeasure += checkMeasurement(measurements, iMeasure, "A07", "AMP", tatlA);
        iMeasure += checkMeasurement(measurements, iMeasure, "A07", "MAW", tatlMw);
        iMeasure += checkMeasurement(measurements, iMeasure, "Z11", "C62", ptdf);
        iMeasure += checkMeasurement(measurements, iMeasure, "Z12", "MAW", patlMarginMw);
        iMeasure += checkMeasurement(measurements, iMeasure, "Z13", "MAW", patlObjMw);
        iMeasure += checkMeasurement(measurements, iMeasure, "Z14", "MAW", tatlMarginMw);
        iMeasure += checkMeasurement(measurements, iMeasure, "Z15", "MAW", tatlObjMw);
        iMeasure += checkMeasurement(measurements, iMeasure, "Z16", "MAW", lfMw);
        iMeasure += checkMeasurement(measurements, iMeasure, "Z17", "MAW", lfThresholdMw);

        assertEquals(iMeasure, measurements.size());
    }

    private int checkMeasurement(List<Analog> measurements, int index, String expectedType, String expectedUnit, Double expectedValue) {
        if (expectedValue != null) {
            Analog measurement = measurements.get(index);
            assertEquals(expectedType, measurement.getMeasurementType());
            assertEquals(expectedUnit, measurement.getUnitSymbol());
            assertEquals(Math.abs(expectedValue), measurement.getAnalogValuesValue(), 1e-6);
            if (expectedUnit.equals("C62")) {
                assertNull(measurement.getPositiveFlowIn());
            } else {
                assertEquals(expectedValue < 0 ? "A02" : "A01", measurement.getPositiveFlowIn());
            }
            return 1;
        } else {
            return 0;
        }
    }

    private void mockCnecResult(FlowCnec cnec, double flowA, double flowMw, double marginA, double marginMw, double relMarginA, double relMarginMw, double ptdf) {
        Side monitoredSide = cnec.getMonitoredSides().contains(Side.LEFT) ? Side.LEFT : Side.RIGHT;
        Mockito.when(raoResult.getFlow(any(), eq(cnec), eq(monitoredSide), eq(Unit.AMPERE))).thenReturn(flowA);
        Mockito.when(raoResult.getFlow(any(), eq(cnec), eq(monitoredSide), eq(Unit.MEGAWATT))).thenReturn(flowMw);
        Mockito.when(raoResult.getMargin(any(), eq(cnec), eq(Unit.AMPERE))).thenReturn(marginA);
        Mockito.when(raoResult.getMargin(any(), eq(cnec), eq(Unit.MEGAWATT))).thenReturn(marginMw);
        Mockito.when(raoResult.getRelativeMargin(any(), eq(cnec), eq(Unit.AMPERE))).thenReturn(relMarginA);
        Mockito.when(raoResult.getRelativeMargin(any(), eq(cnec), eq(Unit.MEGAWATT))).thenReturn(relMarginMw);
        Mockito.when(raoResult.getPtdfZonalSum(any(), eq(cnec), eq(monitoredSide))).thenReturn(ptdf);
    }

    @Test
    public void testExportTwoPreventiveCnecs() {
        FlowCnec cnec1 = crac.newFlowCnec()
            .withId("bbb_cnec1")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withOperator("FR")
            .withInstant(Instant.PREVENTIVE)
            .withOptimized()
            .withNominalVoltage(400.)
            .withReliabilityMargin(0.)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(100.).withSide(Side.RIGHT).add()
            .add();

        mockCnecResult(cnec1, 40, 80, 10, 20, 100, 200, .1);

        FlowCnec cnec2 = crac.newFlowCnec()
            .withId("aaa_cnec2")
            .withNetworkElement("NNL2AA1  NNL3AA1  1")
            .withOperator("NL")
            .withInstant(Instant.PREVENTIVE)
            .withOptimized()
            .withNominalVoltage(400.)
            .withReliabilityMargin(10.)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(1000.).withSide(Side.RIGHT).add()
            .add();

        mockCnecResult(cnec2, 400, 800, -100, -200, -99999999, -999999999, .2);

        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        CneHelper cneHelper = new CneHelper(crac, network, raoResult, raoParameters, exporterParameters);
        CoreCneCnecsCreator cneCnecsCreator = new CoreCneCnecsCreator(cneHelper, new MockCracCreationContext(crac));

        List<ConstraintSeries> cnecsConstraintSeries = cneCnecsCreator.generate();

        // check size
        assertEquals(4, cnecsConstraintSeries.size());

        // check contents
        // start with cnec2 (name starts with aaa)
        checkConstraintSeriesContent(cnecsConstraintSeries.get(0), cnec2, "B88", List.of("10X1001A1001A361"), false,
            800., 1443., 1000., 10., 1443., 1000., .2, 200., 950.,
            200., 950., null, null);
        checkConstraintSeriesContent(cnecsConstraintSeries.get(1), cnec2, "B57", List.of("10X1001A1001A361"), false,
            800., null, null, 10., 1443., 1000., .2, null, null,
            200., 950., null, null);
        /* TODO : reactivate this when we go back to exporting B54 series even if no CRAs are applied
        checkConstraintSeriesContent(cnecsConstraintSeries.get(2), cnec2, "B54", List.of("10X1001A1001A361"), false,
            800., 1443., 1000., 10., null, null, .2, 200., 950.,
            null, null, null, null);
                 */

        // then cnec1 (name starts with bbb)
        checkConstraintSeriesContent(cnecsConstraintSeries.get(2), cnec1, "B88", List.of("10XFR-RTE------Q"), false,
            80., 144., 100., 0., 144., 100., .1, 20., 200.,
            20., 200., null, null);
        checkConstraintSeriesContent(cnecsConstraintSeries.get(3), cnec1, "B57", List.of("10XFR-RTE------Q"), false,
            80., null, null, 0., 144., 100., .1, null, null,
            20., 200., null, null);
        /* TODO : reactivate this when we go back to exporting B54 series even if no CRAs are applied
        checkConstraintSeriesContent(cnecsConstraintSeries.get(5), cnec1, "B54", List.of("10XFR-RTE------Q"), false,
            80., 144., 100., 0., null, null, .1, 20., 200.,
            null, null, null, null);
                 */
    }

    @Test
    public void testExportPreventivePureMnec() {
        FlowCnec cnec1 = crac.newFlowCnec()
            .withId("cnec1")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withOperator("D8")
            .withInstant(Instant.PREVENTIVE)
            .withMonitored()
            .withNominalVoltage(400.)
            .withReliabilityMargin(0.)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(100.).withSide(Side.RIGHT).add()
            .add();

        mockCnecResult(cnec1, 40, 80, 10, 20, 100, 200, .1);

        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        CneHelper cneHelper = new CneHelper(crac, network, raoResult, raoParameters, exporterParameters);
        CoreCneCnecsCreator cneCnecsCreator = new CoreCneCnecsCreator(cneHelper, new MockCracCreationContext(crac));

        List<ConstraintSeries> cnecsConstraintSeries = cneCnecsCreator.generate();

        // check size
        assertEquals(2, cnecsConstraintSeries.size());

        checkConstraintSeriesContent(cnecsConstraintSeries.get(0), cnec1, "B88", List.of("10XDE-VE-TRANSMK"), true,
            80., 144., 100., 0., 144., 100., .1, 20., 200.,
            20., 200., null, null);
        checkConstraintSeriesContent(cnecsConstraintSeries.get(1), cnec1, "B57", List.of("10XDE-VE-TRANSMK"), true,
            80., null, null, 0., 144., 100., .1, null, null,
            20., 200., null, null);
        /* TODO : reactivate this when we go back to exporting B54 series even if no CRAs are applied
        checkConstraintSeriesContent(cnecsConstraintSeries.get(2), cnec1, "B54", List.of("10XDE-VE-TRANSMK"), true,
            80., 144., 100., 0., null, null, null, 20., null,
            null, null, null, null);
         */
    }

    @Test
    public void testExportPreventiveCnecAndMnec() {
        FlowCnec cnec1 = crac.newFlowCnec()
            .withId("cnec1")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withOperator("D7")
            .withInstant(Instant.PREVENTIVE)
            .withOptimized()
            .withMonitored()
            .withNominalVoltage(400.)
            .withReliabilityMargin(10.)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(100.).withSide(Side.RIGHT).add()
            .add();

        mockCnecResult(cnec1, 40, 80, 10, 20, 100, 200, .1);

        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        CneHelper cneHelper = new CneHelper(crac, network, raoResult, raoParameters, exporterParameters);
        CoreCneCnecsCreator cneCnecsCreator = new CoreCneCnecsCreator(cneHelper, new MockCracCreationContext(crac));

        List<ConstraintSeries> cnecsConstraintSeries = cneCnecsCreator.generate();

        // check size
        assertEquals(2, cnecsConstraintSeries.size());

        /* TODO : reactivate this when we go back to exporting CNEC+MNEC branches as both a CNEC and a MNEC
        checkConstraintSeriesContent(cnecsConstraintSeries.get(0), cnec1, "B88", List.of("10XDE-RWENET---W"), true,
            80., 144., 100., 10., 144., 100., null, 20., null,
            20., null, null, null);
        checkConstraintSeriesContent(cnecsConstraintSeries.get(1), cnec1, "B57", List.of("10XDE-RWENET---W"), true,
            80., null, null, 10., 144., 100., null, null, null,
            20., null, null, null);
        checkConstraintSeriesContent(cnecsConstraintSeries.get(2), cnec1, "B54", List.of("10XDE-RWENET---W"), true,
            80., 144., 100., 10., null, null, null, 20., null,
            null, null, null, null);*/

        checkConstraintSeriesContent(cnecsConstraintSeries.get(0), cnec1, "B88", List.of("10XDE-RWENET---W"), false,
            80., 144., 100., 10., 144., 100., .1, 20., 100.,
            20., 100., null, null);
        checkConstraintSeriesContent(cnecsConstraintSeries.get(1), cnec1, "B57", List.of("10XDE-RWENET---W"), false,
            80., null, null, 10., 144., 100., .1, null, null,
            20., 100., null, null);
        /* TODO : reactivate this when we go back to exporting B54 series even if no CRAs are applied
        checkConstraintSeriesContent(cnecsConstraintSeries.get(2), cnec1, "B54", List.of("10XDE-RWENET---W"), false,
            80., 144., 100., 10., null, null, .1, 20., 100.,
            null, null, null, null);
         */
    }

    @Test
    public void testCurativeCnecs() {
        crac.newContingency()
            .withId("contingency1")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .add();
        FlowCnec cnecPrev = crac.newFlowCnec()
            .withId("zzz_cnec1 - N")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withOperator("D2")
            .withInstant(Instant.PREVENTIVE)
            .withOptimized()
            .withNominalVoltage(400.)
            .withReliabilityMargin(30.)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(100.).withSide(Side.RIGHT).add()
            .add();
        FlowCnec cnecOutage = crac.newFlowCnec()
            .withId("cnec1 - Outage")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withOperator("D2")
            .withInstant(Instant.OUTAGE)
            .withContingency("contingency1")
            .withOptimized()
            .withNominalVoltage(400.)
            .withReliabilityMargin(20.)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(200.).withSide(Side.RIGHT).add()
            .add();
        FlowCnec cnecCur = crac.newFlowCnec()
            .withId("cnec1 - Curative")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withOperator("D2")
            .withInstant(Instant.CURATIVE)
            .withContingency("contingency1")
            .withOptimized()
            .withNominalVoltage(400.)
            .withReliabilityMargin(20.)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(150.).withSide(Side.RIGHT).add()
            .add();

        mockCnecResult(cnecPrev, 40, 80, 10, 20, 100, 200, .1);

        mockCnecResult(cnecOutage, 45, 85, 15, 25, 105, 205, .1);
        mockCnecResult(cnecCur, 45, 85, 18, 28, 108, 208, .1);

        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        when(raoResult.getActivatedNetworkActionsDuringState(crac.getState(cnecCur.getState().getContingency().orElseThrow(), Instant.CURATIVE))).thenReturn(Set.of(Mockito.mock(NetworkAction.class)));
        CneHelper cneHelper = new CneHelper(crac, network, raoResult, raoParameters, exporterParameters);
        CoreCneCnecsCreator cneCnecsCreator = new CoreCneCnecsCreator(cneHelper, new MockCracCreationContext(crac));

        List<ConstraintSeries> cnecsConstraintSeries = cneCnecsCreator.generate();

        // check size
        assertEquals(5, cnecsConstraintSeries.size());
        List<String> tsos = List.of("10XDE-EON-NETZ-C");

        // preventive cnec
        checkConstraintSeriesContent(cnecsConstraintSeries.get(3), cnecPrev, "B88", tsos, false,
            80., 144., 100., 30., 144., 100., .1, 20., -10.,
            20., -10., null, null);
        checkConstraintSeriesContent(cnecsConstraintSeries.get(4), cnecPrev, "B57", tsos, false,
            80., null, null, 30., 144., 100., .1, null, null,
            20., -10., null, null);
        /* TODO : reactivate this when we go back to exporting B54 series even if no CRAs are applied
        checkConstraintSeriesContent(cnecsConstraintSeries.get(5), cnecPrev, "B54", tsos, false,
            80., 144., 100., 30., null, null, .1, 20., -10.,
            null, null, null, null);
                 */

        // curative cnecs
        checkConstraintSeriesContent(cnecsConstraintSeries.get(0), cnecCur, "B88", tsos, false,
            85., 217., 150., 20., 289., 200., .1, 65., 450.,
            115., 950., null, null);
        checkConstraintSeriesContent(cnecsConstraintSeries.get(1), cnecOutage, "B57", tsos, false,
            85., null, null, 20., 289., 200., .1, null, null,
            115., 950., null, null);
        checkConstraintSeriesContent(cnecsConstraintSeries.get(2), cnecCur, "B54", tsos, false,
            85., 217., 150., 20., null, null, .1, 65., 450.,
            null, null, null, null);
    }

    @Test
    public void testWithLoopFlow() {
        FlowCnec cnec1 = crac.newFlowCnec()
            .withId("cnec1")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withOperator("D4")
            .withInstant(Instant.PREVENTIVE)
            .withOptimized()
            .withNominalVoltage(400.)
            .withReliabilityMargin(0.)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(100.).withSide(Side.RIGHT).add()
            .add();
        cnec1.newExtension(LoopFlowThresholdAdder.class).withValue(321.).withUnit(Unit.MEGAWATT).add();

        mockCnecResult(cnec1, 40, 80, 10, 20, 100, 200, .1);
        Mockito.when(raoResult.getLoopFlow(any(), eq(cnec1), eq(Side.RIGHT), eq(Unit.MEGAWATT))).thenReturn(123.);

        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        raoParameters.setRaoWithLoopFlowLimitation(true);
        CneHelper cneHelper = new CneHelper(crac, network, raoResult, raoParameters, exporterParameters);
        CoreCneCnecsCreator cneCnecsCreator = new CoreCneCnecsCreator(cneHelper, new MockCracCreationContext(crac));

        List<ConstraintSeries> cnecsConstraintSeries = cneCnecsCreator.generate();

        // check size
        assertEquals(2, cnecsConstraintSeries.size());
        List<String> tsos = List.of("10XDE-ENBW--TNGX");

        checkConstraintSeriesContent(cnecsConstraintSeries.get(0), cnec1, "B88", tsos, false,
            80., 144., 100., 0., 144., 100., .1, 20., 200.,
            20., 200., 123., 321.);
        checkConstraintSeriesContent(cnecsConstraintSeries.get(1), cnec1, "B57", tsos, false,
            80., null, null, 0., 144., 100., .1, null, null,
            20., 200., 123., 321.);
        /* TODO : reactivate this when we go back to exporting B54 series even if no CRAs are applied
        checkConstraintSeriesContent(cnecsConstraintSeries.get(2), cnec1, "B54", countries, false,
            80., 144., 100., 0., null, null, .1, 20., 200.,
            null, null, 123., 321.);
                 */
    }
}
