/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.commons;

import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ZonalDataImpl<I> implements ZonalData<I> {

    protected Map<String, I> dataPerZone;

    public ZonalDataImpl(Map<String, I> dataPerZone) {
        this.dataPerZone = dataPerZone;
    }

    public final Map<String, I> getDataPerZone() {
        return dataPerZone;
    }

    public final void addAll(ZonalData<I> otherData) {
        dataPerZone.putAll(otherData.getDataPerZone());
    }
}
