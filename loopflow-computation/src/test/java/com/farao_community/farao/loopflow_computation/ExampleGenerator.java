/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.glsk.import_.glsk_document_api.providers.Glsk;
import com.farao_community.farao.data.refprog.reference_program.ReferenceExchangeData;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.*;
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
        Crac crac = new SimpleCrac("test-crac");
        Instant instantN = crac.newInstant().setId("N").setSeconds(-1).add();

        crac.newCnec().setId("FR-BE1").setInstant(instantN)
            .newNetworkElement().setId("FR-BE1").add()
            .newThreshold().setMaxValue(200.).setUnit(MEGAWATT).setDirection(Direction.BOTH).setSide(Side.LEFT).add().add();
        crac.newCnec().setId("FR-DE").setInstant(instantN)
            .newNetworkElement().setId("FR-DE").add()
            .newThreshold().setMaxValue(200.).setUnit(MEGAWATT).setDirection(Direction.BOTH).setSide(Side.LEFT).add().add();
        crac.newCnec().setId("BE2-NL").setInstant(instantN)
            .newNetworkElement().setId("BE2-NL").add()
            .newThreshold().setMaxValue(200.).setUnit(MEGAWATT).setDirection(Direction.BOTH).setSide(Side.LEFT).add().add();
        crac.newCnec().setId("DE-NL").setInstant(instantN)
            .newNetworkElement().setId("DE-NL").add()
            .newThreshold().setMaxValue(200.).setUnit(MEGAWATT).setDirection(Direction.BOTH).setSide(Side.LEFT).add().add();
        crac.newCnec().setId("BE1-BE2").setInstant(instantN)
            .newNetworkElement().setId("BE1-BE2").add()
            .newThreshold().setMaxValue(200.).setUnit(MEGAWATT).setDirection(Direction.BOTH).setSide(Side.LEFT).add().add();

        return crac;
    }

    static Glsk glskProvider() {
        HashMap<String, Float> glskBe = new HashMap<>();
        glskBe.put("Generator BE 1", 0.5f);
        glskBe.put("Generator BE 2", 0.5f);

        Map<String, LinearGlsk> glsks = new HashMap<>();
        glsks.put("FR", new LinearGlsk("10YFR-RTE------C", "FR", Collections.singletonMap("Generator FR", 1.f)));
        glsks.put("BE", new LinearGlsk("10YBE----------2", "BE", glskBe));
        glsks.put("DE", new LinearGlsk("10YCB-GERMANY--8", "DE", Collections.singletonMap("Generator DE", 1.f)));
        glsks.put("NL", new LinearGlsk("10YNL----------L", "NL", Collections.singletonMap("Generator NL", 1.f)));
        return new Glsk() {
            @Override
            public Map<String, LinearGlsk> getAllGlsk(Network network) {
                return glsks;
            }

            @Override
            public LinearGlsk getGlsk(Network network, String area) {
                return glsks.get(area);
            }
        };
    }

    static ReferenceProgram referenceProgram() {
        List<ReferenceExchangeData> exchangeDataList = Arrays.asList(
            new ReferenceExchangeData(Country.FR, Country.BE, 50),
            new ReferenceExchangeData(Country.FR, Country.DE, 50),
            new ReferenceExchangeData(Country.BE, Country.NL, 50),
            new ReferenceExchangeData(Country.DE, Country.NL, 50));
        return new ReferenceProgram(exchangeDataList);
    }

    static SystematicSensitivityResult systematicSensitivityResult(Network network, Crac crac, Glsk glsk) {
        SystematicSensitivityResult sensisResults = Mockito.mock(SystematicSensitivityResult.class);

        // flow results
        Mockito.when(sensisResults.getReferenceFlow(crac.getCnec("FR-BE1"))).thenReturn(30.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getCnec("BE1-BE2"))).thenReturn(280.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getCnec("FR-DE"))).thenReturn(170.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getCnec("BE2-NL"))).thenReturn(30.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getCnec("DE-NL"))).thenReturn(170.);

        // sensi results
        LinearGlsk glskFr = glsk.getGlsk(network, "FR");
        LinearGlsk glskBe = glsk.getGlsk(network, "BE");
        LinearGlsk glskDe = glsk.getGlsk(network, "DE");
        LinearGlsk glskNl = glsk.getGlsk(network, "NL");

        Mockito.when(sensisResults.getSensitivityOnFlow(glskFr, crac.getCnec("FR-BE1"))).thenReturn(0.);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskBe, crac.getCnec("FR-BE1"))).thenReturn(-1.5);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskDe, crac.getCnec("FR-BE1"))).thenReturn(-0.4);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskNl, crac.getCnec("FR-BE1"))).thenReturn(-0.8);

        Mockito.when(sensisResults.getSensitivityOnFlow(glskFr, crac.getCnec("BE1-BE2"))).thenReturn(0.);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskBe, crac.getCnec("BE1-BE2"))).thenReturn(0.);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskDe, crac.getCnec("BE1-BE2"))).thenReturn(-0.4);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskNl, crac.getCnec("BE1-BE2"))).thenReturn(-0.8);

        Mockito.when(sensisResults.getSensitivityOnFlow(glskFr, crac.getCnec("FR-DE"))).thenReturn(0.);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskBe, crac.getCnec("FR-DE"))).thenReturn(-0.5);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskDe, crac.getCnec("FR-DE"))).thenReturn(-1.6);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskNl, crac.getCnec("FR-DE"))).thenReturn(-1.2);

        Mockito.when(sensisResults.getSensitivityOnFlow(glskFr, crac.getCnec("BE2-NL"))).thenReturn(0.);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskBe, crac.getCnec("BE2-NL"))).thenReturn(0.5);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskDe, crac.getCnec("BE2-NL"))).thenReturn(-0.4);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskNl, crac.getCnec("BE2-NL"))).thenReturn(-0.8);

        Mockito.when(sensisResults.getSensitivityOnFlow(glskFr, crac.getCnec("DE-NL"))).thenReturn(0.);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskBe, crac.getCnec("DE-NL"))).thenReturn(-0.5);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskDe, crac.getCnec("DE-NL"))).thenReturn(0.4);
        Mockito.when(sensisResults.getSensitivityOnFlow(glskNl, crac.getCnec("DE-NL"))).thenReturn(-1.2);

        return sensisResults;
    }
}
