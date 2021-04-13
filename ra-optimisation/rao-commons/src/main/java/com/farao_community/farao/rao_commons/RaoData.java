/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerInput;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A RaoData is an object that gathers Network, Crac and SystematicSensitivityResult data. It manages
 * variants of these objects to ensure data consistency at any moment. Network will remain the same at any moment
 * with no variant management. It is a single point of entry to manipulate all data related to linear rao with
 * variant management.
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RaoData {
    private final Network network;
    private final Crac crac;
    private final State optimizedState;
    private final Set<State> perimeter;
    private final ReferenceProgram referenceProgram;
    private final ZonalData<LinearGlsk> glsk;
    private final CracResultManager cracResultManager;
    private final RaoParameters raoParameters;

    private Set<BranchCnec> perimeterCnecs;
    private Set<BranchCnec> loopflowCnecs;

    private CracVariantManager cracVariantManager;

    /**
     * This constructor creates a new data variant with a pre-optimisation prefix and set it as the working variant.
     * So accessing data after this constructor will lead directly to the newly created variant data. CRAC and
     * sensitivity data will be empty. It will create a CRAC ResultVariantManager if it does not exist yet.
     *
     * @param network:           Network object.
     * @param crac:              CRAC object.
     * @param optimizedState:    State in which the remedial actions are optimized
     * @param perimeter:         set of State for which the Cnecs are monitored
     * @param referenceProgram:  ReferenceProgram object (needed only for loopflows and relative margin)
     * @param glsk:              GLSK provider (needed only for loopflows)
     * @param baseCracVariantId: Existing variant of the CRAC on which RaoData will be based
     * @param raoParameters:     Configuration of the RAO
     */
    public RaoData(Network network, Crac crac, State optimizedState, Set<State> perimeter, ReferenceProgram referenceProgram, ZonalData<LinearGlsk> glsk, String baseCracVariantId, RaoParameters raoParameters) {
        Objects.requireNonNull(network, "Unable to build RAO data without network.");
        Objects.requireNonNull(crac, "Unable to build RAO data without CRAC.");
        Objects.requireNonNull(optimizedState, "Unable to build RAO data without optimized state.");
        Objects.requireNonNull(crac, "Unable to build RAO data without perimeter.");
        this.network = network;
        this.crac = crac;
        this.optimizedState = optimizedState;
        this.perimeter = perimeter;
        this.referenceProgram = referenceProgram;
        this.glsk = glsk;
        this.raoParameters = raoParameters;
        cracResultManager = new CracResultManager(this);
        addRaoDataVariantManager(baseCracVariantId);
        cracResultManager.fillRangeActionResultsWithNetworkValues();

        computePerimeterCnecs();
        computeLoopflowCnecs();
    }

    private void addRaoDataVariantManager(String cracVariantId) {
        if (cracVariantId != null) {
            cracVariantManager = new CracVariantManager(crac, cracVariantId);
        } else {
            cracVariantManager = new CracVariantManager(crac);
        }
    }

    public static RaoData create(Network network, RaoData raoData) {
        return new RaoData(
                network,
                raoData.getCrac(),
                raoData.getOptimizedState(),
                raoData.getPerimeter(),
                raoData.getReferenceProgram(),
                raoData.getGlskProvider(),
                null,
                raoData.getRaoParameters());
    }

    public Network getNetwork() {
        return network;
    }

    public Crac getCrac() {
        return crac;
    }

    public ReferenceProgram getReferenceProgram() {
        return referenceProgram;
    }

    public ZonalData<LinearGlsk> getGlskProvider() {
        return glsk;
    }

    public RaoParameters getRaoParameters() {
        return raoParameters;
    }

    public Set<BranchCnec> getCnecs() {
        return perimeterCnecs;
    }

    private void computePerimeterCnecs() {
        if (perimeter != null) {
            Set<BranchCnec> cnecs = new HashSet<>();
            perimeter.forEach(state -> cnecs.addAll(crac.getBranchCnecs(state)));
            perimeterCnecs = cnecs;
        } else {
            perimeterCnecs = crac.getBranchCnecs();
        }
    }

    public Set<BranchCnec> getLoopflowCnecs() {
        return loopflowCnecs;
    }

    private void computeLoopflowCnecs() {
        if (!raoParameters.getLoopflowCountries().isEmpty()) {
            loopflowCnecs = perimeterCnecs.stream()
                .filter(cnec -> !Objects.isNull(cnec.getExtension(CnecLoopFlowExtension.class)) && cnecIsInCountryList(cnec, network, raoParameters.getLoopflowCountries()))
                .collect(Collectors.toSet());
        } else {
            loopflowCnecs = perimeterCnecs.stream()
                .filter(cnec -> !Objects.isNull(cnec.getExtension(CnecLoopFlowExtension.class)))
                .collect(Collectors.toSet());
        }
    }

    private static boolean cnecIsInCountryList(Cnec<?> cnec, Network network, Set<Country> loopflowCountries) {
        return cnec.getLocation(network).stream().anyMatch(country -> country.isPresent() && loopflowCountries.contains(country.get()));
    }

    public Set<RangeAction> getAvailableRangeActions() {
        return crac.getRangeActions(network, optimizedState, UsageMethod.AVAILABLE);
    }

    public Set<NetworkAction> getAvailableNetworkActions() {
        return crac.getNetworkActions(network, optimizedState, UsageMethod.AVAILABLE);
    }

    public State getOptimizedState() {
        return optimizedState;
    }

    public Set<State> getPerimeter() {
        return perimeter;
    }

    public CracResultManager getCracResultManager() {
        return cracResultManager;
    }

    public CracVariantManager getCracVariantManager() {
        return cracVariantManager;
    }

    // Delegate methods of RaoDataVariantManager
    public String getWorkingVariantId() {
        return getCracVariantManager().getWorkingVariantId();
    }

    public String getPreOptimVariantId() {
        return getCracVariantManager().getPreOptimVariantId();
    }

    public CracResult getCracResult(String variantId) {
        return getCracVariantManager().getCracResult(variantId);
    }

    public CracResult getCracResult() {
        return getCracVariantManager().getCracResult();
    }

    public SystematicSensitivityResult getSystematicSensitivityResult() {
        return getCracVariantManager().getSystematicSensitivityResult();
    }

    public void setSystematicSensitivityResult(SystematicSensitivityResult systematicSensitivityResult) {
        getCracVariantManager().setSystematicSensitivityResult(systematicSensitivityResult);
    }

    public boolean hasSensitivityValues() {
        return getSystematicSensitivityResult() != null;
    }

    public double getReferenceFlow(Cnec<?> cnec) {
        return getSystematicSensitivityResult().getReferenceFlow(cnec);
    }

    public double getSensitivity(Cnec<?> cnec, RangeAction rangeAction) {
        return getSystematicSensitivityResult().getSensitivityOnFlow(rangeAction, cnec);
    }

    public Map<RangeAction, Double> getPrePerimeterSetPoints() {
        Map<RangeAction, Double> prePerimeterSetPoints = new HashMap<>();
        String prePerimeterId = getCrac().getExtension(ResultVariantManager.class).getPrePerimeterVariantId();
        for (RangeAction rangeAction : getAvailableRangeActions()) {
            prePerimeterSetPoints.put(rangeAction, rangeAction.getExtension(RangeActionResultExtension.class).getVariant(prePerimeterId).getSetPoint(getOptimizedState().getId()));
        }
        return prePerimeterSetPoints;
    }

    public CnecResults getInitialCnecResults() {
        CnecResults cnecResults = new CnecResults();
        Map<BranchCnec, Double> flowsInMW = new HashMap<>();
        Map<BranchCnec, Double> flowsInA = new HashMap<>();
        Map<BranchCnec, Double> loopflowsInMW = new HashMap<>();
        Map<BranchCnec, Double> loopflowThresholdInMW = new HashMap<>();
        Map<BranchCnec, Double> commercialFlowsInMW = new HashMap<>();
        Map<BranchCnec, Double> absolutePtdfSums = new HashMap<>();
        for (BranchCnec cnec : getCnecs()) {
            CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(getCrac().getExtension(ResultVariantManager.class).getInitialVariantId());
            flowsInMW.put(cnec, cnecResult.getFlowInMW());
            flowsInA.put(cnec, cnecResult.getFlowInA());
            loopflowsInMW.put(cnec, cnecResult.getLoopflowInMW());
            loopflowThresholdInMW.put(cnec, cnecResult.getLoopflowThresholdInMW());
            commercialFlowsInMW.put(cnec, cnecResult.getCommercialFlowInMW());
            absolutePtdfSums.put(cnec, cnecResult.getAbsolutePtdfSum());
        }
        return cnecResults;
    }

    public Map<BranchCnec, Double> getPrePerimeterMarginsInAbsoluteMW() {
        Map<BranchCnec, Double> prePerimeterCnecMarginsInAbsoluteMW = new HashMap<>();
        String prePerimeterId = getCrac().getExtension(ResultVariantManager.class).getPrePerimeterVariantId();
        for (BranchCnec cnec : getCnecs()) {
            prePerimeterCnecMarginsInAbsoluteMW.put(cnec, RaoUtil.computeCnecMargin(cnec, prePerimeterId, Unit.MEGAWATT, false));
        }
        return  prePerimeterCnecMarginsInAbsoluteMW;
    }

    public SensitivityAndLoopflowResults getSensitivityAndLoopflowResults() {
        Map<BranchCnec, Double> commercialFlows = new HashMap<>();
        for (BranchCnec cnec : getLoopflowCnecs()) {
            commercialFlows.put(cnec, cnec.getExtension(CnecResultExtension.class).getVariant(getWorkingVariantId()).getCommercialFlowInMW());
        }
        return new SensitivityAndLoopflowResults(getSystematicSensitivityResult(), commercialFlows);
    }

    public LinearOptimizerInput createObjectiveFunctionInput() {
        return LinearOptimizerInput.create()
                .withCnecs(getCnecs())
                .withInitialCnecResults(getInitialCnecResults())
                .withLoopflowCnecs(getLoopflowCnecs())
                .withNetwork(getNetwork())
                .withPrePerimeterCnecMarginsInAbsoluteMW(getPrePerimeterMarginsInAbsoluteMW())
                .withPreperimeterSetpoints(getPrePerimeterSetPoints())
                .withRangeActions(getAvailableRangeActions())
                .build();
    }
}
