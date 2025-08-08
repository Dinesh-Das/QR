package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.dto.DocumentSummary;
import com.cqs.qrmfg.dto.UnifiedDocumentSearchResult;
import com.cqs.qrmfg.dto.WorkflowSummaryDto;
import com.cqs.qrmfg.enums.DocumentSource;
import com.cqs.qrmfg.exception.DocumentException;
import com.cqs.qrmfg.exception.WorkflowNotFoundException;
import com.cqs.qrmfg.model.Document;
import com.cqs.qrmfg.model.Workflow;
import com.cqs.qrmfg.repository.DocumentRepository;
import com.cqs.qrmfg.repository.WorkflowRepository;
import com.cqs.qrmfg.service.DocumentExportService;
import com.cqs.qrmfg.service.DocumentService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DocumentExportServiceImpl implements DocumentExportService {

    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private WorkflowRepository workflowRepository;
    
    @Autowired
    private DocumentService documentService;

    @Override
    public Resource exportDocumentsAsZip(List<DocumentSummary> documents, String exportName) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);
            
            // Add manifest file
            String manifest = createDocumentManifest(documents, "CSV");
            ZipEntry manifestEntry = new ZipEntry("document_manifest.csv");
            zos.putNextEntry(manifestEntry);
            zos.write(manifest.getBytes());
            zos.closeEntry();
            
            // Add each document
            for (DocumentSummary docSummary : documents) {
                try {
                    Document document = documentRepository.findById(docSummary.getId()).orElse(null);
                    if (document != null && document.getFilePath() != null) {
                        Path filePath = Paths.get(document.getFilePath());
                        if (Files.exists(filePath)) {
                            // Create folder structure in ZIP based on document source
                            String folderName = getFolderNameForSource(document.getDocumentSource());
                            String entryName = folderName + "/" + document.getOriginalFileName();
                            
                            ZipEntry entry = new ZipEntry(entryName);
                            zos.putNextEntry(entry);
                            
                            byte[] fileBytes = Files.readAllBytes(filePath);
                            zos.write(fileBytes);
                            zos.closeEntry();
                            
                            System.out.println("Added to ZIP: " + entryName);
                        } else {
                            System.err.println("File not found: " + filePath);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error adding document " + docSummary.getOriginalFileName() + " to ZIP: " + e.getMessage());
                    // Continue with other documents
                }
            }
            
            zos.close();
            
            byte[] zipBytes = baos.toByteArray();
            return new ByteArrayResource(zipBytes) {
                @Override
                public String getFilename() {
                    return exportName + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".zip";
                }
            };
            
        } catch (IOException e) {
            throw new DocumentException("Failed to create ZIP export", e);
        }
    }

    @Override
    public Resource exportWorkflowDocuments(Long workflowId, boolean includeQueryDocuments) {
        try {
            Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found with ID: " + workflowId));
            
            List<DocumentSummary> documents;
            String exportName;
            
            if (includeQueryDocuments) {
                // Get all documents using unified search
                UnifiedDocumentSearchResult result = documentService.searchAllDocuments(
                    null, workflow.getProjectCode(), workflow.getMaterialCode(),
                    Arrays.asList(DocumentSource.WORKFLOW, DocumentSource.QUERY, DocumentSource.RESPONSE)
                );
                documents = result.getAllDocuments();
                exportName = String.format("workflow_%s_%s_%s_all_documents", 
                    workflow.getProjectCode(), workflow.getMaterialCode(), workflow.getPlantCode());
            } else {
                // Get only workflow documents
                documents = documentService.getWorkflowDocuments(workflowId);
                exportName = String.format("workflow_%s_%s_%s_documents", 
                    workflow.getProjectCode(), workflow.getMaterialCode(), workflow.getPlantCode());
            }
            
            return exportDocumentsAsZip(documents, exportName);
            
        } catch (Exception e) {
            throw new DocumentException("Failed to export workflow documents", e);
        }
    }

    @Override
    public String createDocumentManifest(List<DocumentSummary> documents, String format) {
        switch (format.toUpperCase()) {
            case "CSV":
                return createCsvManifest(documents);
            case "JSON":
                return createJsonManifest(documents);
            case "XML":
                return createXmlManifest(documents);
            default:
                return createCsvManifest(documents);
        }
    }
    
    private String createCsvManifest(List<DocumentSummary> documents) {
        StringBuilder csv = new StringBuilder();
        
        // Header
        csv.append("Document Name,File Type,Size (bytes),Source,Uploaded By,Upload Date,Project Code,Material Code,Workflow ID,Query ID,Is Reused\n");
        
        // Data rows
        for (DocumentSummary doc : documents) {
            csv.append(String.format("\"%s\",\"%s\",%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s,%s,%s\n",
                escapeCsv(doc.getOriginalFileName()),
                doc.getFileType() != null ? doc.getFileType() : "",
                doc.getFileSize() != null ? doc.getFileSize() : 0,
                doc.getDocumentSource() != null ? doc.getDocumentSource() : "WORKFLOW",
                doc.getUploadedBy() != null ? doc.getUploadedBy() : "",
                doc.getUploadedAt() != null ? doc.getUploadedAt().toString() : "",
                doc.getProjectCode() != null ? doc.getProjectCode() : "",
                doc.getMaterialCode() != null ? doc.getMaterialCode() : "",
                doc.getWorkflowId() != null ? doc.getWorkflowId().toString() : "",
                doc.getQueryId() != null ? doc.getQueryId().toString() : "",
                doc.getIsReused() != null ? doc.getIsReused().toString() : "false"
            ));
        }
        
        return csv.toString();
    }
    
    private String createJsonManifest(List<DocumentSummary> documents) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"exportDate\": \"").append(LocalDateTime.now().toString()).append("\",\n");
        json.append("  \"totalDocuments\": ").append(documents.size()).append(",\n");
        json.append("  \"documents\": [\n");
        
        for (int i = 0; i < documents.size(); i++) {
            DocumentSummary doc = documents.get(i);
            json.append("    {\n");
            json.append("      \"name\": \"").append(escapeJson(doc.getOriginalFileName())).append("\",\n");
            json.append("      \"fileType\": \"").append(doc.getFileType() != null ? doc.getFileType() : "").append("\",\n");
            json.append("      \"size\": ").append(doc.getFileSize() != null ? doc.getFileSize() : 0).append(",\n");
            json.append("      \"source\": \"").append(doc.getDocumentSource() != null ? doc.getDocumentSource() : "WORKFLOW").append("\",\n");
            json.append("      \"uploadedBy\": \"").append(doc.getUploadedBy() != null ? doc.getUploadedBy() : "").append("\",\n");
            json.append("      \"uploadDate\": \"").append(doc.getUploadedAt() != null ? doc.getUploadedAt().toString() : "").append("\",\n");
            json.append("      \"projectCode\": \"").append(doc.getProjectCode() != null ? doc.getProjectCode() : "").append("\",\n");
            json.append("      \"materialCode\": \"").append(doc.getMaterialCode() != null ? doc.getMaterialCode() : "").append("\",\n");
            json.append("      \"isReused\": ").append(doc.getIsReused() != null ? doc.getIsReused() : false).append("\n");
            json.append("    }");
            if (i < documents.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("  ]\n");
        json.append("}\n");
        
        return json.toString();
    }
    
    private String createXmlManifest(List<DocumentSummary> documents) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<documentManifest>\n");
        xml.append("  <exportDate>").append(LocalDateTime.now().toString()).append("</exportDate>\n");
        xml.append("  <totalDocuments>").append(documents.size()).append("</totalDocuments>\n");
        xml.append("  <documents>\n");
        
        for (DocumentSummary doc : documents) {
            xml.append("    <document>\n");
            xml.append("      <name>").append(escapeXml(doc.getOriginalFileName())).append("</name>\n");
            xml.append("      <fileType>").append(doc.getFileType() != null ? doc.getFileType() : "").append("</fileType>\n");
            xml.append("      <size>").append(doc.getFileSize() != null ? doc.getFileSize() : 0).append("</size>\n");
            xml.append("      <source>").append(doc.getDocumentSource() != null ? doc.getDocumentSource() : "WORKFLOW").append("</source>\n");
            xml.append("      <uploadedBy>").append(doc.getUploadedBy() != null ? doc.getUploadedBy() : "").append("</uploadedBy>\n");
            xml.append("      <uploadDate>").append(doc.getUploadedAt() != null ? doc.getUploadedAt().toString() : "").append("</uploadDate>\n");
            xml.append("      <projectCode>").append(doc.getProjectCode() != null ? doc.getProjectCode() : "").append("</projectCode>\n");
            xml.append("      <materialCode>").append(doc.getMaterialCode() != null ? doc.getMaterialCode() : "").append("</materialCode>\n");
            xml.append("      <isReused>").append(doc.getIsReused() != null ? doc.getIsReused() : false).append("</isReused>\n");
            xml.append("    </document>\n");
        }
        
        xml.append("  </documents>\n");
        xml.append("</documentManifest>\n");
        
        return xml.toString();
    }
    
    private String getFolderNameForSource(DocumentSource source) {
        if (source == null) {
            return "workflow_documents";
        }
        
        switch (source) {
            case WORKFLOW:
                return "workflow_documents";
            case QUERY:
                return "query_documents";
            case RESPONSE:
                return "response_documents";
            default:
                return "other_documents";
        }
    }
    
    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
    
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
    
    private String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
    
    @Override
    public Resource exportWorkflowsToExcel(List<WorkflowSummaryDto> workflows, String exportName) {
        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Workflows");
            
            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            
            // Create data style
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            
            // Create date style
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.cloneStyleFrom(dataStyle);
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "ID", "Project Code", "Material Code", "Material Name", "Material Description", 
                "Item Description", "Current State", "Assigned Plant", "Plant Code", 
                "Initiated By", "Days Pending", "Total Queries", "Open Queries", 
                "Document Count", "Created At", "Last Modified", "Overdue"
            };
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Add data rows
            int rowNum = 1;
            for (WorkflowSummaryDto workflow : workflows) {
                Row row = sheet.createRow(rowNum++);
                
                // ID
                Cell cell0 = row.createCell(0);
                if (workflow.getId() != null) {
                    cell0.setCellValue(workflow.getId());
                }
                cell0.setCellStyle(dataStyle);
                
                // Project Code
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(workflow.getProjectCode() != null ? workflow.getProjectCode() : "");
                cell1.setCellStyle(dataStyle);
                
                // Material Code
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(workflow.getMaterialCode() != null ? workflow.getMaterialCode() : "");
                cell2.setCellStyle(dataStyle);
                
                // Material Name
                Cell cell3 = row.createCell(3);
                cell3.setCellValue(workflow.getMaterialName() != null ? workflow.getMaterialName() : "");
                cell3.setCellStyle(dataStyle);
                
                // Material Description
                Cell cell4 = row.createCell(4);
                cell4.setCellValue(workflow.getMaterialDescription() != null ? workflow.getMaterialDescription() : "");
                cell4.setCellStyle(dataStyle);
                
                // Item Description
                Cell cell5 = row.createCell(5);
                cell5.setCellValue(workflow.getItemDescription() != null ? workflow.getItemDescription() : "");
                cell5.setCellStyle(dataStyle);
                
                // Current State
                Cell cell6 = row.createCell(6);
                cell6.setCellValue(workflow.getCurrentState() != null ? workflow.getCurrentState().toString() : "");
                cell6.setCellStyle(dataStyle);
                
                // Assigned Plant
                Cell cell7 = row.createCell(7);
                cell7.setCellValue(workflow.getAssignedPlant() != null ? workflow.getAssignedPlant() : "");
                cell7.setCellStyle(dataStyle);
                
                // Plant Code
                Cell cell8 = row.createCell(8);
                cell8.setCellValue(workflow.getPlantCode() != null ? workflow.getPlantCode() : "");
                cell8.setCellStyle(dataStyle);
                
                // Initiated By
                Cell cell9 = row.createCell(9);
                cell9.setCellValue(workflow.getInitiatedBy() != null ? workflow.getInitiatedBy() : "");
                cell9.setCellStyle(dataStyle);
                
                // Days Pending
                Cell cell10 = row.createCell(10);
                cell10.setCellValue(workflow.getDaysPending());
                cell10.setCellStyle(dataStyle);
                
                // Total Queries
                Cell cell11 = row.createCell(11);
                cell11.setCellValue(workflow.getTotalQueries());
                cell11.setCellStyle(dataStyle);
                
                // Open Queries
                Cell cell12 = row.createCell(12);
                cell12.setCellValue(workflow.getOpenQueries());
                cell12.setCellStyle(dataStyle);
                
                // Document Count
                Cell cell13 = row.createCell(13);
                cell13.setCellValue(workflow.getDocumentCount());
                cell13.setCellStyle(dataStyle);
                
                // Created At
                Cell cell14 = row.createCell(14);
                if (workflow.getCreatedAt() != null) {
                    cell14.setCellValue(java.sql.Timestamp.valueOf(workflow.getCreatedAt()));
                    cell14.setCellStyle(dateStyle);
                } else {
                    cell14.setCellStyle(dataStyle);
                }
                
                // Last Modified
                Cell cell15 = row.createCell(15);
                if (workflow.getLastModified() != null) {
                    cell15.setCellValue(java.sql.Timestamp.valueOf(workflow.getLastModified()));
                    cell15.setCellStyle(dateStyle);
                } else {
                    cell15.setCellStyle(dataStyle);
                }
                
                // Overdue
                Cell cell16 = row.createCell(16);
                cell16.setCellValue(workflow.isOverdue() ? "Yes" : "No");
                cell16.setCellStyle(dataStyle);
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // Set minimum width to prevent too narrow columns
                if (sheet.getColumnWidth(i) < 2000) {
                    sheet.setColumnWidth(i, 2000);
                }
                // Set maximum width to prevent too wide columns
                if (sheet.getColumnWidth(i) > 8000) {
                    sheet.setColumnWidth(i, 8000);
                }
            }
            
            // Write to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();
            
            byte[] excelBytes = baos.toByteArray();
            return new ByteArrayResource(excelBytes) {
                @Override
                public String getFilename() {
                    return exportName + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
                }
            };
            
        } catch (IOException e) {
            throw new DocumentException("Failed to create Excel export", e);
        }
    }
}