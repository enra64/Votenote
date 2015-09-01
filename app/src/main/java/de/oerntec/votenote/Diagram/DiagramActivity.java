/*
* VoteNote, an android app for organising the assignments you mark as done for uni.
* Copyright (C) 2015 Arne Herdick
*
* This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
* */
package de.oerntec.votenote.Diagram;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

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


public class DiagramActivity extends AppCompatActivity implements DiagramSubjectAdapter.AdapterListener {
    private static DBSubjects mSubjectDb;
    private static DBLessons mLessonDb;
    private static GraphView mGraph;
    private Random r;
    private Map<Integer, LineGraphSeries<DataPoint>> lineGraphMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                LineGraphSeries<DataPoint> newLine = getGroupLineGraph(subjectId, color);
                lineGraphMap.put(subjectId, newLine);
            }
            mGraph.addSeries(lineGraphMap.get(subjectId));
        } else
            mGraph.removeSeries(lineGraphMap.get(subjectId));

        //determine the graph width
        int maxWidth = 0;
        List<Series> list = mGraph.getSeries();
        for (int i = 0; i < list.size(); i++) {
            int newValue = (int) list.get(i).getLowestValueX();
            maxWidth = maxWidth > newValue ? maxWidth : newValue;
        }

        mGraph.getGridLabelRenderer().setNumHorizontalLabels(maxWidth);
        mGraph.getViewport().setMaxX(maxWidth);
    }

    private LineGraphSeries<DataPoint> getGroupLineGraph(int subjectId, int color) {
        //get cursor
        Cursor all = mLessonDb.getAllLessonsForSubject(subjectId);

        //add uebung data to graph
        DataPoint[] dataPointArray = new DataPoint[all.getCount()];
        int arrayCounter = 0;
        float votePercentage = 100 * ((float) all.getInt(1) / (float) all.getInt(2));
        //do first point seperately
        dataPointArray[arrayCounter++] = new DataPoint(all.getInt(0), votePercentage);
        //step over the rest
        while (all.moveToNext())
            dataPointArray[arrayCounter++] = new DataPoint(all.getInt(0), 100 * ((float) all.getInt(1) / (float) all.getInt(2)));

        //close cursor
        all.close();

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
        viewport.setMinX(1);
        viewport.setMinY(0);
        viewport.setMaxY(100);

        mGraph.getLegendRenderer().setVisible(true);
        mGraph.getLegendRenderer().setMargin(100);
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
}