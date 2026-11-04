package sistemperpustakaan;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;

public class Config {
    private static Connection mysqlconfig;
    
    public static Connection configDB() throws SQLException {
        try {
            if (mysqlconfig != null && !mysqlconfig.isClosed()) {
                return mysqlconfig;
            }

            // 1. Pastikan Driver JDBC ter-load dengan baik
            boolean driverLoaded = false;
            String[] driverClasses = {
                "com.mysql.cj.jdbc.Driver",
                "com.mysql.jdbc.Driver"
            };

            for (String driverClass : driverClasses) {
                try {
                    Class.forName(driverClass);
                    driverLoaded = true;
                    break;
                } catch (ClassNotFoundException ignored) {}
            }

            // Jika driver belum ada di standard classpath (misal saat NetBeans Run File terpisah), load dari file jar lokal
            if (!driverLoaded) {
                String[] potentialJars = {
                    "e:/PBO 2 new/sistem perpus/SistemPerpustakaan(Dave)/lib/mysql-connector-j.jar",
                    "e:/PBO 2 new/sistem perpus/SistemPerpustakaanPintar/lib/mysql-connector-j.jar",
                    "e:/PBO 2 new/sistem perpus/lib/mysql-connector-j.jar",
                    "e:/Connector/mysql-connector-j-8.0.33/mysql-connector-j-8.0.33.jar",
                    "C:/mysql-connector-j-9.4.0/mysql-connector-j-9.4.0.jar"
                };

                for (String jarPath : potentialJars) {
                    File jarFile = new File(jarPath);
                    if (jarFile.exists()) {
                        try {
                            URLClassLoader loader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, Config.class.getClassLoader());
                            Driver d = (Driver) Class.forName("com.mysql.cj.jdbc.Driver", true, loader).getDeclaredConstructor().newInstance();
                            DriverManager.registerDriver(new DriverShim(d));
                            driverLoaded = true;
                            System.out.println("Driver MySQL berhasil dimuat dari: " + jarPath);
                            break;
                        } catch (Exception ex) {
                            // Coba driver legacy jika cj gagal
                            try {
                                URLClassLoader loader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, Config.class.getClassLoader());
                                Driver d = (Driver) Class.forName("com.mysql.jdbc.Driver", true, loader).getDeclaredConstructor().newInstance();
                                DriverManager.registerDriver(new DriverShim(d));
                                driverLoaded = true;
                                break;
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }

            String user = "root";
            String pass = "";
            String[] possibleDbs = {"db_perpustakaan", "simperpus", "simperp"};

            for (String dbName : possibleDbs) {
                try {
                    String url = "jdbc:mysql://localhost:3306/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jakarta";
                    Connection conn = DriverManager.getConnection(url, user, pass);
                    if (conn != null) {
                        mysqlconfig = conn;
                        System.out.println("Koneksi berhasil ke database: " + dbName);
                        return mysqlconfig;
                    }
                } catch (SQLException ignored) {
                    // Lanjut mencoba database lainnya
                }
            }

            if (mysqlconfig == null) {
                throw new SQLException("Tidak dapat terhubung ke database (db_perpustakaan / simperpus). Pastikan MySQL/XAMPP/Laragon aktif!");
            }

        } catch (Exception e) {
            System.err.println("koneksi gagal: " + e.getMessage()); 
            throw new SQLException(e.getMessage());
        }
        return mysqlconfig;
    }

    // Helper class agar Driver dari external ClassLoader bisa didaftarkan ke DriverManager
    private static class DriverShim implements Driver {
        private final Driver driver;
        DriverShim(Driver d) { this.driver = d; }
        public boolean acceptsURL(String u) throws SQLException { return this.driver.acceptsURL(u); }
        public Connection connect(String u, java.util.Properties p) throws SQLException { return this.driver.connect(u, p); }
        public int getMajorVersion() { return this.driver.getMajorVersion(); }
        public int getMinorVersion() { return this.driver.getMinorVersion(); }
        public DriverPropertyInfo[] getPropertyInfo(String u, java.util.Properties p) throws SQLException { return this.driver.getPropertyInfo(u, p); }
        public boolean jdbcCompliant() { return this.driver.jdbcCompliant(); }
        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException { return this.driver.getParentLogger(); }
    }
}
