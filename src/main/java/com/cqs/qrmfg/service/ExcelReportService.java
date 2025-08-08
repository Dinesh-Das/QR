package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.QuestionTemplate;
import com.cqs.qrmfg.model.PlantSpecificData;
import com.cqs.qrmfg.model.Workflow;
import com.cqs.qrmfg.repository.QuestionTemplateRepository;
import com.cqs.qrmfg.repository.PlantSpecificDataRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelReportService {

    @Autowired
    private QuestionTemplateRepository questionTemplateRepository;

    @Autowired
    private PlantSpecificDataRepository plantSpecificDataRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public byte[] generateQuestionsAndAnswersReport(Workflow workflow) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Get all question templates
            List<QuestionTemplate> questionTemplates = questionTemplateRepository.findByIsActiveTrueOrderByStepNumberAscOrderIndexAsc();
            
            // Get plant specific data for this workflow
            List<PlantSpecificData> plantDataList = plantSpecificDataRepository.findByWorkflowId(workflow.getId());
            
            // If no workflow-specific data, try to find by material and plant code
            if (plantDataList.isEmpty()) {
                plantDataList = plantSpecificDataRepository.findByPlantCodeAndMaterialCode(
                    workflow.getPlantCode(), workflow.getMaterialCode());
            }

            // Create styles
            Map<String, CellStyle> styles = createAllStyles(workbook);

            // Create main sheet
            Sheet mainSheet = workbook.createSheet("Questions & Answers");
            createMainSheet(mainSheet, workflow, questionTemplates, plantDataList, styles);

            // Create summary sheet
            Sheet summarySheet = workbook.createSheet("Summary");
            createSummarySheet(summarySheet, workflow, questionTemplates, plantDataList, styles);

            // Create team breakdown sheet
            Sheet teamSheet = workbook.createSheet("Team Breakdown");
            createTeamBreakdownSheet(teamSheet, workflow, questionTemplates, plantDataList, styles);

            // Convert to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private Map<String, CellStyle> createAllStyles(Workbook workbook) {
        Map<String, CellStyle> styles = new HashMap<>();
        styles.put("title", createTitleStyle(workbook));
        styles.put("header", createHeaderStyle(workbook));
        styles.put("subHeader", createSubHeaderStyle(workbook));
        styles.put("data", createDataStyle(workbook));
        styles.put("alternateRow", createAlternateRowStyle(workbook));
        styles.put("category", createCategoryStyle(workbook));
        styles.put("answered", createAnsweredStyle(workbook));
        styles.put("unanswered", createUnansweredStyle(workbook));
        styles.put("answeredAlternate", createAnsweredAlternateStyle(workbook));
        styles.put("unansweredAlternate", createUnansweredAlternateStyle(workbook));
        styles.put("infoLabel", createInfoLabelStyle(workbook));
        styles.put("infoValue", createInfoValueStyle(workbook));
        return styles;
    }

    private void createMainSheet(Sheet sheet, Workflow workflow, List<QuestionTemplate> questionTemplates, 
                               List<PlantSpecificData> plantDataList, Map<String, CellStyle> styles) {
        int rowNum = 0;

        // Create title row with enhanced styling
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.setHeightInPoints(30);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("QRMFG Questionnaire Report - Workflow ID: " + workflow.getId());
        titleCell.setCellStyle(styles.get("title"));
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));

        // Add empty row for spacing
        rowNum++;

        // Create workflow info section with better styling
        createWorkflowInfoSection(sheet, workflow, rowNum, styles);
        rowNum += 8;

        // Create header row with enhanced styling (removed repetitive columns)
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.setHeightInPoints(25);
        String[] headers = {"Sr No", "Question Text", "Responsible Team", "Answer", "Data Source", "Last Modified"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.get("header"));
        }

        // Parse plant data to get answers
        Map<String, String> answerMap = new HashMap<>();
        String lastModifiedBy = "";
        String lastModifiedDate = "";
        
        for (PlantSpecificData plantData : plantDataList) {
            try {
                // Parse CQS inputs
                if (plantData.getCqsInputs() != null && !plantData.getCqsInputs().trim().isEmpty()) {
                    JsonNode cqsNode = objectMapper.readTree(plantData.getCqsInputs());
                    extractAnswersFromJson(cqsNode, answerMap, "CQS");
                }
                
                // Parse plant inputs
                if (plantData.getPlantInputs() != null && !plantData.getPlantInputs().trim().isEmpty()) {
                    JsonNode plantNode = objectMapper.readTree(plantData.getPlantInputs());
                    extractAnswersFromJson(plantNode, answerMap, "Plant");
                }
                
                // Get modification info
                if (plantData.getUpdatedBy() != null) {
                    lastModifiedBy = plantData.getUpdatedBy();
                }
                if (plantData.getUpdatedAt() != null) {
                    lastModifiedDate = plantData.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }
            } catch (Exception e) {
                // Log error but continue processing
                System.err.println("Error parsing plant data JSON: " + e.getMessage());
            }
        }

        // Group questions by category
        Map<String, List<QuestionTemplate>> questionsByCategory = questionTemplates.stream()
            .collect(Collectors.groupingBy(
                q -> q.getCategory() != null ? q.getCategory() : "General",
                LinkedHashMap::new,
                Collectors.toList()
            ));

        // Add data rows grouped by category with enhanced formatting
        for (Map.Entry<String, List<QuestionTemplate>> categoryEntry : questionsByCategory.entrySet()) {
            String category = categoryEntry.getKey();
            List<QuestionTemplate> categoryQuestions = categoryEntry.getValue();

            // Add category header with enhanced styling
            Row categoryRow = sheet.createRow(rowNum++);
            categoryRow.setHeightInPoints(25);
            Cell categoryCell = categoryRow.createCell(0);
            categoryCell.setCellValue(category.toUpperCase());
            categoryCell.setCellStyle(styles.get("category"));
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum-1, rowNum-1, 0, 5));

            // Add sub-header row for this category
            Row subHeaderRow = sheet.createRow(rowNum++);
            subHeaderRow.setHeightInPoints(20);
            String[] subHeaders = {"Sr No", "Question Text", "Responsible Team", "Answer", "Data Source", "Last Modified"};
            for (int i = 0; i < subHeaders.length; i++) {
                Cell cell = subHeaderRow.createCell(i);
                cell.setCellValue(subHeaders[i]);
                cell.setCellStyle(styles.get("subHeader"));
            }

            // Add questions in this category
            int questionCounter = 0;
            for (QuestionTemplate question : categoryQuestions) {
                Row dataRow = sheet.createRow(rowNum++);
                dataRow.setHeightInPoints(18);
                
                // Look for answer using field name
                String answerValue = answerMap.get(question.getFieldName());
                String dataSource = "";
                boolean isAnswered = answerValue != null && !answerValue.trim().isEmpty();
                
                if (isAnswered) {
                    // Determine data source based on responsible team
                    if ("CQS".equalsIgnoreCase(question.getResponsible())) {
                        dataSource = "CQS Auto-filled";
                    } else if (question.isForPlant()) {
                        dataSource = "Plant Input";
                    } else {
                        dataSource = "System";
                    }
                }

                // Choose style based on whether question is answered and alternating rows
                CellStyle rowStyle;
                if (isAnswered) {
                    rowStyle = (questionCounter % 2 == 0) ? styles.get("answered") : styles.get("answeredAlternate");
                } else {
                    rowStyle = (questionCounter % 2 == 0) ? styles.get("unanswered") : styles.get("unansweredAlternate");
                }

                // Create cells with data (removed repetitive columns)
                dataRow.createCell(0).setCellValue(question.getSrNo() != null ? question.getSrNo() : 0);
                dataRow.createCell(1).setCellValue(question.getQuestionText() != null ? question.getQuestionText() : "");
                
                // Responsible team without emojis
                String responsibleTeam = question.getResponsible() != null ? question.getResponsible() : "";
                dataRow.createCell(2).setCellValue(responsibleTeam);
                
                dataRow.createCell(3).setCellValue(isAnswered ? answerValue : "Not Answered");
                dataRow.createCell(4).setCellValue(dataSource);
                dataRow.createCell(5).setCellValue(isAnswered ? lastModifiedDate : "");

                // Apply styling to all cells
                for (int i = 0; i < 6; i++) {
                    if (dataRow.getCell(i) != null) {
                        dataRow.getCell(i).setCellStyle(rowStyle);
                    }
                }
                questionCounter++;
            }
            
            // Add spacing row between categories
            Row spacingRow = sheet.createRow(rowNum++);
            spacingRow.setHeightInPoints(10);
        }

        // Set optimal column widths (updated for new structure)
        sheet.setColumnWidth(0, 2500);  // Sr No
        sheet.setColumnWidth(1, 15000); // Question Text (wider since no category column)
        sheet.setColumnWidth(2, 4500);  // Responsible Team
        sheet.setColumnWidth(3, 10000); // Answer (wider)
        sheet.setColumnWidth(4, 4000);  // Data Source
        sheet.setColumnWidth(5, 4500);  // Last Modified

        // Freeze panes to keep headers visible
        sheet.createFreezePane(0, 10); // Freeze first 10 rows (title + info + headers)

        // Add print settings
        sheet.getPrintSetup().setLandscape(true);
        sheet.setFitToPage(true);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);
    }

    private void extractAnswersFromJson(JsonNode jsonNode, Map<String, String> answerMap, String source) {
        if (jsonNode.isObject()) {
            jsonNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                
                if (value.isTextual()) {
                    answerMap.put(key, value.asText());
                } else if (value.isNumber()) {
                    answerMap.put(key, value.asText());
                } else if (value.isBoolean()) {
                    answerMap.put(key, value.asBoolean() ? "Yes" : "No");
                } else if (value.isArray()) {
                    StringBuilder arrayValue = new StringBuilder();
                    for (JsonNode arrayItem : value) {
                        if (arrayValue.length() > 0) arrayValue.append(", ");
                        arrayValue.append(arrayItem.asText());
                    }
                    answerMap.put(key, arrayValue.toString());
                }
            });
        }
    }

    private void createSummarySheet(Sheet sheet, Workflow workflow, List<QuestionTemplate> questionTemplates, 
                                  List<PlantSpecificData> plantDataList, Map<String, CellStyle> styles) {
        int rowNum = 0;

        // Title with enhanced styling
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.setHeightInPoints(30);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("SUMMARY REPORT");
        titleCell.setCellStyle(styles.get("title"));
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4));

        rowNum++;

        // Workflow info
        createWorkflowInfoSection(sheet, workflow, rowNum, styles);
        rowNum += 8;

        // Parse answers to count completion
        Map<String, String> answerMap = new HashMap<>();
        for (PlantSpecificData plantData : plantDataList) {
            try {
                if (plantData.getCqsInputs() != null && !plantData.getCqsInputs().trim().isEmpty()) {
                    JsonNode cqsNode = objectMapper.readTree(plantData.getCqsInputs());
                    extractAnswersFromJson(cqsNode, answerMap, "CQS");
                }
                if (plantData.getPlantInputs() != null && !plantData.getPlantInputs().trim().isEmpty()) {
                    JsonNode plantNode = objectMapper.readTree(plantData.getPlantInputs());
                    extractAnswersFromJson(plantNode, answerMap, "Plant");
                }
            } catch (Exception e) {
                System.err.println("Error parsing plant data JSON: " + e.getMessage());
            }
        }

        // Statistics with enhanced styling
        Row statsHeaderRow = sheet.createRow(rowNum++);
        statsHeaderRow.setHeightInPoints(22);
        Cell statsHeaderCell = statsHeaderRow.createCell(0);
        statsHeaderCell.setCellValue("📈 COMPLETION STATISTICS");
        statsHeaderCell.setCellStyle(styles.get("subHeader"));
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum-1, rowNum-1, 0, 4));

        long totalQuestions = questionTemplates.size();
        long answeredQuestions = questionTemplates.stream()
            .mapToLong(q -> answerMap.containsKey(q.getFieldName()) && 
                           !answerMap.get(q.getFieldName()).trim().isEmpty() ? 1 : 0)
            .sum();
        long unansweredQuestions = totalQuestions - answeredQuestions;
        double completionRate = totalQuestions > 0 ? (double) answeredQuestions / totalQuestions * 100 : 0;

        addInfoRow(sheet, rowNum++, "📊 Total Questions:", String.valueOf(totalQuestions), styles);
        addInfoRow(sheet, rowNum++, "✅ Answered Questions:", String.valueOf(answeredQuestions), styles);
        addInfoRow(sheet, rowNum++, "❌ Unanswered Questions:", String.valueOf(unansweredQuestions), styles);
        addInfoRow(sheet, rowNum++, "📈 Completion Rate:", String.format("%.1f%%", completionRate) + getCompletionEmoji(completionRate), styles);

        rowNum++;

        // Plant data status
        if (!plantDataList.isEmpty()) {
            Row plantStatusHeaderRow = sheet.createRow(rowNum++);
            plantStatusHeaderRow.setHeightInPoints(22);
            Cell plantStatusHeaderCell = plantStatusHeaderRow.createCell(0);
            plantStatusHeaderCell.setCellValue("🏭 PLANT DATA STATUS");
            plantStatusHeaderCell.setCellStyle(styles.get("subHeader"));
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum-1, rowNum-1, 0, 4));

            for (PlantSpecificData plantData : plantDataList) {
                addInfoRow(sheet, rowNum++, "🏭 Plant Code:", plantData.getPlantCode(), styles);
                addInfoRow(sheet, rowNum++, "📊 Completion Status:", getStatusIcon(plantData.getCompletionStatus()) + " " + plantData.getCompletionStatus(), styles);
                addInfoRow(sheet, rowNum++, "📈 Completion Percentage:", plantData.getCompletionPercentage() + "%" + getCompletionEmoji(plantData.getCompletionPercentage()), styles);
                addInfoRow(sheet, rowNum++, "🔄 CQS Sync Status:", getSyncStatusIcon(plantData.getCqsSyncStatus()) + " " + plantData.getCqsSyncStatus(), styles);
                rowNum++;
            }
        }

        // Category breakdown
        Row categoryHeaderRow = sheet.createRow(rowNum++);
        categoryHeaderRow.setHeightInPoints(22);
        Cell categoryHeaderCell = categoryHeaderRow.createCell(0);
        categoryHeaderCell.setCellValue("📂 CATEGORY BREAKDOWN");
        categoryHeaderCell.setCellStyle(styles.get("subHeader"));
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum-1, rowNum-1, 0, 4));

        Map<String, Long> categoryStats = questionTemplates.stream()
            .collect(Collectors.groupingBy(
                q -> q.getCategory() != null ? q.getCategory() : "General",
                Collectors.counting()
            ));

        for (Map.Entry<String, Long> entry : categoryStats.entrySet()) {
            addInfoRow(sheet, rowNum++, "📁 " + entry.getKey() + ":", String.valueOf(entry.getValue()) + " questions", styles);
        }

        // Set optimal column widths
        sheet.setColumnWidth(0, 6000);  // Label column
        sheet.setColumnWidth(1, 8000);  // Value column
        for (int i = 2; i < 5; i++) {
            sheet.setColumnWidth(i, 2000);
        }

        // Add print settings
        sheet.getPrintSetup().setLandscape(false);
        sheet.setFitToPage(true);
    }

    private void createTeamBreakdownSheet(Sheet sheet, Workflow workflow, List<QuestionTemplate> questionTemplates, 
                                        List<PlantSpecificData> plantDataList, Map<String, CellStyle> styles) {
        int rowNum = 0;

        // Title with enhanced styling
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.setHeightInPoints(30);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("TEAM BREAKDOWN REPORT");
        titleCell.setCellStyle(styles.get("title"));
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4));

        rowNum++;

        // Header row with enhanced styling
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.setHeightInPoints(25);
        String[] headers = {"Team", "Total Questions", "Answered", "Unanswered", "Completion %"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.get("header"));
        }

        // Parse answers
        Map<String, String> answerMap = new HashMap<>();
        for (PlantSpecificData plantData : plantDataList) {
            try {
                if (plantData.getCqsInputs() != null && !plantData.getCqsInputs().trim().isEmpty()) {
                    JsonNode cqsNode = objectMapper.readTree(plantData.getCqsInputs());
                    extractAnswersFromJson(cqsNode, answerMap, "CQS");
                }
                if (plantData.getPlantInputs() != null && !plantData.getPlantInputs().trim().isEmpty()) {
                    JsonNode plantNode = objectMapper.readTree(plantData.getPlantInputs());
                    extractAnswersFromJson(plantNode, answerMap, "Plant");
                }
            } catch (Exception e) {
                System.err.println("Error parsing plant data JSON: " + e.getMessage());
            }
        }

        // Group questions by responsible team
        Map<String, List<QuestionTemplate>> questionsByTeam = questionTemplates.stream()
            .collect(Collectors.groupingBy(
                q -> q.getResponsible() != null ? q.getResponsible() : "Unassigned",
                LinkedHashMap::new,
                Collectors.toList()
            ));

        // Add team statistics with enhanced formatting
        int teamRowCounter = 0;
        for (Map.Entry<String, List<QuestionTemplate>> teamEntry : questionsByTeam.entrySet()) {
            String team = teamEntry.getKey();
            List<QuestionTemplate> teamQuestions = teamEntry.getValue();
            
            long totalQuestions = teamQuestions.size();
            long answeredQuestions = teamQuestions.stream()
                .mapToLong(q -> {
                    String answer = answerMap.get(q.getFieldName());
                    return (answer != null && !answer.trim().isEmpty()) ? 1 : 0;
                })
                .sum();
            long unansweredQuestions = totalQuestions - answeredQuestions;
            double completionRate = totalQuestions > 0 ? (double) answeredQuestions / totalQuestions * 100 : 0;

            Row dataRow = sheet.createRow(rowNum++);
            dataRow.setHeightInPoints(20);
            
            // Choose alternating row style
            CellStyle rowStyle = (teamRowCounter % 2 == 0) ? styles.get("data") : styles.get("alternateRow");
            
            // Team name without icon
            dataRow.createCell(0).setCellValue(team);
            dataRow.createCell(1).setCellValue(totalQuestions);
            dataRow.createCell(2).setCellValue(answeredQuestions);
            dataRow.createCell(3).setCellValue(unansweredQuestions);
            dataRow.createCell(4).setCellValue(String.format("%.1f%%", completionRate));

            // Apply styling
            for (int i = 0; i < 5; i++) {
                if (dataRow.getCell(i) != null) {
                    dataRow.getCell(i).setCellStyle(rowStyle);
                }
            }
            teamRowCounter++;
        }

        // Set optimal column widths
        sheet.setColumnWidth(0, 4000);  // Team
        sheet.setColumnWidth(1, 3500);  // Total Questions
        sheet.setColumnWidth(2, 3000);  // Answered
        sheet.setColumnWidth(3, 3000);  // Unanswered
        sheet.setColumnWidth(4, 4000);  // Completion %

        // Add print settings
        sheet.getPrintSetup().setLandscape(false);
        sheet.setFitToPage(true);
    }

    private void createWorkflowInfoSection(Sheet sheet, Workflow workflow, int startRow, Map<String, CellStyle> styles) {
        // Create info section header
        Row infoHeaderRow = sheet.createRow(startRow++);
        infoHeaderRow.setHeightInPoints(20);
        Cell infoHeaderCell = infoHeaderRow.createCell(0);
        infoHeaderCell.setCellValue("WORKFLOW INFORMATION");
        infoHeaderCell.setCellStyle(styles.get("subHeader"));
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(startRow-1, startRow-1, 0, 3));

        // Add workflow details with enhanced styling
        addInfoRow(sheet, startRow++, "Workflow ID:", String.valueOf(workflow.getId()), styles);
        addInfoRow(sheet, startRow++, "Material Code:", workflow.getMaterialCode(), styles);
        addInfoRow(sheet, startRow++, "Material Name:", workflow.getMaterialName() != null ? workflow.getMaterialName() : "N/A", styles);
        addInfoRow(sheet, startRow++, "Project Code:", workflow.getProjectCode(), styles);
        addInfoRow(sheet, startRow++, "Plant Code:", workflow.getPlantCode(), styles);
        addInfoRow(sheet, startRow++, "Status:", workflow.getState().toString(), styles);
        addInfoRow(sheet, startRow++, "Generated:", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), styles);
    }

    private void addInfoRow(Sheet sheet, int rowNum, String label, String value, Map<String, CellStyle> styles) {
        Row row = sheet.createRow(rowNum);
        row.setHeightInPoints(16);
        
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(styles.get("infoLabel"));
        
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(styles.get("infoValue"));
        
        // Merge value cell across multiple columns for better readability
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum, rowNum, 1, 3));
    }

    private String getStatusIcon(String status) {
        switch (status.toUpperCase()) {
            case "COMPLETED": return "✅";
            case "JVC_PENDING": return "🟡";
            case "PLANT_PENDING": return "🔵";
            case "CQS_PENDING": return "🟣";
            case "TECH_PENDING": return "🟠";
            default: return "⚪";
        }
    }

    private String getCompletionEmoji(double percentage) {
        if (percentage >= 90) return " 🎉";
        else if (percentage >= 75) return " 😊";
        else if (percentage >= 50) return " 😐";
        else if (percentage >= 25) return " 😟";
        else return " 😰";
    }

    private String getSyncStatusIcon(String syncStatus) {
        switch (syncStatus.toUpperCase()) {
            case "SYNCED": return "✅";
            case "PENDING": return "⏳";
            case "FAILED": return "❌";
            default: return "❓";
        }
    }

    private String getTeamIcon(String team) {
        switch (team.toUpperCase()) {
            case "CQS": return "🤖";
            case "PLANT": 
            case "ALL PLANTS":
            case "PLANT TO FILL DATA": return "🏭";
            case "JVC": return "🔬";
            case "TECH": return "⚙️";
            case "ADMIN": return "👨‍💼";
            default: return "👤";
        }
    }

    // Style creation methods
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createSubHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createAlternateRowStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createCategoryStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderTop(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createAnsweredStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createUnansweredStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createAnsweredAlternateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createUnansweredAlternateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createInfoLabelStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createInfoValueStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
}