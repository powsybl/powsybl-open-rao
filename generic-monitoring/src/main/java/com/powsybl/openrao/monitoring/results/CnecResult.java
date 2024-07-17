package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.data.cracapi.cnec.Cnec;

public interface CnecResult {

    Cnec getCnec();

    boolean thresholdOvershoot();

    MonitoringResult.Status getStatus();

    String print();

}
