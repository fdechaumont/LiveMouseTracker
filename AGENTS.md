# AGENTS.md

## Project overview

LiveMouseTracker (LMT) tracks live mice using Kinect depth-sensing hardware, RFID antennas, and audio (USV) capture. Licensed GPL v3.

Published as: de Chaumont, F. et al. "Live Mouse Tracker: real-time behavioral analysis of groups of mice." *Nature Biomedical Engineering* (2019). doi: [10.1038/s41551-019-0396-1](https://doi.org/10.1038/s41551-019-0396-1). Preprint: [bioRxiv 345132](https://doi.org/10.1101/345132). Data portal: [livemousetracker.org](https://livemousetracker.org/).

## Build & run

- **No command-line build system for LMT** (no Maven, Gradle, Ant, or Makefile). The LMT project is Eclipse-only.
- Eclipse project name: `LMT 2022` (see `.project`).
- Java compliance level: **1.8** (source and target). The `jdt.core.prefs` enforces this.
- To build an executable JAR, use the Eclipse export descriptor at `LiveMouseTracker/export.jardesc`.
- Deployment: the exported JAR is placed into ICY's `plugins/` directory.
- There is no test suite, no CI, and no linter/formatter config.
- **Current distributed build**: December 2025, build 1266. The LMT plugin JAR is `LMT v1266 - 1 dec 2025.jar`.
- **Launch mechanism**: the `.bat` file copies native DLLs (`jssc.dll`, `ufdw_j4k2_64bit.dll`) to `lib/win64/`, then launches ICY with JVM params tuned for real-time: 6 GB heap (`-Xmx6G`), CMS garbage collector, DirectDraw disabled (`-Dsun.java2d.noddraw=true`). Entry point: `-x plugins.fab.livemousetracker.LMTLauncher`.
- **LMTLauncher**: a trivial bootstrap plugin (16 lines) that sets `launchOK = true`. `LiveMouseTracker` checks this flag at line 651 to validate the system was launched correctly via the bat file.
- **Bundled ICY version**: 1.9.10.0 with ~70 platform libraries. Key bundled dependencies: `sqlite-jdbc-3.8.11.2`, `weka.jar`, `jSerialComm-2.3.0`, `KinectDriver v007`, `ufdw.jar`.

## Source layout

```
LiveMouseTracker/                    # Eclipse sub-project (the LMT plugin)
├── src/
│   ├── plugins/fab/
│   │   ├── livemousetracker/        # Main LMT plugin (~306 .java files)
│   │   ├── kinectdriver/            # Kinect hardware driver plugin
│   │   └── aaa/voc/                 # USV/vocalization analysis (~55 files)
│   └── jssc/                        # Bundled Java Simple Serial Connector source
├── lib/win64/                       # Windows native DLLs (jssc, j3d, ufdw)
├── bin/                             # Eclipse output directory
└── *.jar                            # All vendored dependencies (~30 JARs)
```

- All LMT dependencies are committed as vendored JARs in `LiveMouseTracker/LiveMouseTracker/` and referenced in the root `.classpath`. There is no dependency manager.
- Native libraries in `lib/win64/` are Windows-only.

## ICY plugin discovery

- LMT has **no explicit plugin descriptor files** (no XML, no annotations, no `META-INF/`).
- ICY auto-discovers plugins by scanning all classes under the `plugins.*` package that extend `icy.plugin.abstract_.Plugin`. Any class extending `PluginActionable` becomes a separate ICY plugin.
- This means ~61 classes in LMT are registered as independent ICY plugins (not just the main class). This includes test utilities, calibration tools, RFID readers, USV viewers, etc.
- `PluginBundled` is imported in 4 classes but never implemented — it is designed for sub-plugins packaged inside another plugin's JAR (via `getMainPluginClassName()`), which would hide them from the plugin list. Since LMT never implements it, all ~61 sub-plugins appear independently in ICY's UI instead of being grouped under the main plugin.

## ICY framework APIs used by LMT

The main LMT class extends `PluginActionable` (which extends `Plugin` and implements `Runnable`). The `run()` method is the plugin entry point.

| ICY Package | Key Classes | Purpose in LMT |
|---|---|---|
| `icy.main` | `Icy` (1443 lines) | Application singleton — `Icy.getMainInterface()` for GUI/sequences |
| `icy.plugin.abstract_` | `PluginActionable` (44), `Plugin` (494) | Plugin base class; `getPreferencesRoot()`, `loadLibrary()` |
| `icy.sequence` | `Sequence` (7192 lines) | 5D image container (XYCZT); holds Overlays and ROIs; depth video frames |
| `icy.painter` | `Overlay` (990) | Custom drawing on sequences — mouse tracks, labels, detection zones |
| `icy.roi` | `ROI` (3456), `BooleanMask2D` (2976), `ROIUtil` (2640) | Mouse detection masks; boolean mask ops (union, intersection, subtraction) |
| `plugins.kernel.roi.roi2d` | `ROI2DArea` (2628), `ROI2DRectangle` (152), `ROI2DPolygon` (304) | Concrete ROI shapes — `ROI2DArea` for pixel-level detection, `ROI2DRectangle` for bounding boxes |
| `icy.preferences` | `XMLPreferences` (550) | XML-backed persistent plugin configuration |
| `icy.system.thread` | `ThreadUtil` (606), `Processor` (1080) | EDT dispatch (`invokeNow`/`invokeLater`), background processing (`bgRun`) |
| `icy.type` | `DataType`, `Point5D` | Pixel type enum, n-dimensional geometry |
| `icy.gui.frame` | `IcyFrame` (2674), `IcyFrameListener` | Custom plugin windows within ICY desktop |

## Architecture notes

- **Main class**: `plugins.fab.livemousetracker.LiveMouseTracker` (~5500 lines) is the central hub. It implements `KinectListener`, `ActionListener`, and `IcyFrameListener` in addition to extending `PluginActionable`.
- Key subsystems within `livemousetracker/`: `detection/`, `rfid/`, `track/`, `splitter/`, `machinelearning/`, `morpho/`, `transform/`, `MPEGRecorder/`, `postprocessdatabase/`, `liveanalysis/`, `network/`, `overlay/`, `device/`, `identity/`, `experiment/`, `remotearena/`.
- The `Sequence` object is the core data model — a 5D image (XYCZT) with attached Overlays and ROIs. LMT creates sequences for depth video and attaches custom overlays for rendering.
- `BooleanMask2D` boolean operations (union/intersection/subtraction) are the foundation of the mouse detection and segmentation algorithm.
- Packages follow ICY's plugin convention: `plugins.fab.<pluginname>`.

### Arena configuration (lmt-config.xml)

The XML config file placed in the ICY folder defines arena geometry, antenna positions, and detection parameters. To switch configs, rename the desired file to `lmt-config.xml`.

```xml
<root>
  <cagefloor>
    <polygon wallsize="36">
      <point x="114" y="63"></point>  <!-- rectangle defining cage floor in Kinect pixels -->
    </polygon>
  </cagefloor>
  <antenna x="131.5" y="80.5" ray="35" com="COM30"></antenna>
  <parameters depthSensitivity="14" maxDetectionSize="1000" minDetectionSize="100"
              detectionSplitTargetVolume="31000" maxObservableDepth="3000"/>
  <contrast min="0" max="35200"/>
</root>
```

Provided configs: mouse 50x50 (default, 4x4=16 antennas COM30-45), block 50x50 with/without walls, rat floor (5x5=25 antennas COM50-74, depthSensitivity=8), EPM (cross-shaped floor, single antenna COM100). Rat mode enables `<ratMode/>` with scaleFactor 20/57.

### RFID hardware

- Recommended: **Biomark APT12 PIT tag (FDX)** — 1.54x better read range than original paper recommendation.
- Antenna tuning target: 134.2 kHz. Use A+/A- connectors on RFID reader board (not AR/A-).
- Reading range degrades ~4.2 mm per kHz deviation from 134.2 kHz.

### Network protocols

- **TCP streaming** (`LiveAnalysisServer`, port 7101): real-time tracking data via XML/JAXB serialization.
- **UDP external events** (localhost:8550): send `"start_EventName"` / `"end_EventName"` to record custom events in the EVENT table. Same port as Avisoft USV triggers.
- **Multi-setup constraint**: minimum 1m between systems due to RFID jamming; disconnect unused RFID hub power. Kinect IR cross-talk mitigated by matte boxes.

## SQLite database schema (canonical interface)

The `.sqlite` file is the data contract between LMT and all downstream analysis tools (see Analysis tools ecosystem below).

- **ANIMAL**: `ID`, `RFID`, `NAME`, `GENOTYPE` (base 4 columns). Analysis tools may `ALTER TABLE` to add `AGE`, `SEX`, `STRAIN`, `SETUP`, `TREATMENT`. Schema is variable (3–9 columns); all tools handle this adaptively.
- **DETECTION**: `FRAMENUMBER`, `ANIMALID` (nullable for anonymous), `MASS_X/Y/Z`, `FRONT_X/Y/Z`, `BACK_X/Y/Z`, `REARING`, `LOOK_UP`, `LOOK_DOWN`, `DATA` (zlib-compressed XML mask blob).
- **EVENT**: `NAME` (string event type), `STARTFRAME`, `ENDFRAME`, `IDANIMALA/B/C/D` (up to 4 animals, nullable), `METADATA` (JSON, added dynamically if missing).
- **FRAME**: `FRAMENUMBER`, `TIMESTAMP` (epoch ms), `NUMPARTICLE`, `PAUSED`, `TEMPERATURE`, `HUMIDITY`, `SOUND`, `LIGHTVISIBLE`, `LIGHTVISIBLEANDIR`.
- **RFIDEVENT**: `RFID`, `TIME`, `X`, `Y`.
- **LOG**: `version`, `process`, `date`, `tmin`, `tmax`.

### Measurement constants (shared across all analysis tools)

- Frame rate: **30 fps** (`oneSecond=30`, `oneMinute=1800`, `oneHour=108000`, `oneDay=2592000`)
- Pixel-to-cm: **10/57** for mice (50×50 cm arena), **20/57** for rats (100×100 cm)
- Contact distance: `8/scaleFactor` px (mass center)
- Head-head/genital threshold: **15 px**
- Speed low: **5 cm/s** (stop/move boundary); speed high: **10 cm/s** (fast movement)
- Body slope for rearing: **40** (frontZ − backZ)
- Follow corridor: width `2.5/scaleFactor`, length `24/scaleFactor`, max angle π/4, speed ratio ≥ 2×

### Event rebuilding convention

Analysis tools do **not** use the Java tracker's live-computed events. They rebuild all behavioral events from raw `DETECTION` data, processing in **1-day windows** (to handle multi-day recordings). The `BuildEvent*` module pattern (each module exports `reBuildEvent(connection, file, tmin, tmax, pool)` and `flush(connection)`) is the standard way to add new event types. Events are stored as named intervals in the EVENT table.

## Analysis tools ecosystem

These are separate open-source projects, not bundled in this repository.

| Tool | Language | Interface | Key Capability | Repository |
|------|----------|-----------|----------------|------------|
| **lmt-analysis** | Python | Library | Core analysis library; `Animal`/`Detection`/`EventTimeLine` classes; 30+ `BuildEvent*` modules; trajectory/heatmap/timeline plots | [GitHub](https://github.com/fdechaumont/lmt-analysis) |
| **LMT-Easy** | Python + PyQt5 | Desktop GUI | 7-tab GUI for reliability reports, event rebuilding, database merging, timeline/trajectory/sensor plotting; no coding required | [GitHub](https://github.com/haribo015/LMT-Easy) |
| **LWTools** | Python + Jupyter | Jupyter widgets | LMT-Index normalization, Linear Mixed Models, DABest estimation statistics, multi-night splitting, CSV export | [GitHub](https://github.com/PaulCarrascosa/LMT_Widget_Tool-LWT) |
| **MouseKing** | Python + R + Nextflow | CLI + Docker | Reproducible pipeline: Wilcoxon univariate stats, PCA/MANOVA multivariate stats, Cohen's d effect sizes, behavioral domain taxonomy | [GitHub](https://github.com/DaleAnnear/MouseKing) |
| **lmt_toolkit_analysis** | Python + Django + Vue.js | Web app | Browser-based: upload SQLite → quality control → edit animals → rebuild events → extract profiles; Docker Compose deployment | [GitHub](https://github.com/ntorquet/lmt_toolkit_analysis) |

## Conventions

- No automated code style enforcement. Code style is informal (mixed French/English comments and identifiers).
- Many files contain commented-out imports or experimental code blocks — these are intentional work-in-progress, not dead code to clean up without understanding context.
- The `TestMQTT.java` at the project root level is a standalone test utility, not part of the plugin proper.
- Files named `*Test.java` in the LMT source tree are **not unit tests** — they are ICY plugins (extending `PluginActionable`) that provide interactive test/debug UIs. There is no test framework.
- **Contact**: fabrice.de.chaumont@gmail.com, eye@igbmc.fr. Community Discord via [micecraft.org/lmt](https://micecraft.org/lmt).
