package sistemperpustakaan;

import java.sql.*;

public class Config {
    private static Connection mysqlconfig;
    
    public static Connection configDB() throws SQLException {
        try {
            if (mysqlconfig != null && !mysqlconfig.isClosed()) {
                return mysqlconfig;
            }

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                Class.forName("com.mysql.jdbc.Driver");
            }

            String user = "root";
            String pass = "";
            String[] possibleDbs = {"db_perpustakaan", "simperpus", "simperp"};

            for (String dbName : possibleDbs) {
                try {
                    String url = "jdbc:mysql://localhost:3306/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
                    Connection conn = DriverManager.getConnection(url, user, pass);
                    if (conn != null) {
                        mysqlconfig = conn;
                        System.out.println("Koneksi berhasil ke database: " + dbName);
                        return mysqlconfig;
                    }
                } catch (SQLException ignored) {
                    // Coba database berikutnya jika belum ada
                }
            }
        } catch (Exception e) {
            System.err.println("koneksi gagal: " + e.getMessage()); 
        }
        return mysqlconfig;
    }    
}
