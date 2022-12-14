## Changelog

##### version 1.3 (17/08/2022)
- Manage raster bigger than 2 giga pixels in multi resolution only. All features are not yet implemented, for test only
- GUI : visual saturation in Visibility menu
- GUI : set log scale style for multiviewhed in angular area

##### version 1.2.7 (03/05/2022)
- GUI : Geometries to point doesn't work with lineal geometries
- Correct an error in geopackage saving while layer has several id fields

##### version 1.2.6 (29/04/2022)
- Correct 2 bugs on geopackage IO
- GUI : add entry Geometries to point in Tools menu

##### version 1.2.5 (30/03/2022)
- Get correctly the nodata parameter from DTM coverage at project creation

##### version 1.2.4 (22/09/2021)
- Java 16 compatibility (XML project loading)

##### version 1.2.3 (07/09/2021)
- Update libs

##### version 1.2.2 (16/03/2021)
- land use import raster may be float type but the values must be integer between 0 and 255
- CLI : --landmod add height parameter

##### version 1.2.1
 - UI : Multiviewshed in vector are empty
 - Geopackage format is used by default for vectorial data
 - Bug (IndeOutOfBoundsException) while exporting metrics in shapefile or geopackage when metrics name contains several land codes
 - CLI : --toobject several optimizations
 - CLI : add command --create for creating a new project
 - CLI : add command --addscale for adding new scale data to a project
 - CLI : --viewshed and --viewtan command can be used whith point sampling to calculate several views
 - CLI : --viewtan command exports the land view also in png
 - CLI : --toobject add distance and azimuth to result when no agreg

##### version 1.2 (29/11/2019)
- Multiviewshed can also store the angular height in raster
- CLI : --toobject command may store angular height or angular area (degree option)

##### version 1.1.5 (19/04/2019)
- Metric calculation with point sampling : height can be overidden for each point if the attribute table of the shapefile contains "height" field
- CLI : add --toobject command for point to point visibility
- optimize memory usage for Multiviewshed
- Multiviewshed may store the square degree surface in raster
- Multiviewshed saves automatically the result in project directory
- Menu Metrics - Planimetric: Dest height param was not used in point sampling

##### version 1.1.4 (25/06/2018)
- Solved bug in viewshed/viewtan window position for Windows

##### version 1.1.3 (20/06/2018)
- Bug in tangential view at 0??
- In tangential view, optional viewshed was not rendered correctly

##### version 1.1.2 (05/06/2018)
- Java 9 compatibility
- Bug in tangential view rendering
- Patch metric : contiguity between 359?? and 0?? was not always respected

##### version 1.1.1 (29/05/2018)
- Out of bounds exception when amplitude > 360??
- Multiviewshed : height can be overidden for each point if the attribute table of the shapefile contains "height" field

##### version 1.1 (19/12/2017)
- Metric : add metrics AG, ED, PD, PMS
- Metric : remove IJI metric 
- Metric : for metric calculation, in tangential view, with 360?? amplitude, the contiguity is respected between 359?? and 0?? 

##### version 1.0.2 (24/10/2017)
- CLI : set the locale as defined in GUI

##### version 1.0.1 (21/03/2017)
- Exception at project creation when tif DEM hasn't CRS

##### version 1.0 (22/12/2016)
- Passage en version finale 1.0 !!!

##### version 0.4.6 (28/11/2016)
- Small optimizations of ray calculation

##### version 0.4.5
- Metric : IJI and CONTAG return respectively 0 and 100 when visible classes are less than 3 and 2 in place of NaN value
- Multi viewshed is parallelized
- CLI : add --multiviewshed command

##### version 0.4.4 (25/03/2016)
- Multi-resolution viewshed : exception thrown in some cases at the border
- UI : show vectorial metric result with proportionnal circle

