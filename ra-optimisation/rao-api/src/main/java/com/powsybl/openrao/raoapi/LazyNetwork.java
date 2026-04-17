/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import com.google.common.annotations.Beta;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.extensions.ExtensionAdder;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Area;
import com.powsybl.iidm.network.AreaAdder;
import com.powsybl.iidm.network.Battery;
import com.powsybl.iidm.network.BoundaryLine;
import com.powsybl.iidm.network.BoundaryLineFilter;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Component;
import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.ContainerType;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.DcBus;
import com.powsybl.iidm.network.DcConnectable;
import com.powsybl.iidm.network.DcGround;
import com.powsybl.iidm.network.DcGroundAdder;
import com.powsybl.iidm.network.DcLine;
import com.powsybl.iidm.network.DcLineAdder;
import com.powsybl.iidm.network.DcNode;
import com.powsybl.iidm.network.DcNodeAdder;
import com.powsybl.iidm.network.DcSwitch;
import com.powsybl.iidm.network.DcSwitchAdder;
import com.powsybl.iidm.network.ExportersLoader;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Ground;
import com.powsybl.iidm.network.HvdcConverterStation;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.HvdcLineAdder;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.ImportersLoader;
import com.powsybl.iidm.network.LccConverterStation;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.LineAdder;
import com.powsybl.iidm.network.LineCommutatedConverter;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.NetworkListener;
import com.powsybl.iidm.network.OverloadManagementSystem;
import com.powsybl.iidm.network.ReportNodeContext;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.SubstationAdder;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.iidm.network.TieLineAdder;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.ValidationLevel;
import com.powsybl.iidm.network.VariantManager;
import com.powsybl.iidm.network.VoltageAngleLimit;
import com.powsybl.iidm.network.VoltageAngleLimitAdder;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.VoltageLevelAdder;
import com.powsybl.iidm.network.VoltageSourceConverter;
import com.powsybl.iidm.network.VscConverterStation;

import java.io.File;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * A LazyNetwork is a specific {@link Network} implementation that is defined using a file path.
 * The actual network data is only loaded in memory the first time a method is called on the lazy network.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@Beta
public class LazyNetwork implements Network, AutoCloseable {
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + File.separator;
    private final String networkPath;
    private boolean isLoaded;
    private Network network;

    public LazyNetwork(String networkPath) {
        this.networkPath = networkPath;
        this.isLoaded = false;
    }

    public LazyNetwork(Network network) {
        String networkName = TEMP_DIR + UUID.randomUUID() + ".jiidm";
        // TODO serialize to BIIDM, not stabilized for the moment (04/2026)
        network.write("JIIDM", new Properties(), Path.of(networkName));
        this.networkPath = networkName;
        this.isLoaded = false;
    }

    private void load() {
        if (!isLoaded) {
            network = Network.read(networkPath);
            isLoaded = true;
        }
    }

    @Override
    public void close() throws Exception {
        // TODO: currently modifications on the network will not be saved -> perhaps override networkPath with a UUID and write content to it
        network = null;
        isLoaded = false;
        System.gc();
    }

    @Override
    public Collection<Network> getSubnetworks() {
        load();
        return network.getSubnetworks();
    }

    @Override
    public Network getSubnetwork(String id) {
        load();
        return network.getSubnetwork(id);
    }

    @Override
    public void update(ReadOnlyDataSource dataSource) {
        load();
        network.update(dataSource);
    }

    @Override
    public void update(ReadOnlyDataSource dataSource, Properties properties) {
        load();
        network.update(dataSource, properties);
    }

    @Override
    public void update(ReadOnlyDataSource dataSource, Properties parameters, ReportNode reportNode) {
        load();
        network.update(dataSource, parameters, reportNode);
    }

    @Override
    public void update(ReadOnlyDataSource dataSource, ComputationManager computationManager, ImportConfig config, Properties parameters, ImportersLoader loader, ReportNode reportNode) {
        load();
        network.update(dataSource, computationManager, config, parameters, loader, reportNode);
    }

