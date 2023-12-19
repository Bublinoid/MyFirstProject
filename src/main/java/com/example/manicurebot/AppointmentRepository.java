package com.example.manicurebot;

import com.example.manicurebot.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("UPDATE Appointment a SET a.userId = ?2 WHERE a.date = ?1 AND a.time = ?3 AND a.userId IS NULL")
    @Modifying
    @Transactional
    void reserveTimeSlot(String selectedDate, String userId, String selectedTime);

    @Query("SELECT a.time FROM Appointment a WHERE a.date = ?1 AND a.userId IS NULL")
    List<String> getAvailableTimes(String selectedDate);

    @Query("SELECT a FROM Appointment a WHERE a.date = ?1 AND a.time = ?2 AND a.userId IS NULL")
    Appointment findAvailableAppointment(String selectedDate, String selectedTime);

    @Modifying
    @Transactional
    @Query("UPDATE Appointment a SET a.userId = ?3 WHERE a.date = ?1 AND a.time = ?2 AND a.userId IS NULL")
    void reserveTimeSlotByDateTimeAndId(String selectedDateString, String selectedTimeString, Long id);
    @Query("SELECT DISTINCT a.date FROM Appointment a WHERE a.userId IS NULL")
    List<LocalDate> getAvailableDates();
}
