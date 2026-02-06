/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.parameters.AbstractAlignedRaCracCreationParameters;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class NetworkCracCreationParameters extends AbstractAlignedRaCracCreationParameters {
    public static class CriticalBranches {
        Optional<Set<Country>> countries = Optional.empty();
        Optional<Double> optimizedMinV = Optional.of(150.);
        Optional<Double> optimizedMaxV = Optional.empty();
        boolean monitorOtherBranches = true; // non-critical branches are declared as MNECs
        Map<InstantKind, Double> operationalLimitReduction = Map.of(InstantKind.PREVENTIVE, 0.95);

        public Optional<Set<Country>> countries() {
            return countries;
        }

        public Optional<Double> optimizedMinV() {
            return optimizedMinV;
        }

        public Optional<Double> optimizedMaxV() {
            return optimizedMaxV;
        }

        public boolean monitorOtherBranches() {
            return monitorOtherBranches;
        }

        public Map<Integer, Integer> outageInstantPerVoltageLevel() {
            // key is VL in kV
            // Value is duration under which there is no possibility of RA (fixes TATL to use)
            return Map.of(400, 30, 200, 20, 150, 20);
        }

        public double operationalLimitReduction(InstantKind instantKind) {
            return operationalLimitReduction.getOrDefault(instantKind, 1.0);
        }
    }

    @Override
    public String getName() {
        return "NetworkCracCreationParameters";
    }



    public CriticalBranches criticalBranches() {
        return new CriticalBranches();
    }
}
