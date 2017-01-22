package exceptionalprivilege;

import static exceptionalprivilege.RobotPlayer.*;

public class RobotEndgame {

    static void implementEndgame() {

        try {

            // Get largest multiple of 10
            float total = 0;
            while (total < rc.getTeamBullets() - 20) {
                total += 10;
            }

            rc.donate(total);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
