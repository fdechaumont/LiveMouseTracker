# AGENTS.md

## Project overview

LiveMouseTracker (LMT) is a Java plugin for the [ICY bioimage analysis platform](https://icy.bioimageanalysis.org/) that tracks live mice using Kinect depth-sensing hardware, RFID antennas, and audio (USV) capture. Licensed GPL v3.

Published as: de Chaumont, F. et al. "Live Mouse Tracker: real-time behavioral analysis of groups of mice." *Nature Biomedical Engineering* (2019). doi: [10.1038/s41551-019-0396-1](https://doi.org/10.1038/s41551-019-0396-1). Preprint: [bioRxiv 345132](https://doi.org/10.1101/345132). Data portal: [livemousetracker.org](https://livemousetracker.org/).

The ICY kernel source is available at [gitlab.pasteur.fr/bia/icy/icy](https://gitlab.pasteur.fr/bia/icy/icy) — a Maven project (`org.bioimageanalysis.icy:icy-kernel:2.5.4`), Java 1.8.

## Build & run

- **No command-line build system for LMT** (no Maven, Gradle, Ant, or Makefile). The LMT project is Eclipse-only.
- Eclipse project name: `LMT 2022` (see `.project`).
- Java compliance level: **1.8** (source and target). The `jdt.core.prefs` enforces this.
- To build an executable JAR, use the Eclipse export descriptor at `LiveMouseTracker/export.jardesc`.
- Deployment: the exported JAR is placed into ICY's `plugins/` directory.
- There is no test suite, no CI, and no linter/formatter config.

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
- `PluginBundled` is imported in 4 classes but never implemented — sub-plugins are not grouped under the main plugin in ICY's UI.

## ICY framework APIs used by LMT

The main LMT class extends `PluginActionable` (which extends `Plugin` and implements `Runnable`). The `run()` method is the plugin entry point.

| ICY Package | Key Classes | Purpose in LMT |
|---|---|---|
| `icy.main` | `Icy` | Application singleton — `Icy.getMainInterface()` for GUI/sequences |
| `icy.plugin.abstract_` | `PluginActionable`, `Plugin` | Plugin base class; `getPreferencesRoot()`, `loadLibrary()` |
| `icy.sequence` | `Sequence` (8689 lines) | 5D image container (XYCZT); holds Overlays and ROIs; depth video frames |
| `icy.painter` | `Overlay` | Custom drawing on sequences — mouse tracks, labels, detection zones |
| `icy.roi` | `ROI2D`, `BooleanMask2D`, `ROIUtil` | Mouse detection masks; boolean mask ops (union, intersection, subtraction) |
| `plugins.kernel.roi.roi2d` | `ROI2DArea`, `ROI2DRectangle`, `ROI2DPolygon` | Concrete ROI shapes — `ROI2DArea` for pixel-level detection, `ROI2DRectangle` for bounding boxes |
| `icy.preferences` | `XMLPreferences` | XML-backed persistent plugin configuration |
| `icy.system.thread` | `ThreadUtil`, `Processor` | EDT dispatch (`invokeNow`/`invokeLater`), background processing (`bgRun`) |
| `icy.type` | `DataType`, `Point5D` | Pixel type enum, n-dimensional geometry |
| `icy.gui.frame` | `IcyFrame`, `IcyFrameListener` | Custom plugin windows within ICY desktop |

## Architecture notes

- **Main class**: `plugins.fab.livemousetracker.LiveMouseTracker` (~5500 lines) is the central hub. It implements `KinectListener`, `ActionListener`, and `IcyFrameListener` in addition to extending `PluginActionable`.
- Key subsystems within `livemousetracker/`: `detection/`, `rfid/`, `track/`, `splitter/`, `machinelearning/`, `morpho/`, `transform/`, `MPEGRecorder/`, `postprocessdatabase/`, `liveanalysis/`, `network/`, `overlay/`, `device/`, `identity/`, `experiment/`, `remotearena/`.
- The `Sequence` object is the core data model — a 5D image (XYCZT) with attached Overlays and ROIs. LMT creates sequences for depth video and attaches custom overlays for rendering.
- `BooleanMask2D` boolean operations (union/intersection/subtraction) are the foundation of the mouse detection and segmentation algorithm.
- Packages follow ICY's plugin convention: `plugins.fab.<pluginname>`.

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
