/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.impl;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.glsk.import_.glsk_document_api.providers.Glsk;
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.*;

/**
 * Test case is a 4 nodes network, with 4 countries.
 *
 *       FR   (+100 MW)       BE  (0 MW)
 *          + ------------ +
 *          |              |
 *          |              |
 *          |              |
 *          + ------------ +
 *       DE   (0 MW)          NL  (-100 MW)
 *
 * All lines have same impedance and are monitored.
 * One contingency is simulated, the loss of FR-BE interconnection line.
 * Each Country GLSK is a simple one node GLSK.
 * Compensation is considered as equally shared on each country, and there are no losses.
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
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
            .setId("Bus BE")
            .setName("Bus BE")
            .add();
        voltageLevelBe.newGenerator()
            .setId("Generator BE")
            .setName("Generator BE")
            .setBus("Bus BE")
            .setEnergySource(EnergySource.OTHER)
            .setMinP(1000)
            .setMaxP(2000)
            .setRatedS(100)
            .setTargetP(1500)
            .setTargetV(400)
            .setVoltageRegulatorOn(true)
            .add();
        voltageLevelBe.newLoad()
            .setId("Load BE")
            .setName("Load BE")
            .setBus("Bus BE")
            .setLoadType(LoadType.UNDEFINED)
            .setP0(1500)
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
            .setId("FR-BE")
            .setName("FR-BE")
            .setVoltageLevel1("Voltage level FR")
            .setVoltageLevel2("Voltage level BE")
            .setBus1("Bus FR")
            .setBus2("Bus BE")
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
            .setId("BE-NL")
            .setName("BE-NL")
            .setVoltageLevel1("Voltage level BE")
            .setVoltageLevel2("Voltage level NL")
            .setBus1("Bus BE")
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
        return network;
    }

    static Crac crac() {
        return CracImporters.importCrac("crac.json", ExampleGenerator.class.getResourceAsStream("/crac.json"));
    }

    static Glsk glskProvider() {
        Map<String, LinearGlsk> glsks = new HashMap<>();
        glsks.put("FR", new LinearGlsk("10YFR-RTE------C", "FR", Collections.singletonMap("Generator FR", 1.f)));
        glsks.put("BE", new LinearGlsk("10YBE----------2", "BE", Collections.singletonMap("Generator BE", 1.f)));
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

}