    @Override
    public Collection<Component> getDcComponents() {
        load();
        return network.getDcComponents();
    }

    @Override
    public ZonedDateTime getCaseDate() {
        load();
        return network.getCaseDate();
    }

    @Override
    public Network setCaseDate(ZonedDateTime zonedDateTime) {
        load();
        return network.setCaseDate(zonedDateTime);
    }

    @Override
    public int getForecastDistance() {
        load();
        return network.getForecastDistance();
    }

    @Override
    public Network setForecastDistance(int i) {
        load();
        return network.setForecastDistance(i);
    }

    @Override
    public String getSourceFormat() {
        load();
        return network.getSourceFormat();
    }

    @Override
    public VariantManager getVariantManager() {
        load();
        return network.getVariantManager();
    }

    @Override
    public void allowReportNodeContextMultiThreadAccess(boolean b) {
        load();
        network.allowReportNodeContextMultiThreadAccess(b);
    }

    @Override
    public ReportNodeContext getReportNodeContext() {
        load();
        return network.getReportNodeContext();
    }

    @Override
    public Set<Country> getCountries() {
        load();
        return network.getCountries();
    }

    @Override
    public int getCountryCount() {
        load();
        return network.getCountryCount();
    }

    @Override
    public Iterable<String> getAreaTypes() {
        load();
        return network.getAreaTypes();
    }

    @Override
    public Stream<String> getAreaTypeStream() {
        load();
        return network.getAreaTypeStream();
    }

    @Override
    public int getAreaTypeCount() {
        load();
        return network.getAreaTypeCount();
    }

    @Override
    public AreaAdder newArea() {
        load();
        return network.newArea();
    }

    @Override
    public Iterable<Area> getAreas() {
        load();
        return network.getAreas();
    }

    @Override
    public Stream<Area> getAreaStream() {
        load();
        return network.getAreaStream();
    }

    @Override
    public Area getArea(String s) {
        load();
        return network.getArea(s);
    }

    @Override
    public int getAreaCount() {
        load();
        return network.getAreaCount();
    }

    @Override
    public SubstationAdder newSubstation() {
        load();
        return network.newSubstation();
    }

    @Override
    public Iterable<Substation> getSubstations() {
        load();
        return network.getSubstations();
    }

    @Override
    public Stream<Substation> getSubstationStream() {
        load();
        return network.getSubstationStream();
    }

    @Override
    public int getSubstationCount() {
        load();
        return network.getSubstationCount();
    }

    @Override
    public Iterable<Substation> getSubstations(Country country, String s, String... strings) {
        load();
        return network.getSubstations(country, s, strings);
    }

    @Override
    public Iterable<Substation> getSubstations(String s, String s1, String... strings) {
        load();
        return network.getSubstations(s, s1, strings);
    }

    @Override
    public Substation getSubstation(String s) {
        load();
        return network.getSubstation(s);
    }

    @Override
    public VoltageLevelAdder newVoltageLevel() {
        load();
        return network.newVoltageLevel();
    }

    @Override
    public Iterable<VoltageLevel> getVoltageLevels() {
        load();
        return network.getVoltageLevels();
    }

    @Override
    public Stream<VoltageLevel> getVoltageLevelStream() {
        load();
        return network.getVoltageLevelStream();
    }

    @Override
    public int getVoltageLevelCount() {
        load();
        return network.getVoltageLevelCount();
    }

    @Override
    public VoltageLevel getVoltageLevel(String s) {
        load();
        return network.getVoltageLevel(s);
    }

    @Override
    public LineAdder newLine() {
        load();
        return network.newLine();
    }

    @Override
    public LineAdder newLine(Line line) {
        load();
        return network.newLine(line);
    }

    @Override
    public Iterable<Line> getLines() {
        load();
        return network.getLines();
    }

    @Override
    public Iterable<TieLine> getTieLines() {
        load();
        return network.getTieLines();
    }

    @Override
    public Branch getBranch(String s) {
        load();
        return network.getBranch(s);
    }

