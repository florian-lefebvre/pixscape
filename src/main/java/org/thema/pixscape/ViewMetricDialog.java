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

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.thema.pixscape.metric.Metric;
import org.thema.pixscape.metric.MetricDialog;

/**
 * Dialog form for setting parameters for launching metric calculations with GridMetricTask or PointMetricTask.
 * 
 * @author Gilles Vuidel
 */
public class ViewMetricDialog extends javax.swing.JDialog {

    private final boolean isTan;
    private final SortedSet<Integer> codes;
    
    /** Does user have validated the form ? */
    public boolean isOk = false;
    
    public boolean inverse;
    /** The 3D limits of the sight */
    public Bounds bounds;
    /** Is grid sampling ? or point samping from  shapefile ? */
    public boolean gridSampling;
    /** For grid sampling only */
    public int sample;
    /** Land use codes restriction for grid sampling only*/
    public Set<Integer> selCodes;
    /** The shapefile for point sampling */
    public File pointFile;
    /** the identifier field name for point sampling */
    public String idField;
    /** The metrics to calculate */
    public List<Metric> metrics;
    /** The height of the observed points, -1 if not used */
    public double zDest;

    /** 
     * Creates new form ViewMetricDialog .
     * 
     * @param parent the parent frame
     * @param tangent is tangential view ? or planimetric ?
     * @param codes the project landuse codes
     */
    public ViewMetricDialog(java.awt.Frame parent, boolean tangent, SortedSet<Integer> codes) {
        super(parent, true);
        this.isTan = tangent;
        this.codes = codes;
        
        initComponents();
        setLocationRelativeTo(parent);

        // Close the dialog when Esc is pressed
        String cancelName = "cancel";
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), cancelName);
        ActionMap actionMap = getRootPane().getActionMap();
        actionMap.put(cancelName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doClose();
            }
        });
        
        if(codes != null && !codes.isEmpty()) {
            String s = Arrays.deepToString(codes.toArray());
            landCodesTextField.setText(s.substring(1, s.length()-1));
        } else {
            addAllButton.setEnabled(false);
            landCodesTextField.setEnabled(false);
            landCodeRadioButton.setEnabled(false);
        }
        
        inverseCheckBox.setVisible(!tangent);
        zDestTextField.setVisible(!tangent);
        zDestLabel.setVisible(!tangent);
        metricList.setModel(new DefaultListModel());
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
        buttonGroup2 = new javax.swing.ButtonGroup();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        inverseCheckBox = new javax.swing.JCheckBox();
        boundsButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        gridRadioButton = new javax.swing.JRadioButton();
        pointRadioButton = new javax.swing.JRadioButton();
        sampingRadioButton = new javax.swing.JRadioButton();
        landCodeRadioButton = new javax.swing.JRadioButton();
        sampleSpinner = new javax.swing.JSpinner();
        landCodesTextField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        selectVectorLayerPanel1 = new org.thema.data.ui.SelectVectorLayerPanel();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        metricList = new javax.swing.JList();
        addButton = new javax.swing.JButton();
        addAllButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        zDestLabel = new javax.swing.JLabel();
        zDestTextField = new javax.swing.JTextField();

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/thema/pixscape/Bundle"); // NOI18N
        setTitle(bundle.getString("ViewMetricDialog.title")); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        okButton.setText(bundle.getString("ViewMetricDialog.okButton.text")); // NOI18N
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText(bundle.getString("ViewMetricDialog.cancelButton.text")); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        inverseCheckBox.setText(bundle.getString("ViewMetricDialog.inverseCheckBox.text")); // NOI18N

        boundsButton.setText(bundle.getString("ViewMetricDialog.boundsButton.text")); // NOI18N
        boundsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                boundsButtonActionPerformed(evt);
            }
        });

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("ViewMetricDialog.jPanel2.border.title"))); // NOI18N

        buttonGroup1.add(gridRadioButton);
        gridRadioButton.setSelected(true);
        gridRadioButton.setText(bundle.getString("ViewMetricDialog.gridRadioButton.text")); // NOI18N

        buttonGroup1.add(pointRadioButton);
        pointRadioButton.setText(bundle.getString("ViewMetricDialog.pointRadioButton.text")); // NOI18N

        buttonGroup2.add(sampingRadioButton);
        sampingRadioButton.setSelected(true);
        sampingRadioButton.setText(bundle.getString("ViewMetricDialog.sampingRadioButton.text")); // NOI18N

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, gridRadioButton, org.jdesktop.beansbinding.ELProperty.create("${selected}"), sampingRadioButton, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        buttonGroup2.add(landCodeRadioButton);
        landCodeRadioButton.setText(bundle.getString("ViewMetricDialog.landCodeRadioButton.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, gridRadioButton, org.jdesktop.beansbinding.ELProperty.create("${selected}"), landCodeRadioButton, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        sampleSpinner.setModel(new javax.swing.SpinnerNumberModel(1, 1, null, 1));

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, gridRadioButton, org.jdesktop.beansbinding.ELProperty.create("${selected}"), sampleSpinner, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, gridRadioButton, org.jdesktop.beansbinding.ELProperty.create("${selected}"), landCodesTextField, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        jLabel1.setText(bundle.getString("ViewMetricDialog.jLabel1.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, pointRadioButton, org.jdesktop.beansbinding.ELProperty.create("${selected}"), selectVectorLayerPanel1, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(12, 12, 12)
                        .add(selectVectorLayerPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel2Layout.createSequentialGroup()
                                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(gridRadioButton)
                                    .add(pointRadioButton))
                                .add(0, 0, Short.MAX_VALUE))
                            .add(jPanel2Layout.createSequentialGroup()
                                .add(12, 12, 12)
                                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jPanel2Layout.createSequentialGroup()
                                        .add(landCodeRadioButton)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(landCodesTextField))
                                    .add(jPanel2Layout.createSequentialGroup()
                                        .add(sampingRadioButton)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(sampleSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 66, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(jLabel1)
                                        .add(0, 0, Short.MAX_VALUE)))))
                        .addContainerGap())))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(gridRadioButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(sampingRadioButton)
                    .add(sampleSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(landCodeRadioButton)
                    .add(landCodesTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(pointRadioButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(selectVectorLayerPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("ViewMetricDialog.jPanel1.border.title"))); // NOI18N

        jScrollPane1.setViewportView(metricList);

        addButton.setText(bundle.getString("ViewMetricDialog.addButton.text")); // NOI18N
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        addAllButton.setText(bundle.getString("ViewMetricDialog.addAllButton.text")); // NOI18N
        addAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addAllButtonActionPerformed(evt);
            }
        });

        removeButton.setText(bundle.getString("ViewMetricDialog.removeButton.text")); // NOI18N
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(removeButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(addAllButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(addButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(addButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(addAllButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(removeButton)
                        .add(0, 0, Short.MAX_VALUE))
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );

        zDestLabel.setText(bundle.getString("ViewMetricDialog.zDestLabel.text")); // NOI18N

        zDestTextField.setText(bundle.getString("ViewMetricDialog.zDestTextField.text")); // NOI18N

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(0, 0, Short.MAX_VALUE)
                        .add(okButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 67, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(cancelButton))
                    .add(layout.createSequentialGroup()
                        .add(zDestLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(zDestTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18)
                        .add(inverseCheckBox)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(boundsButton))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        layout.linkSize(new java.awt.Component[] {cancelButton, okButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(zDestLabel)
                        .add(zDestTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(inverseCheckBox))
                    .add(boundsButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(18, 18, 18)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(cancelButton)
                    .add(okButton))
                .addContainerGap())
        );

        getRootPane().setDefaultButton(okButton);

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        inverse = inverseCheckBox.isSelected();

        if(bounds == null) {
            bounds = new Bounds();
        }
        
        gridSampling = gridRadioButton.isSelected();
        selCodes = new TreeSet<>();
        if(gridSampling) {
            if(landCodeRadioButton.isSelected()) {
                for(String s : landCodesTextField.getText().split(",")) {
                    if(!s.trim().isEmpty()) {
                        selCodes.add(Integer.parseInt(s.trim()));
                    }
                }
                sample = 1;
            } else {
                sample = (int) sampleSpinner.getValue();
            }
        } else {
            pointFile = selectVectorLayerPanel1.getSelectedFile();
            idField = selectVectorLayerPanel1.getIdField();
        }
        zDest = Double.parseDouble(zDestTextField.getText());
        metrics = (List)Arrays.asList(((DefaultListModel)metricList.getModel()).toArray());
        isOk = true;
        doClose();
    }//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        doClose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
        doClose();
    }//GEN-LAST:event_closeDialog

    private void boundsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_boundsButtonActionPerformed
        BoundsDialog dlg = new BoundsDialog((Frame) this.getParent(), bounds == null ? new Bounds() : bounds);
        dlg.setVisible(true);
        if(dlg.isOk) {
            bounds = dlg.bounds;
        }
    }//GEN-LAST:event_boundsButtonActionPerformed

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        MetricDialog dlg = new MetricDialog((Frame) this.getParent(), isTan, codes);
        dlg.setVisible(true);
        if(!dlg.isOk) {
            return;
        }
        
        ((DefaultListModel)metricList.getModel()).addElement(dlg.metric);
    }//GEN-LAST:event_addButtonActionPerformed

    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
        int i = 0;
        while(i < metricList.getModel().getSize()) {
            if(metricList.getSelectionModel().isSelectedIndex(i)) {
                ((DefaultListModel)metricList.getModel()).remove(i);
            } else {
                i++;
            }
        }
    }//GEN-LAST:event_removeButtonActionPerformed

    private void addAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addAllButtonActionPerformed
        MetricDialog dlg = new MetricDialog((Frame) this.getParent(), isTan, codes);
        dlg.setVisible(true);
        if(!dlg.isOk) {
            return;
        }

        for(int code : dlg.metric.getCodes()) {
            try {
                Metric m = dlg.metric.getClass().newInstance();
                m.addCode(code);
                ((DefaultListModel)metricList.getModel()).addElement(m);
            } catch (InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(ViewMetricDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }//GEN-LAST:event_addAllButtonActionPerformed

    private void doClose() {
        setVisible(false);
        dispose();
    }
   
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addAllButton;
    private javax.swing.JButton addButton;
    private javax.swing.JButton boundsButton;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JButton cancelButton;
    private javax.swing.JRadioButton gridRadioButton;
    private javax.swing.JCheckBox inverseCheckBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JRadioButton landCodeRadioButton;
    private javax.swing.JTextField landCodesTextField;
    private javax.swing.JList metricList;
    private javax.swing.JButton okButton;
    private javax.swing.JRadioButton pointRadioButton;
    private javax.swing.JButton removeButton;
    private javax.swing.JRadioButton sampingRadioButton;
    private javax.swing.JSpinner sampleSpinner;
    private org.thema.data.ui.SelectVectorLayerPanel selectVectorLayerPanel1;
    private javax.swing.JLabel zDestLabel;
    private javax.swing.JTextField zDestTextField;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

}