##### version 0.4.3 (22/03/2016)
- UI : french translation
- UI : change direct checkbox to inverse checkbox
- UI Multiviewshed : add Z dest param
- UI Multiviewshed : result format was never used
- Tangential : calculates landuse view only when needed, caused NPE with CUDA

##### version 0.4.2 (10/02/2016)
- CUDA : ray is not stopped with NaN DEM value

##### version 0.4.1 (09/02/2016)
- Save tif files with tfw world file when no crs is supplied

##### version 0.4 (30/01/2016)
- Tangential calculation supports multiscale mode
- Add earth curvature and refraction correction options
- P and C metrics can be calculated with multiscale mode

##### version 0.3.11 (19/01/2016)
- UI : add Z dest option for viewshed metric calculation
- Project creation, in some cases, does not save the dtm in float number

##### version 0.3.10 (14/12/2015)
- Project creation, in some cases, does not save the dtm in float number
- Multi viewshed : add raster output option

##### version 0.3.9 (06/11/2015)
- CLI : add command --landmod for batching landuse changes
- Remove Raster metric from UI
- Remove Project static references from Project and MPILauncher

##### version 0.3.8 (20/10/2015)
- Project data save : force land in byte and DSM in float

##### version 0.3.7 (13/10/2015)
- P Metric (Perimeter) : change the calculation from number of border pixel to the length of the "true" contour
- C Metric (Compacity) : change due to Perimeter metric change => always >= 1
- CLI : code metrics can be grouped ex: IJI[1-2,3-4,5,6]
- SD Metric (Shannon distance) : class ranges are static 0, 10, 100, 1000, 10000, +Infinity
- New metric DL (Depth line)

##### version 0.3.6 (26/06/2015)
- CLI : point sampling did not work with MPI
- .tfw was not correct when sampling > 1

##### version 0.3.5 (23/06/2015)
- Window flickering on Windows OS when viewshed or tangential dialog are visible
- Bug when exporting viewshed polygon in multiscale mode
- Optimize metric calculation when using several distances
- CLI : add distance thresholds for metrics

##### version 0.3.4 (22/06/2015)
- Bug in menu "Add scale" : invert dsm and land use rasters
- CLI : add option -multi dmin=... and -mono
- Stop ray tracing when fall on dtm NaN value
- Optimize viewshed in multiscale
- Generate multiscale is false on border

##### version 0.3.3 (05/06/2015)
- CLI : option -sampling land=... does not work

##### version 0.3.2 (13/05/2015)
- Optimize A and DIST metric calculation
- MultiScale generation : better handling of land use

##### version 0.3.1 (27/02/2015)
- Metric result dialog : remove button now works
- Show the distance intervals in metric name
- Add icon and splash screen

##### version 0.3 (26/02/2015)
WARNING : projects are incompatible with previous versions.

- Multi resolution support (viewshed only)
- Metrics are integrated in the GUI
- Several distance intervals can be used for some metrics (A, S, SL)
- Add metrics : FD (fractal dimension), P (viewshed perimeter), C (viewshed compactness), SD (shannon on distances), SL (skyline length ratio)
- Landuse colors are correclty saved in the project

##### version 0.2.2
CAUTION : older project must be removed, the dtm envelope was shifted by one pixel

- In tangential mode, the view is always centered on the orientation
- CLI : for points sampling, the bounds are overridable by fields in the shapefile (dmin, dmax, zmin, zmax, orien, amp)
- CLI : bug : manage only one global option
- Metrics : add Rast metric which is not really a metric... save the view in one raster for each point (for debugging purpose)

##### version 0.2.1 (29/09/2014)
- Metric : add DIST metric

##### version 0.2 (24/09/2014)
- UI : viewshed is updated for each clic
- CLI : add commands metrics : --viewmetric --tanmetric
- CLI : remove commands --global --globaltan
- CLI : add -sampling option with 3 modes : n for grid sampling, land for land selection and points for external shapefile
- Metrics : SUM SHAN IJI CONTAG

##### version 0.1
- initial version
