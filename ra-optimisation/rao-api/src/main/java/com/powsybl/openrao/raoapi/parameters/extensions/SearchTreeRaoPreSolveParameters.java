/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.reports.RaoApiReports;

import java.util.Objects;
import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.CURATIVE_SOLUTION_USAGE;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.MAX_CURATIVE_SEARCH_TREE_DEPTH;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.MAX_PREVENTIVE_SEARCH_TREE_DEPTH;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.PREVENTIVE_SOLUTION_USAGE;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.ST_PRE_SOLVE_PARAMETERS_SECTION;

/**
 * Extension : pre-solve parameters for RAO. Presence of this block activates the pre-solve
 * search-tree ahead of the main search-tree.
 *
 * @author Maxence Fleury {@literal <maxence.fleury at artelys.com>}
 */
public class SearchTreeRaoPreSolveParameters {
    private static final int DEFAULT_MAX_SEARCH_TREE_DEPTH = 0;
    private static final SolutionUsage DEFAULT_SOLUTION_USAGE = SolutionUsage.HINT;

    private int maxPreventiveSearchTreeDepth = DEFAULT_MAX_SEARCH_TREE_DEPTH;
    private int maxCurativeSearchTreeDepth = DEFAULT_MAX_SEARCH_TREE_DEPTH;
    private SolutionUsage preventiveSolutionUsage = DEFAULT_SOLUTION_USAGE;
    private SolutionUsage curativeSolutionUsage = DEFAULT_SOLUTION_USAGE;
    private final ReportNode reportNode;

    /**
     * How the AC search tree uses the network action combination found by the pre-solve
     */
    public enum SolutionUsage {
        HINT,
        FORCED
    }

    public SearchTreeRaoPreSolveParameters(final ReportNode reportNode) {
        this.reportNode = reportNode;
    }

    public int getMaxPreventiveSearchTreeDepth() {
        return maxPreventiveSearchTreeDepth;
    }

    public void setMaxPreventiveSearchTreeDepth(int maxPreventiveSearchTreeDepth) {
        this.maxPreventiveSearchTreeDepth = nonNegativeDepth(MAX_PREVENTIVE_SEARCH_TREE_DEPTH, maxPreventiveSearchTreeDepth);
    }

    public int getMaxCurativeSearchTreeDepth() {
        return maxCurativeSearchTreeDepth;
    }

    public void setMaxCurativeSearchTreeDepth(int maxCurativeSearchTreeDepth) {
        this.maxCurativeSearchTreeDepth = nonNegativeDepth(MAX_CURATIVE_SEARCH_TREE_DEPTH, maxCurativeSearchTreeDepth);
    }

    public SolutionUsage getPreventiveSolutionUsage() {
        return preventiveSolutionUsage;
    }

    public void setPreventiveSolutionUsage(SolutionUsage preventiveSolutionUsage) {
        this.preventiveSolutionUsage = preventiveSolutionUsage;
    }

    public SolutionUsage getCurativeSolutionUsage() {
        return curativeSolutionUsage;
    }

    public void setCurativeSolutionUsage(SolutionUsage curativeSolutionUsage) {
        this.curativeSolutionUsage = curativeSolutionUsage;
    }

    private int nonNegativeDepth(String parameterName, int depth) {
        if (depth < 0) {
            RaoApiReports.reportNegativeMaxSearchTreeDepthPreSolve(reportNode, parameterName, depth);
            return 0;
        }
        return depth;
    }

    public static Optional<SearchTreeRaoPreSolveParameters> load(final PlatformConfig platformConfig, final ReportNode reportNode) {
        Objects.requireNonNull(platformConfig);
        return platformConfig.getOptionalModuleConfig(ST_PRE_SOLVE_PARAMETERS_SECTION)
            .map(config -> {
                SearchTreeRaoPreSolveParameters parameters = new SearchTreeRaoPreSolveParameters(reportNode);
                parameters.setMaxPreventiveSearchTreeDepth(config.getIntProperty(MAX_PREVENTIVE_SEARCH_TREE_DEPTH, DEFAULT_MAX_SEARCH_TREE_DEPTH));
                parameters.setMaxCurativeSearchTreeDepth(config.getIntProperty(MAX_CURATIVE_SEARCH_TREE_DEPTH, DEFAULT_MAX_SEARCH_TREE_DEPTH));
                parameters.setPreventiveSolutionUsage(config.getEnumProperty(PREVENTIVE_SOLUTION_USAGE, SolutionUsage.class, DEFAULT_SOLUTION_USAGE));
                parameters.setCurativeSolutionUsage(config.getEnumProperty(CURATIVE_SOLUTION_USAGE, SolutionUsage.class, DEFAULT_SOLUTION_USAGE));
                return parameters;
            });
    }

    public static boolean isActivated(RaoParameters parameters) {
        return parameters.hasExtension(OpenRaoSearchTreeParameters.class)
            && parameters.getExtension(OpenRaoSearchTreeParameters.class).getPreSolveParameters().isPresent();
    }

    public static int getMaxPreventiveSearchTreeDepth(RaoParameters parameters) {
        if (parameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            Optional<SearchTreeRaoPreSolveParameters> preSolveParameters = parameters.getExtension(OpenRaoSearchTreeParameters.class).getPreSolveParameters();
            if (preSolveParameters.isPresent()) {
                return preSolveParameters.get().getMaxPreventiveSearchTreeDepth();
            }
        }
        return DEFAULT_MAX_SEARCH_TREE_DEPTH;
    }

    public static int getMaxCurativeSearchTreeDepth(RaoParameters parameters) {
        if (parameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            Optional<SearchTreeRaoPreSolveParameters> preSolveParameters = parameters.getExtension(OpenRaoSearchTreeParameters.class).getPreSolveParameters();
            if (preSolveParameters.isPresent()) {
                return preSolveParameters.get().getMaxCurativeSearchTreeDepth();
            }
        }
        return DEFAULT_MAX_SEARCH_TREE_DEPTH;
    }

    public static SolutionUsage getPreventiveSolutionUsage(RaoParameters parameters) {
        if (parameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            Optional<SearchTreeRaoPreSolveParameters> preSolveParameters = parameters.getExtension(OpenRaoSearchTreeParameters.class).getPreSolveParameters();
            if (preSolveParameters.isPresent()) {
                return preSolveParameters.get().getPreventiveSolutionUsage();
            }
        }
        return DEFAULT_SOLUTION_USAGE;
    }

    public static SolutionUsage getCurativeSolutionUsage(RaoParameters parameters) {
        if (parameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            Optional<SearchTreeRaoPreSolveParameters> preSolveParameters = parameters.getExtension(OpenRaoSearchTreeParameters.class).getPreSolveParameters();
            if (preSolveParameters.isPresent()) {
                return preSolveParameters.get().getCurativeSolutionUsage();
            }
        }
        return DEFAULT_SOLUTION_USAGE;
    }
}
