# Live Mouse Tracker

**Real-time tracking and behavioral analysis of group-housed mice using depth sensing, RFID, and audio capture.**

Live Mouse Tracker (LMT) tracks multiple mice simultaneously in a home-cage environment. It uses a Microsoft Kinect v2 depth sensor mounted above the cage to detect and segment individual animals in 3D, identifies them via RFID tags and machine learning, and records their behavior for later analysis.

Licensed under [GPL v3](LICENSE).

Developed by Fabrice de Chaumont & Elodie Ey

**Publication**: de Chaumont, F. et al. "Live Mouse Tracker: real-time behavioral analysis of groups of mice." *Nature Biomedical Engineering* (2019). [doi:10.1038/s41551-019-0396-1](https://doi.org/10.1038/s41551-019-0396-1). Preprint: [bioRxiv 345132](https://doi.org/10.1101/345132).

**Data portal**: [micecraft.org/lmt](https://micecraft.org/lmt) — shared datasets, videos, and analysis scripts (R and Python).

---

## Table of Contents

- [How It Works](#how-it-works)
- [Hardware Requirements](#hardware-requirements)
- [Getting Started](#getting-started)
- [Arena Configuration](#arena-configuration)
- [RFID Setup](#rfid-setup)
- [Multi-Setup Room Configuration](#multi-setup-room-configuration)
- [Tracking Pipeline](#tracking-pipeline)
  - [1. Background Model](#1-background-model)
  - [2. Foreground Detection](#2-foreground-detection)
  - [3. Contact Splitting](#3-contact-splitting)
  - [4. Track Extension](#4-track-extension)
  - [5. Detection Post-Processing](#5-detection-post-processing)
  - [6. Identity Resolution](#6-identity-resolution)
- [Ultrasonic Vocalization Analysis](#ultrasonic-vocalization-analysis)
- [Behavioral Event Detection](#behavioral-event-detection)
- [External Event Integration](#external-event-integration)
- [Validated Performance](#validated-performance)
- [Data Output](#data-output)
- [Post-Processing](#post-processing)
- [Troubleshooting](#troubleshooting)
- [Analysis Ecosystem](#analysis-ecosystem)
- [Historical Context and Related Systems](#historical-context-and-related-systems)
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
4. Extends temporal **tracks** by associating detections across frames
5. Resolves **animal identity** through RFID tag reads (ground truth) and machine learning classification (appearance-based, using infrared/depth histograms)
6. Extracts **behavioral features** per frame: head/tail position, rearing, speed, posture, and social interactions between animals
7. Records everything to **SQLite** databases and **MP4 video** for offline analysis

---

## Hardware Requirements

| Hardware | Interface | Purpose |
|----------|-----------|---------|
| **Microsoft Kinect v2** | USB 3.0 | Depth (512x424 px, 16-bit) + Active IR (512x424 px) at 30 fps. Requires 64-bit Windows and `ufdw_j4k2_64bit.dll` native library. |
| **RFID Antennas** (uRFID_USB) | Serial (COM port, 9600 baud) | Subcutaneous RFID tag reads for animal identification. Multiple antennas positioned around/below the cage. Only one antenna active at a time (round-robin scheduling). |

**optionnal:**:

| **Avisoft-RECORDER** | UDP (localhost:8550) | Ultrasonic vocalization (USV) recording synchronization. Sends start/end triggers with WAV filenames. |
| **Arduino** | Serial (1M baud) | TTL synchronization pulses for external equipment: experiment start/stop, per-frame sync, and behavioral event triggers. |
| **Environmental sensors** | (via Arduino/SensorMonitor) | Temperature, humidity, sound level, visible and IR light — logged per frame. |

**Operating system**: Windows 64-bit (required by Kinect native DLLs).

**Kinect verification**: after installing the Kinect SDK, launch the Kinect Configuration Verifier tool and confirm a stable 30 fps output. Any non-constant or lower value corrupts tracking data.

---

## Getting Started

### Quick Start

1. **Download** the latest LMT distribution from [micecraft.org/lmt](https://micecraft.org/lmt)
2. **Unzip** the archive
3. **Install Java 8** (64-bit) — required
4. **Configure antenna COM ports** (see [RFID Setup](#rfid-setup) below)
5. **Launch** by double-clicking the `.bat` file — see critical note below
6. A **sample dataset** is available from [micecraft.org/lmt](https://micecraft.org/lmt) to test the analysis pipeline before investing in hardware

### Launching LMT

**You must launch LMT using the provided `.bat` file**, not by running `icy.exe` directly. The bat file:

1. Copies native DLLs (`jssc.dll`, `ufdw_j4k2_64bit.dll`) to `lib/win64/`
2. Launches ICY with JVM parameters tuned for real-time operation:
   - **6 GB heap** (`-Xmx6G`)
   - **CMS garbage collector** optimized for low-latency (`-XX:+UseConcMarkSweepGC`, `CMSInitiatingOccupancyFraction=60`)
   - **DirectDraw disabled** (`-Dsun.java2d.noddraw=true`) for rendering stability
3. Starts the `LMTLauncher` bootstrap plugin, which sets a `launchOK` flag that `LiveMouseTracker` validates before proceeding

Launching via `icy.exe` causes time drift (red flashes in the recording window, >1 second drift per 3 hours of recording).

### DIY Assembly

The hardware is assembled from off-the-shelf parts. An IKEA-style PDF with a complete **shopping list and assembly instructions** is available from:
- [micecraft.org/lmt](https://micecraft.org/lmt) (Downloads → Blueprints)
- [livemousetracker.org](https://livemousetracker.org/) (Shopping list and assembly instructions)

### Kinect Calibration

1. Launch the `LiveMouseTrackerCalibration` plugin from the Live Mouse Tracker tab
2. The view shows red, orange, and green dots — **all green means calibrated**
3. Maximize the frame and zoom in/out with the mouse wheel
4. Camera height is approximately 63 cm, but calibrate via the software, **not with a ruler**

### Tracking Initialization

After launching the main `Live Mouse Tracker` plugin and selecting experiment parameters, the system initializes tracking within approximately **10 seconds** of observation. Green antenna circles indicate successful RFID initialization.

### Additional Resources

- **Analysis tutorial**: [Google Slides presentation](https://docs.google.com/presentation/d/1wR7JM2vq5ZjugrwDe4YuuKJm0MWIvkHAvrOH7mQNOEk) — "Data Analysis in Python for Biologists" by Fabrice de Chaumont & Elodie Ey
- **Jupyter examples**: [lmt-analysis/examples/](https://github.com/fdechaumont/lmt-analysis/tree/master/LMT/examples) — trajectory drawing, position extraction, event timelines
- **Rebuild events notebook**: [lmt-analysis/scripts/](https://github.com/fdechaumont/lmt-analysis/tree/master/LMT/scripts) — rebuild all behavioral events from raw detection data
- **Environmental sensor kit**: DIY kit downloadable from [livemousetracker.org](https://livemousetracker.org/)
- **New-user checklist**: available from [livemousetracker.org](https://livemousetracker.org/)
- **Community support**: Discord (link via [micecraft.org/lmt](https://micecraft.org/lmt)), email: fabrice.de.chaumont@gmail.com / eye@igbmc.fr

---

## Arena Configuration

LMT uses an XML configuration file (`lmt-config.xml`) placed in the ICY folder to define the arena geometry, antenna positions, and detection parameters. To switch configurations, rename the desired config file to `lmt-config.xml` and place it in the ICY root folder.

### Config File Format

```xml
<root>
  <cagefloor>
    <polygon wallsize="36">  <!-- wall thickness in pixels; use 2-3 for wall-less arenas -->
      <point x="114" y="63"></point>
      <point x="398" y="63"></point>
      <point x="398" y="353"></point>
      <point x="114" y="353"></point>
    </polygon>
  </cagefloor>

  <antenna x="131.5" y="80.5" ray="35" com="COM30"></antenna>
  <!-- ... more antennas ... -->

  <contrast min="0" max="35200"/>
  <parameters depthSensitivity="14" maxDetectionSize="1000" minDetectionSize="100"
              detectionSplitTargetVolume="31000" maxObservableDepth="3000"/>
</root>
```

### Detection Parameters

| Parameter | Mouse Default | Rat | EPM | Purpose |
|-----------|--------------|-----|-----|---------|
| `depthSensitivity` | 14 | 8 | 8 | Height threshold in mm for foreground detection |
| `maxDetectionSize` | 1000 | 300 | 1000 | Maximum pixel count for a single-mouse detection |
| `minDetectionSize` | 100 | 20 | 100 | Minimum pixel count; smaller = noise |
| `detectionSplitTargetVolume` | 31000 | 1000 | 31000 | Target surface area for contact splitter per animal |
| `maxObservableDepth` | 3000 | 3000 | 3000 | Maximum depth value in mm |
| `contrast min/max` | 0 / 35200 | 0 / 10000 | 0 / 10600 | Depth display contrast range |

### Provided Arena Configurations

| Config | Description | Antennas | COM Ports | Notes |
|--------|-------------|----------|-----------|-------|
| **Original 50x50 (default)** | Standard mouse cage as in publication | 4×4 grid = 16 | COM30–45 | `wallsize=36`, `ray=35` |
| **Block 50x50 with walls** | Block-style antenna layout, high walls | 16 (block layout) | COM30–45 | `wallsize=36` |
| **Block 50x50 without walls** | Block-style antenna layout, minimal walls | 16 (block layout) | COM30–45 | `wallsize=3` |
| **Rat floor** | 100×100 cm arena for rats | 5×5 grid = 25 | COM50–74 | `ray=25`, lower depth sensitivity |
| **EPM** | Elevated Plus Maze | 1 (center) | COM100 | Cross-shaped floor from 2 rectangles, `wallsize=2` |

### Rat Mode

Rat mode uses `<ratMode/>` in the config XML and adjusts parameters for larger animals in a 100×100 cm arena. The pixel-to-cm conversion factor changes to **20/57** (vs. 10/57 for mice). Antenna detection radius is reduced to `ray=25` with 25 antennas in a 5×5 grid.

---

## RFID Setup

### Recommended RFID Tags

**Biomark APT12 PIT tag (FDX)** — Available from [biomark.com](https://www.biomark.com).

### Antenna Tuning

1. Launch the **AntennaTuner** plugin from the Live Mouse Tracker tab
2. The plugin auto-detects all COM ports on plug/unplug and displays current tuning refreshed every second
3. **Target frequency**: 134.2 kHz
4. Use **A+/A- connectors** on the RFID reader board (not AR/A-)
5. To increase frequency: remove coil length; cut and resolder if >10 cm removed
6. Reading of **0.0 / 134.2 kHz** indicates a broken wire or solder joint
7. To solder: wrap coil around wire, burn the thin translucent insulation plastic while soldering
8. **Reading range** degrades approximately **4.2 mm per kHz deviation** from 134.2 kHz

### Antenna Testing

The **Antenna Tuning Tester** plugin cycles through antennas for 5 seconds each, performing 10 reads per second, and displays detected RFID numbers in the output.

### Antenna COM Port Numbering

Assign sequential COM port numbers (e.g., 30, 31, 32...) via Windows Device Manager → port properties → Advanced. The top-left antenna should be COM30, then 31 to its right, and so on.

### RFID Implantation

- Use gas anesthesia and local subcutaneous analgesia
- Insert the RFID in the **neck** and gently push to the **side** of the animal
- We recommend **to avoid reuse** RFID chips — they carry factory-assigned unique numbers; reuse creates duplicate animals and corrupts multi-experiment analysis. Still if you have completly independent experiments sets this is possible.

---

## Multi-Setup Room Configuration

More and more labs run 4 or more parallel setups in the same room. To avoid interference:

**Kinect IR cross-talk**: Kinects sending direct IR to other setups cause image flickering. Solutions:
- Attach a matte box (homemade is fine) to the front of each Kinect
- Use isolating boxes per setup

**RFID jamming**: LMT ensures only one antenna is active per system. With multiple systems:
- Keep a **minimum 1 meter** between setups
- **Disconnect power** from unused RFID reader hubs — their default behavior is to start reading, which jams the signal up to several meters

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

### USV Analysis Tools

- **Online test tool**: [usv.pasteur.cloud](https://usv.pasteur.cloud) — test USV detection on your own WAV files without any installation. Watch examples, listen to them, and process short samples to evaluate detection quality.
- **LMT USV Toolbox**: [GitHub](https://github.com/fdechaumont/LMT-USV-Toolbox) — Python package for offline batch USV analysis. Features:
  - Synchronization pipeline: `LMT.USV.importer` module imports and synchronizes WAV files with LMT tracking data
  - WAV files must be at **300 kHz** sampling rate (converter script `LMT.USV.convert.convertTo300kHz.py` included)
  - Requires `librosa` (`pip install librosa`)
  - Figure generation scripts for Frontiers article
- **Standalone desktop app**: available from [livemousetracker.org](https://livemousetracker.org/) for mass processing of thousands of vocalizations

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

## External Event Integration

LMT can record events from external devices or programs via a simple UDP protocol on **localhost:8550** (the same port used for Avisoft USV triggers). To create a custom event:

```
PacketSender.exe -ua localhost 8550 "start_MyEventName"
PacketSender.exe -ua localhost 8550 "end_MyEventName"
```

The event appears in the **EVENT** table of the SQLite database with the specified name, start/end frames, and associated animals. Query it with any SQLite tool (e.g., [DB Browser for SQLite](https://sqlitebrowser.org/)).

You can also send UDP messages programmatically or use [PacketSender](https://packetsender.com/) for testing. LMT also supports TTL communication via Arduino for hardware-level synchronization.

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

## Source Layout

```
LiveMouseTracker/
├── LiveMouseTracker/                       # Eclipse sub-project (the LMT plugin)
│   ├── src/
│   │   ├── plugins/fab/
│   │   │   ├── livemousetracker/           # Main plugin (~306 Java files)
│   │   │   │   ├── LiveMouseTracker.java   # Central hub class (~5500 lines)
│   │   │   │   ├── LMTLauncher.java        # Bootstrap plugin (launch validation)
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
│   │   └── jssc/                           # Bundled Java Simple Serial Connector source
│   ├── lib/win64/                          # Windows native DLLs
│   ├── bin/                                # Eclipse output
│   └── *.jar                               # Vendored dependencies (~30 JARs)
│
├── resources/
│   ├── icy/                                # ICY kernel source (v1.9.10.0, Eclipse project)
│   ├── Live Mouse Tracker - version December 2025 - build 1266/  # Binary distribution
│   └── ...                                 # Analysis tool sources (lmt-analysis, etc.)
│
├── .classpath                              # Eclipse classpath (references all vendored JARs)
├── .project                                # Eclipse project ("LMT 2022")
├── LICENSE                                 # GPL v3
└── README.md
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| **System is missing frames** | Antivirus may freeze the application to take memory snapshots. **Use Windows Defender** (works well). Avoid Kaspersky (kills performance). |
| **RFID not detected at all** | Check USB hub power supply is connected. Use **A+/A-** connectors on RFID reader board (not AR/A-). |
| **RFID stopped working** (was fine before) | USB hub power supply may have failed. Test with another hub. |
| **Time drift / red flashes in recording window** | Launched via `icy.exe` instead of the `.bat` file. Use the `.bat` launcher for proper JVM memory configuration. Expect <1 s drift per 3 hours. |
| **Tracks cut for one frame / image hangs** (Kinect issue) | (1) Enable Kinect Microphone: Control Panel → Sound → Recording → Kinect Microphone → Enable. (2) Windows privacy settings → Allow microphone access. (3) Uninstall RealTech audio drivers (consume >1 GB RAM + full CPU core). (4) Connect Kinect to a **rear USB 3.0 port** (avoid front panel). |
| **Red antenna circles** (RFID not initializing) | Antennas not numbered correctly. Assign COM port numbers sequentially (30, 31, 32...) via Device Manager → port properties → Advanced. |

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
- **Scripts**: 50+ scripts for quality control, identity profiles, dyadic analysis, behavioral sequences, contact matrices, sensor data, CSV export, flickering filtering, RFID fix, night input (automatic, manual, sensor-based), and more
- Install: clone from [GitHub](https://github.com/fdechaumont/lmt-analysis) and add `LMT/` directory to Python path; dependencies: numpy, scipy, matplotlib, pandas, networkx, seaborn, statsmodels
- Tutorial: [Google Docs tutorial](https://docs.google.com/presentation/d/1wR7JM2vq5ZjugrwDe4YuuKJm0MWIvkHAvrOH7mQNOEk)
- Jupyter examples: [examples/](https://github.com/fdechaumont/lmt-analysis/tree/master/LMT/examples)
- Rebuild events: [scripts/Rebuild all events.ipynb](https://github.com/fdechaumont/lmt-analysis/tree/master/LMT/scripts)

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
- By Marie Bossard, Institut de l'Audition, Paris

### LMT Widget Tool — LWTools — [GitHub](https://github.com/PaulCarrascosa/LMT_Widget_Tool-LWT)

An interactive Jupyter-based analysis tool with statistical testing:

- **Pipeline**: Change Genotypes → Build Night Events → Split Multi-Night DBs → Rebuild Events + Export CSV → Merge CSVs → LMT-Indexer → Interactive Analysis
- **LMT-Index**: normalizes each animal's behavior against a reference genotype within the same cage (computes `LMT_Index_ED` for event duration and `LMT_Index_NOE` for event count)
- **Statistics**: Linear Mixed Models (`statsmodels.mixedlm`), Repeated-Measures ANOVA, DABest estimation statistics (Gardner-Altman/Cummings plots)
- **Night phase segmentation**: user-specified dark/light cycle times; splits multi-night databases into separate per-night files
- **CSV export columns**: Date, Cage, Injection, Night-Phase, Bin, start/stop frames, animal IDs/RFIDs/genotypes, totalLength, meanLength, medianLength, numberOfEvents, stdLength, CI95_low/up
- **Filename convention**: expects `{Date}_{Experiment}_{Cage}_{Injection}.sqlite`
- Install: `pip install LWTools` (requires Python 3.10)
- By Damien Huzard & Paul Carrascosa, Institut de Génomique Fonctionnelle (IGF), Montpellier

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
- By Nicolas Torquet, IGBMC, Strasbourg

### LMT USV Toolbox — [GitHub](https://github.com/fdechaumont/LMT-USV-Toolbox)

A Python package for ultrasonic vocalization analysis synchronized with LMT tracking data:

- **Synchronization**: `LMT.USV.importer` module aligns WAV recordings with behavioral data
- **Detection**: USV extraction from WAV files using librosa
- **Conversion**: WAV files must be 300 kHz; converter script included
- **Figure generation**: scripts for publication-quality figures (used in Frontiers article)
- Install: `pip install librosa`; clone from GitHub
- Also available: [usv.pasteur.cloud](https://usv.pasteur.cloud) (online test, no install) and standalone desktop app from livemousetracker.org

### MiceCraft — [micecraft.org](https://micecraft.org/lmt/)

An upcoming modular platform to design custom behavioral and cognitive testing arenas. Successor to LMT-Blocks. Fully compatible with LMT but not required.

### Data Sharing

[livemousetracker.org](https://livemousetracker.org/) allows registered users to post links to their SQLite databases for sharing with the community. The site also hosts validation videos and analysis scripts in R and Python.

### Shared Analysis Concepts

All tools share these conventions inherited from the LMT data format:

- **Frame rate**: fixed at 30 fps; all time constants derived from this (`oneSecond=30`, `oneMinute=1800`, etc.)
- **Event rebuilding**: recomputed from raw `DETECTION` data (not from the Java tracker's live events), processed in 1-day windows to handle multi-day recordings
- **Event metrics**: three standard measures per event type per animal — **TotalLen** (total duration), **Nb** (number of occurrences), **MeanDur** (mean duration per event)
- **Coordinate system**: pixel coordinates in DETECTION; converted to cm via `scaleFactor = 10/57` for a 50×50 cm arena
- **Night detection**: dark phase typically 20:00–08:00, shown as gray shading on plots; configurable per tool

---

**Contact**: fabrice.de.chaumont@gmail.com, eye@igbmc.fr — or join the Discord community via [micecraft.org/lmt](https://micecraft.org/lmt)
