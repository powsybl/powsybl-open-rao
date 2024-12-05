/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api;

/**
 * Enum representing the different optimizations the rao went through
 *
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */

public final class OptimizationStepsExecuted {
    public static final String FIRST_PREVENTIVE_ONLY = "The RAO only went through first preventive";
    public static final String FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION = "First preventive fell back to initial situation";
    public static final String SECOND_PREVENTIVE_IMPROVED_FIRST = "Second preventive improved first preventive results";
    public static final String SECOND_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION = "Second preventive fell back to initial situation";
    public static final String SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION = "Second preventive fell back to first preventive results";

    private OptimizationStepsExecuted() { }
}
