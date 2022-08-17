/*
 * Copyright (C) 2015 Laboratoire ThéMA - UMR 6049 - CNRS / Université de Franche-Comté
 * http://thema.univ-fcomte.fr
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.thema.pixscape;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.referencing.operation.TransformException;
import org.thema.data.feature.DefaultFeature;
import org.thema.drawshape.PanelMap;
import static org.thema.drawshape.PanelMap.INPUT_CURSOR_MODE;
import org.thema.drawshape.PointShape;
import org.thema.drawshape.SelectableShape;
import org.thema.drawshape.image.RasterShape;
import org.thema.drawshape.layer.DefaultGroupLayer;
import org.thema.drawshape.layer.DefaultLayer;
import org.thema.drawshape.layer.FeatureLayer;
import org.thema.drawshape.layer.Layer;
import org.thema.drawshape.layer.RasterLayer;
import org.thema.drawshape.style.FeatureStyle;
import org.thema.drawshape.style.PointStyle;
import org.thema.drawshape.style.RasterStyle;
import org.thema.drawshape.style.table.UniqueColorTable;
import org.thema.drawshape.ui.MapViewer;
import org.thema.pixscape.view.MultiViewShedResult;
import org.thema.pixscape.view.ViewShedResult;

/**
 * Dialog form for iteractively calculates planimetric view (viewshed).
 * 
 * @author Gilles Vuidel
 */
public class ViewShedDialog extends javax.swing.JDialog implements PanelMap.ShapeMouseListener {

    private boolean isOk = false;
    private Bounds bounds;
    
    private final MapViewer mapViewer;
    private final Project project;

    private PointShape centreShape;
    private DefaultLayer centreLayer;
    private DefaultGroupLayer layers;
    
    private final MetricResultDialog metricDlg;
    
