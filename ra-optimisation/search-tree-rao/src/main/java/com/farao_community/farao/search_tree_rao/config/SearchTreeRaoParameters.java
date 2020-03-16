/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.config;

import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.commons.extensions.AbstractExtension;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SearchTreeRaoParameters extends AbstractExtension<RaoParameters> {

    static final String DEFAULT_RANGE_ACTION_RAO = "LinearRao";
    static final boolean DEFAULT_DC_MODE = false;
    static final boolean DEFAULT_AC_TO_DC_FALLBACK = false;

    private String rangeActionRao = DEFAULT_RANGE_ACTION_RAO;
    private boolean dcMode = DEFAULT_DC_MODE;
    private boolean acToDcFallback = DEFAULT_AC_TO_DC_FALLBACK;

    @Override
    public String getName() {
        return "SearchTreeRaoParameters";
    }

    public String getRangeActionRao() {
        return rangeActionRao;
    }

    public void setRangeActionRao(String rangeActionRaoName) {
        this.rangeActionRao = rangeActionRaoName;
    }

    public boolean isDcMode() {
        return dcMode;
    }

    public void setDcMode(boolean dcMode) {
        this.dcMode = dcMode;
    }

    public boolean isAcToDcFallback() {
        return acToDcFallback;
    }

    public void setAcToDcFallback(boolean acToDcFallback) {
        this.acToDcFallback = acToDcFallback;
    }
}
