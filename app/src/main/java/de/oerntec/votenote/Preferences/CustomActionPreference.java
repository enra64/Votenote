package de.oerntec.votenote.Preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import de.oerntec.votenote.ImportExport.CsvExporter;
import de.oerntec.votenote.ImportExport.XmlExporter;
import de.oerntec.votenote.ImportExport.XmlImporter;
import de.oerntec.votenote.R;

public class CustomActionPreference extends Preference {
    String mActionKey;

    public CustomActionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mActionKey = attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "key");
    }

    @Override
    protected void onClick() {
        switch (mActionKey) {
            case "csv_export":
                CsvExporter.exportDialog(getContext());
                break;
            case "xml_export":
                XmlExporter.exportDialog(getContext());
                break;
            case "xml_import":
                XmlImporter.importDialog(getContext());
                break;
            case "used_libraries":
                //show a dialog with graphview and stackoverflow
                AlertDialog.Builder b = new AlertDialog.Builder(getContext());
                b.setTitle("Hilfen");
                b.setPositiveButton("OK", null);
                b.setView(R.layout.preferences_thanks);
                b.show();
                break;
            default:
                super.onClick();
        }
    }
}
