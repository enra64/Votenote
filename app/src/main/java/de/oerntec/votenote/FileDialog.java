package de.oerntec.votenote;

/*
* 
* This file is licensed under The Code Project Open License (CPOL) 1.02 
* http://www.codeproject.com/info/cpol10.aspx
* http://www.codeproject.com/info/CPOL.zip
* 
* License Preamble:
* This License governs Your use of the Work. This License is intended to allow developers to use the Source
* Code and Executable Files provided as part of the Work in any application in any form.
* 
* The main points subject to the terms of the License are:
*    Source Code and Executable Files can be used in commercial applications;
*    Source Code and Executable Files can be redistributed; and
*    Source Code can be modified to create derivative works.
*    No claim of suitability, guarantee, or any warranty whatsoever is provided. The software is provided "as-is".
*    The Article(s) accompanying the Work may not be distributed or republished without the Author's consent
* 
* This License is entered between You, the individual or other entity reading or otherwise making use of
* the Work licensed pursuant to this License and the individual or other entity which offers the Work
* under the terms of this License ("Author").
*  (See Links above for full license text)
*  https://github.com/18446744073709551615/android-file-chooser-dialog
*/

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Environment;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileDialog {
    private static final int FileOpen = 0;
    private static final int FileSave = 1;
    private static final int FolderChoose = 2;
    public String defaultFileName = "export.xml";
    private int Select_type = FileSave;
    private String mSdcardDirectory = "";
    private Context mContext;
    private String selectedFileName = defaultFileName;
    private EditText inputText;

    private String m_dir = "";
    private List<String> mSubdirs = null;
    private FileDialogListener mSimpleFileDialogListener = null;
    private ArrayAdapter<String> mListAdapter = null;
    private boolean mGoToUpper = false;

    public FileDialog(Context context, String file_select_type, FileDialogListener SimpleFileDialogListener) {
        switch (file_select_type) {
            case "FileOpen":
                Select_type = FileOpen;
                break;
            case "FileSave":
                Select_type = FileSave;
                break;
            case "FolderChoose":
                Select_type = FolderChoose;
                break;
            case "FileOpen..":
                Select_type = FileOpen;
                mGoToUpper = true;
                break;
            case "FileSave..":
                Select_type = FileSave;
                mGoToUpper = true;
                break;
            case "FolderChoose..":
                Select_type = FolderChoose;
                mGoToUpper = true;
                break;
            default:
                Select_type = FileOpen;
                break;
        }

        mContext = context;
        mSdcardDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
        mSimpleFileDialogListener = SimpleFileDialogListener;

        try {
            mSdcardDirectory = new File(mSdcardDirectory).getCanonicalPath();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    ///////////////////////////////////////////////////////////////////////
    // chooseFile_or_Dir() - load directory chooser dialog for initial
    // default sdcard directory
    ///////////////////////////////////////////////////////////////////////
    public void chooseFile_or_Dir() {
        // Initial directory is sdcard directory
        if (m_dir.equals("")) chooseFile_or_Dir(mSdcardDirectory);
        else chooseFile_or_Dir(m_dir);
    }

    ////////////////////////////////////////////////////////////////////////////////
    // chooseFile_or_Dir(String dir) - load directory chooser dialog for initial
    // input 'dir' directory
    ////////////////////////////////////////////////////////////////////////////////
    public void chooseFile_or_Dir(String dir) {
        File dirFile = new File(dir);
        while (!dirFile.exists() || !dirFile.isDirectory()) {
            dir = dirFile.getParent();
            dirFile = new File(dir);
            Log.d("~~~~~", "dir=" + dir);
        }
        Log.d("~~~~~", "dir=" + dir);
        //mSdcardDirectory
        try {
            dir = new File(dir).getCanonicalPath();
        } catch (IOException ioe) {
            return;
        }

        m_dir = dir;
        mSubdirs = getDirectories(dir);


        AlertDialog.Builder dialogBuilder = createDirectoryChooserDialogNew();

        dialogBuilder.setPositiveButton("OK", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Current directory chosen
                // Call registered listener supplied with the chosen directory
                if (mSimpleFileDialogListener != null) {
                    if (Select_type == FileOpen || Select_type == FileSave) {
                        selectedFileName = inputText.getText() + "";
                        mSimpleFileDialogListener.onChosenDir(m_dir + "/" + selectedFileName);
                    } else {
                        mSimpleFileDialogListener.onChosenDir(m_dir);
                    }
                }
            }
        }).setNegativeButton("Cancel", null);
        dialogBuilder.setOnKeyListener(new Dialog.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                    goToUpperLevel();
                    Log.i("keylistener", "back");
                    return true;
                }
                return false;
            }
        });

        final AlertDialog dirsDialog = dialogBuilder.create();

        // Show directory chooser dialog
        dirsDialog.show();
    }

    private boolean createSubDir(String newDir) {
        File newDirFile = new File(newDir);
        return !newDirFile.exists() && newDirFile.mkdir();
    }

    private List<String> getDirectories(String dir) {
        List<String> dirs = new ArrayList<>();
        try {
            File dirFile = new File(dir);

            // if directory is not the base sd card directory add ".." for going up one directory
            if ((mGoToUpper || !m_dir.equals(mSdcardDirectory))
                    && !"/".equals(m_dir)
                    ) {
                dirs.add("..");
            }
            Log.d("~~~~", "m_dir=" + m_dir);
            if (!dirFile.exists() || !dirFile.isDirectory()) {
                return dirs;
            }

            for (File file : dirFile.listFiles()) {
                if (file.isDirectory()) {
                    // Add "/" to directory names to identify them in the list
                    dirs.add(file.getName() + "/");
                } else if (Select_type == FileSave || Select_type == FileOpen) {
                    //avoid showing non-xml files for import
                    if (Select_type == FileOpen) {
                        if (file.getName().contains(".xml"))
                            dirs.add(file.getName());
                    }
                    //show all files when exporting
                    else
                        dirs.add(file.getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Collections.sort(dirs, new Comparator<String>() {
            public int compare(String o1, String o2) {
                //small to big.(a to z) begin with items containing .. | / (list directories first)
                if ("..".equals(o1))
                    return -1;
                if ("..".equals(o2))
                    return 1;
                // dir                  no dir
                if (o1.contains("/") && !o2.contains("/"))
                    return -1;//first first
                //no dir                dir
                if (!o1.contains("/") && o2.contains("/"))
                    return 1;//latter first
                return o1.compareTo(o2);
            }
        });
        return dirs;
    }

    private AlertDialog.Builder createDirectoryChooserDialogNew() {
        View root = LayoutInflater.from(mContext).inflate(R.layout.dialog_file_chooser, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        //set title according to mode
        switch (Select_type) {
            case FileOpen:
                builder.setTitle("Open:");
                break;
            case FileSave:
                builder.setTitle("Save as:");
                break;
            default:
                builder.setTitle("Folder Select:");
        }

        Button newDirButton = (Button) root.findViewById(R.id.dialog_file_chooser_new_directory_button);
        ListView fileList = (ListView) root.findViewById(R.id.dialog_file_chooser_list);
        EditText fileNameEdit = (EditText) root.findViewById(R.id.dialog_file_chooser_file_name_edit);

        //handle the button for a new directory
        if (Select_type == FolderChoose || Select_type == FileSave) {
            newDirButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final EditText input = new EditText(mContext);

                    // Show new folder name input dialog
                    new AlertDialog.Builder(mContext)
                            .setTitle("New Folder Name")
                            .setView(input).setPositiveButton("OK", new OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Editable newDir = input.getText();
                            String newDirName = newDir.toString();
                            // Create new directory
                            if (createSubDir(m_dir + "/" + newDirName)) {
                                // Navigate into the new directory
                                m_dir += "/" + newDirName;
                                updateDirectory();
                            } else {
                                Toast.makeText(mContext, "Failed to create '"
                                        + newDirName + "' folder", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                            .setNegativeButton("Cancel", null).show();
                }
            });
        } else
            newDirButton.setVisibility(View.GONE);

        mListAdapter = createListAdapter(mSubdirs);
        fileList.setAdapter(mListAdapter);
        fileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String m_dir_old = m_dir;
                //lolnopefuckdis
                if (!(view instanceof TextView))
                    return;

                String sel = ((TextView) view).getText().toString();
                if (sel.charAt(sel.length() - 1) == '/')
                    sel = sel.substring(0, sel.length() - 1);

                // Navigate into the sub-directory
                if (sel.equals("..")) {
                    m_dir = m_dir.substring(0, m_dir.lastIndexOf("/"));
                    if ("".equals(m_dir)) {
                        m_dir = "/";
                    }
                } else {
                    m_dir += "/" + sel;
                }
                selectedFileName = defaultFileName;

                if ((new File(m_dir).isFile())) // If the selection is a regular file
                {
                    m_dir = m_dir_old;
                    selectedFileName = sel;
                }

                updateDirectory();
            }
        });

        inputText = fileNameEdit;
        inputText.setText(defaultFileName);

        builder.setView(root);

        builder.setCancelable(false);
        return builder;
    }

    private void goToUpperLevel() {
        if (m_dir.equals(mSdcardDirectory) || "/".equals(m_dir))
            return;
        m_dir = m_dir.substring(0, m_dir.lastIndexOf("/"));
        if ("".equals(m_dir)) {
            m_dir = "/";
        }
        updateDirectory();
    }

    private void updateDirectory() {
        mSubdirs.clear();
        mSubdirs.addAll(getDirectories(m_dir));
        mListAdapter.notifyDataSetChanged();
        //#scorch
        if (Select_type == FileSave || Select_type == FileOpen) {
            inputText.setText(selectedFileName);
        }
    }

    private ArrayAdapter<String> createListAdapter(List<String> items) {
        return new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, android.R.id.text1, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (v instanceof TextView) {
                    // Enable list item (directory) text wrapping
                    TextView tv = (TextView) v;
                    tv.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
                    tv.setEllipsize(null);
                }
                return v;
            }
        };
    }

    //////////////////////////////////////////////////////
    // Callback interface for selected directory
    //////////////////////////////////////////////////////
    public interface FileDialogListener {
        void onChosenDir(String chosenDir);
    }
}