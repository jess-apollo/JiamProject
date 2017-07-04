package com.example.free.sixsense;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

import static java.lang.Math.abs;

public class MainActivity extends Activity implements View.OnClickListener {

    // AudioRecord : 주파수 - 12kHz, 오디오 채널 - mono, 샘플 - pcm 16비트
    int frequency = 12000;
    int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    private DoubleFFT_1D transformer;
    int blockSize = 1024;
    int doubleSize = 1024;

    Button bt_StartStop;
    Button bt_upload;
    boolean started = false;
    boolean isDialog = false;

    RecordAudio recordTask;

    Handler handler = new Handler();

    //TextView alarm;

    // 주파수 파형 그리기
    ImageView imageView;
    Bitmap bitmap;
    Canvas canvas;
    Paint paint;

    // double (fft용)
    double[] bell = new double[doubleSize];
    double[] fire = new double[doubleSize];
    double[] range = new double[doubleSize];

    double[] bellTemp = new double[doubleSize];
    double[] fireTemp = new double[doubleSize];
    double[] rangeTemp = new double[doubleSize];

    double maxB =0, maxF = 0, maxR = 0;
    int indexB = 0, indexF = 0, indexR = 0;


    // File
    File bellAddress = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/recorder/Bell.pcm");
    File fireAddress = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/recorder/Fire.pcm");
    File rangeAddress = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/recorder/Ragne.pcm");

    // 진동
    Vibrator vibe;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bt_StartStop = (Button)findViewById(R.id.bt_StartStop);
        bt_upload = (Button)findViewById(R.id.bt_upload);
        bt_StartStop.setOnClickListener(this);
        bt_upload.setOnClickListener(this);

        // FFT
        transformer = new DoubleFFT_1D(512);

        //
        //alarm = (TextView) findViewById(R.id.alarmView);

        // ImageView & 주파수 파형 그리기
        imageView = (ImageView)findViewById(R.id.fftView);
        bitmap = Bitmap.createBitmap((int)512, (int)300, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        paint.setColor(Color.rgb(155,83,91));
        imageView.setImageBitmap(bitmap);

        // 진동
        vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // .pcm data -> fft
        // Bell
        try {
            DataInputStream disB = new DataInputStream(new BufferedInputStream(new FileInputStream(bellAddress)));
            short[] bufferb = new short[blockSize];

            int i = 0;

             while(disB.available() > 0) {
                i = 0;
                while(i < blockSize-1) {
                    if(disB.available()>0) {
                        bufferb[i] = disB.readShort();
                        bellTemp[i] = (double)bufferb[i] / Short.MAX_VALUE; // 부호 있는 16비트

                    } else {
                        bellTemp[i] = 0;

                    }
                    i++;
                }
                transformer.realForwardFull(bellTemp);

                for(i = 0; i < blockSize-1; i++)
                {
                    bell[i] += bellTemp[i];
                }
                 for(i = 0; i < blockSize-1; i++) {
                     if(maxB < bell[i]) {
                         maxB = bell[i];
                         indexB = i;
                     }
                 }
            }
        } catch (Throwable t) {
            Log.e("AudioTrack", "File is not Found");
        }

        // Fire
        try {
            DataInputStream disF = new DataInputStream(new BufferedInputStream(new FileInputStream(fireAddress)));
            short[] bufferf = new short[blockSize];

            int i = 0;

            while(disF.available() > 0) {
                i = 0;
                while(i < blockSize-1) {
                    if(disF.available()>0) {
                        bufferf[i] = disF.readShort();
                        fireTemp[i] = (double)bufferf[i] / Short.MAX_VALUE; // 부호 있는 16비트

                    } else {
                        fireTemp[i] = 0;
                    }
                    i++;
                }
                transformer.realForwardFull(fireTemp);

                for(i = 0; i < blockSize-1; i++)
                {
                    fire[i] += fireTemp[i];
                }
            }
            for(i = 0; i < blockSize-1; i++) {
                if (maxF < fire[i]) {
                    maxF = fire[i];
                    indexF = i;
                }
            }
        } catch (Throwable t) {
            Log.e("AudioTrack", "File is not Found");
        }

        // Range
        try {
            DataInputStream disR = new DataInputStream(new BufferedInputStream(new FileInputStream(rangeAddress)));
            short[] bufferr = new short[blockSize];

            int i = 0;

            while(disR.available() > 0) {
                i = 0;
                while(i < blockSize-1) {
                    if(disR.available()>0) {
                        bufferr[i] = disR.readShort();
                        rangeTemp[i] = (double)bufferr[i] / Short.MAX_VALUE; // 부호 있는 16비트

                    } else {
                        rangeTemp[i] = 0;
                    }
                    i++;
                }
                transformer.realForwardFull(rangeTemp);

                for(i = 0; i < blockSize-1; i++)
                {
                    range[i] += rangeTemp[i];
                }
            }
            for(i = 0; i < blockSize-1; i++) {
                if (maxR < range[i]) {
                    maxR = range[i];
                    indexR = i;
                }
            }
        } catch (Throwable t) {
            Log.e("AudioTrack", "File is not Found");
        }
        //int i = 0;
    }

