package sistemperpustakaan;

/**
 * SessionManager - Menyimpan data sesi login pengguna secara global (static)
 * Diset saat login berhasil di FormLogin, dibaca oleh Dashboard dan Form lain.
 *
 * @author PC-1B LKS-2026
 */
public class SessionManager {

    // Data pengguna yang sedang login
    public static int    loggedUserId       = -1;
    public static String loggedUsername     = "";
    public static String loggedNamaLengkap  = "";
    public static String loggedRole         = "";   // "Petugas" atau "Siswa"
    public static int    loggedRoleId       = -1;   // 1=Petugas, 2=Siswa

    /** Simpan sesi setelah login berhasil */
    public static void setSession(int userId, String username, String namaLengkap, String role, int roleId) {
        loggedUserId      = userId;
        loggedUsername    = username;
        loggedNamaLengkap = namaLengkap;
        loggedRole        = role;
        loggedRoleId      = roleId;
    }

    /** Hapus sesi saat logout */
    public static void clearSession() {
        loggedUserId      = -1;
        loggedUsername    = "";
        loggedNamaLengkap = "";
        loggedRole        = "";
        loggedRoleId      = -1;
    }

    /** Cek apakah ada sesi aktif */
    public static boolean isLoggedIn() {
        return loggedUserId != -1;
    }

    /** Kembalikan teks ringkasan profil untuk ditampilkan di dashboard */
    public static String getProfileText() {
        if (!isLoggedIn()) return "Belum Login";
        return loggedNamaLengkap + " (" + loggedRole + ")";
    }
}

