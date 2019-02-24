package ndk.personal.account_ledger.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import com.kunzisoft.switchdatetime.SwitchDateTimeDialogFragment;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Stack;

import ndk.personal.account_ledger.R;
import ndk.personal.account_ledger.constants.API;
import ndk.personal.account_ledger.constants.API_Wrapper;
import ndk.personal.account_ledger.constants.Application_Specification;
import ndk.personal.account_ledger.models.Account;
import ndk.utils.Activity_Utils;
import ndk.utils.Date_Utils;
import ndk.utils.Toast_Utils;
import ndk.utils.Validation_Utils;
import ndk.utils.network_task.REST_GET_Task;
import ndk.utils.network_task.REST_Select_Task;
import ndk.utils.network_task.REST_Select_Task_Wrapper;

public class Insert_Transaction_v2 extends AppCompatActivity {

    Context application_context, activity_context = this;

    SharedPreferences settings;

    String current_tparent_account_id = "0", current_taccount_type, current_taccount_commodity_type, current_taccount_commodity_value;
    String current_fparent_account_id, current_faccount_type, current_faccount_commodity_type, current_faccount_commodity_value, current_faccount_taxable, current_faccount_place_holder;

    private Calendar calendar = Calendar.getInstance();

    private EditText edit_purpose;
    private EditText edit_amount;

    private ScrollView login_form;
    private ProgressBar login_progress;

    AutoCompleteTextView autoCompleteTextView_to, autoCompleteTextView_from;

    private Button button_date;
    private Button button_to, button_from;

    private ArrayList<Account> accounts;

    String from_selected_account_id;
    String to_selected_account_id = "0";

    Stack<Account> from_stack;
    Stack<Account> to_stack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_transaction_v2);

        from_stack = new Stack<>();
        to_stack = new Stack<>();

        application_context = getApplicationContext();

        settings = getApplicationContext().getSharedPreferences(Application_Specification.APPLICATION_NAME, Context.MODE_PRIVATE);

        login_form = findViewById(R.id.login_form);
        login_progress = findViewById(R.id.login_progress);

        Button button_submit = findViewById(R.id.button_submit);
        button_from = findViewById(R.id.button_from);
        button_to = findViewById(R.id.button_to);
        button_date = findViewById(R.id.button_date);

        edit_amount = findViewById(R.id.edit_amount);
        edit_purpose = findViewById(R.id.edit_purpose);

        autoCompleteTextView_to = findViewById(R.id.autoCompleteTextView_to);
        autoCompleteTextView_from = findViewById(R.id.autoCompleteTextView_from);

        Button button_tplus = findViewById(R.id.button_tplus);
        Button button_fplus = findViewById(R.id.button_fplus);

//        associate_button_with_time_stamp();
        Insert_Transaction_v2_Utils.associate_button_with_time_stamp(button_date, calendar);

        button_from.setText("From : " + getIntent().getStringExtra("CURRENT_ACCOUNT_FULL_NAME"));
        autoCompleteTextView_from.setText(getIntent().getStringExtra("CURRENT_ACCOUNT_FULL_NAME"), false);
        from_selected_account_id = getIntent().getStringExtra("CURRENT_ACCOUNT_ID");

        current_fparent_account_id = from_selected_account_id;
        current_faccount_type = getIntent().getStringExtra("CURRENT_ACCOUNT_TYPE");
        current_faccount_commodity_type = getIntent().getStringExtra("CURRENT_ACCOUNT_COMMODITY_TYPE");
        current_faccount_commodity_value = getIntent().getStringExtra("CURRENT_ACCOUNT_COMMODITY_VALUE");
        current_faccount_taxable = getIntent().getStringExtra("CURRENT_ACCOUNT_TAXABLE");
        current_faccount_place_holder = getIntent().getStringExtra("CURRENT_ACCOUNT_PLACE_HOLDER");

        // Initialize
        final SwitchDateTimeDialogFragment dateTimeFragment = SwitchDateTimeDialogFragment.newInstance(
                "Pick Time",
                "OK",
                "Cancel"
        );

        // Assign values
        dateTimeFragment.startAtCalendarView();
        dateTimeFragment.set24HoursMode(true);
