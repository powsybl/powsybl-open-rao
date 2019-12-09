package com.farao_community.farao.util;

import com.farao_community.farao.data.crac_api.Contingency;
import com.powsybl.sensitivity.SensitivityComputationResults;

import java.util.Map;

public class SensitivitySecurityAnalysisResult {

    private Map<Contingency, SensitivityComputationResults> resultMap;

//    SensitivityComputationResults : contains a List<SensitivityValue> :
        // each SensitivityValue is
        //    private final SensitivityFactor sensitivityFactor;
            //    each SensitivityFactor is
            //    private final F sensitivityFunction; => RangeAction
            //    private final V sensitivityVariable; => NetworkElement
        //    private final double value; => sensitivity of couple (RangeAction, NetworkElement)
        //    private final double functionReference => reference flux of RangeAction
        //    private final double variableReference => reference flux of NetworkElement

    public SensitivitySecurityAnalysisResult(Map<Contingency, SensitivityComputationResults> contingencySensitivityComputationResultsMap) {
        resultMap = contingencySensitivityComputationResultsMap;
    }
}
