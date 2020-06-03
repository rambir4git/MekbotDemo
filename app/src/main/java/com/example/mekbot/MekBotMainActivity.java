package com.example.mekbot;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class MekBotMainActivity extends AppCompatActivity implements Communication.CompleteListener{

    private static final String TAG = MekBotMainActivity.class.getSimpleName();
    private static final int USER = 10001;
    private static final int BOT = 10002;
    private static final int TABLE = 10003;
    private static final int ELEMENT = 10004;

    private LinearLayout chatLayout;
    private EditText queryEditText;
    private MaterialProgressBar progressBar;
    private RecyclerView suggestions;
    private TextView name,status;

    // previousContext means what topics were discussed before current query
    private JSONArray previousContext = null;

    //  clickListener for table entries
    private View.OnClickListener tableElementClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Toolbar toolbar = findViewById(R.id.mekbot_toolbar);
        this.setSupportActionBar(toolbar);

        final ScrollView scrollview = findViewById(R.id.chatScrollView);
        scrollview.post(() -> scrollview.fullScroll(ScrollView.FOCUS_DOWN));

        chatLayout = findViewById(R.id.chatLayout);
        progressBar = findViewById(R.id.progressbar);
        queryEditText = findViewById(R.id.queryEditText);
        suggestions = findViewById(R.id.recyclerView);


        //send initial messages so that will start talking, lazy way to initialize a bot xD
        sendMessage("List all bot services");


        //other click listeners
        clicklistener();
    }


    void clicklistener(){

        findViewById(R.id.help).setOnClickListener(v -> sendMessage("List all bot services"));
        findViewById(R.id.navigation_drawer).setOnClickListener(v -> Toast.makeText(this, "Open/close drawer", Toast.LENGTH_SHORT).show());
        findViewById(R.id.attach).setOnClickListener(v -> Toast.makeText(this, "File attach implementation", Toast.LENGTH_SHORT).show());
        findViewById(R.id.camera).setOnClickListener(v -> Toast.makeText(this, "Pics attach implementation", Toast.LENGTH_SHORT).show());

        tableElementClickListener = v -> {
            TextView text = v.findViewById(R.id.text);
            sendMessage(text.getText().toString());
        };

        queryEditText.setOnKeyListener((v, keyCode, event) -> {

            // when enter key is pressed
            if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)) {

                // send message to bot
                sendMessage(queryEditText.getText().toString());
                return true;
            }
            return false;
        });
    }


    @Override
    public void TextMessages(JSONArray fulfillmentMessages) {

        AsyncTask.execute(() -> {
            for(int i=0;i<fulfillmentMessages.length();i++){
                try {
                    showTextView(fulfillmentMessages.getString(i),BOT);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void QuickReplies(JSONArray quickReplies) {

        List<String> suggestedChips = new ArrayList<>();
        QuickRepliesAdapter quickRepliesAdapter = new QuickRepliesAdapter(suggestedChips);
        suggestions.setAdapter(quickRepliesAdapter);
        suggestions.requestChildFocus(null,null);

        AsyncTask.execute(() -> {
            for(int i=0;i<quickReplies.length();i++){
                try {
                    suggestedChips.add(quickReplies.getString(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                runOnUiThread(() -> quickRepliesAdapter.notifyDataSetChanged());
            }
        });
    }

    @Override
    public void CustomPayload(JSONObject payload) {

        if(payload.length()>0){

            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {

                    List<String> elements = new ArrayList<>();
                    String title = "List";
                    try {
                        title = payload.getJSONObject("fields").getJSONObject("title").getString("stringValue");
                        JSONArray temp = payload.getJSONObject("fields").getJSONObject("list").getJSONObject("listValue").getJSONArray("values");
                        for(int i=0;i<temp.length();i++){
                            elements.add(temp.getJSONObject(i).getString("stringValue"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    showTable(elements,title);
                }
            });
        }

    }

    @Override
    public void Contexts(JSONArray outputContexts) {

        // these outputContext will be used as previousContext for future query
        previousContext = outputContexts;

        // hide progress bar
        progressBar.setVisibility(View.INVISIBLE);

    }

    @Override
    public void Error(Exception error) {

        // show toast to user
        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        Log.e(TAG, "Communication Error", error);

        // hide progress bar
        progressBar.setVisibility(View.INVISIBLE);

    }


    private void sendMessage(String query) {

        if (query.trim().isEmpty()) {

            Toast.makeText(this, "You cannot send an empty query!", Toast.LENGTH_LONG).show();

        } else {

            // display user msg
            if(!query.equals("List all bot services"))
            showTextView(query, USER);

            // clear input field
            queryEditText.setText("");

            // remove suggestions
            suggestions.setAdapter(null);

            // display progress bar
            progressBar.setVisibility(View.VISIBLE);

            // send query to bot
            Communication communication = Communication.getInstance();
            Communication.toChatbot(communication,previousContext,query,this);

        }
    }


    private void showTable(List<String> list, String titleText){

        FrameLayout layout = getLayout(TABLE);
        TableLayout table = layout.findViewById(R.id.table);
        TextView title = layout.findViewById(R.id.tableTitle);

        title.setText(titleText);

        for(int i=0;i<list.size();i+=2){

            TableRow row = new TableRow(MekBotMainActivity.this);

            FrameLayout element1 = getLayout(ELEMENT);
            row.addView(element1);
            TextView text1 = element1.findViewById(R.id.text);
            text1.setText(list.get(i));
            element1.setOnClickListener(tableElementClickListener);


            FrameLayout element2 = getLayout(ELEMENT);
            TextView text2 = element2.findViewById(R.id.text);
            row.addView(element2);
            if(i+1<list.size()){
                text2.setText(list.get(i+1));
                element2.setOnClickListener(tableElementClickListener);
            }

            runOnUiThread(()-> table.addView(row));
        }

        runOnUiThread(() -> {
            chatLayout.addView(layout);
            layout.getParent().requestChildFocus(layout,layout);
        });

    }


    private void showTextView(String message, int type) {

        FrameLayout layout = getLayout(type);

        TextView msg = layout.findViewById(R.id.chatMsg);
        msg.setText(message);

        TextView time = layout.findViewById(R.id.chatTime);
        time.setText(getCurrentTime());

        // move focus to text view to automatically make it scroll up
        runOnUiThread(() -> {
            chatLayout.addView(layout);
            layout.getParent().requestChildFocus(layout,layout);
        });

    }


    String getCurrentTime() {
        Date date = new Date(Calendar.getInstance().getTimeInMillis());
        SimpleDateFormat format = new SimpleDateFormat("hh:mm a");
        return format.format(date);
    }

    FrameLayout getLayout(int layoutCode) {
        LayoutInflater inflater = LayoutInflater.from(MekBotMainActivity.this);
        FrameLayout layout;
        switch (layoutCode){

            case USER : layout = (FrameLayout) inflater.inflate(R.layout.user_msg_layout, null);
            break;

            case BOT : layout = (FrameLayout) inflater.inflate(R.layout.bot_msg_layout, null);
            break;

            case TABLE : layout = (FrameLayout) inflater.inflate(R.layout.bot_list,null);
            break;

            case ELEMENT : layout = (FrameLayout) inflater.inflate(R.layout.bot_list_element,null);
            break;

            default: throw new IllegalStateException("Unexpected value: " + layoutCode);
        }
        return layout;
    }


    class QuickRepliesAdapter extends RecyclerView.Adapter<QuickRepliesAdapter.QuickRepliesViewHolder>{
        private List<String> list;

        QuickRepliesAdapter(List<String> list){
            this.list = list;
        }

        @NonNull
        @Override
        public QuickRepliesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new QuickRepliesViewHolder(MekBotMainActivity.this.getLayoutInflater().inflate(R.layout.recycler_suggestion,parent,false));
        }

        @Override
        public void onBindViewHolder(@NonNull QuickRepliesViewHolder holder, int position) {
            holder.chip.setText(list.get(position));
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendMessage(holder.chip.getText().toString());
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }


        class QuickRepliesViewHolder extends RecyclerView.ViewHolder {
            TextView chip;

            QuickRepliesViewHolder(View view){
                super(view);
                chip = view.findViewById(R.id.chip);
            }
        }

    }

}
