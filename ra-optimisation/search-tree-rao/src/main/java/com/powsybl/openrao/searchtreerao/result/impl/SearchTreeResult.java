/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class SearchTreeResult {
    private final PerimeterResultWithCnecs mainStateResultWithCnecs;
    private final MultiStateRemedialActionResultImpl allStatesRangeActionResult;

    public SearchTreeResult(PerimeterResultWithCnecs mainStateResultWithCnecs, MultiStateRemedialActionResultImpl allStatesRangeActionResult) {
        this.mainStateResultWithCnecs = mainStateResultWithCnecs;
        this.allStatesRangeActionResult = allStatesRangeActionResult;
    }

    public PerimeterResultWithCnecs getPerimeterResultWithCnecs() {
        return mainStateResultWithCnecs;
    }

    public MultiStateRemedialActionResultImpl getAllStatesRangeActionResult() {
        return allStatesRangeActionResult;
    }
}