//        dateTimeFragment.setMaximumDateTime(calendar.getTime());

        // Define new day and month format
        try {
            dateTimeFragment.setSimpleDateMonthAndDayFormat(Date_Utils.normal_stripped_date_format);
        } catch (SwitchDateTimeDialogFragment.SimpleDateMonthAndDayFormatException e) {
            Log.e(Application_Specification.APPLICATION_NAME, e.getMessage());
        }

        // Set listener
        dateTimeFragment.setOnButtonClickListener(new SwitchDateTimeDialogFragment.OnButtonClickListener() {

            @Override
            public void onPositiveButtonClick(Date date) {

                // Date is get on positive button click
                calendar.set(Calendar.YEAR, dateTimeFragment.getYear());
                calendar.set(Calendar.MONTH, dateTimeFragment.getMonth());
                calendar.set(Calendar.DAY_OF_MONTH, dateTimeFragment.getDay());
                calendar.set(Calendar.HOUR_OF_DAY, dateTimeFragment.getHourOfDay());
                calendar.set(Calendar.MINUTE, dateTimeFragment.getMinute());

//                associate_button_with_time_stamp();
                Insert_Transaction_v2_Utils.associate_button_with_time_stamp(button_date, calendar);

                Log.d(Application_Specification.APPLICATION_NAME, "Selected : " + Date_Utils.date_to_mysql_date_time_string((calendar.getTime())));

            }

            @Override
            public void onNegativeButtonClick(Date date) {
                // Date is get on negative button click
            }
        });

        button_date.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Show
                dateTimeFragment.show(getSupportFragmentManager(), "dialog_time");
            }
        });

        button_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attempt_insert_Transaction();
            }
        });

        button_to.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                initialize_to_account();

                return true;
            }
        });

        button_from.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                initialize_from_account();

                return true;
            }
        });

        bind_auto_text_view_to();

        autoCompleteTextView_to.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Log.d(Application_Specification.APPLICATION_NAME, "Item Position : " + position);
                Log.d(Application_Specification.APPLICATION_NAME, "Selected Account : " + accounts.get(position).toString());

                button_to.setText(button_to.getText().equals("To : ") ? button_to.getText() + autoCompleteTextView_to.getText().toString() : button_to.getText() + " : " + autoCompleteTextView_to.getText().toString());
                autoCompleteTextView_to.setHint(autoCompleteTextView_to.getText().toString() + " : ");

                to_stack.push(new Account(current_taccount_type, current_tparent_account_id, "", "", "", autoCompleteTextView_to.getHint().toString(), current_taccount_commodity_type, current_taccount_commodity_value, button_to.getText().toString()));

                current_tparent_account_id = accounts.get(position).getAccountId();
                current_taccount_type = accounts.get(position).getAccountType();
                current_taccount_commodity_type = accounts.get(position).getCommodityType();
                current_taccount_commodity_value = accounts.get(position).getCommodityValue();

                to_selected_account_id = current_tparent_account_id;

                bind_auto_text_view_to();
            }
        });

        autoCompleteTextView_from.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Log.d(Application_Specification.APPLICATION_NAME, "Item Position : " + position);
                Log.d(Application_Specification.APPLICATION_NAME, "Selected Account : " + accounts.get(position).toString());

                button_from.setText(button_from.getText().equals("From : ") ? button_from.getText() + autoCompleteTextView_from.getText().toString() : button_from.getText() + " : " + autoCompleteTextView_from.getText().toString());
                autoCompleteTextView_from.setHint(autoCompleteTextView_from.getText().toString() + " : ");

                from_stack.push(new Account(current_faccount_type, current_fparent_account_id, "", "", "", autoCompleteTextView_from.getHint().toString(), current_faccount_commodity_type, current_faccount_commodity_value, button_from.getText().toString()));

                current_fparent_account_id = accounts.get(position).getAccountId();
                current_faccount_type = accounts.get(position).getAccountType();
                current_faccount_commodity_type = accounts.get(position).getCommodityType();
                current_faccount_commodity_value = accounts.get(position).getCommodityValue();

                from_selected_account_id = current_fparent_account_id;

                bind_auto_text_view_from();
            }
        });

        autoCompleteTextView_to.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoCompleteTextView_to.showDropDown();
            }
        });

        autoCompleteTextView_from.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoCompleteTextView_from.showDropDown();
            }
        });

        autoCompleteTextView_to.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

//                previous_to(false);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        autoCompleteTextView_from.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

