import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

import de.oerntec.votenote.Database.AdmissionPercentageMeta;
import de.oerntec.votenote.Database.DBAdmissionPercentageMeta;
import de.oerntec.votenote.LessonFragmentStuff.LessonFragment;

public class MainTabAdapter extends FragmentPagerAdapter {
    DBAdmissionPercentageMeta mMetaDb;
    List<AdmissionPercentageMeta> mData;

    public MainTabAdapter(int subjectId, FragmentManager fragmentManager) {
        super(fragmentManager);
        mMetaDb = DBAdmissionPercentageMeta.getInstance();
        mData = mMetaDb.getItemsForSubject(subjectId);
    }

    @Override
    public Fragment getItem(int position) {
        return LessonFragment.newInstance(mData.get(position).id);
    }

    @Override
    public int getCount() {
        return mData.size();
    }
}
