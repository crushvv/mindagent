package com.mindagent.chat.core;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class MockAgentToolService implements AgentToolService {

    @Value("${mindagent.tools.email-enabled:true}")
    private boolean emailEnabled;

    @Value("${mindagent.tools.excel-enabled:true}")
    private boolean excelEnabled;

    @Value("${mindagent.tools.risk-email-to:}")
    private String riskEmailTo;

    @Value("${mindagent.tools.excel-file-path:./data/consultation_records.xlsx}")
    private String excelFilePath;

    @Value("${spring.mail.username:mindagent@local}")
    private String mailFrom;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Override
    public List<ToolExecutionResult> executeByEmotion(
            String sessionId,
            String userId,
            String emotionLabel,
            String routePolicy,
            String reply
    ) {
        List<ToolExecutionResult> results = new ArrayList<>();

        if ("risk".equals(emotionLabel) && emailEnabled) {
            results.add(sendRiskEmail(sessionId, userId, reply));
        }

        if (!"chat".equals(emotionLabel) && excelEnabled) {
            results.add(appendExcelRecord(sessionId, userId, emotionLabel, routePolicy, reply));
        }

        return results;
    }

    private ToolExecutionResult sendRiskEmail(String sessionId, String userId, String reply) {
        if (mailSender == null) {
            return new ToolExecutionResult("sendRiskEmailTool", false, "MAIL_SENDER_NOT_CONFIGURED");
        }
        if (riskEmailTo == null || riskEmailTo.isBlank()) {
            return new ToolExecutionResult("sendRiskEmailTool", false, "RISK_EMAIL_TO_NOT_CONFIGURED");
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(mailFrom);
            mail.setTo(riskEmailTo);
            mail.setSubject("[MindAgent][Risk Alert] session=" + sessionId);
            mail.setText("""
                    A risk-level conversation was detected.
                    sessionId: %s
                    userId: %s
                    time: %s
                    assistantReplySummary: %s
                    """.formatted(sessionId, userId, LocalDateTime.now(), truncate(reply)));
            mailSender.send(mail);
            return new ToolExecutionResult("sendRiskEmailTool", true, "EMAIL_SENT");
        } catch (Exception ex) {
            return new ToolExecutionResult("sendRiskEmailTool", false, "EMAIL_SEND_FAILED");
        }
    }

    private ToolExecutionResult appendExcelRecord(
            String sessionId,
            String userId,
            String emotionLabel,
            String routePolicy,
            String reply
    ) {
        synchronized (this) {
            Path path = Paths.get(excelFilePath);
            try {
                Path parent = path.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }

                Workbook workbook;
                Sheet sheet;

                if (Files.exists(path)) {
                    try (InputStream in = Files.newInputStream(path)) {
                        workbook = new XSSFWorkbook(in);
                    }
                    sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : workbook.createSheet("records");
                } else {
                    workbook = new XSSFWorkbook();
                    sheet = workbook.createSheet("records");
                    Row header = sheet.createRow(0);
                    header.createCell(0).setCellValue("timestamp");
                    header.createCell(1).setCellValue("sessionId");
                    header.createCell(2).setCellValue("userId");
                    header.createCell(3).setCellValue("emotionLabel");
                    header.createCell(4).setCellValue("routePolicy");
                    header.createCell(5).setCellValue("replySummary");
                }

                int nextRow = sheet.getLastRowNum() + 1;
                Row row = sheet.createRow(nextRow);
                row.createCell(0).setCellValue(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                row.createCell(1).setCellValue(sessionId);
                row.createCell(2).setCellValue(userId);
                row.createCell(3).setCellValue(emotionLabel);
                row.createCell(4).setCellValue(routePolicy);
                row.createCell(5).setCellValue(truncate(reply));

                try (OutputStream out = Files.newOutputStream(path)) {
                    workbook.write(out);
                }
                workbook.close();
                return new ToolExecutionResult("appendConsultationExcelTool", true, "EXCEL_APPENDED");
            } catch (IOException ex) {
                return new ToolExecutionResult("appendConsultationExcelTool", false, "EXCEL_APPEND_FAILED");
            }
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 40 ? value : value.substring(0, 40) + "...";
    }
}
