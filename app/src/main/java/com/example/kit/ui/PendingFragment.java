package com.example.kit.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.kit.Constants;
import com.example.kit.R;
import com.example.kit.adapters.ContactRecyclerAdapter;
import com.example.kit.models.Contact;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;

import static android.widget.LinearLayout.HORIZONTAL;
import static com.example.kit.Constants.REMOVE_REQUEST;

public class PendingFragment extends DBGeoFragment implements
        ContactRecyclerAdapter.ContactsRecyclerClickListener
{

    //Tag
    private static final String TAG = "PendingFragment";

    //RecyclerView
    private ContactRecyclerAdapter mPendingRecyclerAdapter;
    private RecyclerView mPendingRecyclerView;
    private ArrayList<Contact> mRecyclerList;

    //Pending
    private HashMap<String, Contact> mPending;

    //Pending Callback
    PendingCallback getData;

    //Listener
    private ListenerRegistration mPendingEventListener;

    //Var
    private PendingFragment mPendingFragment;


//    private static RequestHandler rHandler = new RequestHandler();

    /*
    ----------------------------- Lifecycle ---------------------------------
    */
    public PendingFragment() {
        super();
    }

    public static PendingFragment newInstance() {
        return new PendingFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mPendingFragment = this;
        getData = (PendingCallback)context;
        mPending = getData.getPending();
        mRecyclerList = new ArrayList<>(mPending.values());
        initListener();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_pending, container, false);
        initView(rootView);
        return rootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mPendingEventListener.remove();
    }

    @Override
    public void onResume() {
        initListener();
        super.onResume();
        getPending();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPendingEventListener != null) {
            mPendingEventListener.remove();
        }
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void getPending(){
        mDb.collection(getString(R.string.collection_users)).document(FirebaseAuth.getInstance().getUid()).collection(getString(R.string.collection_pending)).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (!task.isSuccessful()){ return; }
                mPending.clear();
                for (QueryDocumentSnapshot doc: task.getResult()){
                    Contact contact = doc.toObject(Contact.class);
                    mPending.put(contact.getCid(), contact);
                }
                mRecyclerList.clear();
                mRecyclerList = new ArrayList<>(mPending.values());
                notifyRecyclerView();
            }
        });
    }

    private void initView(View v){
        if(mPending.size()==0){
            v.findViewById(R.id.linear).setVisibility(View.VISIBLE);
        }
        mPendingRecyclerView = v.findViewById(R.id.pending_recycler_view);
        mPendingRecyclerAdapter = new ContactRecyclerAdapter(mRecyclerList,
                this, R.layout.layout_pending_list_item, getContext());
        mPendingRecyclerView.setAdapter(mPendingRecyclerAdapter);
        mPendingRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        DividerItemDecoration itemDecor = new DividerItemDecoration(mActivity, HORIZONTAL);
        mPendingRecyclerView.addItemDecoration(itemDecor);
        mPendingRecyclerAdapter.notifyDataSetChanged();
        initSearchView(v);
    }

    private void initSearchView(View v){
        SearchView searchView = v.findViewById(R.id.pending_search_view);
        ImageView icon = searchView.findViewById(R.id.search_button);
        icon.setColorFilter(Color.BLACK);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String queryString) {
                mPendingRecyclerAdapter.getFilter().filter(queryString);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String queryString) {
                mPendingRecyclerAdapter.getFilter().filter(queryString);
                return false;
            }
        });
    }

    private void initListener(){
        CollectionReference pendingCollection = mDb
                .collection(getString(R.string.collection_users))
                .document(FirebaseAuth.getInstance().getUid())
                .collection(getString(R.string.collection_pending));
        mPendingEventListener = pendingCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                Log.d(TAG, "onEvent: called.");
                if (e != null) {
                    Log.e(TAG, "onEvent: Listen failed.", e);
                    return;
                }

                if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                    Contact contact;
                    //Instead of simply using the entire query snapshot
                    //See the actual changes to query results between query snapshots (added, removed, and modified)
                    for (DocumentChange doc : queryDocumentSnapshots.getDocumentChanges()) {
                        switch (doc.getType()) {

                            case ADDED:

                                //Call the model to populate it with document
                                contact = doc.getDocument().toObject(Contact.class);
                                mPending.put(contact.getCid(), contact);
                                mRecyclerList = new ArrayList<>(mPending.values());
                                getData.updatePending(mPending);
                                notifyRecyclerView();

                                Log.d(TAG,"THIS SHOULD BE CALLED");

                                   /* //Just call this method once
                                    if (noContent.isShown()){
                                        //This will be called only if user added some new post
                                        announcementList.add(annonPost);
                                        announcementRecyclerAdapter.notifyDataSetChanged();
                                        noContent.setVisibility(View.GONE);
                                        label.setVisibility(View.VISIBLE);
                                    }*/

                                break;

                            case MODIFIED:
                                contact = doc.getDocument().toObject(Contact.class);
                                mPending.put(contact.getCid(), contact);
                                mRecyclerList = new ArrayList<>(mPending.values());
                                getData.updatePending(mPending);
                                notifyRecyclerView();
                                break;

                            case REMOVED:
                                contact = doc.getDocument().toObject(Contact.class);
                                mPending.remove(contact.getCid());
                                mRecyclerList = new ArrayList<>(mPending.values());
                                getData.updatePending(mPending);
                                notifyRecyclerView();
                        }
                    }

                }

            }
