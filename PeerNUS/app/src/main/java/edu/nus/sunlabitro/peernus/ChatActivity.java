package edu.nus.sunlabitro.peernus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ChatActivity extends AppCompatActivity {

    private FirebaseDatabase database;
    private FirebaseStorage storage;
    private DatabaseReference mFirebaseDatabaseReference;
    private StorageReference mFirebaseStorageReference;
    private LinearLayoutManager mLinearLayoutManager;
    private FirebaseRecyclerAdapter<Message, MessagesViewHolder> mFirebaseAdapter;

    private String USER_PREF;
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";

    private RecyclerView mChatList;
    private ImageView mUploadImageView;
    private EditText mMessageEditText;
    private Button mSendMsgBtn;

    private String chatroomId;
    private String username;
    private String receiverName;

    private ArrayList<Message> messages;

    private final String TAG = "ChatActivity";
    private final int PICK_IMAGE = 101;

    public static class MessagesViewHolder extends RecyclerView.ViewHolder {
        public TextView mMessageTextView;
        public TextView mTimestampTextView;
        public ImageView mImageMessageView;
        public LinearLayout mLinearLayout;

        public MessagesViewHolder (View v) {
            super(v);
            mLinearLayout = (LinearLayout) itemView.findViewById(R.id.messageRow);
            mMessageTextView = (TextView) itemView.findViewById(R.id.message);
            mTimestampTextView = (TextView) itemView.findViewById(R.id.timestamp);
            mImageMessageView = (ImageView) itemView.findViewById(R.id.imageMessage);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        USER_PREF = getString(R.string.USER_PREF);

        username = getSharedPreferences(USER_PREF, MODE_PRIVATE)
                .getString("name", "");
        receiverName = getIntent().getExtras().getString("receiverName");

        if (username.compareTo(receiverName) < 0) {
            chatroomId = username + "_" + receiverName;
        } else {
            chatroomId = receiverName + "_" + username;
        }

        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        mFirebaseDatabaseReference = database.getReference("messages");
        mFirebaseStorageReference = storage.getReference("messages");

        messages = new ArrayList<>();

        mMessageEditText = findViewById(R.id.messageEditText);

        mSendMsgBtn = findViewById(R.id.sendButton);
        mSendMsgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().equals("") || s == null) {
                    mSendMsgBtn.setEnabled(false);
                } else {
                    mSendMsgBtn.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mUploadImageView = (ImageView) findViewById(R.id.addMessageImageView);
        mUploadImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendImage();
            }
        });

        mChatList = (RecyclerView) findViewById(R.id.chatList);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);

        SnapshotParser<Message> parser = new SnapshotParser<Message>() {
            @Override
            public Message parseSnapshot(DataSnapshot dataSnapshot) {
                Message message = dataSnapshot.getValue(Message.class);
                if (message != null) {
                    message.setId(dataSnapshot.getKey());
                }
                return message;
            }
        };

        DatabaseReference messagesRef = mFirebaseDatabaseReference.child(chatroomId);

        Log.d(TAG, messagesRef.toString());

        FirebaseRecyclerOptions<Message> options =
                new FirebaseRecyclerOptions.Builder<Message>()
                        .setQuery(messagesRef, parser)
                        .build();

        mFirebaseAdapter = new FirebaseRecyclerAdapter<Message, MessagesViewHolder>(options) {

            @NonNull
            @Override
            public MessagesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

                return new MessagesViewHolder(layoutInflater
                        .inflate(R.layout.messages_list, parent, false));
            }

            @Override
            protected void onBindViewHolder(@NonNull final MessagesViewHolder holder,
                    int position, @NonNull Message message) {
                LinearLayout.LayoutParams params = new LinearLayout
                        .LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                if (message.getName().equals(username)) {
                    params.gravity = Gravity.END;
                } else {
                    params.gravity = Gravity.START;
                }

                holder.mTimestampTextView.setLayoutParams(params);

                if (message.getText() != null) {
                    holder.mMessageTextView.setText(message.getText());
                    holder.mMessageTextView.setVisibility(View.VISIBLE);
                    holder.mImageMessageView.setVisibility(View.GONE);
                    holder.mMessageTextView.setLayoutParams(params);
                } else if (message.getImageUrl() != null) {
                    String imageUrl = message.getImageUrl();
                    if (imageUrl.startsWith("gs://")) {
                        StorageReference storageReference = FirebaseStorage.getInstance()
                                .getReferenceFromUrl(imageUrl);
                        storageReference.getDownloadUrl().addOnCompleteListener(
                                new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                        if (task.isSuccessful()) {
                                            String downloadUrl = task.getResult().toString();
                                            Glide.with(holder.mImageMessageView.getContext())
                                                    .load(downloadUrl)
                                                    .into(holder.mImageMessageView);
                                        } else {
                                            Log.w(TAG, "Getting download url was not successful.",
                                                    task.getException());
                                        }
                                    }
                                });
                    } else {
                        Glide.with(holder.mImageMessageView.getContext())
                                .load(message.getImageUrl())
                                .into(holder.mImageMessageView);
                    }

                    holder.mMessageTextView.setVisibility(View.GONE);
                    holder.mImageMessageView.setVisibility(View.VISIBLE);
                    holder.mImageMessageView.setLayoutParams(params);

                }

                String timestamp = convertTimestampToDateTime(message.getTimestamp());
                holder.mTimestampTextView.setText(String.valueOf(timestamp));
            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the user is at the bottom of the list, scroll
                // to the bottom of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) && lastVisiblePosition == (positionStart - 1))) {
                    mChatList.scrollToPosition(positionStart);
                }
            }
        });

        mChatList.setLayoutManager(mLinearLayoutManager);
        mChatList.setAdapter(mFirebaseAdapter);

    }

    private void sendMessage() {
        String messageText = mMessageEditText.getText().toString();
        long timestamp = System.currentTimeMillis();
        Message message = new Message(messageText, username, timestamp,null);
        mFirebaseDatabaseReference.child(chatroomId).push().setValue(message);
        mMessageEditText.setText("");
    }

    private void sendImage() {
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");

        Intent pickIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

        startActivityForResult(chooserIntent, PICK_IMAGE);
    }

    private void uploadImage(StorageReference storageReference, Uri uri,
                             final String key, final long timestamp) {
        storageReference
                .putFile(uri)
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        System.out.println("Upload is " + progress + "% done");
                    }
                }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                        System.out.println("Upload is paused");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Handle successful uploads on complete
                        // ...
                        Message message = new Message(null, username, timestamp,
                                taskSnapshot.getDownloadUrl().toString());
                        mFirebaseDatabaseReference.child(chatroomId).child(key)
                            .setValue(message);
                    }
                });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE) {
            if (data != null) {
                final Uri uri = data.getData();
                final long timestamp = System.currentTimeMillis();
                Message tmpMessage = new Message(null, username, timestamp, LOADING_IMAGE_URL);
                mFirebaseDatabaseReference.child(chatroomId).push().setValue(
                        tmpMessage, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError,
                                    DatabaseReference databaseReference) {
                                if (databaseError == null) {
                                    String key = mFirebaseDatabaseReference.getKey();
                                    StorageReference storageReference =
                                            FirebaseStorage.getInstance()
                                                    .getReference("chatImages/" + chatroomId)
                                                    .child(key)
                                                    .child(uri.getLastPathSegment());

                                    uploadImage(storageReference, uri, key, timestamp);
                                }
                            }
                        });
            }
        }
    }

    private String convertTimestampToDateTime(long inputTimestamp) {
        Timestamp timestamp = new Timestamp(inputTimestamp);
        Date date = new Date(timestamp.getTime());

        // S is the millisecond
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy' 'HH:mm");

        return simpleDateFormat.format(date);
    }

    @Override
    protected void onPause() {
        mFirebaseAdapter.stopListening();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mFirebaseAdapter.startListening();
        super.onResume();
    }
}
