package org.jbei.ice.lib.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jbei.ice.controllers.EntryController;
import org.jbei.ice.controllers.SequenceController;
import org.jbei.ice.controllers.common.ControllerException;
import org.jbei.ice.lib.managers.AccountManager;
import org.jbei.ice.lib.managers.ManagerException;
import org.jbei.ice.lib.models.Account;
import org.jbei.ice.lib.models.Entry;
import org.jbei.ice.lib.models.EntryFundingSource;
import org.jbei.ice.lib.models.FundingSource;
import org.jbei.ice.lib.models.Name;
import org.jbei.ice.lib.models.Plasmid;
import org.jbei.ice.lib.models.SelectionMarker;
import org.jbei.ice.lib.models.Sequence;
import org.jbei.ice.lib.models.Strain;
import org.jbei.ice.lib.permissions.PermissionException;
import org.jbei.ice.lib.vo.IDNASequence;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Bulk creation of new entries from csv files.
 * 
 * @author tham
 * 
 */
public class ImportHelper {

    /* The fields from the tab separated file for plasmid + strain are:
     * creator name, creator email, principal investigator,
     * funding source, plasmid name, plasmid alias, plasmid short description, 
     * plasmid backbone, plasmid promoters, plasmid origin, plasmid selection marker, 
     * keywords, plasmid notes, sequence file name, strain name, 
     * strain host, patent information, references.
     *
     * Don't forgot to populate the strain's 'plasmid' field. 
     */

    public static List<HashMap<String, String>> parseStrainPlasmidFile(File file)
            throws UtilityException {
        BufferedReader bufferedReader = null;
        CSVReader csvReader = null;
        List<String[]> parsedCsvContent = null;
        ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();

        try {
            bufferedReader = new BufferedReader(new FileReader(file));
            csvReader = new CSVReader(bufferedReader, '\t', '\"');
            parsedCsvContent = csvReader.readAll();
        } catch (FileNotFoundException e) {
            throw new UtilityException(e);
        } catch (IOException e) {
            throw new UtilityException(e);
        }
        if (parsedCsvContent != null) {
            HashMap<String, String> row = null;
            String[] rawRow = null;
            for (int i = 1; i < parsedCsvContent.size(); i++) { //skip first line
                row = new HashMap<String, String>();
                rawRow = parsedCsvContent.get(i);
                if (rawRow.length != 18) { //column "s" in spreadsheet 
                    throw new UtilityException("Incorrect number of columns for plasmid+strain");
                }

                row.put("name", rawRow[0]);
                row.put("creatorEmail", rawRow[1]);
                row.put("principalInvestigator", rawRow[2]);
                row.put("fundingSource", rawRow[3]);
                row.put("plasmidName", rawRow[4]);
                row.put("plasmidAlias", rawRow[5]);
                row.put("plasmidShortDescription", rawRow[6]);
                row.put("plasmidBackbone", rawRow[7]);
                row.put("plasmidPromoters", rawRow[8]);
                row.put("plasmidOrigin", rawRow[9]);
                row.put("plasmidSelectionMarker", rawRow[10]);
                row.put("plasmidKeywords", rawRow[11]);
                row.put("plasmidLongDescription", rawRow[12]);
                row.put("sequenceFileName", rawRow[13]);
                row.put("strainName", rawRow[14]);
                row.put("strainHost", rawRow[15]);
                row.put("patentInformation", rawRow[16]);
                row.put("references", rawRow[17]);

                result.add(row);
            }
        }

        return result;
    }