    @Override
    public Iterable<Branch> getBranches() {
        load();
        return network.getBranches();
    }

    @Override
    public Stream<Branch> getBranchStream() {
        load();
        return network.getBranchStream();
    }

    @Override
    public int getBranchCount() {
        load();
        return network.getBranchCount();
    }

    @Override
    public Stream<Line> getLineStream() {
        load();
        return network.getLineStream();
    }

    @Override
    public Stream<TieLine> getTieLineStream() {
        load();
        return network.getTieLineStream();
    }

    @Override
    public int getLineCount() {
        load();
        return network.getLineCount();
    }

    @Override
    public int getTieLineCount() {
        load();
        return network.getTieLineCount();
    }

    @Override
    public Line getLine(String s) {
        load();
        return network.getLine(s);
    }

    @Override
    public TieLine getTieLine(String s) {
        load();
        return network.getTieLine(s);
    }

    @Override
    public TieLineAdder newTieLine() {
        load();
        return network.newTieLine();
    }

    @Override
    public Iterable<TwoWindingsTransformer> getTwoWindingsTransformers() {
        load();
        return network.getTwoWindingsTransformers();
    }

    @Override
    public Stream<TwoWindingsTransformer> getTwoWindingsTransformerStream() {
        load();
        return network.getTwoWindingsTransformerStream();
    }

    @Override
    public int getTwoWindingsTransformerCount() {
        load();
        return network.getTwoWindingsTransformerCount();
    }

    @Override
    public TwoWindingsTransformer getTwoWindingsTransformer(String s) {
        load();
        return network.getTwoWindingsTransformer(s);
    }

    @Override
    public Iterable<ThreeWindingsTransformer> getThreeWindingsTransformers() {
        load();
        return network.getThreeWindingsTransformers();
    }

    @Override
    public Stream<ThreeWindingsTransformer> getThreeWindingsTransformerStream() {
        load();
        return network.getThreeWindingsTransformerStream();
    }

    @Override
    public int getThreeWindingsTransformerCount() {
        load();
        return network.getThreeWindingsTransformerCount();
    }

    @Override
    public ThreeWindingsTransformer getThreeWindingsTransformer(String s) {
        load();
        return network.getThreeWindingsTransformer(s);
    }

    @Override
    public Iterable<OverloadManagementSystem> getOverloadManagementSystems() {
        load();
        return network.getOverloadManagementSystems();
    }

    @Override
    public Stream<OverloadManagementSystem> getOverloadManagementSystemStream() {
        load();
        return network.getOverloadManagementSystemStream();
    }

    @Override
    public int getOverloadManagementSystemCount() {
        load();
        return network.getOverloadManagementSystemCount();
    }

    @Override
    public OverloadManagementSystem getOverloadManagementSystem(String s) {
        load();
        return network.getOverloadManagementSystem(s);
    }

    @Override
    public Iterable<Generator> getGenerators() {
        load();
        return network.getGenerators();
    }

    @Override
    public Stream<Generator> getGeneratorStream() {
        load();
        return network.getGeneratorStream();
    }

    @Override
    public int getGeneratorCount() {
        load();
        return network.getGeneratorCount();
    }

    @Override
    public Generator getGenerator(String s) {
        load();
        return network.getGenerator(s);
    }

    @Override
    public Iterable<Battery> getBatteries() {
        load();
        return network.getBatteries();
    }

    @Override
    public Stream<Battery> getBatteryStream() {
        load();
        return network.getBatteryStream();
    }

    @Override
    public int getBatteryCount() {
        load();
        return network.getBatteryCount();
    }

    @Override
    public Battery getBattery(String s) {
        load();
        return network.getBattery(s);
    }

    @Override
    public Iterable<Load> getLoads() {
        load();
        return network.getLoads();
    }

    @Override
    public Stream<Load> getLoadStream() {
        load();
        return network.getLoadStream();
    }

    @Override
    public int getLoadCount() {
        load();
        return network.getLoadCount();
    }

