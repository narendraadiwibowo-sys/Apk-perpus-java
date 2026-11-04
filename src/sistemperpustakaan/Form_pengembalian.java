package sistemperpustakaan;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author PC-1B LKS-2026
 */
public class Form_pengembalian extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Form_pengembalian.class.getName());
    private Connection con;

    /**
     * Creates new form Form_pengembalian
     */
    public Form_pengembalian() {
        initComponents();
        initKembaliButton();
        initDB();
        initCustomListeners();
    }

    private void initDB() {
        try {
            con = Config.configDB();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Koneksi database gagal: " + e.getMessage());
        }
    }

    private void initCustomListeners() {
        // Otomatis cek saat Enter di kolom Nomor Peminjaman
        t_nomor.addActionListener(e -> cekPeminjaman());
        t_nis.addActionListener(e -> cekPeminjaman());

        // Tombol "ya" untuk memproses pengembalian
        b_ya.addActionListener(e -> prosesPengembalian());
    }

    private void cekPeminjaman() {
        String noPinjam = t_nomor.getText().trim();
        if (noPinjam.isEmpty()) return;

        try {
            if (con == null || con.isClosed()) initDB();

            // Coba cari di skema relasional terlebih dahulu
            String sql = "SELECT p.kode_transaksi, a.nomor_induk, p.tgl_tenggat, b.judul "
                       + "FROM peminjaman p "
                       + "JOIN anggota a ON p.id_anggota = a.id_anggota "
                       + "LEFT JOIN detail_peminjaman dp ON p.id_peminjaman = dp.id_peminjaman "
                       + "LEFT JOIN item_buku ib ON dp.id_item = ib.id_item "
                       + "LEFT JOIN buku b ON ib.id_buku = b.id_buku "
                       + "WHERE (p.kode_transaksi = ? OR CAST(p.id_peminjaman AS CHAR) = ?) "
                       + "AND p.status_peminjaman = 'Dipinjam'";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, noPinjam);
            pst.setString(2, noPinjam);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                t_nis.setText(rs.getString("nomor_induk"));
                String judul = rs.getString("judul") != null ? rs.getString("judul") : "Buku";
                Date tglTenggat = rs.getDate("tgl_tenggat");
                long selisih = new Date().getTime() - (tglTenggat != null ? tglTenggat.getTime() : new Date().getTime());
                long terlambat = selisih > 0 ? selisih / (1000 * 60 * 60 * 24) : 0;

                String info = "Peminjaman Ditemukan:\nJudul: " + judul + "\nTenggat: " + tglTenggat;
                if (terlambat > 0) {
                    long denda = terlambat * 1000;
                    info += "\nTerlambat: " + terlambat + " hari (Denda: Rp " + denda + ")";
                }
                JOptionPane.showMessageDialog(this, info, "Info Peminjaman", JOptionPane.INFORMATION_MESSAGE);
            } else {
                // Coba cek di skema peminjaman sederhana (fallback)
                String sqlFlat = "SELECT * FROM peminjaman WHERE (id_pinjam = ? OR id_anggota = ?) AND status = 'dipinjam'";
                try (PreparedStatement pstFlat = con.prepareStatement(sqlFlat)) {
                    pstFlat.setString(1, noPinjam);
                    pstFlat.setString(2, noPinjam);
                    ResultSet rsFlat = pstFlat.executeQuery();
                    if (rsFlat.next()) {
                        t_nis.setText(rsFlat.getString("id_anggota"));
                        JOptionPane.showMessageDialog(this, "Peminjaman ditemukan atas nama: " + rsFlat.getString("nama_peminjam"));
                    } else {
                        JOptionPane.showMessageDialog(this, "Data peminjaman tidak ditemukan atau buku sudah dikembalikan!");
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal cek peminjaman: " + e.getMessage());
        }
    }

    private void prosesPengembalian() {
        String noPinjam = t_nomor.getText().trim();
        String nis = t_nis.getText().trim();
        String kondisi = c_kondisi.getSelectedItem() != null ? c_kondisi.getSelectedItem().toString() : "Baik";

        if (noPinjam.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Silakan isi Nomor Peminjaman!");
            return;
        }

        try {
            if (con == null || con.isClosed()) initDB();

            // 1. Coba di skema database relasional
            String checkRelational = "SELECT p.id_peminjaman, p.tgl_tenggat, dp.id_detail, dp.id_item "
                                   + "FROM peminjaman p "
                                   + "JOIN detail_peminjaman dp ON p.id_peminjaman = dp.id_peminjaman "
                                   + "WHERE (p.kode_transaksi = ? OR CAST(p.id_peminjaman AS CHAR) = ?) "
                                   + "AND p.status_peminjaman = 'Dipinjam'";
            PreparedStatement pstRel = con.prepareStatement(checkRelational);
            pstRel.setString(1, noPinjam);
            pstRel.setString(2, noPinjam);
            ResultSet rsRel = pstRel.executeQuery();

            if (rsRel.next()) {
                int idPeminjaman = rsRel.getInt("id_peminjaman");
                int idDetail = rsRel.getInt("id_detail");
                int idItem = rsRel.getInt("id_item");
                Date tglTenggat = rsRel.getDate("tgl_tenggat");

                long selisih = new Date().getTime() - (tglTenggat != null ? tglTenggat.getTime() : new Date().getTime());
                long terlambat = selisih > 0 ? selisih / (1000 * 60 * 60 * 24) : 0;
                double tarifPerHari = 1000.0;
                double totalDenda = terlambat * tarifPerHari;

                // Tambahkan denda jika kondisi rusak/hilang
                if (kondisi.equalsIgnoreCase("Rusak")) totalDenda += 25000;
                if (kondisi.equalsIgnoreCase("Hilang")) totalDenda += 50000;

                // Update detail peminjaman
                String updDetail = "UPDATE detail_peminjaman SET tgl_kembali = NOW(), status_item = ? WHERE id_detail = ?";
                PreparedStatement pstUpdDetail = con.prepareStatement(updDetail);
                String statusItemStr = kondisi.equalsIgnoreCase("Baik") ? "Dikembalikan" : kondisi;
                pstUpdDetail.setString(1, statusItemStr);
                pstUpdDetail.setInt(2, idDetail);
                pstUpdDetail.executeUpdate();

                // Update peminjaman status
                String updPinjam = "UPDATE peminjaman SET status_peminjaman = 'Selesai' WHERE id_peminjaman = ?";
                PreparedStatement pstUpdPinjam = con.prepareStatement(updPinjam);
                pstUpdPinjam.setInt(1, idPeminjaman);
                pstUpdPinjam.executeUpdate();

                // Update status item buku dan stok buku
                String itemStatus = kondisi.equalsIgnoreCase("Baik") ? "Tersedia" : kondisi;
                String updItem = "UPDATE item_buku SET status_item = ?, kondisi_buku = ? WHERE id_item = ?";
                PreparedStatement pstUpdItem = con.prepareStatement(updItem);
                pstUpdItem.setString(1, itemStatus);
                pstUpdItem.setString(2, kondisi);
                pstUpdItem.setInt(3, idItem);
                pstUpdItem.executeUpdate();

                // Jika ada denda, catat ke tabel denda
                if (totalDenda > 0) {
                    String insDenda = "INSERT INTO denda (id_detail, jumlah_hari_terlambat, tarif_per_hari, total_denda, status_bayar) "
                                    + "VALUES (?, ?, ?, ?, 'Belum Lunas')";
                    PreparedStatement pstDenda = con.prepareStatement(insDenda);
                    pstDenda.setInt(1, idDetail);
                    pstDenda.setInt(2, (int) terlambat);
                    pstDenda.setDouble(3, tarifPerHari);
                    pstDenda.setDouble(4, totalDenda);
                    pstDenda.executeUpdate();

                    JOptionPane.showMessageDialog(this, "Pengembalian berhasil dicatat!\nTerlambat: " + terlambat + " hari\nTotal Denda: Rp " + totalDenda);
                } else {
                    JOptionPane.showMessageDialog(this, "Buku berhasil dikembalikan tepat waktu tanpa denda!");
                }

                resetForm();
                return;
            }

            // 2. Fallback ke skema peminjaman sederhana jika skema relasi tidak menemukan data
            String updSimple = "UPDATE peminjaman SET status = 'dikembalikan' WHERE (id_pinjam = ? OR id_anggota = ?) AND status = 'dipinjam'";
            try (PreparedStatement pstSimple = con.prepareStatement(updSimple)) {
                pstSimple.setString(1, noPinjam);
                pstSimple.setString(2, noPinjam);
                int affected = pstSimple.executeUpdate();
                if (affected > 0) {
                    JOptionPane.showMessageDialog(this, "Pengembalian buku berhasil diproses!");
                    resetForm();
                } else {
                    JOptionPane.showMessageDialog(this, "Data peminjaman tidak ditemukan atau telah dikembalikan.");
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal memproses pengembalian: " + e.getMessage());
        }
    }

    private void resetForm() {
        t_nomor.setText("");
        t_nis.setText("");
        c_kondisi.setSelectedIndex(0);
        b_ya.setSelected(false);
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
        t_nomor = new javax.swing.JTextField();
        t_nis = new javax.swing.JTextField();
        c_kondisi = new javax.swing.JComboBox<>();
        b_ya = new javax.swing.JToggleButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(255, 255, 204));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setText("Nomor Peminjaman");

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel2.setText("NIS");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setText("Kondisi Buku");

        t_nomor.addActionListener(this::t_nomorActionPerformed);

        t_nis.addActionListener(this::t_nisActionPerformed);

        c_kondisi.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Baik", "Rusak", "Hilang", " " }));

        b_ya.setText("ya");
        b_ya.addActionListener(this::b_yaActionPerformed);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(t_nomor, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(t_nis, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(c_kondisi, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(56, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(b_ya, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(46, 46, 46))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(t_nomor, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(17, 17, 17)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(t_nis, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(17, 17, 17)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(c_kondisi, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(b_ya, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(16, Short.MAX_VALUE))
        );

        jMenu1.setText("Form Pengembalian");
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
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void t_nomorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t_nomorActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_t_nomorActionPerformed

    private void t_nisActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t_nisActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_t_nisActionPerformed

    private void b_yaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_b_yaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_b_yaActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new Form_pengembalian().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton b_ya;
    private javax.swing.JComboBox<String> c_kondisi;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField t_nis;
    private javax.swing.JTextField t_nomor;
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