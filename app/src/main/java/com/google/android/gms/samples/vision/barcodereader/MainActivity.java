/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.samples.vision.barcodereader;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.samples.vision.barcodereader.ui.camera.FileUtils;
import com.google.android.gms.vision.barcode.Barcode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Main activity demonstrating how to pass extra parameters to an activity that
 * reads barcodes.
 */
public class MainActivity extends Activity implements View.OnClickListener {

    // use a compound button so either checkbox or switch widgets work.
    private CompoundButton autoFocus;
    private CompoundButton useFlash;
    private CompoundButton autoCapture;
    private static final int wn = 18;
    private ArrayList<CompoundButton> bols;
    private TextView statusMessage;
    private TextView barcodeValue;
    private EditText bcv;
    private ContentValues values;
    private FileDialog fileDialog;
    private Uri imageUri;
    private String selec;
    private String imageurl;
    private static final int CAMERA_REQUEST = 1888;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private static final int RES = 80;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 999;
    private static final int OPEN_DIRECTORY_REQUEST_CODE = 248;

    public Runnable answerTrue = null;
    public Runnable answerFalse = null;

    private Bitmap photo;

    private static final int RC_BARCODE_CAPTURE = 9001;
    private static final String TAG = "BarcodeMain";

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageurl = null;

        bols = new ArrayList<CompoundButton>();

        for(int i=0; i<=wn; i++){
            try {
                int id = (Integer) R.id.class.getField("s"+(i+1)).get(null);
                CompoundButton s = (CompoundButton) findViewById(id);
                s.setChecked(true);
                bols.add(s);
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        statusMessage = (TextView)findViewById(R.id.status_message);
        barcodeValue = (TextView)findViewById(R.id.barcode_value);
        bcv = (EditText) findViewById(R.id.bcv);

        autoFocus = (CompoundButton) findViewById(R.id.auto_focus);
        useFlash = (CompoundButton) findViewById(R.id.use_flash);
        autoCapture = (CompoundButton) findViewById(R.id.use_auto);

        autoFocus.setChecked(true);
        autoCapture.setChecked(true);

        findViewById(R.id.read_barcode).setOnClickListener(this);
        findViewById(R.id.load).setOnClickListener(this);
        findViewById(R.id.save).setOnClickListener(this);
        findViewById(R.id.delvals).setOnClickListener(this);




        if(checkPermissionForReadExtertalStorage()) {
            reloadfiles();
        }
        else{
            try {
                requestPermissionForReadExternalStorage();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        if(checkURI()) {
            values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "New Picture");
            values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
            imageUri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        }
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */

    public boolean checkPermissionForReadExtertalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    public void requestPermissionForReadExternalStorage() throws Exception {
        try {
            ActivityCompat.requestPermissions((Activity) this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    126);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void reloadfiles(){
        File mPath = new File(Environment.getExternalStorageDirectory() + "//Registros//");
        fileDialog = new FileDialog(this, mPath, ".txt");
        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
            public void fileSelected(File file) {
                //Log.d(getClass().getName(), "Selecciona un registro" + file.toString());
                selec = file.toString();
                String a = "";
                try {
                    a = getFileContents(new File(selec));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String aa[] = selec.split("[/]");

                selec = aa[aa.length-1].replace(".txt","").replace("_",".");

                bcv.setText(selec);

                selec = null;

                String b[] = a.split("[,]");

                int cntr = 0;
                for (String z : b){
                    switch (z){
                        case "1":
                            bols.get(cntr).setChecked(true);
                            break;
                        case "0":
                            bols.get(cntr).setChecked(false);
                    }
                    cntr++;
                }

            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean checkURI(){
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to read the contacts
            }

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

            // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
            // app-defined int constant that should be quite unique

            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            else{
                return true;
            }
        }
        else{
            return true;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
            else
            {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }

//        if (requestCode == RES)
//        {
//            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
//            {
//                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
//                performFileSearch("Elige un registro");
//            }
//            else
//            {
//                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
//            }
//        }
    }



    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.read_barcode:
                // launch barcode activity.
                Intent intent = new Intent(this, BarcodeCaptureActivity.class);
                intent.putExtra(BarcodeCaptureActivity.AutoFocus, autoFocus.isChecked());
                intent.putExtra(BarcodeCaptureActivity.UseFlash, useFlash.isChecked());
                intent.putExtra(BarcodeCaptureActivity.AutoCapture, autoCapture.isChecked());
                startActivityForResult(intent, RC_BARCODE_CAPTURE);
                //bcv.setText(barcodeValue.getText());
                break;

            case R.id.load:
                /*if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, RES);
                } else {
                    //performFileSearch("Elige un registro");
                    showFileChooser();
                }*/
                fileDialog.showDialog();

                break;

            case R.id.delvals:
                resetall();
                break;


            case R.id.save:
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                } else if (!worktagValid(bcv.getText().toString())) {
                    Toast.makeText(this, "Introduce un Worktag válido", Toast.LENGTH_SHORT).show();
                } else {

                    String n;
                    //String timeStamp = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
                    n = bcv.getText().toString();
                    //n = n.replace(".", "_") + "_" + timeStamp + ".dat";
                    n = n.replace(".", "_") + ".txt";



                    String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Registros/" + n;
                    System.out.println(filePath + " EX TONNY");


                    File file = new File(filePath);
                    if(file.exists()) {
                        System.out.println("Existe TONNY");
                        confirm(this, "Archivo igual", "El archivo ya existe. ¿Desea sobreescribir?", escribeRegistroR(filePath), novoid());
                    }
                    else{
                        escribeRegistro(filePath);
                    }

                    reloadfiles();
                }
            break;
        }

    }

    public static String getFileContents(final File file) throws IOException {
        final InputStream inputStream = new FileInputStream(file);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        final StringBuilder stringBuilder = new StringBuilder();

        boolean done = false;

        while (!done) {
            final String line = reader.readLine();
            done = (line == null);

            if (line != null) {
                stringBuilder.append(line);
            }
        }

        reader.close();
        inputStream.close();

        return stringBuilder.toString();
    }

    //##########################
    ///DIALOGO DE CONFIRMACION
    public Runnable escribeRegistroR(final String filePath){
        return new Runnable() {
            public void run() {
        escribeRegistro(filePath);
            }
        };
    }
    public Runnable novoid(){
        return new Runnable() {
            public void run() {
            }
        };
    }
    public void escribeRegistro(String filePath){
        StringBuilder OPS = new StringBuilder();
        for (CompoundButton s : bols){
            OPS.append(s.isChecked() ? "1" : "0").append(",");
        }

        OPS = new StringBuilder(OPS.substring(0, OPS.length() - 1));
        System.out.println("Escribiendo - Tonny");
        boolean ya = false;
        try {
            PrintWriter writer = new PrintWriter(filePath, "UTF-8");
            writer.print(OPS);
            writer.close();
            ya = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (ya) {
            Toast.makeText(this, "Registro realizado con éxito", Toast.LENGTH_LONG).show();
            resetall();
        } else {
            Toast.makeText(this, "Hubo un error al hacer el registro", Toast.LENGTH_LONG).show();
        }
    }
    public boolean confirm(Activity act, String Title, String ConfirmText,
                           Runnable yesProc, Runnable noProc) {
        answerTrue = yesProc;
        answerFalse= noProc;
        AlertDialog.Builder alert = new AlertDialog.Builder(act);
        alert.setTitle(Title);
        alert.setMessage(ConfirmText);
        alert.setCancelable(false);
        alert.setPositiveButton("Si", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                answerTrue.run();
            }
        });
        alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                answerFalse.run();
            }
        });
        alert.show().getButton(DialogInterface.BUTTON_NEGATIVE).requestFocus();
        return true;
    }
    ///DIALOGO DE CONFIRMACION
    //##########################