//                previous_from(false);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        autoCompleteTextView_to.setOnDismissListener(new AutoCompleteTextView.OnDismissListener() {
            @Override
            public void onDismiss() {

                if (autoCompleteTextView_to.getListSelection() != ListView.INVALID_POSITION) {
                    autoCompleteTextView_to.setText(autoCompleteTextView_to.getHint().toString().substring(0, autoCompleteTextView_to.getHint().length() - 3), false);
                    autoCompleteTextView_to.setSelection(autoCompleteTextView_to.getText().length());
                }
            }
        });

        autoCompleteTextView_from.setOnDismissListener(new AutoCompleteTextView.OnDismissListener() {
            @Override
            public void onDismiss() {

                if (autoCompleteTextView_from.getListSelection() != ListView.INVALID_POSITION) {
                    autoCompleteTextView_from.setText(autoCompleteTextView_from.getHint().toString().substring(0, autoCompleteTextView_from.getHint().length() - 3), false);
                    autoCompleteTextView_from.setSelection(autoCompleteTextView_from.getText().length());
                }
            }
        });

        button_tplus.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                if (!current_tparent_account_id.equals("0")) {

                    Activity_Utils.start_activity_with_string_extras(activity_context, Insert_Account.class, new Pair[]{new Pair<>("CURRENT_ACCOUNT_ID", current_tparent_account_id), new Pair<>("CURRENT_ACCOUNT_FULL_NAME", button_to.getText().toString().replace("To : ", "")), new Pair<>("CURRENT_ACCOUNT_TYPE", current_taccount_type), new Pair<>("CURRENT_ACCOUNT_COMMODITY_TYPE", current_taccount_commodity_type), new Pair<>("CURRENT_ACCOUNT_COMMODITY_VALUE", current_taccount_commodity_value), new Pair<>("CURRENT_ACCOUNT_TAXABLE", "F"), new Pair<>("CURRENT_ACCOUNT_PLACE_HOLDER", "F"), new Pair<>("ACTIVITY_FOR_RESULT_FLAG", String.valueOf(true))}, true, 0);
                } else {
                    Toast_Utils.longToast(getApplicationContext(), "Please Select a parent account...");
                }

                return true;
            }
        });

        button_tplus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                previous_to(true);

            }
        });

        button_fplus.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                if (!current_fparent_account_id.equals("0")) {

                    Activity_Utils.start_activity_with_string_extras(activity_context, Insert_Account.class, new Pair[]{new Pair<>("CURRENT_ACCOUNT_ID", current_fparent_account_id), new Pair<>("CURRENT_ACCOUNT_FULL_NAME", button_from.getText().toString().replace("From : ", "")), new Pair<>("CURRENT_ACCOUNT_TYPE", current_faccount_type), new Pair<>("CURRENT_ACCOUNT_COMMODITY_TYPE", current_faccount_commodity_type), new Pair<>("CURRENT_ACCOUNT_COMMODITY_VALUE", current_faccount_commodity_value), new Pair<>("CURRENT_ACCOUNT_TAXABLE", current_faccount_taxable), new Pair<>("CURRENT_ACCOUNT_PLACE_HOLDER", current_faccount_place_holder), new Pair<>("ACTIVITY_FOR_RESULT_FLAG", String.valueOf(true))}, true, 1);
                } else {
                    Toast_Utils.longToast(getApplicationContext(), "Please Select a parent account...");
                }

                return true;
            }
        });

        button_fplus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                previous_from(true);

            }
        });

        button_date.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                exchange_accounts();
                return true;
            }
        });

    }

