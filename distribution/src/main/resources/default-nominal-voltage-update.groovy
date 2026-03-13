import com.powsybl.iidm.network.VoltageLevel

/*
    UCTE-DEF file does not provide configuration for default nominal voltage setup.

    This post processor script modifies default nominal voltages in order to adapt it to FMAX
    calculation based on IMAX.

    By default, UCTE sets nominal voltage to 220 and 380kV for the voltage levels 6 and 7, whereas
    default values of Core countries are 225 and 400 kV instead.
 */


network.voltageLevelStream.forEach({ voltageLevel ->
    updateVoltageLevelNominalV(voltageLevel)
})

boolean safeDoubleEquals(double a, double b) {
    return Math.abs(a - b) < 1e-3
}

def updateVoltageLevelNominalV(VoltageLevel voltageLevel) {
    if (safeDoubleEquals(voltageLevel.nominalV, 380)) {
        voltageLevel.nominalV = 400
    } else if (safeDoubleEquals(voltageLevel.nominalV, 220)) {
        voltageLevel.nominalV = 225
    } else {
        // Should not be changed cause is not equal to the default nominal voltage of voltage levels 6 or 7
    }
}