    @Override
    public Load getLoad(String s) {
        load();
        return network.getLoad(s);
    }

    @Override
    public Iterable<ShuntCompensator> getShuntCompensators() {
        load();
        return network.getShuntCompensators();
    }

    @Override
    public Stream<ShuntCompensator> getShuntCompensatorStream() {
        load();
        return network.getShuntCompensatorStream();
    }

    @Override
    public int getShuntCompensatorCount() {
        load();
        return network.getShuntCompensatorCount();
    }

    @Override
    public ShuntCompensator getShuntCompensator(String s) {
        load();
        return network.getShuntCompensator(s);
    }

    @Override
    public Iterable<BoundaryLine> getBoundaryLines(BoundaryLineFilter boundaryLineFilter) {
        load();
        return network.getBoundaryLines(boundaryLineFilter);
    }

    @Override
    public Iterable<BoundaryLine> getBoundaryLines() {
        load();
        return network.getBoundaryLines();
    }

    @Override
    public Stream<BoundaryLine> getBoundaryLineStream(BoundaryLineFilter boundaryLineFilter) {
        load();
        return network.getBoundaryLineStream(boundaryLineFilter);
    }

    @Override
    public Stream<BoundaryLine> getBoundaryLineStream() {
        load();
        return network.getBoundaryLineStream();
    }

    @Override
    public int getBoundaryLineCount() {
        load();
        return network.getBoundaryLineCount();
    }

    @Override
    public BoundaryLine getBoundaryLine(String s) {
        load();
        return network.getBoundaryLine(s);
    }

    @Override
    public Iterable<StaticVarCompensator> getStaticVarCompensators() {
        load();
        return network.getStaticVarCompensators();
    }

    @Override
    public Stream<StaticVarCompensator> getStaticVarCompensatorStream() {
        load();
        return network.getStaticVarCompensatorStream();
    }

    @Override
    public int getStaticVarCompensatorCount() {
        load();
        return network.getStaticVarCompensatorCount();
    }

    @Override
    public StaticVarCompensator getStaticVarCompensator(String s) {
        load();
        return network.getStaticVarCompensator(s);
    }

    @Override
    public Switch getSwitch(String s) {
        load();
        return network.getSwitch(s);
    }

    @Override
    public Iterable<Switch> getSwitches() {
        load();
        return network.getSwitches();
    }

    @Override
    public Stream<Switch> getSwitchStream() {
        load();
        return network.getSwitchStream();
    }

    @Override
    public int getSwitchCount() {
        load();
        return network.getSwitchCount();
    }

    @Override
    public BusbarSection getBusbarSection(String s) {
        load();
        return network.getBusbarSection(s);
    }

    @Override
    public Iterable<BusbarSection> getBusbarSections() {
        load();
        return network.getBusbarSections();
    }

    @Override
    public Stream<BusbarSection> getBusbarSectionStream() {
        load();
        return network.getBusbarSectionStream();
    }

    @Override
    public int getBusbarSectionCount() {
        load();
        return network.getBusbarSectionCount();
    }

    @Override
    public Iterable<HvdcConverterStation<?>> getHvdcConverterStations() {
        load();
        return network.getHvdcConverterStations();
    }

    @Override
    public Stream<HvdcConverterStation<?>> getHvdcConverterStationStream() {
        load();
        return network.getHvdcConverterStationStream();
    }

    @Override
    public int getHvdcConverterStationCount() {
        load();
        return network.getHvdcConverterStationCount();
    }

    @Override
    public HvdcConverterStation<?> getHvdcConverterStation(String s) {
        load();
        return network.getHvdcConverterStation(s);
    }

    @Override
    public Iterable<LccConverterStation> getLccConverterStations() {
        load();
        return network.getLccConverterStations();
    }

    @Override
    public Stream<LccConverterStation> getLccConverterStationStream() {
        load();
        return network.getLccConverterStationStream();
    }

    @Override
    public int getLccConverterStationCount() {
        load();
        return network.getLccConverterStationCount();
    }

