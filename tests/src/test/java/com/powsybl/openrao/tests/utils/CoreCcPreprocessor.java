/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.tests.utils;

import com.powsybl.iidm.network.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CoreCcPreprocessor {
    private CoreCcPreprocessor() {
        // must not be used
    }

    public static void applyCoreCcNetworkPreprocessing(Network network, boolean shouldCreateInjections) {
        network.getVoltageLevelStream().forEach(CoreCcPreprocessor::updateVoltageLevelNominalV);
        balanceNetworkForIdccStudy(network);

        if (shouldCreateInjections) {
        /* When importing an UCTE network file, powsybl ignores generators and loads that do not have an initial power flow.
        It can cause an error if a GLSK file associated to this network includes some factors on
        these nodes. The GLSK importers looks for a Generator (GSK) or Load (LSK) associated to this
        node. If the Generator/Load does not exist, the GLSK cannot be created.
        This script fix this problem, by creating for all missing generators a generator (P, Q = 0),
        and all missing loads a load (P, Q = 0). */
            createMissingGeneratorsAndLoads(network);
        }
    }

    private static void balanceNetworkForIdccStudy(Network network) {
        Map<Country, Double> totalGenerationPerCountry = new HashMap<>();
        Map<Country, Double> totalLoadPerCountry = new HashMap<>();

        network.getGeneratorStream().forEach(generator -> {
            Optional<Substation> substationOptional = generator.getTerminal().getVoltageLevel().getSubstation();
            if (substationOptional.isPresent()) {
                Country country = substationOptional.get().getNullableCountry();
                if (Objects.nonNull(country)) {
                    totalGenerationPerCountry.putIfAbsent(country, 0.);
                    totalGenerationPerCountry.put(country, totalGenerationPerCountry.get(country) + generator.getTargetP());
                }
            }
        });

        network.getLoadStream().forEach(load -> {
            Optional<Substation> substationOptional = load.getTerminal().getVoltageLevel().getSubstation();
            if (substationOptional.isPresent()) {
                Country country = substationOptional.get().getNullableCountry();
                if (Objects.nonNull(country)) {
                    totalLoadPerCountry.putIfAbsent(country, 0.);
                    totalLoadPerCountry.put(country, totalLoadPerCountry.get(country) + load.getP0());
                }
            }
        });

        network.getGeneratorStream().forEach(generator -> {
            Optional<Substation> substationOptional = generator.getTerminal().getVoltageLevel().getSubstation();
            if (substationOptional.isPresent()) {
                Country country = substationOptional.get().getNullableCountry();
                if (Objects.nonNull(country)) {
                    double countryImbalance = totalGenerationPerCountry.get(country) - totalLoadPerCountry.get(country);
                    double countryAbsoluteInjection = totalGenerationPerCountry.get(country) + totalLoadPerCountry.get(country);
                    generator.setTargetP(generator.getTargetP() - countryImbalance * generator.getTargetP() / countryAbsoluteInjection);
                }
            }
        });

        network.getLoadStream().forEach(load -> {
            Optional<Substation> substationOptional = load.getTerminal().getVoltageLevel().getSubstation();
            if (substationOptional.isPresent()) {
                Country country = substationOptional.get().getNullableCountry();
                if (Objects.nonNull(country)) {
                    double countryImbalance = totalGenerationPerCountry.get(country) - totalLoadPerCountry.get(country);
                    double countryAbsoluteInjection = totalGenerationPerCountry.get(country) + totalLoadPerCountry.get(country);
                    load.setP0(load.getP0() + countryImbalance * load.getP0() / countryAbsoluteInjection);
                }
            }
        });
    }

    private static boolean safeDoubleEquals(double a, double b) {
        return Math.abs(a - b) < 1e-3;
    }

    private static void updateVoltageLevelNominalV(VoltageLevel voltageLevel) {
        if (safeDoubleEquals(voltageLevel.getNominalV(), 380)) {
            voltageLevel.setNominalV(400);
        } else if (safeDoubleEquals(voltageLevel.getNominalV(), 220)) {
            voltageLevel.setNominalV(225);
        }
        // Else, Should not be changed cause is not equal to the default nominal voltage of voltage levels 6 or 7
    }

    private static void createMissingGeneratorsAndLoads(Network network) {
        network.getVoltageLevelStream().forEach(voltageLevel -> createMissingGeneratorsAndLoads(network, voltageLevel));
    }

    private static void createMissingGeneratorsAndLoads(Network network, VoltageLevel voltageLevel) {
        voltageLevel.getBusBreakerView().getBuses().forEach(bus -> createMissingGenerator(network, voltageLevel, bus.getId()));
        voltageLevel.getBusBreakerView().getBuses().forEach(bus -> createMissingLoad(network, voltageLevel, bus.getId()));
    }

    private static void createMissingGenerator(Network network, VoltageLevel voltageLevel, String busId) {
        String generatorId = busId + "_generator";
        if (network.getGenerator(generatorId) == null) {
            voltageLevel.newGenerator()
                .setBus(busId)
                .setEnsureIdUnicity(true)
                .setId(generatorId)
                .setMaxP(999999)
                .setMinP(0)
                .setTargetP(0)
                .setTargetQ(0)
                .setTargetV(voltageLevel.getNominalV())
                .setVoltageRegulatorOn(false)
                .add()
                .setFictitious(true);

        }
    }

    private static void createMissingLoad(Network network, VoltageLevel voltageLevel, String busId) {
        String loadId = busId + "_load";
        if (network.getLoad(loadId) == null) {
            voltageLevel.newLoad()
                .setBus(busId)
                .setEnsureIdUnicity(true)
                .setId(loadId)
                .setP0(0)
                .setQ0(0)
                .setLoadType(LoadType.FICTITIOUS)
                .add();
        }
    }
}
