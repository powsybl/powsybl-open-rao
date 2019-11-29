/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation.json;

import com.farao_community.farao.ra_optimisation.*;

import java.util.Arrays;
import java.util.List;

import static com.farao_community.farao.ra_optimisation.TopologicalActionElementResult.TopologicalState.CLOSE;
import static com.farao_community.farao.ra_optimisation.TopologicalActionElementResult.TopologicalState.OPEN;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class ResultExampleBuilder {
    private ResultExampleBuilder() {
        throw new AssertionError("No empty constructor for utility class");
    }

    public static RaoComputationResult buildExampleResult() {
        return new RaoComputationResult(
                RaoComputationResult.Status.SUCCESS,
                buildPreContingencyResult(),
                buildContingencyResults()
        );
    }

    private static MonitoredBranchResult buildMonitoredBranch(String id,
                                                       String name,
                                                       String branchId,
                                                       double maximumFlow,
                                                       double preOptimisationFlow,
                                                       double postOptimisationFlow) {
        return new MonitoredBranchResult(
                id,
                name,
                branchId,
                maximumFlow,
                preOptimisationFlow,
                postOptimisationFlow
        );
    }

    private static PstElementResult buildPstRemedialAction(String id,
                                                    double preOptimisationAngle,
                                                    int preOptimisationTapPosition,
                                                    double postOptimisationAngle,
                                                    int postOptimisationTapPosition) {
        return new PstElementResult(
                id,
                preOptimisationAngle,
                preOptimisationTapPosition,
                postOptimisationAngle,
                postOptimisationTapPosition
        );
    }

    private static RedispatchElementResult buildRedispatchRemedialAction(String id,
                                                                  double preOptimisationTargetP,
                                                                  double postOptimisationTargetP,
                                                                  double redispatchCost) {
        return new RedispatchElementResult(
                id,
                preOptimisationTargetP,
                postOptimisationTargetP,
                redispatchCost
        );
    }

    private static TopologicalActionElementResult buildTopologicalAction(String id, TopologicalActionElementResult.TopologicalState state) {
        return new TopologicalActionElementResult(id, state);
    }

    private static List<MonitoredBranchResult> buildContingency1MonitoredBranches() {
        return Arrays.asList(
                buildMonitoredBranch("MONITORED_BRANCH_1_CO_1", "Monitored branch 1 after CO 1", "BRANCH_1", 100.0, 115.0, 98.0),
                buildMonitoredBranch("MONITORED_BRANCH_2_CO_1", "Monitored branch 2 after CO 1", "BRANCH_2", 100.0, 90.0, 98.5)
        );
    }

    private static List<MonitoredBranchResult> buildContingency2MonitoredBranches() {
        return Arrays.asList(
                buildMonitoredBranch("MONITORED_BRANCH_1_CO_2", "Monitored branch 1 after CO 2", "BRANCH_1", 100.0, 112.0, 97.0),
                buildMonitoredBranch("MONITORED_BRANCH_2_CO_2", "Monitored branch 2 after CO 2", "BRANCH_2", 100.0, 91.0, 99.5)
        );
    }

    private static List<RemedialActionResult> buildContingencyRemedialActions() {
        return Arrays.asList(
                new RemedialActionResult(
                        "CRA",
                        "CRA",
                        true,
                        Arrays.asList(
                                buildPstRemedialAction("PST_1", 0.5, 1, -2.0, -3),
                                buildPstRemedialAction("PST_2", 1.0, 2, -1.0, -2)
                        )
                )
        );
    }

    private static List<MonitoredBranchResult> buildPreContingencyMonitoredBranches() {
        return Arrays.asList(
                buildMonitoredBranch("MONITORED_BRANCH_1", "Monitored branch 1", "BRANCH_1", 100.0, 105.0, 95.0),
                buildMonitoredBranch("MONITORED_BRANCH_2", "Monitored branch 2", "BRANCH_2", 100.0, 80.0, 92.5)
        );
    }

    private static List<RemedialActionResult> buildPreContingencyRemedialActions() {
        return Arrays.asList(
                new RemedialActionResult(
                        "PRA_1",
                        "PRA n°1",
                        true,
                        Arrays.asList(
                                buildPstRemedialAction("PST_1", 0.5, 1, -0.5, -1),
                                buildPstRemedialAction("PST_2", 1.0, 2, 0.0, 0)
                        )
                ),
                new RemedialActionResult(
                        "PRA_2",
                        "PRA n°2",
                        true,
                        Arrays.asList(
                                buildRedispatchRemedialAction("REDISPATCH_GEN_1", 150.0, 175.0, 120.0),
                                buildRedispatchRemedialAction("REDISPATCH_GEN_2", 150.0, 125.0, -10.0)
                        )
                ),
                new RemedialActionResult(
                        "TOPOL_1",
                        "Topol n°1",
                        true,
                        Arrays.asList(
                                buildTopologicalAction("Switch_1", OPEN),
                                buildTopologicalAction("Switch_2", CLOSE)
                        )
                )
        );
    }

    private static List<ContingencyResult> buildContingencyResults() {
        return Arrays.asList(
                new ContingencyResult(
                        "CONTINGENCY_1",
                        "Contingency 1",
                        buildContingency1MonitoredBranches(),
                        buildContingencyRemedialActions()
                ),
                new ContingencyResult(
                        "CONTINGENCY_2",
                        "Contingency 2",
                        buildContingency2MonitoredBranches(),
                        buildContingencyRemedialActions()
                )
        );
    }

    private static PreContingencyResult buildPreContingencyResult() {
        return new PreContingencyResult(
                buildPreContingencyMonitoredBranches(),
                buildPreContingencyRemedialActions()
        );
    }
}
