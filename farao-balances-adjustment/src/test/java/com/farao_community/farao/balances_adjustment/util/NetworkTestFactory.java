/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.balances_adjustment.util;
import com.powsybl.iidm.network.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public final class NetworkTestFactory {

    private NetworkTestFactory() {
        throw new AssertionError("No default constructor in utility class");
    }

    public static Network createNetwork() {
        Network network = NetworkFactory.findDefault().createNetwork("test", "test");
        Substation substationFr1 = network.newSubstation()
                .setId("subFr1")
                .setCountry(Country.FR)
                .setTso("RTE")
                .add();
        VoltageLevel voltageLevelFr1A = substationFr1.newVoltageLevel()
                .setId("vlFr1A")
                .setName("vlFr1A")
                .setNominalV(440.0)
                .setHighVoltageLimit(400.0)
                .setLowVoltageLimit(200.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        voltageLevelFr1A.getBusBreakerView().newBus()
                .setId("busFr1A")
                .setName("busFr1A")
                .add();
        VoltageLevel voltageLevelFr1B = substationFr1.newVoltageLevel()
                .setId("vlFr1B").setName("vlFr1B")
                .setNominalV(200.0)
                .setHighVoltageLimit(400.0)
                .setLowVoltageLimit(200.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        voltageLevelFr1B.getBusBreakerView().newBus()
                .setId("busFr1B")
                .setName("busFr1B")
                .add();

        Substation substationFr2 = network.newSubstation()
                .setId("subFr2")
                .setCountry(Country.FR)
                .setTso("RTE")
                .add();
        VoltageLevel voltageLevelFr2A = substationFr2.newVoltageLevel()
                .setId("vlFr2A")
                .setName("vlFr2A")
                .setNominalV(440.0)
                .setHighVoltageLimit(400.0)
                .setLowVoltageLimit(200.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        voltageLevelFr2A.getBusBreakerView().newBus()
                .setId("busFr2A")
                .setName("busFr2A")
                .add();

        ThreeWindingsTransformer transformerFr1A = substationFr1.newThreeWindingsTransformer()
                .setId("twtFr1A")
                .setName("twtName")
                .newLeg1()
                .setR(1.3)
                .setX(1.4)
                .setG(1.6)
                .setB(1.7)
                .setRatedU(1.1)
                .setVoltageLevel("vlFr1A")
                .setConnectableBus("busFr1A")
                .setBus("busFr1A")
                .add()
                .newLeg2()
                .setR(2.03)
                .setX(2.04)
                .setRatedU(2.05)
                .setVoltageLevel("vlFr1B")
                .setConnectableBus("busFr1B")
                .setBus("busFr1B")
                .add()
                .newLeg3()
                .setR(3.3)
                .setX(3.4)
                .setRatedU(3.5)
                .setVoltageLevel("vlFr1B")
                .setConnectableBus("busFr1B")
                .setBus("busFr1B")
                .add()
                .add();
        transformerFr1A.getLeg1().getTerminal().setP(20).setQ(0);
        transformerFr1A.getLeg2().getTerminal().setP(-12).setQ(0);
        transformerFr1A.getLeg3().getTerminal().setP(-8).setQ(0);

        ThreeWindingsTransformer transformerFr1B = substationFr1.newThreeWindingsTransformer()
                .setId("twtFr1B")
                .newLeg1()
                .setR(1.3)
                .setX(1.4)
                .setG(1.6)
                .setB(1.7)
                .setRatedU(1.1)
                .setVoltageLevel("vlFr1A")
                .setConnectableBus("busFr1A")
                .setBus("busFr1A")
                .add()
                .newLeg2()
                .setR(2.03)
                .setX(2.04)
                .setRatedU(2.05)
                .setVoltageLevel("vlFr1A")
                .setConnectableBus("busFr1A")
                .setBus("busFr1A")
                .add()
                .newLeg3()
                .setR(3.3)
                .setX(3.4)
                .setRatedU(3.5)
                .setVoltageLevel("vlFr1A")
                .setConnectableBus("busFr1A")
                .setBus("busFr1A")
                .add()
                .add();
        transformerFr1B.getLeg1().getTerminal().setP(20).setQ(0);
        transformerFr1B.getLeg2().getTerminal().setP(-20).setQ(0);
        transformerFr1B.getLeg3().getTerminal().setP(0).setQ(0);

        Substation substationEs1 = network.newSubstation()
                .setId("subEs1")
                .setCountry(Country.ES)
                .add();
        VoltageLevel voltageLevelEs1A = substationEs1.newVoltageLevel()
                .setId("vlEs1A")
                .setName("vlEs1A")
                .setNominalV(440.0)
                .setHighVoltageLimit(400.0)
                .setLowVoltageLimit(200.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        voltageLevelEs1A.getBusBreakerView().newBus()
                .setId("busEs1A")
                .setName("busEs1A")
                .add();
        VoltageLevel voltageLevelEs1B = substationEs1.newVoltageLevel()
                .setId("vlEs1B")
                .setName("vlEs1B")
                .setNominalV(440.0)
                .setHighVoltageLimit(400.0)
                .setLowVoltageLimit(200.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        voltageLevelEs1B.getBusBreakerView().newBus()
                .setId("busEs1B")
                .setName("busEs1B")
                .add();

        ThreeWindingsTransformer transformerEs1A = substationEs1.newThreeWindingsTransformer()
                .setId("twtEs1A")
                .newLeg1()
                .setR(1.3)
                .setX(1.4)
                .setG(1.6)
                .setB(1.7)
                .setRatedU(1.1)
                .setVoltageLevel("vlEs1A")
                .setConnectableBus("busEs1A")
                .setBus("busEs1A")
                .add()
                .newLeg2()
                .setR(2.03)
                .setX(2.04)
                .setRatedU(2.05)
                .setVoltageLevel("vlEs1A")
                .setConnectableBus("busEs1A")
                .setBus("busEs1A")
                .add()
                .newLeg3()
                .setR(3.3)
                .setX(3.4)
                .setRatedU(3.5)
                .setVoltageLevel("vlEs1B")
                .setConnectableBus("busEs1B")
                .setBus("busEs1B")
                .add()
                .add();
        transformerEs1A.getLeg1().getTerminal().setP(20).setQ(0);
        transformerEs1A.getLeg2().getTerminal().setP(-15).setQ(0);
        transformerEs1A.getLeg3().getTerminal().setP(-5).setQ(0);

        VscConverterStation csFr1A = voltageLevelFr1A.newVscConverterStation()
                .setId("CFr1A")
                .setName("Converter1")
                .setConnectableBus("busFr1A")
                .setBus("busFr1A")
                .setLossFactor(0.011f)
                .setVoltageSetpoint(405.0)
                .setVoltageRegulatorOn(true)
                .add();
        csFr1A.getTerminal()
                .setP(100.0)
                .setQ(50.0);
        csFr1A.newReactiveCapabilityCurve()
                .beginPoint()
                .setP(5.0)
                .setMinQ(0.0)
                .setMaxQ(10.0)
                .endPoint()
                .beginPoint()
                .setP(10.0)
                .setMinQ(0.0)
                .setMaxQ(10.0)
                .endPoint()
                .add();

        VscConverterStation csEs1A = voltageLevelEs1A.newVscConverterStation()
                .setId("CEs1A")
                .setName("Converter2")
                .setConnectableBus("busEs1A")
                .setBus("busEs1A")
                .setLossFactor(0.011f)
                .setReactivePowerSetpoint(123)
                .setVoltageRegulatorOn(false)
                .add();
        csEs1A.newMinMaxReactiveLimits()
                .setMinQ(0.0)
                .setMaxQ(10.0)
                .add();
        csEs1A.getTerminal()
                .setP(-100.0)
                .setQ(50.0);

        HvdcLine hvdcLine = network.newHvdcLine()
                .setId("hvdcLineFrEs")
                .setName("hvdcLine")
                .setR(5.0)
                .setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER)
                .setNominalV(440.0)
                .setMaxP(-50.0)
                .setActivePowerSetpoint(20.0)
                .setConverterStationId1("CFr1A")
                .setConverterStationId2("CEs1A")
                .add();

        return network;

    }
}
