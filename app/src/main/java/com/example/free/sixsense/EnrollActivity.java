package com.example.free.sixsense;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.content.DialogInterface;


import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;

import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class EnrollActivity extends Activity implements View.OnClickListener {

    Button bt_RecordStop;
    ImageView p_tv1, p_tv2, p_tv3;
    TextView p_name4;
    TextView p_date4;
    EditText editInput;
    boolean isRecord = false;
    boolean isPlaying = false;

    File recordingFile;
    // playFile
    File bellAddress = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/recorder/Bell.pcm");
    File fireAddress = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/recorder/Fire.pcm");
    File rangeAddress = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/recorder/Ragne.pcm");

    PlayAudio playAudio;

    // 주파수 파형 그리기
    ImageView imageView;
    Bitmap bitmap;
    Canvas canvas;
    Paint paint;

    private DoubleFFT_1D trans;

    int frequency = 12000, channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    int blockSize = 1024;

    RecordAudio recordTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.enroll);

        bt_RecordStop = (Button)findViewById(R.id.bt_RecordStop);
        p_tv1 = (ImageView)findViewById(R.id.p_tv1);
        p_tv2 = (ImageView)findViewById(R.id.p_tv2);
        p_tv3 = (ImageView)findViewById(R.id.p_tv3);
        p_name4 = (TextView)findViewById(R.id.p_name4);
        p_date4= (TextView)findViewById(R.id.p_date4);

        editInput = (EditText)findViewById(R.id.editInput);

        imageView = (ImageView)findViewById(R.id.fftView2);
        trans = new DoubleFFT_1D(512);

        bitmap = Bitmap.createBitmap((int)256, (int)100, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        paint.setColor(Color.rgb(155,83,91));
        imageView.setImageBitmap(bitmap);

        p_tv1.setOnClickListener(this);
        p_tv2.setOnClickListener(this);
        p_tv3.setOnClickListener(this);
        bt_RecordStop.setOnClickListener(this);

        p_tv1.setEnabled(true);
        p_tv2.setEnabled(true);
        p_tv3.setEnabled(true);
    }

    private class RecordAudio extends AsyncTask<Void, double[], Void> {

        @Override

        protected Void doInBackground(Void... params) {

            try {
                int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);

                AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency, channelConfiguration, audioEncoding, bufferSize);

                short[] buffer = new short[blockSize];
                double[] toTransform = new double[blockSize];

                audioRecord.startRecording();

                while (isRecord) {
                    int bufferReadResult = audioRecord.read(buffer, 0, blockSize);

                    for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
                        toTransform[i] = (double) buffer[i] / Short.MAX_VALUE; // 부호 있는 16비트
                    }
                    trans.realForwardFull(toTransform);

                    publishProgress(toTransform);
                }
                audioRecord.stop();

            } catch (Throwable t) {
                Log.e("AudioRecord", "Recording Failed");
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(double[]... toTransform) {
            canvas.drawColor(Color.rgb(232,203,143));

            for (int i = 0; i < toTransform[0].length; i++) {
                int x = i;
                int downy = (int) (100 - (toTransform[0][i] * 10));
                int upy = 100;

                canvas.drawLine(x, downy, x, upy, paint);
            }
            imageView.invalidate();
        }
    }

    /***********************************play********************************************************/
    private class PlayAudio extends AsyncTask<Object, Integer, Void> {
        @Override
        protected Void doInBackground(Object... params) {
            isPlaying = true;

            File play = (File) params[0];
            int bufferSize = AudioTrack.getMinBufferSize(frequency,channelConfiguration, audioEncoding);
            short[] audiodata = new short[bufferSize / 4];

            try {
                DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(play)));
                AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC, frequency, channelConfiguration, audioEncoding, bufferSize, AudioTrack.MODE_STREAM);

                audioTrack.play();
                while (isPlaying && dis.available() > 0) {
                    int i = 0;
                    while (dis.available() > 0 && i < audiodata.length) {
                        audiodata[i] = dis.readShort();
                        i++;
                    }
                    audioTrack.write(audiodata, 0, audiodata.length);
                }
                dis.close();

            } catch (Throwable t) {
                Log.e("AudioTrack", "Playback Failed");
            }
            return null;
        }
    }
    /***********************************************************************************************/

    @Override
    public void onClick(View v) {
        if(v == bt_RecordStop) {
            /** stop **/
            if(isRecord) {
                isRecord = false;
                bt_RecordStop.setText("Record");

                prompt();

                //imageView.setImageResource(android.R.color.transparent);
            }
            /** record **/
            else {
                isRecord = true;
                bt_RecordStop.setText("Stop");

                recordTask = new RecordAudio();
                recordTask.execute();
            }
        } else if (v == p_tv2) {
            // 저장된 파일 있다면 재생
            playAudio = new PlayAudio();
            playAudio.execute(fireAddress);
            // 없다면 fail toast
            //Toast.makeText(getApplicationContext(), "저장된 파일이 없습니다.", Toast.LENGTH_SHORT).show();
        } else if (v == p_tv1) {
            // 저장된 파일 있다면 재생
            playAudio = new PlayAudio();
            playAudio.execute(bellAddress);
            // 없다면 fail toast
            //Toast.makeText(getApplicationContext(), "저장된 파일이 없습니다.", Toast.LENGTH_SHORT).show();
        } else if(v == p_tv3) {
            playAudio = new PlayAudio();
            playAudio.execute(rangeAddress);
        }
    }

    // prompt 창 띄우기
    public void prompt() {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.prompts, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView.findViewById(R.id.editInput);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it to result
                                // edit text
                                p_name4.setText(userInput.getText());

                                long now = System.currentTimeMillis();

                                Date date = new Date(now);
                                SimpleDateFormat curDateFormat = new SimpleDateFormat("yyyy.MM.dd.");
                                String strCurDate = curDateFormat.format(date);

                                p_date4.setText(strCurDate);

                                Toast.makeText(getApplicationContext(), "등록되었습니다.", Toast.LENGTH_SHORT).show();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                               dialog.cancel();
                           }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    // Back Button
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
