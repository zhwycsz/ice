package org.jbei.ice.controllers;

import java.util.ArrayList;
import java.util.Set;

import org.jbei.ice.controllers.common.Controller;
import org.jbei.ice.controllers.common.ControllerException;
import org.jbei.ice.controllers.permissionVerifiers.EntryPermissionVerifier;
import org.jbei.ice.lib.managers.AccountManager;
import org.jbei.ice.lib.managers.EntryManager;
import org.jbei.ice.lib.managers.ManagerException;
import org.jbei.ice.lib.managers.SequenceManager;
import org.jbei.ice.lib.models.Account;
import org.jbei.ice.lib.models.Part;
import org.jbei.ice.lib.models.Sequence;
import org.jbei.ice.lib.models.SequenceFeature;
import org.jbei.ice.lib.models.Part.AssemblyStandard;
import org.jbei.ice.lib.permissions.PermissionException;
import org.jbei.ice.lib.utils.AssemblyUtils;
import org.jbei.ice.lib.utils.BiobrickAUtils;
import org.jbei.ice.lib.utils.BiobrickBUtils;
import org.jbei.ice.lib.utils.RawAssemblyUtils;
import org.jbei.ice.lib.utils.SequenceFeatureCollection;
import org.jbei.ice.lib.utils.UtilityException;

public class AssemblyController extends Controller {
    private ArrayList<AssemblyUtils> assemblyUtils = new ArrayList<AssemblyUtils>();

    public AssemblyController(Account account) {
        super(account, new EntryPermissionVerifier());
        getAssemblyUtils().add(new BiobrickAUtils());
        getAssemblyUtils().add(new BiobrickBUtils());
        getAssemblyUtils().add(new RawAssemblyUtils());
    }

    public AssemblyStandard determineAssemblyStandard(Sequence partSequence)
            throws ControllerException {
        AssemblyStandard result = null;
        String partSequenceString = partSequence.getSequence();
        result = determineAssemblyStandard(partSequenceString);
        return result;
    }

    public AssemblyStandard determineAssemblyStandard(String partSequenceString)
            throws ControllerException {
        AssemblyStandard result = null;
        int counter = 0;
        while (result == null && counter < getAssemblyUtils().size()) {
            try {
                result = getAssemblyUtils().get(counter).determineAssemblyStandard(
                    partSequenceString);
            } catch (UtilityException e) {
                throw new ControllerException(e);
            }
            counter = counter + 1;
        }
        return result;
    }

    public SequenceFeatureCollection determineAssemblyFeatures(Sequence partSequence)
            throws ControllerException {

        SequenceFeatureCollection sequenceFeatures = null;
        String partSequenceString = partSequence.getSequence();
        AssemblyStandard standard = determineAssemblyStandard(partSequenceString);
        try {
            if (standard == AssemblyStandard.BIOBRICKA) {
                sequenceFeatures = getAssemblyUtils().get(0)
                        .determineAssemblyFeatures(partSequence);
            } else if (standard == AssemblyStandard.BIOBRICKB) {
                sequenceFeatures = getAssemblyUtils().get(1)
                        .determineAssemblyFeatures(partSequence);
            } else if (standard == AssemblyStandard.RAW) {
                sequenceFeatures = getAssemblyUtils().get(2)
                        .determineAssemblyFeatures(partSequence);
            }
        } catch (UtilityException e) {
            throw new ControllerException(e);
        }

        return sequenceFeatures;
    }

