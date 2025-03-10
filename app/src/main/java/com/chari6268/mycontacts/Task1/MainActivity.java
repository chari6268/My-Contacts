package com.chari6268.mycontacts.Task1;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.chari6268.mycontacts.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int CREATE_FILE_REQUEST_CODE = 102;
    private TextView contactsTextView;
    private ListView contactsListView;
    private List<ContactItem> contactsList = new ArrayList<>();
    CustomLoading loading;
    private String jsonDataToSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contactsTextView = findViewById(R.id.textView);
        contactsListView = findViewById(R.id.contactsListView);
        loading = new CustomLoading(this);

        Button getContactsButton = findViewById(R.id.start);
        getContactsButton.setOnClickListener(v -> {
            if (hasContactsPermission()) {
                getContactsAsJson();
            } else {
                requestContactsPermission();
            }
        });

        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            if (jsonDataToSave != null && !jsonDataToSave.isEmpty()) {
                createFile();
            } else {
                Toast.makeText(this, "No contacts data to save", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean hasContactsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestContactsPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_CONTACTS},
                PERMISSIONS_REQUEST_READ_CONTACTS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getContactsAsJson();
            } else {
                loading.dismisss();
                Toast.makeText(this, "Permission denied to read contacts", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getContactsAsJson() {
        new GetContactsTask().execute();
    }

    private class GetContactsTask extends AsyncTask<Void, Void, JSONArray> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loading.load();
            contactsList.clear();
        }

        @Override
        protected JSONArray doInBackground(Void... voids) {
            JSONArray contactsJsonArray = new JSONArray();
            ContentResolver contentResolver = getContentResolver();

            Cursor cursor = contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    null,
                    null,
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );

            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    try {
                        JSONObject contactJson = new JSONObject();

                        String contactId = cursor.getString(
                                cursor.getColumnIndex(ContactsContract.Contacts._ID));
                        String displayName = cursor.getString(
                                cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                        contactJson.put("name", displayName);

                        int hasPhone = Integer.parseInt(cursor.getString(
                                cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)));

                        // Create a ContactItem object to add to our list
                        ContactItem contactItem = new ContactItem();
                        contactItem.setName(displayName);

                        if (hasPhone > 0) {
                            Cursor phoneCursor = contentResolver.query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                    new String[]{contactId},
                                    null
                            );

                            JSONArray phoneNumbersArray = new JSONArray();
                            List<String> phoneList = new ArrayList<>();

                            if (phoneCursor != null) {
                                while (phoneCursor.moveToNext()) {
                                    String phoneNumber = phoneCursor.getString(
                                            phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                    phoneNumbersArray.put(phoneNumber);
                                    phoneList.add(phoneNumber);
                                }
                                phoneCursor.close();
                            }

                            contactJson.put("phoneNumbers", phoneNumbersArray);
                            contactItem.setPhoneNumbers(phoneList);
                        }

                        Cursor emailCursor = contentResolver.query(
                                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                                new String[]{contactId},
                                null
                        );

                        JSONArray emailArray = new JSONArray();
                        List<String> emailList = new ArrayList<>();

                        if (emailCursor != null) {
                            while (emailCursor.moveToNext()) {
                                String email = emailCursor.getString(
                                        emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
                                emailArray.put(email);
                                emailList.add(email);
                            }
                            emailCursor.close();
                        }

                        contactJson.put("emails", emailArray);
                        contactItem.setEmails(emailList);

                        // Add to our contacts list for the adapter
                        contactsList.add(contactItem);

                        // Add to JSON array for saving
                        contactsJsonArray.put(contactJson);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                cursor.close();
            }

            return contactsJsonArray;
        }

        @Override
        protected void onPostExecute(JSONArray contactsJsonArray) {
            super.onPostExecute(contactsJsonArray);
            try {
                jsonDataToSave = contactsJsonArray.toString(2);

                // Set up the ListView with our custom adapter
                ContactsAdapter adapter = new ContactsAdapter(MainActivity.this, contactsList);
                contactsListView.setAdapter(adapter);

                // Show the ListView and hide loading
                contactsListView.setVisibility(View.VISIBLE);
                contactsTextView.setVisibility(View.GONE);
                loading.dismisss();

                // Update status
                Toast.makeText(MainActivity.this,
                        "Loaded " + contactsList.size() + " contacts",
                        Toast.LENGTH_SHORT).show();

            } catch (JSONException e) {
                e.printStackTrace();
                loading.dismisss();
                contactsTextView.setVisibility(View.VISIBLE);
                contactsTextView.setText("Error creating JSON: " + e.getMessage());
            }
        }
    }

    private void createFile() {
        loading.load();
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "device_contacts.json");

        try {
            startActivityForResult(intent, CREATE_FILE_REQUEST_CODE);
        } catch (Exception e) {
            loading.dismisss();
            Toast.makeText(this, "Error creating file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            contactsTextView.setText("Error: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                try {
                    writeJsonToUri(uri, jsonDataToSave);
                    loading.dismisss();
                    Toast.makeText(this, "Contacts saved successfully", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    loading.dismisss();
                    Toast.makeText(this, "Error saving contacts: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == CREATE_FILE_REQUEST_CODE) {
            loading.dismisss();
            Toast.makeText(this, "File save cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    private void writeJsonToUri(Uri uri, String jsonString) throws IOException {
        try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            if (outputStream != null) {
                outputStream.write(jsonString.getBytes());
                outputStream.flush();
            }
        }
    }
}