    /** 
     * Creates new form ViewShedDialog 
     * @param parent the parent frame
     * @param project the current project
     * @param mapViewer the map viewer for refreshing view
     */
    public ViewShedDialog(Frame parent, Project project, MapViewer mapViewer) {
        super(parent, false);
        this.project = project;
        initComponents();
        getRootPane().setDefaultButton(updateButton);
        multiScaleCheckBox.setEnabled(project.hasMultiScale());
        
        zEyeTextField.setText(""+project.getStartZ());
        minDistTextField.setText(""+project.getMinDistMS());
        
        
        this.mapViewer = mapViewer;
        double x = mapViewer.getLayers().getBounds().getCenterX();
        double y = mapViewer.getLayers().getBounds().getCenterY();
        pointTextField.setText(x + ", " + y);
        
        metricDlg = new MetricResultDialog(this, false, project.getDefaultScaleData().getCodes());
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        buttonGroup1 = new javax.swing.ButtonGroup();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        pointTextField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        zEyeTextField = new javax.swing.JTextField();
        inverseCheckBox = new javax.swing.JCheckBox();
        boundsButton = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        zDestTextField = new javax.swing.JTextField();
        updateButton = new javax.swing.JButton();
        multiScaleCheckBox = new javax.swing.JCheckBox();
        minDistTextField = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        rasterRadioButton = new javax.swing.JRadioButton();
        vectorRadioButton = new javax.swing.JRadioButton();
        metricsButton = new javax.swing.JButton();

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/thema/pixscape/Bundle"); // NOI18N
        setTitle(bundle.getString("ViewShedDialog.title")); // NOI18N
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                formComponentShown(evt);
            }
            public void componentHidden(java.awt.event.ComponentEvent evt) {
                formComponentHidden(evt);
            }
        });

        okButton.setText(bundle.getString("ViewShedDialog.okButton.text")); // NOI18N
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText(bundle.getString("ViewShedDialog.cancelButton.text")); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        jLabel1.setText(bundle.getString("ViewShedDialog.jLabel1.text")); // NOI18N

        pointTextField.setText(bundle.getString("ViewShedDialog.pointTextField.text")); // NOI18N

        jLabel2.setText(bundle.getString("ViewShedDialog.jLabel2.text")); // NOI18N

        zEyeTextField.setText(bundle.getString("ViewShedDialog.zEyeTextField.text")); // NOI18N

        inverseCheckBox.setText(bundle.getString("ViewShedDialog.inverseCheckBox.text")); // NOI18N

        boundsButton.setText(bundle.getString("ViewShedDialog.boundsButton.text")); // NOI18N
        boundsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                boundsButtonActionPerformed(evt);
            }
        });

        jLabel3.setText(bundle.getString("ViewShedDialog.jLabel3.text")); // NOI18N

        zDestTextField.setText(bundle.getString("ViewShedDialog.zDestTextField.text")); // NOI18N

        updateButton.setText(bundle.getString("ViewShedDialog.updateButton.text")); // NOI18N
        updateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateButtonActionPerformed(evt);
            }
        });

        multiScaleCheckBox.setText(bundle.getString("ViewShedDialog.multiScaleCheckBox.text")); // NOI18N

        minDistTextField.setText(bundle.getString("ViewShedDialog.minDistTextField.text")); // NOI18N

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, multiScaleCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected}"), minDistTextField, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("ViewShedDialog.jPanel1.border.title"))); // NOI18N

        buttonGroup1.add(rasterRadioButton);
        rasterRadioButton.setSelected(true);
        rasterRadioButton.setText(bundle.getString("ViewShedDialog.rasterRadioButton.text")); // NOI18N

        buttonGroup1.add(vectorRadioButton);
        vectorRadioButton.setText(bundle.getString("ViewShedDialog.vectorRadioButton.text")); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(rasterRadioButton)
                    .add(vectorRadioButton))
                .addContainerGap(20, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(rasterRadioButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(vectorRadioButton)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        metricsButton.setText(bundle.getString("ViewShedDialog.metricsButton.text")); // NOI18N
        metricsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                metricsButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(updateButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(okButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 67, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(cancelButton))
                    .add(layout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(pointTextField))
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel2)
                            .add(jLabel3))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createSequentialGroup()
                                .add(zDestTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(0, 0, Short.MAX_VALUE))
                            .add(layout.createSequentialGroup()
                                .add(zEyeTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(18, 18, 18)
                                .add(inverseCheckBox)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 65, Short.MAX_VALUE)
                                .add(boundsButton))))
                    .add(layout.createSequentialGroup()
                        .add(multiScaleCheckBox)
                        .add(18, 18, 18)
                        .add(minDistTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 69, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(0, 0, Short.MAX_VALUE))
                    .add(layout.createSequentialGroup()
                        .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(metricsButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 84, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        layout.linkSize(new java.awt.Component[] {cancelButton, okButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(pointTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(zEyeTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(inverseCheckBox)
                    .add(boundsButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(zDestTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(multiScaleCheckBox)
                    .add(minDistTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 16, Short.MAX_VALUE)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(cancelButton)
                            .add(okButton)
                            .add(updateButton)))
                    .add(metricsButton))
                .addContainerGap())
        );

        getRootPane().setDefaultButton(okButton);

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        isOk = true;
        doClose();
    }//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        isOk = false;
        doClose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void boundsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_boundsButtonActionPerformed
        BoundsDialog dlg = new BoundsDialog((Frame) this.getParent(), bounds == null ? new Bounds() : bounds);
        dlg.setVisible(true);
        if(dlg.isOk) {
            bounds = dlg.bounds;
        }
    }//GEN-LAST:event_boundsButtonActionPerformed

    private void updateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateButtonActionPerformed
        String[] coords = pointTextField.getText().split(",");
        Point2D p = new Point2D.Double(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]));
        mouseClicked(p, null, null, INPUT_CURSOR_MODE);
    }//GEN-LAST:event_updateButtonActionPerformed

    private void metricsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_metricsButtonActionPerformed
        metricDlg.setLocation(getX(), getY() + getHeight());
        metricDlg.setVisible(true);
    }//GEN-LAST:event_metricsButtonActionPerformed

    private void formComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentShown
        mapViewer.addMouseListener(this);
        mapViewer.setCursorMode(PanelMap.INPUT_CURSOR_MODE);
        String[] coords = pointTextField.getText().split(",");
        Point2D p = new Point2D.Double(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]));
        centreShape = new PointShape(p);
        centreShape.setStyle(new PointStyle(Color.BLACK, Color.RED));
        centreLayer = new DefaultLayer("Centre", centreShape);
        centreLayer.setRemovable(true);
        layers = new DefaultGroupLayer("ViewShed", true);
        layers.setRemovable(true);
        layers.addLayerFirst(centreLayer);
        ((DefaultGroupLayer)mapViewer.getLayers()).addLayerFirst(layers);
    }//GEN-LAST:event_formComponentShown

    private void formComponentHidden(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentHidden
        mapViewer.removeShapeMouseListener(this);
        if(!isOk) {
            mapViewer.getLayers().removeLayer(layers);
        }
    }//GEN-LAST:event_formComponentHidden

    @Override
    public void mouseClicked(Point2D p, List<SelectableShape> shapes, MouseEvent sourceEvent, int cursorMode) {
        if(cursorMode != INPUT_CURSOR_MODE) {
            return;
        }
        pointTextField.setText(p.getX() + "," + p.getY());
        centreShape.setPoint2D(p);
        for(Layer l : new ArrayList<>(layers.getLayers())) {
            if(!(l instanceof DefaultLayer)) {
                layers.removeLayer(l);
            }
        }
        ViewShedResult result;
        
        if(multiScaleCheckBox.isSelected()) {
            MultiViewShedResult multiResult = project.getMultiComputeView(Double.parseDouble(minDistTextField.getText()))
                    .calcViewShed(new DirectPosition2D(p), Double.parseDouble(zEyeTextField.getText()),
                            Double.parseDouble(zDestTextField.getText()), inverseCheckBox.isSelected(), bounds == null ? new Bounds() : bounds);
            if(rasterRadioButton.isSelected()) {
                for(double res : multiResult.getViews().keySet()) {
                    try {
                        GridEnvelope2D env = multiResult.getZones().get(res);
                        addRasterViewShedLayer(multiResult.getViews().get(res).createTranslatedChild(0, 0), project.getScaleData(res).getGridGeometry().gridToWorld(env), (inverseCheckBox.isSelected()?"direct":"indirect") + "-" + res);
                    } catch (TransformException ex) {
                        Logger.getLogger(ViewShedDialog.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } 
            } else {
                addViewShedLayer(multiResult, inverseCheckBox.isSelected()?"inverse":"direct");
            }
            result = multiResult;
        } else {
            result = project.getSimpleComputeView().calcViewShed(new DirectPosition2D(p), Double.parseDouble(zEyeTextField.getText()),
                Double.parseDouble(zDestTextField.getText()), inverseCheckBox.isSelected(), bounds == null ? new Bounds() : bounds);
            addViewShedLayer(result, inverseCheckBox.isSelected()?"inverse":"direct");
        }

        metricDlg.setResult(result);
        mapViewer.getMap().fullRepaint(); // normalement pas utile mais par moment bug du rafraichissement...
    }
    
    private void addViewShedLayer(ViewShedResult result, String name) {
        Layer layer;
        if(rasterRadioButton.isSelected()) {
            layer = new RasterLayer("Viewshed-" + name, new RasterShape(result.getView(),
                        project.getDefaultScaleData().getGridGeometry().getEnvelope2D(), new RasterStyle( 
                                new Color[] {new Color(0, 0, 0, 120), new Color(0, 0, 0, 0)}, 255, new Color(0, 0, 0, 0)), true), project.getCRS());
        } else {
            layer = new FeatureLayer("Viewshed-" + name, Arrays.asList(new DefaultFeature(name, result.getPolygon())), 
                    new FeatureStyle(new Color(0, 0, 0, 40), null), project.getCRS());
        }
        layer.setRemovable(true);
        layers.addLayerLast(layer);
    }
    
    private void addRasterViewShedLayer(Raster view, Rectangle2D zone, String name) {
        Layer layer;

            layer = new RasterLayer("Viewshed-" + name, new RasterShape(view,
                        zone, new RasterStyle( new UniqueColorTable(Arrays.asList(0.0, 1.0, 255.0),
                                Arrays.asList(new Color(0, 0, 0, 120), new Color(0, 0, 0, 0), new Color(0, 0, 0, 0))), true), true), project.getCRS());

//        // for debugging pixels not tested
//        layer = new RasterLayer("Viewshed-" + name, new RasterShape(view,
//                    zone, new RasterStyle(new UniqueColorTable(Arrays.asList(0.0, 1.0, 255.0), 
//                            Arrays.asList(new Color(127, 127, 127), new Color(255, 255, 255), new Color(255, 0, 0))), true), true), project.getCRS());
        
        layer.setRemovable(true);
        layers.addLayerLast(layer);
    }
    
    private void doClose() {
        setVisible(false);
    }

   
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton boundsButton;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton cancelButton;
    private javax.swing.JCheckBox inverseCheckBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton metricsButton;
    private javax.swing.JTextField minDistTextField;
    private javax.swing.JCheckBox multiScaleCheckBox;
    private javax.swing.JButton okButton;
    private javax.swing.JTextField pointTextField;
    private javax.swing.JRadioButton rasterRadioButton;
    private javax.swing.JButton updateButton;
    private javax.swing.JRadioButton vectorRadioButton;
    private javax.swing.JTextField zDestTextField;
    private javax.swing.JTextField zEyeTextField;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

}