    @Override
    public LccConverterStation getLccConverterStation(String s) {
        load();
        return network.getLccConverterStation(s);
    }

    @Override
    public Iterable<VscConverterStation> getVscConverterStations() {
        load();
        return network.getVscConverterStations();
    }

    @Override
    public Stream<VscConverterStation> getVscConverterStationStream() {
        load();
        return network.getVscConverterStationStream();
    }

    @Override
    public int getVscConverterStationCount() {
        load();
        return network.getVscConverterStationCount();
    }

    @Override
    public VscConverterStation getVscConverterStation(String s) {
        load();
        return network.getVscConverterStation(s);
    }

    @Override
    public Iterable<HvdcLine> getHvdcLines() {
        load();
        return network.getHvdcLines();
    }

    @Override
    public Stream<HvdcLine> getHvdcLineStream() {
        load();
        return network.getHvdcLineStream();
    }

    @Override
    public int getHvdcLineCount() {
        load();
        return network.getHvdcLineCount();
    }

    @Override
    public HvdcLine getHvdcLine(String s) {
        load();
        return network.getHvdcLine(s);
    }

    @Override
    public HvdcLine getHvdcLine(HvdcConverterStation converterStation) {
        load();
        return network.getHvdcLine(converterStation);
    }

    @Override
    public HvdcLineAdder newHvdcLine() {
        load();
        return network.newHvdcLine();
    }

    @Override
    public Iterable<Ground> getGrounds() {
        load();
        return network.getGrounds();
    }

    @Override
    public Stream<Ground> getGroundStream() {
        load();
        return network.getGroundStream();
    }

    @Override
    public int getGroundCount() {
        load();
        return network.getGroundCount();
    }

    @Override
    public Ground getGround(String s) {
        load();
        return network.getGround(s);
    }

    @Override
    public DcNodeAdder newDcNode() {
        load();
        return network.newDcNode();
    }

    @Override
    public Iterable<DcNode> getDcNodes() {
        load();
        return network.getDcNodes();
    }

    @Override
    public Stream<DcNode> getDcNodeStream() {
        load();
        return network.getDcNodeStream();
    }

    @Override
    public int getDcNodeCount() {
        load();
        return network.getDcNodeCount();
    }

    @Override
    public DcNode getDcNode(String s) {
        load();
        return network.getDcNode(s);
    }

    @Override
    public DcLineAdder newDcLine() {
        load();
        return network.newDcLine();
    }

    @Override
    public Iterable<DcLine> getDcLines() {
        load();
        return network.getDcLines();
    }

    @Override
    public Stream<DcLine> getDcLineStream() {
        load();
        return network.getDcLineStream();
    }

    @Override
    public int getDcLineCount() {
        load();
        return network.getDcLineCount();
    }

    @Override
    public DcLine getDcLine(String s) {
        load();
        return network.getDcLine(s);
    }

    @Override
    public DcSwitchAdder newDcSwitch() {
        load();
        return network.newDcSwitch();
    }

    @Override
    public Iterable<DcSwitch> getDcSwitches() {
        load();
        return network.getDcSwitches();
    }

    @Override
    public Stream<DcSwitch> getDcSwitchStream() {
        load();
        return network.getDcSwitchStream();
    }

    @Override
    public int getDcSwitchCount() {
        load();
        return network.getDcSwitchCount();
    }

    @Override
    public DcSwitch getDcSwitch(String s) {
        load();
        return network.getDcSwitch(s);
    }

    @Override
    public DcGroundAdder newDcGround() {
        load();
        return network.newDcGround();
    }

    @Override
    public Iterable<DcGround> getDcGrounds() {
        load();
        return network.getDcGrounds();
    }

    @Override
    public Stream<DcGround> getDcGroundStream() {
        load();
        return network.getDcGroundStream();
    }

    @Override
    public int getDcGroundCount() {
        load();
        return network.getDcGroundCount();
    }

    @Override
    public DcGround getDcGround(String s) {
        load();
        return network.getDcGround(s);
    }

