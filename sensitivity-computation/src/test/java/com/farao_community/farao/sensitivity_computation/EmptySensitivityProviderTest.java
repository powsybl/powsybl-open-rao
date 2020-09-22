package com.farao_community.farao.sensitivity_computation;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.factors.BranchFlowPerPSTAngle;
import com.powsybl.sensitivity.factors.BranchIntensityPerPSTAngle;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class EmptySensitivityProviderTest {
    @Test
    public void cracWithoutRangeActionButWithPst() {
        Crac crac = CommonCracCreation.create();
        Network network = NetworkImportsUtil.import12NodesNetwork();
        EmptySensitivityProvider provider = new EmptySensitivityProvider();
        provider.addSensitivityFactors(crac.getRangeActions(), crac.getCnecs());

        // Common Crac contains 6 CNEC and 1 range action
        List<SensitivityFactor> factorList = provider.getFactors(network);
        assertEquals(4, factorList.size());
        assertEquals(2, factorList.stream().filter(factor -> factor instanceof BranchFlowPerPSTAngle).count());
        assertEquals(2, factorList.stream().filter(factor -> factor instanceof BranchIntensityPerPSTAngle).count());
    }
}
