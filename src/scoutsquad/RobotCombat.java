package scoutsquad;

import battlecode.common.*;

import static scoutsquad.RobotPlayer.rc;


public class RobotCombat {

    static void robotRangedAttack() {

        try {

            // Get all nearby robots
            RobotInfo[] robotAllies = rc.senseNearbyRobots(-1, rc.getTeam());
            RobotInfo[] robotEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

            // Find lowest health robot that isn't blocked by ally
            double lowestHealth = 1000.0;
            Direction bestDirection = null;
            for (RobotInfo enemyInfo: robotEnemies) {

                // See if the proposed bullet will hit an ally
                boolean willCollideWithAlly = false;
                for (RobotInfo allyInfo: robotAllies) {
                    Direction dirToEnemy = rc.getLocation().directionTo(enemyInfo.getLocation());
                    if (willCollideWithRobot(dirToEnemy, allyInfo.getLocation())) {
                        willCollideWithAlly = true;
                    }
                }

                // Set new lowest hp
                double enemyHp = enemyInfo.getHealth();
                if (!willCollideWithAlly && enemyHp < lowestHealth) {
                    lowestHealth = enemyHp;
                    bestDirection = rc.getLocation().directionTo(enemyInfo.getLocation());
                    //System.out.println("New best direction.");
                }
            }

            // Fire bullet if possible
            if (bestDirection != null) {
                if (rc.canFireSingleShot()) {
                    rc.fireSingleShot(bestDirection);
                    //System.out.println("Firing!");
                }
            } else {
                //System.out.println("Can't fire because of null location.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void robotMeleeAttack() {

        try {

            // Get enemy
            Team enemy = rc.getTeam().opponent();

            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

            if (robots.length > 0 && !rc.hasAttacked()) {
                // Use strike() to hit all nearby robots!
                rc.strike();
            } else {
                // No close robots, so search for robots within sight radius
                robots = rc.senseNearbyRobots(-1, enemy);

                // If there is a robot, move towards it
                if (robots.length > 0 && !rc.hasMoved()) {
                    MapLocation myLocation = rc.getLocation();
                    MapLocation enemyLocation = robots[0].getLocation();
                    Direction toEnemy = myLocation.directionTo(enemyLocation);

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean willCollideWithRobot(Direction dir, MapLocation loc) {
        MapLocation myLocation = loc;

        // Get relevant bullet information
        Direction propagationDirection = dir;
        MapLocation bulletLocation = rc.getLocation();

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
