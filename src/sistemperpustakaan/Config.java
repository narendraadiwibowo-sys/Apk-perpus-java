package sistemperpustakaan;
import java.sql.*;
public class Config {
    private static Connection mysqlconfig;
    public static Connection configDB()throws SQLException{
        try {
            String url="jdbc:mysql://localhost:3306/simperpus"; //url database
            String user="root"; //user database
            String pass=""; //password database
            Class.forName("com.mysql.jdbc.Driver");
            mysqlconfig = DriverManager.getConnection(url, user, pass);
            System.out.println("Koneksi berhasil!");            
        } catch (Exception e) {
            System.err.println("koneksi gagal "+e.getMessage()); 
        }
        return mysqlconfig;
    }    
}