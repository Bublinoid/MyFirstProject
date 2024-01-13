package com.example.manicurebot.Appointment;

import com.example.manicurebot.User.User;
import com.example.manicurebot.User.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;

    }

    public List<LocalDate> getAvailableDates() {
        logger.info("Вызван метод getAvailableDates()");

        LocalDate currentDate = LocalDate.now();
        LocalDate endDate = currentDate.plusMonths(1);

        List<LocalDate> availableDates = new ArrayList<>();

        while (currentDate.isBefore(endDate)) {
            if (currentDate.getDayOfWeek() == DayOfWeek.SATURDAY || currentDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                availableDates.add(currentDate);
            }
            currentDate = currentDate.plusDays(1);
        }

        logger.info("Доступные даты: {}", availableDates);
        return availableDates;
    }


    public List<LocalTime> getAvailableTimesForDate(LocalDate selectedDate) {

        List<LocalTime> availableTimes = new ArrayList<>();
        LocalTime startTime = LocalTime.of(10, 0);
        LocalTime endTime = LocalTime.of(18, 0);

        while (startTime.isBefore(endTime)) {
            availableTimes.add(startTime);
            startTime = startTime.plusHours(1);
        }
        logger.info("Доступные времена для даты {}: {}", selectedDate, availableTimes);
        return availableTimes;
    }

    public List<LocalTime> getOccupiedTimesForDate(LocalDate selectedDate) {
        List<Appointment> appointments = appointmentRepository.getAppointmentsByDate(selectedDate);

        List<LocalTime> occupiedTimes = appointments.stream()
                .map(Appointment::getTime)
                .collect(Collectors.toList());

        logger.info("Занятые времена для даты {}: {}", selectedDate, occupiedTimes);
        return occupiedTimes;
    }

    private boolean isTimeSlotAvailable(String selectedDateString, String selectedTimeString) {
        LocalDate selectedDate = LocalDate.parse(selectedDateString);
        LocalTime selectedTime = LocalTime.parse(selectedTimeString);

        // Получаем список доступных времен для выбранной даты
        List<LocalTime> availableTimes = getAvailableTimesForDate(selectedDate);

        // Получаем список занятых времен для выбранной даты
        List<LocalTime> occupiedTimes = getOccupiedTimesForDate(selectedDate);

        // Проверяем, что выбранное время входит в список доступных и не входит в список занятых
        if (availableTimes.contains(selectedTime) && !occupiedTimes.contains(selectedTime)) {
            return true;
        } else {
            // Выводим сообщение, если выбранное время занято
            logger.info("Время {} на дату {} уже занято или недоступно.", selectedTimeString, selectedDateString);
            return false;
        }
    }


    @Transactional
    public boolean makeAppointment(long chatId, LocalDateTime selectedDateTime, UserService userService, String procedureType, Integer nailCount) {
        User user = userService.createUserIfNotExist("FirstName", "LastName", String.valueOf(chatId));
        LocalDate selectedDate = selectedDateTime.toLocalDate();
        LocalTime selectedTime = selectedDateTime.toLocalTime();

        // Логирование для проверки значений chatId и других параметров
        logger.info("Trying to make an appointment for user {} on date {} at time {}", chatId, selectedDate, selectedTime);

        // Проверяем, свободно ли выбранное время
        if (isTimeSlotAvailable(selectedDate.toString(), selectedTime.toString())) {
            // Получаем список занятых времен для выбранной даты
            List<LocalTime> occupiedTimes = getOccupiedTimesForDate(selectedDate);

            // Проверяем, что выбранное время не входит в список занятых
            if (!occupiedTimes.contains(selectedTime)) {
                // Если время свободно, резервируем его
                appointmentRepository.reserveTimeSlot(selectedDate, chatId, selectedTime, procedureType, nailCount);

                // Теперь создадим новую запись в базе данных
                Appointment newAppointment = new Appointment();
                newAppointment.setDate(selectedDate);
                newAppointment.setTime(selectedTime);
                newAppointment.setChatId(chatId);
                newAppointment.setProcedureType(procedureType);
                newAppointment.setNailCount(nailCount);

                // Логирование для проверки значений chatId и других параметров перед сохранением
                logger.info("Saving appointment with user {} on date {} at time {}", chatId, selectedDate, selectedTime);

                appointmentRepository.save(newAppointment);

                // Логирование для проверки успешного сохранения
                logger.info("Appointment saved successfully");

                return true;
            } else {
                // Выводим сообщение, если выбранное время занято
                logger.info("Time {} on date {} is already occupied or unavailable", selectedTime, selectedDate);
                return false;
            }
        } else {
            return false;
        }
    }


    public Optional<Appointment> findAvailableAppointment(LocalDate selectedDate, LocalTime selectedTime) {
        Appointment appointment = appointmentRepository.findAvailableAppointment(selectedDate, selectedTime);
        return Optional.ofNullable(appointment);
    }

    public Optional<Appointment> getAppointmentById(Long appointmentId) {
        return appointmentRepository.findById(appointmentId);
    }

    public List<Appointment> getAppointmentsByUserId(Long userId) {
        return appointmentRepository.findByUserId(userId);
    }

    public void deleteAppointment(Long appointmentId) {
        appointmentRepository.deleteById(appointmentId);
    }



}