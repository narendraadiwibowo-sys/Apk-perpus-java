package sistemperpustakaan;

/**
 * @author PC-1B LKS-2026
 */
public class Form_peminjaman extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Form_peminjaman.class.getName());
    private java.awt.Image bgOriginal;

    public Form_peminjaman() {
        initComponents();
        // Load gambar asli untuk resize
        bgOriginal = new javax.swing.ImageIcon(getClass().getResource("/images/Form Peminjaman.jpg")).getImage();
        // Listener resize saat maximize - background ikut memenuhi layar
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent evt) {
                resizeBackground();
            }
        });
    }

    /** Resize background mengikuti ukuran window saat maximize */
    private void resizeBackground() {
        int w = getContentPane().getWidth();
        int h = getContentPane().getHeight();
        if (bgOriginal != null && w > 0 && h > 0) {
            bgLabel.setIcon(new javax.swing.ImageIcon(
                bgOriginal.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH)));
            bgPanel.setBounds(0, 0, w, h);
            bgLabel.setBounds(0, 0, w, h);
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bgPanel = new javax.swing.JPanel();
        bgLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        bgPanel.setBackground(new java.awt.Color(0, 0, 0));
        bgPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        bgLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/Form Peminjaman.jpg"))); // NOI18N
        bgPanel.add(bgLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 690, 490));

        getContentPane().add(bgPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 640, 359));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public static void main(String args[]) {
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        java.awt.EventQueue.invokeLater(() -> new Form_peminjaman().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel bgLabel;
    private javax.swing.JPanel bgPanel;
    // End of variables declaration//GEN-END:variables
}
