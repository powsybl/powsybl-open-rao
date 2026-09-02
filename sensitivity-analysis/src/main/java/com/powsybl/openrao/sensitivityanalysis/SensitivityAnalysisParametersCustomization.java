/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.sensitivity.SensitivityAnalysisParameters;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Thread-scoped customization of the parameters of the sensitivity analyses run by the calling thread.
 *
 * <p>{@link SystematicSensitivityAdapter} runs each analysis on its own copy of the RAO parameters, so that
 * concurrent analyses never share a mutable parameters instance. That copy is the only safe place to set a
 * parameter whose value must differ from one thread to another: the RAO parameters themselves are shared by all
 * threads and must not be written to.
 *
 * <p>The customization is intentionally an opaque {@link Consumer} so that this module stays independent of any
 * particular sensitivity analysis provider.
 *
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public final class SensitivityAnalysisParametersCustomization {

    private static final ThreadLocal<Consumer<SensitivityAnalysisParameters>> CUSTOMIZATION = new ThreadLocal<>();

    private SensitivityAnalysisParametersCustomization() {
    }

    /**
     * Installs the customization to apply to the parameters of every sensitivity analysis subsequently run by the
     * calling thread. Callers are responsible for calling {@link #remove()} once done, typically in a finally block,
     * so that a pooled thread does not keep it for unrelated work.
     */
    public static void set(Consumer<SensitivityAnalysisParameters> customization) {
        CUSTOMIZATION.set(Objects.requireNonNull(customization));
    }

    public static void remove() {
        CUSTOMIZATION.remove();
    }

    static void apply(SensitivityAnalysisParameters parameters) {
        Consumer<SensitivityAnalysisParameters> customization = CUSTOMIZATION.get();
        if (customization != null) {
            customization.accept(parameters);
        }
    }
}