    public static void createNewStrainsWithPlasmids(List<HashMap<String, String>> parsedContent)
            throws UtilityException {
        // follow PlasmidStrainNewFormPanel.onSubmit()

        for (HashMap<String, String> item : parsedContent) {
            Strain strain = new Strain();
            Plasmid plasmid = new Plasmid();
            HashSet<Name> plasmidNames = new HashSet<Name>();
            plasmidNames.add(new Name(item.get("plasmidName"), plasmid));
            plasmid.setNames(plasmidNames);

            HashSet<SelectionMarker> selectionMarkers = new HashSet<SelectionMarker>();
            if (item.get("plasmidSelectionMarker") != null) {
                selectionMarkers.add(new SelectionMarker(item.get("plasmidSelectionMarker"),
                        plasmid));
            }
            plasmid.setSelectionMarkers(selectionMarkers);
            plasmid.setCreator(item.get("name"));
            plasmid.setCreatorEmail(item.get("creatorEmail"));
            plasmid.setOwner(item.get("name"));
            plasmid.setOwnerEmail(item.get("creatorEmail"));
            plasmid.setAlias(item.get("plasmidAlias"));
            plasmid.setStatus("complete");
            plasmid.setKeywords(item.get("plasmidKeywords"));
            plasmid.setShortDescription(item.get("plasmidShortDescription"));
            plasmid.setReferences(item.get("plasmidLongDescription"));
            plasmid.setBioSafetyLevel(1);
            plasmid.setIntellectualProperty(item.get("patentInformation"));
            FundingSource fundingSource = new FundingSource();
            fundingSource.setFundingSource(item.get("fundingSource"));
            fundingSource.setPrincipalInvestigator(item.get("principalInvestigator"));
            EntryFundingSource newPlasmidFundingSource = new EntryFundingSource();
            newPlasmidFundingSource.setEntry(plasmid);
            newPlasmidFundingSource.setFundingSource(fundingSource);
            Set<EntryFundingSource> plasmidFundingSources = new LinkedHashSet<EntryFundingSource>();
            plasmidFundingSources.add(newPlasmidFundingSource);
            plasmid.setEntryFundingSources(plasmidFundingSources);
            plasmid.setBackbone(item.get("plasmidBackbone"));
            plasmid.setOriginOfReplication(item.get("plasmidOrigin"));
            plasmid.setPromoters(item.get("plasmidPromoters"));
            plasmid.setCircular(true);
            plasmid.setLongDescription(item.get("plasmidLongDescription"));
            plasmid.setLongDescriptionType(Entry.MarkupType.text.name());

            HashSet<Name> strainNames = new HashSet<Name>();
            strainNames.add(new Name(item.get("strainName"), strain));
            strain.setNames(strainNames);
            strain.setCreator(plasmid.getCreator());
            strain.setCreatorEmail(plasmid.getCreatorEmail());
            strain.setOwner(plasmid.getOwner());
            strain.setOwnerEmail(plasmid.getOwnerEmail());
            EntryFundingSource newStrainFundingSource = new EntryFundingSource();
            newStrainFundingSource.setEntry(strain);
            newStrainFundingSource.setFundingSource(fundingSource);
            Set<EntryFundingSource> strainFundingSources = new LinkedHashSet<EntryFundingSource>();
            strainFundingSources.add(newStrainFundingSource);
            strain.setEntryFundingSources(strainFundingSources);
            strain.setHost(item.get("strainHost"));
            strain.setGenotypePhenotype("");
            strain.setPlasmids(item.get("plasmidName"));

            // empty fields
            strain.setAlias("");
            strain.setStatus("complete");
            strain.setKeywords("");
            strain.setShortDescription("");
            strain.setReferences("");
            strain.setBioSafetyLevel(1);
            strain.setIntellectualProperty("");
            strain.setLongDescription("");
            strain.setLongDescriptionType(Entry.MarkupType.text.name());

            Account account = null;
            try {
                account = AccountManager.getByEmail(item.get("creatorEmail"));
            } catch (ManagerException e2) {
                throw new UtilityException(e2);
            }

            // persist
            EntryController entryController = new EntryController(account);
            Plasmid newPlasmid = null;

            try {
                newPlasmid = (Plasmid) entryController.createEntry(plasmid, true, true);
                String plasmidPartNumberString = "[[jbei:"
                        + newPlasmid.getOnePartNumber().getPartNumber() + "|"
                        + newPlasmid.getOneName().getName() + "]]";
                strain.setPlasmids(plasmidPartNumberString);
                entryController.createEntry(strain, true, true);
            } catch (ControllerException e) {
                throw new UtilityException(e);
            }
            // set sequence
            SequenceController sequenceController = new SequenceController(account);

            String sequenceUser = item.get("sequenceUser");
            IDNASequence dnaSequence = null;
            if (sequenceUser != null) {
                dnaSequence = SequenceController.parse(item.get("sequenceUser"));
            }
            Sequence sequence = null;

            if (dnaSequence == null) {
                System.out.println("Could not parse sequence file. Perhaps file is not supported");
            } else {
                try {
                    sequence = SequenceController.dnaSequenceToSequence(dnaSequence);
                    sequence.setSequenceUser(item.get("sequenceUser"));
                    sequence.setEntry(plasmid);
                    sequenceController.save(sequence);
                } catch (ControllerException e) {
                    throw new UtilityException(e);
                } catch (PermissionException e) {
                    throw new UtilityException(e);
                }
            }
        } // end iterate over items

    }

    public static List<HashMap<String, String>> readSequenceFiles(
            List<HashMap<String, String>> parsedContent, String filePath, String filePostFix)
            throws UtilityException {
        for (HashMap<String, String> item : parsedContent) {
            StringBuilder sequenceStringBuilder = new StringBuilder();
            String fileName = item.get("sequenceFileName") + filePostFix;
            File sequenceFile = new File(filePath + File.separator + fileName);
            if (sequenceFile.canRead()) {
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new FileReader(sequenceFile));
                } catch (FileNotFoundException e1) {

                    e1.printStackTrace();
                }

                while (true) {
                    try {
                        String temp = br.readLine();
                        if (temp != null) {
                            sequenceStringBuilder.append(temp + '\n');
                        } else {
                            break;
                        }

                    } catch (IOException e) {
                        throw new UtilityException(e);
                    }
                }

            } else {
                System.err.println("Could not read file " + fileName);
            }
            item.put("sequenceUser", sequenceStringBuilder.toString());
        } // for item loop
        return parsedContent;
    }

    public static void main(String[] args) {
        String fileName = "/home/tham/Documents/Projects/taeksoon's parts/TSL_Strain_101004.data.csv";
        String sequenceFilesDir = "/home/tham/Documents/Projects/taeksoon's parts/sequences";
        File csvFile = new File(fileName);

        if (csvFile.canRead()) {
            try {
                List<HashMap<String, String>> parsedContent = parseStrainPlasmidFile(csvFile);
                parsedContent = readSequenceFiles(parsedContent, sequenceFilesDir, ".gb");
                createNewStrainsWithPlasmids(parsedContent);
                parsedContent.size();
            } catch (UtilityException e) {

                e.printStackTrace();
            }
        }
    }
}