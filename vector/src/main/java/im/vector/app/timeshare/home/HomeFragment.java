package im.vector.app.timeshare.home;




import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import im.vector.app.R;
import im.vector.app.timeshare.TSSessionManager;
import im.vector.app.timeshare.TSUtils.MyDialog;
import im.vector.app.timeshare.filter.FilterActivity;
import im.vector.app.timeshare.home.model.Event;
import im.vector.app.timeshare.home.model.OngoingModel;
import im.vector.app.timeshare.webservices.ApiUtils;
import im.vector.app.timeshare.api_response_body.EventResponse;
import im.vector.app.timeshare.webservices.RetrofitAPI;
import im.vector.app.timeshare.api_request_body.CommonRequest;
import retrofit2.Call;
import retrofit2.Callback;


public class HomeFragment extends Fragment {
    Context mContext;
    MyDialog myDialog;
    String uuid;

   // RequestQueue requestQueue;
    RecyclerView rv_ongoing_events;
   public static RecyclerView rv_allevents;
    ArrayList<OngoingModel> ongoingEventList = new ArrayList<>();
    ArrayList<Event> eventList = new ArrayList<>();
    RvOngoingAdapter rvOngoingAdapter;
    public static RvEventsAdapter rvEventsAdapter;
    ImageView iv_filter,iv_search;
    TSSessionManager tsSessionManager;
    androidx.appcompat.widget.SearchView mSearchView;
    RelativeLayout rl_searchlayout,rl_toolbar_main;
    ShimmerFrameLayout shimmer_ongoing_event,shimmerFrameLayout;
    private RetrofitAPI mAPIService = ApiUtils.getAPIService();
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
     View view =  inflater.inflate(R.layout.fragment_home, container, false);
     mContext = getActivity();
        tsSessionManager = new TSSessionManager(mContext);
        myDialog = new MyDialog(getActivity());


        iv_filter = view.findViewById(R.id.iv_filter);
        iv_filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, FilterActivity.class);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.enter, R.anim.exit);
            }
        });

           findView(view);

            HashMap<String, String> user = new HashMap<>();
            user = tsSessionManager.getUserDetails();
             uuid =  user.get(TSSessionManager.KEY_user_uuid);

            if (uuid!=null){

                Thread thread = new Thread(){
                    @Override
                    public void run() {
                        getAllActvity(uuid);
                    }
                };
                thread.start();
            }



        mSearchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mSearchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchEvent(newText);
                return true;
            }
        });


        iv_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rl_searchlayout.setVisibility(View.VISIBLE);
                rl_toolbar_main.setVisibility(View.GONE);

            }
        });


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        HashMap<String, String> user = new HashMap<>();
        user = tsSessionManager.getUserDetails();
         uuid =  user.get(TSSessionManager.KEY_user_uuid);
        System.out.println("uuid>>"+uuid);
        if (uuid!=null){

            Thread thread = new Thread(){
                @Override
                public void run() {
                    getAllActvity(uuid);
                }
            };
            thread.start();
        }
    }

    private void searchEvent(String string) {
        ArrayList<Event> filteredlist = new ArrayList<Event>();
        for (Event item : eventList) {
            // checking if the entered string matched with any item of our recycler view.
            if (item.getActivity_name().toLowerCase().contains(string)) {
                filteredlist.add(item);
            }
        }
        if (filteredlist.isEmpty()) {
            Toast.makeText(mContext, "No Event Found!", Toast.LENGTH_SHORT).show();
        } else {
          //  rvEventsAdapter.filterList(filteredlist);
        }
    }

    private void getAllActvity(String uuid) {
        CommonRequest timeLineRequest = new CommonRequest(uuid);
        Call<EventResponse> call = mAPIService.getTimelines(timeLineRequest);
        call.enqueue(new Callback<EventResponse>() {
            @Override
            public void onResponse(Call<EventResponse> call, retrofit2.Response<EventResponse> response) {
                 System.out.println("timeline>>" + response.toString());
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
                shimmer_ongoing_event.stopShimmer();
                shimmer_ongoing_event.setVisibility(View.GONE);

                if(response.body()!=null){

                   EventResponse eventResponse = response.body();
                    String message = eventResponse.getMsg();
                    System.out.println("activity_uuid>>"+message);
                     List<Event> events = eventResponse.getGet_timelines();
                            if (events!=null){
                                if (events.size()>0){
                                    for (Event event:events){
                                        String activity_uuid = event.getActivity_uuid();
                                        System.out.println("activity_uuid>>"+activity_uuid);
                                        String activity_name = event.getActivity_name();
                                        String activity_description = event.getActivity_description();
                                        String user_uuid = event.getUser_uuid();
                                        String profile_name = event.getProfile_name();
                                        String user_pic = event.getUser_pic();
                                        String first_name = event.getFirst_name();
                                        String last_name = event.getLast_name();
                                        String category_name = event.getCategory_name();
                                        String sub_category = event.getSub_category();
                                        String start_date_and_time = event.getStart_date_and_time();
                                        String end_date_and_time = event.getEnd_date_and_time();
                                        String post_path = event.getPost_path();
                                        String like_count = event.getLike_count();
                                        String is_like = event.getIs_like();
                                        String created_at = event.getCreated_at();
                                        String location = event.getLocation();
                                        List joiningUserList = event.getGetActivityJoinings();

                                        // add data in eventlist
                                        eventList.add(new Event(activity_uuid,activity_name,activity_description,
                                                user_uuid,profile_name,user_pic,first_name,last_name,category_name,sub_category,
                                                start_date_and_time,end_date_and_time,post_path,like_count,is_like,created_at,location,joiningUserList));
                                    }

                                }
                            }

                        // set data in all events
                        rv_allevents.setLayoutManager(new LinearLayoutManager(getActivity()));
                        rv_allevents.setHasFixedSize(true);
                        rvEventsAdapter = new RvEventsAdapter(getActivity(), eventList);
                        rv_allevents.setAdapter(rvEventsAdapter);
                        // ******************************************//* // ongoing event list
                        rv_ongoing_events.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL,false));
                        rv_ongoing_events.setHasFixedSize(true);
                        rvOngoingAdapter = new RvOngoingAdapter(getActivity(), eventList);
                        rv_ongoing_events.setAdapter(rvOngoingAdapter);

                }
            }

            @Override
            public void onFailure(Call<EventResponse> call, Throwable t) {
                System.out.println("error>>" + t.getCause());

            }
        });

    }


    private void findView(View view) {
        rv_ongoing_events = view.findViewById(R.id.rv_ongoing_events);
        rv_allevents = view.findViewById(R.id.rv_allevents);
        mSearchView = view.findViewById(R.id.searchView);
        rl_searchlayout = view.findViewById(R.id.rl_searchlayout);
        rl_toolbar_main = view.findViewById(R.id.rl_toolbar_main);
        iv_search = view.findViewById(R.id.iv_search);
        shimmer_ongoing_event = view.findViewById(R.id.shimmer_ongoing_event);
       shimmerFrameLayout = view.findViewById(R.id.shimmer_view_container);
        shimmerFrameLayout.startShimmer();
        shimmer_ongoing_event.startShimmer();
    }
}
