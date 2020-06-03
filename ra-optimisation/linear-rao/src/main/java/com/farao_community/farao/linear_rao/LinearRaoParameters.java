/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.commons.extensions.AbstractExtension;

import static java.lang.Math.max;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearRaoParameters extends AbstractExtension<RaoParameters> {
    static final boolean DEFAULT_SECURITY_ANALYSIS_WITHOUT_RAO = false;

    private boolean securityAnalysisWithoutRao = DEFAULT_SECURITY_ANALYSIS_WITHOUT_RAO;

    @Override
    public String getName() {
        return "LinearRaoParameters";
    }

    public LinearRaoParameters setSecurityAnalysisWithoutRao(boolean securityAnalysisWithoutRao) {
        this.securityAnalysisWithoutRao = securityAnalysisWithoutRao;
        return this;
    }

    public boolean isSecurityAnalysisWithoutRao() {
        return securityAnalysisWithoutRao;
    }
}
