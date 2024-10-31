package com.echipa1.mypetapp;


import android.content.Intent;
import android.graphics.Bitmap;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.List;

import classes.Appointment;

public class AddAppointmentActivity extends AppCompatActivity {
    private Spinner spnPet;
    private Spinner spnDoctor;
    private TextInputEditText tietReason;
    private CalendarView cvDate;
    private TimePicker tpHour;
    private MaterialButton btnCamera;
    private Button btnSave;
    private ImageView imageView;
    private Bitmap photoBitmap;
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {

                    Bundle extras = result.getData().getExtras();
                    photoBitmap = (Bitmap) extras.get("data");

                    if (imageView != null && photoBitmap != null) {
                        imageView.setImageBitmap(photoBitmap);
                    }

                    Toast.makeText(this, "Photo captured successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show();
                }
            });
    private Calendar selectedDateTime;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_appointment);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cl_add_consultation), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initComp();

        btnCamera.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraLauncher.launch(intent);
            } catch (Exception e) {
                Log.println(Log.ERROR, "Camera", e.getMessage());
                Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
            }

        });


        btnSave.setOnClickListener(v -> {
            if (validateInputs()) {
                saveAppointment();
            }
        });
    }

    private void initComp() {
        intent = getIntent();
        spnPet = findViewById(R.id.surugiu_george_alexandru_sp_animal);
        spnDoctor = findViewById(R.id.surugiu_george_alexandru_sp_doctor);
        tietReason = findViewById(R.id.surugiu_george_alexandru_tiet_consultationReason);
        cvDate = findViewById(R.id.surugiu_george_alexandru_calendarView);
        tpHour = findViewById(R.id.surugiu_george_alexandru_tp_appointmentHour);
        btnCamera = findViewById(R.id.surugiu_george_alexandru_btn_camera);
        btnSave = findViewById(R.id.surugiu_george_alexandru_bttnSave);
        imageView = findViewById(R.id.serban_andrei_appointment_image);
        selectedDateTime = Calendar.getInstance();
        List<String> pets = intent.getStringArrayListExtra("pets");
        var petAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, pets);
        petAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnPet.setAdapter(petAdapter);

        Calendar currentDate = Calendar.getInstance();
        cvDate.setMinDate(currentDate.getTimeInMillis());

        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.MONTH, 3);
        cvDate.setMaxDate(maxDate.getTimeInMillis());

        tpHour.setIs24HourView(true);
        tpHour.setHour(9);
        tpHour.setMinute(0);

        cvDate.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDateTime = Calendar.getInstance();
            selectedDateTime.set(Calendar.YEAR, year);
            selectedDateTime.set(Calendar.MONTH, month);
            selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            selectedDateTime.set(Calendar.HOUR_OF_DAY, tpHour.getHour());
            selectedDateTime.set(Calendar.MINUTE, tpHour.getMinute());
            selectedDateTime.set(Calendar.SECOND, 0);
            selectedDateTime.set(Calendar.MILLISECOND, 0);

            Log.i("Calendar", "Selected date: " + selectedDateTime.getTime().toString());
        });
    }

    private boolean validateInputs() {
        if (spnPet.getSelectedItem() == null) {
            showError("Please select a pet");
            return false;
        }

        if (spnDoctor.getSelectedItem() == null) {
            showError("Please select a doctor");
            return false;
        }

        String reason = tietReason.getText() != null ? tietReason.getText().toString().trim() : "";
        if (reason.isEmpty()) {
            showError("Please enter a reason for the appointment");
            return false;
        }

        if (reason.length() < 5) {
            showError("Please enter a more detailed reason for the appointment");
            return false;
        }

        // Update selected time
        selectedDateTime.set(Calendar.HOUR_OF_DAY, tpHour.getHour());
        selectedDateTime.set(Calendar.MINUTE, tpHour.getMinute());
        selectedDateTime.set(Calendar.SECOND, 0);
        selectedDateTime.set(Calendar.MILLISECOND, 0);

        // Get current date/time
        Calendar currentDate = Calendar.getInstance();
        currentDate.set(Calendar.SECOND, 0);
        currentDate.set(Calendar.MILLISECOND, 0);

        // Check if date is in the past
        if (selectedDateTime.before(currentDate)) {
            showError("Please select a future date and time");
            return false;
        }

        // Check if it's a weekend
        int dayOfWeek = selectedDateTime.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            showError("Appointments are only available on weekdays");
            return false;
        }

        // Check working hours (9 AM - 5 PM)
        int hour = tpHour.getHour();
        int minute = tpHour.getMinute();

        if (hour < 9) {
            showError("Appointments are only available from 9 AM");
            return false;
        }

        if (hour >= 17) {
            showError("Appointments are only available until 5 PM");
            return false;
        }

        if (hour == 16 && minute > 0) {
            showError("Last appointment must start at 4:00 PM");
            return false;
        }

        // For same-day appointments, ensure at least 1 hour in advance
        if (isSameDay(selectedDateTime, currentDate)) {
            Calendar oneHourFromNow = Calendar.getInstance();
            oneHourFromNow.add(Calendar.HOUR_OF_DAY, 1);

            if (selectedDateTime.before(oneHourFromNow)) {
                showError("Appointments must be scheduled at least 1 hour in advance");
                return false;
            }
        }

        return true;
    }

    private boolean isSameDay(Calendar date1, Calendar date2) {
        return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
                date1.get(Calendar.MONTH) == date2.get(Calendar.MONTH) &&
                date1.get(Calendar.DAY_OF_MONTH) == date2.get(Calendar.DAY_OF_MONTH);
    }


    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void saveAppointment() {
        String selectedPet = spnPet.getSelectedItem().toString();
        String selectedDoctor = spnDoctor.getSelectedItem().toString();
        String reason = tietReason.getText().toString();

        selectedDateTime.set(Calendar.HOUR_OF_DAY, tpHour.getHour());
        selectedDateTime.set(Calendar.MINUTE, tpHour.getMinute());
        Date appointmentDate = selectedDateTime.getTime();

        Appointment appointment = new Appointment(
                selectedPet,
                appointmentDate,
                selectedDoctor,
                reason
        );

        if (photoBitmap != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            photoBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String photoBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);
            appointment.setPhotoBase64(photoBase64);
        }

        intent.putExtra("doctor", selectedDoctor);
        intent.putExtra("appointment", appointment);

        setResult(RESULT_OK, intent);
        finish();
    }


}