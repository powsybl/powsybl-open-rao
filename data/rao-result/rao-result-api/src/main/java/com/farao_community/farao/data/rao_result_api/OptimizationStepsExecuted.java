/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.rao_result_api;

import java.util.Collections;
import java.util.Set;

/**
 * Enum representing the different optimizations the rao went through
 *
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */

public enum OptimizationStepsExecuted {
    SECOND_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION(true, true, false, Collections.emptySet()),
    FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION(false, true, false, Collections.emptySet()),
    SECOND_PREVENTIVE_IMPROVED_FIRST(true, false, false, Set.of(SECOND_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION)),
    SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION(true, false, true, Set.of(SECOND_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION)),
    FIRST_PREVENTIVE_ONLY(false, false, false, Set.of(FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION, SECOND_PREVENTIVE_IMPROVED_FIRST, SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION, SECOND_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));

    private final boolean secondPreventive;
    private final boolean fellBackToInitialSituation;
    private final boolean fellBackToFirstPreventiveSituation;
    private final Set<OptimizationStepsExecuted> possibleOverwrite;

    OptimizationStepsExecuted(boolean secondPreventive, boolean fellBackToInitialSituation, boolean fellBackToFirstPreventiveSituation, Set<OptimizationStepsExecuted> possibleOverwrite) {
        this.secondPreventive = secondPreventive;
        this.fellBackToInitialSituation = fellBackToInitialSituation;
        this.fellBackToFirstPreventiveSituation = fellBackToFirstPreventiveSituation;
        this.possibleOverwrite = possibleOverwrite;
    }

    public boolean hasRunSecondPreventive() {
        return secondPreventive;
    }

    public boolean hasFallenBackToInitialSituation() {
        return fellBackToInitialSituation;
    }

    public boolean hasFallenBackToFirstPreventiveSituation() {
        return fellBackToFirstPreventiveSituation;
    }

    public Set<OptimizationStepsExecuted> getPossibleOverwrite() {
        return possibleOverwrite;
    }

    public boolean isOverwritePossible(OptimizationStepsExecuted optimizationStepsExecuted) {
        if (!getPossibleOverwrite().isEmpty()) {
            return getPossibleOverwrite().contains(optimizationStepsExecuted);
        }
        return false;
    }
}