    @Override
    public Iterable<LineCommutatedConverter> getLineCommutatedConverters() {
        load();
        return network.getLineCommutatedConverters();
    }

    @Override
    public Stream<LineCommutatedConverter> getLineCommutatedConverterStream() {
        load();
        return network.getLineCommutatedConverterStream();
    }

    @Override
    public int getLineCommutatedConverterCount() {
        load();
        return network.getLineCommutatedConverterCount();
    }

    @Override
    public LineCommutatedConverter getLineCommutatedConverter(String s) {
        load();
        return network.getLineCommutatedConverter(s);
    }

    @Override
    public Iterable<VoltageSourceConverter> getVoltageSourceConverters() {
        load();
        return network.getVoltageSourceConverters();
    }

    @Override
    public Stream<VoltageSourceConverter> getVoltageSourceConverterStream() {
        load();
        return network.getVoltageSourceConverterStream();
    }

    @Override
    public int getVoltageSourceConverterCount() {
        load();
        return network.getVoltageSourceConverterCount();
    }

    @Override
    public VoltageSourceConverter getVoltageSourceConverter(String s) {
        load();
        return network.getVoltageSourceConverter(s);
    }

    @Override
    public DcBus getDcBus(String s) {
        load();
        return network.getDcBus(s);
    }

    @Override
    public Iterable<DcBus> getDcBuses() {
        load();
        return network.getDcBuses();
    }

    @Override
    public Stream<DcBus> getDcBusStream() {
        load();
        return network.getDcBusStream();
    }

    @Override
    public int getDcBusCount() {
        load();
        return network.getDcBusCount();
    }

    @Override
    public Identifiable<?> getIdentifiable(String s) {
        load();
        return network.getIdentifiable(s);
    }

    @Override
    public Collection<Identifiable<?>> getIdentifiables() {
        load();
        return network.getIdentifiables();
    }

    @Override
    public <C extends Connectable> Iterable<C> getConnectables(Class<C> clazz) {
        load();
        return network.getConnectables(clazz);
    }

    @Override
    public <C extends Connectable> Stream<C> getConnectableStream(Class<C> clazz) {
        load();
        return network.getConnectableStream(clazz);
    }

    @Override
    public <C extends Connectable> int getConnectableCount(Class<C> clazz) {
        load();
        return network.getConnectableCount(clazz);
    }

    @Override
    public Iterable<Connectable> getConnectables() {
        load();
        return network.getConnectables();
    }

    @Override
    public Stream<Connectable> getConnectableStream() {
        load();
        return network.getConnectableStream();
    }

    @Override
    public Connectable<?> getConnectable(String id) {
        load();
        return network.getConnectable(id);
    }

    @Override
    public int getConnectableCount() {
        load();
        return network.getConnectableCount();
    }

    @Override
    public <C extends DcConnectable> Iterable<C> getDcConnectables(Class<C> clazz) {
        load();
        return network.getDcConnectables(clazz);
    }

    @Override
    public <C extends DcConnectable> Stream<C> getDcConnectableStream(Class<C> clazz) {
        load();
        return network.getDcConnectableStream(clazz);
    }

    @Override
    public <C extends DcConnectable> int getDcConnectableCount(Class<C> clazz) {
        load();
        return network.getDcConnectableCount(clazz);
    }

    @Override
    public Iterable<DcConnectable> getDcConnectables() {
        load();
        return network.getDcConnectables();
    }

    @Override
    public Stream<DcConnectable> getDcConnectableStream() {
        load();
        return network.getDcConnectableStream();
    }

    @Override
    public DcConnectable<?> getDcConnectable(String id) {
        load();
        return network.getDcConnectable(id);
    }

    @Override
    public int getDcConnectableCount() {
        load();
        return network.getDcConnectableCount();
    }

    @Override
    public BusBreakerView getBusBreakerView() {
        load();
        return network.getBusBreakerView();
    }

    @Override
    public BusView getBusView() {
        load();
        return network.getBusView();
    }

