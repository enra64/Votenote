/*
* VoteNote, an android app for organising the assignments you mark as done for uni.
* Copyright (C) 2015 Arne Herdick
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
* */
package de.oerntec.votenote.Diagram;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.Series;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import de.oerntec.votenote.Database.DBLessons;
import de.oerntec.votenote.Database.DBSubjects;
import de.oerntec.votenote.R;
import de.oerntec.votenote.TranslationHelper;


public class DiagramActivity extends AppCompatActivity implements DiagramSubjectAdapter.AdapterListener {
    private static DBSubjects mSubjectDb;
    private static DBLessons mLessonDb;
    private static GraphView mGraph;
    private Random r;
    private Map<Integer, LineGraphSeries<DataPoint>> lineGraphMap;
    private boolean usePercentageForXAxis = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TranslationHelper.adjustLanguage(this);

        setContentView(R.layout.diagramactivity);

        //handle action bar
        setSupportActionBar((Toolbar) findViewById(R.id.diagramactivity_toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        //get db connections
        mSubjectDb = DBSubjects.getInstance();
        mLessonDb = DBLessons.getInstance();

        //init random
        r = new Random();

        //get map for line graph series
        lineGraphMap = new HashMap<>();

        //do all the work for the graph here
        initializeGraph();

        //initialize ListView
        final RecyclerView subjectList = (RecyclerView) findViewById(R.id.diagramactivity_recyclerview_subject_list);
        subjectList.setAdapter(new DiagramSubjectAdapter(this, generateGoldenRatioColors()));

        //give it a layoutmanager (whatever that is)
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        subjectList.setLayoutManager(manager);

        //initialize the x axis caption switch
        ((Switch) findViewById(R.id.diagramactivity_xaxis_switch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                usePercentageForXAxis = isChecked;
                //clear old data
                mGraph.removeAllSeries();
                lineGraphMap.clear();
                //"uncheck" all subjects
                subjectList.swapAdapter(new DiagramSubjectAdapter(DiagramActivity.this, generateGoldenRatioColors()), true);
            }
        });
    }

    private int[] generateGoldenRatioColors() {
        int subjectCount = mSubjectDb.getNumberOfSubjects();
        float sat = .5f, val = .95f;
        float goldenFloat = 0.618033988749895f;
        int[] colorArray = new int[subjectCount];
        for (int i = 0; i < subjectCount; i++) {
            float hue = r.nextInt(3600);
            hue += goldenFloat * (i + 1);
            hue %= 1;
            hue *= 360;
            colorArray[i] = Color.HSVToColor(new float[]{hue, sat, val});
        }
        return colorArray;
    }

    @Override
    public void onClick(int subjectId, boolean enabled, int color) {
        if (enabled) {
            //create new if not already created
            if (!lineGraphMap.containsKey(subjectId)) {
                LineGraphSeries<DataPoint> newGraphLine = getGroupLineGraph(subjectId, color);
                lineGraphMap.put(subjectId, newGraphLine);
            }
            mGraph.addSeries(lineGraphMap.get(subjectId));
        } else
            mGraph.removeSeries(lineGraphMap.get(subjectId));

        if (usePercentageForXAxis) {
            mGraph.getGridLabelRenderer().setNumHorizontalLabels(5);
            mGraph.getViewport().setMaxX(100);
            mGraph.getViewport().setMinX(0);
            return;
        }
        //determine the graph width
        int maxWidth = 0;
        //get all series
        List<Series> list = mGraph.getSeries();
        //iterate through series
        for (int i = 0; i < list.size(); i++) {
            //get largest value
            int highestValue = (int) list.get(i).getHighestValueX();
            int lowestValue = (int) list.get(i).getLowestValueX();
            //which is bigger depends on the lesson sort b/c the database gets that by itself;
            //just take the bigger one
            int newValue = highestValue > lowestValue ? highestValue : lowestValue;
            //take bigger value
            maxWidth = maxWidth > newValue ? maxWidth : newValue;
        }

        mGraph.getGridLabelRenderer().setNumHorizontalLabels(maxWidth);
        mGraph.getViewport().setMaxX(maxWidth);
        mGraph.getViewport().setMinX(1);
    }

    /**
     * Creates a LineGraphSeries object containing the data points of the specified subject
     */
    private LineGraphSeries<DataPoint> getGroupLineGraph(int subjectId, int color) {
        //get cursor
        Cursor allLessonsForSubject = mLessonDb.getAllLessonsForSubject(subjectId);

        //add uebung data to graph
        DataPoint[] dataPointArray = new DataPoint[allLessonsForSubject.getCount()];
        int arrayCounter = 0;

        //go back one so the loop works; the cursor gets moved to first position in getallLessons
        allLessonsForSubject.moveToPrevious();

        if (usePercentageForXAxis)
            while (allLessonsForSubject.moveToNext())
                dataPointArray[arrayCounter++] = new DataPoint(100f * ((float) (allLessonsForSubject.getInt(0) - 1) / (float) (dataPointArray.length - 1)), 100 * ((float) allLessonsForSubject.getInt(1) / (float) allLessonsForSubject.getInt(2)));
        else
            while (allLessonsForSubject.moveToNext())
                dataPointArray[arrayCounter++] = new DataPoint(allLessonsForSubject.getInt(0), 100 * ((float) allLessonsForSubject.getInt(1) / (float) allLessonsForSubject.getInt(2)));

        //close cursor
        allLessonsForSubject.close();

        LineGraphSeries<DataPoint> answer = new LineGraphSeries<>(dataPointArray);

        answer.setColor(color);

        //set name
        answer.setTitle(mSubjectDb.getGroupName(subjectId));

        //add points
        return answer;
    }

    private void initializeGraph() {
        //get graph
        mGraph = (GraphView) findViewById(R.id.diagramactivity_graph);

        //avoid fractions in lables
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(0);

        GridLabelRenderer renderer = mGraph.getGridLabelRenderer();
        Viewport viewport = mGraph.getViewport();

        //set amount of x, y labels
        renderer.setNumVerticalLabels(11);

        //change text size
        renderer.setTextSize(28);

        //change y scaling
        viewport.setYAxisBoundsManual(true);
        viewport.setXAxisBoundsManual(true);
        viewport.setMinY(0);
        viewport.setMaxY(100);
    }

    //avoid losing view states when going back
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return (true);
        }
        return super.onOptionsItemSelected(item);
    }

    public String getPreference(String key, String def) {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(key, def);
    }
}