//    private void previous_from(boolean button_action) {
//
//        if ((autoCompleteTextView_from.getText().toString().isEmpty() || button_action) && from_edit_flag) {
//
//            if (from_stack.isEmpty()) {
//
//                initialize_from_account();
//
//            } else {
//
//                Account current_from_account = from_stack.pop();
//                if (current_from_account.getName().equals(autoCompleteTextView_from.getHint().toString())) {
//
//                    if (!from_stack.isEmpty()) {
//                        current_from_account = from_stack.pop();
//                    }
//                }
//
//                current_fparent_account_id = current_from_account.getAccountId();
//
//                if (current_fparent_account_id.equals("0")) {
//
//                    initialize_from_account();
//
//                } else {
//
//                    current_faccount_type = current_from_account.getAccountType();
//                    current_faccount_commodity_type = current_from_account.getCommodityType();
//                    current_faccount_commodity_value = current_from_account.getCommodityValue();
//
//                    from_selected_account_id = current_fparent_account_id;
//
//                    button_from.setText(current_from_account.getFull_name());
//
//                    autoCompleteTextView_from.setHint(current_from_account.getName());
//
//                    autoCompleteTextView_from.setText(autoCompleteTextView_from.getHint().toString().substring(0, autoCompleteTextView_from.getHint().length() - 3), false);
//                    autoCompleteTextView_from.setSelection(autoCompleteTextView_from.getText().length());
//
//                    bind_auto_text_view_from();
//                }
//            }
//        }
//    }
//
//    private void previous_to(boolean button_action) {
//
//        if ((autoCompleteTextView_to.getText().toString().isEmpty() || button_action) && to_edit_flag) {
//
//            if (to_stack.isEmpty()) {
//                initialize_to_account();
//            } else {
//
//                Account current_to_account = to_stack.pop();
//                if (current_to_account.getName().equals(autoCompleteTextView_to.getHint().toString())) {
//                    if (!to_stack.isEmpty()) {
//                        current_to_account = to_stack.pop();
//                    }
//                }
//
//                current_tparent_account_id = current_to_account.getAccountId();
//
//                if (current_tparent_account_id.equals("0")) {
//
//                    initialize_to_account();
//
//                } else {
//
//                    current_taccount_type = current_to_account.getAccountType();
//                    current_taccount_commodity_type = current_to_account.getCommodityType();
//                    current_taccount_commodity_value = current_to_account.getCommodityValue();
//
//                    to_selected_account_id = current_tparent_account_id;
//
//                    button_to.setText(current_to_account.getFull_name());
//
//                    autoCompleteTextView_to.setHint(current_to_account.getName());
//
//                    autoCompleteTextView_to.setText(autoCompleteTextView_to.getHint().toString().substring(0, autoCompleteTextView_to.getHint().length() - 3), false);
//                    autoCompleteTextView_to.setSelection(autoCompleteTextView_to.getText().length());
//
//                    bind_auto_text_view_to();
//                }
//            }
//        }
//    }

    private void initialize_from_account() {

        current_fparent_account_id = "0";
        button_from.setText("From : ");
        autoCompleteTextView_from.setHint("");

        boolean from_edit_flag = false;
        autoCompleteTextView_from.setText("", false);
        from_edit_flag = true;

        bind_auto_text_view_from();

    }

    private void initialize_to_account() {

        current_tparent_account_id = "0";
        button_to.setText("To : ");
        autoCompleteTextView_to.setHint("");

        boolean to_edit_flag = false;
        autoCompleteTextView_to.setText("", false);
        to_edit_flag = true;

        bind_auto_text_view_to();

    }

    private void exchange_accounts() {

        String temp = current_fparent_account_id;
        current_fparent_account_id = current_tparent_account_id;
        current_tparent_account_id = temp;

        temp = current_faccount_type;
        current_faccount_type = current_taccount_type;
        current_taccount_type = temp;

        temp = current_faccount_commodity_type;
        current_faccount_commodity_type = current_taccount_commodity_type;
        current_taccount_commodity_type = temp;

        temp = current_faccount_commodity_value;
        current_faccount_commodity_value = current_taccount_commodity_value;
        current_taccount_commodity_value = temp;

        temp = from_selected_account_id;
        from_selected_account_id = to_selected_account_id;
        to_selected_account_id = temp;

        temp = button_from.getText().toString();
        button_from.setText(button_to.getText().toString().replace("To", "From"));
        button_to.setText(temp.replace("From", "To"));

        temp = autoCompleteTextView_from.getText().toString();
        autoCompleteTextView_from.setText(autoCompleteTextView_to.getText().toString(), false);
        autoCompleteTextView_to.setText(temp, false);

        Stack<Account> temp_stack = from_stack;
        from_stack = to_stack;
        to_stack = temp_stack;

    }

    private void bind_auto_text_view_to() {

        REST_Select_Task.Async_Response_JSON_array async_response_json_array = json_array -> {

            accounts = new ArrayList<>();
            ArrayList<String> account_full_names = new ArrayList<>();

            try {

                if (!json_array.getJSONObject(0).getString("status").equals("1")) {

                    for (int i = 1; i < json_array.length(); i++) {

                        accounts.add(new Account(json_array.getJSONObject(i).getString("account_type"), json_array.getJSONObject(i).getString("account_id"), json_array.getJSONObject(i).getString("notes"), json_array.getJSONObject(i).getString("parent_account_id"), json_array.getJSONObject(i).getString("owner_id"), json_array.getJSONObject(i).getString("name"), json_array.getJSONObject(i).getString("commodity_type"), json_array.getJSONObject(i).getString("commodity_value"), json_array.getJSONObject(i).getString("name")));
                        account_full_names.add(json_array.getJSONObject(i).getString("name"));
                    }
                } else {
                    edit_purpose.requestFocus();
                }

            } catch (JSONException e) {

                Toast.makeText(getApplicationContext(), "Error : " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                Log.d(Application_Specification.APPLICATION_NAME, "Error : " + e.getLocalizedMessage());
            }

            //Creating the instance of ArrayAdapter containing list of fruit names
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity_context, android.R.layout.select_dialog_item, account_full_names);

            autoCompleteTextView_to.setThreshold(1);//will start working from first character
            autoCompleteTextView_to.setAdapter(adapter);//setting the adapter data into the AutoCompleteTextView
            autoCompleteTextView_to.setTextColor(Color.RED);
            autoCompleteTextView_to.showDropDown();

        };

        REST_Select_Task_Wrapper.execute(REST_GET_Task.get_Get_URL(API_Wrapper.get_http_API(API.select_User_Accounts), new Pair[]{new Pair<>("user_id", settings.getString("user_id", "0")), new Pair<>("parent_account_id", current_tparent_account_id)}), this, Application_Specification.APPLICATION_NAME, new Pair[]{}, async_response_json_array, false, true);
    }

    private void bind_auto_text_view_from() {

        REST_Select_Task.Async_Response_JSON_array async_response_json_array = json_array -> {

            accounts = new ArrayList<>();
            ArrayList<String> account_full_names = new ArrayList<>();

            try {

                if (!json_array.getJSONObject(0).getString("status").equals("1")) {

                    for (int i = 1; i < json_array.length(); i++) {

                        accounts.add(new Account(json_array.getJSONObject(i).getString("account_type"), json_array.getJSONObject(i).getString("account_id"), json_array.getJSONObject(i).getString("notes"), json_array.getJSONObject(i).getString("parent_account_id"), json_array.getJSONObject(i).getString("owner_id"), json_array.getJSONObject(i).getString("name"), json_array.getJSONObject(i).getString("commodity_type"), json_array.getJSONObject(i).getString("commodity_value"), json_array.getJSONObject(i).getString("name")));
                        account_full_names.add(json_array.getJSONObject(i).getString("name"));
                    }
                } else {
                    autoCompleteTextView_to.requestFocus();
                }

            } catch (JSONException e) {

                Toast.makeText(getApplicationContext(), "Error : " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                Log.d(Application_Specification.APPLICATION_NAME, "Error : " + e.getLocalizedMessage());
            }

            //Creating the instance of ArrayAdapter containing list of fruit names
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity_context, android.R.layout.select_dialog_item, account_full_names);

            autoCompleteTextView_from.setThreshold(1);//will start working from first character
            autoCompleteTextView_from.setAdapter(adapter);//setting the adapter data into the AutoCompleteTextView
            autoCompleteTextView_from.setTextColor(Color.RED);
            autoCompleteTextView_from.showDropDown();

        };

        REST_Select_Task_Wrapper.execute(REST_GET_Task.get_Get_URL(API_Wrapper.get_http_API(API.select_User_Accounts), new Pair[]{new Pair<>("user_id", settings.getString("user_id", "0")), new Pair<>("parent_account_id", current_fparent_account_id)}), this, Application_Specification.APPLICATION_NAME, new Pair[]{}, async_response_json_array, false, true);
    }

