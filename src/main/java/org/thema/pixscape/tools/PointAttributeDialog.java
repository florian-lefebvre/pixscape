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


package org.thema.pixscape.tools;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.thema.pixscape.Bounds;
import org.thema.pixscape.BoundsDialog;

/**
 * Dialog form for creating bounds attributes in a shapefile.
 * 
 * @author Gilles Vuidel
 */
public class PointAttributeDialog extends javax.swing.JDialog {

    /** Does user have validated the form ? */
    public boolean isOk = false;
    /** The input shapefile */
    public File pathFile;
    /** the identifier field name in the shapefile */
    public String idField;
    /** change orientation of each point depending on the next ? */
    public boolean setPathOrien;
    /** The bounds paramaters to set in attributes of the ouput shapefile */
    public Bounds bounds;
    /** The name of the output shapefile */
    public String outputName;
    
    /** 
     * Creates new form PathViewDialog 
     * @param parent the parent frame
     */
    public PointAttributeDialog(java.awt.Frame parent) {
        super(parent, true);
        initComponents();
        setLocationRelativeTo(parent);
        getRootPane().setDefaultButton(okButton);
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
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        boundsButton = new javax.swing.JButton();
        pathOrienCheckBox = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        outputNameTextField = new javax.swing.JTextField();
        selectVectorLayerPanel1 = new org.thema.data.ui.SelectVectorLayerPanel();

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/thema/pixscape/Bundle"); // NOI18N
        setTitle(bundle.getString("PointAttributeDialog.title")); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        java.util.ResourceBundle bundle1 = java.util.ResourceBundle.getBundle("org/thema/pixscape/tools/Bundle"); // NOI18N
        okButton.setText(bundle1.getString("PointAttributeDialog.okButton.text")); // NOI18N
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText(bundle1.getString("PointAttributeDialog.cancelButton.text")); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        boundsButton.setText(bundle1.getString("PointAttributeDialog.boundsButton.text")); // NOI18N
        boundsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                boundsButtonActionPerformed(evt);
            }
        });

        pathOrienCheckBox.setText(bundle1.getString("PointAttributeDialog.pathOrienCheckBox.text")); // NOI18N

        jLabel1.setText(bundle1.getString("PointAttributeDialog.jLabel1.text")); // NOI18N

        outputNameTextField.setText(bundle1.getString("PointAttributeDialog.outputNameTextField.text")); // NOI18N
        outputNameTextField.setToolTipText(bundle1.getString("PointAttributeDialog.outputNameTextField.toolTipText")); // NOI18N

        selectVectorLayerPanel1.setDescription(bundle1.getString("PointAttributeDialog.selectVectorLayerPanel1.description")); // NOI18N

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(pathOrienCheckBox)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                                .add(okButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 67, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(cancelButton))
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, boundsButton)))
                    .add(layout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(outputNameTextField))
                    .add(layout.createSequentialGroup()
                        .add(selectVectorLayerPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        layout.linkSize(new java.awt.Component[] {cancelButton, okButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(selectVectorLayerPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(18, 18, 18)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(boundsButton)
                    .add(pathOrienCheckBox))
                .add(18, 18, 18)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(outputNameTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 32, Short.MAX_VALUE)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(cancelButton)
                    .add(okButton))
                .addContainerGap())
        );

        getRootPane().setDefaultButton(okButton);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        pathFile = selectVectorLayerPanel1.getSelectedFile();
        idField = selectVectorLayerPanel1.getIdField();
        setPathOrien = pathOrienCheckBox.isSelected();
        if(bounds == null) {
            bounds = new Bounds();
        }
        outputName = outputNameTextField.getText();
        if(!outputName.endsWith(".shp") && !outputName.endsWith(".gpkg")) {
            outputName += ".gpkg";
        }
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

    private void doClose() {
        setVisible(false);
        dispose();
    }

   
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton boundsButton;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JButton okButton;
    private javax.swing.JTextField outputNameTextField;
    private javax.swing.JCheckBox pathOrienCheckBox;
    private org.thema.data.ui.SelectVectorLayerPanel selectVectorLayerPanel1;
    // End of variables declaration//GEN-END:variables

}