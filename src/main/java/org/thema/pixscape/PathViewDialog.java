/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thema.pixscape;


import com.vividsolutions.jts.geom.Geometry;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;

/**
 *
 * @author gvuidel
 */
public class PathViewDialog extends javax.swing.JDialog {

    public boolean isOk = false;
    public File pathFile;
    public String idField;
    public double startZ;
    public Bounds bounds;
    
    /** Creates new form PathViewDialog */
    public PathViewDialog(java.awt.Frame parent) {
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
        pathSelectFilePanel = new org.thema.common.swing.SelectFilePanel();
        jLabel1 = new javax.swing.JLabel();
        idComboBox = new javax.swing.JComboBox();
        boundsButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        zEyeTextField = new javax.swing.JTextField();

        setTitle("Path view");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        pathSelectFilePanel.setDescription("Path points");
        pathSelectFilePanel.setFileDesc("Shapefile");
        pathSelectFilePanel.setFileExts(".shp");
        pathSelectFilePanel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pathSelectFilePanelActionPerformed(evt);
            }
        });

        jLabel1.setText("Id");

        boundsButton.setText("Bounds...");
        boundsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                boundsButtonActionPerformed(evt);
            }
        });

        jLabel2.setText("Z eye");

        zEyeTextField.setText("1.8");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(12, 12, 12)
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(idComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 163, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                                .add(0, 238, Short.MAX_VALUE)
                                .add(okButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 67, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(cancelButton))
                            .add(pathSelectFilePanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                        .addContainerGap())
                    .add(layout.createSequentialGroup()
                        .add(jLabel2)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(zEyeTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(boundsButton)
                        .add(0, 0, Short.MAX_VALUE))))
        );

        layout.linkSize(new java.awt.Component[] {cancelButton, okButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(pathSelectFilePanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(idComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(jLabel2)
                        .add(zEyeTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(boundsButton)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(cancelButton)
                    .add(okButton))
                .addContainerGap())
        );

        getRootPane().setDefaultButton(okButton);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        pathFile = pathSelectFilePanel.getSelectedFile();
        idField = idComboBox.getSelectedItem().toString();
        startZ = Double.parseDouble(zEyeTextField.getText());
        if(bounds == null)
            bounds = new Bounds();
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
        if(dlg.isOk)
            bounds = dlg.bounds;
    }//GEN-LAST:event_boundsButtonActionPerformed

    private void pathSelectFilePanelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pathSelectFilePanelActionPerformed
        try {
            DefaultComboBoxModel model = new DefaultComboBoxModel();
            ShapefileDataStore datastore = new ShapefileDataStore(pathSelectFilePanel.getSelectedFile().toURI().toURL(),null);
            SimpleFeatureType schema = datastore.getSchema();
            for(AttributeType type : schema.getTypes()) {
                if(!Geometry.class.isAssignableFrom(type.getBinding()))
                    model.addElement(type.getName().getLocalPart());
            }
            idComboBox.setModel(model);
        } catch (Exception ex) {
            Logger.getLogger(PathViewDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_pathSelectFilePanelActionPerformed

    private void doClose() {
        setVisible(false);
        dispose();
    }

   
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton boundsButton;
    private javax.swing.JButton cancelButton;
    private javax.swing.JComboBox idComboBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JButton okButton;
    private org.thema.common.swing.SelectFilePanel pathSelectFilePanel;
    private javax.swing.JTextField zEyeTextField;
    // End of variables declaration//GEN-END:variables

}