//    private void select_to_account() {
//
//        Activity_Utils.start_activity_with_string_extras(this, List_Accounts.class, new Pair[]{new Pair<>("HEADER_TITLE", "NA"), new Pair<>("PARENT_ACCOUNT_ID", "0"), new Pair<>("ACTIVITY_FOR_RESULT_FLAG", String.valueOf(true)), new Pair<>("CURRENT_ACCOUNT_COMMODITY_TYPE", "CURRENCY"), new Pair<>("CURRENT_ACCOUNT_TYPE", "Assets"), new Pair<>("CURRENT_ACCOUNT_COMMODITY_VALUE", "INR"), new Pair<>("CURRENT_ACCOUNT_TAXABLE", String.valueOf(false)), new Pair<>("CURRENT_ACCOUNT_PLACE_HOLDER", String.valueOf(false))}, true, 0);
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {

            switch (requestCode) {
                case 0:
                    bind_auto_text_view_to();
                    break;
                case 1:
                    bind_auto_text_view_from();
                    break;
            }


        }
    }

//    private void associate_button_with_time_stamp() {
//        button_date.setText(Date_Utils.normal_date_time_format_words.format(calendar.getTime()));
//    }

//    private void associate_button_with_time_stamp_plus_one_minute() {
//        calendar.setTime(DateUtils.addMinutes(calendar.getTime(), 5));
//        associate_button_with_time_stamp();
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.insert_transaction, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.menu_item_view_pass_book) {

            Activity_Utils.start_activity_with_string_extras(this, Clickable_Pass_Book_Bundle.class, new Pair[]{new Pair<>("URL", REST_GET_Task.get_Get_URL(API_Wrapper.get_http_API(API.select_User_Transactions_v2), new Pair[]{new Pair<>("user_id", settings.getString("user_id", "0")), new Pair<>("account_id", getIntent().getStringExtra("CURRENT_ACCOUNT_ID"))})), new Pair<>("application_name", Application_Specification.APPLICATION_NAME), new Pair<>("V2_FLAG", getIntent().getStringExtra("CURRENT_ACCOUNT_ID"))
            }, false, 0);
        }

        return super.onOptionsItemSelected(item);
    }

    private void attempt_insert_Transaction() {

        if (to_selected_account_id.equals("0")) {
            Toast_Utils.longToast(this, "Please select To A/C...");
        } else {

            Validation_Utils.reset_errors(new EditText[]{edit_purpose, edit_amount});
            Pair<Boolean, EditText> empty_check_result = Validation_Utils.empty_check(new Pair[]{new Pair<>(edit_amount, "Please Enter Valid Amount..."), new Pair<>(edit_purpose, "Please Enter Purpose...")});

            if (empty_check_result.first) {

                // There was an error; don't attempt login and focus the first form field with an error.
                if (empty_check_result.second != null) {
                    empty_check_result.second.requestFocus();
                }

            } else {

                Pair<Boolean, EditText> zero_check_result = Validation_Utils.zero_check(new Pair[]{new Pair<>(edit_amount, "Please Enter Valid Amount...")});
                if (zero_check_result.first) {
                    if (zero_check_result.second != null) {
                        zero_check_result.second.requestFocus();
                    }
                } else {
//                    execute_insert_Transaction_Task();
                    Insert_Transaction_v2_Utils.execute_insert_Transaction_Task(login_progress, login_form, this, this, settings.getString("user_id", "0"), edit_purpose.getText().toString().trim(), Double.parseDouble(edit_amount.getText().toString().trim()), Integer.parseInt(from_selected_account_id), Integer.parseInt(to_selected_account_id), edit_purpose, edit_amount, button_date, calendar);
                }
            }
        }
    }

