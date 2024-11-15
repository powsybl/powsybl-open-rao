/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.extensions.PtdfApproximation;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class RaoParametersCommons {
    private RaoParametersCommons() {
    }

    public static final String RAO_PARAMETERS_VERSION = "2.5";

    // header
    public static final String VERSION = "version";

    // objective function parameters
    public static final String OBJECTIVE_FUNCTION = "objective-function";
    public static final String OBJECTIVE_FUNCTION_SECTION = "rao-objective-function";
    public static final String TYPE = "type";
    public static final String UNIT = "unit";
    public static final String CURATIVE_MIN_OBJ_IMPROVEMENT = "curative-min-obj-improvement";
    public static final String PREVENTIVE_STOP_CRITERION = "preventive-stop-criterion";
    public static final String ENFORCE_CURATIVE_SECURITY = "enforce-curative-security";

    // range actions optimization parameters
    public static final String RANGE_ACTIONS_OPTIMIZATION = "range-actions-optimization";
    public static final String RANGE_ACTIONS_OPTIMIZATION_SECTION = "rao-range-actions-optimization";
    public static final String MAX_MIP_ITERATIONS = "max-mip-iterations";
    public static final String PST_PENALTY_COST = "pst-penalty-cost";
    public static final String PST_SENSITIVITY_THRESHOLD = "pst-sensitivity-threshold";
    public static final String PST_MODEL = "pst-model";
    public static final String HVDC_PENALTY_COST = "hvdc-penalty-cost";
    public static final String HVDC_SENSITIVITY_THRESHOLD = "hvdc-sensitivity-threshold";
    public static final String INJECTION_RA_PENALTY_COST = "injection-ra-penalty-cost";
    public static final String INJECTION_RA_SENSITIVITY_THRESHOLD = "injection-ra-sensitivity-threshold";
    public static final String LINEAR_OPTIMIZATION_SOLVER = "linear-optimization-solver";
    public static final String LINEAR_OPTIMIZATION_SOLVER_SECTION = "rao-linear-optimization-solver";
    public static final String SOLVER = "solver";
    public static final String RELATIVE_MIP_GAP = "relative-mip-gap";
    public static final String SOLVER_SPECIFIC_PARAMETERS = "solver-specific-parameters";
    public static final String RA_RANGE_SHRINKING = "ra-range-shrinking";

    // topological actions optimization parameters
    public static final String TOPOLOGICAL_ACTIONS_OPTIMIZATION = "topological-actions-optimization";
    public static final String TOPOLOGICAL_ACTIONS_OPTIMIZATION_SECTION = "rao-topological-actions-optimization";
    public static final String MAX_PREVENTIVE_SEARCH_TREE_DEPTH = "max-preventive-search-tree-depth";
    public static final String MAX_AUTO_SEARCH_TREE_DEPTH = "max-auto-search-tree-depth";
    public static final String MAX_CURATIVE_SEARCH_TREE_DEPTH = "max-curative-search-tree-depth";
    public static final String PREDEFINED_COMBINATIONS = "predefined-combinations";
    public static final String RELATIVE_MINIMUM_IMPACT_THRESHOLD = "relative-minimum-impact-threshold";
    public static final String ABSOLUTE_MINIMUM_IMPACT_THRESHOLD = "absolute-minimum-impact-threshold";
    public static final String SKIP_ACTIONS_FAR_FROM_MOST_LIMITING_ELEMENT = "skip-actions-far-from-most-limiting-element";
    public static final String MAX_NUMBER_OF_BOUNDARIES_FOR_SKIPPING_ACTIONS = "max-number-of-boundaries-for-skipping-actions";

    // Multi-threading parameters
    public static final String MULTI_THREADING = "multi-threading";
    public static final String MULTI_THREADING_SECTION = "rao-multi-threading";
    public static final String CONTINGENCY_SCENARIOS_IN_PARALLEL = "contingency-scenarios-in-parallel";
    public static final String PREVENTIVE_LEAVES_IN_PARALLEL = "preventive-leaves-in-parallel";
    public static final String AUTO_LEAVES_IN_PARALLEL = "auto-leaves-in-parallel";
    public static final String CURATIVE_LEAVES_IN_PARALLEL = "curative-leaves-in-parallel";

    // Second Preventive RAO parameters
    public static final String SECOND_PREVENTIVE_RAO = "second-preventive-rao";
    public static final String SECOND_PREVENTIVE_RAO_SECTION = "rao-second-preventive-rao";
    public static final String EXECUTION_CONDITION = "execution-condition";
    public static final String RE_OPTIMIZE_CURATIVE_RANGE_ACTIONS = "re-optimize-curative-range-actions";
    public static final String HINT_FROM_FIRST_PREVENTIVE_RAO = "hint-from-first-preventive-rao";

    // RA usage limits per contingency parameters
    public static final String RA_USAGE_LIMITS_PER_CONTINGENCY = "ra-usage-limits-per-contingency";
    public static final String RA_USAGE_LIMITS_PER_CONTINGENCY_SECTION = "rao-ra-usage-limits-per-contingency";

    public static final String MAX_CURATIVE_RA = "max-curative-ra";
    public static final String MAX_CURATIVE_TSO = "max-curative-tso";
    public static final String MAX_CURATIVE_TOPO_PER_TSO = "max-curative-topo-per-tso";
    public static final String MAX_CURATIVE_PST_PER_TSO = "max-curative-pst-per-tso";
    public static final String MAX_CURATIVE_RA_PER_TSO = "max-curative-ra-per-tso";

    // Not optimized cnecs parameters
    public static final String NOT_OPTIMIZED_CNECS = "not-optimized-cnecs";
    public static final String NOT_OPTIMIZED_CNECS_SECTION = "rao-not-optimized-cnecs";
    public static final String DO_NOT_OPTIMIZE_CURATIVE_CNECS = "do-not-optimize-curative-cnecs-for-tsos-without-cras";

    // Not optimized cnecs parameters
    public static final String LOAD_FLOW_AND_SENSITIVITY_COMPUTATION = "load-flow-and-sensitivity-computation";
    public static final String LOAD_FLOW_AND_SENSITIVITY_COMPUTATION_SECTION = "rao-load-flow-and-sensitivity-computation";

    public static final String LOAD_FLOW_PROVIDER = "load-flow-provider";
    public static final String SENSITIVITY_PROVIDER = "sensitivity-provider";
    public static final String SENSITIVITY_FAILURE_OVERCOST = "sensitivity-failure-overcost";
    public static final String SENSITIVITY_PARAMETERS = "sensitivity-parameters";

    // EXTENSIONS
    public static final String CONSTRAINT_ADJUSTMENT_COEFFICIENT = "constraint-adjustment-coefficient";
    public static final String VIOLATION_COST = "violation-cost";
    public static final String PTDF_APPROXIMATION = "ptdf-approximation";
    // -- LoopFlow parameters
    public static final String LOOP_FLOW_PARAMETERS = "loop-flow-parameters";
    public static final String LOOP_FLOW_PARAMETERS_SECTION = "rao-loop-flow-parameters";

    public static final String ACCEPTABLE_INCREASE = "acceptable-increase";
    public static final String COUNTRIES = "countries";

    // -- Mnec parameters
    public static final String MNEC_PARAMETERS = "mnec-parameters";
    public static final String MNEC_PARAMETERS_SECTION = "rao-mnec-parameters";

    public static final String ACCEPTABLE_MARGIN_DECREASE = "acceptable-margin-decrease";

    // -- Relative margins parameters
    public static final String RELATIVE_MARGINS = "relative-margins-parameters";
    public static final String RELATIVE_MARGINS_SECTION = "rao-relative-margins-parameters";
    public static final String PTDF_BOUNDARIES = "ptdf-boundaries";
    public static final String PTDF_SUM_LOWER_BOUND = "ptdf-sum-lower-bound";

    public static PtdfApproximation stringToPtdfApproximation(String string) {
        try {
            return PtdfApproximation.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new OpenRaoException(String.format("Unknown approximation value: %s", string));
        }
    }
}
