package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.QuestionTemplate;
import com.cqs.qrmfg.repository.QuestionTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class QuestionnaireTemplateInitializer implements CommandLineRunner {

    @Autowired
    private QuestionTemplateRepository questionTemplateRepository;

    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("QuestionnaireTemplateInitializer: Starting template initialization check...");
            
            // Check existing templates
            long existingCount = questionTemplateRepository.countByIsActiveTrue();
            System.out.println("QuestionnaireTemplateInitializer: Found " + existingCount + " existing active templates");
            
            // Force reinitialize with correct data structure (temporary for development)
            if (existingCount > 0) {
                System.out.println("QuestionnaireTemplateInitializer: Clearing existing templates and reinitializing...");
                questionTemplateRepository.deleteAll();
            }
            
            System.out.println("QuestionnaireTemplateInitializer: Initializing templates with correct structure...");
            initializeDefaultTemplate();
        } catch (Exception e) {
            System.err.println("QuestionnaireTemplateInitializer: Error during initialization: " + e.getMessage());
            e.printStackTrace();
            // Don't throw the exception to prevent application startup failure
        }
    }

    private void initializeDefaultTemplate() {
        List<QuestionTemplate> templates = Arrays.asList(
            // General (Step 1)
            createTemplate(1, 1, "General", "Is 16 Section MSDS of the raw material available?", "Is 16 Section MSDS of the raw material available?", "Plant", "radio", "msds_available", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),
            createTemplate(2, 1, "General", "Which information in any one of the 16 sections is not available in full?", "Which information in any one of the 16 sections is not available in full?", "Plant", "textarea", "missing_info", false, null),
            createTemplate(3, 1, "General", "Has the identified missing / more information required from the supplier asked thru Sourcing?", "Has the identified missing / more information required from the supplier asked thru Sourcing?", "Plant", "radio", "sourcing_asked", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(4, 1, "General", "Is CAS number of the raw material based on the pure substance available?", "Is CAS number of the raw material based on the pure substance available?", "Plant", "radio", "cas_available", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),
            createTemplate(5, 1, "General", "For mixtures, are ingredients of mixture available?", "For mixtures, are ingredients of mixture available?", "Plant", "radio", "mixture_ingredients", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(6, 1, "General", "Is % age composition substances in the mixture available?", "Is % age composition substances in the mixture available?", "Plant", "radio", "composition_percentage", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(7, 1, "General", "Is the total %age of all substances in the mixture equal to 100? If not what is the % of substances not available?", "Is the total %age of all substances in the mixture equal to 100? If not what is the % of substances not available?", "Plant", "textarea", "total_percentage", false, null),

            // Physical (Step 2)
            createTemplate(8, 2, "Physical", "Is the material corrosive?", "Is the material corrosive?", "CQS", "radio", "is_corrosive", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),
            createTemplate(9, 2, "Physical", "Does the plant have acid and alkali proof storage facilities to store a corrosive raw material?", "Does the plant have acid and alkali proof storage facilities to store a corrosive raw material?", "Plant", "radio", "corrosive_storage", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(10, 2, "Physical", "Is the material highly toxic?", "Is the material highly toxic?", "CQS", "radio", "highly_toxic", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),
            createTemplate(11, 2, "Physical", "Does the plant have facilities to handle fine powder of highly toxic raw material?", "Does the plant have facilities to handle fine powder of highly toxic raw material?", "Plant", "radio", "toxic_powder_handling", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(12, 2, "Physical", "Does the plant have facilities to crush the stone like solid raw material?", "Does the plant have facilities to crush the stone like solid raw material?", "Plant", "radio", "crushing_facilities", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(13, 2, "Physical", "Does the plant have facilities to heat/melt the raw material if required for charging the same in a batch?", "Does the plant have facilities to heat/melt the raw material if required for charging the same in a batch?", "Plant", "radio", "heating_facilities", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(14, 2, "Physical", "Does the plant have facilities to prepare paste of raw material if required for charging the same in a batch?", "Does the plant have facilities to prepare paste of raw material if required for charging the same in a batch?", "Plant", "radio", "paste_preparation", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),

            // Flammability and Explosivity (Step 3)
            createTemplate(15, 3, "Flammability and Explosivity", "Is Flash point of the raw material given and less than or equal to 65 degree C?", "Is Flash point of the raw material given and less than or equal to 65 degree C?", "CQS", "radio", "flash_point_65", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(16, 3, "Flammability and Explosivity", "Is the raw material is to be catgorised as ClassC / Class B / Class A substance as per Petroleum Act / Rules?", "Is the raw material is to be catgorised as ClassC / Class B / Class A substance as per Petroleum Act / Rules?", "CQS", "select", "petroleum_class", false, "[{\"value\":\"class_a\",\"label\":\"Class A\"},{\"value\":\"class_b\",\"label\":\"Class B\"},{\"value\":\"class_c\",\"label\":\"Class C\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(17, 3, "Flammability and Explosivity", "Does all the plants have the capacity and license to store the raw material?", "Does all the plants have the capacity and license to store the raw material?", "Plant", "radio", "storage_license", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),
            createTemplate(18, 3, "Flammability and Explosivity", "If no, has the plant applied for CCoE license and by when expected to receive the license?", "If no, has the plant applied for CCoE license and by when expected to receive the license?", "Plant", "textarea", "ccoe_license", false, null),
            createTemplate(19, 3, "Flammability and Explosivity", "Is Flash point of the raw material given is less than 21 degree C?", "Is Flash point of the raw material given is less than 21 degree C?", "CQS", "radio", "flash_point_21", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(20, 3, "Flammability and Explosivity", "If yes, does plant have infrastructure to comply State Factories Rule for handling 'Flammable liquids'?", "If yes, does plant have infrastructure to comply State Factories Rule for handling 'Flammable liquids'?", "Plant", "radio", "flammable_infrastructure", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(21, 3, "Flammability and Explosivity", "Does the plant require to have additonal storage capacities to store the raw material?", "Does the plant require to have additonal storage capacities to store the raw material?", "Plant", "radio", "additional_storage", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),
            createTemplate(22, 3, "Flammability and Explosivity", "Is the raw material explosive as per MSDS?", "Is the raw material explosive as per MSDS?", "CQS", "radio", "is_explosive", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),
            createTemplate(23, 3, "Flammability and Explosivity", "If yes, does the plant has facilities to store such raw material?", "If yes, does the plant has facilities to store such raw material?", "Plant", "radio", "explosive_storage", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(24, 3, "Flammability and Explosivity", "Is Autoignition temperature of the material is less than or equal to that of MTO?", "Is Autoignition temperature of the material is less than or equal to that of MTO?", "CQS", "radio", "autoignition_temp", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(25, 3, "Flammability and Explosivity", "Does the plant have facilities to handle the raw material?", "Does the plant have facilities to handle the raw material?", "Plant", "radio", "handling_facilities", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),
            createTemplate(26, 3, "Flammability and Explosivity", "Does the material has Dust explosion hazard?", "Does the material has Dust explosion hazard?", "CQS", "radio", "dust_explosion", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),
            createTemplate(27, 3, "Flammability and Explosivity", "If yes, does plant has infrastructure to handle material having dust explosion hazard?", "If yes, does plant has infrastructure to handle material having dust explosion hazard?", "Plant", "radio", "dust_explosion_handling", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(28, 3, "Flammability and Explosivity", "Does the raw material likely to generate electrostatic charge at the time of transfer or charging?", "Does the raw material likely to generate electrostatic charge at the time of transfer or charging?", "CQS", "radio", "electrostatic_charge", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),
            createTemplate(29, 3, "Flammability and Explosivity", "If yes, does plant has infrastructure to handle material having electrostatic hazard?", "If yes, does plant has infrastructure to handle material having electrostatic hazard?", "Plant", "radio", "electrostatic_handling", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),

            // Toxicity (Step 4) - Complete toxicity questions
            createTemplate(30, 4, "Toxicity", "Is LD 50 (oral) value available and higher than the threshold limit of 200 mg/Kg BW?", "Is LD 50 (oral) value available and higher than the threshold limit of 200 mg/Kg BW?", "CQS", "radio", "ld50_oral", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(31, 4, "Toxicity", "Is LD 50 (Dermal) value available and higher than 1000 mg/Kg BW?", "Is LD 50 (Dermal) value available and higher than 1000 mg/Kg BW?", "CQS", "radio", "ld50_dermal", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(32, 4, "Toxicity", "Is LC50 Inhalation value available and higher than 10 mg/L?", "Is LC50 Inhalation value available and higher than 10 mg/L?", "CQS", "radio", "lc50_inhalation", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(33, 4, "Toxicity", "If no, in any of the above three cases (where avaialble) then does the plant have facilities and /or procedure to minmise the exposure of workman?", "If no, in any of the above three cases (where avaialble) then does the plant have facilities and /or procedure to minmise the exposure of workman?", "Plant", "textarea", "exposure_minimization", false, null),
            createTemplate(34, 4, "Toxicity", "Is the RM a suspect Carcinogenic?", "Is the RM a suspect Carcinogenic?", "CQS", "radio", "carcinogenic", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(35, 4, "Toxicity", "If yes, plant has adequate facilities and /or procedure to minimse the exposure of workman?", "If yes, plant has adequate facilities and /or procedure to minimse the exposure of workman?", "Plant", "textarea", "carcinogenic_control", false, null),
            createTemplate(36, 4, "Toxicity", "Is the RM a suspect Mutagenic?", "Is the RM a suspect Mutagenic?", "CQS", "radio", "mutagenic", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(37, 4, "Toxicity", "If yes, plant has adequate facilities and /or procedure to minimse the exposure of workman?", "If yes, plant has adequate facilities and /or procedure to minimse the exposure of workman?", "Plant", "textarea", "mutagenic_control", false, null),
            createTemplate(38, 4, "Toxicity", "Is the RM a suspect endocrine disruptor?", "Is the RM a suspect endocrine disruptor?", "CQS", "radio", "endocrine_disruptor", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(39, 4, "Toxicity", "If yes, plant has adequate facilities and /or procedure to minimse the exposure of workman?", "If yes, plant has adequate facilities and /or procedure to minimse the exposure of workman?", "Plant", "textarea", "endocrine_control", false, null),
            createTemplate(40, 4, "Toxicity", "Is the RM a reproductive toxicant?", "Is the RM a reproductive toxicant?", "CQS", "radio", "reproductive_toxicant", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(41, 4, "Toxicity", "If yes, plant has adequate facilities and /or procedure to minimse the exposure of workman?", "If yes, plant has adequate facilities and /or procedure to minimse the exposure of workman?", "Plant", "textarea", "reproductive_control", false, null),
            createTemplate(42, 4, "Toxicity", "Is the RM contains Silica > 1%", "Is the RM contains Silica > 1%", "CQS", "radio", "silica_content", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(43, 4, "Toxicity", "Is SWARF analysis required? If yes, analysis done and report available for silica content?", "Is SWARF analysis required? If yes, analysis done and report available for silica content?", "CQS", "radio", "swarf_analysis", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(44, 4, "Toxicity", "In the RM a highly toxic to the environment?", "In the RM a highly toxic to the environment?", "CQS", "radio", "env_toxic", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),
            createTemplate(45, 4, "Toxicity", "If yes, plant has adequate facilities and /or procedure to minimse impact on environment?", "If yes, plant has adequate facilities and /or procedure to minimse impact on environment?", "Plant", "textarea", "env_impact_control", false, null),
            createTemplate(46, 4, "Toxicity", "Is the TLV / STEL values available and found to be higher than the average value observed during the work place monitoring studies at the shopfloor?", "Is the TLV / STEL values available and found to be higher than the average value observed during the work place monitoring studies at the shopfloor?", "Plant", "radio", "tlv_stel_values", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(47, 4, "Toxicity", "Is the RM falls under HHRM category?", "Is the RM falls under HHRM category?", "CQS", "radio", "hhrm_category", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),
            createTemplate(48, 4, "Toxicity", "Does the plant has infrastructure to handle HHRM?", "Does the plant has infrastructure to handle HHRM?", "Plant", "radio", "hhrm_infrastructure", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),

            // Process Safety Management (Step 5)
            createTemplate(49, 5, "Process Safety Management", "PSM Tier I Outdoor - Thershold quanitity (kgs)", "PSM Tier I Outdoor - Thershold quanitity (kgs)", "CQS", "input", "psm_tier1_outdoor", false, null),
            createTemplate(50, 5, "Process Safety Management", "PSM Tier I Indoor - Thershold quanitity (kgs)", "PSM Tier I Indoor - Thershold quanitity (kgs)", "CQS", "input", "psm_tier1_indoor", false, null),
            createTemplate(51, 5, "Process Safety Management", "PSM Tier II Outdoor - Thershold quanitity (kgs)", "PSM Tier II Outdoor - Thershold quanitity (kgs)", "CQS", "input", "psm_tier2_outdoor", false, null),
            createTemplate(52, 5, "Process Safety Management", "PSM Tier II Indoor - Thershold quanitity (kgs)", "PSM Tier II Indoor - Thershold quanitity (kgs)", "CQS", "input", "psm_tier2_indoor", false, null),

            // Reactivity Hazards (Step 6)
            createTemplate(53, 6, "Reactivity Hazards", "What is the compatible class and its incomatibility with other chemicals?", "What is the compatible class and its incomatibility with other chemicals?", "CQS", "textarea", "compatibility_class", false, null),
            createTemplate(54, 6, "Reactivity Hazards", "Is compatibility class available in SAP?", "Is compatibility class available in SAP?", "CQS", "radio", "sap_compatibility", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),
            createTemplate(55, 6, "Reactivity Hazards", "Does the plant have facilities to store and handle incompatible raw material in an isolated manner and away from other incomptible material", "Does the plant have facilities to store and handle incompatible raw material in an isolated manner and away from other incomptible material", "Plant", "radio", "isolated_storage", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),

            // Storage and Handling (Step 7)
            createTemplate(56, 7, "Storage and Handling", "Are any storage conditions required and available in the plant stores?", "Are any storage conditions required and available in the plant stores?", "Plant", "textarea", "storage_conditions_stores", false, null),
            createTemplate(57, 7, "Storage and Handling", "Are any storage conditions required and available in the shop floor?", "Are any storage conditions required and available in the shop floor?", "Plant", "textarea", "storage_conditions_floor", false, null),
            createTemplate(58, 7, "Storage and Handling", "Does it require closed loop handling system during charging?", "Does it require closed loop handling system during charging?", "Plant", "radio", "closed_loop_required", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),
            createTemplate(59, 7, "Storage and Handling", "Does the plant have required Work permit and /or WI/SOP to handle the raw material adequately?", "Does the plant have required Work permit and /or WI/SOP to handle the raw material adequately?", "Plant", "radio", "work_permit_available", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),
            createTemplate(60, 7, "Storage and Handling", "If, yes specify the procedures", "If, yes specify the procedures", "Plant", "textarea", "procedures_details", false, null),

            // PPE (Step 8)
            createTemplate(61, 8, "PPE", "Recommended specific PPEs based on MSDS", "Recommended specific PPEs based on MSDS", "CQS", "textarea", "recommended_ppe", false, null),
            createTemplate(62, 8, "PPE", "Are recommended PPE as per MSDS to handle the RM is already in use at the plants?", "Are recommended PPE as per MSDS to handle the RM is already in use at the plants?", "Plant", "radio", "ppe_in_use", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"partial\",\"label\":\"Partially\"}]"),
            createTemplate(63, 8, "PPE", "If no, by when the plant can procure the require PPE?", "If no, by when the plant can procure the require PPE?", "Plant", "input", "ppe_procurement_date", false, null),

            // Spill Control Measures (Step 9)
            createTemplate(64, 9, "Spill Control Measures", "Does the MSDS provide the specific spill control measures to be taken?", "Does the MSDS provide the specific spill control measures to be taken?", "CQS", "radio", "spill_measures_provided", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),
            createTemplate(65, 9, "Spill Control Measures", "Are the recommended spill control measures available in the plant?", "Are the recommended spill control measures available in the plant?", "Plant", "radio", "spill_measures_available", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"partial\",\"label\":\"Partially\"}]"),

            // First Aid (Step 10)
            createTemplate(66, 10, "First Aid", "Is the raw material poisonous as per the MSDS?", "Is the raw material poisonous as per the MSDS?", "CQS", "radio", "is_poisonous", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),
            createTemplate(67, 10, "First Aid", "Is the name of antidote required to counter the impact of the material given in the MSDS?", "Is the name of antidote required to counter the impact of the material given in the MSDS?", "CQS", "radio", "antidote_specified", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(68, 10, "First Aid", "Is the above specified antidote available in the plants?", "Is the above specified antidote available in the plants?", "Plant", "radio", "antidote_available", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(69, 10, "First Aid", "If the specified antidote is not available then what is source and who will obtain the antidote in the plant?", "If the specified antidote is not available then what is source and who will obtain the antidote in the plant?", "Plant", "textarea", "antidote_source", false, null),
            createTemplate(70, 10, "First Aid", "Does the plant has capability to provide the first aid mentioned in the MSDS with the existing control measures?", "Does the plant has capability to provide the first aid mentioned in the MSDS with the existing control measures?", "Plant", "radio", "first_aid_capability", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),

            // Statutory (Step 11)
            createTemplate(71, 11, "Statutory", "Is the RM or any of its ingredient listed in Table 3 of Rule 137 (CMVR)", "Is the RM or any of its ingredient listed in Table 3 of Rule 137 (CMVR)", "CQS", "radio", "cmvr_listed", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),
            createTemplate(72, 11, "Statutory", "Is the RM or any of its ingredient listed in part II of Schedule I of MSIHC Rule", "Is the RM or any of its ingredient listed in part II of Schedule I of MSIHC Rule", "CQS", "radio", "msihc_listed", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),
            createTemplate(73, 11, "Statutory", "Is the RM or any of its ingredients listed in Schedule II of Factories Act", "Is the RM or any of its ingredients listed in Schedule II of Factories Act", "CQS", "radio", "factories_act_listed", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),
            createTemplate(74, 11, "Statutory", "With the current infrastructure, is the concentration of RM / ingredients listed in Schedule II of Factories Act within permissible concentrations as per Factories Act in the work area.", "With the current infrastructure, is the concentration of RM / ingredients listed in Schedule II of Factories Act within permissible concentrations as per Factories Act in the work area.", "Plant", "radio", "permissible_concentration", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(75, 11, "Statutory", "Mention details of work area monitoring results and describe infrastructure used for handling", "Mention details of work area monitoring results and describe infrastructure used for handling", "Plant", "textarea", "monitoring_details", false, null),
            createTemplate(76, 11, "Statutory", "If actual concentrations of the RM / ingredients listed in Schedule II of Factories Act , in the shopfloor are not available, is the RM / ingredient listed in schedule II of Factories Act included in next six monthly work area monitoring.", "If actual concentrations of the RM / ingredients listed in Schedule II of Factories Act , in the shopfloor are not available, is the RM / ingredient listed in schedule II of Factories Act included in next six monthly work area monitoring.", "Plant", "radio", "monitoring_included", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),
            createTemplate(77, 11, "Statutory", "If permissible limits of exposure of RM / ingredients listed in Schedule II of Factories Act are not complied as work area monitoring , share details fo CAPEX planned for implementing closed loop addition system addition? Note : If permissible limits of exposure of RM / ingredients listed in Schedule II of Factories Act are not complied in any / subsequent work area monitoring , CAPEX is to be raised for implementing closed loop addition system addition?", "If permissible limits of exposure of RM / ingredients listed in Schedule II of Factories Act are not complied as work area monitoring , share details fo CAPEX planned for implementing closed loop addition system addition? Note : If permissible limits of exposure of RM / ingredients listed in Schedule II of Factories Act are not complied in any / subsequent work area monitoring , CAPEX is to be raised for implementing closed loop addition system addition?", "Plant", "textarea", "capex_details", false, null),
            createTemplate(78, 11, "Statutory", "Is the RM listed under Narcotic Drugs and Psychotropic Substances, Act,1988?", "Is the RM listed under Narcotic Drugs and Psychotropic Substances, Act,1988?", "CQS", "radio", "narcotic_listed", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"}]"),
            createTemplate(79, 11, "Statutory", "Does the plant have valid license to handle / store the raw material?", "Does the plant have valid license to handle / store the raw material?", "Plant", "radio", "valid_license", false, "[{\"value\":\"yes\",\"label\":\"Yes\"},{\"value\":\"no\",\"label\":\"No\"},{\"value\":\"na\",\"label\":\"N/A\"}]"),

            // Others (Step 12)
            createTemplate(80, 12, "GAPS", "Inputs required from plants based on the above assessment?", "Inputs required from plants based on the above assessment?", "Plant", "textarea", "plant_inputs_required", false, null),
            createTemplate(81, 12, "GAPS", "Gaps identified vis-à-vis existing controls / protocols", "Gaps identified vis-à-vis existing controls / protocols", "Plant", "textarea", "gaps_identified", false, null),
            createTemplate(82, 12, "GAPS", "1", "Additional input 1", "Plant", "textarea", "additional_input_1", false, null),
            createTemplate(83, 12, "GAPS", "2", "Additional input 2", "Plant", "textarea", "additional_input_2", false, null),
            createTemplate(84, 12, "GAPS", "3", "Additional input 3", "Plant", "textarea", "additional_input_3", false, null),
            createTemplate(85, 12, "GAPS", "4", "Additional input 4", "Plant", "textarea", "additional_input_4", false, null),
            createTemplate(86, 12, "GAPS", "5", "Additional input 5", "Plant", "textarea", "additional_input_5", false, null),
            createTemplate(87, 12, "GAPS", "6", "Additional input 6", "Plant", "textarea", "additional_input_6", false, null)
        );

        questionTemplateRepository.saveAll(templates);
        System.out.println("Initialized " + templates.size() + " questionnaire templates");
    }

    private QuestionTemplate createTemplate(int srNo, int stepNumber, String category, String questionText, 
                                          String comments, String responsible, String questionType, 
                                          String fieldName, boolean isRequired, String options) {
        QuestionTemplate template = new QuestionTemplate();
        template.setSrNo(srNo);
        template.setStepNumber(stepNumber);
        template.setCategory(category);
        template.setQuestionText(questionText);
        template.setComments(comments);
        template.setResponsible(responsible);
        template.setQuestionType(questionType.toUpperCase());
        template.setFieldName(fieldName);
        template.setIsRequired(isRequired);
        template.setOptions(options);
        template.setOrderIndex(srNo);
        template.setIsActive(true);
        template.setVersion(1);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        template.setCreatedBy("SYSTEM");
        template.setUpdatedBy("SYSTEM");

        // Set help text based on field
        switch (fieldName) {
            case "materialName":
                template.setHelpText("Material name from project item master");
                break;
            case "casNumber":
                template.setHelpText("Chemical Abstracts Service number - unique identifier for chemical substances");
                break;
            case "supplierName":
                template.setHelpText("Enter the name of the supplier company");
                break;
            case "physicalState":
                template.setHelpText("Physical state at room temperature (20°C)");
                break;
            case "color":
                template.setHelpText("Describe the color and appearance of the material");
                break;
            case "odor":
                template.setHelpText("Describe the odor characteristics");
                break;
            case "boilingPoint":
                template.setHelpText("Temperature at which the material changes from liquid to gas");
                break;
            case "meltingPoint":
                template.setHelpText("Temperature at which the material changes from solid to liquid");
                break;
            case "hazardCategories":
                template.setHelpText("Select all applicable hazard classifications according to GHS");
                break;
            case "signalWord":
                template.setHelpText("GHS signal word based on the most severe hazard category");
                break;
            case "personalProtection":
                template.setHelpText("Required PPE based on hazard assessment");
                break;
            case "firstAidMeasures":
                template.setHelpText("Describe first aid procedures for different exposure routes");
                break;
            case "storageConditions":
                template.setHelpText("Specify temperature, humidity, and other storage requirements");
                break;
            case "handlingPrecautions":
                template.setHelpText("Describe safe handling procedures and precautions");
                break;
            case "disposalMethods":
                template.setHelpText("Describe proper disposal methods and regulatory requirements");
                break;
            case "spillCleanup":
                template.setHelpText("Describe procedures for cleaning up spills and leaks");
                break;
            case "environmentalHazards":
                template.setHelpText("Environmental impact classifications according to GHS");
                break;
            case "wasteTreatment":
                template.setHelpText("Describe waste treatment and disposal methods");
                break;
            default:
                template.setHelpText("Please provide the required information");
        }

        return template;
    }
}