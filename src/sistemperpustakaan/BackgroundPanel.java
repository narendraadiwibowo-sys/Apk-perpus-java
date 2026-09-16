package sistemperpustakaan;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

/**
 * BackgroundPanel - JPanel kustom yang menampilkan gambar latar belakang
 * secara otomatis STRETCH/SCALE mengikuti ukuran form (termasuk saat Maximize).
 *
 * CARA PAKAI DI NETBEANS:
 * ========================
 * Di constructor form kamu (setelah initComponents()), tambahkan:
 *
 *   BackgroundPanel.terapkan(this, "/images/Dashboard Admin.jpg");
 *
 * Sesuaikan path gambar dengan nama file di folder src/images/
 */
public class BackgroundPanel extends JPanel {

    private Image backgroundImage;

    public BackgroundPanel(String imagePath) {
        try {
            java.net.URL imgURL = getClass().getResource(imagePath);
            if (imgURL != null) {
                backgroundImage = new ImageIcon(imgURL).getImage();
            } else {
                System.err.println("[BackgroundPanel] Gambar tidak ditemukan: " + imagePath);
            }
        } catch (Exception e) {
            System.err.println("[BackgroundPanel] Error memuat gambar: " + e.getMessage());
        }
        setOpaque(false);
    }

    public BackgroundPanel(Image image) {
        this.backgroundImage = image;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            // Stretch gambar penuh mengisi ukuran panel saat ini
            g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (backgroundImage != null) {
            return new Dimension(
                    backgroundImage.getWidth(this),
                    backgroundImage.getHeight(this));
        }
        return super.getPreferredSize();
    }

    /**
     * Metode pembantu - cara paling mudah menerapkan background scalable
     * ke JFrame yang sudah ada TANPA mengubah urutan initComponents().
     *
     * CARA PAKAI - di constructor form, SETELAH initComponents():
     *   BackgroundPanel.terapkan(this, "/images/Dashboard Admin.jpg");
     */
    public static void terapkan(javax.swing.JFrame frame, String imagePath) {
        BackgroundPanel bgPanel = new BackgroundPanel(imagePath);
        bgPanel.setLayout(null);

        java.awt.Container oldPane = frame.getContentPane();
        bgPanel.setPreferredSize(oldPane.getSize());

        java.awt.Component[] components = oldPane.getComponents();
        oldPane.removeAll();

        for (java.awt.Component comp : components) {
            bgPanel.add(comp);
        }

        frame.setContentPane(bgPanel);
        frame.revalidate();
        frame.repaint();
    }
}
