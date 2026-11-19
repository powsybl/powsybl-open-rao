/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * The tool provider, parameters and initial sensitivity result used to run one search tree.
 *
 * @author Maxence Fleury {@literal <maxence.fleury at artelys.com>}
 */
final class SearchTreeRunContext {
    @NonNull private final ToolProvider toolProvider;
    @NonNull private final RaoParameters raoParameters;
    @Nullable private PrePerimeterResult initialOutput;

    SearchTreeRunContext(@NonNull ToolProvider toolProvider, @NonNull RaoParameters raoParameters) {
        this.toolProvider = Objects.requireNonNull(toolProvider);
        this.raoParameters = Objects.requireNonNull(raoParameters);
    }

    @NonNull ToolProvider getToolProvider() {
        return toolProvider;
    }

    @NonNull RaoParameters getRaoParameters() {
        return raoParameters;
    }

    void setInitialOutput(@NonNull PrePerimeterResult initialOutput) {
        this.initialOutput = initialOutput;
    }

    @NonNull PrePerimeterResult getInitialOutput() {
        return Objects.requireNonNull(initialOutput, "initialOutput has not been computed yet");
    }
}