//    private void execute_insert_Transaction_Task() {
//
////        further_Actions further_actions = new further_Actions() {
////            @Override
////            public void onSuccess() {
////                associate_button_with_time_stamp_plus_one_minute();
////            }
////        };
////
////        REST_Insert_Task_Wrapper.execute(this, API_Wrapper.get_http_API(API.insert_Transaction_v2), this, login_progress, login_form, Application_Specification.APPLICATION_NAME, new Pair[]{new Pair<>("event_date_time", Date_Utils.date_to_mysql_date_time_string(calendar.getTime())), new Pair<>("user_id", settings.getString("user_id", "0")), new Pair<>("particulars", edit_purpose.getText().toString().trim()), new Pair<>("amount", edit_amount.getText().toString().trim()), new Pair<>("from_account_id", from_selected_account_id), new Pair<>("to_account_id", to_selected_account_id)}, edit_purpose, new EditText[]{edit_purpose, edit_amount}, further_actions);
//
//        Insert_Transaction_v2_Utils.execute_insert_Transaction_Task(login_progress, login_form, this, this, settings.getString("user_id", "0"), edit_purpose.getText().toString().trim(), Double.parseDouble(edit_amount.getText().toString().trim()), Integer.parseInt(from_selected_account_id), Integer.parseInt(to_selected_account_id), edit_purpose, edit_amount, button_date, calendar);
//    }
}
