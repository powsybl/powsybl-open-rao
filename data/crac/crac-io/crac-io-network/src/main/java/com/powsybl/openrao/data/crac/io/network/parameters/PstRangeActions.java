/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Configures how PST range actions are created.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class PstRangeActions extends AbstractCountriesFilter {
    private Map<String, MinAndMax<Integer>> availableRelativeRangesAtInstants;
    private BiPredicate<PhaseTapChanger, State> pstRaPredicate = (pst, state) -> true;

    /**
     * For every instant, set the relative tap range available for PSTs.
     * Not listing an instant will result in PSTs not being available for optimization at that instant.
     * You can use {@code null} instead of min/max; the value will default to the physical one.
     */
    public void setAvailableRelativeRangesAtInstants(Map<String, MinAndMax<Integer>> availableRelativeRangesAtInstants) {
        this.availableRelativeRangesAtInstants = availableRelativeRangesAtInstants;
    }

    public Optional<Integer> getRangeMin(Instant instant) {
        return availableRelativeRangesAtInstants.get(instant.getId()).getMin();
    }

    public Optional<Integer> getRangeMax(Instant instant) {
        return availableRelativeRangesAtInstants.get(instant.getId()).getMax();
    }

    /**
     * Set the function that says if the PST is available for optimization at a given {@code State}.
     * Defaults to true.
     */
    public void setPstRaPredicate(BiPredicate<PhaseTapChanger, State> pstRaPredicate) {
        this.pstRaPredicate = pstRaPredicate;
    }

    public boolean isAvailable(PhaseTapChanger pst, State state) {
        return availableRelativeRangesAtInstants.containsKey(state.getInstant().getId()) && pstRaPredicate.test(pst, state);
    }
}
