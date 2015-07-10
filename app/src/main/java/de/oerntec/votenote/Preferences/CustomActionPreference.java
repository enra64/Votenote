package de.oerntec.votenote.Preferences;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.widget.Toast;

import de.oerntec.votenote.ImportExport.CsvExporter;
import de.oerntec.votenote.ImportExport.XmlExporter;
import de.oerntec.votenote.ImportExport.XmlImporter;
import de.oerntec.votenote.VersionCheckHelper;

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
            case "version_check":
                if (VersionCheckHelper.isOnline(getContext()))
                    VersionCheckHelper.checkVersion((PreferencesActivity) getContext());
                else
                    Toast.makeText(getContext(), "Offline", Toast.LENGTH_SHORT).show();
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
