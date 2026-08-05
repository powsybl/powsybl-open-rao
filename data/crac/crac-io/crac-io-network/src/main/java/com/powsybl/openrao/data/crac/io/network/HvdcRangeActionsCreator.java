/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;

import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.io.network.parameters.HvdcRangeActions;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class HvdcRangeActionsCreator {
    private final Crac crac;
    private final Network network;
    private final HvdcRangeActions parameters;
    private final NetworkCracCreationContext creationContext;
    //TODO: RA groups for hvdcs?

    HvdcRangeActionsCreator(NetworkCracCreationContext creationContext, Network network, HvdcRangeActions parameters) {
        this.creationContext = creationContext;
        this.crac = creationContext.getCrac();
        this.network = network;
        this.parameters = parameters;
    }

    void addHvdcRangeActions() {
        Set<Instant> instants = crac.getSortedInstants().stream().filter(instant -> !instant.isOutage()).collect(Collectors.toSet());
        network.getHvdcLineStream()
            .filter(hvdc -> Utils.hvdcIsInCountries(hvdc, parameters.getCountries().orElse(null)))
            .forEach(hvdc -> instants.stream()
                .filter(instant -> crac.getStates(instant).stream().anyMatch(state -> parameters.isAvailable(hvdc, state, creationContext)))
                .forEach(instant -> addHvdcRangeActionForInstant(hvdc, instant)));
    }

    private void addHvdcRangeActionForInstant(HvdcLine hvdc, Instant instant) {
        HvdcRangeActionAdder hvdcAdder = crac.newHvdcRangeAction()
            .withId("HVDC_RA_" + hvdc.getId() + "_" + instant.getId())
            .withNetworkElement(hvdc.getId())
            .withInitialSetpoint(hvdc.getConvertersMode().equals(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER) ? hvdc.getActivePowerSetpoint() : -hvdc.getActivePowerSetpoint())
            .newRange().withRangeType(RangeType.ABSOLUTE).withMin(-hvdc.getMaxP()).withMax(hvdc.getMaxP()).add();

        boolean availableForAllStates = crac.getStates(instant).stream().allMatch(state -> parameters.isAvailable(hvdc, state, creationContext));
        if (availableForAllStates) {
            hvdcAdder.newOnInstantUsageRule().withInstant(instant.getId()).add();
        } else {
            crac.getStates().stream().filter(state -> parameters.isAvailable(hvdc, state, creationContext))
                .forEach(
                    state -> hvdcAdder.newOnContingencyStateUsageRule()
                        .withInstant(instant.getId())
                        .withContingency(state.getContingency().orElseThrow().getId())
                        .add());
        }
        parameters.getRange(instant).ifPresent(
            range -> hvdcAdder.newRange().withRangeType(range.rangeType())
                .withMin(range.min()).withMax(range.max())
                .add());
        hvdcAdder.withActivationCost(parameters.getRaCosts(hvdc, instant).activationCost())
                .withVariationCost(parameters.getRaCosts(hvdc, instant).upVariationCost(), VariationDirection.UP)
                .withVariationCost(parameters.getRaCosts(hvdc, instant).downVariationCost(), VariationDirection.DOWN);

        hvdcAdder.add();
    }

}
