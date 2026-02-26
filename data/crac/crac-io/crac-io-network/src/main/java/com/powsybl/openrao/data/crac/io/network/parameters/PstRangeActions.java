/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.range.RangeType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Configures how PST range actions are created.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class PstRangeActions extends AbstractCountriesFilter {
    private Map<String, TapRange> availableTapRangesAtInstants = new HashMap<>();
    private BiPredicate<TwoWindingsTransformer, State> pstRaPredicate = (pst, state) -> true;

    public record TapRange(int min, int max, RangeType rangeType) {
    }

    PstRangeActions() {
    }

    /**
     * For every instant, set the relative tap range available for PSTs.
     * Not listing an instant will result in PSTs not being available for optimization at that instant.
     * You can use {@code null} instead of min/max; the value will default to the physical one.
     */
    public void setAvailableTapRangesAtInstants(Map<String, TapRange> availableRelativeRangesAtInstants) {
        this.availableTapRangesAtInstants = availableRelativeRangesAtInstants;
    }

    public Optional<TapRange> getTapRange(Instant instant) {
        return Optional.ofNullable(availableTapRangesAtInstants.get(instant.getId()));
    }

    /**
     * Set the function that says if the PST is available for optimization at a given {@code State}.
     * Defaults to true.
     */
    public void setPstRaPredicate(BiPredicate<TwoWindingsTransformer, State> pstRaPredicate) {
        this.pstRaPredicate = pstRaPredicate;
    }

    public boolean isAvailable(TwoWindingsTransformer pst, State state) {
        return pstRaPredicate.test(pst, state);
    }
}
