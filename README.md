# Live Mouse Tracker

**Real-time tracking and behavioral analysis of group-housed mice using depth sensing, RFID, and audio capture.**

Live Mouse Tracker (LMT) is a plugin for the [Icy bioimage analysis platform](https://icy.bioimageanalysis.org/) that tracks multiple mice simultaneously in a home-cage environment. It uses a Microsoft Kinect v2 depth sensor mounted above the cage to detect and segment individual animals in 3D, identifies them via RFID tags and machine learning, and records their behavior for later analysis.

Licensed under [GPL v3](LICENSE).

Developed by Fabrice de Chaumont, Elodie Ey, Nicolas Torquet, Thibault Lagache, Stephane Dallongeville, Albane Imbert, Thierry Legou, Anne-Marie Le Sourd, Philippe Faure, Thomas Bourgeron, and Jean-Christophe Olivo-Marin at the [Biological Image Analysis unit](https://research.pasteur.fr/en/team/bioimage-analysis/), Institut Pasteur.

**Publication**: de Chaumont, F. et al. "Live Mouse Tracker: real-time behavioral analysis of groups of mice." *Nature Biomedical Engineering* (2019). [doi:10.1038/s41551-019-0396-1](https://doi.org/10.1038/s41551-019-0396-1). Preprint: [bioRxiv 345132](https://doi.org/10.1101/345132).

**Data portal**: [livemousetracker.org](https://livemousetracker.org/) — shared datasets, videos, and analysis scripts (R and Python).

---

## Table of Contents

- [How It Works](#how-it-works)
- [Hardware Requirements](#hardware-requirements)
- [Tracking Pipeline](#tracking-pipeline)
  - [1. Background Model](#1-background-model)
  - [2. Foreground Detection](#2-foreground-detection)
  - [3. Contact Splitting](#3-contact-splitting)
  - [4. Track Extension](#4-track-extension)
  - [5. Detection Post-Processing](#5-detection-post-processing)
  - [6. Identity Resolution](#6-identity-resolution)
- [Ultrasonic Vocalization Analysis](#ultrasonic-vocalization-analysis)
- [Behavioral Event Detection](#behavioral-event-detection)
- [Data Output](#data-output)
- [Post-Processing](#post-processing)
- [Analysis Ecosystem](#analysis-ecosystem)
- [User Interface](#user-interface)
- [Building and Installation](#building-and-installation)
- [Source Layout](#source-layout)
- [Citation](#citation)

---

## How It Works

LMT mounts a Kinect v2 depth sensor above a home cage containing up to 4 mice. Each mouse is implanted with a subcutaneous RFID tag. At 30 frames per second, the system:

1. Captures synchronized **depth** and **infrared** images from the Kinect
2. Subtracts a learned **background height map** of the empty cage to detect objects above the floor
3. Segments individual mice from merged blobs when animals are in contact, using a **Z-priority flood fill** that exploits 3D depth data
4. Extends temporal **tracks** by associating detections across frames using nearest-neighbor matching
5. Resolves **animal identity** through RFID tag reads (ground truth) and machine learning classification (appearance-based, using infrared/depth histograms)
6. Extracts **behavioral features** per frame: head/tail position, rearing, speed, posture, and social interactions between animals
7. Records everything to **SQLite** databases and **MP4 video** for offline analysis

The system runs indefinitely in **streaming mode**, periodically flushing old data from memory to disk.

---

## Hardware Requirements

| Hardware | Interface | Purpose |
|----------|-----------|---------|
| **Microsoft Kinect v2** | USB 3.0 | Depth (512x424 px, 16-bit) + Active IR (512x424 px) at 30 fps. Requires 64-bit Windows and `ufdw_j4k2_64bit.dll` native library. |
| **RFID Antennas** (uRFID_USB) | Serial (COM port, 9600 baud) | Subcutaneous RFID tag reads for animal identification. Multiple antennas positioned around/below the cage. Only one antenna active at a time (round-robin scheduling). |
| **Avisoft-RECORDER** | UDP (localhost:8550) | Ultrasonic vocalization (USV) recording synchronization. Sends start/end triggers with WAV filenames. |
| **Arduino** | Serial (1M baud) | TTL synchronization pulses for external equipment: experiment start/stop, per-frame sync, and behavioral event triggers. |
| **Environmental sensors** | (via Arduino/SensorMonitor) | Temperature, humidity, sound level, visible and IR light — logged per frame. |

**Operating system**: Windows 64-bit (required by Kinect native DLLs).

---

## Tracking Pipeline

### 1. Background Model

The `BackgroundHeightMapBuilder` maintains a per-pixel **maximum depth** map that converges to the empty cage floor over the first N frames.

- Each incoming depth frame updates the background by taking the per-pixel maximum (the floor is the farthest surface).
- After initialization, the background is frozen and used for subtraction.
- **Self-correction**: small spurious detections (noise, transient objects) are fed back into the background model, patching those pixels to the current depth. This prevents ghost detections from accumulating.

Output: a height-above-floor image where positive values indicate objects above the cage floor.

### 2. Foreground Detection

The `MouseDetector` processes each frame:

1. Compute per-pixel height: `background[x,y] - depth[x,y]`
2. Threshold by `DEPTH_SENSITIVITY` (in mm) to produce a binary foreground mask
3. Remove invalid pixels: saturated infrared (`Short.MIN_VALUE`) and invalid depth (`10000`)
4. Clip to the cage ROI
5. Extract connected components via `BooleanMask2D.getComponents()`
6. Filter by size:
   - **Too small** (< `MIN_SIZE_SEG_OK`): classified as noise, used to correct the background
   - **Too large** (> `MAX_SIZE_OF_CANDIDATE_DETECTION`): multiple mice merged together — sent to the contact splitter
   - **Valid**: wrapped into a `MouseDetection` object

Each `MouseDetection` captures a rich feature set:
- **Shape**: pixel mask (`ROI2DArea`), ellipse-fit axes (angle, major/minor axis), surface area, volume
- **3D position**: mass center (x, y, z), front point (head), back point (tail), spine Z-profile
- **Appearance**: infrared intensity histogram, depth histogram, cropped infrared patch
- **Anatomy**: ear positions, nose position (detected via infrared brightness for dark-furred mice, gradient edges for white mice)
- **Behavior**: rearing flag, looking-up/down flags

### 3. Contact Splitting

When two or more mice are so close that their depth blobs merge into a single detection, the `DetectionSplitter3Optimized` separates them:

1. Find all tracks from the previous frame whose last detection intersects the merged blob
2. If only 1 track matches, accept the blob as a single animal
3. If 2+ tracks match, use each previous detection's **spine points** (central axis) as seeds inside the merged region
4. The `DetectionSplitter3Core` performs a **Z-priority flood fill**:
   - A per-pixel ownership map starts with seed pixels assigned to their respective animals
   - The algorithm iterates from maximum Z (tallest) downward
   - At each Z level, each animal's region expands into neighboring unassigned pixels using 8-connectivity, but only into pixels whose height meets the Z threshold
   - Taller body parts are claimed first, naturally splitting along the "valley" between touching animals
   - Target surface area constraints ensure no animal claims more than its fair share
   - 40 additional unconstrained passes fill any remaining unassigned pixels
5. Each resulting sub-region becomes a new `MouseDetection` marked as `builtByDetectionSplitter = true`

### 4. Track Extension

The `TrackExtender` associates detections across frames using **nearest-neighbor matching**:

1. For each detection at time t, find all existing tracks whose last detection was at t-1
2. Compute Euclidean distance between mass centers
3. Select the closest track within `MAX_DISTANCE_FOR_TRACKING_DIRECT_ASSO_IN_TRACK_PROLONGATOR`
4. If no existing track matches, create a new anonymous `TrackSegment`
5. After a splitter-produced detection, a new track segment is created with identity continuity from the previous one

Tracks exist in two pools managed by `TrackContainer`:
- **AnonymousPool**: tracks not yet assigned to any animal
- **AnimalPool**: tracks assigned to a specific animal via RFID or ML

### 5. Detection Post-Processing

When a detection joins a track (`MouseDetection.postProcess()`), temporal context enables:

1. **Speed computation**: instant velocity vector from mass center displacement
2. **Major axis tracking**: the two endpoints of the ellipse-fit major axis are tracked across frames (swapped to minimize distance), establishing head/tail continuity
3. **Head/tail resolution** using three methods in priority order:
   - **Speed-based**: if all projected speeds are consistent and above threshold, the head is in the direction of motion
   - **ML sub-part classification**: an AdaBoosted RandomForest (Weka) classifies head-half vs. tail-half using infrared/depth histograms of the two halves
   - **Major axis continuity**: fallback using the tracked endpoint closest to the previous head position
4. **Spine profile**: Z-height values interpolated along the front-to-back body axis
5. **Behavioral states**: rearing (small front-back Z difference), looking up/down

### 6. Identity Resolution

The system uses two complementary identity mechanisms. RFID provides ground truth (high confidence, intermittent); machine learning provides continuous probabilistic classification.

#### RFID Identity (highest confidence)

`RFIDManager2` manages serial-port RFID antennas. Since only one antenna can be active at a time (to avoid interference), it uses a **priority scheduling strategy**:

- Each frame, increment `nbFrameSinceLastRFIDReading` for all active animal tracks
- Activate the antenna closest to the animal that has gone the longest without RFID confirmation
- Only activate when a track is within detection range (< 30 pixels of the antenna)

`RFIDSolver2` processes each RFID tag-read event:

1. Find the closest mouse detection to the antenna location (accounting for latency)
2. If ambiguous (multiple detections nearby), discard the event
3. **Match**: RFID matches the animal's assigned tag → confirmed identity, reset frame counter
4. **Mismatch**: RFID belongs to a different known animal → split the track at the conflict point, make conflicting portions anonymous, reassign correctly
5. **New RFID**: assign the tag to the animal or create a new animal
6. RFID events temporarily disable the ML solver to prevent conflicting decisions

#### Machine Learning Identity

When RFID hasn't resolved an anonymous track, `MultiIdentityAgentManager` launches background `Identifier` threads:

1. Build a training set from all identified animals' recent detections, using features:
   - Infrared intensity histogram (binned into N bins)
   - Depth histogram (same bin count)
   - Surface area, volume, mean depth
   - Mean/min/max infrared intensity
2. Train an **AdaBoostM1 + RandomForest** classifier (Weka) per animal subset
3. Classify each detection in the anonymous track
4. Average probability distributions across all detections
5. Solve a **global assignment problem**: find the optimal one-to-one mapping of anonymous tracks to animals, considering all overlapping tracks simultaneously
6. Commit the assignment only if confidence exceeds a configurable threshold

Classifiers are **cached** per animal subset and evicted after 2 minutes to avoid re-training every frame.

#### Diadic Black & White Mode

For 2-animal experiments with contrasting coat colors and no RFID, the system compares mean infrared intensity of the two tracks — darker fur reflects less IR.

---

## Ultrasonic Vocalization Analysis

The `plugins.fab.aaa.voc` package provides a complete USV analysis pipeline:

### Audio Processing Pipeline

1. **Load WAV file** (multi-channel, typically 166 kHz sample rate from Avisoft hardware)
2. **FFT**: 1024-point FFT with 75% overlap → time-frequency spectrogram
3. **Noise cancellation**: `NoiseCanceler` removes background noise from the spectrogram
4. **Vocalization detection**: `FrequencyCancelerAndSTD` identifies spectral regions exceeding a detection threshold (default 0.1, 0.05 for pup vocalizations)
5. **Segmentation**: detected regions are fused if gaps < 40 ms, forming discrete `Voc` objects
6. **Classification**: `VocalizationClassifier` tags each USV with descriptive categories:
   - **Short**: duration < 5 ms
   - **Upward**: frequency sweep > 6500 Hz upward
   - **Downward**: frequency sweep > 6500 Hz downward
   - **Modulated**: multiple frequency crossings around the principal axis
   - **Jump**: sudden discontinuous frequency shift
   - **Harmonics**: harmonic overtones detected
7. **Output**: spectrogram images with overlays, HTML reports, CSV/TXT data files

Long recordings are automatically split into 50-second chunks for processing.

### Audio Triangulation (Experimental)

`TriangulationThread` uses multi-channel microphone arrays to localize vocalization sources:

1. Detect a vocalization on the primary channel
2. Search secondary channels for matching zero-crossings within a time window based on speed of sound (340 m/s)
3. Compute inter-channel time delay → distance offset in centimeters
4. Match localized vocalizations to tracked mouse positions to assign USVs to individual animals

### USV Synchronization During Recording

`AviSoftEventReceiver` listens on UDP port 8550 for trigger events from Avisoft-RECORDER software. When a USV recording starts and stops, LMT records the frame boundaries and WAV filename as an event in the SQLite database.

---

## Behavioral Event Detection

LMT computes **35 behavioral events** in real-time, organized into five categories as defined in the publication:

### Individual Behavior

| Behavior | Detection Method |
|----------|-----------------|
| **Speed / Moving** | Mass center displacement between frames |
| **Stop** | Speed below threshold |
| **Rearing** | Front-back Z difference below threshold (animal is upright) |
| **Head Down** | Head posture angle below threshold |
| **Stretched Attend Posture (SAP)** | Extended body posture during exploration |
| **Huddling** | Body circularity > 0.75 (moment analysis) |
| **Head Detected** | Whether head orientation could be resolved |

### Social Dyadic Events

| Behavior | Detection Method |
|----------|-----------------|
| **Contact** | Detection masks touching (distance < 2 pixels) |
| **Side-by-Side (same way)** | Animals aligned laterally, facing same direction |
| **Side-by-Side Opposite** | Side-by-side facing opposite directions |
| **Nose-to-Nose (Oral-Oral)** | Face-to-face proximity detection |
| **Nose-to-Anogenital (Oral-Genital)** | Oral-genital proximity detection |
| **Make Contact** | Transition into contact |
| **Break Contact** | Transition out of contact |
| **Distance** | Continuous inter-animal distance |

### Dynamic Events

| Behavior | Detection Method |
|----------|-----------------|
| **Approach** | Animal A is faster than B and getting closer |
| **Escape** | B is moving away from approaching A |
| **Follow / Train2** | Both moving, A is behind B and in contact |

### Configuration Events

| Behavior | Detection Method |
|----------|-----------------|
| **Group of 3** | Three animals in proximity |
| **Group of 4** | All four animals in proximity |
| **Train3** | Linear arrangement of three animals |
| **Nest (3+ mice)** | Animals grouped in nest area |

### Group Making/Breaking Events

| Behavior | Detection Method |
|----------|-----------------|
| **Make Group 3** | A third animal joins a pair |
| **Break Group 3** | An animal leaves a group of three |
| **Make Group 4** | A fourth animal joins a group of three |
| **Break Group 4** | An animal leaves a group of four |

### Live Streaming

A TCP socket server (`LiveAnalysisServer`, port 7101) streams tracking data to external clients in real-time using XML serialization (JAXB), enabling integration with external analysis tools. The system also provides a UDP network stream for low-latency third-party device integration (e.g., Arduino-based closed-loop systems, optogenetics triggers).

---

## Validated Performance

As reported in the publication, manual validation by two independent experts over 10-minute experiments (18,000 frames each) with 1–4 mice yielded:

| Metric | 1 mouse | 2 mice | 3 mice | 4 mice |
|--------|---------|--------|--------|--------|
| **Detection rate** | ≥ 99.25% | ≥ 99.25% | ≥ 99.25% | ≥ 99.25% |
| **Segmentation accuracy** | > 98% | > 97% | > 96% | > 95.75% |
| **Orientation accuracy** | > 99.5% | > 99.5% | > 99.4% | > 99.36% |
| **Identity error rate** | — | < 1% | < 2% | < 2.69% |
| **MOTA score** | 0.993 | 0.991 | 0.984 | 0.970 |

Identity switching episodes have a mean duration of 1.64 s. The RFID system continuously validates and corrects identities, preventing error propagation.

---

## Data Output

### SQLite Database

Each experiment produces a `.sqlite` file — the canonical data format shared between LMT and all downstream analysis tools. The schema has 6 core tables:

| Table | Contents |
|-------|----------|
| **ANIMAL** | Registered animals: `ID`, `RFID`, `NAME`, `GENOTYPE`. Analysis tools may add `AGE`, `SEX`, `STRAIN`, `SETUP`, `TREATMENT` columns via `ALTER TABLE`. Schema is variable (3–9 columns); tools handle this adaptively. |
| **DETECTION** | Per-frame detections: `FRAMENUMBER`, `ANIMALID`, 3D mass center (`MASS_X/Y/Z`), head point (`FRONT_X/Y/Z`), tail point (`BACK_X/Y/Z`), `REARING`, `LOOK_UP`, `LOOK_DOWN`, and `DATA` (compressed XML binary mask blob). `ANIMALID` can be `NULL` for anonymous/occluded detections. |
| **FRAME** | Per-frame metadata: `FRAMENUMBER`, `TIMESTAMP` (epoch ms), `NUMPARTICLE`, `PAUSED`, `TEMPERATURE`, `HUMIDITY`, `SOUND`, `LIGHTVISIBLE`, `LIGHTVISIBLEANDIR` |
| **EVENT** | Behavioral events: `ID`, `NAME` (string event type), `DESCRIPTION`, `STARTFRAME`, `ENDFRAME`, `IDANIMALA/B/C/D` (up to 4 animals, nullable), `METADATA` (JSON, added dynamically). Single-animal events use only `IDANIMALA`; pair events use A+B; group events use A+B+C or A+B+C+D. |
| **RFIDEVENT** | Raw RFID tag reads: `ID`, `RFID`, `TIME`, antenna position `X/Y` |
| **LOG** | Processing log entries: `version`, `process`, `date`, `tmin`, `tmax` |

#### Standard Database Indexes

Created by all analysis tools via `BuildDataBaseIndex.py`:

- `detectionIndex` on `DETECTION(ID, FRAMENUMBER)`
- `detectionFastLoadXYIndex` on `DETECTION(ANIMALID, FRAMENUMBER, MASS_X, MASS_Y)`
- `eventIndex` on `EVENT(ID, STARTFRAME, ENDFRAME)`
- `eventStartFrameIndex` on `EVENT(STARTFRAME)`
- `eventEndFrameIndex` on `EVENT(ENDFRAME)`

#### Measurement Constants

All analysis tools share these physical constants (from `ParametersMouse.py`):

| Parameter | Value | Purpose |
|-----------|-------|---------|
| Frame rate | **30 fps** | `oneSecond=30`, `oneMinute=1800`, `oneHour=108000`, `oneDay=2592000` frames |
| Pixel-to-cm | **10/57** | Converts pixel coordinates to centimeters (50×50 cm arena) |
| Arena size | **50 cm** | Standard cage dimensions |
| Contact distance | **8/scaleFactor ≈ 45.6 px** | Mass center distance threshold for "contact" |
| Head-head/genital | **15 px** | Nose-to-nose or nose-to-anogenital distance threshold |
| Speed low threshold | **5 cm/s** | Below this = stopped / SAP |
| Speed high threshold | **10 cm/s** | Above this = fast movement (Train2 requires both animals > this) |
| Body slope threshold | **40** | Z-axis slope (frontZ−backZ) for rearing detection |
| Follow corridor | **2.5/scaleFactor px wide**, **24/scaleFactor px long** | Corridor dimensions for follow behavior |
| Follow max angle | **π/4 (45°)** | Maximum heading angle difference for following |
| Follow speed ratio | **2×** | Follower must be ≥ 2× faster than followed animal |
| Center margin | **7.32 cm** | Center zone boundary (chosen for equal center/periphery area) |
| Vibrissae | **3 cm** | Vibrissae length used for nose-proximity thresholds |

### Video Recording

- **MP4 timelapse**: infrared view recorded at configurable frame rates (default: every 2nd frame = ~15 fps). Split into 10-minute segments. Optionally includes overlay graphics (track lines, animal names, sensor data).
- **Per-animal thumbnails**: circular cropped views of each animal, rotated nose-up, with posture indicators.

### Raw Data

- Infrared frames saved as numbered PNG images
- Background height maps saved periodically (default: every 1800 frames = 1 minute)
- Environmental sensor readings per frame

---

## Post-Processing

The `PostProcessDataBase` ICY plugin batch-processes one or more `.sqlite` databases:

1. **Recompute events**: delete all events and recompute from raw detection data
2. **Huddling detection**: multi-threaded computation across 2-minute windows
3. **Nest detection**: for experiments with 3+ mice
4. **Event merging**: combine fragmented events that were split by the streaming save boundary (every 500 frames)
5. **Deduplication**: iteratively remove duplicate EVENT rows
6. **Vacuum**: reclaim SQLite disk space

---

## User Interface

The main GUI panel (`LiveMouseTrackerPanel`) provides 5 tabs:

### Experiment Tab
- Quick-select buttons for 1, 2, 3, or 4 animals
- Experiment folder and name configuration
- **Start Live** / **Pause** / **Stop** controls

### Save Tab
- SQLite streaming toggle (on by default)
- Background height map save interval
- MP4 recording with/without overlays, frame skip setting

### Advanced Options
- Multi-arena mode (multiple cages)
- Wired animals (tethered cables — rejects cable artifacts)
- Black-and-white dyadic mode (no RFID needed)
- Developer tuning parameters

### TTL Tab
- Arduino TTL synchronization enable/disable
- External event trigger management

### Antenna Setup Tab
- RFID antenna serial number read/write
- Antenna discovery and COM port pairing
- Serial number reset

### Display Controls

Keyboard shortcuts on the tracking overlay:

| Key | Action |
|-----|--------|
| `d` | Cycle display mode (5 modes: HUD/RFID/full detection combinations) |
| `*` | Lock/unlock background height map |
| `r` | Reset background and antennas |
| `+` / `-` | Cycle debug overlays |

The overlay renders:
- Colored track paths per animal (with configurable time window)
- Animal name + frames since last RFID reading
- Zoomed circular thumbnails per animal (rotated nose-up)
- Posture indicators: Rearing, Look Up, Look Down
- ML learning status and head classifier instance count
- Z-spine depth profile per animal
- Environmental sensor readings (temperature, humidity, sound, light)

---

## Building and Installation

### Prerequisites
- **Eclipse IDE** (the project has no Maven/Gradle/Ant build)
- **Java 1.8** (source and target compliance)
- **Icy** (installed and configured as an Eclipse project or available as JARs)

### Build

1. Import the project into Eclipse: the Eclipse project name is `LMT 2022`
2. Ensure the Icy kernel is on the classpath (the `.classpath` references ICY libraries)
3. Build automatically via Eclipse, or use the JAR export descriptor at `LiveMouseTracker/export.jardesc`

### Deploy

1. Export the plugin as a JAR (via Eclipse export or `export.jardesc`)
2. Place the JAR in Icy's `plugins/` directory
3. Launch Icy — the plugin is auto-discovered by scanning classes in the `plugins.*` package that extend `PluginActionable`

### ICY Kernel

The Icy kernel source is available at [gitlab.pasteur.fr/bia/icy/icy](https://gitlab.pasteur.fr/bia/icy/icy) and can be built independently with Maven:

```bash
git clone https://gitlab.pasteur.fr/bia/icy/icy.git
cd icy
mvn clean install
```

This produces `icy/build/icy.jar` and dependencies. The kernel is **not required to build from source** if you already have Icy installed.

---

## Source Layout

```
LiveMouseTracker/
├── LiveMouseTracker/                       # Eclipse sub-project (the LMT plugin)
│   ├── src/
│   │   ├── plugins/fab/
│   │   │   ├── livemousetracker/           # Main plugin (~306 Java files)
│   │   │   │   ├── LiveMouseTracker.java   # Central hub class (~5500 lines)
│   │   │   │   ├── detection/              # MouseDetector, MouseDetection
│   │   │   │   ├── splitter/               # DetectionSplitter (Z-priority flood fill)
│   │   │   │   ├── track/                  # TrackSegment, TrackContainer, TrackExtender, pools
│   │   │   │   ├── rfid/                   # RFIDManager, RFIDSolver, RFIDAntenna
│   │   │   │   ├── identity/               # MultiIdentityAgentManager, ML classifiers
│   │   │   │   ├── machinelearning/        # Weka-based identity and head/tail classification
│   │   │   │   ├── morpho/                 # Morphological ROI operations
│   │   │   │   ├── overlay/                # Track visualization overlay
│   │   │   │   ├── experiment/             # SQLite persistence, Experiment, EventLog
│   │   │   │   ├── device/                 # Arduino TTL, AviSoft USV, sensors
│   │   │   │   ├── MPEGRecorder/           # MP4 video recording
│   │   │   │   ├── calibration/            # Kinect calibration tool
│   │   │   │   ├── liveanalysis/           # Real-time behavioral event detection
│   │   │   │   ├── postprocessdatabase/    # Batch SQLite post-processing
│   │   │   │   └── ...
│   │   │   ├── kinectdriver/               # Kinect v2 hardware driver
│   │   │   └── aaa/voc/                    # USV analysis pipeline (~55 files)
│   │   └── jssc/                           # Bundled Java Simple Serial Connector
│   ├── lib/win64/                          # Windows native DLLs
│   ├── bin/                                # Eclipse output
│   └── *.jar                               # Vendored dependencies (~30 JARs)
│
├── .classpath                              # Eclipse classpath (references all vendored JARs)
├── .project                                # Eclipse project ("LMT 2022")
├── LICENSE                                 # GPL v3
└── README.md
```

---

## Analysis Ecosystem

Several open-source tools exist for post-processing and statistical analysis of LMT data. All read the same `.sqlite` database format described above. These are **separate projects** hosted on GitHub, not bundled in this repository.

### lmt-analysis — [GitHub](https://github.com/fdechaumont/lmt-analysis)

The core Python analysis library underlying most other tools. Provides programmatic access to LMT databases:

- **Classes**: `Animal`, `AnimalPool`, `Detection`, `EventTimeLine`, `Mask` — load/query/plot tracking data
- **Event builders**: ~30 `BuildEvent*` modules that compute behavioral events from raw detections using spatial/distance/angle criteria
- **Visualizations**: 2D/3D trajectory plots, heatmaps, event timelines, duration histograms, sensor data plots
- **Animal masks**: decompress and render the `DATA` blob (zlib-compressed XML) as binary silhouette masks
- **Species support**: parameter sets for both mice and rats (`ParametersMouse` / `ParametersRat`)
- **Novel Object Recognition**: dedicated scripts for NOR test analysis
- Install: clone from [GitHub](https://github.com/fdechaumont/lmt-analysis) and add `LMT/` directory to Python path; dependencies: numpy, scipy, matplotlib, pandas, networkx, seaborn, statsmodels
- Tutorial: [Google Docs tutorial](https://docs.google.com/presentation/d/1wR7JM2vq5ZjugrwDe4YuuKJm0MWIvkHAvrOH7mQNOEk/edit?usp=sharing)

### LMT-Easy — [GitHub](https://github.com/haribo015/LMT-Easy)

A desktop GUI for LMT analysis requiring no coding, built on `lmtanalysis`:

- **7 tabs**: Database Info, Rebuild Events, Merge Databases, Plot Timeline, Plot Trajectory, Plot Sensors, Time Calculator
- **Event rebuilding**: recomputes all 26+ behavioral event types from raw detections in 1-day windows
- **Database merging**: combines multiple experiment SQLite files with RFID-based animal deduplication
- **Timeline plots**: 8 plot types including event timelines, interaction matrices, behavioral profiles, duration/count histograms
- **Trajectory plots**: raw paths, speed-filtered paths, heatmaps (200-bin 2D density with PowerNorm), chronobiology plots with night shading
- **Sensor plots**: temperature, humidity, sound, visible light, IR+visible light time series
- **Reliability reports**: detection rates, RFID match/mismatch counts, frame omissions, per-animal statistics
- **Data export**: graph data to Excel (.xlsx), statistics to text files
- Build standalone executable: `pyinstaller LMTAnalysisInterface.spec`

### LMT Widget Tool — LWTools — [GitHub](https://github.com/PaulCarrascosa/LMT_Widget_Tool-LWT)

An interactive Jupyter-based analysis tool with statistical testing:

- **Pipeline**: Change Genotypes → Build Night Events → Split Multi-Night DBs → Rebuild Events + Export CSV → Merge CSVs → LMT-Indexer → Interactive Analysis
- **LMT-Index**: normalizes each animal's behavior against a reference genotype within the same cage (computes `LMT_Index_ED` for event duration and `LMT_Index_NOE` for event count)
- **Statistics**: Linear Mixed Models (`statsmodels.mixedlm`), Repeated-Measures ANOVA, DABest estimation statistics (Gardner-Altman/Cummings plots)
- **Night phase segmentation**: user-specified dark/light cycle times; splits multi-night databases into separate per-night files
- **CSV export columns**: Date, Cage, Injection, Night-Phase, Bin, start/stop frames, animal IDs/RFIDs/genotypes, totalLength, meanLength, medianLength, numberOfEvents, stdLength, CI95_low/up
- **Filename convention**: expects `{Date}_{Experiment}_{Cage}_{Injection}.sqlite`
- Install: `pip install LWTools` (requires Python 3.10)

### MouseKing — [GitHub](https://github.com/DaleAnnear/MouseKing)

A reproducible, containerized pipeline for high-throughput multi-cage analysis:

- **8 CLI commands**: `integrity`, `rebuild`, `extract`, `processing`, `uni`, `multi`, `royale` (full pipeline), `install`
- **Univariate statistics**: Wilcoxon rank-sum test with Benjamini-Hochberg correction; stacked bar charts by behavioral domain
- **Multivariate statistics**: PCA (z-score normalized per cage), MANOVA on top 5 PCs, ANOVA per PC with Bonferroni correction, pairwise Cohen's d effect sizes
- **Behavioral taxonomy**: classifies events into 5 domains — Spatial Positioning, Motor Behavior, Physical Social Contact, Initiation & Approach, Grouping & Withdrawal
- **Input**: SQLite files + TSV manifest (`RFID`, `Condition`, `Cage` columns) + optional time file for treatment phases
- **Output**: tables/ (raw CSVs), processed/ (filtered/aggregated), univariate/ (Wilcoxon + plots), multivariate/ (PCA + effect sizes)
- **Extra events**: computes `Other contact`, `Move high speed`, `Long chase`, `Flickering` (tracking artifacts), not available in other tools
- Requires: Linux, Docker, Nextflow

### LMT Toolkit Analysis — [GitHub](https://github.com/ntorquet/lmt_toolkit_analysis)

A full-stack web application (Django + Nuxt.js) for browser-based analysis:

- **Architecture**: Django REST API + Celery workers + RabbitMQ + Vue.js frontend; deploy via Docker Compose
- **Quality control**: automatic reliability reports with color-coded thresholds (frame drops, detection rates, temperature warnings, RFID match rates)
- **Animal metadata editing**: web UI for genotype, sex, age, strain, setup, treatment per animal (writes back to SQLite)
- **Analysis presets**: Simple (behavioral profile: duration/count/mean per event per animal), Activity (distance per time bin with night shading)
- **Night period detection**: from sensor light data or user-specified hours
- **Results**: interactive tables with CSV download, activity line plots per time bin
- **Event documentation**: behavioral event descriptions served from database and displayed in the UI
- Install: `docker compose up --build` or native (Django + Celery + RabbitMQ + npm)

### Shared Analysis Concepts

All tools share these conventions inherited from the LMT data format:

- **Frame rate**: fixed at 30 fps; all time constants derived from this (`oneSecond=30`, `oneMinute=1800`, etc.)
- **Event rebuilding**: recomputed from raw `DETECTION` data (not from the Java tracker's live events), processed in 1-day windows to handle multi-day recordings
- **Event metrics**: three standard measures per event type per animal — **TotalLen** (total duration), **Nb** (number of occurrences), **MeanDur** (mean duration per event)
- **Coordinate system**: pixel coordinates in DETECTION; converted to cm via `scaleFactor = 10/57` for a 50×50 cm arena
- **Night detection**: dark phase typically 20:00–08:00, shown as gray shading on plots; configurable per tool

---

## Citation

If you use Live Mouse Tracker in your research, please cite:

de Chaumont, F., Ey, E., Torquet, N., Lagache, T., Dallongeville, S., Imbert, A., Legou, T., Le Sourd, A.-M., Faure, P., Bourgeron, T. & Olivo-Marin, J.-C. Live Mouse Tracker: real-time behavioral analysis of groups of mice. *Nature Biomedical Engineering* (2019). [doi:10.1038/s41551-019-0396-1](https://doi.org/10.1038/s41551-019-0396-1)

Preprint: de Chaumont, F. et al. bioRxiv (2018). [doi:10.1101/345132](https://doi.org/10.1101/345132)

For the ICY platform: de Chaumont, F. et al. Icy: an open bioimage informatics platform for extended reproducible research. *Nature Methods* **9**, 690–696 (2012). [doi:10.1038/nmeth.2075](https://doi.org/10.1038/nmeth.2075)

Please mention the version of LMT you used (shown in the GUI or at the top of the Output tab in Icy).
