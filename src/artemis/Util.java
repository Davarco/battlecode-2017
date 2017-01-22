package artemis;

import battlecode.common.*;
import static artemis.RobotPlayer.*;
import static artemis.Nav.*;

public class Util {

    static void dodgeIncomingBullets(BulletInfo[] incomingBullets) {

        try {

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

        // Set is 1/8
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
