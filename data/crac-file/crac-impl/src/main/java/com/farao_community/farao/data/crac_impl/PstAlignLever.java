/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import java.util.List;

/**
 * PST remedial action alignment
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public class PstAlignLever extends AbstractRangeLever {

    private List<PstLever> pstLevers;

    public PstAlignLever(List<PstLever> pstLevers) {
        this.pstLevers = pstLevers;
    }

    public List<PstLever> getPstLevers() {
        return pstLevers;
    }

    public void setPstLevers(List<PstLever> pstLevers) {
        this.pstLevers = pstLevers;
    }

    public void addPstLever(PstLever pstLever) {
        pstLevers.add(pstLever);
    }
}
