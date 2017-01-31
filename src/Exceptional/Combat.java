package Exceptional;
import battlecode.common.*;

import static Exceptional.Channels.PRIORITY_X;
import static Exceptional.Channels.PRIORITY_Y;
import static Exceptional.RobotPlayer.rc;
import static Exceptional.Util.*;

public class Combat {

    static void destroySurroundingTrees(TreeInfo[] treeInfo) {

        try {

            if (treeInfo.length > 0) {

                // Chop at trees with robots
                for (TreeInfo info: treeInfo) {
                    if (info.getContainedRobot() != null && rc.canChop(info.getID())) {
                        rc.chop(info.getID());
                        return;
                    }
                }

                // Chop lowest hp tree if possible
                int minIdx = 0;
                float minHp = treeInfo[0].getHealth();
                for (int i = 0; i < treeInfo.length; i++) {
                    TreeInfo info = treeInfo[i];
                    if (info.getHealth() < minHp) {
                        minIdx = i;
                        minHp = info.getHealth();
                    }
                }

                if (rc.canChop(treeInfo[minIdx].getID())) {
                    rc.chop(treeInfo[minIdx].getID());
                    //System.out.println("Chopped tree @" + treeInfo[minIdx].toString());
                    return;
                }

                // Chop random tree
                for (TreeInfo info: treeInfo) {
                    if (rc.canChop(info.getID())) {
                        rc.chop(info.getID());
                        return;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void destroyTreesInWay() {

        try {

            // See if trees are in the way
            float x = rc.readBroadcastFloat(PRIORITY_X);
            float y = rc.readBroadcastFloat(PRIORITY_Y);
            MapLocation loc = new MapLocation(x, y);
            if (rc.senseNearbyTrees(rc.getLocation().add(rc.getLocation().directionTo(loc), 2.0f), 2.0f, Team.NEUTRAL).length > 0) {
                if (rc.canFireTriadShot()) {
                    rc.fireTriadShot(rc.getLocation().directionTo(loc));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static MapLocation getPrefEnemyLoc(RobotInfo[] robotInfo) {

        try {

            float minHp = 100;
            RobotInfo bestTarget = null;
            TreeInfo[] treeInfo = combineArrayData(rc.senseNearbyTrees(-1, rc.getTeam()), rc.senseNearbyTrees(-1, Team.NEUTRAL));
            for (RobotInfo info: robotInfo) {
                Direction dir = rc.getLocation().directionTo(info.getLocation());
                if (willNotCollideWithTrees(dir, treeInfo)) {
                    if (info.getHealth()/info.getType().getStartingHealth() < minHp) {
                        minHp = info.getHealth()/info.getType().getStartingHealth();
                        bestTarget = info;
                    }
                }
            }

            if (bestTarget != null) {
                return bestTarget.getLocation();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    static void defaultRangedAttack(RobotInfo[] robotInfo) {

        try {

            // Attack closest enemy and determine spread by distance
            MapLocation prefEnemyLoc = getPrefEnemyLoc(robotInfo);
            if (prefEnemyLoc != null) {
                Direction prefAttackDir = rc.getLocation().directionTo(prefEnemyLoc);
                float prefAttackDist = rc.getLocation().distanceTo(prefEnemyLoc);
                if (rc.canFirePentadShot() && pentadWillNotCollide(prefAttackDir)) {
                    rc.firePentadShot(prefAttackDir);
                } else if (rc.canFireTriadShot() && triadWillNotCollide(prefAttackDir)) {
                    rc.fireTriadShot(prefAttackDir);
                } else if (rc.canFireSingleShot() && singleWillNotCollide(prefAttackDir)) {
                    rc.fireSingleShot(prefAttackDir);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void defaultMeleeAttack() {

        try {

            // Strike if there are enemies within radius
            RobotInfo[] enemyInfoWithinRadius = rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS, rc.getTeam().opponent());
            RobotInfo[] friendlyInfoWithinRadius = rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS, rc.getTeam());
            if (rc.canStrike() && enemyInfoWithinRadius.length > friendlyInfoWithinRadius.length) {
                rc.strike();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean willNotCollideWithTrees(Direction dir, TreeInfo[] treeInfo) {
        for (TreeInfo info: treeInfo) {
            if (willCollideWithObject(rc.getLocation(), dir, info.getLocation(), info.getRadius())) {
                return false;
            }
        }

        return true;
    }

    static boolean pentadWillNotCollide(Direction dir) {

        try {

            RobotInfo[] teamInfo = rc.senseNearbyRobots(-1, rc.getTeam());
            for (RobotInfo info: teamInfo) {
                if (willCollideWithLocation(rc.getLocation(), dir, info.getLocation()) ||
                        willCollideWithLocation(rc.getLocation(), dir.rotateLeftDegrees(GameConstants.PENTAD_SPREAD_DEGREES), info.getLocation()) ||
                        willCollideWithLocation(rc.getLocation(), dir.rotateRightDegrees(GameConstants.PENTAD_SPREAD_DEGREES), info.getLocation()) ||
                        willCollideWithLocation(rc.getLocation(), dir.rotateLeftDegrees(GameConstants.PENTAD_SPREAD_DEGREES*2), info.getLocation()) ||
                        willCollideWithLocation(rc.getLocation(), dir.rotateRightDegrees(GameConstants.PENTAD_SPREAD_DEGREES*2), info.getLocation())) {
                    return false;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    static boolean triadWillNotCollide(Direction dir) {

        try {

            RobotInfo[] teamInfo = rc.senseNearbyRobots(-1, rc.getTeam());
            for (RobotInfo info: teamInfo) {
                if (willCollideWithLocation(rc.getLocation(), dir, info.getLocation()) ||
                        willCollideWithLocation(rc.getLocation(), dir.rotateLeftDegrees(GameConstants.TRIAD_SPREAD_DEGREES), info.getLocation()) ||
                        willCollideWithLocation(rc.getLocation(), dir.rotateRightDegrees(GameConstants.TRIAD_SPREAD_DEGREES), info.getLocation())) {
                    return false;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    static boolean singleWillNotCollide(Direction dir) {

        try {

            RobotInfo[] teamInfo = rc.senseNearbyRobots(-1, rc.getTeam());
            for (RobotInfo info: teamInfo) {
                if (willCollideWithLocation(rc.getLocation(), dir, info.getLocation())) {
                    return false;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }
}
