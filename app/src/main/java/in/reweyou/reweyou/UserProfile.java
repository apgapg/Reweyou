package in.reweyou.reweyou;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import in.reweyou.reweyou.adapter.MessageAdapter;
import in.reweyou.reweyou.adapter.CommentsAdapter;
import in.reweyou.reweyou.classes.ConnectionDetector;
import in.reweyou.reweyou.classes.DividerItemDecoration;
import in.reweyou.reweyou.classes.RequestHandler;
import in.reweyou.reweyou.classes.UserSessionManager;
import in.reweyou.reweyou.model.CommentsModel;
import in.reweyou.reweyou.model.MpModel;

public class UserProfile extends AppCompatActivity implements View.OnClickListener {
    private String i, tag, number, user, result;
    private TextView Name, Reports, Info, Readers;
    private ImageView profilepic;
    private LinearLayout click;
    private int length;
    private Button button;
    Boolean isInternetPresent = false;
    ConnectionDetector cd;
    UserSessionManager session;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private DisplayImageOptions option;
    ImageLoader imageLoader = ImageLoader.getInstance();
    ArrayList<String> profilelist = new ArrayList<>();

    public static final String URL_VERIFY_FOLLOW = "https://www.reweyou.in/reweyou/verify_follow.php";
    public static final String URL_FOLLOW = "https://www.reweyou.in/reweyou/follow_new.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initCollapsingToolbar();
        Intent in=getIntent();
        Bundle bundle = getIntent().getExtras();
        i = bundle.getString("myData");
        cd = new ConnectionDetector(UserProfile.this);
        session = new UserSessionManager(getApplicationContext());
        progressBar = (ProgressBar)findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        Name =(TextView)findViewById(R.id.Name);
        Reports =(TextView)findViewById(R.id.Reports);
        Info =(TextView)findViewById(R.id.Info);
        Readers=(TextView)findViewById(R.id.Readers);
        click=(LinearLayout)findViewById(R.id.click);

        button = (Button)findViewById(R.id.button);
        profilepic=(ImageView)findViewById(R.id.profilepic);

        String fontPath = "fonts/Roboto-Medium.ttf";
        String thinpath="fonts/Roboto-Regular.ttf";
        Typeface font = Typeface.createFromAsset(this.getAssets(), "fontawesome-webfont.ttf");
        Typeface tf = Typeface.createFromAsset(this.getAssets(), fontPath);
        Typeface thin = Typeface.createFromAsset(this.getAssets(), thinpath);

        Name.setTypeface(tf);
        Reports.setTypeface(thin);
        Info.setTypeface(thin);
        Readers.setTypeface(thin);

