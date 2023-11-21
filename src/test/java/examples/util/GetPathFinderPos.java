package examples.util;

import star.finder.util.MathUtil;
import star.finder.util.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

public class GetPathFinderPos {

    public static int[] getNextPos(List<List<Boolean>> curMap, int[] curPos, HashSet<int[]> remove) {
        PriorityQueue<Node> possibleNodes = new PriorityQueue<>();

        for (int x = 0; x < curMap.size(); x++) {
            for (int y = 0; y < curMap.get(x).size(); y++) {
                if (curPos[0] == x && curPos[1] == y) continue;

                if (!curMap.get(x).get(y)) continue;
                if (!remove.contains(new int[]{x, y})) continue;

                Node curPossible = new Node(x, y, null, 0, 0);
                if (hasObstructionBetweenPoints((int) curPos[0], (int) curPos[1], curPossible.x, curPossible.y, curMap)) curPossible.f += MathUtil.getDistance(new int[]{(int) curPos[0], (int) curPos[1]}, new int[]{x, y});
                curPossible.f += MathUtil.getDistance(new int[]{(int) curPos[0], (int) curPos[1]}, new int[]{x, y});
                curPossible.f += getAirAround(3, 3, curMap, curPossible);

                possibleNodes.add(curPossible);
            }
        }

        Node best = possibleNodes.poll();
        assert best != null;
        return new int[] {best.x, best.y};
    }

    public static int getAirAround(int addX, int addY, List<List<Boolean>> grid, Node self) {
        int amAir = 0;

        for (Node node : getNodesAround(addX, addY, grid, self)) {
            if (grid.get(node.x).get(node.y)) {
                amAir++;
            }
        }

        return amAir;
    }

    public static List<Node> getNodesAround(int addX, int addY, List<List<Boolean>> grid, Node self) {
        List<Node> nodes = new ArrayList<>();

        for (int x = -addX; x < addX; ++x) {
            for (int y = -addY; y < addY; ++y) {
                if ((self.y != y || self.x != x) && isOnGrid(grid, self)) nodes.add(new Node(self.x + x, self.y + y, self, 0, 0));
            }
        }

        return nodes;
    }

    public static boolean isOnGrid(List<List<Boolean>> obstacleGrid, Node self) {
        return self.x >= 0 && self.x < obstacleGrid.size() &&
                self.y >= 0 && self.y < obstacleGrid.get(0).size();
    }

    public static boolean hasObstructionBetweenPoints(int x1, int y1, int x2, int y2, List<List<Boolean>> map) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = (x1 < x2) ? 1 : -1;
        int sy = (y1 < y2) ? 1 : -1;

        int err = dx - dy;

        while (true) {
            // Check for an obstruction at the current position
            if (isObstructed(x1, y1, map)) {
                return true;  // Obstruction found
            }

            if (x1 == x2 && y1 == y2) {
                break;  // Reached the destination without obstruction
            }

            int e2 = 2 * err;

            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }

            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }

        return false;  // No obstruction found
    }

    private static boolean isObstructed(int x, int y, List<List<Boolean>> map) {
        if (x >= 0 && x < map.size() && y >= 0 && y < map.get(0).size()) {
            return map.get(x).get(y);
        }

        return true;
    }
}
