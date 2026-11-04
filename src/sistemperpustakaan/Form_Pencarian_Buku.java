package sistemperpustakaan;

import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author PC-1B LKS-2026
 */
public class Form_Pencarian_Buku extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Form_Pencarian_Buku.class.getName());
    private Connection con;

    /**
     * Creates new form Form_Pencarian_Buku
     */
    public Form_Pencarian_Buku() {
        initComponents();
        initKembaliButton();
        initDB();
        initCustomListeners();
        loadAllBooks();
    }

    private void initDB() {
        try {
            con = Config.configDB();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Koneksi database gagal: " + e.getMessage());
        }
    }

    private void initCustomListeners() {
        b_cari.addActionListener(e -> cariBuku());

        // Tekan enter di salah satu textfield langsung eksekusi pencarian
        t_judul.addActionListener(e -> cariBuku());
        t_pengarang.addActionListener(e -> cariBuku());
        t_penerbit.addActionListener(e -> cariBuku());
        t_tahun.addActionListener(e -> cariBuku());
        t_kategori.addActionListener(e -> cariBuku());

        jTable1.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && jTable1.getSelectedRow() >= 0) {
                int r = jTable1.getSelectedRow();
                t_judul.setText(jTable1.getValueAt(r, 0) != null ? jTable1.getValueAt(r, 0).toString() : "");
                t_pengarang.setText(jTable1.getValueAt(r, 1) != null ? jTable1.getValueAt(r, 1).toString() : "");
                t_penerbit.setText(jTable1.getValueAt(r, 2) != null ? jTable1.getValueAt(r, 2).toString() : "");
                t_tahun.setText(jTable1.getValueAt(r, 3) != null ? jTable1.getValueAt(r, 3).toString() : "");
                t_kategori.setText(jTable1.getValueAt(r, 4) != null ? jTable1.getValueAt(r, 4).toString() : "");
            }
        });
    }

    private void loadAllBooks() {
        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Judul Buku", "Pengarang / Penulis", "Penerbit", "Tahun Terbit", "Kategori", "Stok Tersedia"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        try {
            if (con == null || con.isClosed()) initDB();

            // Coba query dari skema relasional terlebih dahulu
            String sql = "SELECT b.judul, "
                       + "IFNULL(GROUP_CONCAT(pn.nama_penulis SEPARATOR ', '), '-') AS penulis, "
                       + "IFNULL(pb.nama_penerbit, '-') AS penerbit, "
                       + "IFNULL(b.tahun_terbit, '-') AS tahun, "
                       + "IFNULL(k.nama_kategori, '-') AS kategori, "
                       + "b.stok_tersedia "
                       + "FROM buku b "
                       + "LEFT JOIN buku_penulis bp ON b.id_buku = bp.id_buku "
                       + "LEFT JOIN penulis pn ON bp.id_penulis = pn.id_penulis "
                       + "LEFT JOIN penerbit pb ON b.id_penerbit = pb.id_penerbit "
                       + "LEFT JOIN kategori k ON b.id_kategori = k.id_kategori "
                       + "GROUP BY b.id_buku ORDER BY b.judul ASC";

            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getString("judul"),
                        rs.getString("penulis"),
                        rs.getString("penerbit"),
                        rs.getString("tahun"),
                        rs.getString("kategori"),
                        rs.getInt("stok_tersedia")
                    });
                }
            }
        } catch (SQLException e) {
            // Fallback ke tabel buku sederhana jika skema relasional belum diisi
            try {
                String sqlOld = "SELECT judul, pengarang, penerbit, '-' AS tahun, kategori, stok FROM buku";
                try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sqlOld)) {
                    while (rs.next()) {
                        model.addRow(new Object[]{
                            rs.getString("judul"),
                            rs.getString("pengarang"),
                            rs.getString("penerbit"),
                            "-",
                            rs.getString("kategori"),
                            rs.getInt("stok")
                        });
                    }
                }
            } catch (Exception ignored) {}
        }
        jTable1.setModel(model);
    }

    private void cariBuku() {
        String judul = t_judul.getText().trim();
        String pengarang = t_pengarang.getText().trim();
        String penerbit = t_penerbit.getText().trim();
        String tahun = t_tahun.getText().trim();
        String kategori = t_kategori.getText().trim();

        if (judul.isEmpty() && pengarang.isEmpty() && penerbit.isEmpty() && tahun.isEmpty() && kategori.isEmpty()) {
            loadAllBooks();
            return;
        }

        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Judul Buku", "Pengarang / Penulis", "Penerbit", "Tahun Terbit", "Kategori", "Stok Tersedia"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        try {
            if (con == null || con.isClosed()) initDB();

            // Query pencarian dengan filter dinamis
            StringBuilder sql = new StringBuilder(
                "SELECT b.judul, "
              + "IFNULL(GROUP_CONCAT(pn.nama_penulis SEPARATOR ', '), '-') AS penulis, "
              + "IFNULL(pb.nama_penerbit, '-') AS penerbit, "
              + "IFNULL(b.tahun_terbit, '-') AS tahun, "
              + "IFNULL(k.nama_kategori, '-') AS kategori, "
              + "b.stok_tersedia "
              + "FROM buku b "
              + "LEFT JOIN buku_penulis bp ON b.id_buku = bp.id_buku "
              + "LEFT JOIN penulis pn ON bp.id_penulis = pn.id_penulis "
              + "LEFT JOIN penerbit pb ON b.id_penerbit = pb.id_penerbit "
              + "LEFT JOIN kategori k ON b.id_kategori = k.id_kategori "
              + "WHERE 1=1 "
            );

            if (!judul.isEmpty()) sql.append("AND b.judul LIKE '%").append(judul).append("%' ");
            if (!penerbit.isEmpty()) sql.append("AND pb.nama_penerbit LIKE '%").append(penerbit).append("%' ");
            if (!tahun.isEmpty()) sql.append("AND CAST(b.tahun_terbit AS CHAR) LIKE '%").append(tahun).append("%' ");
            if (!kategori.isEmpty()) sql.append("AND k.nama_kategori LIKE '%").append(kategori).append("%' ");

            sql.append("GROUP BY b.id_buku ");
            if (!pengarang.isEmpty()) {
                sql.append("HAVING penulis LIKE '%").append(pengarang).append("%' ");
            }
            sql.append("ORDER BY b.judul ASC");

            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql.toString())) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getString("judul"),
                        rs.getString("penulis"),
                        rs.getString("penerbit"),
                        rs.getString("tahun"),
                        rs.getString("kategori"),
                        rs.getInt("stok_tersedia")
                    });
                }
            }
        } catch (SQLException e) {
            // Fallback ke pencarian buku tabel flat
            try {
                String sqlOld = "SELECT judul, pengarang, penerbit, '-' AS tahun, kategori, stok FROM buku WHERE judul LIKE ? OR pengarang LIKE ?";
                try (PreparedStatement pst = con.prepareStatement(sqlOld)) {
                    pst.setString(1, "%" + judul + "%");
                    pst.setString(2, "%" + pengarang + "%");
                    ResultSet rs = pst.executeQuery();
                    while (rs.next()) {
                        model.addRow(new Object[]{
                            rs.getString("judul"),
                            rs.getString("pengarang"),
                            rs.getString("penerbit"),
                            "-",
                            rs.getString("kategori"),
                            rs.getInt("stok")
                        });
                    }
                }
            } catch (Exception ignored) {}
        }
        jTable1.setModel(model);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        t_judul = new javax.swing.JTextField();
        t_pengarang = new javax.swing.JTextField();
        t_penerbit = new javax.swing.JTextField();
        t_tahun = new javax.swing.JTextField();
        t_kategori = new javax.swing.JTextField();
        b_cari = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(255, 255, 204));

        jPanel2.setBackground(new java.awt.Color(255, 255, 204));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setText("Judul");

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel2.setText("Pengarang");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setText("Penerbit");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel4.setText("Tahun Terbit");

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel5.setText("Kategori");

        t_judul.addActionListener(this::t_judulActionPerformed);

        t_pengarang.addActionListener(this::t_pengarangActionPerformed);

        t_penerbit.addActionListener(this::t_penerbitActionPerformed);

        t_tahun.addActionListener(this::t_tahunActionPerformed);

        t_kategori.addActionListener(this::t_kategoriActionPerformed);

        b_cari.setBackground(new java.awt.Color(204, 204, 204));
        b_cari.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        b_cari.setText("Cari");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5)
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 169, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(40, 40, 40)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(t_judul, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(b_cari))
                            .addComponent(t_penerbit, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(t_pengarang, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(t_tahun, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(t_kategori, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 595, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(t_judul, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(b_cari))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(t_pengarang, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(t_penerbit, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(t_tahun, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(t_kategori, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(26, 26, 26)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(38, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jMenu1.setText("Form Pencarian Buku");
        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void t_judulActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t_judulActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_t_judulActionPerformed

    private void t_pengarangActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t_pengarangActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_t_pengarangActionPerformed

    private void t_penerbitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t_penerbitActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_t_penerbitActionPerformed

    private void t_tahunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t_tahunActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_t_tahunActionPerformed

    private void t_kategoriActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t_kategoriActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_t_kategoriActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
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

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new Form_Pencarian_Buku().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton b_cari;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField t_judul;
    private javax.swing.JTextField t_kategori;
    private javax.swing.JTextField t_penerbit;
    private javax.swing.JTextField t_pengarang;
    private javax.swing.JTextField t_tahun;
    // End of variables declaration//GEN-END:variables

    private void initKembaliButton() {
        javax.swing.JButton btnKembaliNavigasi = new javax.swing.JButton("← Kembali");
        btnKembaliNavigasi.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        btnKembaliNavigasi.setBackground(new java.awt.Color(220, 53, 69));
        btnKembaliNavigasi.setForeground(java.awt.Color.WHITE);
        btnKembaliNavigasi.setFocusPainted(false);
        btnKembaliNavigasi.addActionListener(e -> {
            if (SessionManager.loggedRoleId == 2) {
                new Dashboard_user().setVisible(true);
            } else {
                new Dashboard_Admin().setVisible(true);
            }
            this.dispose();
        });
        try {
            jPanel1.add(btnKembaliNavigasi, 0);
        } catch (Exception ex) {
            getContentPane().add(btnKembaliNavigasi);
        }
    }
}