    public void resetall(){
        for(CompoundButton s : bols){
            s.setChecked(true);
            bcv.setText("");
        }
    }

    public static void copyFile(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try
        {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
        finally
        {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
    }

    public boolean worktagValid(String text){
        return text.matches("^[0-9]{8}\\.[0-9]{3}$");
    }



    /**
     * Called when an activity you launched exits, giving you the requestCode
     * you started it with, the resultCode it returned, and any additional
     * data from it.  The <var>resultCode</var> will be
     * {@link #RESULT_CANCELED} if the activity explicitly returned that,
     * didn't return any result, or crashed during its operation.
     * <p/>
     * <p>You will receive this call immediately before onResume() when your
     * activity is re-starting.
     * <p/>
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     * @see #startActivityForResult
     * @see #createPendingResult
     * @see #setResult(int)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri selectedfile = data.getData(); //The uri with the location of the file
        System.out.print(selectedfile +"TONNY");
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {

            try {
                Bitmap thumbnail = MediaStore.Images.Media.getBitmap(
                        getContentResolver(), imageUri);
                Matrix matrix = new Matrix();
                matrix.postRotate(90);

                thumbnail = Bitmap.createBitmap(thumbnail, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), matrix, true);


                imageurl = getRealPathFromURI(imageUri);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        /*if(requestCode == OPEN_DIRECTORY_REQUEST_CODE){// && resultCode == RESULT_OK) {
            if (resultCode == RESULT_OK) {
                // Get the Uri of the selected file
                Uri uri = data.getData();
                Log.d(TAG, "File Uri: " + uri.toString());
                // Get the path
                String path = null;
                path = getRealPathFromURI(uri);
                *//*try {
                    path = FileUtils.getPath(this, uri);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }*//*
                Log.d(TAG, "File Path: " + getRealPathFromURI(uri));
                // Get the file instance
                // File file = new File(path);
                // Initiate the upload
            }
            //break;
        }*/


        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    bcv.setText(barcode.displayValue);
                    Log.d(TAG, "Barcode read: " + barcode.displayValue);
                } else {
                    Log.d(TAG, "No barcode captured, intent data is null");
                }
            } else {
                statusMessage.setText(String.format(getString(R.string.barcode_error),
                        CommonStatusCodes.getStatusCodeString(resultCode)));
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }

    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private void performFileSearch(String messageTitle) {
//        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
//        intent.setType("*text/plain");
//
//        String[] mimeTypes = new String[]{"application/x-binary,application/octet-stream"};
//        if (mimeTypes.length > 0) {
//            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
//        }
//
//        String x = "content:/" + Environment.getExternalStorageDirectory().getAbsolutePath() + "/Registros/";
//
//        Uri uri = Uri.parse(x);
//        intent.putExtra("android.provider.extra.INITIAL_URI", uri);
//        intent.setDataAndType(uri, "text/plain");
//
//        if (intent.resolveActivity(getPackageManager()) != null) {
//            startActivityForResult(Intent.createChooser(intent, messageTitle), OPEN_DIRECTORY_REQUEST_CODE);
//        } else {
//            Log.d(TAG, "Unable to resolve Intent.ACTION_OPEN_DOCUMENT {}");
//        }
        Intent intent = new Intent()
                .setType("text/plain")
                .setAction(Intent.ACTION_GET_CONTENT);

        System.out.print("EEEEEEE TONNY");

        startActivityForResult(Intent.createChooser(intent, messageTitle), OPEN_DIRECTORY_REQUEST_CODE);
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Selecciona un registro"),
                    OPEN_DIRECTORY_REQUEST_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

}
