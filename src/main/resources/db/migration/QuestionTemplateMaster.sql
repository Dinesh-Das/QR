-- Clean approach: Drop and recreate the template table with proper data
-- This avoids any unique constraint issues

-- Drop existing table and sequence if they exist
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE QRMFG_QUESTION_TEMPLATES CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        NULL; -- Table might not exist
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP SEQUENCE QUESTIONNAIRE_TEMPLATE_SEQ';
EXCEPTION
    WHEN OTHERS THEN
        NULL; -- Sequence might not exist
END;
/

-- Create the template table
CREATE TABLE QRMFG_QUESTION_TEMPLATES (
    ID NUMBER(19) PRIMARY KEY,
    SR_NO NUMBER(10) NOT NULL,
    CATEGORY VARCHAR2(100) NOT NULL,
    QUESTION_TEXT VARCHAR2(2000) NOT NULL,
    COMMENTS VARCHAR2(2000),
    RESPONSIBLE VARCHAR2(100) NOT NULL,
    QUESTION_TYPE VARCHAR2(50) DEFAULT 'TEXT',
    STEP_NUMBER NUMBER(10) NOT NULL,
    FIELD_NAME VARCHAR2(100) NOT NULL,
    IS_REQUIRED NUMBER(1) DEFAULT 0,
    OPTIONS VARCHAR2(2000),
    VALIDATION_RULES VARCHAR2(500),
    CONDITIONAL_LOGIC VARCHAR2(1000),
    DEPENDS_ON_QUESTION_ID VARCHAR2(50),
    HELP_TEXT VARCHAR2(500),
    ORDER_INDEX NUMBER(10) NOT NULL,
    IS_ACTIVE NUMBER(1) DEFAULT 1,
    VERSION NUMBER(10) DEFAULT 1,
    CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY VARCHAR2(50) DEFAULT 'SYSTEM',
    UPDATED_BY VARCHAR2(50) DEFAULT 'SYSTEM'
);

-- Create sequence
CREATE SEQUENCE QUESTIONNAIRE_TEMPLATE_SEQ START WITH 88 INCREMENT BY 1;

-- Create indexes
CREATE INDEX IDX_QRMFG_QUEST_TEMPLATE_CAT ON QRMFG_QUESTION_TEMPLATES(CATEGORY);
CREATE INDEX IDX_QRMFG_QUEST_TEMPLATE_STEP ON QRMFG_QUESTION_TEMPLATES(STEP_NUMBER);
CREATE INDEX IDX_QRMFG_QUEST_TEMPLATE_RESP ON QRMFG_QUESTION_TEMPLATES(RESPONSIBLE);
CREATE INDEX IDX_QRMFG_QUEST_TEMPLATE_ORDER ON QRMFG_QUESTION_TEMPLATES(ORDER_INDEX);
CREATE UNIQUE INDEX IDX_QRMFG_QUEST_TEMPLATE_SR ON QRMFG_QUESTION_TEMPLATES(SR_NO, VERSION);

