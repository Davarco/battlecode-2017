package sentinel;

import battlecode.common.*;

import static sentinel.Channels.*;
import static sentinel.RobotPlayer.*;

public class Util {

    static void shakeSurroundingTrees() {

        try {

            // Iterate through neutral tree list
            TreeInfo[] treeInfo = rc.senseNearbyTrees(-1, Team.NEUTRAL);
            for (TreeInfo info: treeInfo) {
                if (rc.canShake(info.getID())) {
                    rc.shake(info.getID());
                    return;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean enemyHostilesInRange(RobotInfo[] robotInfo) {

        try {

            // See if units are hostile if exist
            if (robotInfo.length > 0) {
                for (RobotInfo info : robotInfo) {
                    if (!info.getType().equals(RobotType.GARDENER) && !info.getType().equals(RobotType.ARCHON)) {
                        return true;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    static TreeInfo[] combineArrayData(TreeInfo[] t1, TreeInfo[] t2) {

        // Combine data w/ iteration
        TreeInfo[] info = new TreeInfo[t1.length+t2.length];
        int idx = 0;

        for (int i = 0; i < t1.length; i++) {
            info[idx] = t1[i];
            idx += 1;
        }

        for (int i = 0; i < t2.length; i++) {
            info[idx] = t2[i];
            idx += 1;
        }

        return info;
    }

    static void resetPriorityStatus(RobotInfo[] enemyInfo) {

        try {

            // Update the locations to go to, or reset to 0 if they don't exist
            if (isLocLeader) {
                //rc.setIndicatorDot(rc.getLocation(), 230, 140, 140);
                if (!updatePriorityLocStatus(enemyInfo)) {
                    isLocLeader = false;
                }
            } else {

                // Reset if same as previous round
                float x = rc.readBroadcastFloat(PRIORITY_X);
                float y = rc.readBroadcastFloat(PRIORITY_Y);
                if (prevPriorityX == x && prevPriorityY == y && x != 0 && y != 0) {
                    rc.broadcast(PRIORITY_X, 0);
                    rc.broadcast(PRIORITY_Y, 0);
                    //System.out.println("Resetting priority location, is the same as previous location.");
                }
            }

            prevPriorityX = rc.readBroadcastFloat(PRIORITY_X);
            prevPriorityY = rc.readBroadcastFloat(PRIORITY_Y);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean updatePriorityLocStatus(RobotInfo[] robotInfo) {

        try {

            if (robotInfo.length > 0) {

                // Send new data
                float x = robotInfo[0].getLocation().x;
                float y = robotInfo[0].getLocation().y;
                rc.broadcastFloat(PRIORITY_X, x);
                rc.broadcastFloat(PRIORITY_Y, y);
                rc.setIndicatorDot(new MapLocation(x, y), 255, 255, 255);
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
            rc.broadcastFloat(PRIORITY_X, robotInfo[0].getLocation().x+0.1f);
            rc.broadcastFloat(PRIORITY_Y, robotInfo[0].getLocation().y+0.1f);
            isLocLeader = true;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean priorityLocExists() {

        try {

            // See if they are 0 or not
            //System.out.println(rc.readBroadcastFloat(PRIORITY_X) + " " + rc.readBroadcastFloat(PRIORITY_Y));
            if (rc.readBroadcastFloat(PRIORITY_X) != 0 && rc.readBroadcastFloat(PRIORITY_Y) != 0) {
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    static boolean bulletCollisionImminent(BulletInfo[] bulletInfo) {

        // See if there is a bullet that will collide with robot
        for (BulletInfo info: bulletInfo) {
            if (willCollideWithMe(info)) {
                return true;
            }
        }

        return false;
    }

    static int getTotalFightingRobotCount() {
        try {
            return rc.readBroadcast(CHANNEL_SOLDIER_COUNT) + rc.readBroadcast(CHANNEL_SCOUT_COUNT) +
                    rc.readBroadcast(CHANNEL_TANK_COUNT) + rc.readBroadcast(CHANNEL_LUMBERJACK_COUNT);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    static boolean nearDeath() {

        // Set is 1/8
        return (rc.getHealth()*8 < rc.getType().maxHealth);
    }

    static boolean willCollideWithLocation(MapLocation startLoc, Direction moveDir, MapLocation targetLoc) {
        MapLocation myLocation = targetLoc;

        // Get relevant bullet information
        Direction propagationDirection = moveDir;
        MapLocation bulletLocation = startLoc;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta));

        return (perpendicularDist <= rc.getType().bodyRadius);
    }

    static boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta));

        return (perpendicularDist <= rc.getType().bodyRadius);
    }

    static boolean willCollideWithMe(BulletInfo bullet, float x, float y) {
        MapLocation myLocation = new MapLocation(x, y);

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta));

        return (perpendicularDist <= rc.getType().bodyRadius);
    }
}
