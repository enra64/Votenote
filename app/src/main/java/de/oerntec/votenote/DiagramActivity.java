package de.oerntec.votenote;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import de.oerntec.votenote.Database.DBLessons;
import de.oerntec.votenote.Database.DBSubjects;


public class DiagramActivity extends AppCompatActivity {
    static DBSubjects groupsDB;
    static DBLessons entriesDB;
    static GraphView graph;
    Random r;
    int databaseID, positionID;
    Map<Integer, LineGraphSeries<DataPoint>> lineGraphMap;
    int[] colorArray;
    int colorStackPointer = 0;
    boolean[] enabledArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.diagramactivity);

        //get db connections
        groupsDB = DBSubjects.getInstance();
        entriesDB = DBLessons.getInstance();

        //set the standard databaseID, translated position
        databaseID = getIntent().getIntExtra("databaseID", 1);
        positionID = groupsDB.translateIDtoPosition(databaseID);

        //initialize array containing what is enabled
        enabledArray = new boolean[groupsDB.getNumberOfSubjects() - 1];

        //init random
        r = new Random();

        //generate colormap
        generateGoldenRatioColors();

        //set the standard databaseID, translated position
        databaseID = getIntent().getIntExtra("databaseID", 1);

        //some logging
        Log.i("diagram", "loading diagram for " + databaseID + " (" + groupsDB.getGroupName(databaseID) + "), translated position is " + groupsDB.translateIDtoPosition(databaseID));

        //get map for line graph series
        lineGraphMap = new HashMap<>();

        //set title, start graph
        ((TextView) findViewById(R.id.diagramactivity_text_info)).setText(groupsDB.getGroupName(databaseID));
        //do all the work for the graph here
        initializeGraph();

        //initialize ListView
        initializeListview();
    }

    private void generateGoldenRatioColors() {
        int uebungCount = groupsDB.getNumberOfSubjects();
        float sat = .5f, val = .95f;
        float goldenFloat = 0.618033988749895f;
        colorArray = new int[uebungCount];
        for (int i = 0; i < uebungCount; i++) {
            float hue = r.nextInt(3600);
            hue += goldenFloat * (i + 1);
            hue %= 1;
            hue *= 360;
            colorArray[i] = Color.HSVToColor(new float[]{hue, sat, val});
        }
    }

    private void initializeListview() {
        //initialize ListView
        final ListView uebungChooser = (ListView) findViewById(R.id.diagramactivity_listview_whichuebungs);

        //load names
        Cursor allNamesCursor = groupsDB.getAllButOneGroupNames(databaseID);

        //get listview
        uebungChooser.setAdapter(new SubjectAdapter(this, allNamesCursor, 0));
        uebungChooser.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        uebungChooser.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int translation = groupsDB.translatePositionToIDExclusive(position, databaseID);

                SparseBooleanArray checkedArray = uebungChooser.getCheckedItemPositions();

                if (checkedArray.get(position)) {
                    //apply bool array
                    enabledArray[position] = true;
                    //get from saved value
                    if (lineGraphMap.containsKey(translation))
                        graph.addSeries(lineGraphMap.get(translation));
                        //create new
                    else {
                        LineGraphSeries<DataPoint> newLine = getGroupLineGraph(translation);
                        lineGraphMap.put(translation, newLine);
                        graph.addSeries(lineGraphMap.get(translation));
                    }
                    //handle colors test
                    colorStackPointer++;
                } else {
                    enabledArray[position] = false;
                    graph.removeSeries(lineGraphMap.get(translation));
                    //decrease color stack pointer, because getGroupLineGraph increases it (see above)
                    colorStackPointer--;
                }
                applyMinimumBoundaries();
            }
        });
    }

    private void applyMinimumBoundaries() {
        //save maximum x, y
        int xMax = 0, yMax = 0;
        for (int pos = 0; pos < enabledArray.length; pos++) {
            if (enabledArray[pos]) {
                int id = groupsDB.translatePositionToIDExclusive(pos, databaseID);

                int entryHighestX = groupsDB.getScheduledNumberOfLessons(id);
                int entryHighestY = groupsDB.getScheduledAssignmentsPerLesson(id);
                Log.i("diagram boundaries", groupsDB.getGroupName(id) + " is checked with id " + id + " and \"position\" " + pos + ", using cID " + colorStackPointer);
                if (xMax < entryHighestX)
                    xMax = entryHighestX;
                if (yMax < entryHighestY)
                    yMax = entryHighestY;
            }
        }

        //exclusively handle activity start series
        int entryHighestX = groupsDB.getScheduledNumberOfLessons(databaseID);
        int entryHighestY = groupsDB.getScheduledAssignmentsPerLesson(databaseID);
        if (xMax < entryHighestX)
            xMax = entryHighestX;
        if (yMax < entryHighestY)
            yMax = entryHighestY;

        graph.getGridLabelRenderer().setNumHorizontalLabels(xMax);
        graph.getGridLabelRenderer().setNumVerticalLabels(yMax + 1);
        graph.getViewport().setMaxX(xMax);
        graph.getViewport().setMaxY(yMax);
    }

    private LineGraphSeries<DataPoint> getGroupLineGraph(int group) {
        //get cursor
        Cursor all = entriesDB.getAllLessonsForSubject(group);

        //debug
        Log.d("diagram", "linegraph for " + databaseID + " (" + groupsDB.getGroupName(databaseID) + ")");

        //add uebung data to graph
        DataPoint[] dataPointArray = new DataPoint[all.getCount()];
        int arrayCounter = 0;
        //do first point seperately
        dataPointArray[arrayCounter++] = new DataPoint(all.getInt(0), all.getInt(1));
        //step over the rest
        while (all.moveToNext())
            dataPointArray[arrayCounter++] = new DataPoint(all.getInt(0), all.getInt(1));

        //close cursor
        all.close();

        LineGraphSeries<DataPoint> answer = new LineGraphSeries<>(dataPointArray);

        answer.setColor(colorArray[colorStackPointer]);

        //set name
        answer.setTitle(groupsDB.getGroupName(group));

        //add points
        return answer;
    }

    private void initializeGraph() {
        //get graph
        graph = (GraphView) findViewById(R.id.diagramactivity_graph);

        LineGraphSeries<DataPoint> dataPoints = getGroupLineGraph(databaseID);

        //csp has to be increased since a color was used
        colorStackPointer++;

        lineGraphMap.put(databaseID, dataPoints);

        //avoid fractions in lables
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(0);

        //init graph
        graph.addSeries(lineGraphMap.get(databaseID));

        //set amount of x, y labels
        graph.getGridLabelRenderer().setNumHorizontalLabels(groupsDB.getScheduledNumberOfLessons(databaseID));
        graph.getGridLabelRenderer().setNumVerticalLabels(groupsDB.getScheduledAssignmentsPerLesson(databaseID) + 1);

        //change text size
        graph.getGridLabelRenderer().setTextSize(28);

        // set manual X bounds
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(1);
        graph.getViewport().setMaxX(groupsDB.getScheduledNumberOfLessons(databaseID));

        //change y scaling
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(groupsDB.getScheduledAssignmentsPerLesson(databaseID));

        graph.getLegendRenderer().setVisible(true);
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

    private class SubjectAdapter extends CursorAdapter {
        public SubjectAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return inflater.inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            //find textviews
            TextView description = (TextView) view.findViewById(android.R.id.text1);

            //load strings
            String subjectName = cursor.getString(1);

            //set text
            description.setText(subjectName);
        }
    }
}