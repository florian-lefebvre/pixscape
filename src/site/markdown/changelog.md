## Changelog

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