package com.powsybl.openrao.raoapi;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.LoadType;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.intertemporalconstraint.PowerGradient;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

public final class IcsImporter {
    private static final int OFFSET = 2;
    private static final double COST_UP = 10;
    private static final double COST_DOWN = 10;

    //TODO:QUALITY CHECK: do PO respect constraints?

    private IcsImporter() {
        //should only be used statically
    }

    public static void populateInputWithICS(InterTemporalRaoInput interTemporalRaoInput, InputStream staticInputStream, InputStream seriesInputStream) throws IOException {
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
            .setDelimiter(";")
            .setHeader()
            .setSkipHeaderRecord(true)
            .get();
        Iterable<CSVRecord> staticCsvRecords = csvFormat.parse(new InputStreamReader(staticInputStream));
        Iterable<CSVRecord> seriesCsvRecords = csvFormat.parse(new InputStreamReader(seriesInputStream));

        Map<String, Map<String, CSVRecord>> seriesPerIdAndType = new HashMap<>();
        seriesCsvRecords.forEach(record -> {
            seriesPerIdAndType.putIfAbsent(record.get("RA RD ID"), new HashMap<>());
            seriesPerIdAndType.get(record.get("RA RD ID")).put(record.get("Type of timeseries"), record);
        });

        staticCsvRecords.forEach(staticRecord -> {
            if (shouldBeImported(staticRecord)) {
                String raId = staticRecord.get("RA RD ID");
                Map<String, CSVRecord> seriesPerType = seriesPerIdAndType.get(raId);
                if (seriesPerType != null && seriesPerType.containsKey("P0") && seriesPerType.containsKey("RDP-") && seriesPerType.containsKey("RDP+")) {
                    String networkElement = processNetworks(staticRecord.get("UCT Node or GSK ID"), interTemporalRaoInput, seriesPerType);
                    if (networkElement == null) {
                        return;
                    }
                    interTemporalRaoInput.getRaoInputs().getDataPerTimestamp().forEach((dateTime, raoInput) -> {
                        Crac crac = raoInput.getCrac();
                        Double p0 = Double.parseDouble(seriesPerType.get("P0").get(dateTime.getHour() + OFFSET));
                        InjectionRangeActionAdder injectionRangeActionAdder = crac.newInjectionRangeAction()
                            .withId(raId + "_RD")
                            .withName(staticRecord.get("Generator Name"))
                            .withNetworkElement(networkElement)
                            .withInitialSetpoint(p0)
                            .withVariationCost(COST_UP, VariationDirection.UP)
                            .withVariationCost(COST_DOWN, VariationDirection.DOWN)
                            .newRange()
                            .withMin(p0 - Double.parseDouble(seriesPerType.get("RDP-").get(dateTime.getHour() + OFFSET)))
                            .withMax(p0 + Double.parseDouble(seriesPerType.get("RDP+").get(dateTime.getHour() + OFFSET)))
                            .add();
                        if (staticRecord.get("Preventive").equals("TRUE")) {
                            injectionRangeActionAdder.newOnInstantUsageRule()
                                .withInstant(crac.getPreventiveInstant().getId())
                                .withUsageMethod(UsageMethod.AVAILABLE)
                                .add();
                        }
                        if (staticRecord.get("Curative").equals("TRUE")) {
                            injectionRangeActionAdder.newOnInstantUsageRule()
                                .withInstant(crac.getLastInstant().getId())
                                .withUsageMethod(UsageMethod.AVAILABLE)
                                .add();
                        }
                        injectionRangeActionAdder.add();

                    });

                    PowerGradient powerGradient = PowerGradient.builder()
                        .withNetworkElementId(networkElement)
                        .withMaxValue(Double.parseDouble(
                            staticRecord.get("Maximum positive power gradient [MW/h]").isEmpty() ? "1000" : staticRecord.get("Maximum positive power gradient [MW/h]")
                        )).withMinValue(-Double.parseDouble(
                            staticRecord.get("Maximum negative power gradient [MW/h]").isEmpty() ? "1000" : staticRecord.get("Maximum negative power gradient [MW/h]")
                        )).build();
                    interTemporalRaoInput.getPowerGradients().add(powerGradient);
                }
            }
        });

    }

    private static String processNetworks(String uctNodeOrGskId, InterTemporalRaoInput interTemporalRaoInput, Map<String, CSVRecord> seriesPerType) {
        String generatorId = seriesPerType.get("P0").get("RA RD ID") + "_GENERATOR";
        for (Map.Entry<OffsetDateTime, RaoInput> entry : interTemporalRaoInput.getRaoInputs().getDataPerTimestamp().entrySet()) {
            Bus bus = entry.getValue().getNetwork().getBusBreakerView().getBus(uctNodeOrGskId);
            if (bus == null) {
                return null;
            }
            Double p0 = Double.parseDouble(seriesPerType.get("P0").get(entry.getKey().getHour() + OFFSET));
            processBus(bus, generatorId, p0);
        }
        return generatorId;
    }

    private static void processBus(Bus bus, String generatorId, Double p0) {
        bus.getVoltageLevel().newGenerator()
            .setBus(bus.getId())
            .setEnsureIdUnicity(true)
            .setId(generatorId)
            .setMaxP(999999)
            .setMinP(0)
            .setTargetP(p0)
            .setTargetQ(0)
            .setTargetV(bus.getVoltageLevel().getNominalV())
            .setVoltageRegulatorOn(false)
            .add()
            .setFictitious(true);

        bus.getVoltageLevel().newLoad()
            .setBus(bus.getId())
            .setEnsureIdUnicity(true)
            .setId(bus.getId() + "_LOAD")
            .setP0(p0)
            .setQ0(0)
            .setLoadType(LoadType.FICTITIOUS)
            .add();
    }

    private static boolean shouldBeImported(CSVRecord staticRecord) {
        return staticRecord.get("RD description mode").equals("NODE") &&
            (staticRecord.get("Preventive").equals("TRUE") || staticRecord.get("Curative").equals("TRUE"));
    }
}