package com.chari6268.mycontacts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class ContactsAdapter extends ArrayAdapter<ContactItem> {
    private final Context context;
    private final List<ContactItem> contacts;

    public ContactsAdapter(Context context, List<ContactItem> contacts) {
        super(context, R.layout.contact_item, contacts);
        this.context = context;
        this.contacts = contacts;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.contact_item, parent, false);

            holder = new ViewHolder();
            holder.nameTextView = convertView.findViewById(R.id.contactName);
            holder.phoneTextView = convertView.findViewById(R.id.contactPhone);
            holder.emailTextView = convertView.findViewById(R.id.contactEmail);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ContactItem contact = contacts.get(position);

        holder.nameTextView.setText(contact.getName());

        // Format phone numbers
        StringBuilder phoneStr = new StringBuilder();
        if (!contact.getPhoneNumbers().isEmpty()) {
            for (String phone : contact.getPhoneNumbers()) {
                if (phoneStr.length() > 0) phoneStr.append(", ");
                phoneStr.append(phone);
            }
            holder.phoneTextView.setText(phoneStr.toString());
            holder.phoneTextView.setVisibility(View.VISIBLE);
        } else {
            holder.phoneTextView.setVisibility(View.GONE);
        }

        // Format emails
        StringBuilder emailStr = new StringBuilder();
        if (!contact.getEmails().isEmpty()) {
            for (String email : contact.getEmails()) {
                if (emailStr.length() > 0) emailStr.append(", ");
                emailStr.append(email);
            }
            holder.emailTextView.setText(emailStr.toString());
            holder.emailTextView.setVisibility(View.VISIBLE);
        } else {
            holder.emailTextView.setVisibility(View.GONE);
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView nameTextView;
        TextView phoneTextView;
        TextView emailTextView;
    }
}
