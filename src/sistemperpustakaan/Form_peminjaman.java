package sistemperpustakaan;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;

/**
 *
 * @author PC-1B LKS-2026
 */
public class Form_peminjaman extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Form_peminjaman.class.getName());

    // Koneksi database
    private Connection con;
    // Format tanggal yang dipakai di form
    private static final String DATE_FORMAT = "dd/MM/yyyy";

    /**
     * Creates new form Form_peminjaman
     */
    public Form_peminjaman() {
        initComponents();
        initKembaliButton();
        initDB();
        setDefaultTanggal();
        setupAutoFill();
        setupButtonPinjam();
        setupButtonBatal();
    }

    // ===================== INISIALISASI DB =====================
    private void initDB() {
        try {
            con = Config.configDB();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Koneksi database gagal: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===================== Set tanggal hari ini & default kembali =====================
    private void setDefaultTanggal() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        String today = sdf.format(new Date());
        // Tanggal pinjam = hari ini
        jTextField6.setText(today);
        // Tanggal kembali = 7 hari ke depan
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_MONTH, 7);
        jTextField5.setText(sdf.format(cal.getTime()));
    }

    // ===================== Auto-fill nama anggota saat ID diisi =====================
    private void setupAutoFill() {
        // Listener untuk jTextField1 (ID Anggota) → cari nama otomatis
        jTextField1.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                autoFillNama(jTextField1.getText().trim());
            }
        });
        // Listener untuk jTextField4 (Kode Buku) → cari judul otomatis
        jTextField4.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                autoFillJudul(jTextField4.getText().trim());
            }
        });
    }

    // ===================== Auto-fill nama anggota =====================
    private void autoFillNama(String idAnggota) {
        if (idAnggota.isEmpty()) return;
        try {
            if (con == null || con.isClosed()) initDB();
            String sql = "SELECT nama FROM anggota WHERE id_anggota = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, idAnggota);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    jTextField2.setText(rs.getString("nama"));
                } else {
                    jTextField2.setText("");
                    JOptionPane.showMessageDialog(this, "ID Anggota tidak ditemukan!", "Info", JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===================== Auto-fill judul buku =====================
    private void autoFillJudul(String kodeBuku) {
        if (kodeBuku.isEmpty()) return;
        try {
            if (con == null || con.isClosed()) initDB();
            String sql = "SELECT judul, stok FROM buku WHERE kode_buku = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, kodeBuku);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int stok = rs.getInt("stok");
                    if (stok <= 0) {
                        jTextField3.setText("");
                        JOptionPane.showMessageDialog(this, "Stok buku habis!", "Info", JOptionPane.WARNING_MESSAGE);
                    } else {
                        jTextField3.setText(rs.getString("judul"));
                    }
                } else {
                    jTextField3.setText("");
                    JOptionPane.showMessageDialog(this, "Kode Buku tidak ditemukan!", "Info", JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===================== Setup tombol Pinjam Buku =====================
    private void setupButtonPinjam() {
        jButton1.addActionListener(e -> pinjamBuku());
    }

    // ===================== Setup tombol Batal =====================
    private void setupButtonBatal() {
        jButton2.addActionListener(e -> {
            int konfirmasi = JOptionPane.showConfirmDialog(this, "Batalkan dan tutup form?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
            if (konfirmasi == JOptionPane.YES_OPTION) {
                resetForm();
            }
        });
    }

    // ===================== CREATE - Pinjam Buku =====================
    private void pinjamBuku() {
        String idAnggota = jTextField1.getText().trim();
        String namaPeminjam = jTextField2.getText().trim();
        String kodeBuku = jTextField4.getText().trim();
        String judulBuku = jTextField3.getText().trim();
        String tglPinjamStr = jTextField6.getText().trim();
        String tglKembaliStr = jTextField5.getText().trim();

        // Validasi input
        if (idAnggota.isEmpty() || kodeBuku.isEmpty()) {
            JOptionPane.showMessageDialog(this, "ID Anggota dan Kode Buku wajib diisi!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (judulBuku.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kode Buku tidak valid atau stok habis!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Parse tanggal
        java.sql.Date tglPinjam, tglKembali;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            tglPinjam   = new java.sql.Date(sdf.parse(tglPinjamStr).getTime());
            tglKembali  = new java.sql.Date(sdf.parse(tglKembaliStr).getTime());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Format tanggal salah! Gunakan dd/MM/yyyy", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (con == null || con.isClosed()) initDB();
            con.setAutoCommit(false);

            // 1. Cek apakah anggota sudah pinjam buku yang sama dan belum dikembalikan
            String cekPinjam = "SELECT id_pinjam FROM peminjaman WHERE id_anggota=? AND kode_buku=? AND status='dipinjam'";
            try (PreparedStatement ps = con.prepareStatement(cekPinjam)) {
                ps.setString(1, idAnggota);
                ps.setString(2, kodeBuku);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "Anggota masih meminjam buku ini!", "Info", JOptionPane.WARNING_MESSAGE);
                    con.setAutoCommit(true);
                    return;
                }
            }

            // 2. Insert peminjaman
            String sqlInsert = "INSERT INTO peminjaman (id_anggota, nama_peminjam, kode_buku, judul_buku, tgl_pinjam, tgl_kembali, status) VALUES (?,?,?,?,?,?,'dipinjam')";
            try (PreparedStatement ps = con.prepareStatement(sqlInsert)) {
                ps.setString(1, idAnggota);
                ps.setString(2, namaPeminjam);
                ps.setString(3, kodeBuku);
                ps.setString(4, judulBuku);
                ps.setDate(5, tglPinjam);
                ps.setDate(6, tglKembali);
                ps.executeUpdate();
            }

            // 3. Kurangi stok buku
            String sqlStok = "UPDATE buku SET stok = stok - 1 WHERE kode_buku = ? AND stok > 0";
            try (PreparedStatement ps = con.prepareStatement(sqlStok)) {
                ps.setString(1, kodeBuku);
                ps.executeUpdate();
            }

            con.commit();
            con.setAutoCommit(true);
            JOptionPane.showMessageDialog(this, "Peminjaman berhasil disimpan!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            resetForm();

        } catch (SQLException e) {
            try { con.rollback(); con.setAutoCommit(true); } catch (SQLException ex) { /* ignore */ }
            JOptionPane.showMessageDialog(this, "Gagal menyimpan peminjaman: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===================== RESET form =====================
    private void resetForm() {
        jTextField1.setText("");
        jTextField2.setText("");
        jTextField3.setText("");
        jTextField4.setText("");
        setDefaultTanggal();
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
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        jTextField3 = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();
        jTextField5 = new javax.swing.JTextField();
        jTextField6 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(153, 0, 51));

        jPanel2.setBackground(new java.awt.Color(255, 204, 0));

        jLabel2.setFont(new java.awt.Font("sansserif", 1, 24)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(51, 204, 0));
        jLabel2.setText("FORM PEMINJAMAN BUKU");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(266, Short.MAX_VALUE)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 334, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(217, 217, 217))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(44, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addGap(40, 40, 40))
        );

        jLabel1.setFont(new java.awt.Font("sansserif", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(51, 204, 0));
        jLabel1.setText("ID ANGGOTA:");

        jLabel3.setFont(new java.awt.Font("sansserif", 1, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(51, 204, 0));
        jLabel3.setText("Nama Peminjam:");

        jLabel4.setFont(new java.awt.Font("sansserif", 1, 18)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(51, 204, 0));
        jLabel4.setText("Kode Buku:");

        jLabel5.setFont(new java.awt.Font("sansserif", 1, 18)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(51, 204, 0));
        jLabel5.setText("Judul Buku:");

        jLabel6.setFont(new java.awt.Font("sansserif", 1, 18)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(51, 204, 0));
        jLabel6.setText("Tanggal Kembali:");

        jLabel7.setFont(new java.awt.Font("sansserif", 1, 18)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(51, 204, 0));
        jLabel7.setText("Tanggal Pinjam:");

        jTextField1.addActionListener(this::jTextField1ActionPerformed);

        jTextField4.addActionListener(this::jTextField4ActionPerformed);

        jTextField5.setText("03/11/2026");

        jTextField6.setText("03/11/2026");

        jButton1.setBackground(new java.awt.Color(153, 255, 0));
        jButton1.setForeground(new java.awt.Color(0, 0, 0));
        jButton1.setText("Pinjam Buku");

        jButton2.setBackground(new java.awt.Color(255, 0, 51));
        jButton2.setForeground(new java.awt.Color(0, 0, 0));
        jButton2.setText("Batal");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 133, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 292, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 159, Short.MAX_VALUE))
                                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 169, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, 292, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 292, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, 292, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 292, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 292, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                            .addGap(166, 166, 166)
                            .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(41, 41, 41)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(38, 38, 38)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(40, 40, 40)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel7)
                    .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 69, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addGap(45, 45, 45))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField4ActionPerformed
        // Enter di kode buku → auto-fill judul
        autoFillJudul(jTextField4.getText().trim());
    }//GEN-LAST:event_jTextField4ActionPerformed

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // Enter di ID anggota → auto-fill nama
        autoFillNama(jTextField1.getText().trim());
    }//GEN-LAST:event_jTextField1ActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new Form_peminjaman().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
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