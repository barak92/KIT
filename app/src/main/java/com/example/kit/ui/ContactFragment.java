package com.example.kit.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import de.hdodenhof.circleimageview.CircleImageView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.kit.R;
import com.example.kit.UserClient;

import static android.view.View.VISIBLE;
import static com.example.kit.Constants.FRIENDS;
import static com.example.kit.Constants.MY_REQUEST_PENDING;
import static com.example.kit.Constants.NOT_FRIENDS;
import static com.example.kit.Constants.THEIR_REQUEST_PENDING;

public class ContactFragment extends DBGeoFragment implements
        View.OnClickListener{

    //Tag
    private static final String TAG = "ConatactFragment";

    //widgets
    private CircleImageView mAvatarImage;
    private TextView mProfileName, mUserName, mProfileStatus;
    private Button mSendReqBtn, mAcceptBtn, mDeclineBtn, mDeleteReqBtn, mDeleteBtn;

    //vars
    private String mCurrent_state;

    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    public ContactFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrent_state = NOT_FRIENDS;
        checkState();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_contact, container, false);
        initView(v);
        return v;
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void checkState(){

    }

    private void initView(View v){
        mAvatarImage = v.findViewById(R.id.image_choose_avatar);
        mProfileName = v.findViewById(R.id.profile_displayName);
        mProfileStatus = v.findViewById(R.id.profile_status);
        mSendReqBtn = v.findViewById(R.id.profile_send_req_btn);
        mAcceptBtn = v.findViewById(R.id.profile_accept_req_btn);
        mDeclineBtn = v.findViewById(R.id.profile_decline_btn);
        mDeleteReqBtn = v.findViewById(R.id.profile_delete_request_btn);
        mDeleteBtn = v.findViewById(R.id.profile_delete_btn);
        initState();
        retrieveProfileImage();
    }

    private void initState(){
        switch(mCurrent_state){
            case NOT_FRIENDS:{
                mSendReqBtn.setVisibility(VISIBLE);
                mSendReqBtn.setOnClickListener(this);
                break;
            }
            case FRIENDS:{
                mDeleteBtn.setVisibility(VISIBLE);
                mDeleteBtn.setOnClickListener(this);
                break;
            }
            case MY_REQUEST_PENDING:{
                mDeleteReqBtn.setVisibility(VISIBLE);
                mDeleteReqBtn.setOnClickListener(this);
                break;
            }
            case THEIR_REQUEST_PENDING:{
                mAcceptBtn.setVisibility(VISIBLE);
                mDeclineBtn.setVisibility(VISIBLE);
                mAcceptBtn.setOnClickListener(this);
                mDeclineBtn.setOnClickListener(this);
                break;
            }
        }
    }

    private void retrieveProfileImage(){
        RequestOptions requestOptions = new RequestOptions()
                .error(R.drawable.cartman_cop)
                .placeholder(R.drawable.cartman_cop);
        int avatar = 0;
        try{
            avatar = Integer.parseInt(((UserClient)mActivity.getApplicationContext()).getUser().getAvatar());
        }catch (NumberFormatException e){
            Log.e(TAG, "retrieveProfileImage: no avatar image. Setting default. " + e.getMessage() );
        }

        Glide.with(mActivity)
                .setDefaultRequestOptions(requestOptions)
                .load(avatar)
                .into(mAvatarImage);
    }

    /*
    ----------------------------- onClick ---------------------------------
    */

    @Override
    public void onClick(View v) {
        switch(v.getId()){
//            case R.id.profile_send_req_btn:{
//                sendRequest();
//                break;
//            }
//            case R.id.profile_accept_req_btn:{
//                acceptRequest();
//                break;
//            }
//            case R.id.profile_decline_btn:{
//                declineRequest();
//                break;
//            }
//            case R.id.profile_delete_request_btn:{
//                deleteRequest();
//                break;
//            }
//            case R.id.profile_delete_btn:{
//                deleteContact();
//                break;
//            }
        }
    }
}