/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.linearproblem;

import com.google.ortools.GScipParameters;
import com.google.ortools.Loader;
import com.google.ortools.mathopt.GurobiParametersProto;
import com.google.ortools.mathopt.HighsOptionsProto;
import com.google.ortools.mathopt.LinearConstraint;
import com.google.ortools.mathopt.MathOptInternalException;
import com.google.ortools.mathopt.Model;
import com.google.ortools.mathopt.Solve;
import com.google.ortools.mathopt.SolveParametersProto;
import com.google.ortools.mathopt.SolveResult;
import com.google.ortools.mathopt.SolverType;
import com.google.ortools.mathopt.Variable;
import com.google.ortools.mathopt.XpressParametersProto;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoimpl.result.api.LinearProblemStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Encapsulates OR-Tools' MathOpt objects in order to round up doubles
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-international.com>}
 */
public class OpenRaoMPSolver {
    static {
        try {
            Loader.loadNativeLibraries();
        } catch (UnsatisfiedLinkError e) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.error("Native library jniortools could not be loaded. You can ignore this message if it is not needed.");
        }
    }

    private static final int NUMBER_OF_BITS_TO_ROUND_OFF = 30;
    private static final double MIN_DOUBLE = 1e-6;

    private final SearchTreeRaoRangeActionsOptimizationParameters.Solver solver;
    private final String optProblemName;
    private Model model;
    private Double relativeMipGap;
    private String solverSpecificParameters;
    private GScipParameters gscipParameters;
    private GurobiParametersProto gurobiParameters;
    private XpressParametersProto xpressParameters;
    private HighsOptionsProto highsParameters;
    Map<String, OpenRaoMPConstraint> constraints = new TreeMap<>();
    Map<String, OpenRaoMPVariable> variables = new TreeMap<>();
    OpenRaoMPObjective objective;
    private boolean objectiveMinimization = true;
    /** Values of the variables after the last solve (empty if no feasible solution was found). */
    private Map<Variable, Double> lastSolution = Map.of();

    public OpenRaoMPSolver(String optProblemName, SearchTreeRaoRangeActionsOptimizationParameters.Solver solver) {
        this.solver = solver;
        this.optProblemName = optProblemName;
        // fail fast if the solver is not supported by OR-Tools MathOpt
        getOrToolsSolverType(this.solver);
        if (this.solver == SearchTreeRaoRangeActionsOptimizationParameters.Solver.GUROBI) {
            ensureGurobiLibraryLoaded();
        }
        resetModel();
    }

    public void resetModel() {
        this.model = new Model(optProblemName);
        constraints = new TreeMap<>();
        variables = new TreeMap<>();
        this.objective = new OpenRaoMPObjective(model.getObjective());
        lastSolution = Map.of();
        setSolverSpecificParametersAsString(solverSpecificParameters);
        if (objectiveMinimization) {
            setMinimization();
        } else {
            setMaximization();
        }
    }

    public SearchTreeRaoRangeActionsOptimizationParameters.Solver getSolver() {
        return solver;
    }

    static SolverType getOrToolsSolverType(SearchTreeRaoRangeActionsOptimizationParameters.Solver solver) {
        Objects.requireNonNull(solver);
        return switch (solver) {
            case SCIP -> SolverType.GSCIP;
            case XPRESS -> SolverType.XPRESS;
            case GUROBI -> SolverType.GUROBI;
            case HIGHS -> SolverType.HIGHS;
            case CBC -> throw new OpenRaoException("CBC is no longer supported by OR-Tools MathOpt. Please use SCIP, XPRESS, GUROBI or HIGHS in the RAO parameters.");
            default -> throw new OpenRaoException(String.format("unknown solver %s in RAO parameters", solver));
        };
    }

    private static volatile boolean gurobiLibraryLoaded = false;

    /**
     * Loads the Gurobi shared library into the OR-Tools native layer, if needed.
     *
     * OR-Tools does not link Gurobi: it is dlopen'ed at runtime, and only when
     * {@code com.google.ortools.init.CppBridge.loadGurobiSharedLibrary} is called.
     * Nothing in the OR-Tools Java flow calls it (see google/or-tools#5079), so
     * without this call the first GUROBI solve aborts the JVM with a C++
     * {@code std::bad_function_call}.
     *
     * Note: OR-Tools caches the load result for the whole process, so the first
     * call must already point to a valid library, later calls do not retry.
     */
    static void ensureGurobiLibraryLoaded() {
        if (gurobiLibraryLoaded) {
            return;
        }
        String path = resolveGurobiLibraryPath();
        if (path == null) {
            throw new OpenRaoException("Could not find the Gurobi shared library (libgurobi<version>.so). "
                + "Set the GUROBI_LIB_PATH environment variable to its full path, or GUROBI_HOME to the "
                + "Gurobi installation directory (the one containing lib/libgurobi<version>.so). "
                + "Note: OR-Tools 10.0-rc1 requires a Gurobi version exposing the GRBcopyparams C API (11.x and 12.x; removed in 13.x).");
        }
        if (!com.google.ortools.init.CppBridge.loadGurobiSharedLibrary(path)) {
            throw new OpenRaoException(String.format(
                "Found the Gurobi shared library at %s, but OR-Tools could not load it "
                    + "(incompatible Gurobi version? OR-Tools 10.0-rc1 needs GRBcopyparams: use 11.x or 12.x, not 13.x).", path));
        }
        gurobiLibraryLoaded = true;
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("Loaded Gurobi shared library from {}", path);
    }

    /** @return the first existing Gurobi library candidate path, or null. */
    static String resolveGurobiLibraryPath() {
        for (String candidate : gurobiLibraryCandidates()) {
            if (java.nio.file.Files.isRegularFile(java.nio.file.Path.of(candidate))) {
                return candidate;
            }
        }
        return null;
    }

    private static List<String> gurobiLibraryCandidates() {
        List<String> libDirectories = new ArrayList<>();
        String gurobiHome = System.getenv("GUROBI_HOME");
        if (gurobiHome != null && !gurobiHome.isBlank()) {
            libDirectories.add(gurobiHome + "/lib");
            libDirectories.add(gurobiHome + "/lib64");
        }
        for (String version : new String[] {"1203", "1202", "1201", "1200",
            "1103", "1102", "1101", "1100"}) {
            libDirectories.add("/opt/gurobi" + version + "/linux64/lib");
            libDirectories.add("/opt/gurobi" + version + "/linux64/lib64");
        }
        List<String> candidates = new ArrayList<>();
        String explicitPath = System.getenv("GUROBI_LIB_PATH");
        if (explicitPath != null && !explicitPath.isBlank()) {
            candidates.add(explicitPath);
        }
        String[] libraries = {"libgurobi120.so", "libgurobi110.so"};
        for (String libDirectory : libDirectories) {
            for (String library : libraries) {
                candidates.add(libDirectory + "/" + library);
            }
        }
        return candidates;
    }

    // Only for this class' tests
    Model getModel() {
        return model;
    }

    public boolean hasConstraint(String name) {
        return constraints.containsKey(name);
    }

    public OpenRaoMPConstraint getConstraint(String name) {
        if (hasConstraint(name)) {
            return constraints.get(name);
        } else {
            throw new OpenRaoException(String.format("Constraint %s has not been created yet", name));
        }
    }

    public boolean hasVariable(String name) {
        return variables.containsKey(name);
    }

    public OpenRaoMPVariable getVariable(String name) {
        if (hasVariable(name)) {
            return variables.get(name);
        } else {
            throw new OpenRaoException(String.format("Variable %s has not been created yet", name));
        }
    }

    public OpenRaoMPObjective getObjective() {
        return this.objective;
    }

    public OpenRaoMPVariable makeNumVar(double lb, double ub, String name) {
        return makeVar(lb, ub, false, name);
    }

    public OpenRaoMPVariable makeIntVar(double lb, double ub, String name) {
        return makeVar(lb, ub, true, name);
    }

    public OpenRaoMPVariable makeBoolVar(String name) {
        return makeVar(0, 1, true, name);
    }

    private OpenRaoMPVariable makeVar(double lb, double ub, boolean integer, String name) {
        if (hasVariable(name)) {
            throw new OpenRaoException(String.format("Variable %s already exists", name));
        }
        double roundedLb = roundDouble(lb);
        double roundedUb = roundDouble(ub);
        OpenRaoMPVariable variable = new OpenRaoMPVariable(this, model.addVariable(roundedLb, roundedUb, integer, name));
        variables.put(name, variable);
        return variable;
    }

    public OpenRaoMPConstraint makeConstraint(double lb, double ub, String name) {
        if (hasConstraint(name)) {
            throw new OpenRaoException(String.format("Constraint %s already exists", name));
        } else {
            double roundedLb = roundDouble(lb);
            double roundedUb = roundDouble(ub);
            LinearConstraint constraint = model.addLinearConstraint(roundedLb, roundedUb, name);
            OpenRaoMPConstraint openRaoConstraint = new OpenRaoMPConstraint(constraint);
            constraints.put(name, openRaoConstraint);
            return openRaoConstraint;
        }
    }

    public OpenRaoMPConstraint makeConstraint(String name) {
        return makeConstraint(-infinity(), infinity(), name);
    }

    /**
     * Parses a solver-specific parameter string of the form {@code "name1 value1, name2 value2"}
     * into the OR-Tools MathOpt parameters of the configured solver.
     *
     * @return true if the string is null or was parsed, false if it cannot be parsed
     */
    public boolean setSolverSpecificParametersAsString(String solverSpecificParameters) {
        this.solverSpecificParameters = solverSpecificParameters;
        gscipParameters = null;
        gurobiParameters = null;
        xpressParameters = null;
        highsParameters = null;
        if (solverSpecificParameters != null) {
            List<String[]> parsedParameters = parseParameters(solverSpecificParameters);
            if (parsedParameters == null) {
                return false;
            }
            switch (solver) {
                case SCIP -> {
                    GScipParameters.Builder builder = GScipParameters.newBuilder();
                    for (String[] parameter : parsedParameters) {
                        String name = parameter[0];
                        String value = parameter[1];
                        if ("TRUE".equalsIgnoreCase(value)) {
                            builder.putBoolParams(name, true);
                        } else if ("FALSE".equalsIgnoreCase(value)) {
                            builder.putBoolParams(name, false);
                        } else if (isLong(value)) {
                            builder.putIntParams(name, Integer.parseInt(value));
                        } else if (isDouble(value)) {
                            builder.putRealParams(name, Double.parseDouble(value));
                        } else {
                            builder.putCharParams(name, value);
                        }
                    }
                    gscipParameters = builder.build();
                }
                case GUROBI -> {
                    GurobiParametersProto.Builder builder = GurobiParametersProto.newBuilder();
                    for (String[] parameter : parsedParameters) {
                        builder.addParameters(GurobiParametersProto.Parameter.newBuilder()
                            .setName(parameter[0])
                            .setValue(parameter[1]));
                    }
                    gurobiParameters = builder.build();
                }
                case XPRESS -> {
                    XpressParametersProto.Builder builder = XpressParametersProto.newBuilder();
                    for (String[] parameter : parsedParameters) {
                        builder.addParameters(XpressParametersProto.Parameter.newBuilder()
                            .setName(parameter[0])
                            .setValue(parameter[1]));
                    }
                    xpressParameters = builder.build();
                }
                case HIGHS -> {
                    HighsOptionsProto.Builder builder = HighsOptionsProto.newBuilder();
                    for (String[] parameter : parsedParameters) {
                        String name = parameter[0];
                        String value = parameter[1];
                        if ("TRUE".equalsIgnoreCase(value)) {
                            builder.putBoolOptions(name, true);
                        } else if ("FALSE".equalsIgnoreCase(value)) {
                            builder.putBoolOptions(name, false);
                        } else if (isLong(value)) {
                            builder.putIntOptions(name, Integer.parseInt(value));
                        } else if (isDouble(value)) {
                            builder.putDoubleOptions(name, Double.parseDouble(value));
                        } else {
                            builder.putStringOptions(name, value);
                        }
                    }
                    highsParameters = builder.build();
                }
                default -> throw new OpenRaoException(String.format("unknown solver %s in RAO parameters", solver));
            }
        }
        return true;
    }

    /**
     * Splits {@code "name1 value1, name2 value2"} into {@code {name, value}} pairs.
     * A value may contain spaces (the split is done at the first space of each pair).
     *
     * @return the list of {name, value} pairs, or null if the string cannot be parsed
     */
    private static List<String[]> parseParameters(String solverSpecificParameters) {
        List<String[]> parameters = new ArrayList<>();
        for (String chunk : solverSpecificParameters.split(",")) {
            String trimmed = chunk.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int separator = trimmed.indexOf(' ');
            int separator2 = trimmed.indexOf('\t');
            int splitPosition = separator == -1 ? separator2 : (separator2 == -1 ? separator : Math.min(separator, separator2));
            if (splitPosition <= 0) {
                return null; // a parameter without a value
            }
            String name = trimmed.substring(0, splitPosition);
            String value = trimmed.substring(splitPosition + 1).trim();
            if (value.isEmpty()) {
                return null; // a parameter without a value
            }
            parameters.add(new String[] {name, value});
        }
        if (parameters.isEmpty()) {
            return null;
        }
        return parameters;
    }

    private static boolean isLong(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isDouble(String value) {
        try {
            double d = Double.parseDouble(value);
            return Double.isFinite(d);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void setRelativeMipGap(double relativeMipGap) {
        this.relativeMipGap = relativeMipGap;
    }

    public LinearProblemStatus solve() {
        SolveParametersProto.Builder solveParameters = SolveParametersProto.newBuilder();
        if (relativeMipGap != null) {
            if (solver == SearchTreeRaoRangeActionsOptimizationParameters.Solver.SCIP) {
                // GSCIP: solve to exact optimality. With a non-zero gap, the incumbent returned can differ
                // from the one returned by the legacy SCIP backend, which breaks results depending on ties
                // (e.g. min-on time scenarios). OpenRAO models are small enough for this to be free.
                solveParameters.setRelativeGapTolerance(0.0);
            } else {
                solveParameters.setRelativeGapTolerance(relativeMipGap);
            }
        }
        if (OpenRaoLoggerProvider.TECHNICAL_LOGS.isTraceEnabled()) {
            solveParameters.setEnableOutput(true);
        }
        if (gscipParameters != null) {
            solveParameters.setGscip(gscipParameters);
        }
        if (gurobiParameters != null) {
            solveParameters.setGurobi(gurobiParameters);
        }
        if (xpressParameters != null) {
            solveParameters.setXpress(xpressParameters);
        }
        if (highsParameters != null) {
            solveParameters.setHighs(highsParameters);
        }

        SolveResult result;
        try {
            result = Solve.solve(model, getOrToolsSolverType(solver), new Solve.Args().setSolveParams(solveParameters.build()));
        } catch (IllegalArgumentException | MathOptInternalException e) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.error("Error while solving linear problem {}: {}", optProblemName, e.getMessage());
            return LinearProblemStatus.ABNORMAL;
        }

        lastSolution = result.hasPrimalFeasibleSolution() ? result.getVariableValues() : Map.of();
        return convertResultStatus(result.getTermination().getReason(), result.getTermination().getProblemStatus());
    }

    static LinearProblemStatus convertResultStatus(SolveResult.TerminationReason reason, SolveResult.ProblemStatus problemStatus) {
        return switch (reason) {
            case OPTIMAL -> LinearProblemStatus.OPTIMAL;
            case FEASIBLE, IMPRECISE -> LinearProblemStatus.FEASIBLE;
            case INFEASIBLE -> LinearProblemStatus.INFEASIBLE;
            case UNBOUNDED -> LinearProblemStatus.UNBOUNDED;
            case INFEASIBLE_OR_UNBOUNDED -> problemStatus.getPrimalStatus() == SolveResult.FeasibilityStatus.INFEASIBLE
                ? LinearProblemStatus.INFEASIBLE
                : LinearProblemStatus.UNBOUNDED;
            case NO_SOLUTION_FOUND -> LinearProblemStatus.NOT_SOLVED;
            case NUMERICAL_ERROR, OTHER_ERROR -> LinearProblemStatus.ABNORMAL;
        };
    }

    public int numVariables() {
        return variables.size();
    }

    public int numConstraints() {
        return constraints.size();
    }

    public boolean isMinimization() {
        return objectiveMinimization;
    }

    public void setMinimization() {
        model.getObjective().setToMinimize();
        objectiveMinimization = true;
    }

    public boolean isMaximization() {
        return !objectiveMinimization;
    }

    public void setMaximization() {
        model.getObjective().setToMaximize();
        objectiveMinimization = false;
    }

    /* Method used to make sure the MIP is reproducible. This basically rounds the least significant bits of a double.
     Let's say a double has 10 precision bits (in reality, 52)
     We take an initial double:
       .............//////////.....
     To which we add a "bigger" double :
       .........\\\\\\\\.......
      =>
       .........\\\\||||||...........
     (we "lose" the least significant bits of the first double because the sum double doesn't have enough precision to show them)
     Then we subtract the same "bigger" double:
       .............//////...........
     We get back our original bits for the most significant part, but the least significant bits are still gone.
     */
    static double roundDouble(double value) {
        if (Double.isNaN(value)) {
            throw new OpenRaoException("Trying to add a NaN value in MIP!");
        }
        if (Math.abs(value) < MIN_DOUBLE) {
            return 0.;
        }
        double t = value * (1L << NUMBER_OF_BITS_TO_ROUND_OFF);
        if (t != Double.POSITIVE_INFINITY && value != Double.NEGATIVE_INFINITY && !Double.isNaN(t)) {
            return value - t + t;
        }
        return value;
    }

    /**
     * Double used as "infinity" for variables and constraints.
     * OR-Tools MathOpt handles infinite bounds natively, so a true double infinity can be used.
     */
    public double infinity() {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * Returns the value of a variable in the last solution (0 if the variable has no value,
     * e.g. before the first solve or when no feasible solution was found).
     */
    double solutionValueOf(Variable variable) {
        return lastSolution.getOrDefault(variable, 0.);
    }
}