    @Override
    public VoltageAngleLimitAdder newVoltageAngleLimit() {
        load();
        return network.newVoltageAngleLimit();
    }

    @Override
    public Iterable<VoltageAngleLimit> getVoltageAngleLimits() {
        load();
        return network.getVoltageAngleLimits();
    }

    @Override
    public Stream<VoltageAngleLimit> getVoltageAngleLimitsStream() {
        load();
        return network.getVoltageAngleLimitsStream();
    }

    @Override
    public VoltageAngleLimit getVoltageAngleLimit(String s) {
        load();
        return network.getVoltageAngleLimit(s);
    }

    @Override
    public Network createSubnetwork(String s, String s1, String s2) {
        load();
        return network.createSubnetwork(s, s1, s2);
    }

    @Override
    public Network detach() {
        load();
        return network.detach();
    }

    @Override
    public boolean isDetachable() {
        load();
        return network.isDetachable();
    }

    @Override
    public Set<Identifiable<?>> getBoundaryElements() {
        load();
        return network.getBoundaryElements();
    }

    @Override
    public boolean isBoundaryElement(Identifiable<?> identifiable) {
        load();
        return network.isBoundaryElement(identifiable);
    }

    @Override
    public void flatten() {
        load();
        network.flatten();
    }

    @Override
    public void addListener(NetworkListener networkListener) {
        load();
        network.addListener(networkListener);
    }

    @Override
    public void removeListener(NetworkListener networkListener) {
        load();
        network.removeListener(networkListener);
    }

    @Override
    public IdentifiableType getType() {
        load();
        return network.getType();
    }

    @Override
    public ValidationLevel runValidationChecks() {
        load();
        return network.runValidationChecks();
    }

    @Override
    public ValidationLevel runValidationChecks(boolean throwsException) {
        load();
        return network.runValidationChecks(throwsException);
    }

    @Override
    public ValidationLevel runValidationChecks(boolean throwsException, ReportNode reportNode) {
        load();
        return network.runValidationChecks(throwsException, reportNode);
    }

    @Override
    public ValidationLevel getValidationLevel() {
        load();
        return network.getValidationLevel();
    }

    @Override
    public Network setMinimumAcceptableValidationLevel(ValidationLevel validationLevel) {
        load();
        return network.setMinimumAcceptableValidationLevel(validationLevel);
    }

    @Override
    public Stream<Identifiable<?>> getIdentifiableStream(IdentifiableType identifiableType) {
        load();
        return network.getIdentifiableStream(identifiableType);
    }

    @Override
    public void write(ExportersLoader loader, String format, Properties parameters, DataSource dataSource, ReportNode reportNode) {
        load();
        network.write(loader, format, parameters, dataSource, reportNode);
    }

    @Override
    public void write(ExportersLoader loader, String format, Properties parameters, DataSource dataSource) {
        load();
        network.write(loader, format, parameters, dataSource);
    }

    @Override
    public void write(String format, Properties parameters, DataSource dataSource) {
        load();
        network.write(format, parameters, dataSource);
    }

    @Override
    public void write(ExportersLoader loader, String format, Properties parameters, Path file, ReportNode reportNode) {
        load();
        network.write(loader, format, parameters, file, reportNode);
    }

    @Override
    public void write(ExportersLoader loader, String format, Properties parameters, Path file) {
        load();
        network.write(loader, format, parameters, file);
    }

    @Override
    public void write(String format, Properties parameters, Path file) {
        load();
        network.write(format, parameters, file);
    }

    @Override
    public void write(ExportersLoader loader, String format, Properties parameters, String directory, String baseName, ReportNode reportNode) {
        load();
        network.write(loader, format, parameters, directory, baseName, reportNode);
    }

    @Override
    public void write(ExportersLoader loader, String format, Properties parameters, String directory, String basename) {
        load();
        network.write(loader, format, parameters, directory, basename);
    }

    @Override
    public void write(String format, Properties parameters, String directory, String baseName) {
        load();
        network.write(format, parameters, directory, baseName);
    }

