package examples;

import brigero.board.Board;
import edu.wlu.cs.levy.breezyslam.algorithms.RMHCSLAM;
import edu.wlu.cs.levy.breezyslam.algorithms.SinglePositionSLAM;
import edu.wlu.cs.levy.breezyslam.components.Laser;
import edu.wlu.cs.levy.breezyslam.components.URG04LX;
import ev3dev.sensors.slamtec.RPLidarA1;
import ev3dev.sensors.slamtec.RPLidarProviderListener;
import ev3dev.sensors.slamtec.model.Scan;
import ev3dev.sensors.slamtec.model.ScanDistance;
import examples.autofind.AutoFindTo;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;

public @Slf4j class Demo {
    private final static int MAP_SIZE_PIXELS = 1600;
    private final static double MAP_SIZE_METERS = 200; //32;
    private final static double HOLE_WIDTH_MM = 3000; //32;
    private final static int MAP_QUALITY = 5; // 0-255; default 50

    private final static int SCAN_SIZE = 426;
    private final static int DETECTION_ANGLE_DEG = 360;
    private static boolean isOpen = true;

    private static List<ScanDistance> distances = null;
    private static SinglePositionSLAM slam = null;

    public static void main(String[] args) {
        RPLidarLaser laser = new RPLidarLaser();

        try {
            final String USBPort = "/dev/tty.usbserial-0001";
            final RPLidarA1 lidar = new RPLidarA1(USBPort);
            lidar.init();
            Board board = new Board(800, 800, new ArrayList<>(), new int[]{1, 1}, (int) MAP_SIZE_METERS);
            //AutoFindTo finder = new AutoFindTo(new int[]{100, 100}, new int[]{0, 0});

            new Thread(() -> {
                long timePoint = System.currentTimeMillis();

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                int x = 0;
                while (isOpen) {
                    if (timePoint + 1000 < System.currentTimeMillis()) {
                        byte[] mapBytes = new byte[MAP_SIZE_PIXELS*MAP_SIZE_PIXELS];
                        slam.getmap(mapBytes);
                        List<List<Integer>> twoDList = shortenList(make2DList(mapBytes), 800, 800);

                        for (int i = 0; i < twoDList.size(); ++i) {
                            for (int j = 0; j < twoDList.get(i).size(); ++j) {
                                board.getRenderer().putData(j, i, twoDList.get(i).get(j));
                            }
                        }

                        board.getRenderer().updatePosition(slam.getpos().x_mm, slam.getpos().y_mm);

                        //log.info(String.valueOf(slam.getpos().theta_degrees));

                        board.getRenderer().updateDeg(slam.getpos().theta_degrees);
                        board.getRenderer().reDraw();

                        x++;
                        if (x >= 10) {
                            //finder.tick(board.getRenderer().getAbsPlayerPos(), board.getRenderer());
                        }
                    }
                }
            }).start();

            lidar.addListener(new RPLidarProviderListener() {
                @Override
                public void scanFinished(Scan scan) {
                    if (scan.getDistances().size() < 360) {
                        return;
                    }

                    distances = new ArrayList<>(scan.getDistances());
                    distances.sort(Comparator.comparing(ScanDistance::getAngle));

                    //log.info(String.valueOf(distances.size()));

                    int[] scanInt = new int[SCAN_SIZE];

                    for (int i = 0; i < scanInt.length; i++) {
                        // account for the hand
                        //if (distances.get(i).getAngle() > 130 || distances.get(i).getAngle() < 230) continue;
                        final double angle = (double) i / scanInt.length * DETECTION_ANGLE_DEG;
                        final ScanDistance closest = findClosestScanDistance(distances, angle);
                        scanInt[i] = (int) Math.ceil(closest.getDistance() * 10);
                    }

                    if (slam == null) {
                        slam = new RMHCSLAM(laser, MAP_SIZE_PIXELS, MAP_SIZE_METERS, 9999);
                        slam.hole_width_mm = HOLE_WIDTH_MM;
                        slam.map_quality = MAP_QUALITY;
                    }

                    slam.update(scanInt);
                    //slam.getpos().theta_degrees = 0;
                    //Position position = slam.getpos();

                    //log.info("Measures: {}", scanInt.length);
                }
            });

            lidar.scan();

            Thread.sleep(120000);
            isOpen = false;

            byte[] mapBytes = new byte[MAP_SIZE_PIXELS*MAP_SIZE_PIXELS];
            slam.getmap(mapBytes);

            final String fileName = new Date() + ".pgm";
            BufferedWriter output = new BufferedWriter(new FileWriter(fileName));
            output.write(String.format("P2\n%d %d 255\n", MAP_SIZE_PIXELS, MAP_SIZE_PIXELS));
            for (int y = 0; y < MAP_SIZE_PIXELS; y++) {
                for (int x = 0; x < MAP_SIZE_PIXELS; x++) {
                    output.write(String.format("%d ", (int) mapBytes[(int)(y * MAP_SIZE_PIXELS + x)] & 0xFF));
                }
                output.write("\n");
            }
            output.close();

            //List<List<Integer>> twoDList = make2DList(mapBytes);
            //System.out.println("Print => " + twoDList.size() + " | " + twoDList.get(0).size() + "!!!!");

            //log.info("Wrote the file \"" + fileName + "\"");
            lidar.close();
            //System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ScanDistance findClosestScanDistance(List<ScanDistance> distances, double angle) {
        int left = 0;
        int right = distances.size() - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            ScanDistance midDistance = distances.get(mid);

            if (midDistance.getAngle() == angle) {
                return midDistance;
            } else if (midDistance.getAngle() < angle) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        if (right < 0) {
            return distances.get(0);
        } else if (left >= distances.size()) {
            return distances.get(distances.size() - 1);
        } else {
            ScanDistance before = distances.get(right);
            ScanDistance after = distances.get(left);

            if (Math.abs(before.getAngle() - angle) < Math.abs(after.getAngle() - angle)) {
                return before;
            } else {
                return after;
            }
        }
    }

    static List<ScanDistance> removeDupe(List<ScanDistance> init) {
        HashSet<Integer> angleHash = new HashSet<>();
        List<ScanDistance> returnList = new ArrayList<>();

        for (int i = 0; i < init.size(); ++i) {
            if (angleHash.contains(init.get(i).getAngle())) {
                continue;
            } else {
                returnList.add(init.get(i));
                angleHash.add(init.get(i).getAngle());
            }
        }

        return returnList;
    }

    /*private static List<List<GridBoardData>> parseInts(List<List<Integer>> init) {
        List<List<GridBoardData>> arr = new ArrayList<>();

        for (int y = 0; y < init.size(); y++) {
            List<GridBoardData> tmp = new ArrayList<>();

            for (int x = 0; x < init.get(y).size(); x++) {
                tmp.add(new GridBoardData(x, y, getState(init.get(y).get(x))));
            }

            arr.add(tmp);
        }

        return arr;
    }*/

    public static int getState(int integer) {
        if (integer < 80) {
            return 2;
        }

        if (integer == 127) {
            return 0;
        }

        return 1;
    }

    // 1 = open
    // 2 = unknown
    // 3 = blocked

    /*
        int cellWidth = 50;
        int cellHeight = 50;
     */

    private static List<List<Integer>> make2DList(byte[] mapBytes) {
        List<List<Integer>> list2D = new ArrayList<>();

        for (int y = 0; y < MAP_SIZE_PIXELS; y++) {
            List<Integer> innerList = new ArrayList<>();

            for (int x = 0; x < MAP_SIZE_PIXELS; x++) {
                innerList.add((int) mapBytes[(int)(y * MAP_SIZE_PIXELS + x)] & 0xFF);
            }

            list2D.add(innerList);
        }

        return list2D;
    }

    private static List<List<Integer>> shortenList(List<List<Integer>> originalList, int maxX, int maxY) {
        int originalWidth = originalList.size();
        int originalHeight = originalList.get(0).size();

        List<List<Integer>> shortenedList = new ArrayList<>();

        int widthRatio = originalWidth / maxX;
        int heightRatio = originalHeight / maxY;

        int totlRatio = (originalWidth * originalHeight) / (maxX * maxY);

        for (int x = 0; x < maxX; x++) {
            List<Integer> row = new ArrayList<>();
            for (int y = 0; y < maxY; y++) {
                double sum = 0;
                for (int i = x * widthRatio; i < (x + 1) * widthRatio; i++) {
                    for (int j = y * heightRatio; j < (y + 1) * heightRatio; j++) {
                        sum += originalList.get(j).get(i);
                    }
                }

                double avg = sum / (widthRatio * heightRatio);
                row.add((int) avg);//avg < 80 ? 1 : avg < 128 ? 3 : 2);
            }
            shortenedList.add(row);
        }

        return shortenedList;
    }

    private static class RPLidarLaser extends Laser
    {
        public RPLidarLaser()
        {
            // int scan_size,
            // double scan_rate_hz,
            // double detection_angle_degrees,
            // double distance_no_detection_mm,
            // int detection_margin,
            // double offset_mm)
            //super(682, 10, 240, 4000, 70, 145);

            //////
            // super(426, 10, 360, 12000, 5, 145)
            /////
            super(SCAN_SIZE, 10, DETECTION_ANGLE_DEG, 12000, 0, 0);
        }
    }
}
