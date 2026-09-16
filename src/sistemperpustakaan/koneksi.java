package sistemperpustakaan;

import java.sql.*;
import javax.swing.*;

class koneksi {
    Connection con;
    Statement stm;
    
    public void config(){
       try{
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/simperpus","root","");
            stm = con.createStatement();
        } catch (SQLException e){
        JOptionPane.showMessageDialog(null, "koneksi gagal"+ e.getMessage());
        }
    }
    
}
