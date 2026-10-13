/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.io.network.NetworkCracCreationContext;
import org.apache.commons.lang3.function.TriFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Configures how HVDC range actions are created.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class HvdcRangeActions extends AbstractCountriesFilter {
    private Map<String, Range> availableRangesAtInstants = new HashMap<>();
    private TriFunction<HvdcLine, State, NetworkCracCreationContext, Boolean> hvdcRaPredicate = (hvdc, state, c) -> false;
    private BiFunction<HvdcLine, Instant, RangeActionCosts> raCostsProvider = (hvdc, instant) -> new RangeActionCosts(0, 0, 0);

    public record Range(double min, double max, RangeType rangeType) {
    }

    HvdcRangeActions() {
    }

    /**
     * For every instant, set the absolute range available for HVDCs.
     * Not listing an instant will result in HVDCs not being available for optimization at that instant.
     * You can use {@code null} instead of min/max; the value will default to the physical one.
     */
    public void setAvailableRangesAtInstants(Map<String, Range> availableAbsoluteRangesAtInstants) {
        this.availableRangesAtInstants = availableAbsoluteRangesAtInstants;
    }

    public Optional<Range> getRange(Instant instant) {
        return Optional.ofNullable(availableRangesAtInstants.get(instant.getId()));
    }

    /**
     * Set the function that says if the HVDC is available for optimization at a given {@code State}.
     * Defaults to false.
     */
    public void setHvdcRaPredicate(TriFunction<HvdcLine, State, NetworkCracCreationContext, Boolean> hvdcRaPredicate) {
        this.hvdcRaPredicate = hvdcRaPredicate;
    }

    public boolean isAvailable(HvdcLine hvdc, State state, NetworkCracCreationContext context) {
        return hvdcRaPredicate.apply(hvdc, state, context);
    }

    /**
     * Set the function that provides the costs of moving a given HVDC at a given instant.
     * All costs default to 0.
     */
    public void setRaCostsProvider(BiFunction<HvdcLine, Instant, RangeActionCosts> raCostsProvider) {
        this.raCostsProvider = raCostsProvider;
    }

    public RangeActionCosts getRaCosts(HvdcLine hvdc, Instant instant) {
        return raCostsProvider.apply(hvdc, instant);
    }
}
