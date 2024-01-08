package com.example.manicurebot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("UPDATE Appointment a SET a.chatId = :chatId, a.procedureType = :procedureType, a.nailCount = :nailCount WHERE a.date = :selectedDate AND a.time = :selectedTime AND a.chatId IS NULL")
    @Modifying
    @Transactional
    void reserveTimeSlot(
            @Param("selectedDate") LocalDate selectedDate,
            @Param("chatId") Long chatId,
            @Param("selectedTime") LocalTime selectedTime,
            @Param("procedureType") String procedureType,
            @Param("nailCount") Integer nailCount
    );


    @Query("SELECT a.time FROM Appointment a WHERE a.date = ?1 AND a.chatId IS NULL")
    List<String> getAvailableTimes(@Param("selectedDate") LocalDate selectedDate);

    @Query("SELECT a FROM Appointment a WHERE a.date = :selectedDate AND a.time = :selectedTime AND a.chatId IS NULL")
    Appointment findAvailableAppointment(@Param("selectedDate") LocalDate selectedDate,
                                         @Param("selectedTime") LocalTime selectedTime);

    @Query("SELECT a FROM Appointment a WHERE a.chatId = :userId")
    List<Appointment> findByUserId(@Param("userId") Long userId);


//    @Modifying
//    @Transactional
//    @Query("UPDATE Appointment a SET a.userId = ?3 WHERE a.date = ?1 AND a.time = ?2 AND a.userId IS NULL")
//    void reserveTimeSlotByDateTimeAndId(String selectedDateString, String selectedTimeString, String userId);

    @Query("SELECT DISTINCT a.date FROM Appointment a WHERE a.chatId IS NULL")
    List<LocalDate> getAvailableDates();
    @Query("SELECT a.time FROM Appointment a WHERE a.date = ?1 AND a.chatId IS NOT NULL")
    List<LocalTime> getOccupiedTimesForDate(@Param("selectedDate") LocalDate selectedDate);
    @Query("SELECT a FROM Appointment a WHERE a.date = ?1")
    List<Appointment> getAppointmentsByDate(@Param("selectedDate") LocalDate selectedDate);

}
