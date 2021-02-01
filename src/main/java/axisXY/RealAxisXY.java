package axisXY;

import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class RealAxisXY {
    MySwingWorker mySwingWorker;
    SwingWrapper<XYChart> sw;
    XYChart chart;

    private void go() {

        // Create Chart
        chart =
                QuickChart.getChart(
                        "Linka Monofilament PP4",
                        "Czas[s]",
                        "Wartość",
                        "randomWalk",
                        new double[] {0},
                        new double[] {0});
        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setXAxisTicksVisible(false);

        // Show it
        sw = new SwingWrapper<>(chart);
        sw.setTitle("Canexpol");
        sw.displayChart();

        mySwingWorker = new MySwingWorker();
        mySwingWorker.execute();
    }

    private class MySwingWorker extends SwingWorker<Boolean, double[]> {

        final LinkedList<Double> fifo = new LinkedList<>();

        public MySwingWorker() {
            fifo.add(0.0);
        }

        @Override
        protected Boolean doInBackground() throws Exception {

            String server = "10.1.4.20";
            int path = 64000;

            System.out.println( "Loading contents of URL: " + server );
            // Connect to the server
            Socket socket = new Socket(server, path);

            // Create input and output streams to read from and write to the server
            PrintStream out = new PrintStream(socket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //Path path1 = Paths.get("monifinament.txt");
            Path dataAxisX = Paths.get("daneOsX.txt");
            Path dataAxisY = Paths.get("daneOsY.txt");
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);//formatowanie liczb
            DateTimeFormatter presentTime = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

            // create 4 BigDecimal objects
            BigDecimal bg1, bg2, axX, axY;

            LocalDateTime now = LocalDateTime.now();
            //Files.write(path1, Collections.singleton(now.toString()), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            Files.write(dataAxisX, Collections.singleton(now.toString()), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            Files.write(dataAxisY, Collections.singleton(now.toString()), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            out.println("M0");//komenda wysyłajaca zapytanie

            // Read data from the server until we finish reading the document
            String line, lineX, lineY;// zmienne osiowe
            String lineS = " ";
            String valueX = " ";
            String valueY = " ";

            int axisY, axisX, addValue;

            while (!isCancelled()) {
                line = in.readLine();//odczytuje wartość po wykonaniu zapytania "M0"
                /***Petla sprawdzajaca czy w polu widzenia sie cos znajduje
                 * obydwa czujniki wysylaja bzdure tak jak w if trzeba to wyzerowac***/
                lineX = line.substring(15, 24);
                lineY = line.substring(4, 13);
                //if (line.equals("M0,-099999998,-099999998") | lineX.equals("099999998") | lineY.equals("099999998")) {
                if (line.equals("M0,-099999998,-099999998")) {
                    line = "M0,-0000000000,-000000000";
                } else if (lineX.equals("099999998")) {
                    line = "M0,+" + lineY + ",-0000000000";
                } else if (lineY.equals("099999998")) {
                    line = "M0,-0000000000,+" + lineX;
                } else {
                    System.out.println("Uzyskano dane z obydwu osi");
                }
                /********************************************************************************/
                System.out.println("line = " + line);
                //  Files.write(path1, (lineS + System.lineSeparator()).getBytes(StandardCharsets.UTF_8.toString()), StandardOpenOption.CREATE, StandardOpenOption.APPEND);//zapisuje odczytane dane do pliku txt
                out.println("M0");//komenda wysylajaca zapytanie
                axisY = Integer.parseInt(line.substring(9, 13));//odcina zbedne wartosci y
                axisX = Integer.parseInt(line.substring(20, 24));//odcina zbedne wartosci x
                /*************************Usrednianie wartosci********************************************************/
                addValue = (axisY + axisX)/2;
                bg1 = new BigDecimal(addValue);
                bg2 = bg1.movePointLeft(3);//przesuniecie kropki w lewo
                axX = new BigDecimal(axisX);
                axX = axX.movePointLeft(3);//przesuniecie kropki w lewo
                axY = new BigDecimal(axisY);
                axY = axY.movePointLeft(3);//przesuniecie kropki w lewo
                /****************************************************************************************************/
                Files.write(dataAxisX, (valueX + System.lineSeparator()).getBytes(StandardCharsets.UTF_8.toString()), StandardOpenOption.CREATE, StandardOpenOption.APPEND);//zapisuje odczytane dane do pliku txt
                Files.write(dataAxisY, (valueY + System.lineSeparator()).getBytes(StandardCharsets.UTF_8.toString()), StandardOpenOption.CREATE, StandardOpenOption.APPEND);//zapisuje odczytane dane do pliku txt

                //addValued = Double.parseDouble(String.valueOf(addValue));
                //System.out.println("bg2 = " + bg2);
                //System.out.println("Wynik = " + numberFormat.format(addValue));
                //addxToBytes = numberFormat.format(addValue);
                Thread.sleep(1000);//pauza na 1s

                lineS = bg2.toString();
                lineS = lineS.replaceAll("\\.", ",");//zamiana kropki na przecinek, potrzebnego do excela
                /*************Wartosc Osi X*******************/
                valueX = axX.toString();
                valueX = valueX.replaceAll("\\.", ",");//zamiana kropki na przecinek, potrzebnego do excela
                /*************Wartosc Osi Y*******************/
                valueY = axY.toString();
                valueY = valueY.replaceAll("\\.", ",");//zamiana kropki na przecinek, potrzebnego do excela

                //  fifo.add(addValued);
                fifo.add(bg2.doubleValue());
                if (fifo.size() > 500) {
                    fifo.removeFirst();
                }

                double[] array = new double[fifo.size()];
                for (int i = 0; i < fifo.size(); i++) {
                    array[i] = fifo.get(i);
                }
                publish(array);

                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    // eat it. caught when interrupt is called
                    System.out.println("MySwingWorker shut down.");
                }
            }
            return true;
        }

        @Override
        protected void process(List<double[]> chunks) {
            //  System.out.println("number of chunks: " + chunks.size());
            double[] mostRecentDataSet = chunks.get(chunks.size() - 1);
            chart.updateXYSeries("randomWalk", null, mostRecentDataSet, null);
            sw.repaintChart();

            long start = System.currentTimeMillis();
            long duration = System.currentTimeMillis() - start;
            try {
                Thread.sleep(40 - duration); // 40 ms ==> 25fps
                // Thread.sleep(400 - duration); // 40 ms ==> 2.5fps
            } catch (InterruptedException e) {
                System.out.println("InterruptedException occurred.");
            }
        }
    }
    public static void main(String[] args) throws Exception {

        RealAxisXY swingWorkerRealTime = new RealAxisXY();
        swingWorkerRealTime.go();
    }
}
