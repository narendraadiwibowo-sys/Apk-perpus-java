/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package sistemperpustakaan;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Acer
 */
public class Form_Denda extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Form_Denda.class.getName());
    private Connection con;

    // Komponen UI tambahan yang dipasang secara dinamis di bawah header asli
    private javax.swing.JPanel jPanelCRUD;
    private javax.swing.JLabel lblIdPinjam, lblHari, lblDenda, lblStatus;
    private javax.swing.JTextField txtIdPinjam, txtHari, txtTotalDenda;
    private javax.swing.JComboBox<String> cmbStatus;
    private javax.swing.JButton btnHitung, btnSimpan, btnBayar, btnHapus, btnRefresh;
    private javax.swing.JTable tableDenda;
    private javax.swing.JScrollPane scrollDenda;

    /**
     * Creates new form Form_Denda
     */
    public Form_Denda() {
        initComponents();
        initKembaliButton();
        initDB();
        initCRUDComponents();
        loadTableDenda();
    }

    private void initDB() {
        try {
            con = Config.configDB();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Koneksi database gagal: " + e.getMessage());
        }
    }

    private void initCRUDComponents() {
        jPanelCRUD = new javax.swing.JPanel();
        jPanelCRUD.setBackground(new java.awt.Color(204, 255, 255));
        jPanelCRUD.setLayout(null);

        lblIdPinjam = new javax.swing.JLabel("No Peminjaman:");
        lblIdPinjam.setFont(new java.awt.Font("Segoe UI", 1, 14));
        lblIdPinjam.setBounds(20, 20, 140, 30);
        jPanelCRUD.add(lblIdPinjam);

        txtIdPinjam = new javax.swing.JTextField();
        txtIdPinjam.setBounds(160, 20, 150, 30);
        jPanelCRUD.add(txtIdPinjam);

        btnHitung = new javax.swing.JButton("Hitung / Cek");
        btnHitung.setBounds(320, 20, 120, 30);
        btnHitung.setBackground(new java.awt.Color(255, 204, 0));
        jPanelCRUD.add(btnHitung);

        lblHari = new javax.swing.JLabel("Hari Terlambat:");
        lblHari.setFont(new java.awt.Font("Segoe UI", 1, 14));
        lblHari.setBounds(20, 60, 140, 30);
        jPanelCRUD.add(lblHari);

        txtHari = new javax.swing.JTextField();
        txtHari.setEditable(false);
        txtHari.setBounds(160, 60, 150, 30);
        jPanelCRUD.add(txtHari);

        lblDenda = new javax.swing.JLabel("Total Denda (Rp):");
        lblDenda.setFont(new java.awt.Font("Segoe UI", 1, 14));
        lblDenda.setBounds(20, 100, 140, 30);
        jPanelCRUD.add(lblDenda);

        txtTotalDenda = new javax.swing.JTextField();
        txtTotalDenda.setBounds(160, 100, 150, 30);
        jPanelCRUD.add(txtTotalDenda);

        lblStatus = new javax.swing.JLabel("Status Bayar:");
        lblStatus.setFont(new java.awt.Font("Segoe UI", 1, 14));
        lblStatus.setBounds(20, 140, 140, 30);
        jPanelCRUD.add(lblStatus);

        cmbStatus = new javax.swing.JComboBox<>(new String[]{"Belum Lunas", "Lunas"});
        cmbStatus.setBounds(160, 140, 150, 30);
        jPanelCRUD.add(cmbStatus);

        btnSimpan = new javax.swing.JButton("Simpan");
        btnSimpan.setBackground(new java.awt.Color(102, 255, 102));
        btnSimpan.setBounds(20, 190, 90, 35);
        jPanelCRUD.add(btnSimpan);

        btnBayar = new javax.swing.JButton("Lunasi");
        btnBayar.setBackground(new java.awt.Color(0, 153, 255));
        btnBayar.setForeground(java.awt.Color.WHITE);
        btnBayar.setBounds(120, 190, 90, 35);
        jPanelCRUD.add(btnBayar);

        btnHapus = new javax.swing.JButton("Hapus");
        btnHapus.setBackground(new java.awt.Color(255, 51, 51));
        btnHapus.setForeground(java.awt.Color.WHITE);
        btnHapus.setBounds(220, 190, 90, 35);
        jPanelCRUD.add(btnHapus);

        btnRefresh = new javax.swing.JButton("Refresh");
        btnRefresh.setBounds(320, 190, 90, 35);
        jPanelCRUD.add(btnRefresh);

        tableDenda = new javax.swing.JTable();
        scrollDenda = new javax.swing.JScrollPane(tableDenda);
        scrollDenda.setBounds(460, 20, 380, 220);
        jPanelCRUD.add(scrollDenda);

        jPanel1.setLayout(new java.awt.BorderLayout());
        jPanel1.add(jPanel2, java.awt.BorderLayout.NORTH);
        jPanel1.add(jPanelCRUD, java.awt.BorderLayout.CENTER);
        setSize(880, 420);
        setLocationRelativeTo(null);

        btnHitung.addActionListener(e -> hitungDenda());
        btnSimpan.addActionListener(e -> simpanDenda());
        btnBayar.addActionListener(e -> bayarDenda());
        btnHapus.addActionListener(e -> hapusDenda());
        btnRefresh.addActionListener(e -> {
            resetForm();
            loadTableDenda();
        });

        tableDenda.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tableDenda.getSelectedRow() >= 0) {
                int r = tableDenda.getSelectedRow();
                txtIdPinjam.setText(tableDenda.getValueAt(r, 1).toString());
                txtHari.setText(tableDenda.getValueAt(r, 2).toString());
                String total = tableDenda.getValueAt(r, 3).toString().replace("Rp ", "").replace(",", "");
                txtTotalDenda.setText(total);
                cmbStatus.setSelectedItem(tableDenda.getValueAt(r, 4).toString());
            }
        });
    }

    private void loadTableDenda() {
        DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID Denda", "No Pinjam", "Hari Telat", "Total Denda", "Status"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        try {
            if (con == null || con.isClosed()) initDB();

            String sql = "SELECT d.id_denda, p.kode_transaksi, d.jumlah_hari_terlambat, d.total_denda, d.status_bayar "
                       + "FROM denda d "
                       + "JOIN detail_peminjaman dp ON d.id_detail = dp.id_detail "
                       + "JOIN peminjaman p ON dp.id_peminjaman = p.id_peminjaman "
                       + "ORDER BY d.id_denda DESC";
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getInt("id_denda"),
                        rs.getString("kode_transaksi"),
                        rs.getInt("jumlah_hari_terlambat"),
                        "Rp " + String.format("%,.0f", rs.getDouble("total_denda")),
                        rs.getString("status_bayar")
                    });
                }
            }
        } catch (SQLException e) {
            try {
                String sqlOld = "SELECT id_denda, id_pinjam, hari_terlambat, total_denda, status_bayar FROM denda";
                try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sqlOld)) {
                    while (rs.next()) {
                        model.addRow(new Object[]{
                            rs.getInt(1),
                            rs.getString(2),
                            rs.getInt(3),
                            "Rp " + rs.getDouble(4),
                            rs.getString(5)
                        });
                    }
                }
            } catch (Exception ignored) {}
        }
        tableDenda.setModel(model);
    }

    private void hitungDenda() {
        String noPinjam = txtIdPinjam.getText().trim();
        if (noPinjam.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Masukkan No / Kode Peminjaman!");
            return;
        }

        try {
            if (con == null || con.isClosed()) initDB();
            String sql = "SELECT tgl_tenggat FROM peminjaman WHERE kode_transaksi = ? OR CAST(id_peminjaman AS CHAR) = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, noPinjam);
                pst.setString(2, noPinjam);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    Date tenggat = rs.getDate("tgl_tenggat");
                    long selisih = new Date().getTime() - (tenggat != null ? tenggat.getTime() : new Date().getTime());
                    long hari = selisih > 0 ? selisih / (1000 * 60 * 60 * 24) : 0;
                    double total = hari * 1000.0;
                    txtHari.setText(String.valueOf(hari));
                    txtTotalDenda.setText(String.format("%.0f", total));
                    if (hari == 0) {
                        JOptionPane.showMessageDialog(this, "Tidak ada keterlambatan (Denda Rp 0).");
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "No Peminjaman tidak ditemukan!");
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error hitung denda: " + e.getMessage());
        }
    }

    private void simpanDenda() {
        String noPinjam = txtIdPinjam.getText().trim();
        String totalStr = txtTotalDenda.getText().trim();
        String hariStr = txtHari.getText().trim();
        String status = cmbStatus.getSelectedItem() != null ? cmbStatus.getSelectedItem().toString() : "Belum Lunas";

        if (noPinjam.isEmpty() || totalStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Lengkapi data denda terlebih dahulu!");
            return;
        }

        try {
            if (con == null || con.isClosed()) initDB();
            int hari = hariStr.isEmpty() ? 0 : Integer.parseInt(hariStr);
            double total = Double.parseDouble(totalStr);

            String sqlFind = "SELECT dp.id_detail FROM detail_peminjaman dp "
                           + "JOIN peminjaman p ON dp.id_peminjaman = p.id_peminjaman "
                           + "WHERE p.kode_transaksi = ? OR CAST(p.id_peminjaman AS CHAR) = ?";
            int idDetail = 0;
            try (PreparedStatement pst = con.prepareStatement(sqlFind)) {
                pst.setString(1, noPinjam);
                pst.setString(2, noPinjam);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    idDetail = rs.getInt("id_detail");
                }
            }

            if (idDetail > 0) {
                String sqlIns = "INSERT INTO denda (id_detail, jumlah_hari_terlambat, tarif_per_hari, total_denda, status_bayar) "
                              + "VALUES (?, ?, 1000.00, ?, ?)";
                try (PreparedStatement pstIns = con.prepareStatement(sqlIns)) {
                    pstIns.setInt(1, idDetail);
                    pstIns.setInt(2, hari);
                    pstIns.setDouble(3, total);
                    pstIns.setString(4, status);
                    pstIns.executeUpdate();
                }
            } else {
                String sqlFlat = "INSERT INTO denda (id_pinjam, hari_terlambat, total_denda, status_bayar) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstFlat = con.prepareStatement(sqlFlat)) {
                    pstFlat.setString(1, noPinjam);
                    pstFlat.setInt(2, hari);
                    pstFlat.setDouble(3, total);
                    pstFlat.setString(4, status);
                    pstFlat.executeUpdate();
                }
            }

            JOptionPane.showMessageDialog(this, "Denda berhasil disimpan!");
            resetForm();
            loadTableDenda();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal simpan denda: " + e.getMessage());
        }
    }

    private void bayarDenda() {
        int r = tableDenda.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Pilih denda dari tabel terlebih dahulu!");
            return;
        }

        int idDenda = Integer.parseInt(tableDenda.getValueAt(r, 0).toString());
        try {
            if (con == null || con.isClosed()) initDB();
            String sql = "UPDATE denda SET status_bayar = 'Lunas', tgl_bayar = NOW() WHERE id_denda = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, idDenda);
                pst.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Denda berhasil dilunasi!");
            resetForm();
            loadTableDenda();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal melunasi denda: " + e.getMessage());
        }
    }

    private void hapusDenda() {
        int r = tableDenda.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Pilih denda dari tabel terlebih dahulu!");
            return;
        }

        int idDenda = Integer.parseInt(tableDenda.getValueAt(r, 0).toString());
        int konfirm = JOptionPane.showConfirmDialog(this, "Hapus data denda ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (konfirm != JOptionPane.YES_OPTION) return;

        try {
            if (con == null || con.isClosed()) initDB();
            String sql = "DELETE FROM denda WHERE id_denda = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, idDenda);
                pst.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Data denda berhasil dihapus!");
            resetForm();
            loadTableDenda();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal hapus denda: " + e.getMessage());
        }
    }

    private void resetForm() {
        txtIdPinjam.setText("");
        txtHari.setText("");
        txtTotalDenda.setText("");
        cmbStatus.setSelectedIndex(0);
        tableDenda.clearSelection();
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

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(0, 102, 204));

        jPanel2.setBackground(new java.awt.Color(51, 255, 255));

        jLabel1.setFont(new java.awt.Font("sansserif", 1, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(0, 51, 204));
        jLabel1.setText("FORM DENDA");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(352, Short.MAX_VALUE)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(337, 337, 337))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addComponent(jLabel1)
                .addContainerGap(44, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 339, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

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
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Form_Denda.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Form_Denda.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Form_Denda.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Form_Denda.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Form_Denda().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
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