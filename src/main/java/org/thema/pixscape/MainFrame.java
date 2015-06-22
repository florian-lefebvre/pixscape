/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.SplashScreen;
import java.awt.Toolkit;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.thema.common.Config;
import org.thema.common.JavaLoader;
import org.thema.common.ProgressBar;
import org.thema.common.Util;
import org.thema.common.swing.LoggingDialog;
import org.thema.common.swing.PreferencesDialog;
import org.thema.data.GlobalDataStore;
import org.thema.data.IOImage;
import org.thema.data.feature.DefaultFeature;
import org.thema.data.feature.Feature;
import org.thema.drawshape.image.CoverageShape;
import org.thema.drawshape.image.RasterShape;
import org.thema.drawshape.layer.DefaultGroupLayer;
import org.thema.drawshape.layer.FeatureLayer;
import org.thema.drawshape.layer.RasterLayer;
import org.thema.drawshape.style.FeatureStyle;
import org.thema.drawshape.style.PointStyle;
import org.thema.drawshape.style.RasterStyle;
import org.thema.drawshape.style.table.ColorRamp;
import org.thema.drawshape.style.table.UniqueColorTable;
import org.thema.parallel.ExecutorService;

/**
 *
 * @author gvuidel
 */
public class MainFrame extends javax.swing.JFrame {
    
    private Project project;
    private DefaultGroupLayer rootLayer;
    
    private ViewShedDialog viewshedDlg;
    private ViewTanDialog viewtanDlg;
    
    private final LoggingDialog logFrame;
    
