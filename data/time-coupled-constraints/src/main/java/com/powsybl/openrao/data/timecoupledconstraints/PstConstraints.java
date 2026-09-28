/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.timecoupledconstraints;

import com.powsybl.openrao.commons.OpenRaoException;

import java.util.Optional;

/**
 * @author Atena Amnache {@literal <atena.amnache at rte-france.com>}
 */
public final class PstConstraints {
    private final String pstId;
    private final Integer upwardTapGradient;
    private final Integer downwardTapGradient;

    private PstConstraints(String pstId, Integer upwardTapGradient, Integer downwardTapGradient) {
        this.pstId = pstId;
        this.upwardTapGradient = upwardTapGradient;
        this.downwardTapGradient = downwardTapGradient;
    }

    /**
     * Get the id of the pst on which the constraints apply.
     *
     * @return the pst's id
     */
    public String getPstId() {
        return pstId;
    }

    /**
     * Get the upward tap gradient of the pst.
     *
     * @return upward tap gradient of the pst
     */
    public Optional<Integer> getUpwardTapGradient() {
        return Optional.ofNullable(upwardTapGradient);
    }

    /**
     * Get the downward tap gradient of the pst.
     *
     * @return downward tap gradient of the pst
     */
    public Optional<Integer> getDownwardTapGradient() {
        return Optional.ofNullable(downwardTapGradient);
    }

    public static PstConstraintsBuilder create() {
        return new PstConstraintsBuilder();
    }

    public static final class PstConstraintsBuilder {
        private String pstId;
        private Integer upwardTapGradient;
        private Integer downwardTapGradient;

        public PstConstraintsBuilder withPstId(String pstId) {
            this.pstId = pstId;
            return this;
        }

        public PstConstraintsBuilder withUpwardTapGradient(Integer upwardTapGradient) {
            this.upwardTapGradient = upwardTapGradient;
            return this;
        }

        public PstConstraintsBuilder withDownwardTapGradient(Integer downwardTapGradient) {
            this.downwardTapGradient = downwardTapGradient;
            return this;
        }

        public PstConstraints build() {
            if (pstId == null) {
                throw new OpenRaoException("The id of the PST is mandatory.");
            }
            if (upwardTapGradient != null && upwardTapGradient < 0) {
                throw new OpenRaoException("The upward tap gradient of the PST must be positive.");
            }
            if (downwardTapGradient != null && downwardTapGradient > 0) {
                throw new OpenRaoException("The downward tap gradient of the PST must be negative.");
            }
            return new PstConstraints(pstId, upwardTapGradient, downwardTapGradient);
        }
    }
}
