/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.commons.ucte;

import com.powsybl.iidm.network.Identifiable;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
final class UcteMatchingResult {

    private static final UcteMatchingResult NOT_FOUND = new UcteMatchingResult(MatchStatus.NOT_FOUND, null, false, null);
    private static final UcteMatchingResult SEVERAL_MATCH = new UcteMatchingResult(MatchStatus.SEVERAL_MATCH, null, false, null);

    private final MatchStatus status;
    private final UcteConnectable.Side side;
    private boolean isInverted;
    private final Identifiable<?> iidmIdentifiable;

    enum MatchStatus {
        SINGLE_MATCH(true),
        NOT_FOUND(false),
        SEVERAL_MATCH(true);

        private boolean hasMatched;

        MatchStatus(boolean hasMatched) {
            this.hasMatched = hasMatched;
        }
    }

    boolean hasMatched() {
        return status.hasMatched;
    }

    boolean isInverted() {
        return isInverted;
    }

    private UcteMatchingResult(MatchStatus status, UcteConnectable.Side side, boolean isInverted, Identifiable<?> iidmIdentifiable) {
        this.status = status;
        this.side = side;
        this.isInverted = isInverted;
        this.iidmIdentifiable = iidmIdentifiable;
    }

    static UcteMatchingResult notFound() {
        return NOT_FOUND;
    }

    static UcteMatchingResult severalPossibleMatch() {
        return SEVERAL_MATCH;
    }

    static UcteMatchingResult found(UcteConnectable.Side side, boolean isInverted, Identifiable<?> match) {
        return new UcteMatchingResult(MatchStatus.SINGLE_MATCH, side, isInverted, match);
    }

    UcteMatchingResult invert() {
        isInverted = !isInverted;
        return this;
    }

    Identifiable<?> getIidmIdentifiable() {
        return iidmIdentifiable;
    }

    UcteConnectable.Side getSide() {
        return side;
    }

    MatchStatus getStatus() {
        return status;
    }
}
