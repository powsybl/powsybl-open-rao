package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.Unit;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.linear_rao.config.LinearRaoParameters;
import com.farao_community.farao.util.SensitivityComputationException;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

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
    SystematicAnalysisEngine(LinearRaoParameters linearRaoParameters, ComputationManager computationManager) {
        this.linearRaoParameters = linearRaoParameters;
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
    void run(LinearRaoData linearRaoData) {
        SystematicSensitivityAnalysisResult sensiResults = runSensitivityAnalysis(linearRaoData);
        setResults(linearRaoData, sensiResults);
    }

    /**
     * Run the systematic sensitivity analysis on one Situation.
     *
     * Throw a SensitivityComputationException if the computation fails.
     */
    private SystematicSensitivityAnalysisResult runSensitivityAnalysis(LinearRaoData linearRaoData) {

        SensitivityComputationParameters sensiConfig = fallbackMode ? linearRaoParameters.getFallbackSensiParameters() : linearRaoParameters.getSensitivityComputationParameters();

        try {
            return runSensitivityAnalysisWithConfig(linearRaoData, sensiConfig);
        } catch (SensitivityComputationException e) {
            if (!fallbackMode && linearRaoParameters.getFallbackSensiParameters() != null) { // default mode fails, retry in fallback mode
                LOGGER.warn("Error while running the sensitivity computation with default parameters, fallback sensitivity parameters are now used.");
                fallbackMode = true;
                return runSensitivityAnalysis(linearRaoData);
            } else if (!fallbackMode) { // no fallback mode available, throw an exception
                throw new SensitivityComputationException("Sensitivity computation failed with default parameters. No fallback parameters available.", e);
            } else { // fallback mode fails, throw an exception
                throw new SensitivityComputationException("Sensitivity computation failed with all available sensitivity parameters.", e);
            }
        }
    }

    /**
     * Run the systematic sensitivity analysis with given SensitivityComputationParameters, throw a
     * SensitivityComputationException is the computation fails.
     */
    private SystematicSensitivityAnalysisResult runSensitivityAnalysisWithConfig(LinearRaoData linearRaoData, SensitivityComputationParameters sensitivityComputationParameters) {

        try {
            SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult = SystematicSensitivityAnalysisService
                .runAnalysis(linearRaoData.getNetwork(), linearRaoData.getCrac(), computationManager, sensitivityComputationParameters);

            checkSensiResults(linearRaoData, systematicSensitivityAnalysisResult);
            return systematicSensitivityAnalysisResult;

        } catch (Exception e) {
            throw new SensitivityComputationException("Sensitivity computation fails.", e);
        }
    }

    private void checkSensiResults(LinearRaoData linearRaoData, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        if (!systematicSensitivityAnalysisResult.isSuccess()) {
            throw new SensitivityComputationException("Status of the sensitivity result indicates a failure.");
        }

        if (linearRaoData.getCrac().getCnecs().stream().
            map(systematicSensitivityAnalysisResult::getReferenceFlow).
            anyMatch(f -> Double.isNaN(f))) {
            throw new SensitivityComputationException("Flow values are missing from the output of the sensitivity analysis.");
        }

    }

    /**
     * add results of the systematic analysis (flows and objective function value) in the
     * Crac result variant of the situation.
     */
    private void setResults(LinearRaoData linearRaoData, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        linearRaoData.setSystematicSensitivityAnalysisResult(systematicSensitivityAnalysisResult);
        linearRaoData.getCracResult().setFunctionalCost(-getMinMargin(linearRaoData, systematicSensitivityAnalysisResult));
        linearRaoData.getCracResult().setVirtualCost(fallbackMode ? linearRaoParameters.getFallbackOvercost() : 0);
        updateCnecExtensions(linearRaoData, systematicSensitivityAnalysisResult);
    }

    /**
     * Compute the objective function, the minimal margin.
     */
    private double getMinMargin(LinearRaoData linearRaoData, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {

        if (linearRaoParameters.getObjectiveFunction() == LinearRaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT) {
            return getMinMarginInMegawatt(linearRaoData, systematicSensitivityAnalysisResult);
        } else {
            return getMinMarginInAmpere(linearRaoData, systematicSensitivityAnalysisResult);
        }
    }

    private double getMinMarginInMegawatt(LinearRaoData linearRaoData, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        return linearRaoData.getCrac().getCnecs().stream().
           map(cnec -> cnec.computeMargin(systematicSensitivityAnalysisResult.getReferenceFlow(cnec), Unit.MEGAWATT)).
           min(Double::compareTo).orElseThrow(NoSuchElementException::new);

    }

    private double getMinMarginInAmpere(LinearRaoData linearRaoData, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {

        List<Double> marginsInAmpere = linearRaoData.getCrac().getCnecs().stream().map(cnec ->
            cnec.computeMargin(systematicSensitivityAnalysisResult.getReferenceIntensity(cnec), Unit.AMPERE)
        ).collect(Collectors.toList());

        if (marginsInAmpere.contains(Double.NaN)) {

            if (!fallbackMode) {
                // in default mode, this means that there is an error in the sensitivity computation, or an
                // incompatibility with the sensitivity computation mode (i.e. the sensitivity computation is
                // made in DC mode and no intensity are computed).
                throw new SensitivityComputationException("Intensity values are missing from the output of the sensitivity analysis. Min margin cannot be calculated in AMPERE.");
            } else {

                // in fallback, intensities can be missing as the fallback configuration does not necessarily
                // compute them (example : default in AC, fallback in DC). In that case a fallback computation
                // of the intensity is made, based on the MEGAWATT values and the nominal voltage
                LOGGER.warn("No intensities available in fallback mode, the margins are assessed by converting the flows from MW to A with the nominal voltage of each Cnec.");
                marginsInAmpere = getMarginsInAmpereFromMegawattConversion(linearRaoData, systematicSensitivityAnalysisResult);
            }
        }

        return marginsInAmpere.stream().min(Double::compareTo).orElseThrow(NoSuchElementException::new);
    }

    private List<Double> getMarginsInAmpereFromMegawattConversion(LinearRaoData linearRaoData, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        return linearRaoData.getCrac().getCnecs().stream().map(cnec -> {
                double flowInMW = systematicSensitivityAnalysisResult.getReferenceFlow(cnec);
                double uNom = linearRaoData.getNetwork().getBranch(cnec.getNetworkElement().getId()).getTerminal1().getVoltageLevel().getNominalV();
                return cnec.computeMargin(flowInMW * 1000 / (Math.sqrt(3) * uNom), Unit.AMPERE);
            }
        ).collect(Collectors.toList());
    }

    private void updateCnecExtensions(LinearRaoData linearRaoData, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        linearRaoData.getCrac().getCnecs().forEach(cnec -> {
            CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(linearRaoData.getWorkingVariantId());
            cnecResult.setFlowInMW(systematicSensitivityAnalysisResult.getReferenceFlow(cnec));
            cnecResult.setFlowInA(systematicSensitivityAnalysisResult.getReferenceIntensity(cnec));
            cnecResult.setThresholds(cnec);
        });
    }
}
