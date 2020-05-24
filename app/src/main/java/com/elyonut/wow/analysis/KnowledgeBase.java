package com.elyonut.wow.analysis;

import com.elyonut.wow.model.ThreatLevel;

public class KnowledgeBase {
    public static double MIKUSH_RANGE_METERS = 200.0;
    public static double DEFAULT_RANGE_METERS = 300.0;

    public static double getRangeMeters(String threatType) {
        if (threatType == null)
            return DEFAULT_RANGE_METERS;

        if (threatType.equals("infantry")) return 300;
        if (threatType.equals("antitank_short")) return 700;
        if (threatType.equals("antitank_long")) return 5500;
        if (threatType.equals("observation")) return 3000;
        if (threatType.equals("command")) return 700;
        if (threatType.equals("mortar")) return 700;

        return DEFAULT_RANGE_METERS;

        //נק"ל 300
        //צלף 1200
        //נט קצר 700
        //נט בינוני 3000
        //נט ארוך 5500
        //תצפית 3000
        //סא'ש 7000
        //חבלה 200
    }

    public static ThreatLevel getThreatLevel(String threatType) {
        if (threatType == null)
            return ThreatLevel.None;

        if (threatType.equals("infantry")) return ThreatLevel.High;
        if (threatType.equals("antitank_short")) return ThreatLevel.High;
        if (threatType.equals("antitank_long")) return ThreatLevel.High;
        if (threatType.equals("observation")) return ThreatLevel.Medium;
        if (threatType.equals("command")) return ThreatLevel.High;
        if (threatType.equals("mortar")) return ThreatLevel.High;
        if (threatType.equals("mikush")) return ThreatLevel.High;

        return ThreatLevel.None;
    }
}