-- Insert complete questionnaire data (ID = SR_NO for all records)
-- General Section (1-7)
INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (1, 1, 'General', 'Is 16 Section MSDS of the raw material available?', 'Not applicable', 'NONE', 'DISPLAY', 1, 'question_1', 1, 0, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (2, 2, 'General', 'Which information in any one of the 16 sections is not available in full?', 'Not applicable', 'NONE', 'DISPLAY', 1, 'question_2', 2, 0, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (3, 3, 'General', 'Has the identified missing / more information required from the supplier asked thru Sourcing?', 'Not applicable', 'NONE', 'DISPLAY', 1, 'question_3', 3, 0, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (4, 4, 'General', 'Is CAS number of the raw material based on the pure substance available?', 'Not applicable', 'NONE', 'DISPLAY', 1, 'question_4', 4, 0, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (5, 5, 'General', 'For mixtures, are ingredients of mixture available?', 'Not applicable', 'NONE', 'DISPLAY', 1, 'question_5', 5, 0, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (6, 6, 'General', 'Is % age composition substances in the mixture available?', 'Not applicable', 'NONE', 'DISPLAY', 1, 'question_6', 6, 0, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (7, 7, 'General', 'Is the total %age of all substances in the mixture equal to 100? If not what is the % of substances not available?', 'Not applicable', 'NONE', 'DISPLAY', 1, 'question_7', 7, 0, 1);

-- Physical Section (8-14)
INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (8, 8, 'Physical', 'Is the material corrosive?', 'CQS', 'CQS', 'RADIO', 2, 'question_8', 8, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (9, 9, 'Physical', 'Does the plant have acid and alkali proof storage facilities to store a corrosive raw material?', 'Plant to fill data', 'Plant', 'RADIO', 2, 'question_9', 9, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (10, 10, 'Physical', 'Is the material highly toxic?', 'CQS', 'CQS', 'RADIO', 2, 'question_10', 10, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (11, 11, 'Physical', 'Does the plant have facilities to handle fine powder of highly toxic raw material?', 'Plant to fill data', 'Plant', 'RADIO', 2, 'question_11', 11, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (12, 12, 'Physical', 'Does the plant have facilities to crush the stone like solid raw material?', 'Plant to fill data', 'Plant', 'RADIO', 2, 'question_12', 12, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (13, 13, 'Physical', 'Does the plant have facilities to heat/melt the raw material if required for charging the same in a batch?', 'Plant to fill data', 'Plant', 'RADIO', 2, 'question_13', 13, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (14, 14, 'Physical', 'Does the plant have facilities to prepare paste of raw material if required for charging the same in a batch?', 'Plant to fill data', 'Plant', 'RADIO', 2, 'question_14', 14, 1, 1);

-- Flammability and Explosivity Section (15-29)
INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (15, 15, 'Flammability and Explosivity', 'Is Flash point of the raw material given and less than or equal to 65 degree C?', 'CQS', 'CQS', 'RADIO', 3, 'question_15', 15, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (16, 16, 'Flammability and Explosivity', 'Is the raw material is to be catgorised as ClassC / Class B / Class A substance as per Petroleum Act / Rules?', 'CQS', 'CQS', 'RADIO', 3, 'question_16', 16, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (17, 17, 'Flammability and Explosivity', 'Does all the plants have the capacity and license to store the raw material?', 'Plant to fill data', 'Plant', 'RADIO', 3, 'question_17', 17, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (18, 18, 'Flammability and Explosivity', 'If no, has the plant applied for CCoE license and by when expected to receive the license?', 'Plant to fill data', 'Plant', 'TEXTAREA', 3, 'question_18', 18, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (19, 19, 'Flammability and Explosivity', 'Is Flash point of the raw material given is less than 21 degree C?', 'CQS', 'CQS', 'RADIO', 3, 'question_19', 19, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (20, 20, 'Flammability and Explosivity', 'If yes, does plant have infrastructure to comply State Factories Rule for handling ''Flammable liquids''?', 'Plant to fill data', 'Plant', 'RADIO', 3, 'question_20', 20, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (21, 21, 'Flammability and Explosivity', 'Does the plant require to have additonal storage capacities to store the raw material?', 'Plant to fill data', 'Plant', 'RADIO', 3, 'question_21', 21, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (22, 22, 'Flammability and Explosivity', 'Is the raw material explosive as per MSDS?', 'CQS', 'CQS', 'RADIO', 3, 'question_22', 22, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (23, 23, 'Flammability and Explosivity', 'If yes, does the plant has facilities to store such raw material?', 'Plant to fill data', 'Plant', 'RADIO', 3, 'question_23', 23, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (24, 24, 'Flammability and Explosivity', 'Is Autoignition temperature of the material is less than or equal to that of MTO?', 'CQS', 'CQS', 'RADIO', 3, 'question_24', 24, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (25, 25, 'Flammability and Explosivity', 'Does the plant have facilities to handle the raw material?', 'Plant to fill data', 'Plant', 'RADIO', 3, 'question_25', 25, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (26, 26, 'Flammability and Explosivity', 'Does the material has Dust explosion hazard?', 'CQS', 'CQS', 'RADIO', 3, 'question_26', 26, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (27, 27, 'Flammability and Explosivity', 'If yes, does plant has infrastructure to handle material having dust explosion hazard?', 'Plant to fill data', 'Plant', 'RADIO', 3, 'question_27', 27, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (28, 28, 'Flammability and Explosivity', 'Does the raw material likely to generate electrostatic charge at the time of transfer or charging?', '', 'CQS', 'RADIO', 3, 'question_28', 28, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (29, 29, 'Flammability and Explosivity', 'If yes, does plant has infrastructure to handle material having electrostatic hazard?', 'Plant to fill data', 'Plant', 'RADIO', 3, 'question_29', 29, 1, 1);

-- Toxicity Section (30-48)
INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (30, 30, 'Toxicity', 'Is LD 50 (oral) value available and higher than the threshold limit of 200 mg/Kg BW?', '', 'CQS', 'RADIO', 4, 'question_30', 30, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (31, 31, 'Toxicity', 'Is LD 50 (Dermal) value available and higher than 1000 mg/Kg BW?', '', 'CQS', 'RADIO', 4, 'question_31', 31, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (32, 32, 'Toxicity', 'Is LC50 Inhalation value available and higher than 10 mg/L?', '', 'CQS', 'RADIO', 4, 'question_32', 32, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (33, 33, 'Toxicity', 'If no, in any of the above three cases (where avaialble) then does the plant have facilities and /or procedure to minmise the exposure of workman?', 'Plant to fill data', 'Plant', 'TEXTAREA', 4, 'question_33', 33, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (34, 34, 'Toxicity', 'Is the RM a suspect Carcinogenic?', '', 'CQS', 'RADIO', 4, 'question_34', 34, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (35, 35, 'Toxicity', 'If yes, plant has adequate facilities and /or procedure to minimse the exposure of workman?', 'Plant to fill data', 'Plant', 'TEXTAREA', 4, 'question_35', 35, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (36, 36, 'Toxicity', 'Is the RM a suspect Mutagenic?', 'CQS', 'CQS', 'RADIO', 4, 'question_36', 36, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (37, 37, 'Toxicity', 'If yes, plant has adequate facilities and /or procedure to minimse the exposure of workman?', 'Plant to fill data', 'Plant', 'TEXTAREA', 4, 'question_37', 37, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (38, 38, 'Toxicity', 'Is the RM a suspect endocrine disruptor?', 'CQS', 'CQS', 'RADIO', 4, 'question_38', 38, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (39, 39, 'Toxicity', 'If yes, plant has adequate facilities and /or procedure to minimse the exposure of workman?', 'Plant to fill data', 'Plant', 'TEXTAREA', 4, 'question_39', 39, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (40, 40, 'Toxicity', 'Is the RM a reproductive toxicant?', 'CQS', 'CQS', 'RADIO', 4, 'question_40', 40, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (41, 41, 'Toxicity', 'If yes, plant has adequate facilities and /or procedure to minimse the exposure of workman?', 'Plant to fill data', 'Plant', 'TEXTAREA', 4, 'question_41', 41, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (42, 42, 'Toxicity', 'Is the RM contains Silica > 1%', 'CQS', 'CQS', 'RADIO', 4, 'question_42', 42, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (43, 43, 'Toxicity', 'Is SWARF analysis required? If yes, analysis done and report available for silica content?', 'CQS', 'CQS', 'RADIO', 4, 'question_43', 43, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (44, 44, 'Toxicity', 'In the RM a highly toxic to the environment?', 'CQS', 'CQS', 'RADIO', 4, 'question_44', 44, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (45, 45, 'Toxicity', 'If yes, plant has adequate facilities and /or procedure to minimse impact on environment?', 'Plant to fill data', 'Plant', 'TEXTAREA', 4, 'question_45', 45, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (46, 46, 'Toxicity', 'Is the TLV / STEL values available and found to be higher than the average value observed during the work place monitoring studies at the shopfloor?', 'Plant to fill data', 'Plant', 'RADIO', 4, 'question_46', 46, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (47, 47, 'Toxicity', 'Is the RM falls under HHRM category?', 'CQS', 'CQS', 'RADIO', 4, 'question_47', 47, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (48, 48, 'Toxicity', 'Does the plant has infrastructure to handle HHRM?', 'Plant to fill data', 'Plant', 'RADIO', 4, 'question_48', 48, 1, 1);

-- Process Safety Management Section (49-52)
INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (49, 49, 'Process Safety Management', 'PSM Tier I Outdoor - Thershold quanitity (kgs)', 'CQS', 'CQS', 'TEXT', 5, 'question_49', 49, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (50, 50, 'Process Safety Management', 'PSM Tier I Indoor - Thershold quanitity (kgs)', 'CQS', 'CQS', 'TEXT', 5, 'question_50', 50, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (51, 51, 'Process Safety Management', 'PSM Tier II Outdoor - Thershold quanitity (kgs)', 'CQS', 'CQS', 'TEXT', 5, 'question_51', 51, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (52, 52, 'Process Safety Management', 'PSM Tier II Indoor - Thershold quanitity (kgs)', 'CQS', 'CQS', 'TEXT', 5, 'question_52', 52, 1, 1);

-- Reactivity Hazards Section (53-55)
INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (53, 53, 'Reactivity Hazards', 'What is the compatible class and its incomatibility with other chemicals?', 'CQS', 'CQS', 'TEXTAREA', 6, 'question_53', 53, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (54, 54, 'Reactivity Hazards', 'Is compatibility class available in SAP?', 'CQS', 'CQS', 'RADIO', 6, 'question_54', 54, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (55, 55, 'Reactivity Hazards', 'Does the plant have facilities to store & handle incompatible raw material in an isolated manner and away from other incomptible material', 'Plant to fill data', 'Plant', 'RADIO', 6, 'question_55', 55, 1, 1);

-- Storage and Handling Section (56-60)
INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (56, 56, 'Storage and Handling', 'Are any storage conditions required and available in the plant stores?', 'Plant to fill data', 'Plant', 'RADIO', 7, 'question_56', 56, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (57, 57, 'Storage and Handling', 'Are any storage conditions required and available in the shop floor?', 'Plant to fill data', 'Plant', 'RADIO', 7, 'question_57', 57, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (58, 58, 'Storage and Handling', 'Does it require closed loop handling system during charging?', 'Plant to fill data', 'Plant', 'RADIO', 7, 'question_58', 58, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (59, 59, 'Storage and Handling', 'Does the plant have required Work permit and /or WI/SOP to handle the raw material adequately?', 'Plant to fill data', 'Plant', 'RADIO', 7, 'question_59', 59, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (60, 60, 'Storage and Handling', 'If, yes specify the procedures', 'Plant to fill data', 'Plant', 'TEXTAREA', 7, 'question_60', 60, 1, 1);

-- PPE Section (61-63)
INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (61, 61, 'PPE', 'Recommended specific PPEs based on MSDS', 'CQS', 'CQS', 'TEXTAREA', 8, 'question_61', 61, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (62, 62, 'PPE', 'Are recommended PPE as per MSDS to handle the RM is already in use at the plants?', 'Plant to fill data', 'Plant', 'RADIO', 8, 'question_62', 62, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (63, 63, 'PPE', 'If no, by when the plant can procure the require PPE?', 'Plant to fill data', 'Plant', 'TEXTAREA', 8, 'question_63', 63, 1, 1);

-- Spill Control Measures Section (64-65)
INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (64, 64, 'Spill Control Measures', 'Does the MSDS provide the specific spill control measures to be taken?', 'CQS', 'CQS', 'RADIO', 9, 'question_64', 64, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (65, 65, 'Spill Control Measures', 'Are the recommended spill control measures available in the plant?', 'Plant to fill data', 'Plant', 'RADIO', 9, 'question_65', 65, 1, 1);

-- First Aid Section (66-70)
INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (66, 66, 'First Aid', 'Is the raw material poisonous as per the MSDS?', 'CQS', 'CQS', 'RADIO', 10, 'question_66', 66, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (67, 67, 'First Aid', 'Is the name of antidote required to counter the impact of the material given in the MSDS?', 'CQS', 'CQS', 'RADIO', 10, 'question_67', 67, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (68, 68, 'First Aid', 'Is the above specified antidote available in the plants?', 'Plant to fill data', 'Plant', 'RADIO', 10, 'question_68', 68, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (69, 69, 'First Aid', 'If the specified antidote is not available then what is source and who will obtain the antidote in the plant?', 'Plant to fill data', 'Plant', 'TEXTAREA', 10, 'question_69', 69, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (70, 70, 'First Aid', 'Does the plant has capability to provide the first aid mentioned in the MSDS with the existing control measures?', 'Plant to fill data', 'Plant', 'RADIO', 10, 'question_70', 70, 1, 1);

-- Statutory Section (71-79)
INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (71, 71, 'Statutory', 'Is the RM or any of its ingredient listed in Table 3 of Rule 137 (CMVR)', 'CQS', 'CQS', 'RADIO', 11, 'question_71', 71, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (72, 72, 'Statutory', 'Is the RM or any of its ingredient listed in part II of Schedule I of MSIHC Rule', 'CQS', 'CQS', 'RADIO', 11, 'question_72', 72, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (73, 73, 'Statutory', 'Is the RM or any of its ingredients listed in Schedule II of Factories Act', 'CQS', 'CQS', 'RADIO', 11, 'question_73', 73, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (74, 74, 'Statutory', 'With the current infrastructure, is the concentration of RM / ingredients   listed in Schedule II of Factories Act  within permissible concentrations as per  Factories Act in the work area.', 'Plant to fill data', 'Plant', 'RADIO', 11, 'question_74', 74, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (75, 75, 'Statutory', 'Mention details of work area monitoring results and describe infrastructure used for handling', 'Plant to fill data', 'Plant', 'TEXTAREA', 11, 'question_75', 75, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (76, 76, 'Statutory', 'If actual concentrations  of the  RM / ingredients listed in Schedule II of Factories Act , in the shopfloor are not available, is the RM / ingredient listed in schedule II of Factories Act included in next six monthly work area monitoring.', 'Plant to fill data', 'Plant', 'RADIO', 11, 'question_76', 76, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (77, 77, 'Statutory', 'If permissible  limits of exposure  of RM / ingredients listed in Schedule II of Factories Act are not complied as  work area monitoring , share details fo CAPEX  planned for implementing closed loop addition system addition? Note : If permissible  limits of exposure  of RM / ingredients listed in Schedule II of Factories Act are not complied in any / subsequent work area monitoring  , CAPEX is to be raised for implementing  closed loop addition system addition?', 'Plant to fill data', 'Plant', 'TEXTAREA', 11, 'question_77', 77, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (78, 78, 'Statutory', 'Is the RM listed under Narcotic Drugs and Psychotropic Substances,  Act,1988?', 'CQS', 'CQS', 'RADIO', 11, 'question_78', 78, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (79, 79, 'Statutory', 'Does the plant have valid license to handle / store the raw material?', 'Plant to fill data', 'Plant', 'RADIO', 11, 'question_79', 79, 1, 1);

-- Others Section (80-87)
INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (80, 80, 'Others', 'Inputs required from plants based on the above assessment?', 'Plant to fill data', 'Plant', 'TEXTAREA', 12, 'question_80', 80, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (81, 81, 'Others', 'Gaps identified vis-à-vis existing controls / protocols', 'All Plants', 'Plant', 'TEXTAREA', 12, 'question_81', 81, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (82, 82, 'Others', '1', '', 'Plant', 'TEXTAREA', 12, 'question_82', 82, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (83, 83, 'Others', '2', '', 'Plant', 'TEXTAREA', 12, 'question_83', 83, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (84, 84, 'Others', '3', '', 'Plant', 'TEXTAREA', 12, 'question_84', 84, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (85, 85, 'Others', '4', '', 'Plant', 'TEXTAREA', 12, 'question_85', 85, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (86, 86, 'Others', '5', '', 'Plant', 'TEXTAREA', 12, 'question_86', 86, 1, 1);

INSERT INTO QRMFG_QUESTION_TEMPLATES (ID, SR_NO, CATEGORY, QUESTION_TEXT, COMMENTS, RESPONSIBLE, QUESTION_TYPE, STEP_NUMBER, FIELD_NAME, ORDER_INDEX, IS_REQUIRED, VERSION) 
VALUES (87, 87, 'Others', '6', '', 'Plant', 'TEXTAREA', 12, 'question_87', 87, 1, 1);

-- Commit the transaction
COMMIT;