    public void compare(final int indexD, final double maxD) {

        handler.post(new Runnable() {
            @Override
            public void run() {
                if((960 <= indexD && indexD <= 990) && (100 < maxD && maxD <= 120)) {
                    promptB();
                    vibe.vibrate(1000);
                } else if((680 <= indexD && indexD <= 720) && (20 <= maxD && maxD <= 50)) {
                    promptF();
                    vibe.vibrate(1000);
                } else if((140 <= indexD && indexD <= 200) && (80 < maxD && maxD <= 120)) {
                    promptR();
                    vibe.vibrate(1000);
                }
            }
        });
    }


    // fft 출력용
    private class RecordAudio extends AsyncTask<Void, double[], Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try{
                // AudioRecord 설정
                int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);

                AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency, channelConfiguration, audioEncoding, bufferSize);

                // short형 buffer - PCM형 데이터
                // Double_FFT_1D parameter - double형
                short[] buffer = new short[blockSize];
                double[] toTransform = new double[blockSize];

                audioRecord.startRecording();

                while(started){
                    int bufferReadResult = audioRecord.read(buffer, 0, blockSize);

                    // AudioRecord 객체에서 데이터를 읽기 -> short 형 변수 -> double 형 변환
                    // 값의 범위 -  -1.0 ~ 1.0   => 형 변환 X
                    // short를 32,768.0(Short.MAX_VALUE) 으로 나누면 double로 타입이 바뀜 (short의 최대값)
                    for(int i = 0; i < blockSize-1 && i < bufferReadResult; i++) {
                        toTransform[i] = (double)buffer[i] / Short.MAX_VALUE; // 부호 있는 16비트
                    }


                    // 512가지 값(범위) 사용,  샘플 비율 -  12,000 //  배열의 각 요소 => *23.4Hz (= 12000/2/512) // 최대 인지 가능 주파수 4000 ( 8000/2 )
                    // 따라서 배열의 첫 번째 데이터 -> 영(0)과 23.4Hz 사이 주파수
                    try {
                        // fft , imag = 0
                        transformer.realForwardFull(toTransform);

                    } catch (Exception e)
                    {
                        Log.e("AudioRecord", "Recording Failed");
                    }

                    // publishProgress -> onProgressUpdate call
                    publishProgress(toTransform);

                    int index = 0;
                    int max = 0;

                    int check=0;
                    int checkB, checkF, checkR = 0;

                    for(int i = 0; i < blockSize-1; i++) {
                        if(max < toTransform[i]) {
                            max = (int)toTransform[i];
                            index = i;
                        }
                    }
                    compare(index, max);
                }

                audioRecord.stop();
            }catch(Throwable t){
                Log.e("AudioRecord", "Recording Failed");
            }
            return null;
        }

        // onProgressUpdate -> 메인 스레드로 실행
        // 각 세로선은 배열의 행 하나씩을 나타냄 (단위 주파수 : 11.719Hz)
        @Override
        protected void onProgressUpdate(double[]... toTransform) {

            canvas.drawColor(Color.rgb(232,203,143));

            for(int i = 0; i < toTransform[0].length; i++){
                int x = i;
                int downy = (int) (300 - (toTransform[0][i] * 10));
                int upy = 300;

                canvas.drawLine(x, downy, x, upy, paint);
            }
            imageView.invalidate();
        }
    }

    // Click 처리
    @Override
    public void onClick(View arg0) {
        if(arg0 == bt_StartStop) {
            if (started) {
                started = false;
                bt_StartStop.setText("Start");
                recordTask.cancel(true);
            } else {
                started = true;
                bt_StartStop.setText("Stop");
                recordTask = new RecordAudio();
                recordTask.execute();
          }
        } else if(arg0 == bt_upload) {
            started = false;
            bt_StartStop.setText("Start");
            Intent intent = new Intent(MainActivity.this, EnrollActivity.class);
            startActivity(intent);
        }
    }

    /***********************************************************************************************/
    // prompt_bell 창 띄우기
    public void promptB() {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.bell, null);

        //started = false;
        //bt_StartStop.setText("Start");

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it to result
                                dialog.dismiss();

                                isDialog = false;
                            }
                        });

        if(!isDialog) {
            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();

            // show it
            alertDialog.show();

            isDialog = true;
        }
    }

    // prompt_bell 창 띄우기
    public void promptF() {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.fire, null);

        started = false;
        bt_StartStop.setText("Start");

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it to result
                                isDialog = false;
                            }
                        });

        if(!isDialog) {
            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();

            // show it
            alertDialog.show();

            isDialog = true;
        }
    }

    // prompt_bell 창 띄우기
    public void promptR() {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.range, null);

        //started = false;
        //bt_StartStop.setText("Start");

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it to result
                                isDialog = false;
                            }
                        });

        if(!isDialog) {
            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();

            // show it
            alertDialog.show();

            isDialog = true;
        }
    }
}
