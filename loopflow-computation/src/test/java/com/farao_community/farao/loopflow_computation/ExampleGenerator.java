/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.CracImplFactory;
import com.farao_community.farao.data.refprog.reference_program.ReferenceExchangeData;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.mockito.Mockito;

import java.util.*;

import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * Test case is a 4 nodes network, with 4 countries.
 *
 *       FR   (+100 MW)       BE 1  (+125 MW)
 *          + ------------ +
 *          |              |
 *          |              +  BE 2 (-125 MW)
 *          |              |
 *          + ------------ +
 *       DE   (0 MW)          NL  (-100 MW)
 *
 * All lines have same impedance and are monitored.
 * Each Country GLSK is a simple one node GLSK, except for Belgium where GLSKs on both nodes are equally distributed
 * Compensation is considered as equally shared on each country, and there are no losses.
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class ExampleGenerator {

    private ExampleGenerator() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    static Network network() {
        Network network = Network.create("Test", "code");
        Substation substationFr = network.newSubstation()
            .setId("Substation FR")
            .setName("Substation FR")
            .setCountry(Country.FR)
            .add();
        VoltageLevel voltageLevelFr = substationFr.newVoltageLevel()
            .setId("Voltage level FR")
            .setName("Voltage level FR")
            .setNominalV(400)
            .setTopologyKind(TopologyKind.BUS_BREAKER)
            .setLowVoltageLimit(300)
            .setHighVoltageLimit(500)
            .add();
        voltageLevelFr.getBusBreakerView()
            .newBus()
            .setId("Bus FR")
            .setName("Bus FR")
            .add();
        voltageLevelFr.newGenerator()
            .setId("Generator FR")
            .setName("Generator FR")
            .setBus("Bus FR")
            .setEnergySource(EnergySource.OTHER)
            .setMinP(1000)
            .setMaxP(2000)
            .setRatedS(100)
            .setTargetP(1600)
            .setTargetV(400)
            .setVoltageRegulatorOn(true)
            .add();
        voltageLevelFr.newLoad()
            .setId("Load FR")
            .setName("Load FR")
            .setBus("Bus FR")
            .setLoadType(LoadType.UNDEFINED)
            .setP0(1500)
            .setQ0(0)
            .add();

        Substation substationBe = network.newSubstation()
            .setId("Substation BE")
            .setName("Substation BE")
            .setCountry(Country.BE)
            .add();
        VoltageLevel voltageLevelBe = substationBe.newVoltageLevel()
            .setId("Voltage level BE")
            .setName("Voltage level BE")
            .setNominalV(400)
            .setTopologyKind(TopologyKind.BUS_BREAKER)
            .setLowVoltageLimit(300)
            .setHighVoltageLimit(500)
            .add();
        voltageLevelBe.getBusBreakerView()
            .newBus()
            .setId("Bus BE 1")
            .setName("Bus BE 1")
            .add();
        voltageLevelBe.newGenerator()
            .setId("Generator BE 1")
            .setName("Generator BE 1")
            .setBus("Bus BE 1")
            .setEnergySource(EnergySource.OTHER)
            .setMinP(1000)
            .setMaxP(2000)
            .setRatedS(100)
            .setTargetP(1625)
            .setTargetV(400)
            .setVoltageRegulatorOn(true)
            .add();
        voltageLevelBe.newLoad()
            .setId("Load BE 1")
            .setName("Load BE 1")
            .setBus("Bus BE 1")
            .setLoadType(LoadType.UNDEFINED)
            .setP0(1500)
            .setQ0(0)
            .add();
        voltageLevelBe.getBusBreakerView()
            .newBus()
            .setId("Bus BE 2")
            .setName("Bus BE 2")
            .add();
        voltageLevelBe.newGenerator()
            .setId("Generator BE 2")
            .setName("Generator BE 2")
            .setBus("Bus BE 2")
            .setEnergySource(EnergySource.OTHER)
            .setMinP(1000)
            .setMaxP(2000)
            .setRatedS(100)
            .setTargetP(1500)
            .setTargetV(400)
            .setVoltageRegulatorOn(true)
            .add();
        voltageLevelBe.newLoad()
            .setId("Load BE 2")
            .setName("Load BE 2")
            .setBus("Bus BE 2")
            .setLoadType(LoadType.UNDEFINED)
            .setP0(1625)
            .setQ0(0)
            .add();

        Substation substationDe = network.newSubstation()
            .setId("Substation DE")
            .setName("Substation DE")
            .setCountry(Country.DE)
            .add();
        VoltageLevel voltageLevelDe = substationDe.newVoltageLevel()
            .setId("Voltage level DE")
            .setName("Voltage level DE")
            .setNominalV(400)
            .setTopologyKind(TopologyKind.BUS_BREAKER)
            .setLowVoltageLimit(300)
            .setHighVoltageLimit(500)
            .add();
        voltageLevelDe.getBusBreakerView()
            .newBus()
            .setId("Bus DE")
            .setName("Bus DE")
            .add();
        voltageLevelDe.newGenerator()
            .setId("Generator DE")
            .setName("Generator DE")
            .setBus("Bus DE")
            .setEnergySource(EnergySource.OTHER)
            .setMinP(1000)
            .setMaxP(2000)
            .setRatedS(100)
            .setTargetP(1500)
            .setTargetV(400)
            .setVoltageRegulatorOn(true)
            .add();
        voltageLevelDe.newLoad()
            .setId("Load DE")
            .setName("Load DE")
            .setBus("Bus DE")
            .setLoadType(LoadType.UNDEFINED)
            .setP0(1500)
            .setQ0(0)
            .add();

        Substation substationNl = network.newSubstation()
            .setId("Substation NL")
            .setName("Substation NL")
            .setCountry(Country.NL)
            .add();
        VoltageLevel voltageLevelNl = substationNl.newVoltageLevel()
            .setId("Voltage level NL")
            .setName("Voltage level NL")
            .setNominalV(400)
            .setTopologyKind(TopologyKind.BUS_BREAKER)
            .setLowVoltageLimit(300)
            .setHighVoltageLimit(500)
            .add();
        voltageLevelNl.getBusBreakerView()
            .newBus()
            .setId("Bus NL")
            .setName("Bus NL")
            .add();
        voltageLevelNl.newGenerator()
            .setId("Generator NL")
            .setName("Generator NL")
            .setBus("Bus NL")
            .setEnergySource(EnergySource.OTHER)
            .setMinP(1000)
            .setMaxP(2000)
            .setRatedS(100)
            .setTargetP(1500)
            .setTargetV(400)
            .setVoltageRegulatorOn(true)
            .add();
        voltageLevelNl.newLoad()
            .setId("Load NL")
            .setName("Load NL")
            .setBus("Bus NL")
            .setLoadType(LoadType.UNDEFINED)
            .setP0(1600)
            .setQ0(0)
            .add();

        network.newLine()
            .setId("FR-BE1")
            .setName("FR-BE1")
            .setVoltageLevel1("Voltage level FR")
            .setVoltageLevel2("Voltage level BE")
            .setBus1("Bus FR")
            .setBus2("Bus BE 1")
            .setR(0)
            .setX(5)
            .setB1(0)
            .setB2(0)
            .setG1(0)
            .setG2(0)
            .add();
        network.newLine()
            .setId("FR-DE")
            .setName("FR-DE")
            .setVoltageLevel1("Voltage level FR")
            .setVoltageLevel2("Voltage level DE")
            .setBus1("Bus FR")
            .setBus2("Bus DE")
            .setR(0)
            .setX(5)
            .setB1(0)
            .setB2(0)
            .setG1(0)
            .setG2(0)
            .add();
        network.newLine()
            .setId("BE2-NL")
            .setName("BE2-NL")
            .setVoltageLevel1("Voltage level BE")
            .setVoltageLevel2("Voltage level NL")
            .setBus1("Bus BE 2")
            .setBus2("Bus NL")
            .setR(0)
            .setX(5)
            .setB1(0)
            .setB2(0)
            .setG1(0)
            .setG2(0)
            .add();
        network.newLine()
            .setId("DE-NL")
            .setName("DE-NL")
            .setVoltageLevel1("Voltage level DE")
            .setVoltageLevel2("Voltage level NL")
            .setBus1("Bus DE")
            .setBus2("Bus NL")
            .setR(0)
            .setX(5)
            .setB1(0)
            .setB2(0)
            .setG1(0)
            .setG2(0)
            .add();
        network.newLine()
            .setId("BE1-BE2")
            .setName("BE1-BE2")
            .setVoltageLevel1("Voltage level BE")
            .setVoltageLevel2("Voltage level BE")
            .setBus1("Bus BE 1")
            .setBus2("Bus BE 2")
            .setR(0)
            .setX(5)
            .setB1(0)
            .setB2(0)
            .setG1(0)
            .setG2(0)
            .add();

        return network;
    }

    static Crac crac() {
        Crac crac = new CracImplFactory().create("test-crac");

        crac.newFlowCnec()
            .withId("FR-BE1")
            .withInstant(Instant.PREVENTIVE)
            .withNetworkElement("FR-BE1")
            .newThreshold()
                .withMin(-200.)
                .withMax(200.)
                .withUnit(MEGAWATT)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .add()
            .add();

        crac.newFlowCnec()
            .withId("FR-DE")
            .withInstant(Instant.PREVENTIVE)
            .withNetworkElement("FR-DE")
            .newThreshold()
                .withMin(-200.)
                .withMax(200.)
                .withUnit(MEGAWATT)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .add()
            .add();

        crac.newFlowCnec()
            .withId("BE2-NL")
            .withInstant(Instant.PREVENTIVE)
            .withNetworkElement("BE2-NL")
            .newThreshold()
                .withMin(-200.)
                .withMax(200.)
                .withUnit(MEGAWATT)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .add()
            .add();

        crac.newFlowCnec()
            .withId("DE-NL")
            .withInstant(Instant.PREVENTIVE)
            .withNetworkElement("DE-NL")
            .newThreshold()
                .withMin(-200.)
                .withMax(200.)
                .withUnit(MEGAWATT)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .add()
            .add();

        crac.newFlowCnec()
            .withId("BE1-BE2")
            .withInstant(Instant.PREVENTIVE)
            .withNetworkElement("BE1-BE2")
            .newThreshold()
                .withMin(-200.)
                .withMax(200.)
                .withUnit(MEGAWATT)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .add()
            .add();

        return crac;
    }

    static ZonalData<LinearGlsk> glskProvider() {
        HashMap<String, Float> glskBe = new HashMap<>();
        glskBe.put("Generator BE 1", 0.5f);
        glskBe.put("Generator BE 2", 0.5f);

        Map<String, LinearGlsk> glsks = new HashMap<>();
        glsks.put("10YFR-RTE------C", new LinearGlsk("10YFR-RTE------C", "FR", Collections.singletonMap("Generator FR", 1.f)));
        glsks.put("10YBE----------2", new LinearGlsk("10YBE----------2", "BE", glskBe));
        glsks.put("10YCB-GERMANY--8", new LinearGlsk("10YCB-GERMANY--8", "DE", Collections.singletonMap("Generator DE", 1.f)));
        glsks.put("10YNL----------L", new LinearGlsk("10YNL----------L", "NL", Collections.singletonMap("Generator NL", 1.f)));
        return new ZonalDataImpl<>(glsks);
    }

    static ReferenceProgram referenceProgram() {
        EICode areaFrance = new EICode(Country.FR);
        EICode areaBelgium = new EICode(Country.BE);
        EICode areaNetherlands = new EICode(Country.NL);
        EICode areaGermany = new EICode(Country.DE);
        List<ReferenceExchangeData> exchangeDataList = Arrays.asList(
            new ReferenceExchangeData(areaFrance, areaBelgium, 50),
            new ReferenceExchangeData(areaFrance, areaGermany, 50),
            new ReferenceExchangeData(areaBelgium, areaNetherlands, 50),
            new ReferenceExchangeData(areaGermany, areaNetherlands, 50));
        return new ReferenceProgram(exchangeDataList);
    }

    static SystematicSensitivityResult systematicSensitivityResult(Crac crac, ZonalData<LinearGlsk> glsk) {
        SystematicSensitivityResult sensisResults = Mockito.mock(SystematicSensitivityResult.class);

        // flow results
        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("FR-BE1"))).thenReturn(30.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("BE1-BE2"))).thenReturn(280.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("FR-DE"))).thenReturn(170.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("BE2-NL"))).thenReturn(30.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("DE-NL"))).thenReturn(170.);

        // sensi results
        LinearGlsk glskFr = glsk.getData("10YFR-RTE------C");
        LinearGlsk glskBe = glsk.getData("10YBE----------2");
        LinearGlsk glskDe = glsk.getData("10YCB-GERMANY--8");
        LinearGlsk glskNl = glsk.getData("10YNL----------L");

        Mockito.when(sensisResults.getSensitivityOnFlow(glskFr, crac.getBranchCnec("FR-BE1"))).thenReturn(0.);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskBe, crac.getBranchCnec("FR-BE1"))).thenReturn(-1.5);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskDe, crac.getBranchCnec("FR-BE1"))).thenReturn(-0.4);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskNl, crac.getBranchCnec("FR-BE1"))).thenReturn(-0.8);

        Mockito.when(sensisResults.getSensitivityOnFlow(glskFr, crac.getBranchCnec("BE1-BE2"))).thenReturn(0.);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskBe, crac.getBranchCnec("BE1-BE2"))).thenReturn(0.);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskDe, crac.getBranchCnec("BE1-BE2"))).thenReturn(-0.4);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskNl, crac.getBranchCnec("BE1-BE2"))).thenReturn(-0.8);

        Mockito.when(sensisResults.getSensitivityOnFlow(glskFr, crac.getBranchCnec("FR-DE"))).thenReturn(0.);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskBe, crac.getBranchCnec("FR-DE"))).thenReturn(-0.5);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskDe, crac.getBranchCnec("FR-DE"))).thenReturn(-1.6);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskNl, crac.getBranchCnec("FR-DE"))).thenReturn(-1.2);

        Mockito.when(sensisResults.getSensitivityOnFlow(glskFr, crac.getBranchCnec("BE2-NL"))).thenReturn(0.);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskBe, crac.getBranchCnec("BE2-NL"))).thenReturn(0.5);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskDe, crac.getBranchCnec("BE2-NL"))).thenReturn(-0.4);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskNl, crac.getBranchCnec("BE2-NL"))).thenReturn(-0.8);

        Mockito.when(sensisResults.getSensitivityOnFlow(glskFr, crac.getBranchCnec("DE-NL"))).thenReturn(0.);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskBe, crac.getBranchCnec("DE-NL"))).thenReturn(-0.5);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskDe, crac.getBranchCnec("DE-NL"))).thenReturn(0.4);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskNl, crac.getBranchCnec("DE-NL"))).thenReturn(-1.2);

        return sensisResults;
    }
}