        button.setVisibility(View.GONE);
        recyclerView=(RecyclerView)findViewById(R.id.recycler_view);
        RecyclerView.ItemDecoration dividerItemDecoration = new DividerItemDecoration(ContextCompat.getDrawable(this, R.drawable.line));
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        option = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.irongrip)
                .displayer(new RoundedBitmapDisplayer(1000))
                .showImageForEmptyUri(R.drawable.download)
                .showImageOnFail(R.drawable.download)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();

        //Progress bar
        tag="Random";
        button.setOnClickListener(this);
        click.setOnClickListener(this);
        isInternetPresent = cd.isConnectingToInternet();
        if(isInternetPresent) {
            new JSONTask().execute(i);
                   new JSONTasks().execute(tag,i);
            button(i);
        }
        else {
            Toast.makeText(this, "You are not connected to Internet", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onClick(View v) {
       switch (v.getId()) {
           case R.id.button:
           final int status = (Integer) button.getTag();
           if (status == 1) {
               read(i);
           } else {
               reading(i);
           }
           break;
           case R.id.click:
               Bundle bundle = new Bundle();
               bundle.putString("myData", user);
               Intent in = new Intent(this, Readers.class);
               in.putExtras(bundle);
               startActivity(in);
               break;
       }
    }

    public class JSONTask extends AsyncTask<String, String, List<String>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected List<String> doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            RequestHandler rh= new RequestHandler();
            HashMap<String, String> data = new HashMap<String,String>();
            data.put("number",params[0]);
            try {
                URL url = new URL("https://www.reweyou.in/reweyou/user_list.php");
                connection = (HttpURLConnection) url.openConnection();

                connection.setDoOutput(true);
                connection.setRequestMethod("POST");

                OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
                wr.write(rh.getPostDataString(data));
                wr.flush();


                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();

                String line = "";
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
                String finalJson = buffer.toString();

                JSONArray parentArray = new JSONArray(finalJson);
                StringBuffer finalBufferedData = new StringBuffer();

                for (int i = 0; i < parentArray.length(); i++) {
                    JSONObject finalObject = parentArray.getJSONObject(i);
                    profilelist.add(finalObject.getString("name"));
                    profilelist.add(finalObject.getString("total_reviews"));
                   profilelist.add(finalObject.getString("profilepic"));
                    profilelist.add(finalObject.getString("info"));
                    profilelist.add(finalObject.getString("number"));
                    profilelist.add(finalObject.getString("readers"));
                }

                return profilelist;

                //return buffer.toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<String> result) {
            super.onPostExecute(result);
            Name.setText(result.get(0));
           Reports.setText(result.get(1));
            Info.setText(result.get(3));
            imageLoader.displayImage(result.get(2), profilepic, option);
            user = result.get(4);
            Readers.setText(result.get(5));
        //    progressBar.setVisibility(View.GONE);
            //need to set data to the list
    }
    }
    public class JSONTasks extends AsyncTask<String, String, List<MpModel>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected List<MpModel> doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            RequestHandler rh= new RequestHandler();
            HashMap<String, String> data = new HashMap<String,String>();
            data.put("tag",params[0]);
            data.put("number",params[1]);
            //  tag="All";
            try {
                URL url = new URL("https://www.reweyou.in/reweyou/myreports.php");
                connection = (HttpURLConnection) url.openConnection();

                connection.setDoOutput(true);
                connection.setRequestMethod("POST");


                OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
                wr.write(rh.getPostDataString(data));
                wr.flush();

                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();

                String line = "";
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
                String finalJson = buffer.toString();

                JSONArray parentArray = new JSONArray(finalJson);
                StringBuffer finalBufferedData = new StringBuffer();
                length=parentArray.length();
                List<MpModel> messagelist = new ArrayList<>();

                Gson gson = new Gson();
                for (int i = 0; i < parentArray.length(); i++) {
                    JSONObject finalObject = parentArray.getJSONObject(i);
                    MpModel mpModel = gson.fromJson(finalObject.toString(), MpModel.class);
                    messagelist.add(mpModel);
                }

                return messagelist;
                //return buffer.toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<MpModel> result) {
            super.onPostExecute(result);
            progressBar.setVisibility(View.GONE);
            MessageAdapter adapter = new MessageAdapter(UserProfile.this,result);
           // total.setText("You have reported "+ String.valueOf(length)+ " stories.");
            recyclerView.setAdapter(adapter);
            //need to set data to the list
        }
    }

    private void button(final String i) {
        final String number = session.getMobileNumber();
        // final ProgressDialog loading = ProgressDialog.show(this, "Authenticating", "Please wait", false, false);
        StringRequest strReq = new StringRequest(Request.Method.POST,
                URL_VERIFY_FOLLOW, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                //if the server response is success
                if(response.equalsIgnoreCase("success")){
                    //dismissing the progressbar
                    //     loading.dismiss();

                    //Starting a new activity
                    button.setVisibility(View.VISIBLE);
                    button.setBackgroundColor(ContextCompat.getColor(UserProfile.this, R.color.red));
                    button.setTextColor(ContextCompat.getColor(UserProfile.this, R.color.transparent_bg));
                    button.setText("Unread");
                    button.setTag(1);
                }else{
                    //Displaying a toast if the otp entered is wrong
                    button.setVisibility(View.VISIBLE);
                    button.setBackgroundColor(ContextCompat.getColor(UserProfile.this, R.color.feedbackground));
                    button.setTextColor(ContextCompat.getColor(UserProfile.this, R.color.black));
                    button.setText("Read");
                    button.setTag(0);
                }
            }
        },new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //   Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
                Toast.makeText(getApplicationContext(),"Something went wrong, Try again",Toast.LENGTH_LONG).show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("user", i);
                params.put("number", number);

                return params;
            }
        };
        // Adding request to request queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(strReq);
    }


    private void reading(final String i) {
        HashMap<String, String> user = session.getUserDetails();
        final String number=session.getMobileNumber();
        final String name=session.getUsername();
        final ProgressDialog loading = ProgressDialog.show(UserProfile.this, "Updating", "Please wait", false, false);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_FOLLOW,
                new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {
                        if (response.trim().equals("success")) {
                            //button.setText("Reviewed");
                            loading.dismiss();
                            button.setBackgroundColor(ContextCompat.getColor(UserProfile.this, R.color.red));
                            button.setTextColor(ContextCompat.getColor(UserProfile.this, R.color.transparent_bg));
                            button.setText("Unread");
                            button.setTag(1);
                        } else {
                            loading.dismiss();
                            Toast.makeText(UserProfile.this, "Try Again", Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        loading.dismiss();
                        Toast.makeText(UserProfile.this, "Something went wrong!", Toast.LENGTH_LONG).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> map = new HashMap<String, String>();
                map.put("number", number);
                map.put("user",i);
                map.put("name",name);
                map.put("unread","reading");
                return map;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(UserProfile.this);
        requestQueue.add(stringRequest);
    }

    private void read(final String i) {
        HashMap<String, String> user = session.getUserDetails();
         number=session.getMobileNumber();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_FOLLOW,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (response.trim().equals("success")) {
                            //button.setText("Reviewed");
                            button.setBackgroundColor(ContextCompat.getColor(UserProfile.this, R.color.feedbackground));
                            button.setTextColor(ContextCompat.getColor(UserProfile.this, R.color.black));
                            button.setText("Read");
                            button.setTag(0);
                        } else {
                            Toast.makeText(UserProfile.this, "Try Again", Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(UserProfile.this, error.toString(), Toast.LENGTH_LONG).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> map = new HashMap<String, String>();
                map.put("number", number);
                map.put("user",i);
                return map;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(UserProfile.this);
        requestQueue.add(stringRequest);
    }

    /* Initializing collapsing toolbar
    * Will show and hide the toolbar title on scroll
    */
    private void initCollapsingToolbar() {
        final CollapsingToolbarLayout collapsingToolbar =
                (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(" ");
        AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        appBarLayout.setExpanded(true);

        // hiding & showing the title when toolbar expanded & collapsed
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = false;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    collapsingToolbar.setTitle(getString(R.string.app_name));
                    isShow = true;
                } else if (isShow) {
                    collapsingToolbar.setTitle(" ");
                    isShow = false;
                }
            }
        });
    }

}
