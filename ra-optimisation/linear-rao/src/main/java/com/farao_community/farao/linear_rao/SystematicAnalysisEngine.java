package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Unit;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.CracResult;
import com.farao_community.farao.data.crac_result_extensions.CracResultExtension;
import com.farao_community.farao.flowbased_computation.impl.LoopFlowComputation;
import com.farao_community.farao.linear_rao.config.LinearRaoConfigurationUtil;
import com.farao_community.farao.linear_rao.config.LinearRaoParameters;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.util.SensitivityComputationException;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * A computation engine dedicated to the systematic sensitivity analyses performed
 * in the scope of the LinearRao.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class SystematicAnalysisEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystematicAnalysisEngine.class);

    private RaoParameters raoParameters;

    /**
     * LinearRao configurations, containing the default and fallback configurations
     * of the sensitivity computation
     */
    private LinearRaoParameters linearRaoParameters;

    /**
     * A boolean indicating whether or not the fallback mode of the sensitivity computation
     * engine is active.
     */
    private boolean fallbackMode;

    /**
     * Computation Manager
     */
    private ComputationManager computationManager;

    /**
     * Constructor
     */
    SystematicAnalysisEngine(RaoParameters raoParameters, ComputationManager computationManager) {
        this.raoParameters = raoParameters;
        this.linearRaoParameters = LinearRaoConfigurationUtil.getLinearRaoParameters(raoParameters);
        this.computationManager = computationManager;
        this.fallbackMode = false;
    }

    boolean isFallback() {
        return fallbackMode;
    }

    /**
     * Run the systematic sensitivity analysis on one Situation, and evaluate the value of the
     * objective function on this Situation.
     *
     * Throw a SensitivityComputationException if the computation fails.
     */
    void run(AbstractSituation abstractSituation) {

        SensitivityComputationParameters sensiConfig = fallbackMode ? linearRaoParameters.getFallbackSensiParameters() : linearRaoParameters.getSensitivityComputationParameters();

        try {
            runWithConfig(abstractSituation, sensiConfig);
        } catch (SensitivityComputationException e) {
            if (!fallbackMode && linearRaoParameters.getFallbackSensiParameters() != null) { // default mode fails, retry in fallback mode
                LOGGER.warn("Error while running the sensitivity computation with default parameters, fallback sensitivity parameters are now used.");
                fallbackMode = true;
                run(abstractSituation);
            } else if (!fallbackMode) { // no fallback mode available, throw an exception
                abstractSituation.deleteNetworkVariant();
                abstractSituation.deleteCracResultVariant();
                throw new SensitivityComputationException("Sensitivity computation failed with default parameters. No fallback parameters available.", e);
            } else { // fallback mode fails, throw an exception
                abstractSituation.deleteNetworkVariant();
                abstractSituation.deleteCracResultVariant();
                throw new SensitivityComputationException("Sensitivity computation failed with all available sensitivity parameters.", e);
            }
        }
    }

    /**
     * Run the systematic sensitivity analysis with given SensitivityComputationParameters, throw a
     * SensitivityComputationException is the computation fails.
     */
    private void runWithConfig(AbstractSituation abstractSituation, SensitivityComputationParameters sensitivityComputationParameters) {

        try {
            SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult = SystematicSensitivityAnalysisService
                .runAnalysis(abstractSituation.getNetwork(), abstractSituation.getCrac(), computationManager, sensitivityComputationParameters);

            if (systematicSensitivityAnalysisResult.getStateSensiMap().containsValue(null) || systematicSensitivityAnalysisResult.getCnecFlowMap().isEmpty()) {
                throw new SensitivityComputationException("Some output data of the sensitivity computation are missing.");
            }

            // update Loopflow
            if (useLoopFlowExtension(raoParameters)) {
                computeLoopflowOnCurrentSituationAndUpdateInCnecResult(abstractSituation);
            }
            // end update loopflow

            setResults(abstractSituation, systematicSensitivityAnalysisResult);

        } catch (Exception e) {
            throw new SensitivityComputationException("Sensitivity computation fails.", e);
        }
    }

    /**
     * add results of the systematic analysis (flows and objective function value) in the
     * Crac result variant of the situation.
     */
    private void setResults(AbstractSituation situation, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        situation.setCost(-getMinMargin(situation, systematicSensitivityAnalysisResult));
        situation.setSystematicSensitivityAnalysisResult(systematicSensitivityAnalysisResult);
        updateCracExtension(situation);
        updateCnecExtensions(situation, systematicSensitivityAnalysisResult);
    }

    /**
     * Compute the objective function, the minimal margin.
     */
    private double getMinMargin(AbstractSituation situation, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {

        double minMargin = Double.POSITIVE_INFINITY;
        for (Cnec cnec : situation.getCrac().getCnecs()) {
            double flow = systematicSensitivityAnalysisResult.getCnecFlowMap().getOrDefault(cnec, Double.NaN);
            double margin = cnec.computeMargin(flow, Unit.MEGAWATT);
            if (Double.isNaN(margin)) {
                throw new SensitivityComputationException(String.format("Cnec %s is not present in the sensitivity analysis results. Bad behaviour.", cnec.getId()));
            }
            minMargin = Math.min(minMargin, margin);
        }
        return minMargin;
    }

    private void updateCracExtension(AbstractSituation situation) {
        CracResult cracResult = situation.getCrac().getExtension(CracResultExtension.class).getVariant(situation.getCracResultVariant());
        cracResult.setCost(situation.getCost());
        cracResult.setNetworkSecurityStatus();
    }

    private void updateCnecExtensions(AbstractSituation situation, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        situation.getCrac().getCnecs().forEach(cnec -> {
            CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(situation.getCracResultVariant());
            cnecResult.setFlowInMW(systematicSensitivityAnalysisResult.getCnecFlowMap().getOrDefault(cnec, Double.NaN));
            cnecResult.setFlowInA(systematicSensitivityAnalysisResult.getCnecIntensityMap().getOrDefault(cnec, Double.NaN));
            cnecResult.setThresholds(cnec);
        });
    }

    private void computeLoopflowOnCurrentSituationAndUpdateInCnecResult(AbstractSituation situation) {
        Crac crac = situation.getCrac();
        CracLoopFlowExtension cracLoopFlowExtension = crac.getExtension(CracLoopFlowExtension.class);
        // compute maximum loop flow value F_(0,all)_MAX, and update it for each Cnec in Crac
        if (!Objects.isNull(cracLoopFlowExtension)) {
            LoopFlowComputation loopFlowComputation = new LoopFlowComputation(crac, cracLoopFlowExtension);
            Map<String, Double> loopFlows = loopFlowComputation.calculateLoopFlows(situation.getNetwork());
            updateLoopflowInCnecResult(situation, crac, loopFlows);
        }
    }

    private void updateLoopflowInCnecResult(AbstractSituation situation, Crac crac, Map<String, Double> fZeroAll) {
        boolean isInitialSituation = situation.isInitialSituation(); // test if initial situation or optimized situation
        crac.getCnecs(crac.getPreventiveState()).forEach(cnec -> {
            CnecLoopFlowExtension cnecLoopFlowExtension = cnec.getExtension(CnecLoopFlowExtension.class);
            CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(situation.getCracResultVariant());
            if (!Objects.isNull(cnecLoopFlowExtension)) {
                double currentLoopFlow = fZeroAll.get(cnec.getNetworkElement().getId());
                if (isInitialSituation) {
                    double inputLoopflowLimit = cnecLoopFlowExtension.getInputLoopFlow();
                    double cnecLoopflowThreshold = Math.max(currentLoopFlow, inputLoopflowLimit);
                    cnecResult.setLoopflowconstraint(cnecLoopflowThreshold); // add initial loopflow limit
                } else {
                    cnecResult.setFinalLoopflow(currentLoopFlow); // set current loopflow result
                }
            }
        });
    }

    private static boolean useLoopFlowExtension(RaoParameters parameters) {
        return parameters.isRaoWithLoopFlowLimitation();
    }
}
