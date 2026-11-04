package sistemperpustakaan;

import java.sql.*;
import javax.swing.*;

public class koneksi {
    Connection con;
    Statement stm;
    
    public void config() {
        try {
            con = Config.configDB();
            if (con != null) {
                stm = con.createStatement();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "koneksi gagal: " + e.getMessage());
        }
    }
}
