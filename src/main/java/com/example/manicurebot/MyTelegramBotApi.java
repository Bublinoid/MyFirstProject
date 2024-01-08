package com.example.manicurebot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Component
public class MyTelegramBotApi {
    private final String botToken;
    private final String baseUrl;
    private long offset;

    private final FeedbackService feedbackService;
    private final UserService userService;
    private final UserStatusService userStatusService;
    @Autowired
    private AppointmentService appointmentService;
    @Autowired
    private AppointmentRepository appointmentRepository;
    private LocalDate selectedDate;

    private Set<Long> processedUpdates = new HashSet<>();
    private final RestTemplate restTemplate = new RestTemplate();
    private Map<Long, Boolean> awaitingFeedback = new HashMap<>();
    private Map<Long, String> feedbackTextMap = new HashMap<>();
    private Map<Long, String> feedbackChatMap = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(MyTelegramBotApi.class);
    private GoogleCloudStorageUploader storageUploader;
    private Storage storage;
    private final String bucketName = "telegram_manicure_bot";
    private final String photoUrl1 = "https://storage.googleapis.com/telegram_manicure_bot/photo_2023-11-21_19-58-02.jpg";
    private final String photoUrl2 = "https://storage.googleapis.com/telegram_manicure_bot/photo_2023-11-21_19-58-04.jpg";
    private final String photoUrl3 = "https://storage.googleapis.com/telegram_manicure_bot/photo_2023-11-21_19-58-07.jpg";
    private final String photoUrl4 = "https://storage.googleapis.com/telegram_manicure_bot/photo_2023-11-21_19-58-06.jpg";




    @Autowired
    public MyTelegramBotApi(@Value("${telegram.bot.token}") String botToken,
                            GoogleCloudStorageUploader storageUploader, FeedbackService feedbackService, UserStatusService userStatusService, AppointmentService appointmentService, UserService userService) {
        this.botToken = botToken;
        this.baseUrl = "https://api.telegram.org/bot" + botToken;
        this.feedbackService = feedbackService;
        this.userStatusService = userStatusService;
        this.appointmentService = appointmentService;
        this.userService = userService;
        this.offset = 0;

        this.storageUploader = storageUploader;
        initializeStorage();
    }

    public void downloadPhoto(String photoUrl, String fileName, long chatId) {
        try {
            URL url = new URL(photoUrl);

            try (InputStream inputStream = url.openStream()) {

                storageUploader.uploadFile(inputStream, fileName);
            }

            System.out.println("Фотография успешно загружена в Google Cloud Storage.");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Не удалось загрузить фотографию в Google Cloud Storage.");
        }
    }

