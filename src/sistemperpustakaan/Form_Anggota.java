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
public class Form_Anggota extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Form_Anggota.class.getName());
    private Connection con;

    /**
     * Creates new form Form_Anggota
     */
    public Form_Anggota() {
        initComponents();
        initDB();
        initCustomListeners();
        loadTable();
    }

    private void initDB() {
        try {
            con = Config.configDB();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Koneksi database gagal: " + e.getMessage());
        }
    }

    private void initCustomListeners() {
        b_tambah.addActionListener(e -> tambahAnggota());
        b_update.addActionListener(e -> updateAnggota());
        b_hapus.addActionListener(e -> hapusAnggota());
        b_cari.addActionListener(e -> cariAnggota());

        jTable1.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && jTable1.getSelectedRow() >= 0) {
                int r = jTable1.getSelectedRow();
                t_nis.setText(jTable1.getValueAt(r, 0).toString());
                t_nama.setText(jTable1.getValueAt(r, 1).toString());
                String tipe = jTable1.getValueAt(r, 2) != null ? jTable1.getValueAt(r, 2).toString() : "Siswa";
                for (int i = 0; i < jComboBox1.getItemCount(); i++) {
                    if (jComboBox1.getItemAt(i).equalsIgnoreCase(tipe)) {
                        jComboBox1.setSelectedIndex(i);
                        break;
                    }
                }
                t_buku.setText(jTable1.getValueAt(r, 3) != null ? jTable1.getValueAt(r, 3).toString() : "0");
                t_nomor.setText(jTable1.getValueAt(r, 4) != null ? jTable1.getValueAt(r, 4).toString() : "");
            }
        });
    }

    private void loadTable() {
        DefaultTableModel model = new DefaultTableModel(
            new String[]{"NIS / No Induk", "Nama Lengkap", "Tipe Anggota", "Buku Dipinjam", "No HP"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        try {
            if (con == null || con.isClosed()) initDB();
            String sql = "SELECT a.nomor_induk, p.nama_lengkap, a.tipe_anggota, p.no_hp, "
                       + "(SELECT COUNT(*) FROM peminjaman pm WHERE pm.id_anggota = a.id_anggota AND pm.status_peminjaman = 'Dipinjam') AS pinjam_count "
                       + "FROM anggota a "
                       + "JOIN pengguna p ON a.id_user = p.id_user "
                       + "ORDER BY a.nomor_induk ASC";
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getString("nomor_induk"),
                        rs.getString("nama_lengkap"),
                        rs.getString("tipe_anggota"),
                        rs.getInt("pinjam_count"),
                        rs.getString("no_hp")
                    });
                }
            }
        } catch (SQLException e) {
            try {
                String sqlOld = "SELECT * FROM anggota";
                try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sqlOld)) {
                    while (rs.next()) {
                        model.addRow(new Object[]{
                            rs.getString(1),
                            rs.getString(2),
                            rs.getString(3),
                            0,
                            rs.getString(4)
                        });
                    }
                }
            } catch (Exception ignored) {}
        }
        jTable1.setModel(model);
    }

    private void tambahAnggota() {
        String nis = t_nis.getText().trim();
        String nama = t_nama.getText().trim();
        String tipe = jComboBox1.getSelectedItem() != null ? jComboBox1.getSelectedItem().toString() : "Siswa";
        String noHp = t_nomor.getText().trim();

        if (nis.isEmpty() || nama.isEmpty()) {
            JOptionPane.showMessageDialog(this, "NIS dan Nama Lengkap wajib diisi!");
            return;
        }

        try {
            if (con == null || con.isClosed()) initDB();

            String checkSql = "SELECT id_anggota FROM anggota WHERE nomor_induk = ?";
            try (PreparedStatement pstCheck = con.prepareStatement(checkSql)) {
                pstCheck.setString(1, nis);
                ResultSet rs = pstCheck.executeQuery();
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "Anggota dengan NIS ini sudah terdaftar!");
                    return;
                }
            }

            String dbTipe = tipe.toLowerCase().contains("pegawai") || tipe.toLowerCase().contains("petugas") ? "Petugas" : "Siswa";
            int roleId = dbTipe.equalsIgnoreCase("Petugas") ? 1 : 2;

            String sqlUser = "INSERT INTO pengguna (username, password, nama_lengkap, no_hp, id_role, is_active) VALUES (?, ?, ?, ?, ?, 1)";
            PreparedStatement pstUser = con.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS);
            pstUser.setString(1, nis.toLowerCase());
            pstUser.setString(2, "123456");
            pstUser.setString(3, nama);
            pstUser.setString(4, noHp);
            pstUser.setInt(5, roleId);
            pstUser.executeUpdate();

            ResultSet rsKey = pstUser.getGeneratedKeys();
            int newUserId = 0;
            if (rsKey.next()) {
                newUserId = rsKey.getInt(1);
            }

            String sqlAnggota = "INSERT INTO anggota (id_user, nomor_induk, tipe_anggota, tgl_bergabung) VALUES (?, ?, ?, CURDATE())";
            try (PreparedStatement pstA = con.prepareStatement(sqlAnggota)) {
                pstA.setInt(1, newUserId);
                pstA.setString(2, nis);
                pstA.setString(3, dbTipe);
                pstA.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Data anggota berhasil ditambahkan!");
            resetForm();
            loadTable();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal tambah anggota: " + e.getMessage());
        }
    }

    private void updateAnggota() {
        String nis = t_nis.getText().trim();
        String nama = t_nama.getText().trim();
        String tipe = jComboBox1.getSelectedItem() != null ? jComboBox1.getSelectedItem().toString() : "Siswa";
        String noHp = t_nomor.getText().trim();

        if (nis.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih data anggota yang akan diupdate!");
            return;
        }

        try {
            if (con == null || con.isClosed()) initDB();
            String dbTipe = tipe.toLowerCase().contains("pegawai") || tipe.toLowerCase().contains("petugas") ? "Petugas" : "Siswa";

            String sql = "UPDATE pengguna p JOIN anggota a ON p.id_user = a.id_user "
                       + "SET p.nama_lengkap = ?, p.no_hp = ?, a.tipe_anggota = ? "
                       + "WHERE a.nomor_induk = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, nama);
                pst.setString(2, noHp);
                pst.setString(3, dbTipe);
                pst.setString(4, nis);
                int updated = pst.executeUpdate();
                if (updated > 0) {
                    JOptionPane.showMessageDialog(this, "Data anggota berhasil diupdate!");
                    resetForm();
                    loadTable();
                } else {
                    JOptionPane.showMessageDialog(this, "NIS anggota tidak ditemukan!");
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal update anggota: " + e.getMessage());
        }
    }

    private void hapusAnggota() {
        String nis = t_nis.getText().trim();
        if (nis.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih data anggota yang akan dihapus!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Yakin ingin menghapus anggota dengan NIS " + nis + "?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            if (con == null || con.isClosed()) initDB();
            String findUser = "SELECT id_user FROM anggota WHERE nomor_induk = ?";
            int idUser = 0;
            try (PreparedStatement pstFind = con.prepareStatement(findUser)) {
                pstFind.setString(1, nis);
                ResultSet rs = pstFind.executeQuery();
                if (rs.next()) {
                    idUser = rs.getInt("id_user");
                }
            }

            if (idUser > 0) {
                String delUser = "DELETE FROM pengguna WHERE id_user = ?";
                try (PreparedStatement pstDel = con.prepareStatement(delUser)) {
                    pstDel.setInt(1, idUser);
                    pstDel.executeUpdate();
                }
                JOptionPane.showMessageDialog(this, "Data anggota berhasil dihapus!");
                resetForm();
                loadTable();
            } else {
                JOptionPane.showMessageDialog(this, "NIS tidak ditemukan!");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal hapus anggota: " + e.getMessage());
        }
    }

    private void cariAnggota() {
        String keyword = t_nis.getText().trim();
        if (keyword.isEmpty()) {
            keyword = t_nama.getText().trim();
        }

        if (keyword.isEmpty()) {
            loadTable();
            return;
        }

        DefaultTableModel model = new DefaultTableModel(
            new String[]{"NIS / No Induk", "Nama Lengkap", "Tipe Anggota", "Buku Dipinjam", "No HP"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        try {
            if (con == null || con.isClosed()) initDB();
            String sql = "SELECT a.nomor_induk, p.nama_lengkap, a.tipe_anggota, p.no_hp, "
                       + "(SELECT COUNT(*) FROM peminjaman pm WHERE pm.id_anggota = a.id_anggota AND pm.status_peminjaman = 'Dipinjam') AS pinjam_count "
                       + "FROM anggota a "
                       + "JOIN pengguna p ON a.id_user = p.id_user "
                       + "WHERE a.nomor_induk LIKE ? OR p.nama_lengkap LIKE ? "
                       + "ORDER BY a.nomor_induk ASC";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                String searchPattern = "%" + keyword + "%";
                pst.setString(1, searchPattern);
                pst.setString(2, searchPattern);
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getString("nomor_induk"),
                        rs.getString("nama_lengkap"),
                        rs.getString("tipe_anggota"),
                        rs.getInt("pinjam_count"),
                        rs.getString("no_hp")
                    });
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal mencari data: " + e.getMessage());
        }
        jTable1.setModel(model);
    }

    private void resetForm() {
        t_nis.setText("");
        t_nama.setText("");
        t_buku.setText("");
        t_nomor.setText("");
        if (jComboBox1.getItemCount() > 0) jComboBox1.setSelectedIndex(0);
        jTable1.clearSelection();
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
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        t_nis = new javax.swing.JTextField();
        t_nama = new javax.swing.JTextField();
        t_buku = new javax.swing.JTextField();
        t_nomor = new javax.swing.JTextField();
        b_update = new javax.swing.JButton();
        b_tambah = new javax.swing.JButton();
        b_hapus = new javax.swing.JButton();
        b_cari = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jComboBox1 = new javax.swing.JComboBox<>();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(255, 255, 204));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setText("NIS");

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel2.setText("Nama Lengkap");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setText("Jenis keanggotaan");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel4.setText("Buku Yang Dipinjam");

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel5.setText("Nomor Hp");

        t_nis.addActionListener(this::t_nisActionPerformed);

        t_nama.addActionListener(this::t_namaActionPerformed);

        t_buku.addActionListener(this::t_bukuActionPerformed);

        t_nomor.addActionListener(this::t_nomorActionPerformed);

        b_update.setBackground(new java.awt.Color(204, 204, 204));
        b_update.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        b_update.setText("Update");

        b_tambah.setBackground(new java.awt.Color(204, 204, 204));
        b_tambah.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        b_tambah.setText("Tambah");

        b_hapus.setBackground(new java.awt.Color(204, 204, 204));
        b_hapus.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        b_hapus.setText("Hapus");

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

        jComboBox1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Pegawai", "SIswa" }));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5)
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 169, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(40, 40, 40)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(t_nis, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(b_cari))
                            .addComponent(t_nama, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(t_buku, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(t_nomor, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(40, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(b_update, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(28, 28, 28)
                                .addComponent(b_tambah, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(28, 28, 28)
                                .addComponent(b_hapus, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 595, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(t_nis, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(b_cari))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(t_nama, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(t_buku, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(t_nomor, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(b_update, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(b_tambah, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(b_hapus, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(10, Short.MAX_VALUE))
        );

        jMenu1.setText("Form Anggota");
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

    private void t_nisActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t_nisActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_t_nisActionPerformed

    private void t_namaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t_namaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_t_namaActionPerformed

    private void t_bukuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t_bukuActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_t_bukuActionPerformed

    private void t_nomorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t_nomorActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_t_nomorActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new Form_Anggota().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton b_cari;
    private javax.swing.JButton b_hapus;
    private javax.swing.JButton b_tambah;
    private javax.swing.JButton b_update;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField t_buku;
    private javax.swing.JTextField t_nama;
    private javax.swing.JTextField t_nis;
    private javax.swing.JTextField t_nomor;
    // End of variables declaration//GEN-END:variables
}
