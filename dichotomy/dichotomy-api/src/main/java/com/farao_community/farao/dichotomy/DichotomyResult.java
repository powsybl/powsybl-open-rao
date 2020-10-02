/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.dichotomy;

import com.powsybl.iidm.network.Country;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class DichotomyResult {

    private Map<Pair<Country, Country>, BorderDichotomyResult> orientedNtcValues;

    public DichotomyResult() {
        orientedNtcValues = new HashMap<>();
    }

    public Map<Pair<Country, Country>, BorderDichotomyResult> getOrientedNtcValues() {
        return orientedNtcValues;
    }

    public void addOrientedNtcValue(Pair<Country, Country> orientedBorder, double ntcValue) {
        orientedNtcValues.put(orientedBorder, new BorderDichotomyResult(ntcValue));
    }

    public void addOrientedNtcValue(Pair<Country, Country> orientedBorder, DichotomyLimitType limitType, double ntcValue) {
        orientedNtcValues.put(orientedBorder, new BorderDichotomyResult(limitType, ntcValue));
    }

    public void addOrientedNtcValue(Pair<Country, Country> orientedBorder, BorderDichotomyResult borderDichotomyResult) {
        orientedNtcValues.put(orientedBorder, borderDichotomyResult);
    }
}
