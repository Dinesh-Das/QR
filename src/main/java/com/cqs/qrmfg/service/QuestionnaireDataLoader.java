package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.Question;
import com.cqs.qrmfg.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionnaireDataLoader {

    @Autowired
    private QuestionRepository masterRepository;

    @Transactional
    public void loadQuestionnaireFromCSV(String csvFilePath, String materialCode) {
        List<Question> questions = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header
                }
                
                String[] values = parseCSVLine(line);
                if (values.length >= 5) {
                    Question question = createQuestionFromCSV(values, materialCode);
                    questions.add(question);
                }
            }
            
            // Save all questions
            masterRepository.saveAll(questions);
            
        } catch (IOException e) {
            throw new RuntimeException("Error reading CSV file: " + e.getMessage(), e);
        }
    }

    private Question createQuestionFromCSV(String[] values, String materialCode) {
        Question question = new Question();
        
        // Parse CSV values
        Integer srNo = parseInteger(values[0]);
        String category = values[1].trim();
        String questionText = values[2].trim();
        String comments = values[3].trim();
        String responsible = values[4].trim();
        
        // Set basic fields
        question.setSrNo(srNo);
        question.setCategory(category);
        question.setQuestionText(questionText);
        question.setChecklistText(questionText); // Same as question text
        question.setComments(comments);
        question.setResponsible(responsible);
        question.setMaterialCode(materialCode);
        
        // Generate question ID
        question.setQuestionId(generateQuestionId(materialCode, srNo));
        
        // Set field name for form binding
        question.setFieldName("question_" + srNo);
        
        // Determine question type based on content
        question.setQuestionType(determineQuestionType(questionText, responsible));
        
        // Set step number based on category grouping
        question.setStepNumber(getStepNumberForCategory(category));
        
        // Set order index
        question.setOrderIndex(srNo);
        
        // Set required flag based on responsible party
        question.setIsRequired(!responsible.equalsIgnoreCase("NONE"));
        
        // Set audit fields
        question.setCreatedAt(LocalDateTime.now());
        question.setUpdatedAt(LocalDateTime.now());
        question.setCreatedBy("SYSTEM");
        question.setUpdatedBy("SYSTEM");
        question.setIsActive(true);
        
        return question;
    }

    private String generateQuestionId(String materialCode, Integer srNo) {
        return String.format("%s_Q_%03d", materialCode, srNo);
    }

    private String determineQuestionType(String questionText, String responsible) {
        String lowerText = questionText.toLowerCase();
        
        if (lowerText.contains("is ") && lowerText.contains("?")) {
            return "RADIO"; // Yes/No questions
        } else if (lowerText.contains("does ") && lowerText.contains("?")) {
            return "RADIO"; // Yes/No questions
        } else if (lowerText.contains("specify") || lowerText.contains("mention") || lowerText.contains("details")) {
            return "TEXTAREA"; // Long text responses
        } else if (responsible.equalsIgnoreCase("NONE")) {
            return "DISPLAY"; // Information only
        } else {
            return "TEXT"; // Default to text input
        }
    }

    private Integer getStepNumberForCategory(String category) {
        switch (category.toLowerCase()) {
            case "general": return 1;
            case "physical": return 2;
            case "flammability and explosivity": return 3;
            case "toxicity": return 4;
            case "process safety management": return 5;
            case "reactivity hazards": return 6;
            case "storage and handling": return 7;
            case "ppe": return 8;
            case "spill control measures": return 9;
            case "first aid": return 10;
            case "statutory": return 11;
            case "others": return 12;
            default: return 13;
        }
    }

    private String[] parseCSVLine(String line) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentValue = new StringBuilder();
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        
        values.add(currentValue.toString());
        return values.toArray(new String[0]);
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}