    /**
     * Creates new form MainFrame
     */
    public MainFrame() {
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/org/thema/pixscape/icone64.png")));
        initComponents();
        setLocationRelativeTo(null);
        setTitle("PixScape - " + JavaLoader.getVersion(MainFrame.class));
        mapViewer.putAddLayerButton();
        Config.setProgressBar(mapViewer.getProgressBar());
        logFrame = new LoggingDialog(this);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mapViewer = new org.thema.drawshape.ui.MapViewer();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        newProjectMenuItem = new javax.swing.JMenuItem();
        loadProjectMenuItem = new javax.swing.JMenuItem();
        prefMenuItem = new javax.swing.JMenuItem();
        logMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        dataMenu = new javax.swing.JMenu();
        loadDSMMenuItem = new javax.swing.JMenuItem();
        loadLandUseMenuItem = new javax.swing.JMenuItem();
        msMenu = new javax.swing.JMenu();
        genMSMenuItem = new javax.swing.JMenuItem();
        addScaleMenuItem = new javax.swing.JMenuItem();
        visMenu = new javax.swing.JMenu();
        viewShedMenuItem = new javax.swing.JMenuItem();
        viewTanMenuItem = new javax.swing.JMenuItem();
        multiViewshedMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        optionsMenuItem = new javax.swing.JMenuItem();
        metricMenu = new javax.swing.JMenu();
        viewshedMetricMenuItem = new javax.swing.JMenuItem();
        tanMetricMenuItem = new javax.swing.JMenuItem();
        toolMenu = new javax.swing.JMenu();
        pathOrienMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowActivated(java.awt.event.WindowEvent evt) {
                formWindowActivated(evt);
            }
            public void windowDeactivated(java.awt.event.WindowEvent evt) {
                formWindowDeactivated(evt);
            }
        });

        mapViewer.setTreeLayerVisible(true);

        fileMenu.setText("File");

        newProjectMenuItem.setText("New project");
        newProjectMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newProjectMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(newProjectMenuItem);

        loadProjectMenuItem.setText("Load Project");
        loadProjectMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadProjectMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(loadProjectMenuItem);

        prefMenuItem.setText("Preferences");
        prefMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prefMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(prefMenuItem);

        logMenuItem.setText("Log window");
        logMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(logMenuItem);

        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        dataMenu.setText("Data");

        loadDSMMenuItem.setText("Set DSM");
        loadDSMMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadDSMMenuItemActionPerformed(evt);
            }
        });
        dataMenu.add(loadDSMMenuItem);

        loadLandUseMenuItem.setText("Set land use");
        loadLandUseMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadLandUseMenuItemActionPerformed(evt);
            }
        });
        dataMenu.add(loadLandUseMenuItem);

        msMenu.setText("Multi scale");

        genMSMenuItem.setText("Generate");
        genMSMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                genMSMenuItemActionPerformed(evt);
            }
        });
        msMenu.add(genMSMenuItem);

        addScaleMenuItem.setText("Add scale");
        addScaleMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addScaleMenuItemActionPerformed(evt);
            }
        });
        msMenu.add(addScaleMenuItem);

        dataMenu.add(msMenu);

        menuBar.add(dataMenu);

        visMenu.setText("Visibility");

        viewShedMenuItem.setText("Viewshed...");
        viewShedMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewShedMenuItemActionPerformed(evt);
            }
        });
        visMenu.add(viewShedMenuItem);

        viewTanMenuItem.setText("Tangential...");
        viewTanMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewTanMenuItemActionPerformed(evt);
            }
        });
        visMenu.add(viewTanMenuItem);

        multiViewshedMenuItem.setText("Multi Viewshed");
        multiViewshedMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                multiViewshedMenuItemActionPerformed(evt);
            }
        });
        visMenu.add(multiViewshedMenuItem);
        visMenu.add(jSeparator1);

        optionsMenuItem.setText("Options");
        optionsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optionsMenuItemActionPerformed(evt);
            }
        });
        visMenu.add(optionsMenuItem);

        menuBar.add(visMenu);

        metricMenu.setText("Metric");

        viewshedMetricMenuItem.setText("Planimetric");
        viewshedMetricMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewshedMetricMenuItemActionPerformed(evt);
            }
        });
        metricMenu.add(viewshedMetricMenuItem);

        tanMetricMenuItem.setText("Tangential");
        tanMetricMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tanMetricMenuItemActionPerformed(evt);
            }
        });
        metricMenu.add(tanMetricMenuItem);

        menuBar.add(metricMenu);

        toolMenu.setText("Tools");

        pathOrienMenuItem.setText("Set point attributes");
        pathOrienMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pathOrienMenuItemActionPerformed(evt);
            }
        });
        toolMenu.add(pathOrienMenuItem);

        menuBar.add(toolMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mapViewer, javax.swing.GroupLayout.DEFAULT_SIZE, 692, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mapViewer, javax.swing.GroupLayout.DEFAULT_SIZE, 469, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void loadProjectMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadProjectMenuItemActionPerformed
        File file = Util.getFile(".xml", "Project file");
        if(file == null) {
            return;
        }
        closeProject();
        try {
            project = Project.loadProject(file);
            rootLayer = project.getLayers();
            mapViewer.setRootLayer(rootLayer);
        } catch (IOException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Error while loading project : " + ex.getLocalizedMessage());
        }
    }//GEN-LAST:event_loadProjectMenuItemActionPerformed

    private void viewShedMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewShedMenuItemActionPerformed
        if(viewshedDlg == null) {
            viewshedDlg = new ViewShedDialog(this, project, mapViewer);
            viewshedDlg.setLocation(getX()+getWidth(), getY());
        }
        viewshedDlg.setVisible(true);
        
    }//GEN-LAST:event_viewShedMenuItemActionPerformed

    private void loadLandUseMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadLandUseMenuItemActionPerformed
        File file = Util.getFile(".tif|.asc", "Raster");
        if(file == null) {
            return;
        }
        try {
            GridCoverage2D cov = IOImage.loadCoverage(file);
            project.setLandUse(cov);
            rootLayer.addLayerFirst(new RasterLayer("Land use", new CoverageShape(cov, new RasterStyle(
                    new UniqueColorTable((Map)project.getLandColors())))));
        } catch (Exception ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Error : " + ex.getLocalizedMessage());
        }
    }//GEN-LAST:event_loadLandUseMenuItemActionPerformed

    private void loadDSMMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadDSMMenuItemActionPerformed
        File file = Util.getFile(".tif|.asc", "Raster");
        if(file == null) {
            return;
        }
        try {
            GridCoverage2D cov = IOImage.loadCoverage(file);
            project.setDSM(cov);
            rootLayer.addLayerFirst(new RasterLayer("DSM", new CoverageShape(cov, new RasterStyle(ColorRamp.RAMP_TEMP))));
        } catch (IOException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Error : " + ex.getLocalizedMessage());
        }
    }//GEN-LAST:event_loadDSMMenuItemActionPerformed

    private void viewTanMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewTanMenuItemActionPerformed
        if(viewtanDlg == null) {
            viewtanDlg = new ViewTanDialog(this, project, mapViewer);
            viewtanDlg.setLocation(getX()+getWidth(), getY());
        }
        viewtanDlg.setVisible(true);

    }//GEN-LAST:event_viewTanMenuItemActionPerformed

    private void prefMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prefMenuItemActionPerformed
        PreferencesDialog dlg = new PreferencesDialog(this, true);
        dlg.setProcPanelVisible(true);
        dlg.setVisible(true);
    }//GEN-LAST:event_prefMenuItemActionPerformed

    private void logMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logMenuItemActionPerformed
        logFrame.setVisible(true);
    }//GEN-LAST:event_logMenuItemActionPerformed

    private void multiViewshedMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_multiViewshedMenuItemActionPerformed
        final MultiViewshedDialog dlg = new MultiViewshedDialog(this);
        dlg.setVisible(true);
        if(!dlg.isOk) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                ProgressBar progressBar = Config.getProgressBar("Multi viewshed...");
                try {
                    List<DefaultFeature> viewSheds = new ArrayList<>();
                    List<DefaultFeature> points = GlobalDataStore.getFeatures(dlg.pathFile, dlg.idField, null);
                    progressBar.setMaximum(points.size());
                    progressBar.setProgress(0);
                    for(Feature point : points) {
                        Point p = point.getGeometry().getCentroid();
                        Bounds b = dlg.bounds.updateBounds(point);
                        Geometry view = project.getDefaultComputeView().calcViewShed(
                                new DirectPosition2D(p.getX(), p.getY()), project.getStartZ(), 
                                -1, dlg.direct, b).getPolygon();
                        viewSheds.add(b.createFeatureWithBoundAttr(point.getId(), view));
                        progressBar.incProgress(1);
                    }
                    FeatureLayer l = new FeatureLayer("Multi viewshed", viewSheds, new FeatureStyle(new Color(0, 0, 255, 20), null), project.getCRS());
                    l.setRemovable(true);
                    rootLayer.addLayerFirst(l);
                    l = new FeatureLayer("Points", points, new PointStyle());
                    l.setRemovable(true);
                    rootLayer.addLayerFirst(l);
                } catch (IOException ex) {
                    Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                    JOptionPane.showMessageDialog(MainFrame.this, "An error has occured : " + ex);
                } finally {
                    progressBar.close();
                }
            }
        }).start();
        
    }//GEN-LAST:event_multiViewshedMenuItemActionPerformed

    private void newProjectMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newProjectMenuItemActionPerformed
        NewProjectDialog dlg = new NewProjectDialog(this, true);
        dlg.setVisible(true);
        if(!dlg.isOk) {
            return;
        }
        closeProject();
        
        try {
            GridCoverage2D dtm = IOImage.loadCoverage(dlg.dtm);
            dlg.path.mkdir();
            project = new Project(dlg.name, dlg.path, dtm, dlg.resZ);
            rootLayer = project.getLayers();
            mapViewer.setRootLayer(rootLayer);
        } catch (IOException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(MainFrame.this, "An error has occured while creating project : " + ex);
        }
    }//GEN-LAST:event_newProjectMenuItemActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        closeProject();
        logFrame.dispose();
    }//GEN-LAST:event_formWindowClosed

    private void genMSMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_genMSMenuItemActionPerformed
        double r = project.getDefaultScale().getResolution();
        
        String res = JOptionPane.showInputDialog(this, "Create multi scale database", (int)(r*4) + ", " + (int)(r*16) + ", " + (int)(r*64));
        if(res == null || res.isEmpty()) {
            return;
        }
        NavigableSet<Double> resolutions = new TreeSet<>();
        for(String s : res.split(",")) {
            if(!s.trim().isEmpty()) {
                resolutions.add(Double.parseDouble(s.trim()));
            }
        }
        resolutions = resolutions.tailSet(r, false);
        if(resolutions.isEmpty()) {
            return;
        }
        
        try {
            project.removeScaleData();
            
            Raster dtm = project.getDefaultScale().getDtm();
            Raster dsm = project.getDefaultScale().getDsm();
            Raster land = project.getDefaultScale().getLand();
            for(Double resol : resolutions) {
                int scale = (int)(resol/r);
                WritableRaster dtmSamp = samplingDEM(dtm, scale);
                Envelope2D env = project.getDtmCov().getEnvelope2D();
                env = new Envelope2D(env.getCoordinateReferenceSystem(), 
                        env.x, env.y-(dtmSamp.getHeight()*scale*r-dtm.getHeight()*r), dtmSamp.getWidth() * scale*r, dtmSamp.getHeight() * scale*r);
                GridCoverage2D dtmCov = new GridCoverageFactory().create("", dtmSamp, env);
                ScaleData dataScale = new ScaleData(dtmCov, 
                        land != null ? samplingLanduse(land, scale) : null, 
                        dsm != null ? samplingDEM(dsm, scale) : null, 
                        1);
                project.addScaleData(dataScale);
            }
            
            rootLayer = project.getLayers();
            mapViewer.setRootLayer(rootLayer);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }//GEN-LAST:event_genMSMenuItemActionPerformed

    private void viewshedMetricMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewshedMetricMenuItemActionPerformed
        final ViewMetricDialog dlg = new ViewMetricDialog(this, false, project.getCodes());
        dlg.setVisible(true);
        if(!dlg.isOk) {
            return;
        }
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                ProgressBar progressBar = Config.getProgressBar("Metrics...");
                if(dlg.gridSampling) {
                    GridMetricTask task = new GridMetricTask(project.getStartZ(), -1, 
                            dlg.direct, dlg.bounds, dlg.selCodes, (List)dlg.metrics, dlg.sample, null, progressBar);
                    ExecutorService.execute(task);
                    Map<String, WritableRaster> result = task.getResult();
                    DefaultGroupLayer gl = new DefaultGroupLayer("Metric grid result", true);
                    gl.setRemovable(true);
                    for(String name : result.keySet()) {
                        RasterStyle s = new RasterStyle();
                        s.setNoDataValue(-1);
                        RasterLayer l = new RasterLayer(name,
                                new RasterShape(result.get(name), project.getDtmCov().getEnvelope2D(), s, true), project.getCRS());
                        l.setRemovable(true);
                        gl.addLayerLast(l);
                    }
                    gl.setLayersVisible(false);
                    gl.getLayerFirst().setVisible(true);
                    rootLayer.addLayerFirst(gl);
                } else {
                    PointMetricTask task = new PointMetricTask(project.getStartZ(), -1, 
                            dlg.direct, dlg.bounds, (List)dlg.metrics, dlg.pointFile, dlg.idField, null, progressBar);
                    ExecutorService.execute(task);
                    List<DefaultFeature> features = task.getResult();
                    
                    FeatureLayer l = new FeatureLayer("Metric point result", features);
                    l.setRemovable(true);
                    rootLayer.addLayerFirst(l);
                }

                progressBar.close();
            }
        }).start();
        
    }//GEN-LAST:event_viewshedMetricMenuItemActionPerformed

    private void optionsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optionsMenuItemActionPerformed
        new OptionDialog(this, project).setVisible(true);
    }//GEN-LAST:event_optionsMenuItemActionPerformed

    private void tanMetricMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tanMetricMenuItemActionPerformed
        final ViewMetricDialog dlg = new ViewMetricDialog(this, true, project.getCodes());
        dlg.setVisible(true);
        if(!dlg.isOk) {
            return;
        }
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                ProgressBar progressBar = Config.getProgressBar("Metrics...");
                if(dlg.gridSampling) {
                    GridMetricTask task = new GridMetricTask(project.getStartZ(), dlg.bounds, 
                            dlg.selCodes, (List)dlg.metrics, dlg.sample, null, progressBar);
                    ExecutorService.execute(task);
                    Map<String, WritableRaster> result = task.getResult();
                    DefaultGroupLayer gl = new DefaultGroupLayer("Metric grid result", true);
                    gl.setRemovable(true);
                    for(String name : result.keySet()) {
                        RasterStyle s = new RasterStyle();
                        s.setNoDataValue(-1);
                        RasterLayer l = new RasterLayer(name,
                                new RasterShape(result.get(name), project.getDtmCov().getEnvelope2D(), s, true), project.getCRS());
                        l.setRemovable(true);
                        gl.addLayerLast(l);
                    }
                    gl.setLayersVisible(false);
                    gl.getLayerFirst().setVisible(true);
                    rootLayer.addLayerFirst(gl);
                } else {
                    PointMetricTask task = new PointMetricTask(project.getStartZ(), 
                            dlg.bounds, (List)dlg.metrics, dlg.pointFile, dlg.idField, null, progressBar);
                    ExecutorService.execute(task);
                    List<DefaultFeature> features = task.getResult();
                    FeatureLayer l = new FeatureLayer("Metric point result", features);
                    l.setRemovable(true);
                    rootLayer.addLayerFirst(l);
                }

                progressBar.close();
            }
        }).start();
    }//GEN-LAST:event_tanMetricMenuItemActionPerformed

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        setVisible(false);
        dispose();
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void addScaleMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addScaleMenuItemActionPerformed
        AddScaleDialog dlg = new AddScaleDialog(this, project);
        dlg.setVisible(true);
        
        if(dlg.isOk) {
            rootLayer = project.getLayers();
            mapViewer.setRootLayer(rootLayer);
        }
    }//GEN-LAST:event_addScaleMenuItemActionPerformed

    private void pathOrienMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pathOrienMenuItemActionPerformed
        
        PointAttributeDialog dlg = new PointAttributeDialog(this);
        dlg.setVisible(true);
        if(!dlg.isOk) {
            return;
        }
        try {
            Map<Object, DefaultFeature> pointMap = GlobalDataStore.createDataStore(dlg.pathFile.getParentFile()).getMapFeatures(dlg.pathFile.getName(), dlg.idField);
            List<DefaultFeature> points = new ArrayList<>(new TreeMap<>(pointMap).values());
            List<DefaultFeature> result = new ArrayList<>();
            if(dlg.setPathOrien) {
                for(int i = 0; i < points.size()-1; i++) {
                    Point p1 = points.get(i).getGeometry().getCentroid();
                    Point p2 = points.get(i+1).getGeometry().getCentroid();
                    double dir = Bounds.rad2deg(Math.atan2(p2.getY()-p1.getY(), p2.getX()-p1.getX()));
                    Bounds b = dlg.bounds.createBounds(dir);
                    result.add(b.createFeatureWithBoundAttr(points.get(i).getId(), points.get(i).getGeometry()));
                }
            } else {
                for(Feature p : points) {
                    result.add(dlg.bounds.createFeatureWithBoundAttr(p.getId(), p.getGeometry()));
                }
            }
            DefaultFeature.saveFeatures(result, new File(project.getDirectory(), dlg.outputName), GlobalDataStore.getCRS(dlg.pathFile));
            FeatureLayer l = new FeatureLayer(dlg.outputName, result);
            l.setRemovable(true);
            rootLayer.addLayerFirst(l);
        } catch (IOException | SchemaException ex) {
            throw new RuntimeException(ex);
        }
        
    }//GEN-LAST:event_pathOrienMenuItemActionPerformed

    private void formWindowActivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowActivated
        if(viewshedDlg != null) {
            viewshedDlg.setAlwaysOnTop(true);
        }
        if(viewtanDlg != null) {
            viewtanDlg.setAlwaysOnTop(true);
        }
        logFrame.setAlwaysOnTop(true);
    }//GEN-LAST:event_formWindowActivated

    private void formWindowDeactivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowDeactivated
        if(viewshedDlg != null) {
            viewshedDlg.toBack();
        }
        if(viewtanDlg != null) {
            viewtanDlg.toBack();
        }
        logFrame.toBack();
    }//GEN-LAST:event_formWindowDeactivated

    private WritableRaster samplingDEM(Raster dtm, int scale) {
        WritableRaster raster = dtm.createCompatibleWritableRaster(
                (int)Math.ceil(dtm.getWidth()/(double)scale), 
                (int)Math.ceil(dtm.getHeight()/(double)scale));
        double [] tab = new double[scale*scale];
        for(int y = 0; y < raster.getHeight(); y++) {
            for(int x = 0; x < raster.getWidth(); x++) {
                final int w = Math.min(scale, dtm.getWidth()-x*scale);
                final int h = Math.min(scale, dtm.getHeight()-y*scale);
                dtm.getSamples(x*scale, y*scale, w, h, 0, tab);
                int nbNaN = scale*scale - w*h;
                SummaryStatistics stats = new SummaryStatistics();
                for(int i = 0; i < w*h; i++) {
                    final double v = tab[i];
                    if(Double.isNaN(v)) {
                        nbNaN++;
                    } else {
                        stats.addValue(v);
                    }
                }          
                if(nbNaN < stats.getN()) {
                    raster.setSample(x, y, 0, stats.getMean());
                } else {
                    raster.setSample(x, y, 0, Double.NaN);
                }
            }
        }
        return raster;
    }
    
    private WritableRaster samplingLanduse(Raster land, int scale) {
        WritableRaster raster = land.createCompatibleWritableRaster(
                (int)Math.ceil(land.getWidth()/(double)scale), 
                (int)Math.ceil(land.getHeight()/(double)scale));
        int [] tab = new int[scale*scale];
        for(int y = 0; y < raster.getHeight(); y++) {
            for(int x = 0; x < raster.getWidth(); x++) {
                int w = Math.min(scale, land.getWidth()-x*scale);
                int h = Math.min(scale, land.getHeight()-y*scale);
                land.getSamples(x*scale, y*scale, w, h, 0, tab);
                int [] nb = new int[256];
                for(int i = 0; i < w*h; i++) {
                    nb[tab[i]]++;
                }     
                int max = Integer.MIN_VALUE;
                int landMax = -1;
                for(int i = 0; i < nb.length; i++) {
                    if(nb[i] > max) {
                        max = nb[i];
                        landMax = i;
                    }
                }
                raster.setSample(x, y, 0, landMax);
            }
        }
        return raster;
    }

    private void closeProject() {
        if(project != null) {
            project.close();
        }
        mapViewer.setRootLayer(new DefaultGroupLayer(""));
        project = null;
        viewshedDlg = null;
        viewtanDlg = null;
    }
    
    /**
     * Main program entry point.
     * @param args the command line arguments
     */
    public static void main(String args[]) throws Exception {
        
        // log all uncaught exception
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Logger.getLogger("").log(Level.SEVERE, null, e);
            }
        });
               
        Locale.setDefault(Locale.ENGLISH);
        
        // MPI Execution
        if(args.length > 0 && args[0].equals("-mpi")) {
            new MpiLauncher(Arrays.copyOfRange(args, 1, args.length)).run();
            System.exit(0);
        }

        // CLI execution
        if(args.length > 0 && !args[0].equals(JavaLoader.NOFORK)) {        
            if(!GraphicsEnvironment.isHeadless() && SplashScreen.getSplashScreen() != null) {
                SplashScreen.getSplashScreen().close();
            }
            new CLITools().execute(args);
        } else { // UI execution
            JavaLoader.launchGUI(MainFrame.class, args.length == 0, 2048);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem addScaleMenuItem;
    private javax.swing.JMenu dataMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuItem genMSMenuItem;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JMenuItem loadDSMMenuItem;
    private javax.swing.JMenuItem loadLandUseMenuItem;
    private javax.swing.JMenuItem loadProjectMenuItem;
    private javax.swing.JMenuItem logMenuItem;
    private org.thema.drawshape.ui.MapViewer mapViewer;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenu metricMenu;
    private javax.swing.JMenu msMenu;
    private javax.swing.JMenuItem multiViewshedMenuItem;
    private javax.swing.JMenuItem newProjectMenuItem;
    private javax.swing.JMenuItem optionsMenuItem;
    private javax.swing.JMenuItem pathOrienMenuItem;
    private javax.swing.JMenuItem prefMenuItem;
    private javax.swing.JMenuItem tanMetricMenuItem;
    private javax.swing.JMenu toolMenu;
    private javax.swing.JMenuItem viewShedMenuItem;
    private javax.swing.JMenuItem viewTanMenuItem;
    private javax.swing.JMenuItem viewshedMetricMenuItem;
    private javax.swing.JMenu visMenu;
    // End of variables declaration//GEN-END:variables
}
