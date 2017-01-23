package artemis;

import battlecode.common.*;
import static artemis.Channels.*;
import static artemis.RobotPlayer.*;

public class Nav {

    static boolean updatePriorityLocStatus(RobotInfo[] robotInfo) {

        try {

            if (robotInfo.length > 0) {

                // Send new data
                rc.broadcastFloat(PRIORITY_X, robotInfo[0].getLocation().x);
                rc.broadcastFloat(PRIORITY_Y, robotInfo[0].getLocation().y);
                return true;

            } else {

                // Reset if there are no longer enemies
                rc.broadcastFloat(PRIORITY_X, 0);
                rc.broadcastFloat(PRIORITY_Y, 0);
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    static void setPriorityLoc(RobotInfo[] robotInfo) {

        try {

            // Broadcast robot info
            rc.broadcastFloat(PRIORITY_X, robotInfo[0].getLocation().x);
            rc.broadcastFloat(PRIORITY_Y, robotInfo[0].getLocation().y);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void moveToPriorityLoc() {

        try {

            // Move to location broadcasted
            float x = rc.readBroadcastFloat(PRIORITY_X);
            float y = rc.readBroadcastFloat(PRIORITY_Y);
            moveTowardsLocation(new MapLocation(x, y));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean priorityLocExists() {

        try {

            // See if they are 0 or not
            if (rc.readBroadcastFloat(PRIORITY_X) != 0 && rc.readBroadcastFloat(PRIORITY_Y) != 0) {
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    static void moveTowardsEnemy(RobotInfo[] robotInfo) {

        try {

            // Move towards first enemy
            // TODO: This is pretty bad, needs to be fixed before robots get clusterfucked
            moveTowardsLocation(robotInfo[0].getLocation());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void moveTowardsLocation(MapLocation loc) {

        try {

            // Add 90 deg if can't move in that dir
            Direction dir = rc.getLocation().directionTo(loc);
            if (rc.canMove(dir)) {
                rc.move(dir);
            } else {
                if (rc.canMove(dir.rotateLeftDegrees(90))) {
                    rc.move(dir.rotateLeftDegrees(90));
                } else if (rc.canMove(dir.rotateRightDegrees(90))) {
                    rc.move(dir.rotateRightDegrees(90));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean avoidMapBoundaries() {

        try {

            // Move in opposite of first wall direction
            final float GARDENER_SPACE_RADIUS = 4.0f;
            float radians = 0;
            while (radians <= Math.PI*2) {
                MapLocation loc = rc.getLocation().add(new Direction(radians), GARDENER_SPACE_RADIUS);
                if (!rc.onTheMap(loc)) {
                    tryMove(rc.getLocation().directionTo(loc).opposite(), 5, 36);
                    return true;
                }
                radians += (float)(Math.PI/3);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    static void evadeRobotGroup(RobotInfo[] robotInfo) {

        // Move in opposite direction
        try {
            tryMove(rc.getLocation().directionTo(robotInfo[0].getLocation()).opposite(), 5, 36);
        } catch (Exception e) {
            System.out.println("Evade robot exception.");
        }
    }

    /*
    Preset stuff...
     */
    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
    }
}