    @Override
    public ContainerType getContainerType() {
        load();
        return network.getContainerType();
    }

    @Override
    public Network getNetwork() {
        load();
        return network.getNetwork();
    }

    @Override
    public Network getParentNetwork() {
        load();
        return network.getParentNetwork();
    }

    @Override
    public String getId() {
        load();
        return network.getId();
    }

    @Override
    public Set<String> getAliases() {
        load();
        return network.getAliases();
    }

    @Override
    public Optional<String> getAliasFromType(String aliasType) {
        load();
        return network.getAliasFromType(aliasType);
    }

    @Override
    public Optional<String> getAliasType(String alias) {
        load();
        return network.getAliasType(alias);
    }

    @Override
    public void addAlias(String alias) {
        load();
        network.addAlias(alias);
    }

    @Override
    public void addAlias(String alias, boolean ensureAliasUnicity) {
        load();
        network.addAlias(alias, ensureAliasUnicity);
    }

    @Override
    public void addAlias(String alias, String aliasType) {
        load();
        network.addAlias(alias, aliasType);
    }

    @Override
    public void addAlias(String alias, String aliasType, boolean ensureAliasUnicity) {
        load();
        network.addAlias(alias, aliasType, ensureAliasUnicity);
    }

    @Override
    public void removeAlias(String alias) {
        load();
        network.removeAlias(alias);
    }

    @Override
    public boolean hasAliases() {
        load();
        return network.hasAliases();
    }

    @Override
    public Optional<String> getOptionalName() {
        load();
        return network.getOptionalName();
    }

    @Override
    public String getNameOrId() {
        load();
        return network.getNameOrId();
    }

    @Override
    public Network setName(String name) {
        load();
        return network.setName(name);
    }

    @Override
    public boolean isFictitious() {
        load();
        return network.isFictitious();
    }

    @Override
    public void setFictitious(boolean fictitious) {
        load();
        network.setFictitious(fictitious);
    }

    @Override
    public <E extends Extension<Network>> void addExtension(Class<? super E> aClass, E e) {
        load();
        network.addExtension(aClass, e);
    }

    @Override
    public <E extends Extension<Network>> E getExtension(Class<? super E> aClass) {
        load();
        return network.getExtension(aClass);
    }

    @Override
    public <E extends Extension<Network>> E getExtensionByName(String s) {
        load();
        return network.getExtensionByName(s);
    }

    @Override
    public <E extends Extension<Network>> boolean removeExtension(Class<E> aClass) {
        load();
        return network.removeExtension(aClass);
    }

    @Override
    public <E extends Extension<Network>> Collection<E> getExtensions() {
        load();
        return network.getExtensions();
    }

    @Override
    public String getImplementationName() {
        load();
        return network.getImplementationName();
    }

    @Override
    public <E extends Extension<Network>, B extends ExtensionAdder<Network, E>> B newExtension(Class<B> type) {
        load();
        return network.newExtension(type);
    }

    @Override
    public boolean hasProperty() {
        load();
        return network.hasProperty();
    }

    @Override
    public boolean hasProperty(String s) {
        load();
        return network.hasProperty(s);
    }

    @Override
    public String getProperty(String s) {
        load();
        return network.getProperty(s);
    }

    @Override
    public String getProperty(String s, String s1) {
        load();
        return network.getProperty(s, s1);
    }

    @Override
    public String setProperty(String s, String s1) {
        load();
        return network.setProperty(s, s1);
    }

    @Override
    public boolean removeProperty(String s) {
        load();
        return network.removeProperty(s);
    }

    @Override
    public Set<String> getPropertyNames() {
        load();
        return network.getPropertyNames();
    }

    @Override
    public boolean equals(Object o) {
        load();
        return network.equals(o);
    }

    @Override
    public int hashCode() {
        load();
        return network.hashCode();
    }

    public static Network of(String networkPath) {
        return new LazyNetwork(networkPath);
    }
}