    private ReplyKeyboardMarkup getMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("/start"));
        row.add(new KeyboardButton("/help"));
        row.add(new KeyboardButton("/photos"));
        row.add(new KeyboardButton("/price"));
        row.add(new KeyboardButton("/feedback"));
        row.add(new KeyboardButton("/write_feedback"));
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    @Scheduled(fixedRate = 1000)
    public void scheduledProcessUpdates() {
        processUpdates();
    }

    private void sendPhotos(long chatId) {
        try {
            List<String> photoUrls = new ArrayList<>();
            photoUrls.add(photoUrl1);
            photoUrls.add(photoUrl2);
            photoUrls.add(photoUrl3);
            photoUrls.add(photoUrl4);
            for (String photoUrl : photoUrls) {
                sendPhoto(String.valueOf(chatId), photoUrl);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(String.valueOf(chatId), "Произошла ошибка при отправке фотографий.");
        }
    }

    public void processUpdates() {
        String url = baseUrl + "/getUpdates?offset=" + offset + "&timeout=30";
        try {
            URL urlObject = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
            connection.setRequestMethod("GET");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                if (connection.getResponseCode() == 200) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode rootNode = objectMapper.readTree(response.toString());

                    if (rootNode.has("result")) {
                        for (JsonNode updateNode : rootNode.get("result")) {
                            long updateId = updateNode.get("update_id").asLong();

                            if (!processedUpdates.contains(updateId)) {
                                processedUpdates.add(updateId);
                                offset = Math.max(offset, updateId + 1);

                                JsonNode messageNode = updateNode.get("message");
                                if (messageNode != null) {
                                    JsonNode chatNode = messageNode.get("chat");
                                    if (chatNode != null) {
                                        long chatId = chatNode.get("id").asLong();
                                        String text = messageNode.get("text").asText();
                                        System.out.println("Received update: " + text);

                                        if ("/start".equals(text)) {
                                            System.out.println("Received /start command");
                                            sendStartMessage(chatId);
                                        } else if ("/help".equals(text)) {
                                            System.out.println("Received /help command");
                                            sendHelpMessage(chatId);
                                        } else if ("/photos".equals(text)) {
                                            System.out.println("Received /photos command");
                                            sendPhotos(chatId);
                                        } else if ("/price".equals(text)) {
                                            System.out.println("Received /price command");
                                            sendPriceMessage(chatId);
                                        } else if ("/feedback".equals(text)) {
                                            System.out.println("Received /feedback command");
                                            System.out.println("Received update for chatId: " + chatId);
                                            handleFeedbackCommand(String.valueOf(updateNode));
                                        } else if ("/write_feedback".equals(text)) {
                                            System.out.println("Received /write_feedback command");
                                            awaitingFeedback.put(chatId, true);
                                            sendMessage(String.valueOf(chatId), "Спасибо, что хотите оставить отзыв! Пожалуйста, напишите свой отзыв:");
                                        } else if (text.startsWith("/make_an_appointment")) {
                                            System.out.println("Received /make_an_appointment command");
                                            handleMakeAppointmentCommand(chatId);
                                        } else if (awaitingFeedback.containsKey(chatId) && awaitingFeedback.get(chatId)) {
                                            handleWriteFeedbackCommand(response.toString());
                                            awaitingFeedback.put(chatId, false);
                                        } else {
                                            System.out.println("Unknown command: " + text);
                                        }
                                    } else {
                                        System.out.println("Chat node is null");
                                    }
                                } else {
                                    System.out.println("Message node is null");
                                }
                            }
                            JsonNode callbackQueryNode = updateNode.get("callback_query");
                            if (callbackQueryNode != null) {
                                CallbackQuery callbackQuery = objectMapper.treeToValue(callbackQueryNode, CallbackQuery.class);
                                handleCallbackQuery(callbackQuery);
                            }

                        }
                    }
                } else {
                    System.out.println("Failed to retrieve updates. Response Code: " + connection.getResponseCode());
                }
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    private void sendPhoto(String chatId, String photoUrl) {
        String url = baseUrl + "/sendPhoto";
        String requestBody = "chat_id=" + chatId + "&photo=" + photoUrl;

        try {
            URL urlObject = new URL(url);

            HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                System.out.println("Response Code: " + connection.getResponseCode());
                System.out.println("Response Body: " + response.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeStorage() {
        storage = StorageOptions.getDefaultInstance().getService();
    }

    private void uploadPhotosToGCS() {
        String localPhotosDirectory = "photos";
        File[] photoFiles = new File(localPhotosDirectory).listFiles();

        if (photoFiles != null) {
            for (File photoFile : photoFiles) {
                String objectName = "photos/" + photoFile.getName();
                byte[] photoBytes = readBytesFromFile(photoFile);

                BlobId blobId = BlobId.of(bucketName, objectName);
                Blob blob = storage.create(BlobInfo.newBuilder(blobId)
                        .setContentType("image/jpeg")
                        .build(), photoBytes);

                System.out.println("Photo uploaded to GCS: " + objectName);
            }
        } else {
            System.out.println("No photos found in the local directory.");
        }
    }

    private byte[] readBytesFromFile(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            fileInputStream.read(bytes);
            return bytes;
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    private List<String> getPhotoUrlsFromGCS() {
        List<String> photoUrls = new ArrayList<>();
        photoUrls.add(photoUrl1);
        photoUrls.add(photoUrl2);
        photoUrls.add(photoUrl3);
        photoUrls.add(photoUrl4);

        for (String photoUrl : photoUrls) {
            System.out.println("Photo URL from GCS: " + photoUrl);
        }

        return photoUrls;
    }

    private void sendStartMessage(long chatId) {
        sendMessage(String.valueOf(chatId), "Добрый день! Я Ева\n" +
                "Я мастер маникюра. Сделаю Вам красивые ногти с оригинальным дизайном. " +
                "Гарантирую приятную атмосферу, стерильные инструменты, одноразовые расходники. " +
                "Жду Вас у себя в студии. \nА пока Вы можете ознакомиться с командами ниже." +
                "\nХорошего дня!" +
                "\n" +
                "\n/photos - Фотографии работ" +
                "\n/make_an_appointment - Записаться на маникюр" +
                "\n/price - Цены" +
                "\n/feedback - Отзывы" +
                "\n/write_feedback - Написать отзыв");
    }

    private void sendHelpMessage(long chatId) {
        sendMessage(String.valueOf(chatId), "Если у Вас возникли вопросы, " +
                "вы можете обратиться прямо к мастеру маникюра Еве " +
                "в телеграме, нажав по ссылке ниже. \nХорошего дня!" +
                "\nhttps://t.me/evaaasik");
    }

        private void sendMessage(String chatId, String text) {
            try {
                String url = baseUrl + "/sendMessage";
                String requestBody = "chat_id=" + chatId + "&text=" + URLEncoder.encode(text, String.valueOf(StandardCharsets.UTF_8));

                System.out.println("Request Body: " + requestBody);

                URL urlObject = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                connection.setDoOutput(true);

                try (DataOutputStream dos = new DataOutputStream(connection.getOutputStream())) {
                    byte[] input = requestBody.getBytes("utf-8");
                    dos.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                System.out.println("Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        System.out.println("Response Body: " + response);
                    }
                } else {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                        StringBuilder errorResponse = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            errorResponse.append(line);
                        }
                        System.out.println("Error Response: " + errorResponse);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    public void handleCallbackQuery(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();

        if (data.startsWith("procedure:")) {
            handleProcedureSelection(chatId, data.replace("procedure:", ""));
        } else if (data.startsWith("manicure:")) {
            handleManicureProcedureSelection(chatId, data.replace("manicure:", ""));
        } else if (data.startsWith("selectDate:")) {
            String selectedDateStr = data.replace("selectDate:", "");
            selectedDate = LocalDate.parse(selectedDateStr);
            handleSelectedDate(chatId, selectedDateStr, callbackQuery);
        } else if (data.startsWith("selectedTime:")) {
            String selectedTime = data.replace("selectedTime:", "");
            String username = callbackQuery.getMessage().getChat().getUserName();
            handleSelectedTime(chatId, selectedTime, username, selectedDate);
        }
    }


    private void sendProcedureMenu(long chatId) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton manicureButton = new InlineKeyboardButton();
        manicureButton.setText("Записаться на маникюр");
        manicureButton.setCallbackData("procedure:manicure");

        InlineKeyboardButton myAppointmentsButton = new InlineKeyboardButton();
        myAppointmentsButton.setText("Мои записи");
        myAppointmentsButton.setCallbackData("procedure:my_appointments");

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(manicureButton);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(myAppointmentsButton);

        rows.add(row1);
        rows.add(row2);

        inlineKeyboardMarkup.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите действие:");
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void sendManicureProcedureMenu(long chatId) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Здесь добавьте кнопки для каждой процедуры маникюра
        InlineKeyboardButton complexManicureButton = new InlineKeyboardButton();
        complexManicureButton.setText("Комплексный маникюр");
        complexManicureButton.setCallbackData("manicure:complex");

        InlineKeyboardButton combinedManicureButton = new InlineKeyboardButton();
        combinedManicureButton.setText("Комбинированный маникюр");
        combinedManicureButton.setCallbackData("manicure:combined");

        InlineKeyboardButton spaManicureButton = new InlineKeyboardButton();
        spaManicureButton.setText("Спа для рук");
        spaManicureButton.setCallbackData("manicure:spa");

        InlineKeyboardButton frenchManicureButton = new InlineKeyboardButton();
        frenchManicureButton.setText("Фрэнч");
        frenchManicureButton.setCallbackData("manicure:french");

        InlineKeyboardButton nailDesignButton = new InlineKeyboardButton();
        nailDesignButton.setText("Дизайн ногтя");
        nailDesignButton.setCallbackData("manicure:nail_design");

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(complexManicureButton);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(combinedManicureButton);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(spaManicureButton);

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        row4.add(frenchManicureButton);

        List<InlineKeyboardButton> row5 = new ArrayList<>();
        row5.add(nailDesignButton);

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);
        rows.add(row5);

        inlineKeyboardMarkup.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите вид маникюра:");
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void handleProcedureSelection(long chatId, String procedure) {
        if (procedure.equals("manicure")) {
            sendManicureProcedureMenu(chatId);
            userStatusService.setUserStatus(chatId, UserStatus.WAITING_FOR_PROCEDURE_SELECTION);
        } else if (procedure.equals("my_appointments")) {
            // Здесь добавьте логику для вывода списка записей пользователя
        }
    }

    private void handleManicureProcedureSelection(long chatId, String manicureType) {
        // Здесь добавьте логику для обработки выбора конкретного вида маникюра
        if (manicureType.equals("complex")) {
            // Логика для комплексного маникюра
            userStatusService.setUserStatus(chatId, UserStatus.WAITING_FOR_DATE);
            getAvailableDates(chatId);  // Можно также вызвать ваш существующий метод
        } else if (manicureType.equals("combined")) {
            // Логика для комбинированного маникюра
            userStatusService.setUserStatus(chatId, UserStatus.WAITING_FOR_DATE);
            getAvailableDates(chatId);
        } else if (manicureType.equals("spa")) {
            // Логика для спа-маникюра
            userStatusService.setUserStatus(chatId, UserStatus.WAITING_FOR_DATE);
            getAvailableDates(chatId);
        } else if (manicureType.equals("french")) {
            // Логика для фрэнча
            userStatusService.setUserStatus(chatId, UserStatus.WAITING_FOR_DATE);
            getAvailableDates(chatId);
        } else if (manicureType.equals("nail_design")) {
            // Логика для дизайна ногтя
            // Здесь можно добавить логику для выбора количества ногтей и вывода списка дат
        }
    }




    public void handleMakeAppointmentCommand(long chatId) throws TelegramApiException {
        userStatusService.setUserCommand(chatId, "/make_an_appointment");

        sendProcedureMenu(chatId);
        userStatusService.setUserStatus(chatId, UserStatus.WAITING_FOR_PROCEDURE_SELECTION);

        List<LocalDate> availableDates = appointmentService.getAvailableDates();

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (LocalDate date : availableDates) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(date.toString());
            button.setCallbackData("selectDate:" + date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            row.add(button);
            rows.add(row);
        }
        inlineKeyboardMarkup.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите дату:");
        message.setReplyMarkup(inlineKeyboardMarkup);

        execute(message);

        System.out.println("Message with inline date keyboard executed");
        System.out.println("Entering handleMakeAppointmentCommand method");

    }

        public void handleSelectedDate(long chatId, String selectedDate, CallbackQuery callbackQuery) {
            System.out.println("Selected date: " + selectedDate);
            System.out.println("Entering handleSelectedDate method");

            if (callbackQuery != null) {
                Message message = callbackQuery.getMessage();
                if (message != null) {
                    System.out.println("Message ID: " + message.getMessageId());

                    // Преобразуйте строку selectedDate в объект LocalDate
                    LocalDate selectedLocalDate = LocalDate.parse(selectedDate);

                    // Получите имя пользователя из чата
                    String username = message.getChat().getUserName();

                    // Получите объект пользователя по имени пользователя из вашего сервиса пользователя
                    User user = userService.getUserByUsername(username);

                    if (user != null) {
                        // Отправка списка доступного времени с использованием inline-клавиатуры
                        List<List<InlineKeyboardButton>> keyboardButtons = new ArrayList<>();
                        List<LocalTime> availableTimes = appointmentService.getAvailableTimesForDate(selectedLocalDate);

                        for (LocalTime time : availableTimes) {
                            InlineKeyboardButton button = new InlineKeyboardButton();
                            button.setText(time.toString());
                            button.setCallbackData("selectedTime:" + time);
                            List<InlineKeyboardButton> row = new ArrayList<>();
                            row.add(button);
                            keyboardButtons.add(row);
                        }

                        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
                        inlineKeyboardMarkup.setKeyboard(keyboardButtons);

                        SendMessage responseMessage = new SendMessage();
                        responseMessage.setChatId(String.valueOf(chatId));
                        responseMessage.setText("Выберите время:");

                        if (username != null) {
                            try {
                                responseMessage.setReplyMarkup(inlineKeyboardMarkup);
                                execute(responseMessage);
                                System.out.println("Message with inline time keyboard executed");
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                                System.out.println("Error executing message with inline time keyboard");
                            }
                        } else {
                            System.out.println("User is null");
                        }
                    } else {
                        System.out.println("User is null");
                    }
                } else {
                    System.out.println("Message is null");
                }
            } else {
                System.out.println("CallbackQuery is null");
            }
        }

    public void handleSelectedTime(long chatId, String selectedTime, String username, LocalDate selectedDate) {
        System.out.println("Entering handleSelectedTime method");
        System.out.println("Selected time: " + selectedTime);

        if (username != null) {
            User user = userService.createUserIfNotExist("FirstName", "LastName", username);
            LocalTime time = LocalTime.parse(selectedTime, DateTimeFormatter.ISO_LOCAL_TIME);
            LocalDateTime selectedDateTime = LocalDateTime.of(selectedDate, time);

            // Проверяем, записан ли пользователь на выбранное время
            Optional<Appointment> optionalAppointment = appointmentService.findAvailableAppointment(selectedDate, time);

            if (optionalAppointment.isPresent() && optionalAppointment.get().getUserId() != null && optionalAppointment.get().getUserId().equals(user.getId())) {
                // Если пользователь уже записан, выведите сообщение об этом
                String message = String.format("Вы уже записаны на маникюр! Ваша ячейка зарезервирована на: %s в %s",
                        selectedDate, selectedTime);
                sendMessage(String.valueOf(chatId), message);
            } else {
                // Пользователь не записан, продолжаем логику
                boolean success = appointmentService.makeAppointment(username, selectedDateTime, userService);

                if (success) {
                    String message = String.format("Вы успешно записаны на маникюр! Ваша ячейка зарезервирована на: %s в %s",
                            selectedDate, selectedTime);
                    sendMessage(String.valueOf(chatId), message);
                } else {
                    sendMessage(String.valueOf(chatId), "Извините, выбранное время уже занято. Пожалуйста, выберите другое время.");
                }
            }
        } else {
            sendMessage(String.valueOf(chatId), "Извините, произошла ошибка. Пожалуйста, повторите попытку.");
        }
    }




        public void sendInlineDateKeyboard(long chatId, String text, List<LocalDate> dates) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(text);

            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            for (LocalDate date : dates) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(date.toString());
                button.setCallbackData("selectDate:" + date.format(DateTimeFormatter.ISO_LOCAL_DATE));
                row.add(button);
                rows.add(row);
            }
            inlineKeyboardMarkup.setKeyboard(rows);
            message.setReplyMarkup(inlineKeyboardMarkup);

            try {
                execute(message);
                System.out.println("Message with inline keyboard executed successfully");
            } catch (TelegramApiException e) {
                e.printStackTrace();
                System.out.println("Error executing message with inline keyboard");
            }
        }




        public void getAvailableDates(long chatId) {
                userStatusService.setUserStatus(chatId, UserStatus.WAITING_FOR_PROCEDURE_SELECTION);
                List<LocalDate> availableDates = appointmentRepository.getAvailableDates();
        }




        private void execute(SendMessage message) throws TelegramApiException {
            String url = baseUrl + "/sendMessage";
            String chatId = message.getChatId().toString();
            String text = message.getText();

            try {
                URL urlObject = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("chat_id", chatId);
                requestBody.addProperty("text", text);

                if (message.getReplyMarkup() instanceof InlineKeyboardMarkup) {
                    InlineKeyboardMarkup inlineKeyboardMarkup = (InlineKeyboardMarkup) message.getReplyMarkup();

                    JsonArray keyboardArray = new JsonArray();
                    for (List<InlineKeyboardButton> row : inlineKeyboardMarkup.getKeyboard()) {
                        JsonArray rowArray = new JsonArray();
                        for (InlineKeyboardButton button : row) {
                            JsonObject buttonObject = new JsonObject();
                            buttonObject.addProperty("text", button.getText());
                            buttonObject.addProperty("callback_data", button.getCallbackData());
                            // Добавьте другие свойства кнопки по необходимости
                            rowArray.add(buttonObject);
                        }
                        keyboardArray.add(rowArray);
                    }

                    JsonObject replyMarkup = new JsonObject();
                    replyMarkup.add("inline_keyboard", keyboardArray);
                    requestBody.add("reply_markup", replyMarkup);
                }

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = requestBody.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                System.out.println("Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        System.out.println("Response Body: " + response.toString());
                    }
                } else {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                        StringBuilder errorResponse = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            errorResponse.append(line);
                        }
                        System.out.println("Error Response: " + errorResponse.toString());
                    }
                    throw new TelegramApiException("Error executing message: HTTP response code " + responseCode);
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new TelegramApiException("Error executing message", e);
            }
        }



    private void sendPriceMessage(long chatId) {
        String messageForPrice = "<b>-> Комплекс:</b> Снятие + маникюр + покрытие гель лаком + уход 1700 рублей.\n\n" +
                "-> Маникюр комбинированный <b>500</b> рублей.\n" +
                "-> Спа для рук <b>300</b> рублей.\n" +
                "-> Фрэнч <b>400</b> рублей.\n" +
                "-> Дизайн от <b>50</b> рублей за ноготь.";
        sendMessageWithHTML(String.valueOf(chatId), messageForPrice);
    }

    private void sendMessageWithHTML(String chatId, String htmlMessage) {
        try {
            String url = baseUrl + "/sendMessage";
            String escapedHtmlMessage = URLEncoder.encode(htmlMessage, String.valueOf(StandardCharsets.UTF_8));
            String requestBody = "chat_id=" + chatId + "&text=" + escapedHtmlMessage + "&parse_mode=HTML";
            System.out.println("Sending message to chatId: " + chatId);
            System.out.println("Message: " + escapedHtmlMessage);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            HttpStatus responseCode = response.getStatusCode();
            System.out.println("Response Code: " + responseCode);

            if (!responseCode.is2xxSuccessful()) {
                String errorResponse = response.getBody();
                System.out.println("Error Response: " + errorResponse);
            } else {
                String responseBody = response.getBody();
                System.out.println("Response Body: " + responseBody);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void logRequestBody(String requestBody) {
        logger.debug("Received request body: " + requestBody);
    }

    public void handleWriteFeedbackCommand(@RequestBody String requestBody) {
        logRequestBody(requestBody);

        try {
            JsonNode jsonNode = objectMapper.readTree(requestBody);

            if (jsonNode.has("result")) {
                JsonNode resultNode = jsonNode.get("result");

                if (resultNode.isArray()) {
                    for (JsonNode updateNode : resultNode) {
                        if (updateNode.has("message")) {
                            JsonNode messageNode = updateNode.get("message");

                            if (messageNode.has("chat")) {
                                JsonNode chatNode = messageNode.get("chat");

                                long chatId = chatNode.get("id").asLong();
                                String feedbackText = extractFeedbackText(updateNode);
                                System.out.println("ChatID: " + chatId);
                                System.out.println("Feedback Text: " + feedbackText);

                                if (chatId != 0) {
                                    if (awaitingFeedback.containsKey(chatId) && awaitingFeedback.get(chatId)) {
                                        awaitingFeedback.put(chatId, false);

                                        if (!feedbackText.trim().isEmpty()) {
                                            if (isCommand(feedbackText)) {
                                                sendMessage(String.valueOf(chatId), "Пожалуйста, отправьте текст как ваш отзыв.");
                                            } else {
                                                saveFeedback(chatId, feedbackText, updateNode.toString());
                                            }
                                        } else {
                                            sendMessage(String.valueOf(chatId), "Текст отзыва не может быть пустым. Пожалуйста, напишите свой отзыв.");
                                        }
                                    } else {
                                        sendMessage(String.valueOf(chatId), "Произошла ошибка. Вы не ожидаете отзыв. Пожалуйста, воспользуйтесь командой /write_feedback.");
                                    }
                                } else {
                                    System.out.println("Chat ID not found or invalid");
                                    sendMessage(String.valueOf(chatId), "Произошла ошибка при обработке вашей команды. Пожалуйста, попробуйте еще раз.");
                                }
                            } else {
                                System.out.println("Chat node not found");
                            }
                        } else {
                            System.out.println("Message node not found");
                        }
                    }
                } else {
                    System.out.println("Result node is not an array");
                }
            } else {
                System.out.println("Result node not found");
            }
        } catch (IOException e) {
            handleExecuteError(e);
        }
    }

    private boolean isCommand(String text) {
        return text.trim().startsWith("/") && !text.trim().startsWith("/write_feedback");
    }

    private void handleFeedbackCommand(@RequestBody String requestBody) {
        logRequestBody(requestBody);

        try {
            JsonNode jsonNode = objectMapper.readTree(requestBody);
            String chatIdString = String.valueOf(extractChatId(jsonNode));

            if (!chatIdString.isEmpty()) {
                long chatId = Long.parseLong(chatIdString);

                if (jsonNode.has("message")) {
                    JsonNode messageNode = jsonNode.get("message");

                    if (messageNode.has("chat")) {
                        JsonNode chatNode = messageNode.get("chat");
                        List<Feedback> feedbackList = feedbackService.getAllFeedback();

                        if (!feedbackList.isEmpty()) {
                            StringBuilder message = new StringBuilder("<b>Отзывы:</b>\n\n");
                            for (Feedback feedback : feedbackList) {
                                message.append("<strong>").append(feedback.getUsername()).append("</strong>: ")
                                        .append(feedback.getMessage()).append("\n");
                            }
                            sendMessageWithHTML(String.valueOf(chatId), message.toString());
                        } else {
                            sendMessage(String.valueOf(chatId), "Пока нет отзывов.");
                        }
                    } else {
                        System.out.println("Chat node not found in the request");
                    }
                } else {
                    System.out.println("Message node not found in the request");
                }
            } else {
                System.out.println("Chat ID not found or invalid");
            }
        } catch (IOException | NumberFormatException e) {
            handleExecuteError(e);
        }
    }

    private void saveFeedback(long chatId, String feedbackText, String requestBody) {
        try {
            String username = extractUsername(requestBody);
            if (chatId != 0 && username != null && !username.isEmpty() && feedbackText != null) {
                Feedback feedback = new Feedback();
                feedback.setChatId(chatId);
                feedback.setUsername(username);
                feedback.setMessage(feedbackText);

                feedbackService.saveFeedback(feedback);
                System.out.println("Отзыв успешно сохранен: " + feedback);

                awaitingFeedback.put(chatId, false);
                sendMessageWithHTML(String.valueOf(chatId), "Спасибо за ваш отзыв!");
            } else {
                System.out.println("Некорректные данные для сохранения отзыва");
                sendMessageWithHTML(String.valueOf(chatId), "Произошла ошибка при сохранении отзыва. Пожалуйста, попробуйте еще раз.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Произошла ошибка при сохранении отзыва");
            sendMessageWithHTML(String.valueOf(chatId), "Произошла ошибка при сохранении отзыва. Пожалуйста, попробуйте еще раз.");
        }
    }

    private long extractChatId(String requestBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(requestBody);
            JsonNode messageNode = jsonNode.get("message");

            if (messageNode != null) {
                JsonNode chatNode = messageNode.get("chat");
                if (chatNode != null) {
                    long chatId = extractChatIdFromNode(chatNode);
                    if (chatId != 0) {
                        return chatId;
                    }
                } else {
                    System.out.println("Chat node not found in the request");
                }
            } else {
                System.out.println("Message node not found in the request");
            }

            JsonNode chatIdNode = jsonNode.path("chat_id");
            if (!chatIdNode.isMissingNode()) {
                return chatIdNode.asLong();
            }

            MultiValueMap<String, String> params = UriComponentsBuilder.fromUriString(requestBody).build().getQueryParams();
            if (params.containsKey("chat_id")) {
                return Long.parseLong(params.getFirst("chat_id"));
            }

            return extractChatIdFromNode(jsonNode);
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }

        return 0;
    }

    private long extractChatId(JsonNode jsonNode) {
        JsonNode messageNode = jsonNode.get("message");

        if (messageNode != null) {
            JsonNode chatNode = messageNode.get("chat");
            if (chatNode != null) {
                return extractChatIdFromNode(chatNode);
            } else {
                System.out.println("Chat node not found in the request");
            }
        } else {
            System.out.println("Message node not found in the request");
        }

        return 0;
    }

    private long extractChatIdFromNode(JsonNode node) {
        if (node != null && node.has("id") && node.get("id").isNumber()) {
            long chatId = node.get("id").asLong();
            System.out.println("Chat ID found: " + chatId);
            return chatId;
        } else {
            System.out.println("Chat ID not found or invalid");
            return 0;
        }
    }

    private String extractFeedbackText(JsonNode jsonNode) {
        JsonNode messageNode = jsonNode.path("message");
        if (!messageNode.isMissingNode()) {
            JsonNode textNode = messageNode.path("text");
            return textNode.asText();
        }
        return "";
    }

    private String extractUsername(String requestBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(requestBody);
            JsonNode messageNode = jsonNode.get("message");

            if (messageNode != null) {
                JsonNode fromNode = messageNode.get("from");

                if (fromNode != null && fromNode.has("username")) {
                    return fromNode.get("username").asText();
                }
            }
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String extractMessage(String requestBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(requestBody);
            JsonNode messageNode = jsonNode.get("message");

            if (messageNode != null && messageNode.has("text")) {
                return messageNode.get("text").asText();
            }
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void handleExecuteError(IOException e) {
        e.printStackTrace();
    }

    private void handleExecuteError(Exception e) {
        e.printStackTrace();
    }

}



