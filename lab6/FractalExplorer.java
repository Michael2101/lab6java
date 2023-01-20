package lab6;

import lab4.*;
import lab5.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;


public class FractalExplorer {

  
    private final int displaySize;


    private JImageDisplay displayImage;
    private FractalGenerator fractalGenerator;
    private JButton buttonSave;
    private JButton buttonReset;

    private final Rectangle2D.Double complexPlaneRange;
 
    private JComboBox<FractalGenerator> fractalSelectorComboBox;


    private int rowsRemains = 0;

    public static void main(String[] args) {
        FractalExplorer fractalExplorer = new FractalExplorer(800);
        fractalExplorer.createAndShowGUI();
        fractalExplorer.drawFractal();
    }

    private FractalExplorer(int displaySize) {
        this.displaySize = displaySize;
        this.fractalGenerator = new Mandelbrot();
        this.complexPlaneRange = new Rectangle2D.Double(0, 0, 0, 0);
        fractalGenerator.getInitialRange(this.complexPlaneRange);
    }


    public void createAndShowGUI() {
        
        displayImage = new JImageDisplay(displaySize, displaySize);
        displayImage.addMouseListener(new EmphasizeActionListener());

        buttonReset = new JButton("Reset display");
        buttonReset.addActionListener(new ResetActionListener());

        buttonSave = new JButton("Save fractal");
        buttonSave.addActionListener(new SaveActionListener());

        JLabel label = new JLabel("Fractal:");
        fractalSelectorComboBox = new JComboBox<>();
        fractalSelectorComboBox.addItem(new Mandelbrot());
        fractalSelectorComboBox.addItem(new Tricorn());
        fractalSelectorComboBox.addItem(new BurningShip());
        fractalSelectorComboBox.addActionListener(new ComboBoxSelectItemActionListener());

        JPanel jPanelSelector = new JPanel();
        JPanel jPanelButtons = new JPanel();
        jPanelSelector.add(label, BorderLayout.CENTER);
        jPanelSelector.add(fractalSelectorComboBox, BorderLayout.CENTER);
        jPanelButtons.add(buttonReset, BorderLayout.CENTER);
        jPanelButtons.add(buttonSave, BorderLayout.CENTER);

        JFrame frame = new JFrame("fractal renderer");
        frame.setLayout(new BorderLayout());
        frame.add(displayImage, BorderLayout.CENTER);
        frame.add(jPanelSelector, BorderLayout.NORTH);
        frame.add(jPanelButtons, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        frame.setResizable(false);
    }


    private void drawFractal() {
        enableUI(false);
        rowsRemains = displaySize;
        for (int y = 0; y < displaySize; y++) {
            FractalWorker drawRow = new FractalWorker(y);
            drawRow.execute();
        }
    }
    // Ф-я отвечает за работу визуальной оболочки во время рисовки фрактала
    public void enableUI(boolean val) {
        buttonSave.setEnabled(val);
        buttonReset.setEnabled(val);
        fractalSelectorComboBox.setEnabled(val);
    }


    private class ResetActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            displayImage.clearImage();
            fractalGenerator.getInitialRange(complexPlaneRange);
            drawFractal();
        }
    }


    private class SaveActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            FileNameExtensionFilter fileFilter = new FileNameExtensionFilter("PNG Images", "png");
            fileChooser.setFileFilter(fileFilter);
            fileChooser.setAcceptAllFileFilterUsed(false);
            int t = fileChooser.showSaveDialog(displayImage);
            if (t == JFileChooser.APPROVE_OPTION) {
                try {
                    ImageIO.write(displayImage.picture, "png", fileChooser.getSelectedFile());
                } catch (Exception ee) {
                    JOptionPane.showMessageDialog(
                            displayImage,
                            ee.getMessage(),
                            "Error saving fractal",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }
    }

    private class ComboBoxSelectItemActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            fractalGenerator = (FractalGenerator) fractalSelectorComboBox.getSelectedItem();
            fractalGenerator.getInitialRange(complexPlaneRange);
            drawFractal();
        }
    }


    private class EmphasizeActionListener extends MouseAdapter implements MouseListener {
        public void mouseClicked(MouseEvent e) {
            if (rowsRemains > 0) {
                return;
            }
            double x = FractalGenerator.getCoord(
                    complexPlaneRange.x,
                    complexPlaneRange.x + complexPlaneRange.width, displaySize, e.getX()
            );
            double y = FractalGenerator.getCoord(
                    complexPlaneRange.y,
                    complexPlaneRange.y + complexPlaneRange.width,
                    displaySize,
                    e.getY()
            );
            fractalGenerator.recenterAndZoomRange(complexPlaneRange, x, y, 0.5);
            drawFractal();
        }
    }

    
    class FractalWorker extends SwingWorker<Object, Object> {
        
        private final int selectedY;
       
        private ArrayList<Integer> rowColors;

        public FractalWorker(int selectedY) {
            this.selectedY = selectedY;
        }

       // Отрисовка фракталов в фоновом режиме
        public Object doInBackground() {
            rowColors = new ArrayList<>(displaySize);
            for (int x = 0; x < displaySize; x++) {
                int count = fractalGenerator.numIterations(
                        FractalGenerator.getCoord(
                                complexPlaneRange.x,
                                complexPlaneRange.x + complexPlaneRange.width,
                                displaySize,
                                x
                        ),
                        FractalGenerator.getCoord(
                                complexPlaneRange.y,
                                complexPlaneRange.y + complexPlaneRange.width,
                                displaySize,
                                selectedY
                        )
                );
                int rgbColor;
                if (count == -1) {
                    rgbColor = 0;
                } else {
                    float hue = 0.7f + (float) count / 200f;
                    rgbColor = Color.HSBtoRGB(hue, 1f, 1f);
                }
                rowColors.add(rgbColor);
            }
            return null;
        }

        // Вызывается после полной отрисовки всех фракталов
        public void done() {
            for (int x = 0; x < displaySize; x++) {
                displayImage.drawPixel(x, selectedY, rowColors.get(x));
            }
            displayImage.repaint(0, 0, selectedY, displaySize, 1);
            rowsRemains--;
            if (rowsRemains == 0)
                enableUI(true);
        }
    }
}