    public static void main(String[] args) {
        try {
            mainRunBiobrickBTest();
        } catch (PermissionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ControllerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //mainRunJoin();

        /*
        InnerFeature feature = (InnerFeature) SequenceManager.getFeature(1347);
        System.out.println(feature.toString());
        */
    }

    private static void mainRunBiobrickBTest() throws PermissionException, ControllerException {
        AssemblyController as = null;
        try {
            as = new AssemblyController(AccountManager.get(86));
        } catch (ManagerException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            Part part2 = (Part) EntryManager.get(4394);
            Sequence part2Sequence = SequenceManager.getByEntry(part2);
            @SuppressWarnings("unused")
            Set<SequenceFeature> sequenceFeatures = part2Sequence.getSequenceFeatures();

            ////Part result = as.joinBiobrickB(part2, part2);

            SequenceFeatureCollection temp = as.determineAssemblyFeatures(part2Sequence);
            //sequenceFeatures.addAll(temp);
            //SequenceManager.saveSequence(part2Sequence);

            System.out.println("===\n" + part2.getOnePartNumber().getPartNumber() + ": "
                    + part2Sequence.getSequence().length());
            for (SequenceFeature item : temp) {

                System.out.println(item.getName() + ": " + item.getStart() + ":" + item.getEnd());
            }

            //sequenceFeatures.addAll(determineAssemblyFeatures(part2));
            // SequenceManager.saveSequence(part2Sequence);
        } catch (ManagerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @SuppressWarnings("unused")
    private static void mainRunJoin() {
        try {
            Part part1 = (Part) EntryManager.get(4394);
            Sequence part1Sequence = SequenceManager.getByEntry(part1);
            Set<SequenceFeature> sequenceFeatures = part1Sequence.getSequenceFeatures();
            //sequenceFeatures.addAll(determineAssemblyFeatures(part1));
            //SequenceManager.saveSequence(part1Sequence);
            AssemblyController as = new AssemblyController(AccountManager.get(86));
            ////Part newPart = as.joinBiobrickA(part1, part1);

            System.out.println(sequenceFeatures.size());
        } catch (ManagerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void setAssemblyUtils(ArrayList<AssemblyUtils> assemblyUtils) {
        this.assemblyUtils = assemblyUtils;
    }

    public ArrayList<AssemblyUtils> getAssemblyUtils() {
        return assemblyUtils;
    }

    /**
     * Comparator for assembly basic sequence features
     * 
     * @param sequenceFeatures1
     * @param sequenceFeatures2
     * @return 0 if identical.
     */
    public int compareAssemblyAnnotations(AssemblyStandard standard,
            SequenceFeatureCollection sequenceFeatures1, SequenceFeatureCollection sequenceFeatures2) {
        int result = -1;
        if (standard == AssemblyStandard.BIOBRICKA) {
            result = getAssemblyUtils().get(0).compareAssemblyAnnotations(sequenceFeatures1,
                sequenceFeatures2);

        } else if (standard == AssemblyStandard.BIOBRICKB) {
            result = getAssemblyUtils().get(1).compareAssemblyAnnotations(sequenceFeatures1,
                sequenceFeatures2);

        } else if (standard == AssemblyStandard.RAW) {
            result = getAssemblyUtils().get(2).compareAssemblyAnnotations(sequenceFeatures1,
                sequenceFeatures2);
        }

        return result;
    }

    public void populateAssemblyAnnotations(Sequence partSequence) throws ControllerException {
        SequenceFeatureCollection newSequenceFeatures = determineAssemblyFeatures(partSequence);
        Set<SequenceFeature> temp = partSequence.getSequenceFeatures();
        SequenceFeatureCollection oldSequenceFeatures = null;
        if (temp.size() > 0) {
            // old sequencefeatures exist
            if (temp instanceof SequenceFeatureCollection) {
                oldSequenceFeatures = (SequenceFeatureCollection) temp;
            } else {
                throw new ControllerException("Bad SequenceFeatureCollection");
            }

            // If innerFeature has not changed, keep all old sequenceFeatures
            Part part = null;
            if (partSequence.getEntry() instanceof Part) {
                part = (Part) partSequence.getEntry();
            }
            if (compareAssemblyAnnotations(part.getPackageFormat(), newSequenceFeatures,
                oldSequenceFeatures) == 0) {
                newSequenceFeatures = oldSequenceFeatures;
            } else {
                // discard old sequenceFeatures
            }
        }
        partSequence.setSequenceFeatures(newSequenceFeatures);
        try {
            SequenceManager.saveSequence(partSequence);
        } catch (ManagerException e) {
            throw new ControllerException(e);
        }
    }
}
