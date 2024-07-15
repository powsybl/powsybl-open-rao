package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.data.cracapi.cnec.Cnec;

public interface CnecResult {

    public Cnec getCnec();

    public boolean thresholdOvershoot();

    public MonitoringResult.Status getStatus();

    public String print();

}
