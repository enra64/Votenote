package de.oerntec.votenote.tests;

import android.test.InstrumentationTestCase;

import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMetaStuff.AdmissionPercentageMetaPojo;

/**
 * Created by arne on 3/21/16.
 */
public class PercentageCalculatorTests extends InstrumentationTestCase {
    public void test() throws Exception {
        final int expected = 1;
        final int reality = 5;
        assertEquals(expected, reality);
        // TODO: 3/21/16 create data pojos via excel or something,
        // also create the results in excel, so we have something to compare to
    }

    public AdmissionPercentageMetaPojo test_create_meta(
            int userAssignmentsPerLessonEstimation,
            int userLessonCountEstimation,
            int baselineTargetPercentage,
            String mode,
            int bonusTargetPercentage,
            boolean bonusTargetPercentageEnabled
    ) {
        return new AdmissionPercentageMetaPojo(
                0,
                1,
                userAssignmentsPerLessonEstimation,
                userLessonCountEstimation,
                baselineTargetPercentage,
                "bert",
                mode,
                bonusTargetPercentage,
                bonusTargetPercentageEnabled,
                null,
                false);
    }
}
