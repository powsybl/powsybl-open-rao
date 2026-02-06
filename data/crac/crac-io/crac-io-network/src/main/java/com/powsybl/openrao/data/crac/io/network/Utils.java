package com.powsybl.openrao.data.crac.io.network;

import com.powsybl.iidm.network.*;

import java.util.Optional;
import java.util.Set;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class Utils {

    private Utils() {
        // should not be used
    }

    public static boolean branchIsInVRange(Branch<?> branch, Optional<Double> minV, Optional<Double> maxV) {
        return (minV.isEmpty() || (branch.getTerminal1().getVoltageLevel().getNominalV() >= minV.get() && branch.getTerminal2().getVoltageLevel().getNominalV() >= minV.get()))
            && (maxV.isEmpty() || (branch.getTerminal1().getVoltageLevel().getNominalV() <= maxV.get() && branch.getTerminal2().getVoltageLevel().getNominalV() <= maxV.get()));
    }

    public static boolean terminalIsInCountries(Terminal terminal, Set<Country> countries) {
        Optional<Substation> optionalSubstation = terminal.getVoltageLevel().getSubstation();
        return optionalSubstation.isPresent() && optionalSubstation.get().getCountry().isPresent() &&
            countries.contains(optionalSubstation.get().getCountry().get());
    }

    public static boolean branchIsInCountries(Branch<?> branch, Optional<Set<Country>> countries) {
        if (countries.isEmpty()) {
            return true;
        }
        return terminalIsInCountries(branch.getTerminal1(), countries.get()) || terminalIsInCountries(branch.getTerminal2(), countries.get());
    }

    public static boolean generatorIsInCountries(Generator generator, Set<Country> countries) {
        if (countries == null) {
            return true;
        }
        Optional<Substation> substationOptional = generator.getTerminal().getVoltageLevel().getSubstation();
        if (substationOptional.isEmpty()) {
            return false;
        }
        Substation substation = substationOptional.get();
        if (substation.getCountry().isEmpty()) {
            return false;
        }
        return countries.contains(substation.getCountry().get());
    }
}
