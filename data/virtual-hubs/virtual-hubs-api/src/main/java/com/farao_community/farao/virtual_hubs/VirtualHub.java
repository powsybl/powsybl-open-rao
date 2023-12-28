/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.virtual_hubs;

import java.util.Objects;

/**
 * Virtual hub description POJO
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
public class VirtualHub {
    private final String code;
    private final String eic;
    private final boolean isMcParticipant;
    private final String nodeName;
    private final MarketArea relatedMa;

    public VirtualHub(String code, String eic, boolean isMcParticipant, String nodeName, MarketArea relatedMa) {
        this.code = Objects.requireNonNull(code, "VirtualHub creation does not allow null code");
        this.eic = Objects.requireNonNull(eic, "VirtualHub creation does not allow null eic");
        this.isMcParticipant = isMcParticipant;
        this.nodeName = Objects.requireNonNull(nodeName, "VirtualHub creation does not allow null nodeName");
        this.relatedMa = Objects.requireNonNull(relatedMa, "VirtualHub creation does not allow null relatedMa");
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

    public String getNodeName() {
        return nodeName;
    }

    public MarketArea getRelatedMa() {
        return relatedMa;
    }
}
