package symphome.de.whatsapp;

import android.Manifest;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    Button btSend;
    Button btShowMessages;
    EditText etSend;

    OkHttpClient client = new OkHttpClient();

    List<String> messages = new ArrayList<>();
    ArrayAdapter<String> adapter;

    Thread updateMessageThread = new Thread(new UpdateMessageThread());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView lvMessaages = findViewById(R.id.lvMessages);
        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                messages
        );
        lvMessaages.setAdapter(adapter);

        btSend = findViewById(R.id.btSendMessage);
        btSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMesssage();
            }
        });

        btShowMessages = findViewById(R.id.btShowMessages);
        btShowMessages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Getting Messages", Toast.LENGTH_SHORT).show();
                getMessages();
            }
        });


        updateMessageThread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateMessageThread.interrupt();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMessageThread.start();
    }

    private void sendMesssage(){
        etSend = findViewById(R.id.etMessage);
        if(etSend.getText().toString().equals("")) {
            return;
        }

        String message = "";
        try {
            message = URLEncoder.encode(etSend.getText().toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        long timeStamp = Calendar.getInstance().getTimeInMillis();

        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/AndroidServices/ReceiveMessageServlet?"
                        + "time=" + timeStamp + "&message=" + message)
                .build();

        client.newCall(request).enqueue(
                new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MainActivity.this.etSend.setText("");
                                MainActivity.this.getMessages();
                            }
                        });
                    }
                }
        );
    }

    private void getMessages(){
        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/AndroidServices/SendMessageServlet")
                .build();

        client.newCall(request).enqueue(
                new Callback() {
                    @Override
                    public void onFailure(Call call, IOException ex) {
                        Log.d("",ex.getMessage());

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        // Log.d("", response.body().string());
                        String body = response.body().string();
                        if(body.contains(":")){
                            String[] messages = body.split("\n");
                            MainActivity.this.messages = null;
                            MainActivity.this.messages = new ArrayList<>(Arrays.asList(messages));
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    MainActivity.this.showMessages();
                                }
                            });
                        }
                    }
                }
        );
    }

    public void showMessages() {
        Log.d("", "------------------------SIZE:" + this.messages.size());

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        for (int i = 0; i < messages.size(); i++) {
            try {
                long yourmilliseconds = Long.parseLong(messages.get(i).substring(0, messages.get(i).indexOf(":")));
                Log.d("", messages.get(i).substring(0, messages.get(i).indexOf(":")));
                Date resultdate = new Date(yourmilliseconds);
                messages.set(i, sdf.format(resultdate) + messages.get(i).substring(messages.get(i).indexOf(":")));
            } catch (Exception e) {
                Log.d("", messages.get(i).substring(0, messages.get(i).indexOf(":")));
            }
        }

        ListView lvMessaages = findViewById(R.id.lvMessages);
        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                this.messages
        );
        lvMessaages.setAdapter(adapter);
    }

    class UpdateMessageThread implements Runnable{

        @Override
        public void run() {
            boolean isRunning;
            try {
                isRunning = true;
                while(isRunning) {
                    Log.d("", "####################: Update Messages");
                    MainActivity.this.getMessages();
                    Thread.sleep(1000);
                }

            } catch (Exception e) {
                isRunning = false;
            } //try-catch
        } // Method Run
    }
}