//                if (queryDocumentSnapshots != null) {
//                    for (DocumentChange doc: queryDocumentSnapshots.getDocumentChanges())
//                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
//                        Contact contact = doc.toObject(Contact.class);
//                        mPending.put(contact.getCid(), contact);
//                        mRecyclerList = new ArrayList<>(mPending.values());
//                        notifyRecyclerView();
//
//
//                    }
//                    Log.d(TAG, "onEvent: number of contacts: " + mPending.size());
//
//                }
//            }
        });


    }

    private void notifyRecyclerView(){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                mPendingRecyclerAdapter = new ContactRecyclerAdapter(mRecyclerList,
                        mPendingFragment, R.layout.layout_pending_list_item, getContext());
                mPendingRecyclerView.setAdapter(mPendingRecyclerAdapter);
                mPendingRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
                DividerItemDecoration itemDecor = new DividerItemDecoration(mActivity, HORIZONTAL);
                mPendingRecyclerView.addItemDecoration(itemDecor);
                mPendingRecyclerAdapter.notifyDataSetChanged();
            }
        });
    }

    /*
    ----------------------------- onClick ---------------------------------
    */

    @Override
    public void onContactSelected(final int position) {
        getData.initContactFragment(mRecyclerList.get(position).getCid(), null);
    }
//        android.app.AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
//        builder.setTitle("Accept Friend Request?");
//        builder.setPositiveButton("ACCEPT", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//
//                handleRequest(mRequests.get(position), true);
//            }
//        });
//        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.cancel();
//            }
//        });
//        builder.show();
//    }


    private void areYouSure(final Contact contact){
        final View dialogView = View.inflate(mActivity, R.layout.dialog_requests, null);
        final AlertDialog alertDialog = new AlertDialog.Builder(mActivity).create();
        ((TextView)dialogView.findViewById(R.id.text)).setText("ARE YOU SURE?");
        dialogView.findViewById(R.id.cancel_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
        ((Button)dialogView.findViewById(R.id.proceed_btn)).setText("DELETE REQUEST");
        dialogView.findViewById(R.id.proceed_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                remove(contact);
                alertDialog.dismiss();
            }
        });
        alertDialog.setView(dialogView);
        alertDialog.show();
    }


    @Override
    public void onAcceptSelected(int position) {
    }

    @Override
    public void onRejectSelected(int position) {
    }

    @Override
    public void onDeleteSelected(int position) {
        areYouSure(mRecyclerList.get(position));
//        PendingDialogFragment requestDialog = new PendingDialogFragment(Constants.GET_ACCEPT_REQUEST, mRecyclerList.get(position),
//                getActivity(), this);
//        requestDialog.setTargetFragment(PendingFragment.this, 1);
//        requestDialog.show(getFragmentManager(), "PendingDialogFragment");
    }

    @Override
    public void onContactLongClick(final int position) {

    }

    public void remove(final Contact contact) {
        mDb.collection(getString(R.string.collection_users)).document(contact.getCid()).collection(getString(R.string.collection_requests)).document(FirebaseAuth.getInstance().getUid()).delete();
        mDb.collection(getString(R.string.collection_users)).document(FirebaseAuth.getInstance().getUid()).collection(getString(R.string.collection_pending)).document(contact.getCid()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                mPending.remove(contact.getCid());
                mRecyclerList = new ArrayList<>(mPending.values());
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        mPendingRecyclerAdapter = new ContactRecyclerAdapter(mRecyclerList,
                                mPendingFragment, R.layout.layout_pending_list_item, getContext());
                        mPendingRecyclerView.setAdapter(mPendingRecyclerAdapter);
                        mPendingRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
                        DividerItemDecoration itemDecor = new DividerItemDecoration(mActivity, HORIZONTAL);
                        mPendingRecyclerView.addItemDecoration(itemDecor);
                        mPendingRecyclerAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }


    /*
    ----------------------------- Pending Callback ---------------------------------
    */

    public interface PendingCallback {
        HashMap<String, Contact> getPending();
        void initContactFragment(String contactID, String state);
        void updatePending(HashMap<String, Contact> pending);
    }

}
