package exceptionalprivilege;

import static exceptionalprivilege.BroadcastChannels.*;
import static exceptionalprivilege.RobotPlayer.*;
import battlecode.common.*;


public class RobotMove {

    static void robotMove() {

        try {

            if (!rc.hasMoved()) {
                dodgeIncomingBullets();
            }

            if (!rc.hasMoved()) {
                if (isSquadLeader) {
                    squadLeaderMove();
                } else {
                    followSquadLeader();
                }
            }

            if (isSquadLeader && nearDeath()) {
                transferCommand();
            }

            // Shake surrounding trees
            TreeInfo[] treeInfo = rc.senseNearbyTrees();
            for (TreeInfo info: treeInfo) {
                if (rc.canShake(info.getLocation())) {
                    rc.shake(info.getLocation());
                    //System.out.println("Shaking tree!");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void squadLeaderMove() {

        try {

            // See if there are enemies around
            RobotInfo[] robotInfo = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            boolean enemyRobotsInSight = (robotInfo.length > 0);

            if (enemyRobotsInSight) {

                // Move towards first robot
                tryMove(rc.getLocation().directionTo(robotInfo[0].getLocation()), 5, 36);

                // Update for other squad members
                rc.broadcast(squadChannel+2, (int)robotInfo[0].getLocation().x);
                rc.broadcast(squadChannel+3, (int)robotInfo[0].getLocation().y);

                // See if a new target is needed
                if ((rc.readBroadcast(ASSIST_TEAM_X) == 0 && rc.readBroadcast(ASSIST_TEAM_Y) == 0) ||
                        (rc.readBroadcast(ASSIST_TEAM_X) == tempLocX && rc.readBroadcast(ASSIST_TEAM_Y) == tempLocY)) {

                    rc.broadcast(ASSIST_TEAM_X, (int)robotInfo[0].getLocation().x);
                    rc.broadcast(ASSIST_TEAM_Y, (int)robotInfo[0].getLocation().y);
                    tempLocX = (int)robotInfo[0].getLocation().x;
                    tempLocY = (int)robotInfo[0].getLocation().y;
                    //System.out.println("Enemy @" + tempLocX + ", " + tempLocY);

                } else {
                    tempLocX = rc.readBroadcast(ASSIST_TEAM_X);
                    tempLocY = rc.readBroadcast(ASSIST_TEAM_Y);
                }

            } else {

                // No enemies found
                int x = rc.readBroadcast(ASSIST_TEAM_X);
                int y = rc.readBroadcast(ASSIST_TEAM_Y);
                MapLocation enemyBcLoc = new MapLocation(x, y);

                // Check if there are still robots at assist location, reset if there aren't
                if (x != 0 && y != 0 && rc.canSenseAllOfCircle(enemyBcLoc, (float)3.0)) {
                    if (rc.senseNearbyRobots(enemyBcLoc, (float)3.0, rc.getTeam().opponent()).length == 0) {
                        rc.broadcast(ASSIST_TEAM_X, 0);
                        rc.broadcast(ASSIST_TEAM_Y, 0);
                        //System.out.println("Location @" + x + ", " + y + " has already been decimated.");
                        x = 0;
                        y = 0;
                    }
                }

                // Check if temp loc has stayed the same, reset if it is
                if (x != 0 && y != 0 && rc.readBroadcast(ASSIST_TEAM_X) == tempLocX && rc.readBroadcast(ASSIST_TEAM_Y) == tempLocY) {
                    rc.broadcast(ASSIST_TEAM_X, 0);
                    rc.broadcast(ASSIST_TEAM_Y, 0);
                    //System.out.println("Location @" + x + ", " + y + " has not been updated.");
                    x = 0;
                    y = 0;
                }

                if (x != 0 && y != 0) {

                    // Go to assist location
                    if (!tryMove(rc.getLocation().directionTo(enemyBcLoc), 5, 36)) {

                        // Move randomly
                        if (tryMove(randomDirection())) {

                            // Update for other squad members
                            rc.broadcast(squadChannel+2, (int)rc.getLocation().x);
                            rc.broadcast(squadChannel+3, (int)rc.getLocation().y);

                        } else {

                            // Can't move
                            rc.broadcast(squadChannel+2, 0);
                            rc.broadcast(squadChannel+3, 0);
                        }

                    } else {

                        // Update for other squad members
                        rc.broadcast(squadChannel+2, (int)rc.getLocation().x);
                        rc.broadcast(squadChannel+3, (int)rc.getLocation().y);
                    }

                } else {

                    // No assist location, move randomly
                    if (tryMove(randomDirection())) {

                        // Update for other squad members
                        rc.broadcast(squadChannel+2, (int)rc.getLocation().x);
                        rc.broadcast(squadChannel+3, (int)rc.getLocation().y);

                    } else {

                        // Can't move
                        rc.broadcast(squadChannel+2, 0);
                        rc.broadcast(squadChannel+3, 0);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void followSquadLeader() {

        try {

            // Get x and y from squad channel
            int x = rc.readBroadcast(squadChannel + 2);
            int y = rc.readBroadcast(squadChannel + 3);

            if (squadChannel != 0 ) {

                if (x != 0 && y != 0) {

                    // Move towards leader designated position
                    MapLocation loc = new MapLocation(x, y);
                    tryMove(rc.getLocation().directionTo(loc), 5, 36);

                } else if (x == 0 && y == 0) {

                    // Move away from leader designated position
                    MapLocation loc = new MapLocation(x, y);
                    tryMove(rc.getLocation().directionTo(loc).opposite(), 5, 36);
                }

                // Add as leader if no leader found
                if (rc.readBroadcast(squadChannel+1) == 0) {
                    rc.broadcast(squadChannel+1, rc.getID());
                    isSquadLeader = true;
                }

            } else {
                tryMove(randomDirection());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void transferCommand() {

        try {

            // Remove as squad leader
            isSquadLeader = false;
            rc.broadcast(squadChannel+1, 0);
            //System.out.println("Transferring command. ID: " + rc.getID() + " Origin: " + squadChannel + ", " + rc.readBroadcast(squadChannel) +
            //        " (" + rc.getLocation().x + ", " + rc.getLocation().y + ")");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void dodgeIncomingBullets() {

        try {

            // Get bullets that will collide
            BulletInfo[] incomingBullets = rc.senseNearbyBullets();

            for (BulletInfo info: incomingBullets) {

                if (willCollideWithMe(info)) {

                    // Find movement that won't collide
                    float strideRadius = rc.getType().strideRadius;
                    float x = rc.getLocation().x;
                    float y = rc.getLocation().y;

                    if (!willCollideWithMe(info, x, y+strideRadius) && rc.canMove(Direction.getNorth())) {
                        rc.move(Direction.getNorth());
                        return;
                    } else if (!willCollideWithMe(info, x+strideRadius, y) && rc.canMove(Direction.getEast())) {
                        rc.move(Direction.getEast());
                        return;
                    } else if (!willCollideWithMe(info, x, y-strideRadius) && rc.canMove(Direction.getSouth())) {
                        rc.move(Direction.getSouth());
                        return;
                    } else if (!willCollideWithMe(info, x-strideRadius, y) && rc.canMove(Direction.getWest())) {
                        rc.move(Direction.getWest());
                        return;
                    } else {
                        //System.out.println("Failed to dodge bullet!");
                        Direction dir = randomDirection();
                        while (!rc.canMove(dir)) {
                            dir = randomDirection();
                        }

                        rc.move(dir);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Dodge failed!");
            e.printStackTrace();
        }
    }

    static boolean nearDeath() {
        return (rc.getHealth()*8 < rc.getType().maxHealth);
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

    // Extra collision function that takes in new location as input as well
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

    // Archon gardener only
    static void escapeFromEnemy(RobotInfo[] robotInfo) {

        // Move in opposite direction
        try {
            tryMove(rc.getLocation().directionTo(robotInfo[0].getLocation()).opposite(), 5, 36);
        } catch (Exception e) {
            System.out.println("Escape from enemy exception.");
        }
    }
}
