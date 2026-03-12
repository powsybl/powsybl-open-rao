/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.loopflowcomputation;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.EnergySource;
import com.powsybl.iidm.network.LoadType;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.openrao.commons.EICode;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.impl.CracImplFactory;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceExchangeData;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.WeightedSensitivityVariable;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.powsybl.openrao.commons.Unit.MEGAWATT;

/**
 * Test case is a network with 5 nodes and 1 xnode (in 4 countries).
 *
 *       FR   (+100 MW)       BE 1  (+125 MW)
 *          + ------------ +---------------------------+ XBE (-25 MW)
 *          |              |
 *          |              +  BE 2 (-100 MW)
 *          |              |
 *          + ------------ +
 *       DE   (0 MW)          NL  (-100 MW)
 *
 * All lines have same impedance and are monitored.
 * Each Country GLSK is a simple one node GLSK, except for Belgium where GLSKs are equally distributed
 * on the 2 nodes + the xnode
 * Compensation is considered as equally shared on each country, and there are no losses.
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class ExampleGenerator {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";

    private ExampleGenerator() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    static Network network() {
        Network network = Network.create("Test", "code");
        Substation substationFr = network.newSubstation()
            .setId("Substation FR").setName("Substation FR").setCountry(Country.FR)
            .add();
        VoltageLevel voltageLevelFr = substationFr.newVoltageLevel()
            .setId("Voltage level FR").setName("Voltage level FR")
            .setTopologyKind(TopologyKind.BUS_BREAKER)
            .setNominalV(400).setLowVoltageLimit(300).setHighVoltageLimit(500)
            .add();
        voltageLevelFr.getBusBreakerView().newBus()
            .setId("Bus FR").setName("Bus FR")
            .add();
        voltageLevelFr.newGenerator()
            .setId("Generator FR").setName("Generator FR")
            .setBus("Bus FR").setEnergySource(EnergySource.OTHER)
            .setMinP(1000).setMaxP(2000).setRatedS(100).setTargetP(1600)
            .setTargetV(400).setVoltageRegulatorOn(true)
            .add();
        voltageLevelFr.newLoad()
            .setId("Load FR").setName("Load FR")
            .setBus("Bus FR").setLoadType(LoadType.UNDEFINED)
            .setP0(1500).setQ0(0)
            .add();

        Substation substationBe = network.newSubstation()
            .setId("Substation BE").setName("Substation BE").setCountry(Country.BE)
            .add();
        VoltageLevel voltageLevelBe = substationBe.newVoltageLevel()
            .setId("Voltage level BE").setName("Voltage level BE")
            .setTopologyKind(TopologyKind.BUS_BREAKER)
            .setNominalV(400).setLowVoltageLimit(300).setHighVoltageLimit(500)
            .add();
        voltageLevelBe.getBusBreakerView().newBus()
            .setId("Bus BE 1").setName("Bus BE 1")
            .add();
        voltageLevelBe.newGenerator()
            .setId("Generator BE 1").setName("Generator BE 1")
            .setBus("Bus BE 1").setEnergySource(EnergySource.OTHER)
            .setMinP(1000).setMaxP(2000).setRatedS(100).setTargetP(1625)
            .setTargetV(400).setVoltageRegulatorOn(true)
            .add();
        voltageLevelBe.newLoad()
            .setId("Load BE 1").setName("Load BE 1")
            .setBus("Bus BE 1").setLoadType(LoadType.UNDEFINED)
            .setP0(1500).setQ0(0)
            .add();
        voltageLevelBe.getBusBreakerView().newBus()
            .setId("Bus BE 2").setName("Bus BE 2")
            .add();
        voltageLevelBe.newGenerator()
            .setId("Generator BE 2").setName("Generator BE 2")
            .setBus("Bus BE 2").setEnergySource(EnergySource.OTHER)
            .setMinP(1000).setMaxP(2000).setRatedS(100).setTargetP(1500)
            .setTargetV(400).setVoltageRegulatorOn(true)
            .add();
        voltageLevelBe.newLoad()
            .setId("Load BE 2").setName("Load BE 2")
            .setBus("Bus BE 2").setLoadType(LoadType.UNDEFINED)
            .setP0(1600).setQ0(0)
            .add();

        Substation substationDe = network.newSubstation()
            .setId("Substation DE").setName("Substation DE").setCountry(Country.DE)
            .add();
        VoltageLevel voltageLevelDe = substationDe.newVoltageLevel()
            .setId("Voltage level DE").setName("Voltage level DE")
            .setTopologyKind(TopologyKind.BUS_BREAKER)
            .setNominalV(400).setLowVoltageLimit(300).setHighVoltageLimit(500)
            .add();
        voltageLevelDe.getBusBreakerView().newBus()
            .setId("Bus DE").setName("Bus DE")
            .add();
        voltageLevelDe.newGenerator()
            .setId("Generator DE").setName("Generator DE")
            .setBus("Bus DE").setEnergySource(EnergySource.OTHER)
            .setMinP(1000).setMaxP(2000).setRatedS(100).setTargetP(1500)
            .setTargetV(400).setVoltageRegulatorOn(true)
            .add();
        voltageLevelDe.newLoad()
            .setId("Load DE").setName("Load DE")
            .setBus("Bus DE").setLoadType(LoadType.UNDEFINED)
            .setP0(1500).setQ0(0)
            .add();

        Substation substationNl = network.newSubstation()
            .setId("Substation NL").setName("Substation NL").setCountry(Country.NL)
            .add();
        VoltageLevel voltageLevelNl = substationNl.newVoltageLevel()
            .setId("Voltage level NL").setName("Voltage level NL")
            .setTopologyKind(TopologyKind.BUS_BREAKER)
            .setNominalV(400).setLowVoltageLimit(300).setHighVoltageLimit(500)
            .add();
        voltageLevelNl.getBusBreakerView().newBus()
            .setId("Bus NL").setName("Bus NL")
            .add();
        voltageLevelNl.newGenerator()
            .setId("Generator NL").setName("Generator NL")
            .setBus("Bus NL").setEnergySource(EnergySource.OTHER)
            .setMinP(1000).setMaxP(2000).setRatedS(100).setTargetP(1500)
            .setTargetV(400).setVoltageRegulatorOn(true)
            .add();
        voltageLevelNl.newLoad()
            .setId("Load NL").setName("Load NL")
            .setBus("Bus NL").setLoadType(LoadType.UNDEFINED)
            .setP0(1600).setQ0(0)
            .add();

        network.newLine()
            .setId("FR-BE1").setName("FR-BE1")
            .setVoltageLevel1("Voltage level FR").setVoltageLevel2("Voltage level BE")
            .setBus1("Bus FR").setBus2("Bus BE 1")
            .setR(0).setX(5).setB1(0).setB2(0).setG1(0).setG2(0)
            .add();
        network.newLine()
            .setId("FR-DE").setName("FR-DE")
            .setVoltageLevel1("Voltage level FR").setVoltageLevel2("Voltage level DE")
            .setBus1("Bus FR").setBus2("Bus DE")
            .setR(0).setX(5).setB1(0).setB2(0).setG1(0).setG2(0)
            .add();
        network.newLine()
            .setId("BE2-NL").setName("BE2-NL")
            .setVoltageLevel1("Voltage level BE").setVoltageLevel2("Voltage level NL")
            .setBus1("Bus BE 2").setBus2("Bus NL")
            .setR(0).setX(5).setB1(0).setB2(0).setG1(0).setG2(0)
            .add();
        network.newLine()
            .setId("DE-NL").setName("DE-NL")
            .setVoltageLevel1("Voltage level DE").setVoltageLevel2("Voltage level NL")
            .setBus1("Bus DE").setBus2("Bus NL")
            .setR(0).setX(5).setB1(0).setB2(0).setG1(0).setG2(0)
            .add();
        network.newLine()
            .setId("BE1-BE2").setName("BE1-BE2")
            .setVoltageLevel1("Voltage level BE").setVoltageLevel2("Voltage level BE")
            .setBus1("Bus BE 1").setBus2("Bus BE 2")
            .setR(0).setX(5).setB1(0).setB2(0).setG1(0).setG2(0)
            .add();
        voltageLevelBe.newDanglingLine()
            .setId("BE1-XBE").setBus("Bus BE 1").setPairingKey("XBE")
            .setP0(25).setQ0(0)
            .setR(0).setX(5).setB(0).setG(0)
            .add();

        return network;
    }

    static Crac crac() {
        Crac crac = new CracImplFactory().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE);

        crac.newFlowCnec()
            .withId("FR-BE1")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withNetworkElement("FR-BE1")
            .newThreshold().withMin(-200.).withMax(200.).withUnit(MEGAWATT).withSide(TwoSides.ONE).add()
            .add();

        crac.newFlowCnec()
            .withId("FR-DE")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withNetworkElement("FR-DE")
            .newThreshold().withMin(-200.).withMax(200.).withUnit(MEGAWATT).withSide(TwoSides.TWO).add()
            .add();

        crac.newFlowCnec()
            .withId("BE2-NL")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withNetworkElement("BE2-NL")
            .newThreshold().withMin(-200.).withMax(200.).withUnit(MEGAWATT).withSide(TwoSides.ONE).add()
            .add();

        crac.newFlowCnec()
            .withId("DE-NL")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withNetworkElement("DE-NL")
            .newThreshold().withMin(-200.).withMax(200.).withUnit(MEGAWATT).withSide(TwoSides.TWO).add()
            .add();

        crac.newFlowCnec()
            .withId("BE1-BE2")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withNetworkElement("BE1-BE2")
            .newThreshold().withMin(-200.).withMax(200.).withUnit(MEGAWATT).withSide(TwoSides.ONE).add()
            .add();

        return crac;
    }

    static ZonalData<SensitivityVariableSet> glskProvider() {
        List<WeightedSensitivityVariable> glskBe = new ArrayList<>();
        glskBe.add(new WeightedSensitivityVariable("Generator BE 1", 1.0f / 3.0f));
        glskBe.add(new WeightedSensitivityVariable("Generator BE 2", 1.0f / 3.0f));
        glskBe.add(new WeightedSensitivityVariable("BE1-XBE", 1.0f / 3.0f));

        Map<String, SensitivityVariableSet> glsks = new HashMap<>();
        glsks.put("10YFR-RTE------C", new SensitivityVariableSet("10YFR-RTE------C", List.of(new WeightedSensitivityVariable("Generator FR", 1.))));
        glsks.put("10YBE----------2", new SensitivityVariableSet("10YBE----------2", glskBe));
        glsks.put("10YCB-GERMANY--8", new SensitivityVariableSet("10YCB-GERMANY--8", List.of(new WeightedSensitivityVariable("Generator DE", 1.))));
        glsks.put("10YNL----------L", new SensitivityVariableSet("10YNL----------L", List.of(new WeightedSensitivityVariable("Generator NL", 1.))));
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

    static SystematicSensitivityResult systematicSensitivityResult(Crac crac, ZonalData<SensitivityVariableSet> glsk) {
        SystematicSensitivityResult sensisResults = Mockito.mock(SystematicSensitivityResult.class);

        // flow results
        Mockito.when(sensisResults.getReferenceFlow(crac.getFlowCnec("FR-BE1"), TwoSides.ONE)).thenReturn(30.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getFlowCnec("BE1-BE2"), TwoSides.ONE)).thenReturn(280.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getFlowCnec("FR-DE"), TwoSides.TWO)).thenReturn(170.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getFlowCnec("BE2-NL"), TwoSides.ONE)).thenReturn(30.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getFlowCnec("DE-NL"), TwoSides.TWO)).thenReturn(170.);

        // sensi results
        SensitivityVariableSet glskFr = glsk.getData("10YFR-RTE------C");
        SensitivityVariableSet glskBe = glsk.getData("10YBE----------2");
        SensitivityVariableSet glskDe = glsk.getData("10YCB-GERMANY--8");
        SensitivityVariableSet glskNl = glsk.getData("10YNL----------L");

        Mockito.when(sensisResults.getSensitivityOnFlow(glskFr, crac.getFlowCnec("FR-BE1"), TwoSides.ONE)).thenReturn(0.);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskBe, crac.getFlowCnec("FR-BE1"), TwoSides.ONE)).thenReturn(-1.5);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskDe, crac.getFlowCnec("FR-BE1"), TwoSides.ONE)).thenReturn(-0.4);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskNl, crac.getFlowCnec("FR-BE1"), TwoSides.ONE)).thenReturn(-0.8);

        Mockito.when(sensisResults.getSensitivityOnFlow(glskFr, crac.getFlowCnec("BE1-BE2"), TwoSides.ONE)).thenReturn(0.);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskBe, crac.getFlowCnec("BE1-BE2"), TwoSides.ONE)).thenReturn(0.);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskDe, crac.getFlowCnec("BE1-BE2"), TwoSides.ONE)).thenReturn(-0.4);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskNl, crac.getFlowCnec("BE1-BE2"), TwoSides.ONE)).thenReturn(-0.8);

        Mockito.when(sensisResults.getSensitivityOnFlow(glskFr, crac.getFlowCnec("FR-DE"), TwoSides.TWO)).thenReturn(0.);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskBe, crac.getFlowCnec("FR-DE"), TwoSides.TWO)).thenReturn(-0.5);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskDe, crac.getFlowCnec("FR-DE"), TwoSides.TWO)).thenReturn(-1.6);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskNl, crac.getFlowCnec("FR-DE"), TwoSides.TWO)).thenReturn(-1.2);

        Mockito.when(sensisResults.getSensitivityOnFlow(glskFr, crac.getFlowCnec("BE2-NL"), TwoSides.ONE)).thenReturn(0.);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskBe, crac.getFlowCnec("BE2-NL"), TwoSides.ONE)).thenReturn(0.5);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskDe, crac.getFlowCnec("BE2-NL"), TwoSides.ONE)).thenReturn(-0.4);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskNl, crac.getFlowCnec("BE2-NL"), TwoSides.ONE)).thenReturn(-0.8);

        Mockito.when(sensisResults.getSensitivityOnFlow(glskFr, crac.getFlowCnec("DE-NL"), TwoSides.TWO)).thenReturn(0.);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskBe, crac.getFlowCnec("DE-NL"), TwoSides.TWO)).thenReturn(-0.5);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskDe, crac.getFlowCnec("DE-NL"), TwoSides.TWO)).thenReturn(0.4);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskNl, crac.getFlowCnec("DE-NL"), TwoSides.TWO)).thenReturn(-1.2);

        return sensisResults;
    }
}
