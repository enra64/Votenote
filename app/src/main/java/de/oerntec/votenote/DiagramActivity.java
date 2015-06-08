package de.oerntec.votenote;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class DiagramActivity extends Activity {
    static DBGroups groupsDB;
    static DBEntries entriesDB;
    static GraphView graph;
    Random r;
    SimpleCursorAdapter uebungChooserAdapter;
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
        groupsDB = DBGroups.getInstance();
        entriesDB = DBEntries.getInstance();

        //set the standard databaseID, translated position
        databaseID = getIntent().getIntExtra("databaseID", 1);
        positionID = groupsDB.translateIDtoPosition(databaseID);

        //initialize array containing what is enabled
        enabledArray = new boolean[groupsDB.getUebungCount() - 1];

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
        setTitle(groupsDB.getGroupName(databaseID));

        //do all the work for the graph here
        initializeGraph();

        //initialize ListView
        initializeListview();
    }

    private void generateGoldenRatioColors() {
        int uebungCount = groupsDB.getUebungCount();
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
        ListView uebungChooser = (ListView) findViewById(R.id.diagramactivity_listview_whichuebungs);

        // set up the drawer's list view with items and click listener
        Cursor allNamesCursor = groupsDB.getAllButOneGroupNames(databaseID);
        //define wanted columns
        String[] sourceColumns = {DatabaseCreator.GROUPS_NAMEN};

        //define id values of views to be set
        int[] targetViews = {R.id.diagramactivity_listview_listitem_text_name};

        // create the adapter using the cursor pointing to the desired data
        uebungChooserAdapter = new SimpleCursorAdapter(
                this,
                R.layout.diagramactivity_listitem,
                allNamesCursor,
                sourceColumns,
                targetViews,
                0);

        //get listview
        uebungChooser = (ListView) findViewById(R.id.diagramactivity_listview_whichuebungs);
        uebungChooser.setAdapter(uebungChooserAdapter);
        uebungChooser.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckBox thisBox = (CheckBox) view.findViewById(R.id.diagramactivity_listview_listitem_checkbox);
                int translation = groupsDB.translatePositionToIDExclusive(position, databaseID);

                thisBox.setChecked(!thisBox.isChecked());
                if (thisBox.isChecked()) {
                    enabledArray[position] = true;
                    if (lineGraphMap.containsKey(translation))
                        graph.addSeries(lineGraphMap.get(translation));
                    else {
                        LineGraphSeries<DataPoint> newLine = getGroupLineGraph(translation);
                        lineGraphMap.put(translation, newLine);
                        graph.addSeries(lineGraphMap.get(translation));
                    }
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

                int entryHighestX = groupsDB.getScheduledUebungInstanceCount(id);
                int entryHighestY = groupsDB.getScheduledAssignmentsPerUebung(id);
                Log.i("diagram boundaries", groupsDB.getGroupName(id) + " is checked with id " + id + " and \"position\" " + pos + ", using cID " + colorStackPointer);
                if (xMax < entryHighestX)
                    xMax = entryHighestX;
                if (yMax < entryHighestY)
                    yMax = entryHighestY;
            }
        }

        //exclusively handle activity start series
        int entryHighestX = groupsDB.getScheduledUebungInstanceCount(databaseID);
        int entryHighestY = groupsDB.getScheduledAssignmentsPerUebung(databaseID);
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
        Cursor all = entriesDB.getGroupRecords(group);

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
        graph.getGridLabelRenderer().setNumHorizontalLabels(groupsDB.getScheduledUebungInstanceCount(databaseID));
        graph.getGridLabelRenderer().setNumVerticalLabels(groupsDB.getScheduledAssignmentsPerUebung(databaseID) + 1);

        //change text size
        graph.getGridLabelRenderer().setTextSize(28);

        // set manual X bounds
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(1);
        graph.getViewport().setMaxX(groupsDB.getScheduledUebungInstanceCount(databaseID));

        //change y scaling
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(groupsDB.getScheduledAssignmentsPerUebung(databaseID));

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
}