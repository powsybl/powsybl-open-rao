/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.util.cgmes;

import com.powsybl.iidm.network.*;

import java.util.Objects;

/**
 * @author Philippe Edwards{@literal <philippe.edwards at rte-france.com>}
 */
public class CgmesBranchHelper {

    private final String mrId;

    private Branch.Side tieLineSide = null;
    private boolean isTieLine = false;

    private Branch<?> branch = null;

    /**
     * Constructor.
     *
     * @param mrId,                 CGMES-id of the branch
     * @param network,              powsybl iidm network object
     *
     */
    public CgmesBranchHelper(String mrId, Network network) {
        this.mrId = mrId;
        interpret(network);
    }

    public boolean isTieLine() {
        return isTieLine;
    }

    public Branch.Side getTieLineSide() {
        return tieLineSide;
    }

    public Branch<?> getBranch() {
        return branch;
    }

    protected void interpret(Network network) {
        branch = network.getBranch(mrId);
        if (Objects.isNull(branch)) {
            for (Line line : network.getLines()) {
                if (line.isTieLine()) {
                    TieLine tieLine = (TieLine) line;
                    if (tieLine.getHalf1().getId().equals(mrId)) {
                        isTieLine = true;
                        tieLineSide = Branch.Side.ONE;
                        branch = line;
                        return;
                    } else if (tieLine.getHalf2().getId().equals(mrId)) {
                        isTieLine = true;
                        tieLineSide = Branch.Side.TWO;
                        branch = line;
                        return;
                    }
                }
            }
        }
    }
}
