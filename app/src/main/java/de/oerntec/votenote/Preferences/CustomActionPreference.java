package de.oerntec.votenote.Preferences;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import de.oerntec.votenote.ImportExport.CsvExporter;
import de.oerntec.votenote.ImportExport.XmlExporter;
import de.oerntec.votenote.ImportExport.XmlImporter;

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
            default:
                super.onClick();
        }
    }
}
