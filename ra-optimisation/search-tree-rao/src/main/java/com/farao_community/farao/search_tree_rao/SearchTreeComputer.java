/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_commons.SensitivityComputer;
import com.farao_community.farao.rao_commons.ToolProvider;

import java.util.Objects;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class SearchTreeComputer {
    private ToolProvider toolProvider;
    private Set<BranchCnec> cnecs;
    private BranchResult fixedPtdfs;

    private SearchTreeComputer() {
        // Should not be used
    }

    public static SearchTreeComputerBuilder create() {
        return new SearchTreeComputerBuilder();
    }

    SensitivityComputer getSensitivityComputerWithComputedCommercialFlows(Set<RangeAction> rangeActions) {
        SensitivityComputer.SensitivityComputerBuilder builder = getBuilder(rangeActions);
        builder.withCommercialFlowsResults(toolProvider.getLoopFlowComputation(), toolProvider.getLoopFlowCnecs(cnecs));
        return builder.build();
    }

    SensitivityComputer getSensitivityComputerWithFixedCommercialFlows(BranchResult fixedCommercialFlows, Set<RangeAction> rangeActions) {
        SensitivityComputer.SensitivityComputerBuilder builder = getBuilder(rangeActions);
        builder.withCommercialFlowsResults(fixedCommercialFlows);
        return builder.build();
    }

    SensitivityComputer getSensitivityComputer(Set<RangeAction> rangeActions) {
        return getBuilder(rangeActions).build();
    }

    SensitivityComputer.SensitivityComputerBuilder getBuilder(Set<RangeAction> rangeActions) {
        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder =  SensitivityComputer.create()
                .withToolProvider(toolProvider)
                .withCnecs(cnecs)
                .withRangeActions(rangeActions);
        if (fixedPtdfs != null) {
            sensitivityComputerBuilder.withPtdfsResults(fixedPtdfs);
        }
        return sensitivityComputerBuilder;
    }

    public static final class SearchTreeComputerBuilder {
        private ToolProvider toolProvider;
        private Set<BranchCnec> cnecs;
        private BranchResult fixedPtdfs;

        public SearchTreeComputerBuilder withToolProvider(ToolProvider toolProvider) {
            this.toolProvider = toolProvider;
            return this;
        }

        public SearchTreeComputerBuilder withCnecs(Set<BranchCnec> cnecs) {
            this.cnecs = cnecs;
            return this;
        }

        public SearchTreeComputerBuilder withPtdfsResults(BranchResult fixedPtdfs) {
            this.fixedPtdfs = fixedPtdfs;
            return this;
        }

        public SearchTreeComputer build() {
            Objects.requireNonNull(toolProvider);
            Objects.requireNonNull(cnecs);
            SearchTreeComputer searchTreeComputer = new SearchTreeComputer();
            searchTreeComputer.toolProvider = toolProvider;
            searchTreeComputer.cnecs = cnecs;
            searchTreeComputer.fixedPtdfs = fixedPtdfs;
            return searchTreeComputer;
        }
    }
}
