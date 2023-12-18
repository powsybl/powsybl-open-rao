/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.virtual_hubs;

import java.util.Objects;

/**
 * Market area description POJO
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
public class MarketArea {
    private final String code;
    private final String eic;
    private final boolean isMcParticipant;

    public MarketArea(String code, String eic, boolean isMcParticipant) {
        this.code = Objects.requireNonNull(code, "MarketArea creation does not allow null code");
        this.eic = Objects.requireNonNull(eic, "MarketArea creation does not allow null eic");
        this.isMcParticipant = isMcParticipant;
    }

    public String getCode() {
        return code;
    }

    public String getEic() {
        return eic;
    }

    public boolean isMcParticipant() {
        return isMcParticipant;
    }
}
